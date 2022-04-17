package com.rpc.netty.service;


import com.rpc.netty.client.NettyClient;
import com.rpc.netty.client.decoder.NettyDecoder;
import com.rpc.netty.client.encoder.NettyEncoder;
import com.rpc.netty.client.utils.NettyUtils;
import com.rpc.netty.client.utils.RpcHelper;
import com.rpc.netty.service.handler.NettyServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 16:39 2022/3/27
 * @Modified By: Netty 服务端
 */
@Slf4j
public class NettyService {

    private static final int SERVER_WORKER_THREADS = 8;

    private static final int LISTEN_PORT = 9959;

    private int serverSelectorThreads = 3;
    /**
     * 这里最大的超时时间 要超过客户端轮询超时时间
     */
    private int serverChannelMaxIdleTimeSeconds = NettyClient.LONG_POLL_TIMEOUT + 20;

    private DefaultEventExecutorGroup defaultEventExecutorGroup;

    private NettyEncoder encoder;
    private NettyConnectManageHandler connectionManageHandler;
    private final EventLoopGroup eventLoopGroupSelector;
    private final EventLoopGroup eventLoopGroupBoss;
    private final ServerBootstrap serverBootstrap;
    private final NettyServerHandler serverHandler;

    public NettyService(){
        this.serverBootstrap = new ServerBootstrap();
        this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(
                SERVER_WORKER_THREADS,
                new ThreadFactory() {

                    private AtomicInteger threadIndex = new AtomicInteger(0);

                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "NettyServerCodecThread_" + this.threadIndex.incrementAndGet());
                    }
                });
        this.encoder = new NettyEncoder();
        this.connectionManageHandler = new NettyConnectManageHandler();
        this.serverHandler = new NettyServerHandler();
        if (useEpoll()) {
            this.eventLoopGroupBoss = new EpollEventLoopGroup(1, new ThreadFactory() {
                private AtomicInteger threadIndex = new AtomicInteger(0);

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, String.format("NettyEPOLLBoss_%d", this.threadIndex.incrementAndGet()));
                }
            });

            this.eventLoopGroupSelector = new EpollEventLoopGroup(serverSelectorThreads, new ThreadFactory() {
                private AtomicInteger threadIndex = new AtomicInteger(0);
                private int threadTotal = serverSelectorThreads;

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, String.format("NettyServerEPOLLSelector_%d_%d", threadTotal, this.threadIndex.incrementAndGet()));
                }
            });
        } else {
            this.eventLoopGroupBoss = new NioEventLoopGroup(1, new ThreadFactory() {
                private AtomicInteger threadIndex = new AtomicInteger(0);

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, String.format("NettyNIOBoss_%d", this.threadIndex.incrementAndGet()));
                }
            });

            this.eventLoopGroupSelector = new NioEventLoopGroup(serverSelectorThreads, new ThreadFactory() {
                private AtomicInteger threadIndex = new AtomicInteger(0);
                private int threadTotal = serverSelectorThreads;

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, String.format("NettyServerNIOSelector_%d_%d", threadTotal, this.threadIndex.incrementAndGet()));
                }
            });
        }
        ServerBootstrap childHandler =
                this.serverBootstrap.group(this.eventLoopGroupBoss, this.eventLoopGroupSelector)
                        //是否使用 epoll
                        .channel(useEpoll() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                        //设置服务端接收连接的队列长度 如果连接已满 将会拒绝新的连接
                        .option(ChannelOption.SO_BACKLOG, 1024)
                        //设置端口复用
                        .option(ChannelOption.SO_REUSEADDR, true)
                        //不启用 TCP会主动探测空闲连接的有效性 的功能
                        .option(ChannelOption.SO_KEEPALIVE, false)
                        .childOption(ChannelOption.TCP_NODELAY, true)
                        .localAddress(new InetSocketAddress(LISTEN_PORT))
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            public void initChannel(SocketChannel ch) throws Exception {
                                ch.pipeline().addLast(
                                        defaultEventExecutorGroup,
                                                encoder,
                                                new NettyDecoder(),
                                                new IdleStateHandler(0, 0, serverChannelMaxIdleTimeSeconds),
                                                connectionManageHandler,
                                                serverHandler
                                        );
                            }
                        });
        childHandler.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        try {
            this.serverBootstrap.bind().sync();
        } catch (InterruptedException e1) {
            throw new RuntimeException("this.serverBootstrap.bind().sync() InterruptedException", e1);
        }
    }

    private boolean useEpoll(){
        return NettyUtils.isLinuxPlatform() && Epoll.isAvailable();
    }


    @ChannelHandler.Sharable
    @Slf4j
    static class NettyConnectManageHandler extends ChannelDuplexHandler {

        @Override
        public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress,
                            ChannelPromise promise) throws Exception {
            final String local = localAddress == null ? "UNKNOWN" : RpcHelper.parseSocketAddressAddr(localAddress);
            final String remote = remoteAddress == null ? "UNKNOWN" : RpcHelper.parseSocketAddressAddr(remoteAddress);
            log.info("NETTY CLIENT PIPELINE: CONNECT  {} => {}", local, remote);

            super.connect(ctx, remoteAddress, localAddress, promise);
        }

        @Override
        public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            final String remoteAddress = RpcHelper.parseChannelRemoteAddr(ctx.channel());
            log.info("NETTY CLIENT PIPELINE: DISCONNECT {}", remoteAddress);
            NettyUtils.closeChannel(ctx.channel());
            super.disconnect(ctx, promise);
        }

        @Override
        public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            final String remoteAddress = RpcHelper.parseChannelRemoteAddr(ctx.channel());
            log.info("NETTY CLIENT PIPELINE: CLOSE {}", remoteAddress);
            NettyUtils.closeChannel(ctx.channel());
            super.close(ctx, promise);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent event = (IdleStateEvent) evt;
                if (event.state().equals(IdleState.ALL_IDLE)) {
                    final String remoteAddress = RpcHelper.parseChannelRemoteAddr(ctx.channel());
                    log.warn("NETTY CLIENT PIPELINE: IDLE exception [{}]", remoteAddress);
                    NettyUtils.closeChannel(ctx.channel());
                }
            }

            ctx.fireUserEventTriggered(evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            final String remoteAddress = RpcHelper.parseChannelRemoteAddr(ctx.channel());
            log.warn("NETTY CLIENT PIPELINE: exceptionCaught {}", remoteAddress);
            log.warn("NETTY CLIENT PIPELINE: exceptionCaught exception.", cause);
            NettyUtils.closeChannel(ctx.channel());
        }
    }
}
