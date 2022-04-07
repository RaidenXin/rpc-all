package com.rpc.netty.client;

import com.rpc.netty.client.command.RpcCommand;
import com.rpc.netty.client.decoder.NettyDecoder;
import com.rpc.netty.client.encoder.NettyEncoder;
import com.rpc.netty.client.exception.RpcConnectException;
import com.rpc.netty.client.exception.RpcSendRequestException;
import com.rpc.netty.client.exception.RpcTimeoutException;
import com.rpc.netty.client.handler.NettyClientHandler;
import com.rpc.netty.client.response.ResponseFuture;
import com.rpc.netty.client.utils.NettyUtils;
import com.rpc.netty.client.utils.RpcHelper;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 18:46 2022/3/26
 * @Modified By:
 */
@Slf4j
public class NettyClient {


    private static final long LOCK_TIMEOUT_MILLIS = 3000;

    private static final long CONNECT_TIMEOUT_MILLIS = 1000l;
    /**
     * 长轮询超时时间 单位秒
     */
    public static final int LONG_POLL_TIMEOUT = 300;

    private Bootstrap bootstrap;

    private final EventLoopGroup eventLoopGroupWorker;

    private final Lock lockChannelTables = new ReentrantLock();

    private DefaultEventExecutorGroup defaultEventExecutorGroup;

    protected final ConcurrentMap<Integer /* opaque */, ResponseFuture> responseTable =
            new ConcurrentHashMap<>(256);

    private final ConcurrentMap<String /* addr */, ChannelWrapper> channelTables = new ConcurrentHashMap<String, ChannelWrapper>();

