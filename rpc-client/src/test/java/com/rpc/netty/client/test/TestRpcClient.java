package com.rpc.netty.client.test;

import com.rpc.netty.client.RpcClient;
import com.rpc.netty.client.test.model.ResponseData;
import com.rpc.netty.core.response.RpcResponse;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 18:41 2022/4/17
 * @Modified By:
 */
public class TestRpcClient {

    @Test
    public void testRpcClient(){
        try {
            Properties properties = System.getProperties();
            properties.put("rpc.service.url", "127.0.0.1:9959");
            Map<String, String> params = new HashMap<>();
            params.put("key", "value");
            RpcResponse<ResponseData> request = RpcClient.request("/test/handler", null, ResponseData.class);
            System.err.println(request);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
