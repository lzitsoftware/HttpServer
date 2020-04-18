package cn.nike8.httpserver;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.MixedAttribute;

import java.util.*;

/**
 * 由FullRequest包装请求信息
 *
 * Created by wenfan on 2020/4/14 17:20
 */
public class ServerRequest {

    private String relativeUri;
    private FullHttpRequest request;
    private Map<String,List<String>> headers;
    private Set<Cookie> cookies;
    private Map<String,MixedAttribute> files;
    private Map<String,List<String>> forms;
    private QueryStringDecoder queryDecoder;
    private HttpPostRequestDecoder postDecoder;
    private Map<String,Object> attributes;
    private List<ServerRequestFilter> filters;

    public ServerRequest(FullHttpRequest request){
        this.request = request;
        this.attributes = new HashMap<>();
        this.filters = new ArrayList<>();
        this.relativeUri = ServerUtils.normalize(path());
    }

    public String relativeUri(){
        return this.relativeUri;
    }

    private QueryStringDecoder urlDecoder(){
        if (queryDecoder == null)
            queryDecoder = new QueryStringDecoder(request.uri());
        return queryDecoder;
    }

    public Map<String,List<String>> allparams(){
        return queryDecoder.parameters();
    }

    public List<String> params(String name){
        /*for (String paramName:allparams().keySet()){
            if (paramName.equals(name)) {
                return allparams().get(paramName);
            }
        }
        return null;*/
        List<String> value = allparams().get(name);
        if (value != null)
            return value;
        else
            return Collections.emptyList();
    }


    /**
     * 根据name返回param的一个元素
     * @param name
     * @return
     */
    public String param(String name){
        List<String> values = this.params(name);
        return values.isEmpty() ? null: values.get(0);
    }

    private String path() {
        return urlDecoder().path();
    }

    public HttpPostRequestDecoder postDecoder(){
        if (this.postDecoder == null)
            this.postDecoder = new HttpPostRequestDecoder(request);
        return postDecoder;
    }


    public ServerRequest filter(ServerRequestFilter filter){
        this.filters.add(filter);
        return this;
    }

    public List<ServerRequestFilter> filters(){
        return this.filters;
    }


    /**
     * 返回两个//之间的路径
     * @return
     */
    public String peekUriPrefix(){
        if (this.relativeUri.equals("/"))
            return "/";
        int index = 1;
        while(index < this.relativeUri.length()){
            if (this.relativeUri.charAt(index) == '/')
                break;
            index++;
        }
        return this.relativeUri.substring(0,index);
    }

    /**
     * 弹出两个//之间的路径
     */
    public void popUriPrefix(){
        if (this.relativeUri.equals("/"))
            return ;
        int index = 1;
        while(index < this.relativeUri.length()){
            if (this.relativeUri.charAt(index) == '/')
                break;
            index++;
        }
        if (index == this.relativeUri.length())
            this.relativeUri = "/";
        else
            this.relativeUri = this.relativeUri.substring(index);
    }


    /**
     * 将路径中的root根目录去掉
     * @param root
     */
    public void popRootUri(String root){
        if(root.equals("/")){
            return;
        }
        if(this.relativeUri.equals(root)){
            this.relativeUri = "/";
            return;
        }
        this.relativeUri = this.relativeUri.substring(root.length());
    }

    public String method(){
        return request.method().name();
    }

    public ByteBuf content(){
        return request.content();
    }

    public FullHttpRequest request(){
        return request;
    }

    public DecoderResult decoderResult(){
        return request.decoderResult();
    }

    public Map<String,List<String>> allHeaders(){
        if (headers == null){
            headers = new HashMap<>();
            for (Map.Entry<String, String> head:request.headers()){
                headers.put(head.getKey(),request.headers().getAll(head.getKey()));
            }
        }
        return headers;
    }


    /**
     * 从request中获取cookies
     * @return
     */
    public Set<Cookie> cookies(){
        if (cookies == null){
            String cookie = request.headers().get(HttpHeaderNames.COOKIE);
            if (cookie == null){
                cookies = Collections.emptySet();
            }else{
                cookies = ServerCookieDecoder.LAX.decode(cookie);
            }
        }
        return cookies;
    }


    /**
     * 由cookie name 获取value
     * @param cookieName
     * @return
     */
    public String cookie(String cookieName){
        for (Cookie cookie:cookies){
            if (cookie.name().equalsIgnoreCase(cookieName)){
                return cookie.value();
            }
        }
        return null;
    }







}
