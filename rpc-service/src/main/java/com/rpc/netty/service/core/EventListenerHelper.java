package com.rpc.netty.service.core;

import com.rpc.netty.service.event.Event;
import com.rpc.netty.service.listener.EventListener;

/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 20:52 2022/4/7
 * @Modified By: 事件监听助手
 */
public final class EventListenerHelper {

    private static final ThreadLocal<EventListener<? extends Event>> LISTENER_THREAD_LOCAL = new ThreadLocal<>();

    public static final void registerListener(EventListener<? extends Event> listener){
        LISTENER_THREAD_LOCAL.set(listener);
    }

    public static final EventListener<? extends Event>  getListener(){
        return LISTENER_THREAD_LOCAL.get();
    }

    public static final void removeListener(){
        LISTENER_THREAD_LOCAL.remove();
    }
}
