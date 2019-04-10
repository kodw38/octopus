package com.octopus.tools.i18n;

import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;

/**
 * User: wangfeng2
 * Date: 14-8-19
 * Time: 上午12:13
 */
public class TestExist extends XMLObject implements IExistStyle {
    public TestExist(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
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
    public Object export(Object all) {
        System.out.println(all);
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
