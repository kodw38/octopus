package com.octopus.isp.bridge.launchers.impl;

import com.caucho.hessian.io.*;
import com.caucho.services.server.ServiceContext;
import com.octopus.isp.ds.ClientInfo;
import com.octopus.isp.ds.Env;
import com.octopus.isp.ds.RequestParameters;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cls.ClassUtils;
import com.octopus.utils.cls.POJOUtil;
import com.octopus.utils.exception.ISPException;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import com.octopus.utils.xml.desc.Desc;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.util.StringUtil;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

/**
 * Created by Administrator on 2018/10/31.
 */
public class LauncherCommon {
    transient static Log log = LogFactory.getLog(LauncherCommon.class);


    public static void getHeaders(HttpServletRequest paramHttpServletRequest, Hashtable paramHashtable,XMLParameter env,Properties luncherProperties,Map cache_headers)
    {
        //默认是admin,经过鉴权时修改对应的值
        HashMap map = new HashMap();
        map.put("auth_type","admin");
        map.put("auth_level","0");
        env.addAuthInfo(map);

        boolean iscacheheader= StringUtils.isTrue(luncherProperties.getProperty("iscacheheader"));
        if(iscacheheader && cache_headers.containsKey(paramHttpServletRequest.getRemoteAddr())){
            paramHashtable.putAll((Map)cache_headers.get(paramHttpServletRequest.getRemoteAddr()));
        }else {
            Enumeration localEnumeration1 = paramHttpServletRequest.getHeaderNames();

            StringBuffer str2 = new StringBuffer();
            while (localEnumeration1.hasMoreElements()) {
                String str1 = (String) localEnumeration1.nextElement();
                if(str1.equalsIgnoreCase("cookie")) continue;
                if (null != str1) {
                    Enumeration localEnumeration2 = paramHttpServletRequest.getHeaders(str1);
                    while (localEnumeration2.hasMoreElements()) {
                        if (str2.length() > 0) {
                            str2.append(";");
                        }
                        str2.append((String) localEnumeration2.nextElement());
                    }
                    String v = str2.toString();
                    if("undefined".equals(v)){
                        v="";
                    }
                    if("authinfo".equals(str1)){
                        log.debug("authinfo:"+v);
                        Map m = StringUtils.convert2MapJSONObject(v);
                        env.addAuthInfo(m);
                    }
                    paramHashtable.put(str1, v);
                    str2.delete(0, str2.length());
                }
            }
            if(iscacheheader) {
                Hashtable t = new Hashtable();
                t.putAll(paramHashtable);
                cache_headers.put(paramHttpServletRequest.getRemoteAddr(), t);
            }
        }
        if(log.isDebugEnabled()) {
            log.debug("request headers:" + paramHashtable);
        }
    }

    public static void setRequestCommonInfo(HttpServletRequest request,RequestParameters pars){
        pars.addRequestProperties("ContextPath", request.getContextPath());
        pars.addRequestProperties("Method", request.getMethod());
        pars.addRequestProperties("Scheme", request.getScheme());
        pars.addRequestProperties("ServerName", request.getServerName());
        pars.addRequestProperties("ServerPort", request.getServerPort());
        pars.setRequestURL(request.getRequestURL().toString());
        pars.setQueryString(request.getQueryString());
        pars.setRequestURI(request.getRequestURI());
        pars.setRequestResourceName(getResourceName(request.getRequestURI()));
        pars.setQueryStringMap(getQueryStringMap(pars.getQueryString()));
    }

