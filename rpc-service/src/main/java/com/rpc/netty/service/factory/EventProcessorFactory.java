package com.rpc.netty.service.factory;

import com.rpc.netty.service.core.EventProcessor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 20:18 2022/4/17
 * @Modified By:
 */
public interface EventProcessorFactory {

    default List<EventProcessor> getBeans() {
        ServiceLoader<EventProcessor> handlers = ServiceLoader.load(EventProcessor.class);
        List<EventProcessor> workHandlers = new ArrayList<>();
        for (EventProcessor workHandler : handlers){
            workHandlers.add(workHandler);
        }
        return workHandlers;
    }

    static EventProcessorFactory createFactory(){
        ServiceLoader<EventProcessorFactory> handlers = ServiceLoader.load(EventProcessorFactory.class);
        Iterator<EventProcessorFactory> iterator = handlers.iterator();
        if (iterator.hasNext()){
            return iterator.next();
        }
        return new EventProcessorFactory(){};
    }
}
