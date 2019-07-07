package com.octopus.isp.bridge.launchers.impl;

import com.octopus.isp.actions.websocket.jetty.MyWebSocketServlet;
import com.octopus.isp.bridge.IBridge;
import com.octopus.isp.bridge.ILauncher;
import com.octopus.isp.bridge.impl.Bridge;
import com.octopus.isp.bridge.launchers.IConvert;
import com.octopus.isp.bridge.launchers.impl.pageframe.SessionManager;
import com.octopus.isp.bridge.launchers.impl.pageframe.util.HttpUtils;
import com.octopus.isp.cell.impl.Cell;
import com.octopus.isp.ds.*;
import com.octopus.isp.utils.ISPUtil;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.SNUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.exception.Logger;
import com.octopus.utils.img.ImageUtils;
import com.octopus.utils.net.NetUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import sun.misc.BASE64Decoder;

import javax.servlet.*;
import javax.servlet.DispatcherType;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.util.*;

/**
 * User: wfgao_000
 * Date: 15-8-20
 * Time: 下午4:58
 */
public class WebPageFrameLauncher extends Cell implements ILauncher {
    static transient Log log = LogFactory.getLog(WebPageFrameLauncher.class);
    IConvert inputconvert;
    IConvert outputconvert;
    DataEnv env=null;
    XMLDoObject statHandler;
    Map cache_headers= new HashMap();
    public static String WebPageFrameLauncher="WebPageFrameLauncher";
    String LoginUserNameKey,LoginUserPwdKey;
    List fromAccessIps=null;
    public WebPageFrameLauncher(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        env = (DataEnv)getPropertyObject("env");

        XMLMakeup uns = (XMLMakeup)ArrayUtils.getFirst(xml.getByTagProperty("property","name","LoginUserNameKey"));
        if(null != uns){
            LoginUserNameKey=uns.getText();
        }
        XMLMakeup ups = (XMLMakeup)ArrayUtils.getFirst(xml.getByTagProperty("property","name","LoginUserPwdKey"));
        if(null != ups){
            LoginUserPwdKey=ups.getText();
        }
        if(null != env.getEnv() && StringUtils.isTrue((String)env.getEnv().get("webselfstart")) && StringUtils.isNotBlank(env.getEnv().get("webport"))) {
            int port =0,sslport=0;
            String sp = ((String)env.getEnv().get("webport"));
            if(StringUtils.isNotBlank(sp)){
                port = Integer.parseInt(sp);
            }
            String websocket=(String)env.getEnv().get("isUsedWebSocket");
            boolean isUsedWebSocket=false;
            if(StringUtils.isNotBlank(websocket)){
                isUsedWebSocket = StringUtils.isTrue(websocket);
            }
            String p = ((String)env.getEnv().get("ssl_port"));
            if(StringUtils.isNotBlank(p)){
                sslport = Integer.parseInt(p);
            }
            start((String) env.getEnv().get("webcontextpath"), (String) env.getEnv().get("webcontentpath"), port,isUsedWebSocket, sslport, (String) env.getEnv().get("ishttps"), (String) env.getEnv().get("ssl_keystoretype"), (String) env.getEnv().get("ssl_keystore"), (String) env.getEnv().get("ssl_password"), (String) env.getEnv().get("ssl_mgr_password"));
        }

    }

    public void doInitial()throws Exception{
        if(null != properties.getProperty("limit_from_access_ips")){
            String ips = properties.getProperty("limit_from_access_ips");
            if(StringUtils.isNotBlank(ips)) {
                fromAccessIps = Arrays.asList(ips.split(","));
            }
        }
    }

    public void start(){
        //System.out.println();
    }

    @Override
    public boolean addEnv(String key, Object value) {
        env.getEnv().addParameter(key, value);
        return true;
    }



