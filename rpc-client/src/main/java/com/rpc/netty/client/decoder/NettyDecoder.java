package com.rpc.netty.client.decoder;

import com.rpc.netty.client.utils.NettyUtils;
import com.rpc.netty.client.command.RpcCommand;
import com.rpc.netty.client.utils.RpcHelper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;

@Slf4j
public class NettyDecoder extends LengthFieldBasedFrameDecoder {

    private static final int FRAME_MAX_LENGTH = Integer.parseInt(System.getProperty("arch.mq.rpc.frameMaxLength", "16777216"));

    public NettyDecoder() {
        super(FRAME_MAX_LENGTH, 0, 4, 0, 4);
    }

    @Override
    public Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = null;
        try {
            frame = (ByteBuf) super.decode(ctx, in);
            if (null == frame) {
                return null;
            }

            ByteBuffer byteBuffer = frame.nioBuffer();

            return RpcCommand.decode(byteBuffer);
        } catch (Exception e) {
            log.error("decode exception, " + RpcHelper.parseChannelRemoteAddr(ctx.channel()), e);
            NettyUtils.closeChannel(ctx.channel());
        } finally {
            if (null != frame) {
                frame.release();
            }
        }
        return null;
    }
}
