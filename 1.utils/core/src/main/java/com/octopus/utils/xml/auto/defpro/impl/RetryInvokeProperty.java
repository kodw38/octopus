package com.octopus.utils.xml.auto.defpro.impl;

import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import com.octopus.utils.xml.auto.defpro.IObjectInvokeProperty;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.SocketTimeoutException;
import java.util.Iterator;
import java.util.Map;

/**
 * User: Administrator
 * Date: 15-4-1
 * Time: 下午8:50
 */
public class RetryInvokeProperty implements IObjectInvokeProperty {
    transient static Log log = LogFactory.getLog(RetryInvokeProperty.class);
    @Override
    public boolean isAsyn() {
        return false;
    }

    @Override
    public Object exeProperty(Map proValue, XMLDoObject obj,XMLMakeup xml, XMLParameter parameter, Map input,Map output, Map config) {
        if(null != proValue){
            Object t = proValue.get("times");
            int times = 0;
            if(null != t){
                if(t instanceof String)
                    times=Integer.parseInt((String)t);
                else
                    times = (Integer)t;
            }

            Object i = proValue.get("interval");
            int interval = 0;
            if(null != i){
                if(i instanceof String)
                interval=Integer.parseInt((String)i);
                else
                    interval =(Integer)i;
            }
            while(times!=0){
                try{
                    if(times>0)
                        times--;
                    obj.doCheckThing(xml.getId(),parameter,  input,  output,config,xml);
                    if(parameter.getResult() instanceof ResultCheck && !((ResultCheck)parameter.getResult()).isSuccess()){
                        if(t instanceof String)
                        log.error("retry: "+(Integer.parseInt((String)t)-times));
                        else
                            log.error("retry: "+((Integer)t-times));

                        if(interval>0){
                            try{
                                Thread.sleep(interval);
                            }catch (Throwable e){
                                log.error(e.getMessage(), e);
                            }
                        }
                        continue;
                    }
                    if(parameter.containsParameter(XMLParameter.XMLLOGIC_BACK_CALL_KEY)){
                        //入参带入回调方法中
                        if(null != parameter && null != input){
                            Iterator its = input.keySet().iterator();
                            while(its.hasNext()){
                                String k = (String)its.next();
                                parameter.addParameter("${"+k+"}",input.get(k));
                            }
                        }
                        Object[] os = (Object[])parameter.getParameter(XMLParameter.XMLLOGIC_BACK_CALL_KEY);
                        if(null != os){
                            try{
                                ExecutorUtils.synWork((Object) os[0], (String) os[1], (Class[]) os[2], (Object[]) os[3]);
                            }catch (SocketTimeoutException ex){
                                if(interval>0){
                                    try{
                                        Thread.sleep(interval);
                                    }catch (Exception e){
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                    return null;
                }catch (Throwable ex){
                    if(t instanceof  String)
                    log.error("retry: "+(Integer.parseInt((String)t)-times),ex);
                    else
                        log.error("retry: "+((Integer)t-times),ex);
                    if(interval>0){
                        try{
                        Thread.sleep(interval);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return null;
    }

}
