package com.octopus.tools.dataclient.impl.engines.impl;

import com.octopus.tools.dataclient.impl.engines.ITyper;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;

/**
 * User: Administrator
 * Date: 14-9-24
 * Time: 下午5:04
 */
public class DBResultTyper extends XMLObject implements ITyper {
    public DBResultTyper(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
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
    public Object typer(Object o) {
        return o;
    }
}
