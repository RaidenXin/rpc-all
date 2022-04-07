package com.rpc.netty.client.utils;

import com.rpc.netty.client.utils.RpcHelper;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 10:35 2022/3/27
 * @Modified By:
 */
@Slf4j
public final class NettyUtils {

    public static final String OS_NAME = System.getProperty("os.name");

    private static boolean isLinuxPlatform = false;
    private static boolean isWindowsPlatform = false;

    static {
        if (OS_NAME != null && OS_NAME.toLowerCase().contains("linux")) {
            isLinuxPlatform = true;
        }

        if (OS_NAME != null && OS_NAME.toLowerCase().contains("windows")) {
            isWindowsPlatform = true;
        }
    }

    public static boolean isLinuxPlatform() {
        return isLinuxPlatform;
    }

    public static void closeChannel(Channel channel) {
        final String addrRemote = RpcHelper.parseChannelRemoteAddr(channel);
        channel.close().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                log.info("closeChannel: close the connection to remote address[{}] result: {}", addrRemote,
                        future.isSuccess());
            }
        });
    }
}
