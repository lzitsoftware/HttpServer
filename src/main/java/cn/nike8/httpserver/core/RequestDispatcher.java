package cn.nike8.httpserver.core;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 * Created by wenfan on 2020/4/13 21:09
 */
public interface RequestDispatcher {

    void dispatcher(ChannelHandlerContext context, FullHttpRequest request);

}
