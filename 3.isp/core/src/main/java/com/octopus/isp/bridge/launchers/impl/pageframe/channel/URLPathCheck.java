package com.octopus.isp.bridge.launchers.impl.pageframe.channel;

import com.octopus.isp.bridge.launchers.impl.pageframe.util.HttpUtils;
import com.octopus.isp.ds.RequestParameters;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: wfgao_000
 * Date: 15-8-19
 * Time: 下午2:29
 */
public class URLPathCheck extends XMLDoObject {
    private static HashMap<String,byte[]> redirectPageCache = new HashMap<String, byte[]>();
    private static List<String> UNCHECK_URL = new ArrayList<String>();

    public URLPathCheck(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        XMLMakeup[] ms = xml.getChild("uncheckurl")[0].getChild("property");
        for(XMLMakeup m:ms){
            if(StringUtils.isNotBlank(m.getText()))
                UNCHECK_URL.add(m.getText());
        }
    }
    static HashMap<String,Pattern> urlpattern = new HashMap();
    boolean checkPattern(String pattern,String value){
        if(pattern.charAt(0)=='^' && value.contains("?")) {
            Pattern p = urlpattern.get(pattern);
            if (null == p) {
                p = Pattern.compile(pattern);
                urlpattern.put(pattern, p);
            }
            Matcher is = p.matcher(value);
            if (is.matches()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Object doSomeThing(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        RequestParameters param  =(RequestParameters)env;
        HttpServletRequest request = (HttpServletRequest)env.getParameter("${request}");
        String url = request.getRequestURI().substring(request.getContextPath().length());
        String hp = param.getClientInfo().getHeaderPath();
        if(null != hp && url.startsWith(hp)){
            url = url.substring(hp.length());
        }
        boolean isun=false;
        for (String key: UNCHECK_URL) {
            if (url.indexOf(key) > -1) {
                if(url.contains(".")){
                    HttpUtils httpUtils = (HttpUtils)getObjectById("HttpUtils");
                    httpUtils.redirect(param,null,null);
                }
                ((RequestParameters) env).setStop();
            }
            if(checkPattern(key,url+"?"+param.getQueryString())){
                ((RequestParameters) env).setInterrupt();
                ((RequestParameters) env).setNextTask(2);
            }

        }

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