    public NettyClient() {
        this.eventLoopGroupWorker = new NioEventLoopGroup(1);
        this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(1,
                new ThreadFactory() {
                    private AtomicInteger threadIndex = new AtomicInteger(0);
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "NettyClientWorkerThread_" + this.threadIndex.incrementAndGet());
                    }
                });
        bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroupWorker).channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, false)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                .handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast(defaultEventExecutorGroup,
                        new NettyEncoder(),
                        new NettyDecoder(),
                        new IdleStateHandler(0, 0,  LONG_POLL_TIMEOUT),
                        new NettyConnectManageHandler(NettyClient.this),
                        new NettyClientHandler(NettyClient.this));
            }
        });
    }

    public void closeChannel(final Channel channel){
        this.closeChannel(null, channel);
    }

    public void closeChannel(final String addr, final Channel channel) {
        if (null == channel)
            return;

        final String addrRemote = null == addr ? RpcHelper.parseChannelRemoteAddr(channel) : addr;

        try {
            if (this.lockChannelTables.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                try {
                    boolean removeItemFromTable = true;
                    final ChannelWrapper prevCW = this.channelTables.get(addrRemote);

                    log.info("closeChannel: begin close the channel[{}] Found: {}", addrRemote, prevCW != null);

                    if (null == prevCW) {
                        log.info("closeChannel: the channel[{}] has been removed from the channel table before", addrRemote);
                        removeItemFromTable = false;
                    } else if (prevCW.getChannel() != channel) {
                        log.info("closeChannel: the channel[{}] has been closed before, and has been created again, nothing to do.",
                                addrRemote);
                        removeItemFromTable = false;
                    }

                    if (removeItemFromTable) {
                        this.channelTables.remove(addrRemote);
                        log.info("closeChannel: the channel[{}] was removed from channel table", addrRemote);
                    }

                    NettyUtils.closeChannel(channel);
                } catch (Exception e) {
                    log.error("closeChannel: close the channel exception", e);
                } finally {
                    this.lockChannelTables.unlock();
                }
            } else {
                log.warn("closeChannel: try to lock channel table, but timeout, {}ms", LOCK_TIMEOUT_MILLIS);
            }
        } catch (InterruptedException e) {
            log.error("closeChannel exception", e);
        }
    }

    /**
     * 处理之前发出的请求的响应
     *
     * @param ctx channel handler context.
     * @param command response command instance.
     */
    public void processResponseCommand(ChannelHandlerContext ctx, RpcCommand command) {
        final int serialNo = command.getSerialNo();
        final ResponseFuture responseFuture = responseTable.remove(serialNo);
        if (responseFuture != null) {
            responseFuture.putResponse(command);
        } else {
            log.warn("receive response, but not matched any request, " + RpcHelper.parseChannelRemoteAddr(ctx.channel()));
            log.warn(command.toString());
        }
    }

    public RpcCommand invokeSync(String addr, final RpcCommand request, long timeoutMillis)
            throws InterruptedException,IllegalAccessException, RpcConnectException,RpcSendRequestException,RpcTimeoutException {
        long beginStartTime = System.currentTimeMillis();
        final Channel channel = this.getAndCreateChannel(addr);
        if (channel != null && channel.isActive()) {
            try {
                long costTime = System.currentTimeMillis() - beginStartTime;
                if (timeoutMillis < costTime) {
                    throw new RpcTimeoutException("invokeSync call timeout");
                }
                RpcCommand response = this.invokeSyncImpl(channel, request, timeoutMillis - costTime);
                return response;
            } catch (RpcSendRequestException e) {
                log.warn("invokeSync: send request exception, so close the channel[{}]", addr);
                this.closeChannel(addr, channel);
                throw e;
            } catch (RpcTimeoutException e) {
                log.warn("invokeSync: wait response timeout exception, the channel[{}]", addr);
                throw e;
            }
        } else {
            this.closeChannel(addr, channel);
            throw new RpcConnectException(addr);
        }
    }

    public RpcCommand invokeSyncImpl(final Channel channel, final RpcCommand request,
                                                                  final long timeoutMillis)
            throws InterruptedException, RpcSendRequestException, RpcTimeoutException {
        final int serialNo = request.getSerialNo();

        try {
            final ResponseFuture responseFuture = new ResponseFuture(serialNo, timeoutMillis);
            this.responseTable.put(serialNo, responseFuture);
            final SocketAddress addr = channel.remoteAddress();
            channel.writeAndFlush(request).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture f) throws Exception {
                    if (f.isSuccess()) {
                        responseFuture.setSendRequestOK(true);
                        return;
                    } else {
                        responseFuture.setSendRequestOK(false);
                    }
                    //删除缓存
                    responseTable.remove(serialNo);
                    responseFuture.setCause(f.cause());
                    responseFuture.putResponse(null);
                    log.warn("send a request command to channel <" + addr + "> failed.");
                }
            });

            RpcCommand responseCommand = responseFuture.waitResponse(timeoutMillis);
            if (null == responseCommand) {
                if (responseFuture.isSendRequestOK()) {
                    throw new RpcTimeoutException(RpcHelper.parseSocketAddressAddr(addr), timeoutMillis,
                            responseFuture.getCause());
                } else {
                    throw new RpcSendRequestException(RpcHelper.parseSocketAddressAddr(addr), responseFuture.getCause());
                }
            }

            return responseCommand;
        } finally {
            this.responseTable.remove(serialNo);
        }
    }

    private Channel getAndCreateChannel(final String addr) throws IllegalAccessException, InterruptedException {
        if (null == addr) {
            throw new IllegalAccessException("The addr parameter cannot be null !");
        }

        ChannelWrapper cw = this.channelTables.get(addr);
        if (cw != null && cw.isOK()) {
            return cw.getChannel();
        }

        return this.createChannel(addr);
    }

    private Channel createChannel(final String addr) throws InterruptedException {
        ChannelWrapper cw = this.channelTables.get(addr);
        if (cw != null && cw.isOK()) {
            return cw.getChannel();
        }

        if (this.lockChannelTables.tryLock(LOCK_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
            try {
                boolean createNewConnection;
                cw = this.channelTables.get(addr);
                if (cw != null) {

                    if (cw.isOK()) {
                        return cw.getChannel();
                    } else if (!cw.getChannelFuture().isDone()) {
                        createNewConnection = false;
                    } else {
                        this.channelTables.remove(addr);
                        createNewConnection = true;
                    }
                } else {
                    createNewConnection = true;
                }

                if (createNewConnection) {
                    ChannelFuture channelFuture = this.bootstrap.connect(RpcHelper.string2SocketAddress(addr));
                    log.info("createChannel: begin to connect remote host[{}] asynchronously", addr);
                    cw = new ChannelWrapper(channelFuture);
                    this.channelTables.put(addr, cw);
                }
            } catch (Exception e) {
                log.error("createChannel: create channel exception", e);
            } finally {
                this.lockChannelTables.unlock();
            }
        } else {
            log.warn("createChannel: try to lock channel table, but timeout, {}ms", LOCK_TIMEOUT_MILLIS);
        }

        if (cw != null) {
            ChannelFuture channelFuture = cw.getChannelFuture();
            if (channelFuture.awaitUninterruptibly(CONNECT_TIMEOUT_MILLIS)) {
                if (cw.isOK()) {
                    log.info("createChannel: connect remote host[{}] success, {}", addr, channelFuture.toString());
                    return cw.getChannel();
                } else {
                    log.warn("createChannel: connect remote host[" + addr + "] failed, " + channelFuture.toString(), channelFuture.cause());
                }
            } else {
                log.warn("createChannel: connect remote host[{}] timeout {}ms, {}", addr, CONNECT_TIMEOUT_MILLIS,
                        channelFuture.toString());
            }
        }

        return null;
    }

    public void shutdown() {
        try {

            for (ChannelWrapper cw : this.channelTables.values()) {
                this.closeChannel(null, cw.getChannel());
            }

            this.channelTables.clear();

            this.eventLoopGroupWorker.shutdownGracefully();
            if (this.defaultEventExecutorGroup != null) {
                this.defaultEventExecutorGroup.shutdownGracefully();
            }
        } catch (Exception e) {
            log.error("NettyRemotingClient shutdown exception, ", e);
        }
    }

    static class ChannelWrapper {
        private final ChannelFuture channelFuture;

        public ChannelWrapper(ChannelFuture channelFuture) {
            this.channelFuture = channelFuture;
        }

        public boolean isOK() {
            return this.channelFuture.channel() != null && this.channelFuture.channel().isActive();
        }

        public boolean isWritable() {
            return this.channelFuture.channel().isWritable();
        }

        private Channel getChannel() {
            return this.channelFuture.channel();
        }

        public ChannelFuture getChannelFuture() {
            return channelFuture;
        }
    }

    @Slf4j
    static class NettyConnectManageHandler extends ChannelDuplexHandler {


        private NettyClient nettyClient;

        public NettyConnectManageHandler(NettyClient nettyClient){
            this.nettyClient = nettyClient;
        }

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
            nettyClient.closeChannel(ctx.channel());
            super.disconnect(ctx, promise);
        }

        @Override
        public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            final String remoteAddress = RpcHelper.parseChannelRemoteAddr(ctx.channel());
            log.info("NETTY CLIENT PIPELINE: CLOSE {}", remoteAddress);
            nettyClient.closeChannel(ctx.channel());
            super.close(ctx, promise);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent event = (IdleStateEvent) evt;
                if (event.state().equals(IdleState.ALL_IDLE)) {
                    final String remoteAddress = RpcHelper.parseChannelRemoteAddr(ctx.channel());
                    log.warn("NETTY CLIENT PIPELINE: IDLE exception [{}]", remoteAddress);
                    nettyClient.closeChannel(ctx.channel());
                }
            }

            ctx.fireUserEventTriggered(evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            final String remoteAddress = RpcHelper.parseChannelRemoteAddr(ctx.channel());
            log.warn("NETTY CLIENT PIPELINE: exceptionCaught {}", remoteAddress);
            log.warn("NETTY CLIENT PIPELINE: exceptionCaught exception.", cause);
            nettyClient.closeChannel(ctx.channel());
        }
    }
}
