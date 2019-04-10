package com.octopus.isp.bridge.launchers.out;

import com.octopus.isp.bridge.IBridge;
import com.octopus.isp.bridge.ILauncher;
import com.octopus.isp.bridge.launchers.impl.pageframe.util.HttpUtils;
import com.octopus.isp.ds.Env;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.file.FileUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.XMLUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

/**
 * User: Administrator
 * Date: 14-9-3
 * Time: 下午5:56
 */
public class HttpServletFilter implements Filter {
    static transient Log log = LogFactory.getLog(HttpServletFilter.class);
    ILauncher launcher;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        try {
            String bridgePath = filterConfig.getInitParameter("isp_path");
            if(StringUtils.isBlank(bridgePath))
                bridgePath = "classpath:com/octopus/isp/bridge/bridge.xml";
            IBridge bridge = (IBridge) XMLObject.loadApplication(bridgePath,null,true,true);
            launcher = bridge.getLauncher("web");
            launcher.addEnv(Env.KEY_HOME,getHome(filterConfig));

        } catch (Exception e) {
            log.error("",e);
        }
    }

    String getHome(FilterConfig filterConfig){
        try{
        InputStream inputStream = filterConfig.getServletContext().getResourceAsStream("/WEB-INF/web.xml");
        if(null != inputStream){
            StringBuffer sb = FileUtils.getFileContentStringBuffer(inputStream);
            XMLMakeup xml = XMLUtil.getDataFromString(sb.toString());
            if(null  != xml){
                XMLMakeup[] ws = xml.getChild("welcome-file-list");
                if(null != ws && ws.length>0){
                    XMLMakeup[] fs =ws[0].getChild("welcome-file");
                    if(null != fs && fs.length>0)
                        return fs[0].getText();
                }
            }

        }else{
            log.error("not read welcome-file config");
        }
        }catch (Exception e){

        }
        return "";
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse)servletResponse;
        HttpServletRequest request = (HttpServletRequest)servletRequest;

        long l = System.currentTimeMillis();
        try{
            Object ret = launcher.invoke(new Object[]{servletRequest,servletResponse});
            log.debug(ret);

            if(null != ret){
                String rsp = ret.toString();
                if(rsp.startsWith("{") || rsp.startsWith("["))
                    response.setContentType("application/json;charset=UTF-8");
                else if(ret instanceof String && ((String)ret).startsWith("<script")){
                    response.setContentType("text/html;charset=UTF-8");
                }else if(ret instanceof String){
                    response.setContentType("text;charset=UTF-8");
                }else {
                    response.setContentType("application/text;charset=UTF-8");
                }
                response.getOutputStream().write(ret.toString().getBytes("UTF-8"));
                response.flushBuffer();
            }else if(!response.isCommitted()){
                //System.out.println("--websocket--");
                request.getSession().setAttribute("WEB-Launcher",launcher);

                filterChain.doFilter(request,response);
            }
        }catch(Exception e){

            log.error("request error:"+request.getRequestURL().toString(),e);
            if(null != launcher) {
                HttpUtils.redirectError((XMLObject) launcher, request, response, "pc", e);
            }
        }finally {
            //if(log.isInfoEnabled()) {
                log.error("http costTime:" + (System.currentTimeMillis() - l));
            //}
        }
    }

    @Override
    public void destroy() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

}
