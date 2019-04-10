package com.octopus.utils.bftask.impl;

import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.bftask.BFParameters;
import com.octopus.utils.bftask.IBFExecutor;
import com.octopus.utils.bftask.IBFTask;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.XMLDoObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: wangfeng2
 * Date: 14-8-22
 * Time: 下午12:36
 */
public class BFTask extends XMLObject implements IBFTask {
    static transient Log log = LogFactory.getLog(BFTask.class);
    XMLDoObject executor;
    public BFTask(XMLMakeup xml,XMLObject parent,Object[] containers) throws Exception {
        super(xml,parent,containers);
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

    public void doTask(BFParameters parameters) throws Exception {
        XMLDoObject exe=null;
        try{
            exe = (XMLDoObject)getPropertyObject("executor");
            execute(exe,"before",parameters,null);
            parameters.setError(false);

        }catch (Exception e){
            //log.error(e.getCause());
            parameters.setError(true);

            parameters.setException(e);
            throw e;
        }finally {
            execute(exe,"after",parameters,null);
        }

    }

    void execute(XMLDoObject exe,String steptype,BFParameters parameters,Throwable error) throws Exception {
        XMLMakeup x = (XMLMakeup)ArrayUtils.getFirst(getXML().getChild(steptype));
        if(null != x){
            List<XMLMakeup> steps = x.getChildren();
            for(XMLMakeup step:steps){
                if(parameters.isStop())return;

                //if(parameters.isInterrupt())break;
                String id = step.getProperties().getProperty("action");
                if(null == id)
                    id= step.getId();
                if(id.startsWith("${")){
                    //如果是调用具体服务,对于key和action是动态的,需要复制该配置为对应的服务,包括action,和key
                    step = step.clone();
                    if(null != step) {
                        id = (String) ObjectUtils.getValueByPath(parameters, id);
                        if(null != id) {
                            step.getProperties().setProperty("action", id);
                            step.getProperties().setProperty("key", id);
                        }
                    }

                }
                if(StringUtils.isBlank(id)) continue;

                if(null != step.getProperties().getProperty("isenable") && !StringUtils.isTrue(step.getProperties().getProperty("isenable"))) continue;

                if(parameters.getJumpTaskList().contains(id)) continue;

                if(log.isDebugEnabled()){
                    System.out.println("begin to do BFtask:"+id);
                }
                parameters.addTaskCode(id);
                if(exe instanceof IBFExecutor) {
                    ((IBFExecutor)exe).execute(step, id, parameters, error);
                }else{
                    Map in= (Map)parameters.get("^${input}");
                    if(null == in){
                        in = new HashMap();
                    }
                    in.put("exe_id",id);
                    in.put("exe_xml",step);
                    in.put("exe_error",error);
                    parameters.put("^${input}",in);
                    /*if("${targetNames}".equals(id)) {
                        exe.doThing(parameters, null);
                    }else{*/
                        in.put("exe_xml",step);
                        exe.doThing(parameters, step);
                    //}
                }
            }
        }
    }

}
