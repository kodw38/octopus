package com.octopus.tools.i18n;

import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;

/**
 * User: wangfeng2
 * Date: 14-8-19
 * Time: 上午12:09
 */
public class TestFormate extends XMLObject implements IFormat {
    public TestFormate(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
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
    public Object formate(Object o) {
        System.out.println(o);
        return "2014/08/18";
    }

    @Override
    public Object formate2System(Object o) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
