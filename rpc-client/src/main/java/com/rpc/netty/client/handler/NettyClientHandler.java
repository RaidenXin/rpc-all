package com.rpc.netty.client.handler;

import com.rpc.netty.client.NettyClient;
import com.rpc.netty.client.command.RpcCommand;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 22:52 2020/10/17
 * @Modified By:
 */
@Slf4j
public class NettyClientHandler extends SimpleChannelInboundHandler<RpcCommand>  {

    private NettyClient nettyClient;

    public NettyClientHandler(NettyClient nettyClient){
        this.nettyClient = nettyClient;
    }
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcCommand msg) throws Exception {
        nettyClient.processResponseCommand(ctx, msg);
    }
}
