package cn.nike8.httpserver.start;

import cn.nike8.httpserver.*;
import cn.nike8.httpserver.core.HttpServer;

/**
 * Created by wenfan on 2020/4/16 15:50
 */
public class SimpleServer {

    public static void main(String[] args){
        Router router = new Router(new ServerRequestHanlder() {
            @Override
            public void handle(ServerContext context, ServerRequest request) {
                context.json("hello");
            }
        });
        router.handler("/hello", new ServerRequestHanlder() {
            @Override
            public void handle(ServerContext context, ServerRequest request) {
                context.json("helloworld");
            }
        }).handler("/redirectExternal", new ServerRequestHanlder() {
            @Override
            public void handle(ServerContext context, ServerRequest request) {
                context.redirect("http://www.baidu.com",false);
            }
        }).handler("/redirectInternal", new ServerRequestHanlder() {
            @Override
            public void handle(ServerContext context, ServerRequest request) {
                context.redirect("/hello",true);
            }
        }).handler("/error", new ServerRequestHanlder() {
            @Override
            public void handle(ServerContext context, ServerRequest request) {
                context.error("Custom Exception show!",500);
            }
            // 多级路由
        }).child("/child", new ServerRouteable() {
            @Override
            public Router route() {
                return new Router(new ServerRequestHanlder() {
                    @Override
                    public void handle(ServerContext context, ServerRequest request) {
                        context.json(new String []{"hello","childRouter"});
                    }
                }).child("/subChild", new ServerRouteable() {
                    @Override
                    public Router route() {
                        return new Router(new ServerRequestHanlder() {
                            @Override
                            public void handle(ServerContext context, ServerRequest request) {
                                context.json(new String[]{"hello","subChildRouter"});
                            }
                        });
                    }
                }).handler("/ping", new ServerRequestHanlder() {
                    @Override
                    public void handle(ServerContext context, ServerRequest request) {
                        System.out.println("handling....");
                        context.json("pong");
                    }
                }).filter(new ServerRequestFilter() {
                    @Override
                    public boolean filter(ServerContext context, ServerRequest request, boolean before) {
                        if (before){
                            System.out.printf("before %s\n do somothing...",request.relativeUri());
                        }else
                            System.out.printf("after %s\n do somothing...",request.relativeUri());
                        return true;
                    }
                });
            }
        });

        ServerRequestDispatcher dispatcher = new ServerRequestDispatcher("/server",router);
        dispatcher/*.exception(500,new ServerExceptionHandler() {
            @Override
            public void handle(ServerContext context, ServerException excetption) {
                context.json("mother fucker!");
            }
        })*/.exception(new ServerExceptionHandler() {
            @Override
            public void handle(ServerContext context, ServerException excetption) {
                context.json("what's fucking this?",500);
            }
        });

        HttpServer httpServer = new HttpServer("localhost",8080,30,200,dispatcher);
        httpServer.start();

        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                httpServer.stop();
            }
        });




    }

}
