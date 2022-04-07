package com.rpc.netty.client.exception;

/**
 * @创建人:Raiden
 * @Descriotion:
 * @Date:Created in 15:53 2022/3/27
 * @Modified By:
 */
public class RpcTimeoutException extends RpcException{

    private static final long serialVersionUID = 4106899185095281793L;

    public RpcTimeoutException(String message) {
        super(message);
    }

    public RpcTimeoutException(String addr, long timeoutMillis) {
        this(addr, timeoutMillis, null);
    }

    public RpcTimeoutException(String addr, long timeoutMillis, Throwable cause) {
        super("wait response on the channel <" + addr + "> timeout, " + timeoutMillis + "(ms)", cause);
    }
}
