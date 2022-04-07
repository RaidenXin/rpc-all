package com.rpc.netty.service.core;

import com.rpc.netty.service.event.Event;
import com.rpc.netty.service.listener.EventListener;


/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 14:51 2022/4/3
 * @Modified By:
 */
public interface EventProcessor<T extends Event,E extends EventListener> {

    void handleEvent(T event);

    void registerEventListener(E listener);

    void removeEventListener(E listener);
}
