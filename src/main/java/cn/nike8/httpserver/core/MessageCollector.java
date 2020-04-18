package cn.nike8.httpserver.core;


import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

/**
 * Created by wenfan on 2020/4/13 21:12
 */
@ChannelHandler.Sharable
public class MessageCollector extends ChannelInboundHandlerAdapter {


    private final static Logger log = LoggerFactory.getLogger(MessageCollector.class);


    private ThreadPoolExecutor[] executors;
    private RequestDispatcher dispatcher;
    private int requestMaxInflight = 1000;
    private int corePoolSize = 1;
    private int maximunPoolSize = 1;
    private long keepalive = 30;

    public MessageCollector(int workerThreads,RequestDispatcher dispatcher){
        ThreadFactory threadFactory = new ServerThreadFactory();
        this.executors = new ThreadPoolExecutor[workerThreads];
        for (int i =0;i < workerThreads;i++ ){
            ArrayBlockingQueue queue = new ArrayBlockingQueue<Runnable>(requestMaxInflight);
            this.executors[i] = new ThreadPoolExecutor(corePoolSize,
                    maximunPoolSize,
                    keepalive,
                    TimeUnit.SECONDS,
                    queue,
                    threadFactory,
                    new ThreadPoolExecutor.CallerRunsPolicy());
        }
        this.dispatcher = dispatcher;

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("Connection has established between local and "+ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.info("Connection has closed between local and "+ctx.channel().remoteAddress());
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (msg instanceof FullHttpRequest){
            // 随机从线程池中选出一个线程来处理
            Checksum checksum = new CRC32();
            checksum.update(ctx.hashCode());
            int index =(int) (checksum.getValue() % executors.length);
            this.executors[index].execute(
                    new Runnable() {
                        @Override
                        public void run() {
                            dispatcher.dispatcher(ctx,(FullHttpRequest)msg);
                        }
                    }
            );
        }

    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }



    /**
     * 优雅的关闭线程池资源
     */
    public void close(){
        for (int i = 0; i< executors.length; i++){
            executors[i].shutdown();
        }
        for (int i = 0; i< executors.length; i++){
            try {
                executors[i].awaitTermination(10,TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            executors[i].shutdownNow();
        }

    }

}
