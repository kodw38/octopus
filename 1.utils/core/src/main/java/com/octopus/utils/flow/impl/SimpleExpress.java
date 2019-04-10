package com.octopus.utils.flow.impl;

import com.octopus.utils.bftask.BFParameters;
import com.octopus.utils.flow.IExpress;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;

/**
 * User: Administrator
 * Date: 14-8-23
 * Time: 下午10:11
 */
public class SimpleExpress extends XMLObject implements IExpress {
    public SimpleExpress(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
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

    @Override
    public double express(String exp, BFParameters parameters) {
        if(parameters.getNextTask()!=0){
            return parameters.getNextTask();
        }
        return Double.parseDouble(exp);
    }
}