    @Override
    public Object invoke(Object obj) throws Exception {

        HttpServletRequest request = (HttpServletRequest)((Object[])obj)[0];
        HttpServletResponse response = (HttpServletResponse)((Object[])obj)[1];

        RequestParameters pars = new RequestParameters();
        try {
            //set header from request
            LauncherCommon.getHeaders(request, pars.getRequestHeaders(), pars,properties,cache_headers);

            //set request common data
            LauncherCommon.setRequestCommonInfo(request,pars);

            pars.addParameter("${request}", request);
            pars.addParameter("${response}", response);

            /*if(pars.getQueryStringMap().containsKey("targetinsid")){
            pars.getRequestHeaders().put("targetinsid",pars.getQueryStringMap().get("targetinsid"));
            }*/

            /*pars.addParameter("${webroot}",request.getSession().getServletContext().getRealPath("/"));
            */
            //post service info , changed remove the if for restful
            //if(pars.getRequestProperties().get("Method").equals("POST")){
            LauncherCommon.setActions(pars,properties,this);
            //restful post
            LauncherCommon.setActionsFromRestful(pars,properties,this);

            String srvname = "";
            if(null != pars.getTargetNames() && pars.getTargetNames().length>0){
                srvname=pars.getTargetNames()[0];
            }

            //set request id
            if (!pars.getRequestHeaders().contains(pars.KEY_REQUESTID)) {
                String user = (String) request.getHeader("user");
                Bridge b = (Bridge) getObjectById("bridge");
                if (null != b) {
                    pars.setRequestId(ISPUtil.getRequestId("WEB", b.getInstanceId(), (user == null ? "" : user), srvname));
                } else {
                    pars.setRequestId(ISPUtil.getRequestId("WEB", "", user, srvname));
                }
            } else {
                pars.setRequestId((String) pars.getRequestHeaders().get(pars.KEY_REQUESTID));
            }
            if(Logger.isDebugEnabled()) {
                Logger.debug(this.getClass(), pars, (null == getXML() ? "" : getXML().getId()), "begin", null);
            }

            LauncherCommon.setCookie(pars, request.getCookies());

            if (null != inputconvert)
                obj = inputconvert.convert(pars,obj);

            //set env
            pars.setEnv(env.getEnv());

            LauncherCommon.setClientInfo(pars,request);


                //old session
            SessionManager sm = (SessionManager) getObjectById("SessionManager");
            pars.setSessionManager(sm);
            //find session and set session into this request env , AuthInfo only between tb server
            String sessionKey = (String) properties.get("SessionKey");
            pars.getRequestProperties().put("SessionKey", sessionKey);
            if(null == pars.getAuthInfo()|| StringUtils.isBlank((String) pars.getAuthInfo().get("UserName"))){
                //the request from user web request and exclude from tb to tb
                getCookies(pars, pars.getRequestHeaders(), pars.getRequestCookies(), sessionKey, sm);
            }else{
                //the request is must from tb other server, so limit from ip is better for security
                if(!(null == fromAccessIps || fromAccessIps.size()==0 || ArrayUtils.isInStringArray(fromAccessIps, pars.getClientInfo().getClientIp()))) {
                    throw new Exception("the client ip ["+pars.getClientInfo().getClientIp()+"] don't have permission to access");
                }
                Map m = pars.getAuthInfo();
                //if request from other tb instance with auth info , it will not search user info from redis ,improve performance
                Session session = sm.createEmptySession();
                session.putAll(m);
                if(null != m.get("SESSION_ID") && StringUtils.isNotBlank(m.get("SESSION_ID"))){
                    log.info("session id changed "+m.get("SESSION_ID")+" from "+session.getSessionId());
                    session.setSessionId((String)m.get("SESSION_ID"));
                }
                session.put("UserName", m.get("UserName"));
                session.put("USER_TYPE", m.get("USER_TYPE"));
                if(log.isDebugEnabled()){
                    log.debug("set session by AuthInfo");
                }
                pars.setSession(session);

            }
            if("json".equals(pars.getProtocol())) {
                //request data
                Object objpar = getRequestData(request, pars);
                if (log.isDebugEnabled()) {
                    log.debug("receive request data\n" + objpar);
                }
                if (null != inputconvert) {
                    objpar = inputconvert.convert(pars,objpar);
                }
                if (null != objpar) {
                    pars.setRequestData(objpar);
                }
            }
            //login

            String[] user = getUserInfo(pars);
            if (user != null) {
                //session from login info
                Session session = sm.getSessionByUser(user[0]);
                if (null != session  && null != pars.getRequestCookies()) {
                    pars.getRequestCookies().put(sessionKey, session.getSessionId());
                    pars.setSession(session);
                    if(log.isDebugEnabled()){
                        log.debug("set session by login in");
                    }
                }
            }

            if (null == pars.getTargetNames()) {
                doThing(pars, getXML());
            }
            if (!pars.isStop()) {
                if (Logger.isDebugEnabled()) {
                    Logger.debug(this.getClass(), pars, (null == getXML() ? "" : getXML().getId()), "do action begin" + ArrayUtils.toJoinString(pars.getTargetNames()), null);
                }
            /*if(log.isDebugEnabled()) {
                pars.printTime("WebPageFrame invoke srv begin");
            }*/
                try {
                    ((IBridge) getPropertyObject("bridge")).evaluate(pars);
                } catch (Exception e) {
                    //Logger.error(this.getClass(),pars,(null == getXML() ? "" : getXML().getId()),e.getMessage(),e);
                    throw e;
                }
            /*if(log.isDebugEnabled()) {
                pars.printTime("WebPageFrame invoke srv end");
            }*/
                if (Logger.isDebugEnabled()) {
                    Logger.debug(this.getClass(), pars, (null == getXML() ? "" : getXML().getId()), "do action end" + ArrayUtils.toJoinString(pars.getTargetNames()), null);
                }
                HashMap map = new HashMap();
                map.put("flowid", "output");
                doCheckThing(getXML().getId(), pars, map, null, null, null);
                if (!pars.isStop()) {
                    Object ret = pars.getResult();
                    if (null != ret) {
                        if (ret instanceof ResultCheck) {
                            ret = ((ResultCheck) ret).getRet();
                        }
                        if (null != outputconvert) {
                            ret = outputconvert.convert(pars,ret);
                        }

                    }
                    if (null == ret) {
                        ret = "";
                    }
                    addResponseDataSize(pars, ret.toString().length());
                    if(Logger.isDebugEnabled()) {
                        Logger.debug(this.getClass(), pars, (null == getXML() ? "" : getXML().getId()), "end" + ArrayUtils.toJoinString(pars.getTargetNames()), null);
                    }
                /*if (log.isDebugEnabled()) {
                    pars.printTime("WebPageFrame end");
                }*/
                    if("hessian".equals(pars.getProtocol())){
                        LauncherCommon.setHessianResponse(pars,ret);
                        return null;
                    }else {
                        return ret;
                    }
                }
                return null;
            } else {
                if (null != pars.getResult()) {
                    if (pars.getResult() instanceof ResultCheck) {
                        if (!((ResultCheck) pars.getResult()).isSuccess()) {
                            HttpUtils hu = (HttpUtils) getObjectById("HttpUtils");
                            hu.writeError(pars);
                            return null;
                        } else {
                            return ((ResultCheck) pars.getResult()).getRet();
                        }
                    } else {
                        return pars.getResult();
                    }
                }
                return null;
            }
        }finally {

        }
    }