    public static void setActions(RequestParameters pars,Properties properties,XMLObject obj) throws Exception {
        String srvname = "";
        if(isHessian(pars.getRequestURI(),properties)){
            HashMap input = new LinkedHashMap();
            pars.setProtocol("hessian");
            String name = getHessianRequestData(pars,(HttpServletRequest)pars.get("${request}"),(HttpServletResponse)pars.get("${response}"),input,obj);
            if(null != name){
                pars.setTargetNames(new String[]{name});
                if(null != input) {
                    pars.setRequestData(input);
                }
            }
        }else {
            if (null != pars.getQueryStringMap() && pars.getQueryStringMap().containsKey("actions")) {
                srvname = (StringUtils.trim((String) pars.getQueryStringMap().get("actions")));
                String[] actions = srvname.split(",");
                pars.setTargetNames(actions);
            }
        }
    }
    static boolean isHessian(String uri ,Properties properties){
        if(null != properties.getProperty("hessian") && StringUtils.isTrue(properties.getProperty("hessian")) && null != properties.getProperty("hessian_startwith") && null != uri && uri.startsWith(properties.getProperty("hessian_startwith")) ){
            return true;
        }
        return false;
    }
    //**************parse hession  *************************/
    static HessianInputFactory _inputFactory = new HessianInputFactory();
    static HessianFactory _hessianFactory = new HessianFactory();
    public static String getHessianRequestData(RequestParameters pars,HttpServletRequest request,HttpServletResponse response,Map emptyInput,XMLObject obj)throws Exception{

        String serviceId = request.getPathInfo();
        String objectId = request.getParameter("id");
        if (objectId == null) {
            objectId = request.getParameter("ejbid");
        }
        ServiceContext.begin(request, response, serviceId, objectId);

        InputStream is = request.getInputStream();
        OutputStream os = response.getOutputStream();
        SerializerFactory serializerFactory = new SerializerFactory();

        HessianInputFactory.HeaderType header = _inputFactory.readHeader(is);
        AbstractHessianInput in=null;
        AbstractHessianOutput out=null;
        switch (header) {
            case CALL_1_REPLY_1:
                in = _hessianFactory.createHessianInput(is);
                out = _hessianFactory.createHessianOutput(os);
                break;

            case CALL_1_REPLY_2:
                in = _hessianFactory.createHessianInput(is);
                out = _hessianFactory.createHessian2Output(os);
                break;

            case HESSIAN_2:
                in = _hessianFactory.createHessian2Input(is);
                in.readCall();
                out = _hessianFactory.createHessian2Output(os);
                break;

            default:
                throw new IllegalStateException(header + " is an unknown Hessian call");
        }
        if (serializerFactory != null)
        {
            in.setSerializerFactory(serializerFactory);
            out.setSerializerFactory(serializerFactory);
            pars.put("${hessianRequest}", in);
            pars.put("${hessianResponse}",out);

            ServiceContext context = ServiceContext.getContext();
            in.skipOptionalCall();
            String h;
            while ((h = in.readHeader()) != null) {
                Object value = in.readObject();
                context.addHeader(h, value);
            }

            String methodName = in.readMethod();
            int argLength = in.readMethodArgLength();
            String srvid = methodName.substring(2);
            srvid = StringUtils.lowerCaseFirstChar(srvid);
            XMLDoObject o = (XMLDoObject)obj.getObjectById(srvid);
            if(null == o){
                srvid = StringUtils.upperCaseFirstChar(srvid);
                o = (XMLDoObject)obj.getObjectById(srvid);
            }
            if(null != o) {
                Map stru = o.getDescStructure();
                String clzz = Desc.getGeneratorClassName(srvid, (String) stru.get("package"));
                Class c = Class.forName(clzz);
                Method method = ClassUtils.getMethodByName(c, "do" + StringUtils.upperCaseFirstChar(srvid));

                Class<?>[] args = method.getParameterTypes();
                Object[] values = new Object[args.length];
                for (int i = 0; i < args.length; i++) {
                    values[i] = in.readObject(args[i]);
                }
                Map ps = (Map)stru.get("input");
                String[] names=null;
                if(null != ps && ps.size()>0){
                    names = (String[])ps.keySet().toArray(new String[0]);
                    Map m = getInputParameters(names, values);
                    emptyInput.putAll(m);
                }
                pars.put("${hessianMethod}",method);
                pars.put("${hessianInputParameterNames}",names);
                return srvid;
            }else{
                throw new ISPException("HessianError","can't find TB Service by name ["+srvid+"]");
            }

        }
        return null;
    }


