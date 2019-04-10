package com.octopus.isp.bridge.launchers.impl.pageframe;

import com.octopus.isp.ds.RequestParameters;
import com.octopus.utils.flow.FlowParameters;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-8-24
 * Time: 上午9:49
 */
public class PageFrameError extends XMLDoObject{
    public PageFrameError(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    @Override
    public Object doSomeThing(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(env instanceof RequestParameters) {
            RequestParameters rp = (RequestParameters) env;
            rp.setStop();
            log.error("",rp.getException());
            return rp.getException();
        }else if(env instanceof FlowParameters){
            FlowParameters rp = (FlowParameters)env;
            rp.setStop();
            return rp.getException();
        }else{
            throw  new Exception("not support env type ["+env.getClass().getName()+"]");
        }
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;
    }

    @Override
    public ResultCheck checkReturn(String xmlid,XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return new ResultCheck(false,ret);
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
