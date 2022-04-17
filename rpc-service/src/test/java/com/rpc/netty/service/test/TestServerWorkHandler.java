package com.rpc.netty.service.test;

import com.rpc.netty.client.command.RpcCommand;
import com.rpc.netty.service.annotation.RpcRequestMapping;
import com.rpc.netty.service.core.EventHelper;
import com.rpc.netty.service.core.EventListenerHelper;
import com.rpc.netty.service.event.Event;
import com.rpc.netty.service.handler.ServerWorkHandler;
import com.rpc.netty.service.test.event.TestEvent;
import com.rpc.netty.service.test.listener.TestEventListener;
import com.rpc.netty.service.test.model.ResponseData;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 19:00 2022/4/17
 * @Modified By:
 */
@RpcRequestMapping(path = "test")
@Slf4j
public class TestServerWorkHandler implements ServerWorkHandler {

    @RpcRequestMapping(path = "handler")
    public ResponseData handler(){
        EventListenerHelper.registerListener(new TestEventListener("test"));
        Runnable runnable = () -> {
            try {
                TimeUnit.SECONDS.sleep(10);
                log.info("开始发送事件");
                EventHelper.publishEvent(new TestEvent("test"));
                log.info("事件发送完成");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        };
        Thread thread = new Thread(runnable);
        thread.start();
        return null;
    }
}
