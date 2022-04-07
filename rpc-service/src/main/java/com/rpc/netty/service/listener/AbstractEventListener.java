package com.rpc.netty.service.listener;


import com.rpc.netty.service.event.Event;

/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 17:39 2022/4/3
 * @Modified By:
 */
public abstract class AbstractEventListener<T extends Event> implements EventListener<T> {

    private EventListenerChain rootListener;

    public void setRootListener(EventListenerChain listener){
        this.rootListener = listener;
    }

    /**
     * 清理掉自己
     */
    protected void removeListener() {
        if (rootListener != null){
            rootListener.removeListener(this);
        }
    }

    public abstract void clear();
}
