package com.octopus.isp.bridge.launchers.impl.pageframe.channel;

import com.octopus.isp.ds.RequestParameters;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.BasicHttpContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;

/**
 * User: wfgao_000
 * Date: 15-8-18
 * Time: 上午4:49
 */
public class HttpProxy extends XMLDoObject {
    private static transient Log log = LogFactory.getLog(HttpProxy.class);
    final String targethost="http.target_host";
    final String FMC_TYPE="HTTPPROXY_RRDIRECT";
    final String sessionIDName= "JSESSIONID";
    final char [][] QuoTokenChars={{'\'','\''},{'\"','\"'}};
    final char [] QuoToken={'\'','\"','/'};
    public static String[] PAGE_ARRAY={"html","jsp","php","htm"};

    List<HttpProxyInfo> proxyList=null;
    Map<String,HttpProxyInfo> tempProxyMap = null;

    public HttpProxy(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        configProxy();
    }



    BasicHeader[] getHeaderFromTable(Hashtable<String,String> headHashtable){
        List li = new ArrayList();
        Enumeration<String> en = headHashtable.keys();
        while(en.hasMoreElements()){
            String k = en.nextElement();
            li.add(new BasicHeader(k,headHashtable.get(k)));

        }
        return (BasicHeader[])li.toArray(new BasicHeader[li.size()]);
    }

    class ProxyPath{
        String proxyContext;
        String proxyHttphost;
        String proxyPath;
        String requestBase;
        String proxyTargetHost;
        String requestSessionID;
        String requestHttphost;
        String requestQueryString;

        public String getRequestQueryString() {
            return requestQueryString;
        }

        public void setRequestQueryString(String requestQueryString) {
            this.requestQueryString = requestQueryString;
        }

        public String getProxyContext() {
            return proxyContext;
        }

        public void setProxyContext(String proxyContext) {
            this.proxyContext = proxyContext;
        }

        public String getProxyHttphost() {
            return proxyHttphost;
        }

        public void setProxyHttphost(String proxyHttphost) {
            this.proxyHttphost = proxyHttphost;
        }

        public String getProxyPath() {
            return proxyPath;
        }

        public void setProxyPath(String proxyPath) {
            this.proxyPath = proxyPath;
        }

        public String getRequestBase() {
            return requestBase;
        }

        public void setRequestBase(String requestBase) {
            this.requestBase = requestBase;
        }

        public String getProxyTargetHost() {
            return proxyTargetHost;
        }

        public void setProxyTargetHost(String proxyTargetHost) {
            this.proxyTargetHost = proxyTargetHost;
        }

        public String getRequestSessionID() {
            return requestSessionID;
        }

        public void setRequestSessionID(String requestSessionID) {
            this.requestSessionID = requestSessionID;
        }

        public String getRequestHttphost() {
            return requestHttphost;
        }

        public void setRequestHttphost(String requestHttphost) {
            this.requestHttphost = requestHttphost;
        }

        public String getProxyRequestPath() {
            if(StringUtils.isBlank(requestQueryString)){
                return proxyHttphost+proxyPath;
            }else{
                return proxyHttphost+proxyPath+"?"+requestQueryString;
            }
        }

    }

    public String encoding(String s,String regionCode,String requestURI,String requestScheme,String requestServerName,String requestServerPort,String contextPath,Map headers){
        try{
            if(StringUtils.isNotBlank(regionCode) && StringUtils.isNotBlank(s)){
                int c = requestURI.indexOf("/", 1);
                if(c<0){
                    c = requestURI.length();
                }
                String first = requestURI.substring(1, c);
                HttpProxyInfo proxyInfo = getHttpProxyHost(first,regionCode,requestURI,requestScheme,requestServerName,requestServerPort,contextPath,headers);
                if(null != proxyInfo){
                    return new String(s.getBytes("ISO-8859-1"),proxyInfo.getEncoding());
                }
            }
        }catch (Exception e){

        }
        return s;
    }

