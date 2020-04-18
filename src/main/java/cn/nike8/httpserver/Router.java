package cn.nike8.httpserver;

import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.*;

/**
 * Created by wenfan on 2020/4/15 16:00
 */
public class Router{
    private ServerRequestHanlder requestHanlder;
    private Map<String,ServerRequestHanlder> subHandlers = new HashMap<>();
    private Map<String,Map<String,ServerRequestHanlder>> subMethodHandlers = new HashMap<>();
    private Map<String,Router> subRouters = new HashMap<>();
    private List<ServerRequestFilter> filters = new ArrayList<>();
    private final static List<String> METHODS = Arrays.asList("get","post","head","put","delete","trace","options","patch","connect");

    public Router(ServerRequestHanlder requestHanlder){
        this.requestHanlder = requestHanlder;
    }



    public void handler(ServerContext context,ServerRequest request){
        for (ServerRequestFilter filter: this.filters)
            request.filter(filter);
        String prefix = request.peekUriPrefix();
        String method = request.method().toLowerCase();
        // 处理除去rootUri 后的请求
        Router router = subRouters.get(prefix);
        if (router != null){
            request.popUriPrefix();
            router.handler(context,request);
            return;
        }
        if (prefix.equals(request.relativeUri())){
            Map<String,ServerRequestHanlder> hanlders = subMethodHandlers.get(prefix);
            ServerRequestHanlder hanlder = null;
            if (hanlders != null)
                hanlder = hanlders.get(method);
            if (hanlder == null)
                hanlder = subHandlers.get(prefix);
            if (hanlder != null){
                handleImpl(hanlder, context, request);
                return;
            }
        }
        //无请求处理类时，选择初始化时默认的处理器
        if (this.requestHanlder != null) {
            this.handleImpl(requestHanlder, context, request);
            return;
         }
         // 找不到的处理路径的handler
         throw new ServerException(HttpResponseStatus.NOT_FOUND);

    }

    public Router handler(String path,ServerRequestHanlder requestHanlder){
        path = ServerUtils.normalize(path);
        if (path.indexOf("/") != path.lastIndexOf("/")){
            throw new IllegalArgumentException("Only / is allowed in the path");
        }
        this.subHandlers.put(path,requestHanlder);
        return this;
    }

    public Router handler(String path,String method,ServerRequestHanlder requestHanlder){
        path = ServerUtils.normalize(path);
        if (path.indexOf("/") != path.lastIndexOf("/")){
            throw new IllegalArgumentException("Only / is allowed in the path");
        }
        if (!METHODS.contains(method)){
            throw new IllegalArgumentException("Illegal HTTP method "+method);
        }
        Map<String,ServerRequestHanlder> hanlders = subMethodHandlers.get(path);
        if (hanlders == null){
            hanlders = new HashMap<>();
            subMethodHandlers.put(path,hanlders);
        }
        hanlders.put(path,requestHanlder);
        return this;
    }

    public Router child(String path,Router router){
        path = ServerUtils.normalize(path);
        if (path.equals("/"))
            throw new IllegalArgumentException("Child path shouldn't be /");
        if (path.indexOf("/") != path.lastIndexOf("/")){
            throw new IllegalArgumentException("Only / is allowed in the path");
        }
        this.subRouters.put(path,router);
        return this;
    }

    public Router child(String path,ServerRouteable routeable){
        this.child(path,routeable.route());
        return this;
    }


    /**
     * 添加过滤器
     * @param requestFilters
     * @return
     */
    public Router filter(ServerRequestFilter... requestFilters){
        for (ServerRequestFilter filter: requestFilters){
            this.filters.add(filter);
        }
        return this;
    }




    /**
     *  issue 必须要按照 先添加待处理路径前的过滤器，再添加待处理路径后的过滤器
     * @param hanlder
     * @param context
     * @param request
     */
    public void handleImpl(ServerRequestHanlder hanlder,ServerContext context,ServerRequest request){

        // 处理拦截器之前的请求
        for (ServerRequestFilter filter:filters)
            if (!filter.filter(context,request,true))
                return;

        // 处理请求
        hanlder.handle(context,request);

        // 处理拦截器之后的请求
        for (ServerRequestFilter filter:filters)
            if (!filter.filter(context,request,false))
                return;

    }


}
