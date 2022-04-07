package com.rpc.netty.service.handler;

import com.rpc.netty.client.command.RpcCommand;
import io.netty.channel.ChannelHandlerContext;

/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 14:12 2022/4/5
 * @Modified By:
 */
public interface ServerWorkHandler {

    void processRequestCommand(ChannelHandlerContext ctx, RpcCommand msg);
}
