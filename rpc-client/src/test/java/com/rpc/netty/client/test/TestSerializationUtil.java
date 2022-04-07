package com.rpc.netty.client.test;

import com.rpc.netty.client.test.model.ResponseData;
import com.rpc.netty.client.utils.SerializationUtil;
import com.rpc.netty.service.response.RpcResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 21:44 2022/4/7
 * @Modified By:
 */
@Slf4j
public class TestSerializationUtil {

    @Test
    public void testEncodeResponseAndDecodeResponse(){
        ResponseData data = new ResponseData(1, "张三");
        RpcResponse<ResponseData> response = RpcResponse.success(data);
        response.setMessage("恭喜您查询成功");
        log.info("开始状态: {}", response.toString());
        log.info("开始编码");
        byte[] bytes = SerializationUtil.encodeResponse(response);
        log.info("开始解码");
        response = SerializationUtil.decodeResponse(bytes, ResponseData.class);
        log.info("结束状态状态: {}", response.toString());
    }
}
