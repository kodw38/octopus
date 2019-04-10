package com.octopus.isp.executors;

import com.octopus.utils.bftask.BFParameters;
import com.octopus.utils.bftask.IBFExecutor;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;

/**
 * User: Administrator
 * Date: 14-9-3
 * Time: 上午10:28
 */
public class XMLDoObjectExecutor extends XMLObject implements IBFExecutor{

    public XMLDoObjectExecutor(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
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
    public void execute(XMLMakeup xml,String action, BFParameters parameters, Throwable error) throws Exception {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
