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
 * Date: 16-6-2
 * Time: 上午9:16
 */
public class DoAction{
    transient static Log log = LogFactory.getLog(DoAction.class);
    XMLDoObject obj;
    XMLParameter env;
    Map input;
    Map output;
    Map config;
    Object[] callback;
    XMLMakeup xml;

    public XMLDoObject getObj() {
        return obj;
    }

    public Map getConfig() {
        return config;
    }

    public XMLMakeup getXml(){
        return xml;
    }
    public DoAction(XMLDoObject obj,XMLParameter env,Map input,Map output,Map config,Object[] callback,XMLMakeup xml){
        this.obj=obj;
        this.env=env;
        this.input=input;
        this.output=output;
        this.config=config;
        this.callback=callback;
        this.xml=xml;

    }
    public void doAction()throws Exception{
        obj.doCheckThing(xml.getId(),env, input, output,config,xml);
        if(null != callback){
            //入参带入回调方法中
            if(null != env && null != input){
                Iterator its = input.keySet().iterator();
                while(its.hasNext()){
                    String k = (String)its.next();
                    env.addParameter("${" + k + "}", input.get(k));
                }
            }
            ExecutorUtils.synWork(callback[0], (String) callback[1], (Class[]) callback[2], (Object[]) callback[3]);
        }

    }
    public void doNewContainerAction()throws Exception{
        //log.error("-----do doNewContainerAction---");
        FlowParameters nd =  new FlowParameters(false);
        //入参带入回调方法中
        if(null != nd && null != input){
            Iterator its = input.keySet().iterator();
            while(its.hasNext()){
                String k = (String)its.next();
                nd.addParameter("${" + k + "}", input.get(k));
            }
        }
        obj.doCheckThing(xml.getId(),nd, input, output,config,xml);
        //log.error("-----finished doNewContainerAction----");
        if(null != callback){
            log.error("----do callback---");
            ExecutorUtils.synWork(callback[0], (String) callback[1], (Class[]) callback[2], new Object[]{nd,((Object[])callback[3])[1],((Object[])callback[3])[2]});
        }

    }
}
