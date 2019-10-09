package com.octopus.isp.actions;

import com.octopus.isp.ds.RequestParameters;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.bftask.BFParameters;
import com.octopus.utils.exception.ExceptionUtil;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * 保存当前的服务流信息和请求数据，等待外界再次触发,只保存流程版本，不保存服务版本。所以下次触发时使用这次保存的的流程服务
 * Created by robai on 2017/11/1.
 */
public class SuspendTheRequest extends XMLDoObject {
    public SuspendTheRequest(XMLMakeup xml, XMLObject parent, Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        String reqSv = env.getTargetNames()[0];
        String reqid = (String)env.get("${requestId}");
        XMLMakeup x = getObjectById(reqSv).getXML();

        String nodeid = (String)config.get("nodeid");

        if(StringUtils.isNotBlank(env.getSuspendXMlId())){
            xmlid = env.getSuspendXMlId();
            if(StringUtils.isBlank(nodeid) && null != getObjectById(xmlid)){
                nodeid=getObjectById(xmlid).getXML().getProperties().getProperty("nodeid");
            }
        }
        String type;//TIME_OUT_BEGIN,TIME_OUT_EXCEPTION,FLOW,EXCEPTION
        //发生超时异常保存redo
        if(env.getStatus()==XMLParameter.TIMEOUT_DELETE){
            type="TIME_OUT_BEGIN";
        }else if(env.getStatus()==XMLParameter.HAPPEN_TIMEOUT){
            type="TIME_OUT_BEGIN";
        }else if(env.getStatus()==XMLParameter.HAPPEN_PREJUDGETIMEOUT){
            type="TIME_OUT_PREJUDGE";
        }else if(env.getStatus()==XMLParameter.HAPPEN_TIMEOUT_EXCEPTION){
            type="TIME_OUT_EXCEPTION";
        }else if(env.getStatus()==XMLParameter.HAPPEN_NOTIFICATION){
            type="NOTIFICATION";
        }else if(xmlid.contains(",interrupt")){
            type="INTERRUPT_POINT";
        }else{
            type="EXCEPTION";
        }
        String state="1";//1.入录，2.处理异常依旧，3.处理完成

        String id = reqSv+"|"+xmlid+"|"+nodeid+"|"+type+"|"+reqid+"|"+env.getLoginUserName()+"|";
        //env.setInterruptPoint(reqSv+"|"+xmlid+"|"+seq+"|"+reqid);
        String envdata = ObjectUtils.convertKeyWithoutThreadNameMap2String(env);
        //save request data in this node
        log.info("staff node id:"+id);
        String error=null;
        if(null != env.getResult() && env.getResult() instanceof ResultCheck && !((ResultCheck)(env.getResult())).isSuccess() ){
            if(((ResultCheck)(env.getResult())).getRet() instanceof Exception) {
                error = ExceptionUtils.getFullStackTrace((Exception) ((ResultCheck) (env.getResult())).getRet());
                if(null != error) {
                    if(error.length()>2000) {
                        error = error.substring(0, 2000);
                    }
                }
            }
        }
        if(null == error && env.isError() && null != env.getException()){
            error = ExceptionUtils.getFullStackTrace((Exception) env.getException());
            if(null != error) {
                if(error.length()>2000) {
                    error = error.substring(0, 2000);
                }
            }
        }
        //env.put("startSuspend", ArrayUtils.toJoinString(env.getTargetNames()) + "|" + x.getId() + "|" + pars. + xmlid);
        String flowbody = x.toString();
        /*String inputdata = ObjectUtils.convertMap2String(input);
        String outputdata = ObjectUtils.convertMap2String(output);
        String configdata = ObjectUtils.convertMap2String(config);
        */
        env.setResult("send to staff node "+id+" , please wait");
        //save this service
        Map map = new HashMap();
        map.put("interruptPoint",id);
        map.put("requestData",envdata);
        map.put("interruptSV",flowbody);
        map.put("svName",reqSv);
        map.put("nodeid",xmlid);
        map.put("error",error);
        map.put("type",type);
        map.put("state",state);
        map.put("user",env.getLoginUserName());
        if(env.getStatus()==XMLParameter.TIMEOUT_DELETE) {
            map.put("op", "delete");
        }else{
            map.put("op","");
        }
        return map;

    }

    @Override
    public ResultCheck checkReturn(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return new ResultCheck(true,ret);
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret, Exception e) throws Exception {
        return false;
    }
}
