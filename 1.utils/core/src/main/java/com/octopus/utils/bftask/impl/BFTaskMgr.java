package com.octopus.utils.bftask.impl;

import com.octopus.utils.bftask.BFParameters;
import com.octopus.utils.bftask.IBFExecutor;
import com.octopus.utils.bftask.IBFTaskMgr;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;

import java.util.HashMap;
import java.util.Map;

/**
 * User: wangfeng2
 * Date: 14-8-22
 * Time: 下午2:30
 */
public class BFTaskMgr extends XMLObject implements IBFTaskMgr {
    IBFExecutor executor=null;
    Map<String,BFTask> tasks =new HashMap();

    public BFTaskMgr(XMLMakeup xml,XMLObject parent,Object[] containers) throws Exception {
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

    public boolean isExist(String taskkey){
        return tasks.containsKey(taskkey);
    }

    public void doTask(String taskkey,BFParameters parameters) throws Exception {
        tasks.get(taskkey).doTask(parameters);
    }

}
