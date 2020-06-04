package com.octopus.utils.xml.auto.defpro.impl;

import com.octopus.utils.flow.FlowParameters;
import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.thread.ds.InvokeTask;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import com.octopus.utils.xml.auto.defpro.IObjectInitProperty;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;

/**
 * init="{}"
 * User: wfgao_000
 * Date: 15-5-14
 * Time: 下午4:24
 */
public class SystemInitialProperty implements IObjectInitProperty {
    transient static Log log = LogFactory.getLog(SystemInitialProperty.class);
    @Override
    public Object exeProperty(Map proValue, XMLDoObject obj, XMLMakeup xml, XMLParameter parameter, Map input, Map output,Map config) {
        try {
            if(parameter == null)
                parameter = new FlowParameters(true);
            if(null != proValue){
                if(proValue.containsKey("ripe")){
                    if("objectsInitial".equalsIgnoreCase((String)proValue.get("ripe"))){
                        //在所有对象的doInitial方法执行完成后执行，应用初始化
                        obj.addApplicationInitialAction(obj,"doCheckThing",new Class[]{String.class,XMLParameter.class,Map.class,Map.class,Map.class,XMLMakeup.class},new Object[]{xml.getId(),parameter,input,output,config,xml});
                        return null;
                    }else if("applicationInitial".equalsIgnoreCase((String)proValue.get("ripe"))){
                        //应用初始化后执行，执行完应用就ready，可以被外围访问
                        obj.addApplicationReadyAction(obj,"doCheckThing",new Class[]{String.class,XMLParameter.class,Map.class,Map.class,Map.class,XMLMakeup.class},new Object[]{xml.getId(),parameter,input,output,config,xml});
                        return null;
                    }else if("applicationReady".equalsIgnoreCase((String)proValue.get("ripe"))){
                        //系统ready后异步执行
                        InvokeTask task = new InvokeTask(obj,"doCheckThing",new Class[]{String.class,XMLParameter.class,Map.class,Map.class,Map.class,XMLMakeup.class},new Object[]{xml.getId(),parameter,input,output,config,xml});
                        obj.addApplicationFinishedAction(task);
                        return null;
                    }
                }
            }

            if(obj.isAsyn(xml)){
                ExecutorUtils.work(obj,"doCheckThing",new Class[]{String.class,XMLParameter.class,Map.class,Map.class,Map.class,XMLMakeup.class},new Object[]{xml.getId(),parameter,input,output,config,xml});
            }else{
                obj.doCheckThing(xml.getId(),parameter,input,output,config,xml);
            }
        } catch (Exception e) {
            log.error(xml.getId(),e);

        }
        return null;
    }

    @Override
    public boolean isAsyn() {
        return false;
    }

    @Override
    public void destroy(XMLMakeup xml) throws Exception {

    }
}
