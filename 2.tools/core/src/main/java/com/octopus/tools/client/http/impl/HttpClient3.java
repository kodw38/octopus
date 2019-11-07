package com.octopus.tools.client.http.impl;

import com.octopus.tools.client.http.HttpDS;
import com.octopus.tools.client.http.IHttpClient;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.exception.Logger;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import com.octopus.utils.xml.desc.Desc;
import io.netty.handler.codec.http.HttpResponse;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.mapreduce.HashTable;
import sun.misc.BASE64Encoder;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.util.*;

/**
 * User: Administrator
 * Date: 14-10-27
 * Time: 下午5:33
 */
public class HttpClient3 extends XMLDoObject implements IHttpClient {
    static transient Log log = LogFactory.getLog(HttpClient3.class);
    HashMap<String,IHttpParse> ps;
    String SAVE_RESPONSE_PREFIX="IF$_";
    MultiThreadedHttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();

    HttpClient client=null;
    public HttpClient3(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        HttpConnectionManagerParams pars = new HttpConnectionManagerParams();
        pars.setConnectionTimeout(2000);
        pars.setSoTimeout(2000);
        pars.setMaxTotalConnections(500);
        pars.setDefaultMaxConnectionsPerHost(500);
        pars.setStaleCheckingEnabled(true);
        /*HostConfiguration hc = new HostConfiguration();
        hc.setHost("http://hq.htsc.com.cn");
        pars.setMaxConnectionsPerHost(hc, 200);*/
        connectionManager.setParams(pars);
        HttpClientParams httpClientParams = new HttpClientParams();
        // 设置httpClient的连接超时，对连接管理器设置的连接超时是无用的
        httpClientParams.setConnectionManagerTimeout(5000); //等价于4.2.3中的CONN_MANAGER_TIMEOUT
        client = new HttpClient(connectionManager);
        client.setParams(httpClientParams);
        //另外设置http client的重试次数，默认是3次；当前是禁用掉（如果项目量不到，这个默认即可）
        httpClientParams.setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(0, false));
    }

    @Override
    public void httpInvoke(HttpDS parameters) throws Exception {
        try{

            //if(url.isRedirect())
            String proxyName=parameters.getProxyName();
            String proxyPwd=parameters.getProxyPwd();
            String proxyUrl=parameters.getProxyUrl();
            int proxyPort=parameters.getProxyPort();
            String[] removeRequestCookies=parameters.getRemoveRequestCookies();
            String[] saveResponseCookies=parameters.getSaveResponseCookies();
            int connectionTimeout=parameters.getConnectionTimeout(),readTimeout=parameters.getReadTimeout();
            Boolean redirect=parameters.isRedirect();
            String url=parameters.getUrl();
            if(connectionTimeout==0 && StringUtils.isNotBlank(getXML().getProperties().getProperty("connectiontimeout")))
                connectionTimeout=Integer.parseInt(getXML().getProperties().getProperty("connectiontimeout"));
            if(readTimeout==0 && StringUtils.isNotBlank(getXML().getProperties().getProperty("readtimeout")))
                readTimeout=Integer.parseInt(getXML().getProperties().getProperty("readtimeout"));
            if(redirect==null && StringUtils.isNotBlank(getXML().getProperties().getProperty("redirect")))
                redirect=Boolean.valueOf(getXML().getProperties().getProperty("redirect"));
            if(removeRequestCookies==null && StringUtils.isNotBlank(getXML().getProperties().getProperty("removerequestcookies")))
                removeRequestCookies=getXML().getProperties().getProperty("removerequestcookies").split(",");
            if(saveResponseCookies==null && StringUtils.isNotBlank(getXML().getProperties().getProperty("saveresponsecookies")))
                saveResponseCookies=getXML().getProperties().getProperty("saveresponsecookies").split(",");



            if(connectionTimeout>0)
                client.getHttpConnectionManager().getParams().setConnectionTimeout(connectionTimeout);
            if(readTimeout>0)
                client.getHttpConnectionManager().getParams().setSoTimeout(readTimeout);
            if(StringUtils.isNotBlank(proxyUrl))
                client.getHostConfiguration().setProxy(proxyUrl,proxyPort);

            Header[] headers=null;
            if(null != parameters.getRequestHeaders() && parameters.getRequestHeaders().size()>0){
                doRequestCookie(parameters.getRequestHeaders(),removeRequestCookies,saveResponseCookies);
                headers = getHeaderFromTable(parameters.getRequestHeaders());
            }
            if("GET".equalsIgnoreCase(parameters.getMethodName())){
                StringBuffer par = new StringBuffer();
                if(null != parameters.getProperties()) {

                    Iterator<String> its = parameters.getProperties().keySet().iterator();
                    while(its.hasNext()) {
                        String k = its.next();
                        if(par.length()>0){
                            par.append("&");
                        }
                        if(parameters.getProperties().get(k) instanceof String) {
                            par.append(k).append("=").append(parameters.getProperties().get(k));
                        }
                    }
                }
                if(par.length()>0) {
                    if (url.contains("?")) {
                        url += "&" + par.toString();
                    } else {
                        url += "?" + par.toString();
                    }
                }
                GetMethod httpget = new GetMethod(url);
                try{
                    httpget.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler());
                    if(null != headers){
                        for(Header h:headers)
                            httpget.addRequestHeader(h);
                    }

                    client.executeMethod(httpget);
                    Header[] hs = httpget.getResponseHeaders();
                    if(null != hs)
                        setHeaderToHttpServletResponse(hs, saveResponseCookies, parameters);
                    parameters.setStatusCode(httpget.getStatusCode());
                    if(null != httpget.getResponseBody()) {
                        parameters.getResponseOutputStream().write(httpget.getResponseBody());
                    }else{
                        if(log.isDebugEnabled()){
                            System.out.println("response body is null, url:"+url);
                        }
                    }
                }catch (Exception e){
                    throw new Exception(url,e);
                }finally {
                    httpget.releaseConnection();
                }


            }else if("POST".equals(parameters.getMethodName())){
                long l=0;
                if(log.isDebugEnabled())
                    l = System.currentTimeMillis();
                PostMethod httppost = new PostMethod(url);
                try{
                    httppost.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler());
                    if(null != headers)
                        for(Header h:headers){
                            if(log.isDebugEnabled()) {
                                log.debug("POST Header ["+h.getName()+":"+h.getValue()+"]");
                            }
                            if(!parameters.isSendCookie() && h.getName().equals("Cookie")) continue;
                            httppost.addRequestHeader(h);
                        }

                    if(null != parameters.getRequestInputStream()){
                        httppost.setRequestBody(parameters.getRequestInputStream());
                    }
                    if(null != parameters.getProperties()) {
                        Iterator<String> its = parameters.getProperties().keySet().iterator();
                        while(its.hasNext()) {
                            String k = its.next();
                            httppost.addParameter(k,parameters.getProperties().get(k));
                        }
                    }
                    if(Logger.isInfoEnabled()){
                        Logger.info(this.getClass(),null,"http","before http post",null);
                    }
                    client.executeMethod(httppost);

                    Header[] hs = httppost.getResponseHeaders();
                    if(null != hs) {
                        setHeaderToHttpServletResponse(hs, saveResponseCookies, parameters);
                    }
                    parameters.setStatusCode(httppost.getStatusCode());
                    if(null != httppost.getResponseBody()) {
                        parameters.getResponseOutputStream().write(httppost.getResponseBody());
                        if(log.isDebugEnabled()){
                            log.debug("url:"+url+" |response:"+new String(((ByteArrayOutputStream) parameters.getResponseOutputStream()).toByteArray(), "utf-8"));
                        }
                    }else{
                        if(log.isDebugEnabled()){
                            log.debug("response body is null, url:"+url);
                        }
                    }
                    if(Logger.isInfoEnabled()){
                        Logger.info(this.getClass(),null,"http","end http post",null);
                    }
                    if(log.isDebugEnabled()) {
                        log.debug("httpclient3 post lost:" + (System.currentTimeMillis() - l) + " ms");
                    }
                }catch (Exception ioe){
                    throw new Exception(url,ioe);
                }finally {
                    httppost.releaseConnection();
                }
            }else {
                throw new Exception("now not support the request method["+parameters.getMethodName()+"]");
            }
        }catch (Exception e){
            if(!(e instanceof ConnectException)){
                log.error("url:"+parameters.getUrl(), e);
            }
            throw e;
        }finally {

        }
    }

    /*void invoke(UrlDS url,HttpDS parameters)throws  Exception{
        try{
            HttpClient client = new HttpClient();
            //if(url.isRedirect())
            if(url.getConnectionTimeout()>0)
                client.getHttpConnectionManager().getParams().setConnectionTimeout(url.getConnectionTimeout());
            if(url.getReadTimeout()>0)
                client.getHttpConnectionManager().getParams().setSoTimeout(url.getReadTimeout());
            if(StringUtils.isNotBlank(url.getProxyUrl()))
                client.getHostConfiguration().setProxy(url.getProxyUrl(),url.getProxyPort());

            Header[] headers=null;
            if(null != parameters.getRequestHeaders() && parameters.getRequestHeaders().size()>0){
                doRequestCookie(parameters.getRequestHeaders(),url);
                headers = getHeaderFromTable(parameters.getRequestHeaders());
            }

            if("GET".equalsIgnoreCase(parameters.getMethodName())){
                GetMethod httpget = new GetMethod(url.getUrl());
                httpget.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler());
                if(null != headers){
                    for(Header h:headers)
                        httpget.addRequestHeader(h);
                }
                client.executeMethod(httpget);
                Header[] hs = httpget.getResponseHeaders();
                if(null != hs)
                    setHeaderToHttpServletResponse(hs, url, parameters);
                parameters.setStatusCode(httpget.getStatusCode());
                if(null != httpget.getResponseBody())
                    parameters.getResponseOutputStream().write(httpget.getResponseBody());

            }else if("POST".equals(parameters.getMethodName())){
                PostMethod httppost = new PostMethod(url.getUrl());
                httppost.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler());
                for(Header h:headers)
                    httppost.addRequestHeader(h);
                if(null != parameters.getRequestInputStream()){
                    httppost.setRequestBody(parameters.getRequestInputStream());
                }
                client.executeMethod(httppost);
                Header[] hs = httppost.getResponseHeaders();
                if(null != hs)
                    setHeaderToHttpServletResponse(hs, url, parameters);
                parameters.setStatusCode(httppost.getStatusCode());
                if(null != httppost.getResponseBody())
                    parameters.getResponseOutputStream().write(httppost.getResponseBody());
            }else {
                throw new Exception("now not support the request method["+parameters.getMethodName()+"]");
            }

        }catch (Exception e){
        }
    }
*/
    void setHeaderToHttpServletResponse(Header[] hs,String[] saveResponseCookies,HttpDS httpDS){
        if(null != hs){
            Hashtable<String,String> rhs = new Hashtable<String, String>();
            for(Header h:hs){
                if(h.getName().equals("Set-Cookie")){
                    String v = doResponseCookie(h.getValue(),saveResponseCookies);
                    if(rhs.containsKey("Set-Cookie")){
                        v =rhs.get("Set-Cookie") +";"+v;
                    }
                    rhs.put("Set-Cookie",v);
                }else
                    rhs.put(h.getName(),h.getValue());
                if(log.isDebugEnabled()){
                    log.debug("response header:"+h.toString());
                }
            }
            httpDS.setResponseHeaders(rhs);
        }
    }

    String doResponseCookie(String cookie,String[] saveResponseCookies){
        if(StringUtils.isNotBlank(cookie)){
            StringBuffer sb = new StringBuffer();

            //设置代理SessionID
            Map<String,String> cookieMap = new LinkedHashMap<String, String>();
            String[] ss = cookie.split(";");
            for(String s:ss){
                int n=s.indexOf("=");
                if(n>0){
                    String k = s.substring(0,n).trim();
                    String v = s.substring(n+1);
                    cookieMap.put(k,v);
                }else{
                    cookieMap.put(s,null);
                }
            }
            if(null != saveResponseCookies){
                for(String s:saveResponseCookies){
                    if(cookieMap.containsKey(s)){
                        String v = cookieMap.get(s);
                        cookieMap.remove(s);
                        cookieMap.put(SAVE_RESPONSE_PREFIX+s,v);
                    }
                }
            }

            Iterator its = cookieMap.keySet().iterator();
            while(its.hasNext()){
                String k = (String)its.next();
                String v = cookieMap.get(k);
                if(sb.length()>0)sb.append(";");
                if(null != v)
                    sb.append(" ").append(k).append("=").append(v);
                else
                    sb.append(" ").append(k);
            }
            return sb.toString();
        }
        return "";

    }

    void doRequestCookie(Hashtable<String,String> headHashtable,String[] removeRequestCookies, String[] saveResponseCookies){
        //reset cookie
        String cookieName = null;
        if(headHashtable.containsKey("cookie")){
            cookieName="cookie";
        }
        if(headHashtable.containsKey("Cookie")){
            cookieName="Cookie";
        }
        //log.error("proxyRequestCookie:"+headHashtable.get(cookieName));
        if(StringUtils.isNotBlank(cookieName)){
            String cookieValue = headHashtable.get(cookieName);
            String[] ss = cookieValue.split(";");
            LinkedHashMap<String,String> cookieMap = new LinkedHashMap();
            for(String s:ss){
                if(s.contains("=")){
                    int n=s.indexOf("=");
                    if(n>0){
                        String k = s.substring(0,n).trim();
                        String v = s.substring(n+1);
                        cookieMap.put(k,v);
                    }else{
                        cookieMap.put(s,null);
                    }
                }
            }
            if(null != removeRequestCookies){
                for(String c:removeRequestCookies){
                    if(cookieMap.containsKey(c)){
                        cookieMap.remove(c);
                    }
                }
            }
            if(null != saveResponseCookies){
                String k,v;
                for(String c:saveResponseCookies){
                    k = SAVE_RESPONSE_PREFIX+c;
                    if(cookieMap.containsKey(k)){
                        v = cookieMap.get(k);
                        cookieMap.remove(k);
                        cookieMap.put(k.substring(SAVE_RESPONSE_PREFIX.length()),v);
                    }
                }
            }

            StringBuffer sb = new StringBuffer();
            Iterator its = cookieMap.keySet().iterator();
            while(its.hasNext()){
                String k = (String)its.next();
                String v = (String)cookieMap.get(k);
                if(sb.length()>0)sb.append("; ");
                if(null != v)
                    sb.append(k).append("=").append(v);
                else
                    sb.append(k);
            }
            headHashtable.put(cookieName,sb.toString());
        }
        if(headHashtable.containsKey("if-modified-since")){
            headHashtable.remove("if-modified-since");
        }
        if(headHashtable.containsKey("If-Modified-Since")){
            headHashtable.remove("If-Modified-Since");
        }


        if(headHashtable.containsKey("Content-Length")){
            headHashtable.remove("Content-Length");
        }
        if(headHashtable.containsKey("content-length")){
            headHashtable.remove("content-length");
        }
    }

    Header[] getHeaderFromTable(Hashtable<String,String> headHashtable){
        List li = new ArrayList();
        Enumeration<String> en = headHashtable.keys();
        while(en.hasMoreElements()){
            String k = en.nextElement();
            if(StringUtils.isNotBlank(headHashtable.get(k)))
                li.add(new Header(k,headHashtable.get(k)));

        }
        return (Header[])li.toArray(new Header[li.size()]);
    }

    @Override
    public Object doSomeThing(String xmlid,XMLParameter env,Map input,Map output,Map cfg) throws Exception {
        if(null == env.getParameter("http_trans_flag")){
            client = new HttpClient(connectionManager);
            env.addGlobalParameter("http_trans_flag","1");
        }
        String method=null,url=null,charset=null,inputStream=null,inputStreamEncode=null;
        Map addRequestHeaders =null,format=null,data=null;
        boolean isSendCookie=false;
        HttpDS ds = null;
        if(null != input){
            if(null != input.get("ds") && input.get("ds") instanceof HttpDS) {
                ds = (HttpDS) input.get("ds");
            }
            method = (String)input.get("method");
            url = (String)input.get("url");
            log.debug("remote url 1:"+url+" "+env.getParameter("${input_data}"));
            log.debug("remote url 2:"+url+" "+env.getValueFromExpress("(${input_data}.web_password)",this));
            url = (String)env.getValueFromExpress(url,this);
            log.debug("remote url 3:"+url);
            if(url.startsWith("https")){
                Protocol myhttps = new Protocol("https", new MySSLProtocolSocketFactory(), 443);
                Protocol.registerProtocol("https", myhttps);
            }
            isSendCookie = StringUtils.isTrue((String)input.get("issendcookie"));
            charset=(String)input.get("charset");
            Object hs = input.get("addRequestHeaders");
            if(null != hs && hs instanceof Map){
                addRequestHeaders=(Map)hs;
            }
            inputStream=(String)input.get("inputstream");
            inputStreamEncode=(String)input.get("inputstreamencode");

            if(null != input.get("data")) {
                Object d = input.get("data");
                if(d instanceof String && ((String)d).startsWith("{")){
                    d = StringUtils.convert2MapJSONObject((String)d);
                }
                if(d instanceof Map) {
                    data = (Map) d;
                }
            }
        }

        if(StringUtils.isNotBlank(inputStreamEncode)){
            if(inputStreamEncode.equalsIgnoreCase("base64")){
               // System.out.println(inputStream);
                inputStream = org.apache.commons.lang.StringUtils.deleteSpaces(new BASE64Encoder().encode(inputStream.getBytes()));
            }
        }
        if(null != ds){
            //response header to request header
            if(null !=ds.getResponseHeaders()){
                if(ds.getResponseHeaders().containsKey("Set-Cookie")){
                    String s = ds.getResponseHeaders().get("Set-Cookie");
                    if(null == ds.getRequestHeaders()){
                        ds.setRequestHeaders(new Hashtable<String, String>());
                    }
                    if(ds.getRequestHeaders().containsKey("Cookie"))
                        ds.getRequestHeaders().put("Cookie",s+";"+ds.getRequestHeaders().get("Cookie"));
                    else
                        ds.getRequestHeaders().put("Cookie",s);
                }

            }
        }else
            ds = new HttpDS();
        if(null!=addRequestHeaders){
            Iterator<String> its = addRequestHeaders.keySet().iterator();
            while(its.hasNext()){
                String k = its.next();
                if(k.equalsIgnoreCase("Set-Cookie")){
                    String s = getHeadValue(addRequestHeaders.get("Set-Cookie"));
                    if(null == ds.getRequestHeaders()){
                        ds.setRequestHeaders(new Hashtable<String, String>());
                    }
                    if(ds.getRequestHeaders().containsKey("Cookie"))
                        ds.getRequestHeaders().put("Cookie",s+";"+ds.getRequestHeaders().get("Cookie"));
                    else
                        ds.getRequestHeaders().put("Cookie",s);
                }else {
                    ds.getRequestHeaders().put(k, getHeadValue(addRequestHeaders.get(k)));
                }
            }
        }
        if(StringUtils.isNotBlank(inputStream)){
            //log.error("old_inputStream:"+inputStream);
            //if is service desc , can not to parse
            if(!Desc.isDescriptionString(inputStream)) {
                inputStream = (String) env.getValueFromExpress(inputStream, this);
            }
            //log.error("inputStream:"+inputStream);

            ByteArrayInputStream in = new ByteArrayInputStream(inputStream.getBytes());
            ds.setRequestInputStreamLength(in.available());
            ds.setRequestInputStream(in);
        }
        if(log.isDebugEnabled() && null != ds.getRequestHeaders()){
            log.debug("    Request Headers:");
            Hashtable ht = ds.getRequestHeaders();
            Enumeration en = ht.keys();
            while(en.hasMoreElements()){
                Object k = en.nextElement();
                log.debug("      "+k+"="+ds.getRequestHeaders().get(k));
            }
        }
        if(null !=data){
            ds.getProperties().putAll(data);
            if(log.isDebugEnabled()){
                log.debug("data:"+data);
            }
        }
        //log.error("url:"+url);
        //log.error("headers:"+ds.getRequestHeaders());
        //log.error("data:"+data);
        ds.setUrl(url);
        ds.setMethodName(method);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ds.setResponseOutputStream(out);
        ds.setSendCookie(isSendCookie);
        httpInvoke(ds);
        String str=null;
        Object ret=ds;
        if(null == ret){
            if(log.isDebugEnabled()){
                System.out.println("["+Thread.currentThread().getName()+"] [debug] httpclient3 return null url:"+url+" \nrespnse string:"+str);
            }
        }
        if(null !=ds.getResponseHeaders()){
            Hashtable<String,String> ht = ds.getResponseHeaders();
            if(null != env.getParameter("${response}") && env.getParameter("${response}") instanceof HttpServletResponse ){
                HttpServletResponse hr = (HttpServletResponse)env.getParameter("${response}");

                Iterator<String> its = ht.keySet().iterator();
                while(its.hasNext()) {
                    String k = its.next();
                    String v = ht.get(k);
                    hr.setHeader(k, v);
                }
            }
        }

        return ret;

    }
    String getHeadValue(Object o){
        if(null != o) {
            if (o instanceof String) {
                return (String) o;
            } else if (o instanceof List) {
                return StringUtils.join((List)o,";");
            }else{
                return o.toString();
            }
        }else{
            return "";
        }
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid,XMLParameter env, Map input,Map output, Map cfg) throws Exception {


        if(log.isDebugEnabled()){
            System.out.println("["+Thread.currentThread().getName()+"] ["+new Date()+"] [debug] httpclient3 url:"+input.get("url"));
        }
        if(null != input && null != input.get("ds")){
            if(!(input.get("ds") instanceof HttpDS)){
                if(log.isDebugEnabled()){
                    System.out.println(" HttpClient3 checkInput error, parameter.get(\"ds\") is :"+input.get("ds"));
                }
                return false;
            }
        }
        if(null != input && null != input.get("method") &&null != input.get("url")){
            if(log.isDebugEnabled()){
                System.out.println("["+Thread.currentThread().getName()+"] ["+new Date()+"] [debug] httpclient3 url check true");
            }
            return true;
        }else{
           if(null == input)
               log.error("httpclient request input is null");
            if(null == input.get("method"))
                log.error("httpclient request method is null");
            if(null == input.get("url"))
                log.error("httpclient request url is null,check variables in url");
        }
        return false;
    }

    @Override
    public ResultCheck checkReturn(String xmlid,XMLParameter env, Map input,Map output, Map cfg, Object ret) throws Exception {
        try{
/*
            if(null != output){
                Map filter = (Map)output.get("filter");
                if(null != filter){
                    List list = (List)filter.get("ds");
                    for(int i=list.size()-1;i>=0;i--){
                        char c = ((String)((List)list.get(i)).get(1)).charAt(0) ;
                        if(c!='0' && c!='6'){
                            list.remove(i);
                        }

                    }
                }
            }
            if(log.isDebugEnabled()){
                String u = null;
                if(null == ret){
                    if(null != input)
                     u = (String)input.get("url");
                    if(null == u){
                        u = (String)input.get("url");
                    }
                }
                System.out.println("["+Thread.currentThread().getName()+"] ["+new Date()+"] [debug] httpclient3 return:"+ret +(null!=u?" from url:"+u:""));
            }
*/

            return new ResultCheck(true,ret);
        }finally {

        }
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config,Object ret,Exception e) throws Exception {
        return true;
    }

}