    public HttpProxyInfo getProxyInfo(String  requestURI,String regionCode,String requestScheme,String requestServerName,String requestServerPort,String contextPath,Map headers) throws Exception {
        int c = requestURI.indexOf("/", 1);
        if(c<0){
            c = requestURI.length();
        }
        String first = requestURI.substring(1,c);
        HttpProxyInfo proxyInfo = getHttpProxyHost(first,regionCode,requestURI,requestScheme,requestServerName,requestServerPort,contextPath,headers);
        return proxyInfo;
    }

    public boolean isProxy(HttpServletRequest req) {
        try{
            int c = req.getRequestURI().indexOf("/",1);
            if(c<0){
                c = req.getRequestURI().length();
            }
            String first = req.getRequestURI().substring(1,c);
            return isProxyPath(first);
        }catch (Exception e){
            return false;
        }
    }

    public boolean isProxyPath(String first){
        if(null != proxyList && proxyList.size()>0){
            if(null == tempProxyMap){
                tempProxyMap = new HashMap();
                for(HttpProxyInfo h:proxyList){
                    for(String sw:h.getStartwith()){
                        if(!tempProxyMap.containsKey(sw)){
                            tempProxyMap.put(sw,h);
                        }
                    }
                }
            }
            return tempProxyMap.containsKey(first);
        }
        return false;
    }

    ProxyPath getProxyPath(String requestScheme,String requestServerName,String requestServerPort,String contextPath
            ,String requestURI,String queryString,Hashtable headers ,String patternValue)throws Exception{
        if(StringUtils.isBlank(patternValue)) return null;
        if(StringUtils.isBlank(requestURI) || requestURI.equals("/")) return null;
        HttpProxyInfo proxyInfo = getProxyInfo(requestURI,patternValue,requestScheme,requestServerName,requestServerPort,contextPath,headers);
        if(null != proxyInfo && StringUtils.isNotBlank(proxyInfo.getTargetProxyHost())){
            ProxyPath p = new ProxyPath();
            String path = requestURI;
            String requestHost=requestScheme+"://"+requestServerName+":"+requestServerPort+contextPath;
            if(StringUtils.isNotBlank(path) && !path.startsWith("/")){
                path = "/"+path;
            }

            p.setProxyHttphost(proxyInfo.getTargetProxyHost());
            p.setProxyPath(path);
            p.setProxyContext(proxyInfo.getCode());
            p.setRequestBase("");
            p.setRequestHttphost(requestHost);
            String sq = getParameter(queryString,proxyInfo);
            if(StringUtils.isNotBlank(sq)){
                p.setRequestQueryString(sq);
            }

            return p;
        }
        return null;
    }

    private String getParameter(String queryStr,HttpProxyInfo p) throws UnsupportedEncodingException {
        if(StringUtils.isNotBlank(queryStr)){
            queryStr = StringUtils.toHtmlInput(queryStr);
            StringBuffer sb = new StringBuffer();
            String[] ps = StringUtils.splitExcludeToken(queryStr,"&",QuoTokenChars,false);
            for(int i=ps.length-1;i>=0;i--){
                if(StringUtils.isNotBlank(ps[i])){
                    String[] kv = StringUtils.splitExcludeToken(ps[i],"=",null,true);
                    if(kv.length>0 && StringUtils.isNotBlank(kv[0]) ){//&& sb.indexOf("&"+kv[0]+"=")<0
                        String v = "";
                        if(kv.length==2) v = kv[1];
                        if(StringUtils.isNotBlank(v) && StringUtils.isNotStartWith(v,QuoToken)){
                            v = URLEncoder.encode(URLDecoder.decode(v), "ISO-8859-1");
                        }

                        if(sb.length()==0){
                            sb.append(kv[0]).append("=").append(v);
                        }else{
                            sb.insert(0,kv[0]+"="+v+"&");
                        }
                    }

                }else{
                    sb.insert(0,"&");
                }
            }
            return sb.toString();
        }
        return null;
    }

    String getProxySessionName(String proxyContext){
        return "PROXY."+proxyContext+".SESSIONID";
    }

    ThreadLocal arrayid=new ThreadLocal();
    ThreadLocal sessionid=new ThreadLocal();

