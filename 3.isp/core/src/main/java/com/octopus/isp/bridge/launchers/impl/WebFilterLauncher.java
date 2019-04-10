package com.octopus.isp.bridge.launchers.impl;

import com.octopus.isp.bridge.IBridge;
import com.octopus.isp.bridge.ILauncher;
import com.octopus.isp.bridge.launchers.IConvert;
import com.octopus.isp.ds.ClientInfo;
import com.octopus.isp.ds.Env;
import com.octopus.isp.ds.RequestParameters;
import com.octopus.isp.ds.Session;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;

/**
 * User: Administrator
 * Date: 14-9-29
 * Time: 下午1:30
 */
public class WebFilterLauncher extends XMLObject implements ILauncher{
    IConvert inputconvert;
    IConvert outputconvert;
    Env env;
    public WebFilterLauncher(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    @Override
    public void notifyObject(String op, Object obj) throws Exception {

    }

    @Override
    public void addTrigger(Object cond, InvokeTaskByObjName task) throws Exception {

    }

    @Override
    public void destroy() throws Exception {

    }

    @Override
    public void initial() throws Exception {

    }

    public boolean addEnv(String key,Object value){
        env.addParameter(key,value);
        return true;
    }

    @Override
    public void start() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object invoke(Object obj) throws Exception {
        HttpServletRequest request = (HttpServletRequest)((Object[])obj)[0];
        HttpServletResponse response = (HttpServletResponse)((Object[])obj)[1];

        if(null!=inputconvert)
            obj = inputconvert.convert(obj);
        RequestParameters pars = new RequestParameters();
        pars.setEnv(env);
        pars.setRequestData(obj);
        pars.addRequestProperties("ContextPath", request.getContextPath());
        pars.addRequestProperties("Method", request.getMethod());
        pars.addRequestProperties("Scheme", request.getScheme());
        pars.addRequestProperties("ServerName",request.getServerName());
        pars.addRequestProperties("ServerPort",request.getServerPort());
        pars.setQueryString(request.getQueryString());
        pars.setRequestURL(request.getRequestURI());
        Session session = new Session();
        Enumeration ss = request.getSession().getAttributeNames();
        while(ss.hasMoreElements()){
            String k = (String)ss.nextElement();
            Object v = request.getSession().getAttribute(k);
            session.put(k, v);
        }
        session.setSessionId(request.getSession().getId());
        session.setCreateDate(request.getSession().getCreationTime());
        session.setLastAccessDate(request.getSession().getLastAccessedTime());
        pars.setSession(session);
        pars.addParameter("${request}", request);
        pars.addParameter("${response}",response);
        getHeaders(request, pars.getRequestHeaders());
        pars.addClientInfo("ClientIp",getIp(request));
        String[] osbt=getOSBrowserTerminal(pars.getHeader("user-agent"));
        pars.getClientInfo().setClientKind(request);
        if(null != osbt && osbt.length>2){

            pars.addClientInfo(ClientInfo.CLIENT_IP,getIp(request));
            pars.addClientInfo(ClientInfo.CLIENT_OS,osbt[0]);
            pars.addClientInfo(ClientInfo.CLIENT_BROWSER,osbt[1]);
            pars.addClientInfo(ClientInfo.CLIENT_TERMINAL,osbt[2]);
        }
        pars.setQueryStringMap(getQueryStringMap(pars.getQueryString()));

        Object ret = ((IBridge)getPropertyObject("bridge")).evaluate(pars);
        if(null != ret){
            return outputconvert.convert(ret);
        }
        return ret;
    }
    private void getHeaders(HttpServletRequest paramHttpServletRequest, Hashtable paramHashtable)
    {
        Enumeration localEnumeration1 = paramHttpServletRequest.getHeaderNames();
        while (localEnumeration1.hasMoreElements())
        {
            String str1 = (String)localEnumeration1.nextElement();
            if(null != str1){
                String str2 = "";
                Enumeration localEnumeration2 = paramHttpServletRequest.getHeaders(str1);
                while (localEnumeration2.hasMoreElements())
                {
                    if (str2.length() > 0)
                        str2 = str2 + ";";
                    str2 = str2 + ((String)localEnumeration2.nextElement());
                }
                paramHashtable.put(str1, str2);
            }
        }
    }
    private HashMap getQueryStringMap(String queryStr) throws UnsupportedEncodingException {
        if(null != queryStr){
            queryStr = URLDecoder.decode(queryStr, "UTF-8");
            HashMap map = new HashMap();
            String[] ps = queryStr.split("\\&");
            for(int i=ps.length-1;i>=0;i--){
                if(StringUtils.isNotBlank(ps[i])){
                    String[] kv = ps[i].split("\\=");
                    if(kv.length==2){
                        if(StringUtils.isNotBlank(kv[0]) && StringUtils.isNotBlank(kv[1]) && !map.containsKey(kv[0]))
                            map.put(kv[0], kv[1]);
                    }
                }
            }
            return map;
        }
        return null;
    }

    public String getIp(HttpServletRequest request) {
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

    public String[] getOSBrowserTerminal(String user_agent){
        String info = user_agent.toUpperCase();
        if(info.indexOf("(")==-1){// 针对火狐浏览器 swfupload控件 上传文件请求
            return null;
        }
        String[] ret = new String[3];
        String infoTemp=info.substring(info.indexOf("(") + 1, info.indexOf(")") - 1);
        String[] strInfo = infoTemp.split(";");
        if ((info.indexOf("MSIE")) > -1) {
            ret[1]=(strInfo[1].trim());
            ret[0]=(strInfo[2].trim());
        } else {
            String[] str = info.split(" ");
            if (info.indexOf("NAVIGATOR") < 0 && info.indexOf("FIREFOX") > -1) {
                ret[1]=(str[str.length - 1].trim());
                ret[0]=(strInfo[0].trim());
            } else if ((info.indexOf("OPERA")) > -1) {
                ret[1]=(str[0].trim());
                ret[0]=(strInfo[0].trim());
            } else if (info.indexOf("CHROME") < 0 && info.indexOf("SAFARI") > -1) {
                ret[1]=(str[str.length - 1].trim());
                ret[0]=(strInfo[2].trim());
            } else if (info.indexOf("CHROME") > -1) {
                ret[1]=(str[str.length - 2].trim());
//Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.116 Safari/537.36
                ret[0]=(infoTemp);
            } else if (info.indexOf("NAVIGATOR") > -1) {
                ret[1]=(str[str.length - 1].trim());
                ret[0]=(strInfo[2].trim());
            } else {
                ret[1]=("Unknown Browser");
                ret[0]=("Unknown OS");
            }
        }
        if(StringUtils.containsIgnoreCase(user_agent, "PC")
                || StringUtils.containsIgnoreCase(user_agent,"Windows NT")){
            ret[2]="PC";
        }else if(StringUtils.containsIgnoreCase(user_agent, "IPhone") || StringUtils.containsIgnoreCase(user_agent, "Mobile")){
            ret[2]="PHONE";
        }else if(StringUtils.containsIgnoreCase(user_agent, "ipad") || StringUtils.containsIgnoreCase(user_agent, "pod")){
            ret[2]="PAD";
        }else{
            ret[2]="PC";
        }
        return ret;
    }

}
