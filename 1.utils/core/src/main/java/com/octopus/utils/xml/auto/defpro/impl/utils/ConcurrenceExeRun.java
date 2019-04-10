package com.octopus.utils.xml.auto.defpro.impl.utils;

import com.octopus.utils.flow.FlowParameters;
import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Iterator;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-5-6
 * Time: 下午3:50
 */
public class ConcurrenceExeRun implements Runnable{
    transient static Log log = LogFactory.getLog(ConcurrenceExeRun.class);
    XMLDoObject obj;
    XMLParameter env;
    Map input;
    Map output;
    Map config;
    Object[] callback;
    XMLMakeup xml;
    public ConcurrenceExeRun(Object[] os){
        if(os.length>0)
            obj = (XMLDoObject)os[0];
        if(os.length>1)
            env=(XMLParameter)os[1];
        if(os.length>2)
            input =(Map)os[2];
        if(os.length>3)
            output=(Map)os[3];
        if(os.length>4)
            config=(Map)os[4];
        if(os.length>5)
            callback=(Object[])os[5];
        if(os.length>6)
            xml= (XMLMakeup)os[6];
    }
    @Override
    public void run() {
        try {
            doAction();
        } catch (Exception e) {
            log.error("concurrence Exception",e);
        }
    }

    public void doAction()throws Exception{
        FlowParameters dc = new FlowParameters(false);
        //入参带入回调方法中
        if(null != dc && null != input){
            Iterator its = input.keySet().iterator();
            while(its.hasNext()){
                String k = (String)its.next();
                dc.addParameter("${" + k + "}", input.get(k));
            }
        }
        obj.doCheckThing(xml.getId(),dc, input,output, config,xml);
        if(null != callback){
            ExecutorUtils.synWork(callback[0], (String) callback[1], (Class[]) callback[2], new Object[]{dc,((Object[]) callback[3])[1],((Object[]) callback[3])[2]});
        }
    }
}