    String[] getUserInfo(RequestParameters request){
        Object o = request.getRequestData();
        if(o instanceof Map){
            Map in = (Map)o;

            String userName = (String)in.get(LoginUserNameKey);
            String userPwd = (String)in.get(LoginUserPwdKey);
            if(StringUtils.isNotBlank(userName) && StringUtils.isNotBlank(userPwd))
            return new String[]{userName,userPwd};
            return null;
        }
        return null;
    }
    private void getCookies(RequestParameters parameters,Hashtable headers, Hashtable paramHashtable,String sessionkey,SessionManager sm) throws Exception {
        String cookieName = null;
        if(headers.containsKey("cookie")){
            cookieName="cookie";
        }
        if(headers.containsKey("Cookie")){
            cookieName="Cookie";
        }
        if(null !=cookieName){
            String cookies = (String)headers.get(cookieName);
            log.debug("CookieName:" + cookieName+" cookies:"+cookies);
            //log.debug("Cookie:"+cookies);
            //log.debug("Cookie1:"+(String)headers.get("cookie"));
            //log.debug("Cookie2:"+(String)headers.get("Cookie"));
            parameters.getRequestProperties().put("${RequestCookie}",cookies);
            String[] ss = cookies.split(";");
            for(String s:ss){
                if(s.contains("=")){
                    int n=s.indexOf("=");
                    if(n>0){
                        String k = s.substring(0,n).trim();
                        String v = s.substring(n+1);
                        log.debug("sessionKey:"+k+" value:"+v);
                        if(k.equals(sessionkey) && parameters.getSession()==null){
                            //log.debug("get session from "+k+" value "+v);
                            if(StringUtils.isNotBlank(v)){
                                //log.debug("get session by cookie:"+v);
                                Session session = sm.getSessionById(v,null);
                                if(null!=session){
                                    paramHashtable.put(k,v);
                                    parameters.setSession(session);

                                    putInfo2Auth4Trans(session,parameters.getAuthInfo());
                                    if(log.isDebugEnabled()) {
                                        log.debug("set session from " + k + " value " + v + " session:" + session);
                                    }
                                    break;
                                }
                            }
                        }else if(k.equals("JSESSIONID") && parameters.getSession()==null){
                            paramHashtable.put(k,v);
                            if(StringUtils.isNotBlank(v)){
                                try {
                                    Session session = sm.getSessionById(null, v);
                                    if (null != session) {
                                        parameters.setSession(session);
                                        putInfo2Auth4Trans(session, parameters.getAuthInfo());
                                        if (log.isDebugEnabled()) {
                                            log.debug("set jssession from " + k + " value " + v + " session:" + session);
                                        }
                                        break;
                                    } else {
                                        //log.debug("not find session by JSESSIONID=" + v);
                                    }
                                }catch (Exception e){
                                    log.debug("not find session by JSESSIONID=" + v);
                                }
                            }
                        }else{
                            if(!paramHashtable.containsKey(k))
                                paramHashtable.put(k,v);
                        }
                    }else{
                        paramHashtable.put(s,null);
                    }
                }
            }
        }
    }
    void putInfo2Auth4Trans(Session s,Map m){
        if(null != s && null!=m) {
            m.putAll(s);
            m.put("UserName",s.getUserName());
            m.put("USER_TYPE",s.get("USER_TYPE"));
        }
    }
    //webpageLanucher do nothing when there is new service add or update
    public void notifyObject(String op,Object obj)throws Exception{

    }

