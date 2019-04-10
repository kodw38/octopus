package com.octopus.utils.flow.impl;

import com.octopus.utils.bftask.IBFExecutor;
import com.octopus.utils.flow.FlowParameters;
import com.octopus.utils.flow.IExpress;
import com.octopus.utils.flow.IFlow;
import com.octopus.utils.flow.IFlowMgr;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.LinkedHashMap;

/**
 * User: Administrator
 * Date: 14-8-23
 * Time: 下午4:07
 */
public class FlowMgr extends XMLObject implements IFlowMgr{
    LinkedHashMap<String,IFlow> flows;
    static transient Log log = LogFactory.getLog(FlowMgr.class);

    IExpress express;
    IBFExecutor executor;

    public FlowMgr(XMLMakeup xml,XMLObject parent,Object[] containers) throws Exception {
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

    public boolean isExist(String flowid){
        return flows.containsKey(flowid);
    }

    public void doFlow(String flowid,FlowParameters parameters)throws Exception{
        if(flows.containsKey(flowid)){
            if(log.isDebugEnabled()){
               System.out.println("execute flow :"+flowid);
            }
            flows.get(flowid).doFlow(parameters);
        }else{
            throw new Exception("not exist the flow id :"+flowid);
        }
    }
    public void doFirstFlow(FlowParameters parameters)throws Exception{
        if(flows.size()>0){
            String key = flows.keySet().iterator().next();
            if(log.isDebugEnabled()){
                System.out.println("execute first flow :"+key);
            }
            flows.get(key).doFlow(parameters);
        }else{
            throw new Exception("not config the flow");
        }
    }
}
