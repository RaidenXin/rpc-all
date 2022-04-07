package com.rpc.netty.client.exception;

/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 15:54 2022/3/27
 * @Modified By:
 */
public class RpcSendRequestException extends RpcException{

    private static final long serialVersionUID = 5391285827332482364L;

    public RpcSendRequestException(String addr) {
        this(addr, null);
    }

    public RpcSendRequestException(String addr, Throwable cause) {
        super("send request to <" + addr + "> failed", cause);
    }
}
