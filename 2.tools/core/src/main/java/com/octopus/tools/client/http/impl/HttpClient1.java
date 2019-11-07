package com.octopus.tools.client.http.impl;

import com.octopus.tools.client.http.HttpDS;
import com.octopus.tools.client.http.IHttpClient;
import com.octopus.utils.alone.ArrayUtils;
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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
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

        //long l = System.currentTimeMillis();
        try{
            String url = (String)input.get("url");
            String method = (String)input.get("method");
            Object reget = input.get("reget");
            boolean b = true;
            if(null != reget && reget instanceof Boolean){
                b = (Boolean) reget;
            }
            Object headers = input.get("addRequestHeaders");
            Object data = input.get("data");
            if(null != url){
                Map header=null;
                Map d = null;
                if(null != headers && headers instanceof Map)
                    header=(Map)headers;
                if(null != data && data instanceof Map)
                    d = (Map)data;
                return HttpURLConnectionUtils.sendRequest(url,method,header,d,60000,b);

            }
            return null;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }finally {

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