    void doRequestCookie(Hashtable<String,String> headHashtable,ProxyPath proxyPath,HttpSession session){
        //reset cookie
        String cookieName = null;
        if(headHashtable.containsKey("cookie")){
            cookieName="cookie";
        }
        if(headHashtable.containsKey("Cookie")){
            cookieName="Cookie";
        }
        //log.error("proxyRequestCookie:"+headHashtable.get(cookieName));
        if(StringUtils.isNotBlank(cookieName)){
            String cookieValue = headHashtable.get(cookieName);
            String[] ss = cookieValue.split(";");
            LinkedHashMap<String,String> cookieMap = new LinkedHashMap();
            for(String s:ss){
                if(s.contains("=")){
                    int n=s.indexOf("=");
                    if(n>0){
                        String k = s.substring(0,n).trim();
                        String v = s.substring(n+1);
                        cookieMap.put(k,v);
                    }else{
                        cookieMap.put(s,null);
                    }
                }
            }
            if(cookieMap.containsKey(sessionIDName)){
                cookieMap.remove(sessionIDName);
            }
            if(cookieMap.containsKey("arrayid")){
                //String v = cookieMap.get("arrayid");
                cookieMap.remove("arrayid");
                /*LinkedHashMap<String,String> temp = new LinkedHashMap();
                temp.put("arrayid",v);
                temp.putAll(cookieMap);
                cookieMap=temp;*/
            }
            Map map = (Map)session.getAttribute("proxy_cookies");
            if(null != map){
                Iterator its = map.keySet().iterator();
                while(its.hasNext()){
                    String key = (String)its.next();
                    if(isProxyName(proxyPath.getProxyContext(),key)){
                        cookieMap.put(getRealName(proxyPath.getProxyContext(),key),(String)map.get(key));
                    }
                }
            }
            /*if(cookieMap.containsKey("arrayid")){
                arrayid.set(cookieMap.get("arrayid"));
                cookieMap.remove("arrayid");
            }
            if(cookieMap.containsKey("proxy_arrayid")){
                cookieMap.put("arrayid",cookieMap.get("proxy_arrayid"));
                cookieMap.remove("proxy_arrayid");
            }
            if(cookieMap.containsKey(sessionIDName)){
                sessionid.set(cookieMap.get(sessionIDName));
                proxyPath.setRequestSessionID((String) cookieMap.get(sessionIDName));
                cookieMap.remove(sessionIDName);
            }

            String proxySessioinIDName= getProxySessionName(proxyPath.getProxyContext());
            if(cookieMap.containsKey(proxySessioinIDName)){
                cookieMap.put(sessionIDName, cookieMap.get(proxySessioinIDName));
                cookieMap.remove(proxySessioinIDName);
            }*/
            StringBuffer sb = new StringBuffer();
            Iterator its = cookieMap.keySet().iterator();
            while(its.hasNext()){
                String k = (String)its.next();
                String v = (String)cookieMap.get(k);
                if(sb.length()>0)sb.append("; ");
                if(null != v)
                    sb.append(k).append("=").append(v);
                else
                    sb.append(k);
            }
            //log.error("proxyRequestCookie:"+sb.toString());
            headHashtable.put(cookieName,sb.toString());
        }


/*
        if(headHashtable.containsKey("host")){
            //headHashtable.put("host",proxyPath.getProxyHttphost());
            headHashtable.remove("host");
        }
        if(headHashtable.containsKey("referer")){
            headHashtable.remove("referer");
            //headHashtable.put("referer",proxyPath.getProxyHttphost());
        }
*/
        if(headHashtable.containsKey("if-modified-since")){
            headHashtable.remove("if-modified-since");
        }
        if(headHashtable.containsKey("If-Modified-Since")){
            headHashtable.remove("If-Modified-Since");
        }


        if(headHashtable.containsKey("Content-Length")){
            headHashtable.remove("Content-Length");
        }
        if(headHashtable.containsKey("content-length")){
            headHashtable.remove("content-length");
        }
    }
    String getProxyName(String proxyContext,String name){
        return "PROXY_"+proxyContext+"_"+name;
    }

    boolean isProxyName(String proxyContext,String name){
        return name.startsWith("PROXY_"+proxyContext+"_");
    }

