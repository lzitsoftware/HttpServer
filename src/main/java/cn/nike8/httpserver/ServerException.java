package cn.nike8.httpserver;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Created by wenfan on 2020/4/14 16:55
 */
public class ServerException extends RuntimeException{

    private HttpResponseStatus status;
    private String message;

    public ServerException(String message, HttpResponseStatus status,Throwable t) {
        super(t);
        this.status = status;
        this.message = status.reasonPhrase();
        if (message != null)
            this.message = message;
    }

    public ServerException(HttpResponseStatus status,Throwable t){
        this(null,status,t);
    }

    public ServerException(String message,HttpResponseStatus status){
        this(message,status,null);
    }

    public ServerException(HttpResponseStatus status){
        this(null,status,null);
    }

    public ServerException(String message,int code){
        this(message,HttpResponseStatus.valueOf(code));
    }

    public HttpResponseStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
