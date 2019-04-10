package com.octopus.utils.xml.auto.defpro.impl;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.flow.FlowParameters;
import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import com.octopus.utils.xml.auto.defpro.IObjectInitProperty;
import com.octopus.utils.xml.auto.defpro.impl.utils.DoAction;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.SchedulerException;

import java.util.List;
import java.util.Map;
import java.util.Timer;

/**
 * worktime="{crons:['cronexpression','xxx']}"
 * User: Administrator
 * Date: 15-1-12
 * Time: 上午9:36
 */
public class WorkTimeInitProperty implements IObjectInitProperty {
    static transient Log log = LogFactory.getLog(WorkTimeInitProperty.class);
    @Override
    public Object exeProperty(Map proValue,XMLDoObject obj,XMLMakeup xml,XMLParameter parameter,Map input,Map output,Map config) {
        //if work time task, use a new Parameter Container
        if(parameter == null)
            parameter = new FlowParameters(false);

        if(null != proValue){
            List crons = (List)proValue.get("crons");
            boolean isConcurrent=true;
            if(StringUtils.isNotBlank(proValue.get("isconcurrent"))){
                isConcurrent = StringUtils.isTrue((String)proValue.get("isconcurrent"));
            }
            if(null != crons && crons.size()>0){
                try{
                    String[] s_crons = new String[crons.size()];
                    for(int i=0;i<crons.size();i++){
                       s_crons[i]=(String)crons.get(i);
                    }
                    if(null!=parameter){
                        //ExecutorUtils.workTimeTask(s_crons, new DoAction(obj,parameter,input,output,config,(Object[])parameter.getParameter(XMLParameter.XMLLOGIC_BACK_CALL_KEY),xml), "doAction",null,null,isConcurrent);
                        ExecutorUtils.workTimeTask(obj.getEnvData(),s_crons, new DoAction(obj,parameter,input,output,config,(Object[])parameter.getParameter(XMLParameter.XMLLOGIC_BACK_CALL_KEY),xml), "doNewContainerAction",null,null,isConcurrent);
                        parameter.removeParameter(XMLParameter.XMLLOGIC_BACK_CALL_KEY);
                        return null;
                    }
                    ExecutorUtils.workTimeTask(obj.getEnvData(),s_crons, obj, "doCheckThing",new Class[]{String.class,XMLParameter.class,Map.class,Map.class,Map.class,XMLMakeup.class},new Object[]{xml.getId(),parameter,input,output,config,xml},isConcurrent);
                }catch (Exception e){
                    log.error(e.getMessage(), e);
                }
            }else if(proValue.containsKey("delay")){
                Object o = proValue.get("delay");
                if(null != o) {
                    int l=0;
                    if(o instanceof String){
                       l = Integer.parseInt((String)o);
                    }
                    if(o instanceof Integer){
                        l = (Integer)o;
                    }
                    if(l>0) {
                        Object[] objs = (Object[])parameter.getParameter(XMLParameter.XMLLOGIC_BACK_CALL_KEY);
                        log.error("------XMLLOGIC_BACK_CALL_KEY:"+objs);
                        ExecutorUtils.delayWork(xml.getId(),new DoAction(obj,parameter,input,output,config,(Object[])parameter.getParameter(XMLParameter.XMLLOGIC_BACK_CALL_KEY),xml), "doNewContainerAction",null,null,l);
                    }
                }

            }
        }
        return null;
    }



    @Override
    public boolean isAsyn() {
        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void destroy(XMLMakeup xml) throws Exception {
        ExecutorUtils.removeWorkTimeTask(xml.getId());
    }
}
