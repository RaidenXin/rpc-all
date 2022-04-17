package com.rpc.netty.service.test;

import com.rpc.netty.client.command.RpcCommand;
import com.rpc.netty.service.annotation.RpcRequestMapping;
import com.rpc.netty.service.handler.ServerWorkHandler;
import com.rpc.netty.service.test.model.ResponseData;
import io.netty.channel.ChannelHandlerContext;

/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 19:00 2022/4/17
 * @Modified By:
 */
@RpcRequestMapping(path = "test")
public class TestServerWorkHandler implements ServerWorkHandler {

    @RpcRequestMapping(path = "handler")
    public ResponseData handler(){
        ResponseData response = new ResponseData(1, "张三");
        return response;
    }
}
