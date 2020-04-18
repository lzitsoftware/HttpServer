package cn.nike8.httpserver;

import cn.nike8.httpserver.core.RequestDispatcher;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by wenfan on 2020/4/15 15:48
 */
public class ServerRequestDispatcher implements RequestDispatcher {

    private static final Logger log = LoggerFactory.getLogger(ServerRequestDispatcher.class);

    private String contextRoot;
    private Router router;
    private Map<Integer,ServerExceptionHandler> exceptionHandler = new HashMap<>();
    private ServerExceptionHandler defaultExceptionHandler = new DefaultServerExceptionHandler();

    static class DefaultServerExceptionHandler implements ServerExceptionHandler{

        @Override
        public void handle(ServerContext context, ServerException excetption) {
            if (excetption.getStatus().code() == 500){
                log.error("Internal Server Error :" + excetption.getMessage());
            }
            context.error(excetption.getMessage(),500);
        }
    }

    public ServerRequestDispatcher(String contextRoot,Router router){
        this.contextRoot = contextRoot;
        this.router = router;
    }

    public ServerRequestDispatcher(Router router){
        this("/",router);
    }





    @Override
    public void dispatcher(ChannelHandlerContext channelHandlerContext, FullHttpRequest request) {
        ServerContext serverContext = new ServerContext(channelHandlerContext,contextRoot);
        try{
            handleImpl(serverContext,new ServerRequest(request));
        }catch (ServerException e){
            this.handleException(serverContext,e);
        }catch (Exception e){
            this.handleException(serverContext,new ServerException(HttpResponseStatus.INTERNAL_SERVER_ERROR,e));
        }finally {
            request.release();
        }
    }



    public ServerRequestDispatcher exception(int code,ServerExceptionHandler exceptionHandler){
        this.exceptionHandler.put(code,exceptionHandler);
        return this;
    }

    public ServerRequestDispatcher exception(ServerExceptionHandler exceptionHandler){
        this.defaultExceptionHandler = exceptionHandler;
        return this;
    }

    private void handleImpl(ServerContext context,ServerRequest request) throws Exception{
        if (request.decoderResult().isFailure()){
            context.interrupt("HTTP proocol decode failed !",400);
        }
        if (request.relativeUri().contains("./") || request.relativeUri().contains(".\\"))
            context.interrupt("unsecure url not allowed!",400);
        if (!request.relativeUri().startsWith(contextRoot)){
            throw new ServerException(HttpResponseStatus.NOT_FOUND);
        }
        request.popRootUri(contextRoot);
        router.handler(context,request);
    }

    private void handleException(ServerContext context,ServerException e){
        ServerExceptionHandler handler = this.exceptionHandler.getOrDefault(e.getStatus().code(),defaultExceptionHandler);
        try{
            handler.handle(context,e);
        }catch (Exception e1){
            this.defaultExceptionHandler.handle(context,new ServerException(HttpResponseStatus.INTERNAL_SERVER_ERROR,e1));
        }
    }



}