    public static void setHessianResponse(RequestParameters pars, Object res) throws Exception {
        HttpServletResponse response = (HttpServletResponse) pars.get("${response}");
        response.setContentType("x-application/hessian");
        AbstractHessianInput in = (AbstractHessianInput) pars.getParameter("${hessianRequest}");
        AbstractHessianOutput out = (AbstractHessianOutput) pars.getParameter("${hessianResponse}");
        try {

            if (null != res && res instanceof String && ((String) res).startsWith("{")) {
                res = StringUtils.convert2MapJSONObject((String) res);
                if (null != res && StringUtils.isTrue((String) ((Map) res).get("is_error"))) {
                    out.writeFault((String) ((Map) res).get("errorcode"), escapeMessage((String) ((Map) res).get("msg")), null);
                    return;
                }

            }
            //convert to bean
            Method method = (Method) pars.get("${hessianMethod}");
            if(null != method) {
                String[] pns = (String[]) pars.get("${hessianInputParameterNames}");
                in.completeCall();
                Object r=null;
                if (null != res) {
                    Type type = method.getReturnType();
                    AtomicLong dataSize = new AtomicLong(0);
                    r = POJOUtil.convertUSDL2POJO(res, type, dataSize);

                }
                out.writeReply(r);
            }else{
                throw new ISPException("HessianError","can't get Method");
            }
        }finally
        {
            out.close();
            in.close();
            //response.flushBuffer();
            ServiceContext.end();
        }

    }

