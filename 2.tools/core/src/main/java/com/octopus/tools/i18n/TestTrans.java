package com.octopus.tools.i18n;

import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;

import java.util.Properties;

/**
 * User: wangfeng2
 * Date: 14-8-19
 * Time: 上午12:08
 */
public class TestTrans extends XMLObject implements ITransport {
    public TestTrans(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
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
    public Object transport(Object o,Properties locale) {
        System.out.println(o);
        return "2014-08-18 00:00:00";
    }

    @Override
    public Object transport2System(Object o, Properties locale) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
