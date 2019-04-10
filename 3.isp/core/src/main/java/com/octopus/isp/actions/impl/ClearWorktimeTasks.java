package com.octopus.isp.actions.impl;

import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.List;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-8-19
 * Time: 下午5:16
 */
public class ClearWorktimeTasks extends XMLDoObject {
    public ClearWorktimeTasks(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    @Override
    public Object doSomeThing(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        String[] exc = null;
        if(null != input) {
            Object o = input.get("exclude");
            if (null != o) {
                if (o.getClass().isArray()) {
                    exc = (String[]) o;
                } else if (o instanceof List) {
                    exc = (String[]) ((List) o).toArray(new String[0]);

                } else {
                    exc = new String[]{(String) o};
                }
            }
        }
        ExecutorUtils.clearAllWorkTimeTasks(exc);
        return null;
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResultCheck checkReturn(String xmlid,XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return new ResultCheck(true,null);  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config,Object ret,Exception e) throws Exception {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