    void addRequestDataSize(RequestParameters pars,long requestDataSize){
        try {
            if (null != statHandler) {
                if(null != pars.getTargetNames()) {
                    for(String n:pars.getTargetNames()) {
                        HashMap input = new HashMap();
                        input.put("op", "addRequestDataSize");
                        input.put("srvId", n);
                        pars.setRequestDataSize(requestDataSize);
                        statHandler.doSomeThing(null, pars, input, null, null);
                    }
                }

            }
        }catch (Exception e){
            log.error("add request Data size error",e);
        }
    }
    void addResponseDataSize(RequestParameters pars,long requestDataSize){
        try {
            if (null != statHandler) {
                if(null != pars && null != pars.getTargetNames()) {
                    for (String n : pars.getTargetNames()) {
                        HashMap input = new HashMap();
                        input.put("op", "addResponseDataSize");
                        input.put("srvId", n);
                        pars.setResponseDataSize(requestDataSize);
                        statHandler.doSomeThing(null, pars, input, null, null);
                    }
                }
            }
        }catch (Exception e){
            log.error("add request Data size error",e);
        }
    }
    Object getRequestData(HttpServletRequest request,RequestParameters pars) throws Exception {

        boolean isMultipart = ServletFileUpload.isMultipartContent(request);
        if (isMultipart) {//ArrayUtils.isArrayLikeString(pars.getTargetNames(),"upload")
            HashMap<String, InputStream> fileMap=new HashMap<String,InputStream>();
            HashMap<String,Object> iDataMap=new HashMap<String,Object>();
            iDataMap.putAll(pars.getQueryStringMap());

            readReqeustInputStream(request, fileMap, iDataMap);
            iDataMap.put("files",fileMap);
            pars.getQueryStringMap();
            return iDataMap;
        }
        long datasize=0;
        request.setCharacterEncoding("UTF-8");
        String paramStr=null;
        int len = request.getContentLength();
        if(len>0){
            InputStream in =request.getInputStream();
            byte buffer[] = new byte[len];
            int total = 0;
            int once = 0;
            while ((total < len) && (once >=0)) {
                once = in.read(buffer,total,len);
                total += once;
            }

            paramStr = new String(buffer,"UTF-8").trim();
            //add request data size to stat
            datasize+=paramStr.length();
            addRequestDataSize(pars, datasize);

            Map ret=null;
            if(paramStr.startsWith("{")){
                ret= StringUtils.convert2MapJSONObject(paramStr);
            }else if(paramStr.startsWith("[")){
                List rt = StringUtils.convert2ListJSONObject(paramStr);
                return rt;

            }else if(paramStr.contains("=")){
                ret = StringUtils.convertHtmlAndChar2Map(paramStr);
            }
            if(null != ret){
                //接口传进来的数据不进行转换，默认是真实数据。
                //ret = pars.getMapValueFromParameter(ret,this);
                //handvar(ret,pars);
                //ret.putAll(pars.getQueryStringMap());
                return ret;
            }else{
                return paramStr;
            }
        }
        return pars.getQueryStringMap();
    }
    /**
     * get image content from httpServletRequest post String
     * @param str
     * @return
     */
    public static Map<String,InputStream> getImageFromHttpServletRequestPostString(String str){

        if(null != str) {
            String[] ss = str.split("\r\n");
            String name=null;
            HashMap ret = new HashMap();
            StringBuffer sb = new StringBuffer();
            for(int i=0;i<ss.length;i++){
                if(ss[i].indexOf("filename")>0){
                    List<String> fs = StringUtils.getTagsNoMark(ss[i],"filename=\"","\"");
                    if(null != fs && fs.size()==1){
                        name = fs.get(0);
                    }
                }
                if(null != name && (i==ss.length-3 || i==ss.length-2)){
                    sb.append(ss[i]+"\r\n");
                }
            }

            BASE64Decoder decoder = new BASE64Decoder();
            try {
                // Base64解码
                byte[] b = decoder.decodeBuffer(sb.toString());
                for (int i = 0; i < b.length; ++i) {
                    if (b[i] < 0) {// 调整异常数据
                        b[i] += 256;
                    }
                }
                ByteArrayInputStream in = new ByteArrayInputStream(b);
                ret.put(name,in);
            }catch(Exception e){

            }


            if(ret.size()>0){
                return ret;
            }
        }
        return null;
    }
    public static String unicode2String(String unicode) {
        StringBuffer string = new StringBuffer();
        String[] hex = unicode.split("\\\\u");
        for (int i = 1; i < hex.length; i++) {
            // 转换出每一个代码点
            int data = Integer.parseInt(hex[i], 16);
            // 追加成string
            string.append((char) data);
        }
        return string.toString();
    }

