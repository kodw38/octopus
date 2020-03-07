package com.octopus.tools.client.http.impl;

import com.octopus.tools.client.http.HttpDS;
import com.octopus.tools.client.http.IHttpClient;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.exception.Logger;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import sun.misc.BASE64Encoder;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.net.ConnectException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

/**
 * User: Administrator
 * Date: 14-10-25
 * Time: 上午10:31
 */
public class HttpClient4 extends XMLDoObject implements IHttpClient{
    HashMap<String,IHttpParse> ps;
    String SAVE_RESPONSE_PREFIX="IF$_";
    HttpClient httpClient=null;
    public HttpClient4(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        HttpParams params = new BasicHttpParams();
        Integer CONNECTION_TIMEOUT = 2 * 1000; //设置请求超时2秒钟 根据业务调整
        Integer SO_TIMEOUT = 2 * 1000; //设置等待数据超时时间2秒钟 根据业务调整
        Long CONN_MANAGER_TIMEOUT = 500L; //该值就是连接不够用的时候等待超时时间，一定要设置，而且不能太大 ()
        params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, CONNECTION_TIMEOUT);
        params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, SO_TIMEOUT);
        params.setLongParameter(ClientPNames.CONN_MANAGER_TIMEOUT, CONN_MANAGER_TIMEOUT);
        //在提交请求之前 测试连接是否可用
        params.setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, true);
        PoolingClientConnectionManager conMgr = new PoolingClientConnectionManager();
        conMgr.setMaxTotal(200); //设置整个连接池最大连接数 根据自己的场景决定
        //是路由的默认最大连接（该值默认为2），限制数量实际使用DefaultMaxPerRoute并非MaxTotal。
        //设置过小无法支持大并发(ConnectionPoolTimeoutException: Timeout waiting for connection from pool)，路由是对maxTotal的细分。
        conMgr.setDefaultMaxPerRoute(conMgr.getMaxTotal());//（目前只有一个路由，因此让他等于最大值）
        //另外设置http client的重试次数，默认是3次；当前是禁用掉（如果项目量不到，这个默认即可）

        httpClient = new DefaultHttpClient(conMgr,params);

        //httpClient.setHttpRequestRetryHandler(new DefaultHttpRequestRetryHandler(0, false));
    }

    @Override
    public void notifyObject(String op, Object obj) throws Exception {

    }

    @Override
    public void destroy() throws Exception {

    }

    @Override
    public void initial() throws Exception {

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
    @Override
    public void httpInvoke(HttpDS parameters) throws Exception {
/*
        IHttpParse p = null;
        UrlDS url = p.getUrl(parameters);
        invoke(url,parameters);
*/
        HttpClient client = httpClient;
        try{
            //if SSL two ways

            if(null != parameters.getSSLCacert()){
                if(StringUtils.isNotBlank(parameters.getSSLCacert().get("cacert"))
                        && StringUtils.isNotBlank(parameters.getSSLCacert().get("key"))
                        && StringUtils.isNotBlank(parameters.getSSLCacert().get("cert"))
                        ) {
                    log.debug("set ssl cert:"+parameters.getSSLCacert());
                    HttpClient tempClient = SSLUtils.getSSLSocktetBidirectional(null,parameters.getSSLCacert().get("cacert"),parameters.getSSLCacert().get("cert"),parameters.getSSLCacert().get("key"));
                    //SSLConnectionSocketFactory sslsf = getSocketFactoryPEM(parameters.getSSLCacert().get("pem"), parameters.getSSLCacert().get("key"));
                    //org.apache.http.impl.client.CloseableHttpClient tempClient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
                    client = tempClient;
                    //HttpGet httpget = new HttpGet(url);
                    //httpClient.execute(httpget);

                }

            }

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

            Header[] headers=null;
            if(null != parameters.getRequestHeaders() && parameters.getRequestHeaders().size()>0){
                doRequestCookie(parameters.getRequestHeaders(),removeRequestCookies,saveResponseCookies);
                headers = getHeaderFromTable(parameters.getRequestHeaders());
            }
            HttpResponse response=null;
            if("GET".equalsIgnoreCase(parameters.getMethodName())){

                    StringBuffer par = new StringBuffer();
                    if (null != parameters.getProperties()) {

                        Iterator<String> its = parameters.getProperties().keySet().iterator();
                        while (its.hasNext()) {
                            String k = its.next();
                            if (par.length() > 0) {
                                par.append("&");
                            }
                            par.append(k).append("=").append(parameters.getProperties().get(k));
                        }
                    }
                    if (par.length() > 0) {
                        if (url.contains("?")) {
                            url += "&" + par.toString();
                        } else {
                            url += "?" + par.toString();
                        }
                    }
                    HttpGet httpget = new HttpGet(url);
                try {
                    if (null != headers)
                        for (Header h : headers) {
                            //System.out.println(h.getName()+":"+h.getValue());
                            if (h.getName().equals("Cookie")) continue;
                            httpget.addHeader(h);
                        }
                    response = client.execute(httpget);
                    parameters.setStatusCode(response.getStatusLine().getStatusCode());
                    Header[] hs = response.getAllHeaders();
                    if(null != hs) {
                        setHeaderToHttpServletResponse(hs, saveResponseCookies, parameters);
                    }
                    if(null !=response.getEntity()) {
                        response.getEntity().writeTo(parameters.getResponseOutputStream());
                    }
                }catch (Exception e){
                    throw e;
                }finally {
                    httpget.releaseConnection();
                }

            }else if("POST".equals(parameters.getMethodName())){
                long l=0;
                if(log.isDebugEnabled())
                    l = System.currentTimeMillis();
                HttpPost httppost = new HttpPost(url);
                try{
                    if(null != headers)
                        for(Header h:headers){
                            //System.out.println(h.getName()+":"+h.getValue());
                            if(h.getName().equals("Cookie")) continue;
                            httppost.addHeader(h);
                        }
                        //httppost.addHeader("Content-Type","application/x-www-form-urlencoded");

                    HttpEntity httpEntity = createProxyPostEntity(parameters);
                    if (null != httpEntity) {
                        httppost.setEntity(httpEntity);
                    }
                    if(Logger.isInfoEnabled()){
                        Logger.info(this.getClass(), null, "http", "before http post", null);
                    }
                    response = client.execute(httppost);
                    parameters.setStatusCode(response.getStatusLine().getStatusCode());
                    Header[] hs = response.getAllHeaders();
                    if(null != hs) {
                        setHeaderToHttpServletResponse(hs, saveResponseCookies, parameters);
                    }
                    if(null !=response.getEntity()) {
                        response.getEntity().writeTo(parameters.getResponseOutputStream());
                    }
                }catch (Exception ioe){
                    throw new Exception(url,ioe);
                }finally {
                    httppost.releaseConnection();
                }
            }else {
                throw new Exception("now not support the request method["+parameters.getMethodName()+"]");
            }
            if(null != response){
               // setHeaderToHttpServletResponse(response, url,parameters);
                //setContentToHttpServletResponse(response, parameters);
            }
        }catch (Exception e){
            if(!(e instanceof ConnectException)){
                log.error("url:"+parameters.getUrl(), e);
            }
            throw e;
        }finally {

        }
    }
    void setHeaderToHttpServletResponse(Header[] hs, String[] saveResponseCookies, HttpDS httpDS){
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
    /*void invoke(UrlDS url,HttpDS parameters)throws  Exception{

        try{
            if(!url.isRedirect())
                httpClient.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, false);
            //do header
            if(url.getConnectionTimeout()>0){
                httpClient.getParams().setLongParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,url.getConnectionTimeout());
            }
            if(url.getReadTimeout()>0){
                httpClient.getParams().setLongParameter(CoreConnectionPNames.SO_TIMEOUT,url.getReadTimeout());
            }
            if(StringUtils.isNotBlank(url.getProxyUrl())){
                HttpHost proxy = new HttpHost(url.getProxyUrl(),url.getProxyPort());
                httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
            }

            Header[] headers=null;
            if(null != parameters.getRequestHeaders() && parameters.getRequestHeaders().size()>0){
                doRequestCookie(parameters.getRequestHeaders(),url);
                headers = getHeaderFromTable(parameters.getRequestHeaders());
            }
            //get exe and get response
            BasicHttpContext context = new BasicHttpContext();
            HttpResponse response=null;
            if("GET".equalsIgnoreCase(parameters.getMethodName())){
                HttpGet req = new HttpGet(url.getUrl());
                if(null != headers)
                    req.setHeaders(headers);
                response = httpClient.execute(req,context);
            }else if("POST".equals(parameters.getMethodName())){
                HttpPost post = new HttpPost(url.getUrl());
                    if (null != headers)
                        post.setHeaders(headers);
                    HttpEntity httpEntity = createProxyPostEntity(parameters);
                    if (null != httpEntity) {
                        post.setEntity(httpEntity);
                    }
                    response = httpClient.execute(post, context);

            }else {
                throw new Exception("now not support the request method["+parameters.getMethodName()+"]");
            }

            if(null != response){
                setHeaderToHttpServletResponse(response, url,parameters);
                setContentToHttpServletResponse(response, parameters);
            }
        }catch (Exception e){
             e.printStackTrace();
        }finally {

        }
    }*/

    /*void setContentToHttpServletResponse(HttpResponse proxyResponse,HttpDS ds) throws IOException {
        //header code:204 No Content, 304 Not Modified, 205 Reset Content
        if(!ArrayUtils.isInIntArray(new int[]{304, 204, 205}, proxyResponse.getStatusLine().getStatusCode())){
            BufferedOutputStream out = new BufferedOutputStream(ds.getResponseOutputStream());
            HttpEntity content = proxyResponse.getEntity();
            if(StringUtils.isNotBlank(ds.getBase())){
                if(null != content.getContentType() && (content.getContentType().getValue().contains("html") || content.getContentType().getValue().contains("jsp")) ){
                    out.write(ds.getBase().getBytes());
                }
            }
            InputStream proxyIn = new BufferedInputStream(content.getContent());
            while (true){
                int i;
                if ((i = proxyIn.read()) < 0)
                    break;
                out.write(i);
            }
            if (proxyIn != null)
                proxyIn.close();
            if (out != null)
            {
                out.flush();
                out.close();
            }
        }
    }

    void setHeaderToHttpServletResponse(HttpResponse proxyResponse,UrlDS urlDS,HttpDS httpDS){
        HttpEntity content = proxyResponse.getEntity();
        if(null  != content){
            Header[] hs = proxyResponse.getAllHeaders();
            if(null != hs){
                Hashtable<String,String> rhs = new Hashtable<String, String>();
                for(Header h:hs){
                    if(h.getName().equals("Set-Cookie")){
                        String v = doResponseCookie(h.getValue(),urlDS);
                        if(rhs.containsKey("Set-Cookie")){
                            v =rhs.get("Set-Cookie") +";"+v;
                        }
                        rhs.put("Set-Cookie",v);
                    }else
                    rhs.put(h.getName(),h.getValue());
                }
                httpDS.setResponseHeaders(rhs);
            }
            if(null != content.getContentType())
                httpDS.setContentType(content.getContentType().getValue());
            if(content.getContentLength() !=0)
                httpDS.setContentLength((int) content.getContentLength());
            httpDS.setStatusCode(proxyResponse.getStatusLine().getStatusCode());
        }
    }*/

    String doResponseCookie(String cookie,UrlDS ds){
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
            if(null != ds.getSaveResponseCookies()){
                for(String s:ds.getSaveResponseCookies()){
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

    HttpEntity createProxyPostEntity(HttpDS ds)throws IOException {
        if(null != ds.getRequestInputStream()){
            InputStreamEntity reqEntity = new InputStreamEntity(ds.getRequestInputStream(),ds.getRequestInputStreamLength());
            return reqEntity;
        }
        return null;
    }

    void doRequestCookie(Hashtable<String,String> headHashtable,UrlDS urlDS){
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
            if(null != urlDS.getRemoveRequestCookies()){
                for(String c:urlDS.getRemoveRequestCookies()){
                    if(cookieMap.containsKey(c)){
                        cookieMap.remove(c);
                    }
                }
            }
            if(null != urlDS.getSaveResponseCookies()){
                String k,v;
                for(String c:urlDS.getSaveResponseCookies()){
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

    BasicHeader[] getHeaderFromTable(Hashtable<String,String> headHashtable){
        List li = new ArrayList();
        Enumeration<String> en = headHashtable.keys();
        while(en.hasMoreElements()){
            String k = en.nextElement();
            li.add(new BasicHeader(k,headHashtable.get(k)));

        }
        return (BasicHeader[])li.toArray(new BasicHeader[li.size()]);
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {

        String method=null,url=null,charset=null,inputStream=null,inputStreamEncode=null;
        Map addRequestHeaders =null,format=null,data=null;
        HttpDS ds = null;
        if(null != input){
            ds =(HttpDS)input.get("ds");
            method = (String)input.get("method");
            url = (String)input.get("url");
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
        if(null != config && null != config.get("ssl") && config.get("ssl") instanceof Map && ((Map)config.get("ssl")).size()>0){
            ds.setSSLCacert((Map)config.get("ssl"));
        }
        if(null!=addRequestHeaders){
            Iterator<String> its = addRequestHeaders.keySet().iterator();
            while(its.hasNext()){
                String k = its.next();
                if(k.equalsIgnoreCase("Set-Cookie")){
                    String s = (String)addRequestHeaders.get("Set-Cookie");
                    if(null == ds.getRequestHeaders()){
                        ds.setRequestHeaders(new Hashtable<String, String>());
                    }
                    if(ds.getRequestHeaders().containsKey("Cookie"))
                        ds.getRequestHeaders().put("Cookie",s+";"+ds.getRequestHeaders().get("Cookie"));
                    else
                        ds.getRequestHeaders().put("Cookie",s);
                }else {
                    ds.getRequestHeaders().put(k, (String) addRequestHeaders.get(k));
                }
            }
        }
        if(StringUtils.isNotBlank(inputStream)){
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
        }
        ds.setUrl(url);
        ds.setMethodName(method);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ds.setResponseOutputStream(out);
        httpInvoke(ds);
        String str=null;
        Object ret=ds;
        if(null == ret){
            if(log.isDebugEnabled()){
                System.out.println("["+Thread.currentThread().getName()+"] [debug] httpclient3 return null url:"+url+" \nrespnse string:"+str);
            }
        }

        return ret;
    }

    @Override
    public ResultCheck checkReturn(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return new ResultCheck(true,ret);
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret, Exception e) throws Exception {
        return false;
    }

    protected static SSLConnectionSocketFactory getSocketFactoryPEM(String pemPath,String keypath) throws Exception {
        byte[] pem = fileToBytes(pemPath);
        byte[] pemKey = fileToBytes(keypath);

        byte[] certBytes = parseDERFromPEM(pem, "-----BEGIN CERTIFICATE-----", "-----END CERTIFICATE-----");
        byte[] keyBytes = parseDERFromPEM(pemKey, "-----BEGIN PRIVATE KEY-----", "-----END PRIVATE KEY-----");

        X509Certificate cert = generateCertificateFromDER(certBytes);
        RSAPrivateKey key  = generatePrivateKeyFromDER(keyBytes);

        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(null);
        keystore.setCertificateEntry("cert-alias", cert);
        keystore.setKeyEntry("key-alias", key, "123".toCharArray(), new Certificate[] {cert});

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keystore, "123".toCharArray());

        KeyManager[] km = kmf.getKeyManagers();

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(km, null, null);
        SSLConnectionSocketFactory sslsf =
                new SSLConnectionSocketFactory(context,null, null,
                        SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
        return sslsf;
    }
    public static byte[] parseDERFromPEM(byte[] pem, String beginDelimiter, String endDelimiter) {
        String data = new String(pem);
        String[] tokens = data.split(beginDelimiter);
        tokens = tokens[1].split(endDelimiter);
        return DatatypeConverter.parseBase64Binary(tokens[0]);
    }

    public static RSAPrivateKey generatePrivateKeyFromDER(byte[] keyBytes) throws InvalidKeySpecException, NoSuchAlgorithmException {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);

        KeyFactory factory = KeyFactory.getInstance("RSA");

        return (RSAPrivateKey)factory.generatePrivate(spec);
    }
    public static X509Certificate generateCertificateFromDER(byte[] certBytes) throws CertificateException {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");

        return (X509Certificate)factory.generateCertificate(new ByteArrayInputStream(certBytes));
    }
    public static byte[] fileToBytes(String filePath) {
        byte[] buffer = null;
        File file = new File(filePath);

        FileInputStream fis = null;
        ByteArrayOutputStream bos = null;

        try {
            fis = new FileInputStream(file);
            bos = new ByteArrayOutputStream();

            byte[] b = new byte[1024];

            int n;

            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }

            buffer = bos.toByteArray();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (null != bos) {
                    bos.close();
                }
            } catch (IOException ex) {
            } finally{
                try {
                    if(null!=fis){
                        fis.close();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

        return buffer;
    }
}
