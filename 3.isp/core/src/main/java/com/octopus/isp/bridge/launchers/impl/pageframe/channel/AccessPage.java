package com.octopus.isp.bridge.launchers.impl.pageframe.channel;

import com.octopus.isp.bridge.launchers.impl.pageframe.util.HttpUtils;
import com.octopus.isp.ds.RequestParameters;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-8-21
 * Time: 下午5:41
 */
public class AccessPage extends XMLDoObject {
    transient static Log log = LogFactory.getLog(AccessPage.class);
    public AccessPage(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }
    Map redirects=null;
    @Override
    public Object doSomeThing(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(((RequestParameters)env).getRequestProperties().get("Method").equals("GET")){
            boolean b = false;
            if(null != redirects){
                Iterator its = redirects.keySet().iterator();
                while(its.hasNext()){
                    String uri = (String)its.next();
                    String taruri = (String)redirects.get(uri);
                    if(StringUtils.isNotBlank(uri) && StringUtils.isNotBlank(taruri)){
                        if(env instanceof RequestParameters && ((RequestParameters) env).getRequestURI().indexOf(uri)>=0){
                            String tar = taruri + ((RequestParameters) env).getRequestURL().substring(((RequestParameters) env).getRequestURL().indexOf(uri)+uri.length());
                            if(StringUtils.isNotBlank(((RequestParameters) env).getQueryString())){
                                tar+="?"+((RequestParameters) env).getQueryString();
                            }
                            HttpServletRequest request = (HttpServletRequest)env.get("${request}");
                            HttpServletResponse response = (HttpServletResponse)env.get("${response}");
                            if(log.isDebugEnabled()){
                                log.debug("redirect from "+((RequestParameters) env).getRequestURL()+" to "+tar);
                            }
                            b=true;
                            ((RequestParameters) env).setStop();
                            HttpUtils.setRedirect(env);
                            request.getRequestDispatcher(tar).forward(request,response);

                        }
                    }

                }
            }
            if(!b) {
                HttpUtils hu = (HttpUtils) getObjectById("HttpUtils");
                hu.redirect((RequestParameters) env, null, null);
            }

        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void doInitial() throws Exception {
        XMLMakeup[] xms  = getXML().find("redirect");
        if(null != xms && xms.length>0) {
            redirects = new HashMap();
            for(XMLMakeup x:xms){
                if(StringUtils.isNotBlank(x.getProperties().get("uri")) && StringUtils.isNotBlank(x.getText())) {
                    redirects.put(x.getProperties().get("uri"), x.getText());
                }
            }

        }
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