    void readReqeustInputStream(HttpServletRequest request,Map<String,InputStream> fileMap,Map<String,Object> iData)throws Exception{

            FileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload servletFileUpload = new ServletFileUpload(factory);
            //servletFileUpload.setSizeMax(maxPostSize);
            servletFileUpload.setHeaderEncoding("UTF-8");
            List<?> fileItems = servletFileUpload.parseRequest(request);
            for (Object fileItem : fileItems) {
                FileItem item = (FileItem) fileItem;
                if (!item.isFormField()) { // 是文件，默认只传一个
                    String fileName = item.getName();
                    if (org.apache.commons.lang.StringUtils.isNotBlank(fileName)) {
                        int start = fileName.lastIndexOf("\\");
                        String name = fileName.substring(start + 1, fileName.length());

                        fileMap.put(name, item.getInputStream());
                    }
                } else { // 不是文件,为Form表单中的其他信息
                    iData.put(item.getFieldName(), item.getString("UTF-8"));
                }
            }

    }

    void handvar(Map ret,RequestParameters pars)throws Exception{
        if(null != ret){
            Iterator ist = ret.keySet().iterator();
            while(ist.hasNext()){
                Object k = ist.next();
                if(ret.get(k) instanceof String){
                    Object o=null;
                    if(((String)ret.get(k)).startsWith("${")){
                        o = ObjectUtils.getValueByPath(pars.getReadOnlyParameter(),(String) ret.get(k));
                    }else{
                        o = pars.getExpressValueFromMap((String) ret.get(k),this);
                    }
                    if(null != o){
                        ret.put(k,o);
                    }else{
                        throw new Exception("not find the value["+ret.get(k)+"]");
                    }

                }else if(ret.get(k) instanceof List){
                    List li = new LinkedList();
                    for(int i=((List)ret.get(k)).size()-1;i>=0;i--){
                        Object o = ((List)ret.get(k)).get(i);
                        ((List)ret.get(k)).remove(o);
                        if(o instanceof Map){
                            handvar((Map)o,pars);
                        }else if(o instanceof String){
                            o = pars.getExpressValueFromMap((String)o,this);
                        }else{
                            throw new Exception("not find the value["+ret.get(k)+"]");
                        }
                        li.add(o);
                    }
                    ((List)ret.get(k)).addAll(li);
                }else if(ret.get(k) instanceof Map){
                    handvar((Map)ret.get(k),pars);
                }
            }
        }
    }


