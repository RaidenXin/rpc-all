package com.rpc.netty.service.test.event;

import com.rpc.netty.service.event.Event;

/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 20:01 2022/4/17
 * @Modified By:
 */
public class TestEvent implements Event {

    private String key;

    public TestEvent(String key){
        this.key = key;
    }

    @Override
    public String getKey() {
        return key;
    }
}