    String getRealName(String proxyContext,String name){
        if(name.startsWith("PROXY_")){
            log.error("proxyHead:"+name);
            return name.substring(("PROXY_"+proxyContext+"_").length());
        }
        return name;
    }
    Header[] doResponseCookie(Header[] hs,ProxyPath proxyPath,HttpSession session){
        List<Header> li = new ArrayList();
        if(null != hs){
            StringBuffer sb = new StringBuffer();
            for(Header h:hs){
                //System.out.println(h.getName());

                if(h.getName().equals("Content-Length")) continue;
                if(h.getName().equals("content-length")) continue;

                if("Set-Cookie".equals(h.getName())){
                    //设置代理SessionID
                    Map<String,String> cookieMap = new LinkedHashMap<String, String>();
                    String cookie = h.getValue();

                    String[] ss = cookie.split(";");
                    for(String s:ss){
                        int n=s.indexOf("=");
                        if(n>0){
                            String k = s.substring(0,n).trim();
                            String v = s.substring(n+1);
                            cookieMap.put(k,v);
                        }else{
                            cookieMap.put(s,null);
                        }
                    }
                    if(cookieMap.containsKey(sessionIDName)){
                        String proxySessionIDName = getProxyName(proxyPath.getProxyContext(),sessionIDName);
                        if(null==session.getAttribute("proxy_cookies"))session.setAttribute("proxy_cookies",new HashMap());
                        Map map = (Map)session.getAttribute("proxy_cookies");
                        map.put(proxySessionIDName,cookieMap.get(sessionIDName));
                        //cookieMap.put(proxySessionIDName,cookieMap.get(sessionIDName));
                        cookieMap.remove(sessionIDName);
                    }

                    if(cookieMap.containsKey("arrayid")){
                        String proxyIDName = getProxyName(proxyPath.getProxyContext(),"arrayid");
                        if(null==session.getAttribute("proxy_cookies"))session.setAttribute("proxy_cookies",new HashMap());
                        Map map = (Map)session.getAttribute("proxy_cookies");
                        map.put(proxyIDName,cookieMap.get("arrayid"));
                        cookieMap.remove("arrayid");
                    }

                    Iterator its = cookieMap.keySet().iterator();
                    while(its.hasNext()){
                        String k = (String)its.next();
                        String v = cookieMap.get(k);
                        if(sb.length()>0)sb.append(";");
                        if(null != v)
                            sb.append(" ").append(k).append("=").append(v);
                        else
                            sb.append(" ").append(k);
                    }
                }else{
                    li.add(h);
                }


            }
            if(sb.length()>0){
                li.add(new BasicHeader("Set-Cookie",sb.toString()));
            }
            return li.toArray(new Header[0]);
        }
        return null;
    }

    HttpEntity createProxyPostEntity(HttpServletRequest request)throws IOException {
        InputStreamEntity reqEntity = new InputStreamEntity(request.getInputStream(),request.getContentLength());
        return reqEntity;
    }

    HttpResponse getResponse(HttpServletRequest request,String method,Hashtable<String,String> headHashtable,ProxyPath proxyPath) throws Exception {
        HttpClient httpclient = new DefaultHttpClient();
        httpclient.getParams().setParameter(ClientPNames.HANDLE_REDIRECTS, false);
        //do header
        Header[] headers=null;
        if(null != headHashtable && headHashtable.size()>0){
            doRequestCookie(headHashtable,proxyPath,request.getSession());
            headers = getHeaderFromTable(headHashtable);
        }
        String sessionId = request.getSession().getId();
        //proxy request path
        /*Object old_target = FMC.getCacheData(FMC_TYPE,sessionId+"|"+proxyPath.getRequestHttphost());
        if(null != old_target && !"".equals(old_target)){
            proxyPath.setProxyHttphost(old_target.toString());
        }*/

        BasicHttpContext context = new BasicHttpContext();
        HttpResponse response=null;
        if("GET".equals(method)){
            HttpGet req = new HttpGet(proxyPath.getProxyRequestPath());
            if(null != headers)
                req.setHeaders(headers);
            response = httpclient.execute(req,context);

            if(log.isDebugEnabled()){
                System.out.println(req.getRequestLine()+"->"+response.getStatusLine()+"-:");
            }
        }else if("POST".equals(method)){
            HttpPost post = new HttpPost(proxyPath.getProxyRequestPath());
            if(null != headers)
                post.setHeaders(headers);
            HttpEntity httpEntity = createProxyPostEntity(request);
            if(null != httpEntity)
                post.setEntity(httpEntity);
            response = httpclient.execute(post,context);
            if(log.isDebugEnabled()){
                System.out.println(post.getRequestLine()+"->"+response.getStatusLine()+"-:");
            }
        }else {
            //System.out.println(method);
        }
        //如果有跳转，获取跳转后的访问地址
        if(null != context.getAttribute(targethost)){
            String target = context.getAttribute(targethost).toString();
            proxyPath.setProxyTargetHost(target);
            /*if(!proxyPath.getProxyHttphost().equals(proxyPath.getProxyTargetHost()))
                FMC.addCacheData(FMC_TYPE,sessionId+"|"+proxyPath.getProxyHttphost(),proxyPath.getProxyTargetHost());*/
        }

        return response;
    }

