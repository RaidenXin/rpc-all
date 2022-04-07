package com.rpc.netty.client.rule;

import com.rpc.netty.client.core.RpcService;

/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 23:30 2022/4/6
 * @Modified By: 轮询规则
 */
public class PollingRules implements RpcRoutingRules{

    @Override
    public RpcService select(RpcService[] rpcServices, int serialNo) {
        final int currentIndex = serialNo % rpcServices.length;
        RpcService serviceInfo = rpcServices[currentIndex];
        return serviceInfo;
    }
}