    static String escapeMessage(String msg)
    {
        if (msg == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();

        int length = msg.length();
        for (int i = 0; i < length; i++)
        {
            char ch = msg.charAt(i);
            switch (ch)
            {
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '\000':
                    sb.append("&#00;");
                    break;
                case '&':
                    sb.append("&amp;");
                    break;
                default:
                    sb.append(ch);
            }
        }
        return sb.toString();
    }

    static Map getInputParameters(String[] parNames,Object receive)throws Exception{
        HashMap map = new LinkedHashMap();
        AtomicLong dataSize = new AtomicLong(0);
        if (parNames.length == 1 && !receive.getClass().isArray()) {
            //map.put(ns[0], POJOUtil.convertPojo2Map( receive));
            map.put(parNames[0], POJOUtil.convertPOJO2USDL(receive, dataSize));
        } else {
            for (int i = 0; i < parNames.length; i++) {
                //map.put(ns[i], POJOUtil.convertPojo2Map(((Object[]) receive)[i]));
                map.put(parNames[i], POJOUtil.convertPOJO2USDL(((Object[]) receive)[i], dataSize));
            }
        }
        return map;
    }

    /**
     * set targetName by maybe restful according to env.json restfulAddressMapping property
     * @param pars
     * @param properties
     * @param obj
     */
    public static void setActionsFromRestful(RequestParameters pars,Properties properties,XMLObject obj){
        if (null == pars.getTargetNames() && StringUtils.isTrue(properties.getProperty("restful")) &&
                (
                        "POST".equals(pars.getRequestProperties().get("Method")) || "GET".equals(pars.getRequestProperties().get("Method"))
                        || "PUT".equals(pars.getRequestProperties().get("Method")) || "DELETE".equals(pars.getRequestProperties().get("Method"))
                )) {
            String uri = pars.getRequestURI();
            HashMap param = new HashMap();
            String srvid = parseRestfulRul(uri,properties.getProperty("restful_startwith"),pars.getEnv(),param);
            if(StringUtil.isNotBlank(srvid)){
                pars.setTargetNames(new String[]{(String) srvid});
                if(null != param && param.size()>0){
                    if(null == pars.getRequestData()){
                        pars.setRequestData(param);
                    }else if(pars.getRequestData() instanceof Map){
                        ((Map)pars.getRequestData()).putAll(param);
                    }
                }
            }else{
                String name = uri.substring(uri.lastIndexOf("/") + 1);
                if (StringUtils.isNotBlank(name) && !name.contains(".")) {
                    pars.setTargetNames(new String[]{name});
                }
            }
            /*List<String> ms = StringUtils.getTagsNoMark(uri,"{","}");
            Object me = pars.getValueFromExpress("getvalue(${env}.restfulAddressMapping." + uri + ")", obj);
            if (null != me && me instanceof String && StringUtils.isNotBlank(me)) {
                pars.setTargetNames(new String[]{(String) me});
            } else {
                String startwith = properties.getProperty("restful_startwith");
                if (StringUtils.isNotBlank(startwith) && uri.startsWith("/" + startwith + "/")) {
                    String name = uri.substring(uri.lastIndexOf("/") + 1);
                    pars.setTargetNames(new String[]{name});
                } else {
                    String name = uri.substring(uri.lastIndexOf("/") + 1);
                    if (StringUtils.isNotBlank(name) && !name.contains(".")) {
                        pars.setTargetNames(new String[]{name});
                    }
                }
            }*/
        }
    }
    static Map<String,Map<String,List<String>>> cacheUriSrvId = null;
    static String parseRestfulRul(String uri,String startWith,Env env,Map param){
        String srvid = null;

        Map m = (Map)env.get("restfulAddressMapping");
        //    reUri      srvid    parNames
        if(null == cacheUriSrvId) {
            Map<String,Map<String,List<String>>> np=new HashMap();
            if (null != m) {
                Iterator<String> its = m.keySet().iterator();
                while (its.hasNext()) {
                    String u = its.next();
                    String id = (String) m.get(u);
                    if (StringUtils.isNotBlank(u) && StringUtils.isNotBlank(id)) {
                        List<String> ms = StringUtils.getTagsNoMark(u, "{", "}");
                        String ru = null;
                        if (null == ms || ms.size() == 0) {
                            ru = u;
                        } else {
                            ru = u.substring(0,u.indexOf("{"));
                        }
                        Map v = new HashMap<String, List<String>>();
                        v.put("SRV_ID", id);
                        if (null != ms && ms.size() > 0) {
                            v.put("PAR_NAMES", ms);
                        }
                        np.put(ru, v);
                    }
                }
            }
            if(null !=np && np.size()>0){
                cacheUriSrvId=np;
            }
        }

        if(null != cacheUriSrvId && cacheUriSrvId.size()>0){
            Iterator<String> its = cacheUriSrvId.keySet().iterator();
            while(its.hasNext()){
                String u = its.next();
                if(uri.indexOf(u)>=0){
                    Map c = (Map)cacheUriSrvId.get(u);
                    srvid = (String)c.get("SRV_ID");
                    if(null != param) {
                        List<String> p = (List) c.get("PAR_NAMES");
                        String pv = uri.substring(uri.indexOf(u) + u.length());
                        String[] vs = pv.split("/");
                        for (int i = 0; i < vs.length; i++) {
                            if (p.size() > i) {
                                param.put(p.get(i), vs[i]);
                            }
                        }
                    }
                    break;
                }
            }
        }

        if (StringUtils.isBlank(uri) && StringUtils.isNotBlank(startWith) && uri.startsWith("/" + startWith + "/")) {
            srvid = uri.substring(uri.lastIndexOf("/") + 1);
        }
        return srvid;
    }

    public static void setCookie(RequestParameters par,Cookie[] cs){
        StringBuffer cookie = new StringBuffer();
        if(null != cs){
            for(Cookie e:cs){
                if(cookie.length()!=0){
                    cookie.append(";");
                }
                cookie.append(e.getName()).append("=").append(e.getValue());
            }
        }
        par.getRequestHeaders().put("cookie",cookie.toString());
    }

    public static void setClientInfo(RequestParameters pars,HttpServletRequest request){
        try {
            pars.addClientInfo("ClientIp", getIp(request));
            pars.getClientInfo().setClientKind(request);
            String[] osbt = getOSBrowserTerminal(pars.getHeader("user-agent"));


            if (null != osbt && osbt.length > 2) {
                pars.addClientInfo(ClientInfo.CLIENT_IP, getIp(request));
                pars.addClientInfo(ClientInfo.CLIENT_OS, osbt[0]);
                pars.addClientInfo(ClientInfo.CLIENT_BROWSER, osbt[1]);
                pars.addClientInfo(ClientInfo.CLIENT_TERMINAL, osbt[2]);
            }
        }catch (Exception e){
            log.error("setClientInfo",e);
        }
    }

    private static String[] getOSBrowserTerminal(String user_agent){
        if(StringUtils.isBlank(user_agent))return null;
        String info = user_agent.toUpperCase();
        if(info.indexOf("(")==-1){// 针对火狐浏览器 swfupload控件 上传文件请求
            return null;
        }
        String[] ret = new String[3];
        String infoTemp=info.substring(info.indexOf("(") + 1, info.indexOf(")") - 1);
        String[] strInfo = infoTemp.split(";");
        if ((info.indexOf("MSIE")) > -1) {
            ret[1]=(strInfo[1].trim());
            ret[0]=(strInfo[2].trim());
        } else {
            String[] str = info.split(" ");
            if (info.indexOf("NAVIGATOR") < 0 && info.indexOf("FIREFOX") > -1) {
                ret[1]=(str[str.length - 1].trim());
                ret[0]=(strInfo[0].trim());
            } else if ((info.indexOf("OPERA")) > -1) {
                ret[1]=(str[0].trim());
                ret[0]=(strInfo[0].trim());
            } else if (info.indexOf("CHROME") < 0 && info.indexOf("SAFARI") > -1) {
                ret[1]=(str[str.length - 1].trim());
                ret[0]=(strInfo[2].trim());
            } else if (info.indexOf("CHROME") > -1) {
                ret[1]=(str[str.length - 2].trim());
//Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/34.0.1847.116 Safari/537.36
                ret[0]=(infoTemp);
            } else if (info.indexOf("NAVIGATOR") > -1) {
                ret[1]=(str[str.length - 1].trim());
                ret[0]=(strInfo[2].trim());
            } else {
                ret[1]=("Unknown Browser");
                ret[0]=("Unknown OS");
            }
        }
        if(StringUtils.containsIgnoreCase(user_agent, "PC")
                || StringUtils.containsIgnoreCase(user_agent,"Windows NT")){
            ret[2]="PC";
        }else if(StringUtils.containsIgnoreCase(user_agent, "IPhone") || StringUtils.containsIgnoreCase(user_agent, "Mobile")){
            ret[2]="PHONE";
        }else if(StringUtils.containsIgnoreCase(user_agent, "ipad") || StringUtils.containsIgnoreCase(user_agent, "pod")){
            ret[2]="PAD";
        }else{
            ret[2]="PC";
        }
        return ret;
    }

    private static String getIp(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("http_client_ip");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        // 如果是多级代理，那么取第一个ip为客户ip
        if (ip != null && ip.indexOf(",") != -1) {
            ip = ip.substring(ip.lastIndexOf(",") + 1, ip.length()).trim();
        }
        return ip;
    }

    private static String getResourceName(String uri){
        if(StringUtils.isNotBlank(uri)){
            if(uri.contains("/")){
                return uri.substring(uri.lastIndexOf("/")+1) ;
            }else{
                return uri;
            }
        }
        return "";
    }

    private static HashMap getQueryStringMap(String queryStr) {
        try {
            if (null != queryStr) {
                queryStr = URLDecoder.decode(queryStr, "UTF-8");
                HashMap map = new HashMap();
                String[] ps = queryStr.split("\\&");
                for (int i = ps.length - 1; i >= 0; i--) {
                    if (StringUtils.isNotBlank(ps[i])) {
                        String[] kv = ps[i].split("\\=");
                        if (kv.length == 2) {
                            if (StringUtils.isNotBlank(kv[0]) && StringUtils.isNotBlank(kv[1]) && !map.containsKey(kv[0])) {
                                if (queryStr.indexOf("actions=") >= 0 && null != (kv[1]) && (kv[1]).startsWith("{") && (kv[1]).endsWith("}")) {
                                    map.put(kv[0], StringUtils.convert2MapJSONObject(kv[1]));
                                } else if (queryStr.indexOf("actions=") >= 0 && null != (kv[1]) && (kv[1]).startsWith("[") && (kv[1]).endsWith("]")) {
                                    map.put(kv[0], StringUtils.convert2ListJSONObject(kv[1]));
                                } else {
                                    map.put(kv[0], kv[1]);
                                }
                            }
                        }
                    }
                }
                return map;
            }
            return null;
        }catch (Exception e){
            log.error("getQueryStringMap",e);
        }
        return null;
    }
}
