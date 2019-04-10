package com.octopus.octopus.actions;

import com.octopus.isp.bridge.launchers.impl.pageframe.channel.CheckPageAuth;
import com.octopus.isp.ds.Context;
import com.octopus.isp.ds.Env;
import com.octopus.isp.ds.Session;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-9-1
 * Time: 下午5:13
 */
public class CheckPageUrlAuth extends CheckPageAuth {
    public CheckPageUrlAuth(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    @Override
    protected boolean checkAuth(String action, Session session, Context context, Env env) {
        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }
}
