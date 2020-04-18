package cn.nike8.httpserver.core;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by wenfan on 2020/4/13 21:21
 */
public class ServerThreadFactory implements ThreadFactory {


    private final AtomicInteger sequence = new AtomicInteger(1);

    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r,"http-"+sequence.getAndIncrement());
        return thread;
    }
}
