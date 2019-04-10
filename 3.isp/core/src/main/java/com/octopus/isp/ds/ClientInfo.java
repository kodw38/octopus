package com.octopus.isp.ds;

import com.octopus.isp.bridge.launchers.impl.pageframe.channel.ClientConst;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;

/**
 * User: wfgao_000
 * Date: 15-8-12
 * Time: 上午10:07
 */
public class ClientInfo extends HashMap {
    public static String CLIENT_IP="ClientIp";
    public static String CLIENT_OS="ClientOS";
    public static String CLIENT_BROWSER="ClientBrowser";
    public static String CLIENT_TERMINAL="ClientTerminal";
    public static String CLIENT_LANGUAGE="ClientLanguage";
    public static String CLIENT_COUNTRY="ClientCountry";
    private static Log log = LogFactory.getLog(ClientInfo.class);

    public void setClientKind(HttpServletRequest request) throws IOException {
        String user_agent=request.getHeader("user-agent");
        if(StringUtils.isBlank(user_agent))
            user_agent = request.getHeader("User-Agent");
        if(StringUtils.isBlank(user_agent))
            user_agent = request.getHeader("USER-AGENT");
        String kind= getClientKind(user_agent);
        put("kind",kind);

        put(CLIENT_LANGUAGE,request.getLocale().getLanguage());
        put(CLIENT_COUNTRY,request.getLocale().getCountry());
    }
    public String getClientIp(){return (String)get(CLIENT_IP);}
    public String getLanguage(){
        return (String)get(CLIENT_LANGUAGE);
    }
    public String getCountry(){
        return (String)get(CLIENT_COUNTRY);
    }
    public static String getClientKind(String str){
        log.debug("USER-AGENT:"+str);
        if(StringUtils.containsIgnoreCase(str, "PC")
                || StringUtils.containsIgnoreCase(str,"Windows NT")){
            return ClientConst.ClientKind.PC;
        }else if(StringUtils.containsIgnoreCase(str, "IPhone") || StringUtils.containsIgnoreCase(str, "Mobile")){
            return ClientConst.ClientKind.PHONE;
        }else if(StringUtils.containsIgnoreCase(str, "ipad") || StringUtils.containsIgnoreCase(str, "pod")){
            return ClientConst.ClientKind.PAD;
        }else{
            return ClientConst.ClientKind.PC;
        }
    }

    public String getIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("http_client_ip");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        // 如果是多级代理，那么取第一个ip为客户ip
        if (ip != null && ip.indexOf(",") != -1) {
            ip = ip.substring(ip.lastIndexOf(",") + 1, ip.length()).trim();
        }
        return ip;
    }

    public String getHeaderPath(){
        String kind = (String)get("kind");
        StringBuffer header =null;
        if( null != kind && kind.equals(ClientConst.ClientKind.PC)){
            header = new StringBuffer();
            header.append("/pc");
        }
        if( null != kind && kind.equals(ClientConst.ClientKind.PAD)){
            header = new StringBuffer();
            header.append("/pad");
        }
        if( null != kind && kind.equals(ClientConst.ClientKind.PHONE)){
            header = new StringBuffer();
            header.append("/phone");
        }

        if(null != header)
            return header.toString();
        return null;
    }

}
