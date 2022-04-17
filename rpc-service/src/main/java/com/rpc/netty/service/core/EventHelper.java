package com.rpc.netty.service.core;

import com.rpc.netty.service.event.Event;

/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 20:52 2022/4/17
 * @Modified By:
 */
public final class EventHelper {

    /**
     * 发布事件
     * @param event
     */
    public static final void publishEvent(Event event){
        EventManager.publishEvent(event);
    }
}
