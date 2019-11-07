package com.octopus.tools.client.http.impl;

import com.octopus.tools.client.http.HttpDS;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.*;

public class HttpURLConnectionUtils {
    transient static Log log = LogFactory.getLog(HttpURLConnectionUtils.class);
    public static String[] txtType = new String[]{"text","asp","xml","html","java","application","json","php","js"};
    static boolean isHtml(String base,String contextType){
        try {
            if (ArrayUtils.isLikeArrayInString(contextType, txtType)
                    && (
                    base.endsWith("/")
                            || (base.indexOf("/", 8) < 0 || (base.indexOf("/", 8) > 0 && base.substring(base.lastIndexOf("/")).indexOf(".") < 0))
                            || (base.indexOf("/", 8) > 0
                            && base.indexOf(".", base.lastIndexOf("/")) > 0
                            && ArrayUtils.isInStringArray(
                            HttpURLConnectionUtils.txtType, base.substring(base.indexOf(".", base.lastIndexOf("/")) + 1
                                    , (base.indexOf("?") > 0 ? base.indexOf("?") : base.length()))
                    )))) {
                return true;
            }
            return false;
        }catch (Exception e){
            return false;
        }
    }
    public static String removeRelative(String url){

        if(url.indexOf("../")>0){
            url = StringUtils.replaceAllWithReg(url,"/[^/]+/\\.\\./","/");
        }
        if(url.indexOf("./")>0){
            url = StringUtils.replace(url,"./","");
        }
        if(url.endsWith("/.")){
            url = StringUtils.replace(url,"/.","/");
        }
        return url;

    }
    static LinkedList cacherul = new LinkedList();
    public static HttpDS sendRequest(String url, String method, Map headers, Map data,int timeout,boolean isReget){
        url = removeRelative(url);
        if(url.length()<10) return null;
        if(!isReget) {
            if (cacherul.contains(url)) return null;
            cacherul.add(url);

            if (cacherul.size() > 100000) {
                //cacherul = Collections.co(cacherul,100000);
            }
        }
        HttpURLConnection conn=null;
        try {
            if (log.isDebugEnabled()) {
                log.debug("url:" + url);
            }
            URL u = new URL(url);
            conn = (HttpURLConnection) u.openConnection();
            if(url.startsWith("https")){
                SSLContext sslcontext = SSLContext.getInstance("SSL","SunJSSE");
                sslcontext.init(null, new TrustManager[]{new MyX509TrustManager()}, new java.security.SecureRandom());
                ((HttpsURLConnection)conn).setSSLSocketFactory(sslcontext.getSocketFactory());
                conn.setInstanceFollowRedirects(false);
            }
            if(timeout==0){
                timeout=40000;
            }
            conn.setRequestProperty("User-agent","Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.81 Safari/537.36");
            conn.setConnectTimeout(timeout);
            conn.setReadTimeout(timeout);
            conn.setDoOutput(true);// 打开写入属性
            conn.setDoInput(true);// 打开读取属性

            if (StringUtils.isNotBlank(method))
                conn.setRequestMethod(method);
            //set request header

            if (null != headers) {
                Iterator<String> its = ((Map) headers).keySet().iterator();
                while (its.hasNext()) {
                    String k = its.next();
                    conn.addRequestProperty(k, (String) ((Map) headers).get(k));
                }
            }
            //set data
            if (null != data && data instanceof Map) {
                Iterator<String> its = ((Map) data).keySet().iterator();
                StringBuffer sb = new StringBuffer();
                while (its.hasNext()) {
                    String k = its.next();
                    conn.addRequestProperty(k, (String) ((Map) data).get(k));
                    if (sb.length() != 0)
                        sb.append("&").append(k).append("=").append((String) ((Map) data).get(k));
                    else
                        sb.append(k).append("=").append((String) ((Map) data).get(k));
                }
                if(null != sb) {
                    String s = sb.toString();
                    if(StringUtils.isNotBlank(s)) {
                        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), "UTF-8"));
                        out.write(sb.toString());
                        out.flush();
                        out.close();
                    }
                }
            }

            HttpDS ds = new HttpDS();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ds.setResponseOutputStream(out);
            try {
                int response_code = conn.getResponseCode();
                Map<String, List<String>> m = conn.getHeaderFields();
                Hashtable ht = getResponseHeaders(m);
                String charset = "UTF-8";
                if (null != m && null != m.get("Charset") && m.get("Charset").size() > 0) {
                    charset = m.get("Charset").get(0);
                }
                ds.setResponseHeaders(ht);
                if (response_code == HttpURLConnection.HTTP_OK) {
                    try {
                        ds.setStatusCode(response_code);
                        InputStream in = conn.getInputStream();
                        if (null != ds.getResponseHeaders() && null != ds.getResponseHeaders().get("Content-Type") &&
                                isHtml(url, ds.getResponseHeaders().get("Content-Type"))) {
                            //                InputStreamReader reader = new InputStreamReader(in,charSet);
                            BufferedReader reader = new BufferedReader(new InputStreamReader(in, charset));
                            //byte [] b  = new byte[1024];
                            String line = null;
                            while ((line = reader.readLine()) != null) {
                                //if (log.isDebugEnabled())
                                //    log.debug(line);
                                //System.out.println(new String(line.getBytes()));
                                out.write(line.getBytes());
                            }
                        } else {
                        /*byte[] bs = new byte[1024];
                        while(in.read(bs)>0){
                            out.write(bs);
                        }*/
                        /*byte[] b = new byte[in.available()];
                        in.read(b);
                        out.write(b);*/
                            byte[] buffer = new byte[1024];
                            int length;
                            while ((length = in.read(buffer)) > 0) {
                                out.write(buffer, 0, length);
                            }
                        }
                    }catch (Exception ex){
                        log.error("get inputStream error");
                    }
                }else{

                }

            } catch (Exception e) {
                //log.error(url, e);
                if (!(e instanceof SocketException || e instanceof SocketTimeoutException)) {
                    log.error(url, e);
                }
            }
            return ds;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }finally {
            if(conn !=null){
                conn.disconnect();
            }
        }
    }
    static Hashtable<String,String> getResponseHeaders(Map<String, List<String>> d){
        if(null != d) {
            Hashtable t = new Hashtable();
            Iterator<String> its = d.keySet().iterator();
            while(its.hasNext()){
                String k = its.next();
                String ss = ArrayUtils.toString(d.get(k),";");
                if(null!= ss && null != k)
                    t.put(k,ss);
            }
            return t;
        }
        return null;
    }
}
