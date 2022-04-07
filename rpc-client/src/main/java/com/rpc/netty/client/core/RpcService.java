package com.rpc.netty.client.core;

import com.rpc.netty.client.utils.StringUtils;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class RpcService {

    private String host;
    private int port;
    private String addr;

    public RpcService(String host, int port){
        this.host = host;
        this.port = port;
        this.addr = new StringBuilder(this.host).append(":").append(this.port).toString();
    }

    public String getAddr(){
        if (this.addr == null){
            this.addr = new StringBuilder(this.host).append(":").append(this.port).toString();
        }
        return addr;
    }

    public SocketAddress getSocketAddress() {
        if (StringUtils.isBlank(host)){
            throw new IllegalArgumentException("MQ service hsot is null");
        }
        return new InetSocketAddress(host, port);
    }
}
