package com.octopus.isp.bridge.launchers.impl.pageframe.channel;

import com.octopus.isp.ds.RequestParameters;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * User: wfgao_000
 * Date: 15-8-18
 * Time: 上午4:33
 */
public class URLSecurityCheck extends XMLDoObject {
    private static Pattern PATTERN = null;
    public URLSecurityCheck(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        PATTERN=Pattern.compile(xml.getText());
    }

    @Override
    public Object doSomeThing(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        RequestParameters par = (RequestParameters)env;
        //if has login set SessionManager User for log
        /*if(null == SessionManager.getUser() && null != par.getRequest().getSession() && null !=par.getRequest().getSession().getAttribute("sessionData")){
            SessionData data = (SessionData)par.getRequest().getSession().getAttribute("sessionData");
            if(null != data && null != data.getUserInfo()){
                SessionManager.setUser(data.getUserInfo());
            }
        }
*/
        String query = par.getQueryString();
        String uri = par.getRequestURI();
        if(StringUtils.isBlank(query) && uri.length()>1){
            HttpServletRequest request = (HttpServletRequest)env.get("${request}");
            URL u = request.getServletContext().getResource(uri);
            if(null !=u){
                if(new File(u.getFile()).isDirectory()){
                    String msg = "can not access directory:"+u.getFile();
                    throw new Exception(msg);
                }
            }
        }
        if ((!(StringUtils.isBlank(query)))&& (PATTERN.matcher(StringUtils.escapeURLDecode(query.toLowerCase())).find())) {
            String msg = "the queryString is not security:"+query;
            throw new Exception(msg);
        }
        if (StringUtils.isNotBlank(uri) && uri.length()>1 && PATTERN.matcher(StringUtils.escapeURLDecode(uri.toLowerCase())).find()) {
            String msg = "the uri is not security:"+uri;
            throw new Exception(msg);
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
