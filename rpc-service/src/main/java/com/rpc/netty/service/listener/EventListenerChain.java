package com.rpc.netty.service.listener;

import com.rpc.netty.service.event.Event;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 15:42 2022/4/3
 * @Modified By: 监听者链
 */
@Slf4j
public class EventListenerChain implements EventListener {

    private List<EventListener> listeners;
    private ReentrantReadWriteLock.ReadLock readLock;
    private ReentrantReadWriteLock.WriteLock writeLock;

    public EventListenerChain(){
        this.listeners = new ArrayList<>();
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        this.readLock = lock.readLock();
        this.writeLock = lock.writeLock();
    }

    @Override
    public void onEvent(Event event) {
        readLock.lock();
        /**
         * 每次执行完所有的监听者后移除全部监听
         * 等待下一次请求注册监听
         */
        List<EventListener> eventListeners;
        try {
            eventListeners = new ArrayList<>(listeners);
        }finally {
            readLock.unlock();
        }
        for (EventListener listener : eventListeners){
            try {
                listener.onEvent(event);
            }catch (Throwable e){
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 添加监听器
     * @param listener
     */
    public void addListener(EventListener listener){
        writeLock.lock();
        try {
            if (listeners == null){
                listeners = new ArrayList<>();
            }
            ((AbstractEventListener) listener).setRootListener(this);
            listeners.add(listener);
        }finally {
            writeLock.unlock();
        }
    }

    /**
     * 移除监听器
     * @param listener
     */
    public void removeListener(EventListener listener){
        writeLock.lock();
        try {
            if (listeners == null){
                return;
            }
            listeners.remove(listener);
        }finally {
            writeLock.unlock();
            //删除完成后 执行清理工作
            ((AbstractEventListener) listener).clear();
        }
    }
}
