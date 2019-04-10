package com.octopus.isp.bridge.launchers.impl.pageframe.util;

import com.alibaba.fastjson.JSON;
import com.octopus.isp.bridge.launchers.impl.pageframe.channel.IPageCodePathMapping;
import com.octopus.isp.ds.ClientInfo;
import com.octopus.isp.ds.RequestParameters;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.*;

/**
 * User: wfgao_000
 * Date: 15-8-19
 * Time: 下午3:22
 */
public class HttpUtils extends XMLObject {
    transient static Log log = LogFactory.getLog(HttpUtils.class);
    IPageCodePathMapping pageCodePathMapping;
    static String[] TYPE={
            "ez=application/andrew-inset",
            "hqx=application/mac-binhex40",
            "cpt=application/mac-compactpro",
            "doc=application/msword",
            "bin=application/octet-stream",
            "dms=application/octet-stream",
            "lha=application/octet-stream",
            "lzh=application/octet-stream",
            "exe=application/octet-stream",
            "class=application/octet-stream",
            "so=application/octet-stream",
            "dll=application/octet-stream",
            "oda=application/oda",
            "pdf=application/pdf",
            "ai=application/postscript",
            "eps=application/postscript",
            "ps=application/postscript",
            "smi=application/smil",
            "smil=application/smil",
            "mif=application/vnd.mif",
            "xls=application/vnd.ms-excel",
            "ppt=application/vnd.ms-powerpoint",
            "wbxml=application/vnd.wap.wbxml",
            "wmlc=application/vnd.wap.wmlc",
            "wmlsc=application/vnd.wap.wmlscriptc",
            "bcpio=application/x-bcpio",
            "vcd=application/x-cdlink",
            "pgn=application/x-chess-pgn",
            "cpio=application/x-cpio",
            "csh=application/x-csh",
            "dcr=application/x-director",
            "dir=application/x-director",
            "dxr=application/x-director",
            "dvi=application/x-dvi",
            "spl=application/x-futuresplash",
            "gtar=application/x-gtar",
            "hdf=application/x-hdf",
            "js=application/x-javascript",
            "skp=application/x-koan",
            "skd=application/x-koan",
            "skt=application/x-koan",
            "skm=application/x-koan",
            "latex=application/x-latex",
            "nc=application/x-netcdf",
            "cdf=application/x-netcdf",
            "sh=application/x-sh",
            "shar=application/x-shar",
            "swf=application/x-shockwave-flash",
            "sit=application/x-stuffit",
            "sv4cpio=application/x-sv4cpio",
            "sv4crc=application/x-sv4crc",
            "tar=application/x-tar",
            "tcl=application/x-tcl",
            "tex=application/x-tex",
            "texinfo=application/x-texinfo",
            "texi=application/x-texinfo",
            "t=application/x-troff",
            "tr=application/x-troff",
            "roff=application/x-troff",
            "man=application/x-troff-man",
            "me=application/x-troff-me",
            "ms=application/x-troff-ms",
            "ustar=application/x-ustar",
            "src=application/x-wais-source",
            "xhtml=application/xhtml+xml",
            "xht=application/xhtml+xml",
            "zip=application/zip",
            "au=audio/basic",
            "snd=audio/basic",
            "mid=audio/midi",
            "midi=audio/midi",
            "kar=audio/midi",
            "mpga=audio/mpeg",
            "mp2=audio/mpeg",
            "mp3=audio/mpeg",
            "aif=audio/x-aiff",
            "aiff=audio/x-aiff",
            "aifc=audio/x-aiff",
            "m3u=audio/x-mpegurl",
            "ram=audio/x-pn-realaudio",
            "rm=audio/x-pn-realaudio",
            "rpm=audio/x-pn-realaudio-plugin",
            "ra=audio/x-realaudio",
            "wav=audio/x-wav",
            "pdb=chemical/x-pdb",
            "xyz=chemical/x-xyz",
            "bmp=image/bmp",
            "gif=image/gif",
            "ief=image/ief",
            "jpeg=image/jpeg",
            "jpg=image/jpeg",
            "jpe=image/jpeg",
            "png=image/png",
            "tiff=image/tiff",
            "tif=image/tiff",
            "djvu=image/vnd.djvu",
            "djv=image/vnd.djvu",
            "wbmp=image/vnd.wap.wbmp",
            "ras=image/x-cmu-raster",
            "pnm=image/x-portable-anymap",
            "pbm=image/x-portable-bitmap",
            "pgm=image/x-portable-graymap",
            "ppm=image/x-portable-pixmap",
            "rgb=image/x-rgb",
            "xbm=image/x-xbitmap",
            "xpm=image/x-xpixmap",
            "xwd=image/x-xwindowdump",
            "igs=model/iges",
            "iges=model/iges",
            "msh=model/mesh",
            "mesh=model/mesh",
            "silo=model/mesh",
            "wrl=model/vrml",
            "vrml=model/vrml",
            "css=text/css",
            "html=text/html",
            "htm=text/html",
            "asc=text/plain",
            "txt=text/plain",
            "rtx=text/richtext",
            "rtf=text/rtf",
            "sgml=text/sgml",
            "sgm=text/sgml",
            "tsv=text/tab-separated-values",
            "wml=text/vnd.wap.wml",
            "wmls=text/vnd.wap.wmlscript",
            "etx=text/x-setext",
            "xsl=text/xml",
            "xml=text/xml",
            "mpeg=video/mpeg",
            "mpg=video/mpeg",
            "mpe=video/mpeg",
            "qt=video/quicktime",
            "mov=video/quicktime",
            "mxu=video/vnd.mpegurl",
            "avi=video/x-msvideo",
            "movie=video/x-sgi-movie",
            "ice=x-conference/x-cooltalk"
    };
    static Map<String,String> typeMap = new HashMap();
    static {
        for(String ts:TYPE){
            String[] t = ts.split("=");
            typeMap.put(t[0].trim(),t[1].trim());
        }
    }
    //存放缓存页面
    static Map<String,String> pageCacheMap=new HashMap<String, String>();
    static Map<String,byte[]> itemCacheMap=new HashMap<String, byte[]>();
    String startwith;
    boolean isCache;
    String[] pagesuffix;
    String[] i18ns;
    public HttpUtils(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        startwith=xml.getFirstCurChildText("property","name","startwith");
        isCache=StringUtils.isTrue(xml.getFirstCurChildText("property", "name", "cachepage"));
        if(null != xml.getFirstCurChildText("property","name","pagesuffix"))
        pagesuffix =xml.getFirstCurChildText("property","name","pagesuffix").split(",");
        if(null != xml.getFirstCurChildText("property","name","i18n"))
        i18ns =xml.getFirstCurChildText("property","name","i18n").split(",");
        pageCodePathMapping = (IPageCodePathMapping)Class.forName(xml.getFirstCurChildText("property","name","pagecodepathmapping")).newInstance();

    }

