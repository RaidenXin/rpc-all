package com.rpc.netty.service.core;

import com.rpc.netty.client.NettyClient;
import com.rpc.netty.service.listener.EventListener;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * 清理任务
 */
@Slf4j
public class CleanListenerTask implements Delayed {
    //任务开始时间
    private long startTime;
    private EventListener listener;

    private CleanListenerTask(EventListener listener, long startTime) {
        this.listener = listener;
        this.startTime = startTime;
    }

    public void run() {
        EventManager.removeEventListener(listener);
    }


    public static final CleanListenerTask build(EventListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener Can't be null !");
        }
        //这里的开始时间 是 轮询超时时间 减去 5 秒是为了防止超时
        long startTime = System.currentTimeMillis() + ((NettyClient.LONG_POLL_TIMEOUT - 5) * 1000);
        CleanListenerTask task = new CleanListenerTask(listener, startTime);
        if (log.isInfoEnabled()) {
            log.info("创建任务！" + task.toString());
        }
        return task;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert((startTime) - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        return Long.compare(this.getDelay(TimeUnit.MILLISECONDS), o.getDelay(TimeUnit.MILLISECONDS));
    }

    @Override
    public String toString() {
        return "CleanListenerTask{" +
                "startTime=" + startTime +
                ", listener=" + listener +
                '}';
    }
}
