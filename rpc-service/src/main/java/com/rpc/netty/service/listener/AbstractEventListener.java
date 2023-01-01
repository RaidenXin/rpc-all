package com.rpc.netty.service.listener;


import com.rpc.netty.client.command.RpcCommand;
import com.rpc.netty.client.utils.SerializationUtil;
import com.rpc.netty.core.response.RpcResponse;
import com.rpc.netty.service.event.Event;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 17:39 2022/4/3
 * @Modified By: 抽象的事件监听者
 */
public abstract class AbstractEventListener<T extends Event> implements EventListener<T> {

    private static final Logger log = LoggerFactory.getLogger(AbstractEventListener.class);

    private EventListenerChain rootListener;
    private ChannelHandlerContext ctx;
    private int serialNo;

    public void setRootListener(EventListenerChain listener){
        this.rootListener = listener;
    }

    public int getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(int serialNo) {
        this.serialNo = serialNo;
    }

    public void setChannel(ChannelHandlerContext ctx){
        this.ctx = ctx;
    }

    public ChannelHandlerContext getChannel(){
        return ctx;
    }

    public final void onEvent(T event){
        if (log.isInfoEnabled()){
            log.info("触发事件！{}", this);
        }
        RpcCommand response = new RpcCommand();
        response.setSerialNo(this.serialNo);
        RpcResponse<?> rpcResponse;
        try {
            Object config = onEventHandler(event);
            rpcResponse = RpcResponse.success(config);
            try {
                response.setBody(SerializationUtil.encodeResponse(rpcResponse));
                ctx.writeAndFlush(response);
            } catch (Throwable e) {
                log.error("process request over, but response failed", e);
                log.error(rpcResponse.toString());
                log.error(response.toString());
            }
        }catch (Exception e){
            rpcResponse = RpcResponse.fail(e.getMessage());
            response.setBody(SerializationUtil.encodeResponse(rpcResponse));
            try {
                ctx.writeAndFlush(response);
            } catch (Throwable t) {
                log.error("process request over, but response failed", t);
                log.error(rpcResponse.toString());
                log.error(response.toString());
            }
        }finally {
            removeListener();
        }
    }

    protected abstract Object onEventHandler(T event);

    /**
     * 这个 key 必须与被监听的事件的一致
     * @return
     */
    protected abstract String getKey();

    public void clear() {
        if (log.isInfoEnabled()){
            log.info("开始执行清理任务！{}", this);
        }
        if (isOk()){
            RpcCommand response = new RpcCommand();
            response.setSerialNo(this.serialNo);
            //如果超时了就直接返回空
            RpcResponse rpcResponse = RpcResponse.success(null);
            response.setBody(SerializationUtil.encodeResponse(rpcResponse));
            try {
                ctx.writeAndFlush(response);
            } catch (Throwable e) {
                log.error("process request over, but response failed", e);
                log.error(rpcResponse.toString());
                log.error(response.toString());
            }
        }
    }

    private boolean isOk(){
        if (ctx == null){
            return false;
        }
        return ctx.channel() != null && ctx.channel().isActive();
    }

    /**
     * 清理掉自己
     */
    protected void removeListener() {
        if (rootListener != null){
            rootListener.removeListener(this);
        }
    }
}