    @Override
    public void notifyObject(String op, Object obj) throws Exception {

    }

    @Override
    public void addTrigger(Object cond, InvokeTaskByObjName task) throws Exception {

    }

    @Override
    public void destroy() throws Exception {

    }

    @Override
    public void initial() throws Exception {

    }

    void redirectPageSource(XMLParameter env,HttpServletRequest request,HttpServletResponse response,String pageCode,String pageNum,String url,String append)throws Exception{
        String pageseq="";
        if(StringUtils.isNotBlank(pageCode)){
            pageseq = createThePageSeq(request, pageCode);
        }else{
            if(null !=pageNum) {
                pageCode = getPageCode(request,request.getSession().getId(),pageNum);
                if(isSubPage(request, request.getSession().getId(), pageNum,pageCode,url)){
                    pageseq=pageNum;
                }else{
                    throw  new Exception("the page do not relate gate-page,can not access.");
                }
            }
        }

        String basepath = null;
        if(StringUtils.isNotBlank(pageseq)){
            basepath=request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+request.getContextPath()+"/"+startwith+"/"+pageseq+url.substring(0,url.lastIndexOf("/")+1);
        }else{
            basepath=request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+request.getContextPath()+url.substring(0,url.lastIndexOf("/")+1);
        }
        response.setContentType("text/html;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        String cotent="";
        if(isCache && StringUtils.isBlank(append)){
            if(!pageCacheMap.containsKey(url)){
                InputStream in = request.getServletContext().getResourceAsStream(url);
                if(null != in){
                    byte[] bs = new byte[in.available()];
                    in.read(bs);
                    in.close();
                    pageCacheMap.put(url,new String(bs,"UTF-8"));
                }else{
                    throw new Exception("not find resource by url["+url+"]");
                }

            }
            cotent = pageCacheMap.get(url);
        }else{
            InputStream in = request.getSession().getServletContext().getResourceAsStream(url);
            if(in == null)
                throw new Exception("not find the source by "+url);
            byte[] bs = new byte[in.available()];
            in.read(bs);
            in.close();
            cotent=new String(bs,"UTF-8");
            if(StringUtils.isNotBlank(append))
                cotent+=append;
        }
        cotent = (String)getI18n(env,url,cotent);

        response.getWriter().println("<!DOCTYPE HTML><base href=\"" + basepath + "\"/>" + cotent);
        response.flushBuffer();
    }

    Object getI18n(XMLParameter env,String url,Object cotent) throws Exception {
        if(url.contains(".") && ArrayUtils.isInStringArray(i18ns,url.substring(url.lastIndexOf(".")+1))) {
            String cn;
            if(cotent instanceof byte[]){
                cn=new String((byte[])cotent);
            }else{
                cn = (String)cotent;
            }
            XMLDoObject i18o = (XMLDoObject) getObjectById("i18n");
            if (null != i18o) {
                HashMap input = new HashMap();
                input.put("data", cn);
                input.put("url", url);
                Object t = i18o.doSomeThing(null, env, input, null, null);
                if (null != t && t instanceof String) {
                    cn = (String) t;
                }
            }
            if(cotent instanceof byte[]){
                return cn.getBytes();
            }else{
                return cn;
            }
        }
        return cotent;
    }

    void redirectJspPageSource(HttpServletRequest request,HttpServletResponse response,String pageCode,String pageNum,String url,String append)throws Exception{
        response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
        String newLocn = "/login.html";
        response.setHeader("Location",newLocn);
        response.sendRedirect(url);

    }

    public static void setRedirect(XMLParameter env){
        env.addParameter("isRedirect",true);
    }
    public boolean isRedirect(XMLParameter env){
        Object o = env.getParameter("isRedirect");
        if(null != o && (Boolean)o){
            return true;
        }
        return false;
    }
    public void setLogin(XMLParameter env){
        env.addParameter("isLogin",true);
    }
    public boolean isLogint(XMLParameter env){
        Object o = env.getParameter("isLogin");
        if(null != o && (Boolean)o){
            return true;
        }
        return false;
    }

    String getType(String s){
        String t = typeMap.get(s);
        if(StringUtils.isNotBlank(t))
            return t;
        else
            return "text/html";
    }



    /**
     * 以sessionId,pageCode为关键字创建pageseq并保存FMC,返回pageseq
     * @param pageCode
     */
    String createThePageSeq(HttpServletRequest par,String pageCode) throws Exception {
        String sessionId=par.getSession().getId();
        String ret = (sessionId+"|"+pageCode).hashCode()+"";
        String key =sessionId+"|"+ret;
        par.getSession().setAttribute(key,pageCode);
        return ret;
    }
    /**
     * 判断是否是子页面请求,是否已经打开入口页面，该次访问的页面是否入口页面的子页面。
     * @return
     */
    boolean isSubPage(HttpServletRequest request,String sessionId,String pageseq,String pageCode,String url) throws Exception {
        if(StringUtils.isNotBlank(pageseq) && StringUtils.isNumeric(pageseq)){
            sessionId=request.getSession().getId();
            //查找页面关联配置，检查是否子页面
            String refer = request.getHeader("referer");
            if(StringUtils.isNotBlank(refer)){
                String r[] =getPageSeqAndCode(refer);
                if(StringUtils.isNotBlank(r[0])){
                    if(null != pageseq && pageseq.equals(r[0]))
                        return true;
                    else
                        return false;
                }
                if(StringUtils.isNotBlank(r[1])){
                    if(null != pageCode && pageCode.equals(r[1]))
                        return true;
                    else
                        return false;
                }
                //可以继续判断当前uri和初始pagecode是否一致

            }
        }
        return false;

    }
   String getPageSeq(String uri){
       if(uri.contains("/"+startwith+"/")){
           int n = uri.indexOf("/"+startwith+"/");
           int ln = uri.lastIndexOf("/"+startwith+"/");
           int len = startwith.length()+2;
           if(ln==n)
               ln=n+len;
           else
               ln=ln+len;
           int m = uri.indexOf("/",ln);
           if(m>n){
               String seq = uri.substring(ln,m);
               if(StringUtils.isNumeric(seq)){
                   return seq;
               }
           }
       }
       return null;
   }
   public String[] getPageSeqAndCode(String uri){
       String pageSeq=null,pagecode=null,url=null;
       if(uri.contains("/"+startwith+"/")){
           int n = uri.indexOf("/"+startwith+"/");
           int ln = uri.lastIndexOf("/"+startwith+"/");
           int len = startwith.length()+2;
           if(ln==n)
               ln=n+len;
           else
               ln=ln+len;
           int m = uri.indexOf("/",ln);
           if(m>n){
               String seq = uri.substring(ln,m);
               if(StringUtils.isNumeric(seq)){
                   pageSeq=seq;
               }
               url=uri.substring(m);
           }else{
               pagecode=  uri.substring(ln);
           }
       }
       return new String[]{pageSeq,pagecode,url};
   }
    public String getPageCode(HttpServletRequest request,String sessionId,String pageseq)throws Exception{
        sessionId=request.getSession().getId();
        if(StringUtils.isNotBlank(pageseq) && StringUtils.isNumeric(pageseq)){
            String key = sessionId+"|"+pageseq;
            Object ret = request.getSession().getAttribute(key);
            return (String)ret;
        }
        return null;
    }

    /**
     * 真实页面路径请求，并缓存页面
     * @param request
     * @param response
     * @param url
     * @throws Exception
     */
    void redirectNoPageSource(XMLParameter env,HttpServletRequest request,HttpServletResponse response,String url)throws Exception{
        String type = getType(url.substring(url.lastIndexOf(".")+1).toLowerCase());
        response.setContentType(type + ";charset=UTF-8");

        response.setCharacterEncoding("UTF-8");
        byte[] cotent;
        //System.out.println(url);
        if(isCache){
            if(!itemCacheMap.containsKey(url)){
                InputStream in = request.getSession().getServletContext().getResourceAsStream(url);
                if(null != in){
                    byte[] bs = new byte[in.available()];
                    in.read(bs);
                    in.close();
                    itemCacheMap.put(url,bs);
                }else{
                    log.error("not find resource:"+url);
                }
            }
            cotent = itemCacheMap.get(url);
        }else{
            InputStream in = request.getSession().getServletContext().getResourceAsStream(url);
            if(null != in) {
                byte[] bs = new byte[in.available()];
                in.read(bs);
                in.close();
                cotent = (byte[])getI18n(env,url,bs);
                response.getOutputStream().write(cotent);
                response.flushBuffer();
            }else
            log.error("the resource[" + url + "] is not exist.");
            if(null != in) {
                in.close();
            }
        }
    }

    public void writeError(RequestParameters pars) throws Exception {
        Object o = ((ResultCheck)pars.getResult()).getRet();
        if(o instanceof Exception){
            Throwable e= ExceptionUtils.getCause((Exception)o);
            if(null == e)
                 e = (Exception)o;
             redirect2Error(pars,"<script language=\"javascript\">document.getElementById(\"error_msg\").innerHTML=\""+e.getMessage()+"\";</script>",null);
        }else{
            HttpServletResponse response= (HttpServletResponse)pars.getParameter("${response}");

            response.setContentType("text/html;charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            if(((ResultCheck)pars.getResult()).getRet() instanceof Throwable){
                response.getWriter().print(ExceptionUtils.getFullStackTrace(ExceptionUtils.getCause((Throwable) ((ResultCheck) pars.getResult()).getRet())));
            }else{
                response.getWriter().print (o.toString());
            }
            response.flushBuffer();
        }
    }
    /**
     * 判断是否缓存,根据真实地址访问
     * @param env
     * @throws Exception
     */

    public void redirect(RequestParameters env,String uri,String append)throws Exception{
        HttpServletRequest request = (HttpServletRequest)env.get("${request}");
        HttpServletResponse response = (HttpServletResponse)env.get("${response}");

        if(StringUtils.isBlank(uri))
            uri = request.getRequestURI().substring(((String) request.getContextPath()).length());

        //get page seq
        String pageSeq=null;
        String pagecode=null;
        String[] r = getPageSeqAndCode(uri);
        pageSeq=r[0];
        pagecode=r[1];
        if(StringUtils.isNotBlank(pagecode)){
            uri=getRealPathByPageCode(pagecode);
        }
        if(StringUtils.isNotBlank(r[2])){
            uri = r[2];
        }else{
            //get client kind
            ClientInfo clientInfo = env.getClientInfo();
            String header = clientInfo.getHeaderPath();
            if(!uri.startsWith(header+"/")){
                if(null  != header && !"".equals(header)){
                    if(uri.startsWith("/"))
                        uri = header+uri;
                    else
                        uri = header+"/"+uri;
                }
            }
        }

        String sessionKey = (String)env.getRequestProperties().get("SessionKey");
        /*String cookie = response.getHeader("Set-Cookie");
        if(StringUtils.isBlank(cookie)){
            cookie = (String)env.getRequestProperties().get("${RequestCookie}");
        }
        cookie=delete(cookie,sessionKey);
        if(StringUtils.isNotBlank(cookie)){
            cookie =sessionKey+"="+env.getRequestCookies().get(sessionKey)+";"+cookie;
        }else{
            cookie=  sessionKey+"="+env.getRequestCookies().get(sessionKey);//+"; HttpOnly";
        }
        response.setHeader("Set-Cookie",cookie);
        */
        if(null != env.getRequestCookies().get(sessionKey)){
            String cookieid=  sessionKey+"="+env.getRequestCookies().get(sessionKey);
            response.addHeader("Set-Cookie",cookieid);
        }
        setRedirect(env);
        //页面资源
        if(StringUtils.isNotBlank(pagecode) || (uri.contains(".") && ArrayUtils.isInStringArray(pagesuffix,uri.substring(uri.lastIndexOf(".")+1)))){
            if("jsp".equals(uri.substring(uri.lastIndexOf(".")+1))){
                redirectJspPageSource(request, response, pagecode, pageSeq, uri, append);
            }else {
                redirectPageSource(env,request, response, pagecode, pageSeq, uri, append);
            }
        }else{//非页面资源
            if(uri.indexOf(".")>0) {
                redirectNoPageSource(env, request, response, uri);
            }else{
                request.getRequestDispatcher(uri).forward(request,response);
            }
        }

        //后序动作不执行直接返回
        ((RequestParameters) env).setStop();
    }

    public void redirect2Error(RequestParameters par,String errormsg,String i18n)throws Exception{
        String text = getXML().getFirstCurChildText("property","name","errorpage");
        if(StringUtils.isNotBlank(text)){
            redirect(par,text,errormsg);

        }else{
            throw new Exception("not config errorpage");
        }
    }
    public static void redirectError(XMLObject o,HttpServletRequest request,HttpServletResponse response,String clienttype,Exception e){
        String url=o.getObjectById("HttpUtils").getXML().getFirstCurChildText("property","name","errorpage");
        String errormsg = "<script language=\"javascript\">document.getElementById(\"error_msg\").innerHTML=\""+ ExceptionUtils.getRootCauseMessage(e)+"\";</script>";
        if(StringUtils.isNotBlank(url)){
            InputStream in = request.getSession().getServletContext().getResourceAsStream("/"+clienttype+"/"+url);
            if(null != in) {
                try {
                    byte[] bs = new byte[in.available()];
                    in.read(bs);
                    in.close();
                    response.getOutputStream().write(bs);
                    response.getOutputStream().write(errormsg.getBytes());
                    response.flushBuffer();
                }catch (Exception ep){

                }
            }
        }else{
            log.error("not config errorpage");
        }
    }
    public void setSession2Cookie(RequestParameters env,String sessionId){
        HttpServletResponse response = (HttpServletResponse)env.get("${response}");
        String sessionKey = (String)env.getRequestProperties().get("SessionKey");
        if(null != sessionKey){
            env.getRequestCookies().put(sessionKey,sessionId);
            String cookieid=  sessionKey+"="+env.getRequestCookies().get(sessionKey);
            response.addHeader("Set-Cookie",cookieid);
        }
    }

    String delete(String cookie,String key){
        cookie = StringUtils.removeTags(cookie,key,";");
        if(StringUtils.isNotBlank(cookie)){
            if(cookie.contains(key)){
                cookie=cookie.trim();
                int n = cookie.indexOf(key);
                if(n==0){
                    cookie="";
                }else if(n>0){
                    cookie=cookie.substring(0,cookie.length()-2);
                }
                return cookie;
            }else{
                return cookie;
            }
        }
        return null;
    }

    public String getRealPathByPageCode(String pagecode){
        if(null != pageCodePathMapping){
            return pageCodePathMapping.getRealPathByCode(pagecode);
        }
        return "";
        //return "/home.html";
    }


    public static Object invokeRestful(String url,String method,Map header,Object pars){
        HttpURLConnection conn=null;
        //long l = System.currentTimeMillis();
        try{
            if(null != url){
                if(log.isDebugEnabled()){
                    log.debug("url:"+url);
                }
                URL u = new URL(url);
                conn = (HttpURLConnection)u.openConnection();
                conn.setConnectTimeout(40000);
                conn.setReadTimeout(40000);
                conn.setDoOutput(true);// 打开写入属性
                conn.setDoInput(true);// 打开读取属性

                if(org.apache.commons.lang.StringUtils.isNotBlank(method)) {
                    conn.setRequestMethod(method);
                }
                if(null != header) {
                    Iterator its = header.keySet().iterator();
                    while(its.hasNext()) {
                        String k = (String)its.next();
                        String v = (String)header.get(k);
                        conn.setRequestProperty(k,v);
                    }
                }
                if(null != pars) {
                    String data=null;
                    if(pars instanceof JSONObject){
                        data = ((JSONObject)pars).toString();
                    }else if(pars instanceof JSONArray){
                        data = ((JSONArray)pars).toString();
                    }
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), "UTF-8"));
                    if(null != data) {
                        out.write(data);
                    }
                    out.flush();
                    out.close();
                }
                //ByteArrayOutputStream out = new ByteArrayOutputStream() ;
                try{
                    int response_code = conn.getResponseCode();
                    StringBuffer sb  = new StringBuffer();
                    if (response_code == HttpURLConnection.HTTP_OK) {
                        InputStream in = conn.getInputStream();
                        //                InputStreamReader reader = new InputStreamReader(in,charSet);
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
                        //byte [] b  = new byte[1024];
                        String line = null;
                        while ((line = reader.readLine()) != null) {
                            if(log.isDebugEnabled())
                                log.debug(line);
                            //System.out.println(new String(line.getBytes()));
                            //out.write(line.getBytes());
                            sb.append(line);
                        }
                        String r = sb.toString();
                        if(null != r && !"".equals(r)) {
                            if (r.startsWith("[")||r.startsWith("{")){
                                return JSON.parse(r);
                            }else{
                                return r;
                            }
                        }else{
                            return null;
                        }
                    }
                }catch (Exception e){
                    if(!(e instanceof SocketException || e instanceof SocketTimeoutException)){
                        log.error(url,e);
                    }
                    throw e;
                }
                //System.out.println("lost:"+(System.currentTimeMillis()-l));
            }
            return null;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }finally {
            if(conn !=null){
                conn.disconnect();
            }
        }
    }
    public static void main(String[] args){
        try{
            HashMap header = new HashMap();
            header.put("Content-Type","application/json;charset=UTF-8");
            header.put("Accept","application/json");
            header.put("Authorization","Basic 1212121");
            JSONObject apr = new JSONObject();
            apr.put("orderNo","12");
            apr.put("Signature","12");
            apr.put("Status","1");
            Object o = invokeRestful("http://10.11.18.139:8080/sim/sentSimStatus","POST",header,apr);
            System.out.println(o);
        }catch (Exception e){
            e.printStackTrace();
        }
    }


}
