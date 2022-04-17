package com.rpc.netty.service.test.listener;

import com.rpc.netty.service.listener.AbstractEventListener;
import com.rpc.netty.service.test.event.TestEvent;
import com.rpc.netty.service.test.model.ResponseData;
import lombok.extern.slf4j.Slf4j;

/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 20:03 2022/4/17
 * @Modified By:
 */
@Slf4j
public class TestEventListener extends AbstractEventListener<TestEvent> {

    private String key;

    public TestEventListener(String key) {
        this.key = key;
    }

    @Override
    protected ResponseData onEventHandler(TestEvent event) {
        log.info("触发事件监听器！");
        return new ResponseData(1, "李四");
    }

    @Override
    public String getKey() {
        return key;
    }
}