    void setHeaderToHttpServletResponse(HttpServletRequest request,HttpResponse proxyResponse,HttpServletResponse res,ProxyPath proxyPath){
        HttpEntity content = proxyResponse.getEntity();
        if(null  != content){
            Header[] hs = proxyResponse.getAllHeaders();
            hs = doResponseCookie(hs,proxyPath,request.getSession());
            if(null != hs){
                for(Header h:hs){
                    res.addHeader(h.getName(),h.getValue());
                }
            }
            if(null != content.getContentType())
                res.setContentType(content.getContentType().getValue());
            if(content.getContentLength() !=0)
                res.setContentLength((int) content.getContentLength());
            res.setStatus(proxyResponse.getStatusLine().getStatusCode());
        }
    }

    void setContentToHttpServletResponse(HttpResponse proxyResponse,HttpServletResponse res,String base) throws IOException {
        //header code:204 No Content, 304 Not Modified, 205 Reset Content
        if(!ArrayUtils.isInIntArray(new int[]{304, 204, 205}, proxyResponse.getStatusLine().getStatusCode())){
            BufferedOutputStream out = new BufferedOutputStream(res.getOutputStream());
            HttpEntity content = proxyResponse.getEntity();
            if(null != content.getContentType() && content.getContentType().getValue().contains("html")){
                out.write(base.getBytes());
            }
            InputStream proxyIn = new BufferedInputStream(content.getContent());
            while (true){
                int i;
                if ((i = proxyIn.read()) < 0)
                    break;
                out.write(i);
            }
            if (proxyIn != null)
                proxyIn.close();
            if (out != null)
            {
                out.flush();
                out.close();
            }
        }
    }


    void configProxy(){
        XMLMakeup[] proxs = getXML().getChild("proxys");

        if(proxs!=null && proxs.length>0){
            XMLMakeup[] els = proxs[0].getChild("proxy");
            proxyList = new LinkedList<HttpProxyInfo>();
            for(XMLMakeup e:els){
                String code = e.getProperties().getProperty("key");
                String encoding = e.getChild("encoding")[0].getText();
                XMLMakeup[] phs = e.getChild("proxyhosts");
                HashMap<String,String> map = new HashMap();
                String[] startwiths=null;
                if(null != phs){
                    XMLMakeup[] ps = phs[0].getChild("proxyhost");
                    String hostcode,host;
                    for(XMLMakeup p:ps){
                        hostcode = p.getProperties().getProperty("pattern");
                        host = p.getText();
                        map.put(hostcode,host);
                    }
                }
                XMLMakeup[] sw = e.getChild("startwith");
                String txt = sw[0].getText();
                if(null != sw && StringUtils.isNotBlank(txt)){
                    startwiths=txt.split(",");
                }
                if(StringUtils.isNotBlank(code) && map.size()>0 && null != startwiths && startwiths.length>0){
                    HttpProxyInfo proxy = new HttpProxyInfo();
                    proxy.setCode(code);
                    proxy.setStartwith(startwiths);
                    proxy.setHttpHosts(map);
                    proxy.setEncoding(encoding);
                    proxyList.add(proxy);
                }

            }

        }
    }

