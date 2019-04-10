package com.octopus.tools.i18n;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;

import java.util.Properties;

/**
 * User: wangfeng2
 * Date: 14-8-18
 * Time: 下午5:15
 */
public class Locale extends XMLObject {

    public Locale(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
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


    public String getLocaleString(Properties properties){
        String s = getXML().getProperties().getProperty("locale");
        String[] ks = s.split("\\.");
        StringBuffer sb = new StringBuffer();
        boolean first=true;
        for(String k:ks){
            if(StringUtils.isNotBlank(properties.getProperty(k))) {
                if (!first) {
                    sb.append(".").append(properties.getProperty(k));
                } else {
                    sb.append(properties.getProperty(k));
                    first = false;
                }
            }
        }
        return sb.toString();
    }

}
