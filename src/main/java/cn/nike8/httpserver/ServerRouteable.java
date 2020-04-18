package cn.nike8.httpserver;

/**
 * Created by wenfan on 2020/4/15 15:55
 */
@FunctionalInterface
public interface ServerRouteable {

    Router route();
}
