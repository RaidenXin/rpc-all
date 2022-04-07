package com.rpc.netty.service.listener;


import com.rpc.netty.service.event.Event;

/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 14:58 2022/4/3
 * @Modified By:
 */
public interface EventListener<T extends Event> {

    void onEvent(T event);
}
