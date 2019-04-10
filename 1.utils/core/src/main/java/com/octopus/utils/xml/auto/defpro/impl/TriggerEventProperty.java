package com.octopus.utils.xml.auto.defpro.impl;

import com.octopus.utils.thread.ds.InvokeTask;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import com.octopus.utils.xml.auto.defpro.IObjectInitProperty;

import java.util.Map;

/**
 * Created by Administrator on 2019/3/12.
 */
public class TriggerEventProperty implements IObjectInitProperty {
    @Override
    public void destroy(XMLMakeup xml) throws Exception {

    }

    @Override
    public Object exeProperty(Map proValue, XMLDoObject obj, XMLMakeup xml, XMLParameter parameter, Map input, Map output, Map config) throws Exception {
        if(null != proValue && null != obj){
            if(proValue.containsKey("src") && proValue.containsKey("cond")){
                XMLObject xo = obj.getObjectById((String)proValue.get("src"));
                if(null != xo){
                    xo.addTrigger(proValue.get("cond"),new InvokeTaskByObjName(obj.getXML().getId(),"doCheckThing",new Class[]{String.class,XMLParameter.class,Map.class,Map.class,Map.class,XMLMakeup.class},new Object[]{xml.getId(),parameter,input,output,config,xml}));
                }
            }
        }
        return null;
    }

    @Override
    public boolean isAsyn() {
        return false;
    }
}
