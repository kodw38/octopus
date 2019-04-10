package com.octopus.isp.actions;

import com.octopus.isp.ds.Context;
import com.octopus.isp.ds.Env;
import com.octopus.isp.ds.RequestParameters;
import com.octopus.isp.ds.Session;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-9-1
 * Time: 上午10:55
 */
public abstract class CheckAuth extends XMLDoObject {
    List<String> uncheck = new ArrayList<String>();
    public CheckAuth(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);

        XMLMakeup xs = (XMLMakeup)ArrayUtils.getFirst(xml.getChild("uncheckactions"));
        if(null != xs){
            XMLMakeup[] s = xs.getChild("property");
            for(XMLMakeup x:s){
                if(null != x){
                    uncheck.add(x.getText());
                }
            }
        }
    }

    @Override
    public Object doSomeThing(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean checkInput(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        RequestParameters par =(RequestParameters)env;
        String[] ss = par.getTargetNames();
        if(null != ss){
            if(uncheck.containsAll(Arrays.asList(ss))){
                return true;
            }else{
                if(null !=par.getSession()){
                    for(String s:ss){
                        if(!checkAuth(s,par.getSession(),par.getContext(),par.getEnv()))
                            throw new Exception("not auth to access the service "+ss);
                    }
                    return true;
                }else{
                    throw new Exception("you have not login,can not access the service "+ArrayUtils.toString(ss));
                }
            }
        }else{
            ((RequestParameters) env).setStop();
            return true;
        }
        //throw new Exception("not auth to access the service "+ss);
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
