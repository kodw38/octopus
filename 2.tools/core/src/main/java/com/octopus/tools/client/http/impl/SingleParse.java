package com.octopus.tools.client.http.impl;

import com.octopus.tools.client.http.HttpDS;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Administrator
 * Date: 14-10-25
 * Time: 下午12:41
 */
public class SingleParse extends XMLObject implements IHttpParse {
    List<XMLMakeup> list= new ArrayList<XMLMakeup>();
    public SingleParse(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        String us = xml.getProperties().getProperty("uss");
        if(StringUtils.isNotBlank(us)){
            String[] uss = us.split(",");
            for(String s:uss){
                XMLMakeup[] xs = getXML().getParent().getParent().getByTagProperty("us","key",s);
                if(ArrayUtils.isNotEmpty(xs)){
                    for(XMLMakeup x:xs)
                        list.add(x);
                }
            }
        }
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

    @Override
    public UrlDS getUrl(HttpDS parameters) throws Exception {
        String key = parameters.getProperties().get("urlcode");
        if(StringUtils.isNotBlank(key)){
            for(XMLMakeup x:list){
                XMLMakeup[] is = x.getByTagProperty("u","key",key);
                if(ArrayUtils.isNotEmpty(is)){
                    for(XMLMakeup t:is){
                        UrlDS ds = new UrlDS();
                        String u = t.getProperties().getProperty("url");
                        List<String> ms = StringUtils.getTagsNoMark(u,"[","]");
                        if(null !=ms && ms.size()>0){
                            for(String m:ms){
                                u=StringUtils.replace(u,"["+m+"]",parameters.getProperties().get(m));
                            }
                        }
                        if(StringUtils.isNotBlank(u)){
                            String proxy = x.getProperties().getProperty("proxy");
                            String readtimeout = x.getProperties().getProperty("readtimeout");
                            String connectiontimeout = x.getProperties().getProperty("connectiontimeout");
                            String redirect = x.getProperties().getProperty("redirect");
                            if(StringUtils.isNotBlank(connectiontimeout))
                                ds.setConnectionTimeout(Integer.parseInt(connectiontimeout));
                            if(StringUtils.isNotBlank(readtimeout))
                                ds.setReadTimeout(Integer.parseInt(readtimeout));
                            if(StringUtils.isNotBlank(redirect))
                                ds.setRedirect(StringUtils.isTrue(redirect));
                            if(StringUtils.isNotBlank(proxy)){
                                if(proxy.contains(":")){
                                    ds.setProxyUrl(proxy.substring(0,proxy.indexOf(":")));
                                }else{
                                    ds.setProxyUrl(proxy);
                                }
                                if(proxy.contains("#")){
                                    ds.setProxyPort(Integer.parseInt(proxy.substring(proxy.indexOf(":")+1,proxy.indexOf("#"))));
                                }else if(proxy.contains(":")){
                                    ds.setProxyPort(Integer.parseInt(proxy.substring(proxy.indexOf(":")+1)));
                                }

                            }
                            String removerequestcookies = x.getProperties().getProperty("removerequestcookies");
                            String saveresponsecookies = x.getProperties().getProperty("saveresponsecookies");
                            if(StringUtils.isNotBlank(removerequestcookies))
                                ds.setRemoveRequestCookies(removerequestcookies.split(","));
                            if(StringUtils.isNotBlank(saveresponsecookies))
                                ds.setSaveResponseCookies(saveresponsecookies.split(","));
                            ds.setUrl(u);
                            return ds;
                        }
                    }
                }
            }
        }
        return null;
    }
}
