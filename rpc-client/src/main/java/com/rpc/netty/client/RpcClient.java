package com.rpc.netty.client;

import com.rpc.netty.client.command.RpcCommand;
import com.rpc.netty.client.core.RpcService;
import com.rpc.netty.client.factory.RpcRoutingRulesFactory;
import com.rpc.netty.client.factory.RpcServiceFactory;
import com.rpc.netty.client.rule.PollingRules;
import com.rpc.netty.client.rule.RpcRoutingRules;
import com.rpc.netty.client.utils.SerializationUtil;
import com.rpc.netty.client.utils.StringUtils;
import com.rpc.netty.service.response.RpcResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 13:13 2022/4/5
 * @Modified By:
 */
@Slf4j
public final class RpcClient {


    private static AtomicInteger atomicLong;
    private static RpcService[] serviceInfos;
    private static NettyClient nettyClient;
    private static RpcRoutingRules rules;

    private RpcClient(){}

    static {
        init();
    }


    private static void init(){
        final String urls = System.getProperty("rpc.service.url");
        if (StringUtils.isBlank(urls)){
            throw new IllegalArgumentException("The {rpc.service.url} configuration cannot be empty！");
        }
        serviceInfos = RpcServiceFactory.newInstance(urls);
        nettyClient = new NettyClient();
        atomicLong = new AtomicInteger(0);
        String rulesClassName = System.getProperty("rpc.service.rules");
        if (StringUtils.isBlank(rulesClassName)){
            rules = new PollingRules();
        }else {
            RpcRoutingRulesFactory factory = RpcRoutingRulesFactory.createFactory();
            List<RpcRoutingRules> rpcRoutingRules = factory.getBeans();
            if (rpcRoutingRules != null){
                rules = rpcRoutingRules.stream().filter(rules -> rulesClassName.equals(rules.getClass().getName())).findFirst().orElse(new PollingRules());
            }
        }
    }



    public static <T> RpcResponse<T> request(Object params, Class<T> clazz){
        int serialNo = atomicLong.addAndGet(1);
        RpcCommand request = new RpcCommand();
        request.setSerialNo(serialNo);
        request.setBody(SerializationUtil.encode(params));
        try {
            RpcService service = rules.select(serviceInfos, serialNo);
            final RpcCommand response = nettyClient.invokeSync(service.getAddr(), request, NettyClient.LONG_POLL_TIMEOUT * 1000 * 2);
            RpcResponse<T> rpcResponse = SerializationUtil.decodeResponse(response.getBody(), clazz);
            if (rpcResponse.success()){
                log.info("返回消息 Response：{} SerialNo：{}", rpcResponse.toString(), serialNo);
                return rpcResponse;
            }else {
                log.error("返回消息 SerialNo：{} Code：{}，Message：{}", serialNo, rpcResponse.getCode(), rpcResponse.getMessage());
                return null;
            }
        } catch (Exception e) {
            log.error("Error obtaining MQ information ! SerialNo：{}", serialNo,e);
            return null;
        }
    }
}
