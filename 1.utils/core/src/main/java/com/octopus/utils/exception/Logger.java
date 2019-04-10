package com.octopus.utils.exception;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cls.ClassUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.helpers.*;
import org.apache.log4j.spi.LocationInfo;

import javax.servlet.http.HttpServletRequest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Created by robai on 2018/1/14.
 */
public class Logger {
    static transient org.apache.commons.logging.Log log = LogFactory.getLog(Logger.class);
    static DateFormat dateFormat=null;

    public static String getString(XMLParameter pars,String srvName,String msg,String clazz,Throwable e){
        return getString(pars,srvName,msg,clazz,null,e);
    }
    //[date][threadname][serverip][insid][accType][clientip][sessionid][userid][reqid][srvid][input][cls:line][msg][errcode][errmsg]
    public static String getString(XMLParameter pars,String srvName,String msg,String clazz,Map in,Throwable e){
        StringBuffer sb = new StringBuffer();
        if(null == dateFormat){
            dateFormat = getDateFormat();
        }
        String curdate = dateFormat.format(new Date());
        String threadName = Thread.currentThread().getName();
        String serverip = "",insid="",reqid="",accType="",sessionid="",input="",userid="",clientip="",srvid="",errcode="",errmsg="",cls="",line="";

        if(null != pars){
            if(null != srvName){
                srvid=srvName;
            }else {
                String[] ts = pars.getTargetNames();
                if (null != ts && ts.length == 1) {
                    srvid = ts[0];
                }
            }
            if(null != in){
                input=in.toString();
            }else{
                Object o = pars.getParameter("${input_data}");
                if (null != o) {
                    input = o.toString();
                }
            }

            reqid = (String)pars.getParameter("${requestId}");
            Map clinfo = (Map)pars.getParameter("${clientInfo}");
            if(null != clinfo) {
                clientip = (String) clinfo.get("ClientIp");
            }
            if(StringUtils.isNotBlank(reqid)) {
                accType = reqid.substring(0, reqid.indexOf("`"));
                if("WEB".equals(accType)) {
                    Object se = pars.getParameter("${session}");
                    if (null != se && se instanceof Map) {
                        userid=(String)((Map)se).get("UserName");
                        sessionid=(String)((Map)se).get("KEY_SESSION_ID");

                    }
                }
            }
            Map d = (Map)pars.getParameter("${env}");
            if(null != d){
                serverip = (String)d.get("${ip}");
                insid = (String)d.get("${local_instanceId}");
            }
        }
        if(null !=e){
            ErrMsg em = ExceptionUtil.getMsg(e);
            if(StringUtils.isNotBlank(em.getCode()))
                errcode=em.getCode();
            if(StringUtils.isNotBlank(em.getMsg()))
                errmsg=em.getMsg();
        }
        if(null !=e) {
            LocationInfo locationInfo = new LocationInfo(e, clazz);
            cls=locationInfo.getFileName();
            line=locationInfo.getLineNumber()+"";
        }

        sb.append("[").append(curdate).append("]")
                .append("[").append(threadName).append("]")
                .append("[").append(serverip).append("]")
                .append("[").append(insid).append("]")
                .append("[").append(accType).append("]")
                .append("[").append(clientip).append("]")
                .append("[").append(sessionid).append("]")
                .append("[").append(userid).append("]")
                .append("[").append(reqid).append("]")
                .append("[").append(srvid).append("]")
                .append("[").append(input.toString()).append("]")
                .append("[").append(cls).append(":").append(line).append("]")
                .append("[").append(msg==null?"":msg).append("]")
                .append("[").append(errcode).append("]")
                .append("[").append(errmsg).append("]")
                ;

        return sb.toString();
    }
    //[date][threadname][serverip][insid][accType][clientip][sessionid][userid][reqid][srvid][input][errcode][errmsg]
    public static String getTraceString(XMLParameter pars,Throwable e){
        StringBuffer sb = new StringBuffer("[TRADE]");
        if(null == dateFormat){
            dateFormat = getDateFormat();
        }
        String curdate = dateFormat.format(new Date());
        String threadName = Thread.currentThread().getName();
        String serverip = "",insid="",reqid="",accType="",sessionid="",input="",userid="",clientip="",srvid="",errcode="",errmsg="",cls="",line="";

        if(null != pars){
            String[] ts = pars.getTargetNames();
            if(null != ts && ts.length==1){
                srvid=ts[0];
            }
            Object o = pars.getParameter("${input_data}");
            if(null !=o){
                input=o.toString();
            }
            reqid = (String)pars.getParameter("${requestId}");
            Map clinfo = (Map)pars.getParameter("${clientInfo}");
            if(null != clinfo) {
                clientip = (String) clinfo.get("ClientIp");
            }
            if(StringUtils.isNotBlank(reqid)) {
                accType = reqid.substring(0, reqid.indexOf("`"));
                if("WEB".equals(accType)) {
                    Object se = pars.getParameter("${session}");
                    if (null != se && se instanceof Map) {
                        userid=(String)((Map)se).get("UserName");
                        sessionid=(String)((Map)se).get("KEY_SESSION_ID");

                    }
                }
            }
            Map d = (Map)pars.getParameter("${env}");
            if(null != d){
                serverip = (String)d.get("${ip}");
                insid = (String)d.get("${local_instanceId}");
            }
        }
        if(null !=e){
            ErrMsg em = ExceptionUtil.getMsg(e);
            if(StringUtils.isNotBlank(em.getCode()))
                errcode=em.getCode();
            if(StringUtils.isNotBlank(em.getMsg()))
                errmsg=em.getMsg();
        }


        sb.append("[").append(curdate).append("]")
                .append("[").append(threadName).append("]")
                .append("[").append(serverip).append("]")
                .append("[").append(insid).append("]")
                .append("[").append(accType).append("]")
                .append("[").append(clientip).append("]")
                .append("[").append(sessionid).append("]")
                .append("[").append(userid).append("]")
                .append("[").append(reqid).append("]")
                .append("[").append(srvid).append("]")
                .append("[").append(input).append("]")
                .append("[").append(errcode).append("]")
                .append("[").append(errmsg).append("]")
                ;

        return sb.toString();
    }
    public static String getString(HttpServletRequest request){
        if(null != request){
            StringBuffer sb = new StringBuffer();
            sb.append("[").append(Logger.getDateFormat().format(new Date())).append("]");
            sb.append("[").append(Thread.currentThread().getName()).append("]");
            sb.append("[").append(request.getRemoteAddr()).append("]");
            sb.append("[").append(request.getRequestURL().toString()).append("]");
            return sb.toString();
        }
        return "";
    }
    public static void error(Class c,XMLParameter pars,String id,String msg,Map input,Throwable e){
        if(log.isErrorEnabled()) {
            log.error(getString(pars, id, msg,c.getName(),input,e), e);
        }
    }
    public static void error(Class c,XMLParameter pars,String id,String msg,Throwable e){
        if(log.isErrorEnabled()) {
            log.error(getString(pars, id, msg,c.getName(),null,e), e);
        }
    }
    public static void debug(Class c,XMLParameter pars,String id,String msg,Map input,Throwable e){
        if(log.isDebugEnabled()){
            log.debug(getString(pars,id,msg,c.getName(),input,e),e);
        }
    }
    public static void debug(Class c,XMLParameter pars,String id,String msg,Throwable e){
        if(log.isDebugEnabled()){
            log.debug(getString(pars,id,msg,c.getName(),null,e),e);
        }
    }
    public static void info(Class c,XMLParameter pars,String id,String msg,Map input,Throwable e){
        if(log.isInfoEnabled()){
            log.info(getString(pars, id, msg,c.getName(),input,e), e);
        }
    }
    public static void info(Object o){
        if(log.isInfoEnabled()){
            log.info(o==null?null:o.toString());
        }
    }
    public static void debug(Object o){
        if(log.isDebugEnabled()){
            log.info(o==null?null:o.toString());
        }
    }
    public static void info(Class c,XMLParameter pars,String id,String msg,Throwable e){
        if(log.isInfoEnabled()){
            log.info(getString(pars, id, msg,c.getName(),null,e), e);
        }
    }
    public static void warn(Class c,XMLParameter pars,String id,String msg,Map input,Throwable e){
        if(log.isWarnEnabled()){
            log.warn(getString(pars,id,msg,c.getName(),input,e),e);
        }
    }
    public static boolean isDebugEnabled(){
        return log.isDebugEnabled();
    }
    public static boolean isInfoEnabled(){
        return log.isInfoEnabled();
    }
    public static boolean isWarnEnabled(){
        return log.isWarnEnabled();
    }
    public static boolean isErrorEnabled(){
        return log.isErrorEnabled();
    }
    public static DateFormat getDateFormat(){
        String dateFormatStr = "ISO8601";

        DateFormat df;
        if (dateFormatStr.equalsIgnoreCase("ISO8601"))
        {
            df = new ISO8601DateFormat();
        }
        else
        {
            if (dateFormatStr.equalsIgnoreCase("ABSOLUTE"))
            {
                df = new AbsoluteTimeDateFormat();
            }
            else
            {
                if (dateFormatStr.equalsIgnoreCase("DATE")) {
                    df = new DateTimeDateFormat();
                } else {
                    try
                    {
                        df = new SimpleDateFormat(dateFormatStr);
                    }
                    catch (IllegalArgumentException e)
                    {
                        LogLog.error("Could not instantiate SimpleDateFormat with " + dateFormatStr, e);

                        df = (DateFormat) OptionConverter.instantiateByClassName("org.apache.log4j.helpers.ISO8601DateFormat", DateFormat.class, null);
                    }
                }
            }
        }
        return df;
    }
}
