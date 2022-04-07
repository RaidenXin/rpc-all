package com.rpc.netty.client.factory;

import com.rpc.netty.client.rule.RpcRoutingRules;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 23:36 2022/4/6
 * @Modified By:
 */
public class DefaultRpcRoutingRulesFactory implements RpcRoutingRulesFactory{
    @Override
    public List<RpcRoutingRules> getBeans() {
        ServiceLoader<RpcRoutingRules> routingRules = ServiceLoader.load(RpcRoutingRules.class);
        List<RpcRoutingRules> rules = new ArrayList<>();
        for (RpcRoutingRules workHandler : routingRules){
            rules.add(workHandler);
        }
        return rules;
    }
}
