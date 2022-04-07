package com.rpc.netty.service.core;

import com.rpc.netty.service.event.Event;
import com.rpc.netty.service.listener.EventListener;
import com.rpc.netty.service.listener.EventListenerChain;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 16:04 2022/4/3
 * @Modified By:
 */
public abstract class AbstractEventProcessor<T extends Event,E extends EventListener> implements EventProcessor<T,E> {

    private Map<String, EventListenerChain> listener;

    public AbstractEventProcessor(){
        listener = new ConcurrentHashMap<>();
    }

    protected EventListener getListener(String key){
        return this.listener.get(key);
    }

    protected void addListener(String key, EventListener listener){
        EventListenerChain chain = this.listener.get(key);
        if (chain != null){
            chain.addListener(listener);
        }else {
            EventListenerChain listenerChain = new EventListenerChain();
            chain = this.listener.putIfAbsent(key, listenerChain);
            listenerChain = chain == null? listenerChain : chain;
            listenerChain.addListener(listener);
        }
    }

    protected void removeListener(String key, EventListener listener){
        EventListenerChain chain = this.listener.get(key);
        if (chain != null){
            chain.removeListener(listener);
        }
    }
}
