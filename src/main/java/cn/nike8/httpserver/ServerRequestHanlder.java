package cn.nike8.httpserver;

/**
 * Created by wenfan on 2020/4/15 15:53
 */
@FunctionalInterface
public interface ServerRequestHanlder {

    void handle(ServerContext context,ServerRequest request);

}
