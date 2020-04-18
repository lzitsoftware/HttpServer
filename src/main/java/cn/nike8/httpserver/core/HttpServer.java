package cn.nike8.httpserver.core;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by wenfan on 2020/4/13 18:00
 */
public class HttpServer {

    private final static Logger log = LoggerFactory.getLogger(HttpServer.class);

    private String ip;
    private int port;
    private int ioThreads;
    private int workerThreads;
    private RequestDispatcher requestDispatcher;
    private ServerBootstrap bootstrap;
    private EventLoopGroup group;
    private Channel serverChannel;
    private MessageCollector messageCollector;

    public HttpServer(String ip,int port,int ioThreads,int workerThreads,RequestDispatcher dispatcher){
        this.ip = ip;
        this.port = port;
        this.ioThreads = ioThreads;
        this.workerThreads = workerThreads;
        this.requestDispatcher = dispatcher;
    }

    public void start(){
        bootstrap = new ServerBootstrap();
        group = new NioEventLoopGroup(ioThreads);
        bootstrap.group(group);
        messageCollector = new MessageCollector(workerThreads,requestDispatcher);
        bootstrap.channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel channel) throws Exception {
                ChannelPipeline pipeline = channel.pipeline();
                pipeline.addLast(new ReadTimeoutHandler(10));
                pipeline.addLast(new HttpServerCodec());
                // max_size = 1G
                pipeline.addLast(new HttpObjectAggregator(1 << 30));
                pipeline.addLast(new ChunkedWriteHandler());
                pipeline.addLast(messageCollector);
            }
        });
        // 当服务请求线程全满时，其他完成3次请求握手后的请求放入队列，队列大小默认50，
        bootstrap.option(ChannelOption.SO_BACKLOG,100)
                //关闭Nagle算法，即无论数据块多大，都要发送请求，不累计
                 .option(ChannelOption.TCP_NODELAY,true)
                 .option(ChannelOption.SO_REUSEADDR,true)
                 .childOption(ChannelOption.SO_KEEPALIVE,true);

        serverChannel = bootstrap.bind(this.ip,this.port).channel();
        log.info("Server {} has started at {}",this.ip,this.port);
    }


    public void stop(){
        // 先关闭服务端socket
        serverChannel.close();
        // 再斩断消息来源，停止io线程池
        group.shutdownGracefully();
        // 停止业务线程池
        messageCollector.close();
    }

}
