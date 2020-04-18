package cn.nike8.httpserver;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;

import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by wenfan on 2020/4/14 9:24
 */
public class ServerContext {

    private ChannelHandlerContext ctx;
    private String contextRoot;
    private Set<Cookie> cookies = new HashSet<>();


    public ServerContext(ChannelHandlerContext ctx,String contextRoot){
        this.ctx = ctx;
        this.contextRoot = contextRoot;
    }

    public ServerContext addCookie(String name,String value,String domain,String path,
                                   long maxAge,boolean httpOnly,boolean isSecure){
        Cookie cookie = null;
        for (Cookie ck:cookies){
            if (ck.name().equals(name)){
                cookie = ck;
                break;
            }
        }
        if (cookie == null){
            cookie = new DefaultCookie(name,value);
            if (domain != null)
                cookie.setDomain(domain);
            if (path != null)
                cookie.setPath(path);
            if (cookie.maxAge() >= 0)
                cookie.setMaxAge(maxAge);
            cookie.setHttpOnly(httpOnly);
            cookie.setSecure(isSecure);
            cookies.add(cookie);
        }
        return this;
    }

    public ServerContext addCookie(String name,String value){
        return this.addCookie(name,value,null,this.contextRoot,-1,false,false);
    }


    /**
     * 重定向和写cookie 返回给客户端
     * @param location
     * @param withinContext   是否在本服务中
     */
    public void redirect(String location,boolean withinContext){
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND);
        if (location.startsWith("/"))
            location = withinContext ? Paths.get(contextRoot,location).toString() : location;
        response.headers().add(HttpHeaderNames.LOCATION,location);
        for(Cookie cookie :cookies){
            response.headers().add(HttpHeaderNames.SET_COOKIE,ServerCookieEncoder.LAX.encode(cookie));
        }
        cookies.clear();
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }


    /**
     * 向浏览器写入
     * @param content
     * @param contentType
     * @param statusCode
     */
    public void text(String content,String contentType,int statusCode){
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer();
        byte[] bytes = content.getBytes(Charset.forName("utf8"));
        byteBuf.writeBytes(bytes);
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.valueOf(statusCode),byteBuf);
        response.headers().add(HttpHeaderNames.CONTENT_TYPE,String.format("%s; charset=utf-8",contentType));
        response.headers().add(HttpHeaderNames.CONTENT_LENGTH,bytes.length);
        for(Cookie cookie :cookies){
            response.headers().add(HttpHeaderNames.SET_COOKIE,ServerCookieEncoder.LAX.encode(cookie));
        }
        ctx.writeAndFlush(response);
        cookies.clear();
    }


    public void error(String errorMessage,int statusCode){
        error(errorMessage,"text/plain",statusCode);
    }

    public void error(String errorMessage,String contentType,int statusCode){
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer();
        byte[] bytes = errorMessage.getBytes(Charset.forName("utf8"));
        byteBuf.writeBytes(bytes);
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.valueOf(statusCode),byteBuf);
        response.headers().add(HttpHeaderNames.CONTENT_TYPE,String.format("%s; charset=utf-8",contentType));
        response.headers().add(HttpHeaderNames.CONTENT_LENGTH,bytes.length);
        for(Cookie cookie :cookies){
            response.headers().add(HttpHeaderNames.SET_COOKIE,ServerCookieEncoder.LAX.encode(cookie));
        }
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        cookies.clear();
    }

    public void json(Object o,int statusCode){
        text(JSON.toJSONString(o),"application/json",statusCode);
    }

    public void json(Object o){
        json(o,200);
    }

    public void interrupt(String content,int code){
        throw new ServerException(content,code);
    }





}
