package com.octopus.utils.xml.auto.defpro.impl;

import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import com.octopus.utils.xml.auto.defpro.IObjectInvokeProperty;
import com.octopus.utils.xml.auto.defpro.impl.utils.DoAction;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 16-6-1
 * Time: 下午12:56
 */
public class SimpleAsynProperty implements IObjectInvokeProperty,Serializable {
    transient static Log log = LogFactory.getLog(SimpleAsynProperty.class);

    @Override
    public Object exeProperty(Map proValue, XMLDoObject obj, XMLMakeup xml, XMLParameter parameter, Map input, Map output, Map config) {
        try{
            if(null!=parameter){

                /*obj.doCheckThing(xml.getId(),parameter,  input,  output,config,xml);
                */
                ExecutorUtils.work(new DoAction(obj,parameter,input,output,config,(Object[])parameter.getParameter(XMLParameter.XMLLOGIC_BACK_CALL_KEY),xml), "doAction",null,null);
                parameter.removeParameter(XMLParameter.XMLLOGIC_BACK_CALL_KEY);
                /*if(null != os && os.getClass().isArray()){
                    Object[] ss = (Object[])os;
                    parameter.removeParameter(BACK_CALL_KEY);
                    ExecutorUtils.work((Object)ss[0],(String)ss[1],(Class[])ss[2],((Object[])ss[3]));
                }else{
                    ExecutorUtils.work(obj,"doCheckThing",new Class[]{String.class,XMLParameter.class,Map.class,Map.class,Map.class,XMLMakeup.class},new Object[]{xml.getId(),parameter,input,output,config,xml});
                }*/
                return null;
            }
        }catch (Exception e){
            log.error(e.getMessage(), e);
        }

        return null;
    }

    @Override
    public boolean isAsyn() {
        return true;
    }

}
