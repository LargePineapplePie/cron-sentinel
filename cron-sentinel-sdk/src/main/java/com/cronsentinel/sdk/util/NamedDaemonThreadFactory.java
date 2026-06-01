package com.cronsentinel.sdk.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 创建带名称的守护线程（daemon），保证不会阻止用户应用 JVM 正常退出。
 */
public final class NamedDaemonThreadFactory implements ThreadFactory {

    private final String prefix;
    private final AtomicInteger counter = new AtomicInteger(1);

    public NamedDaemonThreadFactory(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, prefix + "-" + counter.getAndIncrement());
        t.setDaemon(true);
        // 低于普通优先级，尽量不和业务线程争抢
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    }
}
