package com.octopus.isp.bridge.launchers.impl.pageframe.channel;/**
 * Created by admin on 2020/7/17.
 */

import com.octopus.isp.ds.RequestParameters;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.http.HttpRequest;

import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServletRequest;
import java.util.Iterator;
import java.util.Map;

/**
 * 如果配置了自己的servelt,该类将bridge调用结束，留给后续的servlet处理
 * @ClassName MatchServletStopBridge
 * @Description ToDo
 * @Author Kod Wong
 * @Date 2020/7/17 10:12
 * @Version 1.0
 **/
public class MatchServletStopBridge extends XMLDoObject {
    public MatchServletStopBridge(XMLMakeup xml, XMLObject parent, Object[] containers) throws Exception {
        super(xml, parent, containers);
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {

        String s = ((RequestParameters)env).getRequestURI();
        HttpServletRequest req = ((HttpServletRequest)env.getParameter("${request}"));
        Map m = req.getServletContext().getServletRegistrations();
        if(null != m) {
            Iterator its= m.keySet().iterator();
            while(its.hasNext()) {
                ServletRegistration sr = (ServletRegistration)m.get(its.next());
                if(!sr.getClassName().startsWith("org.eclipse")) {//filter container servlet
                    if (ArrayUtils.isLikeArrayInString(s, sr.getMappings())) {
                        ((RequestParameters) env).setStop();
                        req.setAttribute("RequestParameters", env);
                    }
                }
            }
        }
        return true;
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return null;
    }

    @Override
    public ResultCheck checkReturn(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return new ResultCheck(true,ret);
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret, Exception e) throws Exception {
        return false;
    }
}
