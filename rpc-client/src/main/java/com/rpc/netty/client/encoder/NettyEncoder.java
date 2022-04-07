package com.rpc.netty.client.encoder;

import com.rpc.netty.client.utils.NettyUtils;
import com.rpc.netty.client.command.RpcCommand;
import com.rpc.netty.client.utils.RpcHelper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;


@ChannelHandler.Sharable
@Slf4j
public class NettyEncoder extends MessageToByteEncoder<RpcCommand> {

    @Override
    public void encode(ChannelHandlerContext ctx, RpcCommand rpcCommand, ByteBuf out)
        throws Exception {
        try {
            ByteBuffer header = rpcCommand.encodeHeader();
            out.writeBytes(header);
            byte[] bodys = rpcCommand.getBody();
            if (bodys != null) {
                out.writeBytes(bodys);
            }
        } catch (Exception e) {
            log.error("encode exception, " + RpcHelper.parseChannelRemoteAddr(ctx.channel()), e);
            if (rpcCommand != null) {
                log.error(rpcCommand.toString());
            }
            NettyUtils.closeChannel(ctx.channel());
        }
    }
}
