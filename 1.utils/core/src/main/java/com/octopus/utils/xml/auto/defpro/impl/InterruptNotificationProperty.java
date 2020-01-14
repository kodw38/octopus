package com.octopus.utils.xml.auto.defpro.impl;

import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.exception.ISPException;
import com.octopus.utils.time.DateTimeUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import com.octopus.utils.xml.auto.defpro.IObjectInvokeProperty;
import com.octopus.utils.xml.auto.defpro.impl.utils.DoAction;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 如果设置了interruptnotification=true属性，执行到这里时，将中断，抛出等待信息。
 * 当前请求信息走requestsuspend，并发送消息到kafka，抛出异常信息给使用者。
 kafka处理任务接收到该信息时发送信息给使用者，表明在处理，处理完成后，激活之前的请求设置该节点返回值。再发送信息给使用者
 * Created by Administrator on 2018/9/18.
 */
public class InterruptNotificationProperty implements IObjectInvokeProperty  {
        static transient Log log = LogFactory.getLog(InterruptNotificationProperty.class);
        @Override
        /**
         * if return false , the object will continue execute as old process
         */
    public Object exeProperty(Map proValue, XMLDoObject obj, XMLMakeup xml, XMLParameter parameter, Map input, Map output, Map config)throws Exception{
        if(log.isDebugEnabled()){
            log.debug("isRedoService:"+parameter.isRedoService()+"; current Do xmlid:"+xml.getId()+","+obj.getXML().getId()+"; suspendXmlID:"+parameter.getSuspendXMlId());
        }
        if(!parameter.isRedoService() ||(parameter.isRedoService() && !(xml.getId()+","+obj.getXML().getId()).equals(parameter.getSuspendXMlId()))) {
            //suspend current request
            try {
                String id = suspendRequest(parameter, obj, xml,config);
                if (StringUtils.isNotBlank(id) && (!proValue.containsKey("isContinueAndNotify")
                        || ((proValue.get("isContinueAndNotify") instanceof String && StringUtils.isTrue((String)proValue.get("isContinueAndNotify"))) || (Boolean)proValue.get("isContinueAndNotify") ) )) {
                    //send suspend message to kafka
                    sendKafka(parameter, obj, id,xml);

                }
            } catch (Exception e) {
                log.error("", e);
                try {
                    DoAction doA = new DoAction(obj, parameter, input, output, config, (Object[]) parameter.getParameter(XMLParameter.XMLLOGIC_BACK_CALL_KEY), xml);
                    doA.doAction();
                    //obj.doCheckThing(xml.getId(),parameter,  input,  output,config,xml);
                    if(null != parameter.getResult()) {
                        if(parameter.getResult() instanceof ResultCheck) {
                            ResultCheck rc = (ResultCheck) parameter.getResult();
                            if (null != rc) {
                                return rc.getRet();
                            } else {
                                return null;
                            }
                        }else{
                            return parameter.getResult();
                        }
                    }else{
                        return null;
                    }
                } catch (Exception e1) {
                    throw e1;
                }

            }
            //parameter.setError(true);
            //Exception e = new ISPException("500", "interrupt notification for " + xml.getId());
            //parameter.setException(e);
            throw new ISPException("500", "interrupt notification for " + xml.getId());
        }else{
            return XMLDoObject.NOT_EXE_PROPERTY;
        }
    }


    @Override
    public boolean isAsyn() {
        return false;
    }
    Object sendKafka(XMLParameter parameter,XMLDoObject obj,String id,XMLMakeup xml)throws Exception{
        XMLDoObject queue = (XMLDoObject)obj.getObjectById("kafkaClient");
        //let asyn continue to do , get data from queue
        HashMap in = new HashMap();
        in.put("op","send");
        in.put("topic","interruptNotification");
        in.put("key",id);
        String user = (String)parameter.getValueFromExpress("${session}.UserName",null);
        String action = parameter.getTargetNames()[0];
        String point = xml.getId() + "[" + obj.getXML().getId()+"]";
        String status = "start";
        String value = "{\"user\":\""+user+"\",\"type\":\""+action+"\",\"point\":\""+"background:"+point+"\",\"status\":\""+status+"\",\"date\":\""+ DateTimeUtils.getCurrDateTime()+"\"}";
        in.put("value",value);
        Object r = queue.doSomeThing(null,parameter,in,null,null);
        //send a interrupt message to user
        if(log.isInfoEnabled()){
            log.info("sended interrupt message to interruptNotification of kafka topic");
        }
        in.put("topic","interruptNotificationMessage");
        in.put("key",id);
        in.put("value",value);
        queue.doSomeThing(null,parameter,in,null,null);
        if(log.isInfoEnabled()){
            log.info("sended interrupt message to interruptNotificationMessage of kafka topic");
        }
        return r;
    }

    String suspendRequest(XMLParameter parameter,XMLDoObject obj,XMLMakeup xml,Map config) throws Exception {
        Object call =null;
        try {
            if((null == config || !config.containsKey("notification")) ){
                String v = "{action:'finishedInterruptNotification',op:'send',isAsyn:'false',data:{user:'${session}.UserName',type:'${targetNames}[0]',date:'(${systemdate})',point:'"+xml.getId()+"["+obj.getXML().getId()+"]'}}";
                String str = "{notification:"+v+"}";
                if(!xml.getProperties().containsKey("config")){
                    xml.getProperties().setProperty("config", str);
                }else{
                    String cnf = xml.getProperties().getProperty("config");
                    Map m = StringUtils.convert2MapJSONObject(cnf);
                    m.put("notification",v);
                    xml.getProperties().setProperty("config", ObjectUtils.convertMap2String(m));
                }
            }
            call = parameter.getParameter(XMLParameter.XMLLOGIC_BACK_CALL_KEY);
            parameter.removeParameter(XMLParameter.XMLLOGIC_BACK_CALL_KEY);
            if (!parameter.isSuspend()) {
                parameter.setSuspendXMlId(xml.getId() + "," + obj.getXML().getId());////这里使用的是别名和实际服务名称，方便主服务查找，重做
                //parameter.setTimeoutXMlId(xmlid + "," + getXML().getId());////这里使用的是别名和实际服务名称，方便主服务查找，重做
                parameter.setStatus(XMLParameter.HAPPEN_NOTIFICATION);
                XMLDoObject save = (XMLDoObject) obj.getObjectById((String) ((Map) parameter.getParameter("${env}")).get("saveRedoService"));
                //parameter.setException(e);
                //parameter.setError(true);
                save.doThing(parameter, null);
                ResultCheck rc = (ResultCheck) parameter.getResult();
                if(log.isInfoEnabled()){
                    log.info("suspend current request:"+rc.getRet());
                }
                return (String) rc.getRet();
            }
        }catch (Exception e){
            parameter.setSuspendXMlId(null);
            parameter.setStatus(0);
            throw e;
        }finally {
            if(null != call)
            parameter.addParameter(XMLParameter.XMLLOGIC_BACK_CALL_KEY,call);
        }
        return null;
    }
}
