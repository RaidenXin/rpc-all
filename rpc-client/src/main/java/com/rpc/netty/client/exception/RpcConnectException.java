package com.rpc.netty.client.exception;

public class RpcConnectException extends RpcException {
    private static final long serialVersionUID = -5565366231695911316L;

    public RpcConnectException(String addr) {
        this(addr, null);
    }

    public RpcConnectException(String addr, Throwable cause) {
        super("connect to " + addr + " failed", cause);
    }
}
