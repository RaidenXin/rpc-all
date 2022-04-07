package com.rpc.netty.service.factory;

import com.rpc.netty.service.handler.ServerWorkHandler;

import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 14:22 2022/4/5
 * @Modified By:
 */
public interface ServerWorkHandlerFactory {

    List<ServerWorkHandler> getBeans();

    static ServerWorkHandlerFactory createFactory(){
        ServiceLoader<ServerWorkHandlerFactory> handlers = ServiceLoader.load(ServerWorkHandlerFactory.class);
        Iterator<ServerWorkHandlerFactory> iterator = handlers.iterator();
        if (iterator.hasNext()){
            return iterator.next();
        }
        return new DefaultServerWorkHandlerFactory();
    }
}
