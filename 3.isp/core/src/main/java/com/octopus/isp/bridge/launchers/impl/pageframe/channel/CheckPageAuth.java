package com.octopus.isp.bridge.launchers.impl.pageframe.channel;

import com.octopus.isp.ds.Context;
import com.octopus.isp.ds.Env;
import com.octopus.isp.ds.RequestParameters;
import com.octopus.isp.ds.Session;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-9-1
 * Time: 上午10:38
 */
public abstract class CheckPageAuth extends XMLDoObject {
    public CheckPageAuth(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    @Override
    public Object doSomeThing(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean checkInput(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        RequestParameters par =(RequestParameters)env;
        String url = par.getRequestURI();
        if(((RequestParameters)env).getRequestProperties().get("Method").equals("GET")) {
            if (null != url) {

                if (null != par.getSession()) {

                    if (!checkAuth(url, par.getSession(), par.getContext(), par.getEnv()))
                        throw new Exception("not auth to access the service " + url);

                    return true;
                } else {
                    throw new Exception("you have not login,can not access the service " + url);
                }

            }
            throw new Exception("not auth to access the service " + url);
        }
        return true;
    }

    protected abstract boolean checkAuth(String action,Session session,Context context,Env env);


    @Override
    public ResultCheck checkReturn(String xmlid,XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return new ResultCheck(true,ret);  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config,Object ret,Exception e) throws Exception {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