    //jetty 6.1
    public void start(String contenxt,String baseResource,int port,boolean isUsedWebSocket,int sslport,String ishttps,String keyStoreType,String kstorePath,String storePassword,String managerPassword){
        //web.xml路径
//        String serverWebXml = web+"WEB-INF" +"/web.xml";
        //startHttpWeb
        startWeb(baseResource, contenxt, isUsedWebSocket, port);

        //startHttpsWeb

        if(StringUtils.isTrue(ishttps)){
            startHttps(baseResource,contenxt,sslport,isUsedWebSocket,kstorePath,storePassword,managerPassword);
        }


    }

    void startHttps(String baseResource,String contextPath,int sslport,boolean isUsedWebSocket,String ksStorePath,String storePassword,String managerPassword){
        if(sslport>0) {
            if(StringUtils.isBlank(contextPath)) {
                contextPath = "/";
            }

            //String kstorePath="C:\\log\\build\\user\\define\\classes\\tb_ws.jks";
            //String kstorePath = "C:\\log\\build\\user\\define\\classes\\wildcard.u.com.my_2018.pfx";
            //String storePassword="treasurebag";
            //String storePassword = "umobile0!8";
            //String managerPassword="treasurebag";
            try {

                org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server(sslport);
                ServerConnector httpConnector = null;
                // Setup SSL
                org.eclipse.jetty.util.ssl.SslContextFactory sslContextFactory = new org.eclipse.jetty.util.ssl.SslContextFactory();
                sslContextFactory.setKeyStorePath(System.getProperty("jetty.keystore.path", ksStorePath));
                sslContextFactory.setKeyStorePassword(System.getProperty("jetty.keystore.password", storePassword));
                if(StringUtils.isNotBlank(managerPassword)) {
                    sslContextFactory.setKeyManagerPassword(System.getProperty("jetty.keymanager.password", managerPassword));
                }
                // Setup HTTP Configuration
                HttpConfiguration httpConf = new HttpConfiguration();
                httpConf.setSecurePort(sslport);
                httpConf.setSecureScheme("https");
                httpConnector = new ServerConnector(server, new HttpConnectionFactory(httpConf));
                // Setup HTTPS Configuration
                HttpConfiguration httpsConf = new HttpConfiguration(httpConf);
                httpsConf.addCustomizer(new SecureRequestCustomizer());
                ServerConnector httpsConnector = new ServerConnector(server,
                        new SslConnectionFactory(sslContextFactory, "http/1.1"),
                        new HttpConnectionFactory(httpsConf));
                httpsConnector.setName("secured"); // named connector
                httpsConnector.setPort(sslport);
                // Add connectors
                server.setConnectors(new Connector[]{httpConnector, httpsConnector});


                org.eclipse.jetty.webapp.WebAppContext webapp = new org.eclipse.jetty.webapp.WebAppContext();
                webapp.setContextPath(contextPath);

                webapp.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
                //webapp.setResourceBase(webContextPath);
                //定位项目中class文件的位置
                webapp.setClassLoader(Thread.currentThread().getContextClassLoader());
                if(StringUtils.isNotBlank(baseResource)) {
                    webapp.setBaseResource(org.eclipse.jetty.util.resource.Resource.newResource(baseResource));
                    File tmpFile = new File(baseResource + "/web_temp");
                    webapp.setTempDirectory(tmpFile);
                }else{
                    webapp.setBaseResource(org.eclipse.jetty.util.resource.Resource.newClassPathResource(""));
                }

                if (isUsedWebSocket) {
                    webapp.addServlet(new ServletHolder(new MyWebSocketServlet()), "/websocket");
                }
                //        webapp.setDescriptor(serverWebXml);
                FilterHolder holder = new FilterHolder(new MyFilter(this, null));
                webapp.addFilter(holder, "/*", EnumSet.of(DispatcherType.REQUEST));
                server.setHandler(webapp);
                server.start();
                System.out.println("web Server [https://"+ NetUtils.getip()+":"+sslport + baseResource + "] is started!");
                //server.join();
                // Wire up contexts for secure handling to named connector
                //String secureHosts[] = new String[]{"@secured"};

                //ContextHandler test1Context = new ContextHandler();
                //test1Context.setContextPath(contenxt);
                //test1Context.setHandler(new MyFilter(this,null));
                //test1Context.setVirtualHosts(secureHosts);

                //ContextHandler test2Context = new ContextHandler();
                //test2Context.setContextPath("/test2");
                //test2Context.setHandler(new HelloHandler("Hello2"));
                //test2Context.setVirtualHosts(secureHosts);

                //ContextHandler rootContext = new ContextHandler();
                //rootContext.setContextPath("/");
                //rootContext.setHandler(new RootHandler("/test1", "/test2"));
                //rootContext.setVirtualHosts(secureHosts);

                // Wire up context for unsecure handling to only
                // the named 'unsecured' connector
                //ContextHandler redirectHandler = new ContextHandler();
                //redirectHandler.setContextPath("/");
                //redirectHandler.setHandler(new SecureSchemeHandler());
                //redirectHandler.setVirtualHosts(new String[]{"@unsecured"});

                // Establish all handlers that have a context
                //ContextHandlerCollection contextHandlers = new ContextHandlerCollection();
                //contextHandlers.setHandlers(new Handler[]
                //        {redirectHandler, rootContext, test1Context, test2Context});

                // Create server level handler tree
                //HandlerList handlers = new HandlerList();
                //handlers.addHandler(contextHandlers);
                //handlers.addHandler(new DefaultHandler()); // round things out

                //server.setHandler(handlers);

                //server.start();
                //server.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void startWeb(String resourcePath,String contextPath,boolean isUsedWebSocket,int port){
        if(port>0) {


            Server server = new Server(port);

            WebAppContext context = new WebAppContext();
            if (StringUtils.isBlank(contextPath)) {
                contextPath = "/";
            }else {
                context.setContextPath(contextPath);
            }
            context.setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false");
            context.setClassLoader(Thread.currentThread().getContextClassLoader());
            try {
                if (StringUtils.isNotBlank(resourcePath)) {
                    context.setResourceBase(resourcePath);
                    File tmpFile = new File(resourcePath + "/web_temp");
                    context.setTempDirectory(tmpFile);
                }else{
                    context.setBaseResource(org.eclipse.jetty.util.resource.Resource.newClassPathResource(""));
                }
                FilterHolder holder = new FilterHolder(new MyFilter(this, null));
                context.addFilter(holder, "/*",EnumSet.of(DispatcherType.REQUEST));
                if (isUsedWebSocket) {
                    context.addServlet(new ServletHolder(new MyWebSocketServlet()), "/websocket");
                }
                //context.addServlet(HelloWordService.class,"/hessian/ProxyGetUU");
                server.setHandler(context);
                server.start();
                System.out.println("web Server [http://" + NetUtils.getip() + ":" + port + resourcePath + "] is started!");
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    class MyFilter implements Filter {
        ILauncher launcher;
        String home;
        public MyFilter(ILauncher launcher,String home){
            this.launcher =launcher;
            this.home=home;
        }
        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
            if(null != home) {
                launcher.addEnv(Env.KEY_HOME, home);
            }
        }

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
            HttpServletResponse response = (HttpServletResponse)servletResponse;
            HttpServletRequest request = (HttpServletRequest)servletRequest;

            long l = System.currentTimeMillis();
            try{
                Object ret = launcher.invoke(new Object[]{servletRequest,servletResponse});
                if(log.isDebugEnabled()) {
                    log.debug(ret);
                }
                if(null != ret){
                    String rsp = ret.toString();
                    if(rsp.startsWith("{") || rsp.startsWith("["))
                        response.setContentType("application/json;charset=UTF-8");
                    else if(ret instanceof String && ((String)ret).startsWith("<script")){
                        response.setContentType("text/html;charset=UTF-8");
                    }else
                        response.setContentType("application/text;charset=UTF-8");

                    response.getOutputStream().write(ret.toString().getBytes("UTF-8"));
                    response.flushBuffer();
                }else if(!response.isCommitted()){
                    //System.out.println("--websocket--");
                    request.getSession().setAttribute("WEB-Launcher",launcher);

                    filterChain.doFilter(request,response);
                }
            }catch(Exception e){
                if(null != request.getQueryString()) {
                    log.error("request error:" + request.getRequestURL().toString() + "/" + request.getQueryString(), e);
                }else{
                    log.error("request error:" + request.getRequestURL().toString(), e);
                }
                HttpUtils.redirectError((XMLObject) launcher, request, response, "pc", e);
            }finally {
                if(log.isInfoEnabled()) {
                    log.info("http tootle:"+request.getRequestURI()+(request.getQueryString()==null?"":"?"+URLDecoder.decode(request.getQueryString()))+"   " + (System.currentTimeMillis() - l)+"ms");
                }
            }
        }

        @Override
        public void destroy() {

        }
    }
}
