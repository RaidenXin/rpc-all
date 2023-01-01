package com.rpc.netty.client;

import com.rpc.netty.client.command.RpcCommand;
import com.rpc.netty.client.core.RpcService;
import com.rpc.netty.client.factory.RpcRoutingRulesFactory;
import com.rpc.netty.client.factory.RpcServiceFactory;
import com.rpc.netty.client.rule.PollingRules;
import com.rpc.netty.client.rule.RpcRoutingRules;
import com.rpc.netty.client.utils.SerializationUtil;
import com.rpc.netty.client.utils.StringUtils;
import com.rpc.netty.core.response.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 13:13 2022/4/5
 * @Modified By:
 */
public final class RpcClient {


    private static final String RPC_SERVICE_URL = "rpc.service.url";
    private static final String RPC_SERVICE_RULES = "rpc.service.rules";

    private static final Logger log = LoggerFactory.getLogger(RpcClient.class);
    private static AtomicInteger atomicLong;
    private static RpcService[] serviceInfos;
    private static NettyClient nettyClient;
    private static RpcRoutingRules rules;

    private static RpcClient client;

    private RpcClient(){
        final String urls = System.getProperty(RPC_SERVICE_URL);
        if (StringUtils.isBlank(urls)){
            throw new IllegalArgumentException("The {rpc.service.url} configuration cannot be empty！");
        }
        serviceInfos = RpcServiceFactory.newInstance(urls);
        nettyClient = new NettyClient();
        atomicLong = new AtomicInteger(0);
        RpcRoutingRulesFactory factory = RpcRoutingRulesFactory.createFactory();
        List<RpcRoutingRules> rpcRoutingRules = factory.getBeans();
        if (rpcRoutingRules != null && rpcRoutingRules.size() > 0){
            String rulesClassName = System.getProperty(RPC_SERVICE_RULES);
            rules = rpcRoutingRules.stream().filter(rules -> rules.getClass().getName().equals(rulesClassName)).findFirst().orElse(new PollingRules());
        }else {
            rules = new PollingRules();
        }
    }

    static {
        try {
            client = new RpcClient();
        }catch (Throwable e){
            log.error(e.getMessage(), e);
        }
    }

    public static <T> RpcResponse<T> request(String path, Object params, Class<T> clazz){
        return client.requestHandler(path, params, clazz);
    }



    private <T> RpcResponse<T> requestHandler(String path, Object params, Class<T> clazz){
        //请求流水号
        final int serialNo = atomicLong.addAndGet(1);
        RpcCommand request = RpcCommand.build(serialNo, path, SerializationUtil.encode(params));
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
