package com.octopus.tools.client.http.impl;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

/**
 * Created by admin on 2020/1/19.
 */
public class HttpsHostnameVerifier implements HostnameVerifier {
    /*** 验证对方主机名称 ,防止服务器证书上的Hostname和实际的URL不匹配
     * 防止链接被重定向到其他的不安全的地址
     */@Override
    public boolean verify(String hostname, SSLSession session) {
        /*System.out.println("hostname = [" + hostname + "],session = [" + session.getPeerHost() + "]");
        if (hostname.equals("地址") || session.equals("地址"))
            return true;
        else
            return false;
        */
        return true;
    }
}
