package com.rpc.netty.service.core;

import com.rpc.netty.client.command.RpcCommand;
import com.rpc.netty.client.exception.RpcException;
import com.rpc.netty.client.utils.SerializationUtil;
import com.rpc.netty.service.annotation.RpcRequestMapping;
import com.rpc.netty.service.core.EventListenerHelper;
import com.rpc.netty.service.core.EventManager;
import com.rpc.netty.service.event.Event;
import com.rpc.netty.service.factory.ServerWorkHandlerFactory;
import com.rpc.netty.service.handler.ServerWorkHandler;
import com.rpc.netty.service.listener.AbstractEventListener;
import com.rpc.netty.service.listener.EventListener;
import com.rpc.netty.service.mapping.MappingHandler;
import com.rpc.netty.core.response.RpcResponse;
import com.rpc.netty.service.core.CleanListenerTask;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@ChannelHandler.Sharable
@Slf4j
public class NettyServerHandler extends SimpleChannelInboundHandler<RpcCommand> {


    private static final String OBLIQUE_LINE = "/";

    private static final int PUBLIC_WORKER_THREADS = 8;

    private static final int CLEAR_WORKER_THREADS = 3;

    private final ExecutorService publicExecutor;
    private final ExecutorService clearWorkerExecutor;

    private final DelayQueue<CleanListenerTask> taskDelayQueue;

    private Map<String, MappingHandler> methodMapping;

