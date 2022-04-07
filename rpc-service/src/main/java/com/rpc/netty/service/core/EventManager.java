package com.rpc.netty.service.core;

import com.rpc.netty.service.event.Event;
import com.rpc.netty.service.listener.EventListener;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.ParameterizedType;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 16:29 2022/4/3
 * @Modified By:
 */
@Slf4j
public final class EventManager {

    private static final Map<Class<? extends Event>, EventProcessor> eventProcessor;

    static {
        eventProcessor = new ConcurrentHashMap<>();
        ServiceLoader<EventProcessor> eventProcessors = ServiceLoader.load(EventProcessor.class);
        for (EventProcessor processor : eventProcessors){
            //获取处理的泛型类
            Class<? extends Event> key = (Class)((ParameterizedType)processor.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
            eventProcessor.putIfAbsent(key, processor);
        }
    }

    /**
     * 发布事件
     * @param event
     */
    public static final void publishEvent(Event event){
        if (event == null){
            throw new IllegalArgumentException("The parameter cannot be null");
        }
        Class<? extends Event> key = event.getClass();
        EventProcessor processor = eventProcessor.get(key);
        if (processor != null){
            processor.handleEvent(event);
        }else {
            log.error("No corresponding event handler was found! class:{}", key.getName());
        }
    }

    /**
     * 注册监听器
     * @param listener
     */
    public static final void registerEventListener(EventListener listener){
        if (listener == null){
            return;
        }
        Class<? extends Event> key = (Class)((ParameterizedType) listener.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        EventProcessor processor = eventProcessor.get(key);
        if (processor != null){
            processor.registerEventListener(listener);
        }else {
            log.error("No corresponding event handler was found! class:{}", key.getName());
        }
    }

    /**
     * 注册监听器
     * @param listener
     */
    public static final void removeEventListener(EventListener listener){
        if (listener == null){
            return;
        }
        Class<? extends Event> key = (Class)((ParameterizedType) listener.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        EventProcessor processor = eventProcessor.get(key);
        if (processor != null){
            processor.removeEventListener(listener);
        }else {
            log.error("No corresponding event handler was found! class:{}", key.getName());
        }
    }
}
