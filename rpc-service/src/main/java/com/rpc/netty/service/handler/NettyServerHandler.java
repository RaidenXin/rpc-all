package com.rpc.netty.service.handler;

import com.rpc.netty.client.command.RpcCommand;
import com.rpc.netty.client.exception.RpcException;
import com.rpc.netty.client.utils.SerializationUtil;
import com.rpc.netty.service.annotation.RpcRequestMapping;
import com.rpc.netty.service.core.EventListenerHelper;
import com.rpc.netty.service.core.EventManager;
import com.rpc.netty.service.event.Event;
import com.rpc.netty.service.factory.ServerWorkHandlerFactory;
import com.rpc.netty.service.listener.EventListener;
import com.rpc.netty.service.mapping.MappingHandler;
import com.rpc.netty.core.response.RpcResponse;
import com.rpc.netty.service.task.CleanListenerTask;
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
                RpcRequestMapping requestMapping = clazz.getAnnotation(RpcRequestMapping.class);
                String rootPath = requestMapping != null ? requestMapping.path() : "";
                Method[] methods = clazz.getMethods();
                for (Method method : methods){
                    RpcRequestMapping mapping = method.getAnnotation(RpcRequestMapping.class);
                    if (mapping != null){
                        Class<?>[] parameterTypes = method.getParameterTypes();
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
                        String api = path.toString();
                        MappingHandler mappingHandler = MappingHandler.build(api, serverWorkHandler, method, parameterTypes.length > 0 ? parameterTypes[0] : null);
                        methodMapping.putIfAbsent(api, mappingHandler);
                    }
                }
            }
        }
        this.methodMapping = methodMapping;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcCommand rpcCommand) throws Exception {
        processRequestCommand(channelHandlerContext, rpcCommand);
    }

    private void processRequestCommand(ChannelHandlerContext ctx, RpcCommand msg) {
        int serialNo = msg.getSerialNo();
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
                    if (mappingHandler == null){
                        throw new RpcException("No corresponding method was found");
                    }

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
                    Object result = request == null ? mappingHandler.invoke() :  mappingHandler.invoke(request);
                    EventListener<? extends Event> listener = EventListenerHelper.getListener();
                    //如果返回了监听器说明要监听
                    if (listener != null){
                        //注册一个监听器
                        EventManager.registerEventListener(listener);
                        //设置一个清理任务
                        submitCleanupTask(listener);
                    }else {
                        response = RpcResponse.success(result);
                        rpcCommand.setBody(SerializationUtil.encodeResponse(response));
                        try {
                            ctx.writeAndFlush(rpcCommand);
                        } catch (Throwable e) {
                            log.error("process request over, but response failed", e);
                            log.error(result.toString());
                            log.error(rpcCommand.toString());
                        }
                    }
                }catch (Exception e){
                    log.error("NettyService 处理请求出错！", e);
                    response = RpcResponse.fail(e.getMessage());
                    rpcCommand.setBody(SerializationUtil.encode(response));
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

    private void submitCleanupTask(EventListener<? extends Event> listener){
        CleanListenerTask task = CleanListenerTask.build(listener);
        taskDelayQueue.put(task);
        //提交一个清理任务
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
}
