package com.rpc.netty.service.test.event;

import com.rpc.netty.service.core.AbstractEventProcessor;
import com.rpc.netty.service.listener.EventListener;
import com.rpc.netty.service.test.listener.TestEventListener;

/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 20:55 2022/4/17
 * @Modified By:
 */
public class TestEventProcessor extends AbstractEventProcessor<TestEvent, TestEventListener> {

    @Override
    public void handleEvent(TestEvent event) {
        EventListener listener = getListener(event.getKey());
        listener.onEvent(event);
    }

    @Override
    public void registerEventListener(TestEventListener listener) {
        addListener(listener.getKey(), listener);
    }

    @Override
    public void removeEventListener(TestEventListener listener) {
        removeListener(listener.getKey(), listener);
    }
}
