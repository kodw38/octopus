package com.octopus.isp.bridge.launchers.impl.pageframe.channel;

import com.octopus.isp.ds.RequestParameters;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-9-1
 * Time: 上午10:40
 */
public class IsAction extends XMLDoObject {
    public IsAction(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    @Override
    public Object doSomeThing(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        RequestParameters par =(RequestParameters)env;
        HttpServletRequest request = (HttpServletRequest)env.getParameter("${request}");
        String uri = request.getRequestURI();
        String requestPath = uri.substring(request.getContextPath().length());

        if(requestPath.endsWith("/service")){
            //获取归属的页面信息
            par.setStop();
            return true;

        }
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResultCheck checkReturn(String xmlid,XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
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
