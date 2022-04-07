package com.rpc.netty.client.response;

import com.rpc.netty.client.command.RpcCommand;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 返回的 Future
 */
public class ResponseFuture {

    private final int serialNo;
    private final long timeoutMillis;
    private final long beginTimestamp = System.currentTimeMillis();
    private final CountDownLatch countDownLatch = new CountDownLatch(1);

    private volatile RpcCommand response;
    private volatile boolean sendRequestOK = true;
    private volatile Throwable cause;

    public ResponseFuture(int serialNo, long timeoutMillis) {
        this.serialNo = serialNo;
        this.timeoutMillis = timeoutMillis;
    }

    public boolean isTimeout() {
        long diff = System.currentTimeMillis() - this.beginTimestamp;
        return diff > this.timeoutMillis;
    }

    public RpcCommand waitResponse(final long timeoutMillis) throws InterruptedException {
        this.countDownLatch.await(timeoutMillis, TimeUnit.MILLISECONDS);
        return this.response;
    }

    public void putResponse(final RpcCommand response) {
        this.response = response;
        this.countDownLatch.countDown();
    }

    public long getBeginTimestamp() {
        return beginTimestamp;
    }

    public boolean isSendRequestOK() {
        return sendRequestOK;
    }

    public void setSendRequestOK(boolean sendRequestOK) {
        this.sendRequestOK = sendRequestOK;
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public Throwable getCause() {
        return cause;
    }

    public void setCause(Throwable cause) {
        this.cause = cause;
    }

    public int getSerialNo() {
        return serialNo;
    }

    public RpcCommand getResponse() {
        return response;
    }

    public void setResponse(RpcCommand response) {
        this.response = response;
    }
}
