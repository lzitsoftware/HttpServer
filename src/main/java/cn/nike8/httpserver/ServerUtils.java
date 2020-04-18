package cn.nike8.httpserver;

import java.nio.charset.Charset;

/**
 * Created by wenfan on 2020/4/14 18:23
 */
public class ServerUtils {

    public final static Charset UTF8 = Charset.forName("utf8");

    public static String normalize(String uri){
        uri = uri.trim();
        while (uri.startsWith("/"))
            uri = uri.substring(1);
        while(uri.endsWith("/"))
            uri = uri.substring(0,uri.length()-1);
        //uri添加/
        if (!uri.startsWith("/"))
            uri = "/"+uri;
        return uri;
    }

}
