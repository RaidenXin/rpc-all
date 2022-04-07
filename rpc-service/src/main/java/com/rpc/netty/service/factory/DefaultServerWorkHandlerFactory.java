package com.rpc.netty.service.factory;

import com.rpc.netty.service.handler.ServerWorkHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

class DefaultServerWorkHandlerFactory implements ServerWorkHandlerFactory{
    @Override
    public List<ServerWorkHandler> getBeans() {
        ServiceLoader<ServerWorkHandler> handlers = ServiceLoader.load(ServerWorkHandler.class);
        List<ServerWorkHandler> workHandlers = new ArrayList<>();
        for (ServerWorkHandler workHandler : handlers){
            workHandlers.add(workHandler);
        }
        return workHandlers;
    }
}
