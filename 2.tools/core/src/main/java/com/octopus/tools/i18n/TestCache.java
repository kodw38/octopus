package com.octopus.tools.i18n;

import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * User: wangfeng2
 * Date: 14-8-19
 * Time: 上午12:09
 */
public class TestCache extends XMLObject implements ICache {
    List li = new ArrayList();

    public TestCache(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
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
    public Object get(Properties locale) {
        if(li.size()>0)
        return li.get(0);
        return null;
    }

    @Override
    public void add(Object o, Properties locale) {
        li.add(o);
    }

    @Override
    public Object getAll() {
        return li;
    }
}