    public NettyServerHandler(){
        this.taskDelayQueue = new DelayQueue<>();
        this.publicExecutor =  Executors.newFixedThreadPool(PUBLIC_WORKER_THREADS, new ThreadFactory() {
            private AtomicInteger threadIndex = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "ServerWorkPublicExecutor_" + this.threadIndex.incrementAndGet());
            }
        });
        this.clearWorkerExecutor =  Executors.newFixedThreadPool(CLEAR_WORKER_THREADS, new ThreadFactory() {
            private AtomicInteger threadIndex = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "ServerClearWorkerExecutor_" + this.threadIndex.incrementAndGet());
            }
        });
        ServerWorkHandlerFactory factory = ServerWorkHandlerFactory.createFactory();
        List<ServerWorkHandler> beans = factory.getBeans();
        Map<String, MappingHandler> methodMapping = new HashMap<>();
        for (ServerWorkHandler serverWorkHandler : beans){
            if (serverWorkHandler != null){
                Class<? extends ServerWorkHandler> clazz = serverWorkHandler.getClass();
                Method[] methods = clazz.getMethods();
                for (Method method : methods){
                    RpcRequestMapping mapping = method.getAnnotation(RpcRequestMapping.class);
                    if (Objects.isNull(mapping)){
                        continue;
                    }
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    String api = getApiPath(clazz, mapping);
                    MappingHandler mappingHandler = MappingHandler.build(api, serverWorkHandler, method, parameterTypes.length > 0 ? parameterTypes[0] : null);
                    methodMapping.putIfAbsent(api, mappingHandler);
                }
            }
        }
        this.methodMapping = methodMapping;
    }

    /**
     * 获取请求路径
     * @param clazz
     * @param mapping
     * @return
     */
    private String getApiPath(Class<? extends ServerWorkHandler> clazz, RpcRequestMapping mapping) {
        RpcRequestMapping requestMapping = clazz.getAnnotation(RpcRequestMapping.class);
        String rootPath = requestMapping != null ? requestMapping.path() : "";
        StringBuilder path = new StringBuilder();
        if (rootPath.startsWith(OBLIQUE_LINE)){
            path.append(rootPath);
        }else {
            path.append(OBLIQUE_LINE);
            path.append(rootPath);
        }
        String secondaryPath = mapping.path();
        if (secondaryPath.startsWith(OBLIQUE_LINE)){
            path.append(secondaryPath);
        }else {
            path.append(OBLIQUE_LINE);
            path.append(secondaryPath);
        }
        return path.toString();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcCommand rpcCommand) throws Exception {
        processRequestCommand(channelHandlerContext, rpcCommand);
    }

    private void processRequestCommand(ChannelHandlerContext ctx, RpcCommand msg) {
        final int serialNo = msg.getSerialNo();
        String path = msg.getPath();
        byte[] body = msg.getBody();
        try {
            //提交一个主任务
            publicExecutor.execute(() -> {
                RpcCommand rpcCommand = new RpcCommand();
                rpcCommand.setSerialNo(serialNo);
                RpcResponse<?> response;
                try {
                    MappingHandler mappingHandler = methodMapping.get(path);
                    if (Objects.isNull(mappingHandler)){
                        throw new RpcException("No corresponding method was found");
                    }
                    // 解析出请求参数
                    Object request = SerializationUtil.decode(body, mappingHandler.getParamType());
                    if (log.isInfoEnabled()){
                        SocketAddress socketAddress = ctx.channel().remoteAddress();
                        if (socketAddress instanceof InetSocketAddress){
                            InetSocketAddress inetSocketAddress = (InetSocketAddress) socketAddress;
                            log.info("addr: {}:{} msg:{}", inetSocketAddress.getHostString(), inetSocketAddress.getPort(), request);
                        }else {
                            log.info("msg:{}", request);
                        }
                    }
                    Object result = Objects.isNull(request) ? mappingHandler.invoke() :  mappingHandler.invoke(request);
                    EventListener<? extends Event> listener = EventListenerHelper.getListener();
                    //如果返回了监听器说明要监听
                    if (Objects.nonNull(listener)){
                        // 注册事件监听器
                        registerEventListener(listener, ctx, serialNo);
                    }else {
                        writeAndFlush(ctx, rpcCommand, result);
                    }
                }catch (Exception e){
                    log.error("NettyService 处理请求出错！", e);
                    response = RpcResponse.fail(e.getMessage());
                    rpcCommand.setBody(SerializationUtil.encodeResponse(response));
                    try {
                        ctx.writeAndFlush(rpcCommand);
                    } catch (Throwable t) {
                        log.error("process request over, but response failed", t);
                        log.error(response.toString());
                        log.error(rpcCommand.toString());
                    }
                }finally {
                    //删除缓存的监听器
                    EventListenerHelper.removeListener();
                }
            });
        }catch (Exception e){
            log.error("NettyService 处理请求出错！", e);
        }
    }

    /**
     * 注册事件监听器
     * @param listener
     */
    private void registerEventListener(EventListener<? extends Event> listener, ChannelHandlerContext ctx, final int serialNo) {
        if (listener instanceof AbstractEventListener){
            //设置通道
            ((AbstractEventListener) listener).setChannel(ctx);
            //设置流水号
            ((AbstractEventListener) listener).setSerialNo(serialNo);
        }
        //注册一个监听器
        EventManager.registerEventListener(listener);
        //设置一个清理任务
        CleanListenerTask task = CleanListenerTask.build(listener);
        taskDelayQueue.put(task);
        //提交一个清理任务监听器的任务
        clearWorkerExecutor.execute(() -> {
            CleanListenerTask queryTask = null;
            do {
                try {
                    queryTask = taskDelayQueue.poll(1000, TimeUnit.MINUTES);
                } catch (InterruptedException e) {
                }
            }while (queryTask == null);
            queryTask.run();
        });
    }

    /**
     * 回写数据
     * @param ctx
     * @param rpcCommand
     * @param result
     */
    private void writeAndFlush(ChannelHandlerContext ctx, RpcCommand rpcCommand, Object result) {
        final RpcResponse<?> response = RpcResponse.success(result);
        rpcCommand.setBody(SerializationUtil.encodeResponse(response));
        try {
            ctx.writeAndFlush(rpcCommand);
        } catch (Throwable e) {
            log.error("process request over, but response failed", e);
            log.error(result.toString());
            log.error(rpcCommand.toString());
        }
    }
}
