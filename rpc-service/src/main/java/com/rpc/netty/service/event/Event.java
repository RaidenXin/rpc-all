package com.rpc.netty.service.event;

/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 14:55 2022/4/3
 * @Modified By: 事件标识接口
 */
public interface Event {

    /**
     * 这个 key 必须与监器的一致
     * @return
     */
    String getKey();
}