    @Override
    public Object doSomeThing(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        RequestParameters par = (RequestParameters)env;

        String method = (String)input.get("Method");
        String patternValue = (String)input.get("Pattern");
        String requestScheme= (String)input.get("Scheme");
        String requestServerName= (String)input.get("ServerName");
        String requestServerPort= String.valueOf(input.get("ServerPort"));
        String contextPath= (String)input.get("ContextPath");
        String requestURI=(String)input.get("RequestURI");
        String queryString=(String)input.get("QueryString");
        Hashtable headers=(Hashtable)input.get("Headers");
        HttpServletRequest request = (HttpServletRequest)input.get("Request");
        HttpServletResponse response = (HttpServletResponse)input.get("Response");
        if(StringUtils.isNotBlank(patternValue)){//match fix host
            ProxyPath proxyPath=getProxyPath(requestScheme,requestServerName,requestServerPort,contextPath,requestURI,queryString, headers ,patternValue);
            if(null != proxyPath && StringUtils.isNotBlank(proxyPath.getProxyRequestPath())){
                HttpResponse proxyResponse = getResponse(request,method,headers,proxyPath);
                if(null != proxyResponse){
                    setHeaderToHttpServletResponse(request,proxyResponse,response,proxyPath);
                    setContentToHttpServletResponse(proxyResponse, response, proxyPath.getRequestBase());
                }
                par.setStop();
                //log.error("mycookie:"+sb.toString());
            }
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
        return false;
    }


    public HttpProxyInfo getHttpProxyHost(String startwith,String patternValue,String requestURI,String requestScheme,String requestServerName,String requestServerPort,String contextPath,Map headers){
        if(null != proxyList && proxyList.size()>0){
            if(null == tempProxyMap){
                tempProxyMap = new HashMap();
                for(HttpProxyInfo h:proxyList){
                    for(String sw:h.getStartwith()){
                        if(!tempProxyMap.containsKey(sw)){
                            tempProxyMap.put(sw,h);
                        }
                    }
                }
            }
            boolean isproxy=false;
            if(tempProxyMap.containsKey(startwith)){
                isproxy=true;
            }else if(isNotPage(requestURI)){

                String ref = (String)headers.get("referer");
                if(StringUtils.isNotBlank(ref)){
                    String rootURL = requestScheme+"://"+requestServerName;
                    if(ref.length()>=rootURL.length() && ref.charAt(rootURL.length())==':'){
                        rootURL=rootURL+ ":"+requestServerPort;
                    }
                    rootURL=rootURL+contextPath;
                    String s= ref.substring(rootURL.length());
                    int c = s.indexOf("/",1);
                    if(c<0){
                        c = s.length();
                    }
                    startwith = s.substring(1,c);
                    isproxy = tempProxyMap.containsKey(startwith);
                }
            }
            if(isproxy){
                HttpProxyInfo ret = new HttpProxyInfo();
                HttpProxyInfo origon = tempProxyMap.get(startwith);
                ret.setCode(origon.getCode());
                ret.setEncoding(origon.getEncoding());
                ret.setTargetProxyHost(origon.getHttpHosts().get(patternValue));
                if(StringUtils.isNotBlank(ret.getTargetProxyHost()))
                    return ret;
            }

        }
        return null;
    }

    public class HttpProxyInfo{
        String[] startwith;
        String code;
        String targetProxyHost;
        String encoding;
        Map<String,String> httpHosts;

        public String getEncoding() {
            return encoding;
        }

        public void setEncoding(String encoding) {
            this.encoding = encoding;
        }

        public String getTargetProxyHost() {
            return targetProxyHost;
        }

        public void setTargetProxyHost(String targetProxyHost) {
            this.targetProxyHost = targetProxyHost;
        }

        public String[] getStartwith() {
            return startwith;
        }

        public void setStartwith(String[] startwith) {
            this.startwith = startwith;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public Map<String,String> getHttpHosts() {
            return httpHosts;
        }

        public void setHttpHosts(Map<String,String> httpHost) {
            this.httpHosts = httpHost;
        }


    }

    boolean isNotPage(String u){
        int n = u.lastIndexOf(".");
        if(n>0){
            String sub = u.substring(n+1);
            sub = sub.toLowerCase();
            return !ArrayUtils.isInStringArray(PAGE_ARRAY,sub);
        }
        return false;
    }
}
