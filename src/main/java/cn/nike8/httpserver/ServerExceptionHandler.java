package cn.nike8.httpserver;

/**
 * Created by wenfan on 2020/4/15 15:56
 */
@FunctionalInterface
public interface ServerExceptionHandler {
    void handle(ServerContext context,ServerException excetption);
}
