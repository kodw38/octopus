package com.octopus.isp.bridge.launchers.impl.pageframe.channel;

import com.octopus.isp.bridge.launchers.impl.pageframe.util.HttpUtils;
import com.octopus.isp.ds.Env;
import com.octopus.isp.ds.RequestParameters;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-8-20
 * Time: 下午2:23
 */
public class URLHome extends XMLDoObject {
    public URLHome(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    @Override
    public Object doSomeThing(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        RequestParameters param = (RequestParameters) env;
        HttpServletRequest request = (HttpServletRequest)param.get("${request}");
        HttpServletResponse response = (HttpServletResponse)param.get("${response}");
        String url = request.getRequestURI().substring(request.getContextPath().length());
        if(url.equals("/") || StringUtils.isBlank(url)){
            //定向到home目录
            XMLMakeup[] home = getXML().getChild("home");
            if(ArrayUtils.isNotEmpty(home)){
                if(StringUtils.isNotBlank(home[0].getText())){
                    ((RequestParameters)env).getEnv().addParameter(Env.KEY_HOME,home[0].getText());

                }
            }
            String homepage = (String)((RequestParameters)env).getEnv().getParameter(Env.KEY_HOME);
            if(null == homepage)
                throw new Exception("please config home page.");
            String reurl = "/"+homepage;
            HttpUtils httpUtils = (HttpUtils)getObjectById("HttpUtils");
            httpUtils.redirect(param,reurl,null);

            //param.setLoginCheckURL(false);
        }
        return null;
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
        return new ResultCheck(true,null);
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
