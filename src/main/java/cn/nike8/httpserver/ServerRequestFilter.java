package cn.nike8.httpserver;

/**
 * Created by wenfan on 2020/4/14 9:12
 */
public interface ServerRequestFilter {

    boolean filter(ServerContext context,ServerRequest request,boolean beforeOrAfter);

}
