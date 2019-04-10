package com.octopus.tools.client.http.impl;

import com.octopus.tools.client.http.HttpDS;
import com.octopus.tools.client.http.IHttpClient;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-6-13
 * Time: 下午6:15
 */
public class HttpClient1 extends XMLDoObject implements IHttpClient {
    transient static Log log = LogFactory.getLog(HttpClient1.class);

    public HttpClient1(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    @Override
    public void httpInvoke(HttpDS parameters) throws Exception {

    }


    @Override
    public Object doSomeThing(String xmlid,XMLParameter env, Map input, Map output,Map config) throws Exception {
        HttpURLConnection conn=null;
        //long l = System.currentTimeMillis();
        try{
            String url = (String)input.get("url");
            String method = (String)input.get("method");
            if(null != url){
                if(log.isDebugEnabled()){
                    log.debug("url:"+url);
                }
                URL u = new URL(url);
                conn = (HttpURLConnection)u.openConnection();
                conn.setConnectTimeout(40000);
                conn.setReadTimeout(40000);
                conn.setDoOutput(true);// 打开写入属性
                conn.setDoInput(true);// 打开读取属性


                if(StringUtils.isNotBlank(method))
                    conn.setRequestMethod(method);
                Object data = input.get("data");
                if(null != data && data instanceof Map) {
                    Iterator<String> its = ((Map)data).keySet().iterator();
                    StringBuffer sb = new StringBuffer();
                    while(its.hasNext()) {
                        String k = its.next();
                        conn.addRequestProperty(k,(String)((Map)data).get(k));
                        if(sb.length()!=0)
                            sb.append("&").append(k).append("=").append((String)((Map)data).get(k));
                        else
                            sb.append(k).append("=").append((String)((Map)data).get(k));
                    }
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), "UTF-8"));
                    out.write(sb.toString());
                    out.flush();
                    out.close();
                }

                HttpDS ds = new HttpDS();
                ByteArrayOutputStream out = new ByteArrayOutputStream() ;
                ds.setResponseOutputStream(out);
                try{
                    int response_code = conn.getResponseCode();
                    StringBuffer sb  = new StringBuffer();
                    if (response_code == HttpURLConnection.HTTP_OK) {
                        InputStream in = conn.getInputStream();
        //                InputStreamReader reader = new InputStreamReader(in,charSet);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                        //byte [] b  = new byte[1024];
                        String line = null;
                        while ((line = reader.readLine()) != null) {
                            if(log.isDebugEnabled())
                                log.debug(line);
                            //System.out.println(new String(line.getBytes()));
                            out.write(line.getBytes());
                        }

                    }
                }catch (Exception e){
                    if(!(e instanceof SocketException || e instanceof SocketTimeoutException)){
                        log.error(url,e);
                    }
                }
                //System.out.println("lost:"+(System.currentTimeMillis()-l));
                return ds;
            }
            return null;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }finally {
            if(conn !=null){
                conn.disconnect();
            }
        }
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid,XMLParameter env, Map input,Map output, Map config) throws Exception {
        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResultCheck checkReturn(String xmlid,XMLParameter env, Map input,Map output, Map config, Object ret) throws Exception {
        if(null != ret)
            return new ResultCheck(true,ret);
        else
            return new ResultCheck(false,null);
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config,Object ret,Exception e) throws Exception {
        throw new Exception("now support rollback");
    }
}
