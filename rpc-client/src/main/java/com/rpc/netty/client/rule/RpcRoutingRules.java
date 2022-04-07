package com.rpc.netty.client.rule;

import com.rpc.netty.client.core.RpcService;

/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 23:28 2022/4/6
 * @Modified By:
 */
public interface RpcRoutingRules {

    RpcService select(RpcService[] rpcServices,int serialNo);
}
