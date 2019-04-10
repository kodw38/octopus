package com.octopus.isp.bridge.launchers.impl;

import com.octopus.isp.bridge.IBridge;
import com.octopus.isp.bridge.launchers.impl.pageframe.SessionManager;
import com.octopus.isp.ds.DataEnv;
import com.octopus.isp.ds.RequestParameters;
import com.octopus.isp.utils.CXFUtil;
import com.octopus.isp.utils.ISPUtil;
import com.octopus.utils.alone.SNUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cls.POJOUtil;
import com.octopus.utils.exception.ErrMsg;
import com.octopus.utils.exception.ExceptionUtil;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import com.octopus.utils.xml.desc.Desc;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by kod on 2017/2/27.
 */
public class CXFWebServiceLauncher extends XMLDoObject{
    transient static Log log = LogFactory.getLog(CXFWebServiceLauncher.class);
    String host;
    boolean isdevelop = true;
    static XMLDoObject statHandler;
    String compilepath = null;
    String hostport = null;
    String keypath = null;
    String pwd = null;
    String storepwd = null;
    static DataEnv env=null;
    boolean isThrowException;
    static IBridge bridge;
    static List<String> cache = new ArrayList();

    public CXFWebServiceLauncher(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        String cfg = xml.getProperties().getProperty("config");
        Map m = StringUtils.convert2MapJSONObject(cfg);
        if (null != m) {
            compilepath = (String) getEmptyParameter().getExpressValueFromMap((String) m.get("compilepath"),this);
        }
        env = (DataEnv)getPropertyObject("env");
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;
    }

    private static void getHeaders(HttpServletRequest paramHttpServletRequest, Hashtable paramHashtable)
    {
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
                paramHashtable.put(str1, v);
                str2.delete(0, str2.length());
            }
        }


    }
    /**
     * 执行外部传入调用的参数,需要静态static方法
     *
     * @param receive
     * @return
     */
    public static Object invoke(String srvName, String retClass, String parNames, Object receive) throws Exception {
        try {
            RequestParameters par = new RequestParameters();
            Message message = PhaseInterceptorChain.getCurrentMessage();
            if(null != message){
                HttpServletRequest request = (HttpServletRequest)message.get(AbstractHTTPDestination.HTTP_REQUEST);
                if(null != request) {
                    getHeaders(request, par.getRequestHeaders());
                    LauncherCommon.setRequestCommonInfo(request,par);
                    LauncherCommon.setClientInfo(par,request);

                }
            }

            par.setTargetNames(new String[]{srvName});
            if(null != bridge) {
                SessionManager sm = (SessionManager) ((XMLObject) bridge).getObjectById("SessionManager");
                par.setSessionManager(sm);
            }
            if (!par.getRequestHeaders().contains(par.KEY_REQUESTID)) {

                par.setRequestId(ISPUtil.getRequestId("WS",bridge.getInstanceId(),"",srvName));
            } else {
                par.setRequestId((String) par.getRequestHeaders().get(par.KEY_REQUESTID));
            }
            if(null != env) {
                par.setEnv(env.getEnv());
            }
            if (StringUtils.isNotBlank(parNames)) {
                String[] ns = parNames.split(",");
                HashMap map = new LinkedHashMap();
                AtomicLong dataSize = new AtomicLong(0);
                if (ns.length == 1 && !receive.getClass().isArray()) {
                    //map.put(ns[0], POJOUtil.convertPojo2Map( receive));
                    map.put(ns[0], POJOUtil.convertPOJO2USDL(receive, dataSize));
                } else {
                    for (int i = 0; i < ns.length; i++) {
                        //map.put(ns[i], POJOUtil.convertPojo2Map(((Object[]) receive)[i]));
                        map.put(ns[i], POJOUtil.convertPOJO2USDL(((Object[]) receive)[i], dataSize));
                    }
                }
                if(log.isDebugEnabled()){
                    log.debug("---------------"+srvName+" input parameters---------\n"+map);
                }
                par.setRequestData(map);
                addRequestDataSize(par, dataSize.longValue());
            }
            long t = System.currentTimeMillis();
            Object ret = bridge.evaluate(par);
            log.debug(srvName+" invoke cost:" + (System.currentTimeMillis() - t));
            if (null != ret) {
                if (ret instanceof ResultCheck) {
                    if (((ResultCheck) ret).isSuccess()) {
                        ret = ((ResultCheck) ret).getRet();
                        if(log.isDebugEnabled()){
                            log.debug("---------------"+srvName+" return ---------\n"+ret);
                        }
                        Type type = POJOUtil.getTypeByString(retClass);
                        AtomicLong dataSize = new AtomicLong(0);
                        Object r = POJOUtil.convertUSDL2POJO(ret, type, dataSize);
                        addResponseDataSize(par, dataSize.longValue());
                        return r;

                    } else {
                        if (((ResultCheck) ret).getRet() instanceof Exception) {
                            addResponseDataSize(par, 0);
                            throw  (Exception)((ResultCheck) ret).getRet();
                        }
                    }
                }
            }
            return null;
        } catch (Throwable e) {
            //log.error("CXFWebService [" + srvName + "] invoke error " + ExceptionUtil.getRootString(e), e);
            //throw new Fault(e);
            throw  getSoapFault(ExceptionUtil.getRootCase(e));//(Exception) ExceptionUtil.getRootCase(e);
        } finally {
            //System.out.println("T:"+(System.currentTimeMillis()-l));
        }

    }
    static SoapFault getSoapFault(Throwable e){
        ErrMsg em = ExceptionUtil.getMsg(e);
        //return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(em.getMsg()).type("application/json;charset=UTF-8").build();

        //return Response.status(Response.Status.BAD_REQUEST).entity(em.getMsg()).type("text/plain").build();
        SoapFault ret = new SoapFault(em.getMsg(),new QName(em.getCode()));
        return ret;
    }

    static void addRequestDataSize(RequestParameters pars, long requestDataSize) {
        try {
            if (null != statHandler) {
                if (null != pars.getTargetNames()) {
                    for (String n : pars.getTargetNames()) {
                        HashMap input = new HashMap();
                        input.put("op", "addRequestDataSize");
                        input.put("srvId", n);
                        pars.setRequestDataSize(requestDataSize);
                        statHandler.doSomeThing(null, pars, input, null, null);
                    }
                }

            }
        } catch (Exception e) {
            log.error("add request Data size error", e);
        }
    }

    static void addResponseDataSize(RequestParameters pars, long requestDataSize) {
        try {
            if (null != statHandler) {
                for (String n : pars.getTargetNames()) {
                    HashMap input = new HashMap();
                    input.put("op", "addResponseDataSize");
                    input.put("srvId", n);
                    pars.setResponseDataSize(requestDataSize);
                    statHandler.doSomeThing(null, pars, input, null, null);
                }
            }
        } catch (Exception e) {
            log.error("add request Data size error", e);
        }
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        if (null != input) {
            String op = (String) input.get("op");
            Object data = input;//input.get("data");
            if (StringUtils.isNotBlank(op)) {
                if ("addService".equals(op) && null != data && Map.class.isAssignableFrom(data.getClass())) {
                    Map desc = (Map) data;
                    if (!cache.contains((String) desc.get("name"))) {
                        if(((String) ((String) desc.get("name"))).startsWith("(")) return null;// if tenant service do not create cxf api
                        CXFUtil wrap = new CXFUtil();
                        if (hostport == null && null != env) {
                            String s = getXML().getProperties().getProperty("config");
                            if (StringUtils.isNotBlank(s)) {
                                Map map = StringUtils.convert2MapJSONObject(s);
                                if (null != map) {
                                    hostport = (String) env.getValueFromExpress((String) map.get("host"),this);
                                    keypath = (String) env.getValueFromExpress((String) map.get("key"),this);
                                    pwd = (String) env.getValueFromExpress((String) map.get("pwd"),this);
                                    storepwd = (String) env.getValueFromExpress((String) map.get("storepwd"),this);
                                    isThrowException = StringUtils.isTrue((String) config.get("isthrowexception"));
                                }
                            }
                        }
                        Class serClass = Desc.getServiceWrapClassByDesc((String) desc.get("name"), Desc.getInvokeDescStructure(desc), compilepath, this.getClass().getName(), "invoke", isThrowException);
                        if (null != serClass && StringUtils.isNotBlank(hostport)) {
                            wrap.addService(hostport, (String) desc.get("name"), serClass, serClass,keypath,pwd,storepwd);
                            log.debug("CXF:" + desc.get("name"));
                            cache.add((String) desc.get("name"));
                        } else {
                            log.warn("get desc service error.");
                        }
                    }
                } else if ("deleteService".equals(op)) {
                    if (data instanceof String) {
                        Desc.deleteServiceWrapClass((String) data,getObjectById((String)data), compilepath);
                        CXFUtil wrap = new CXFUtil();
                        wrap.deleteService((String) data);
                        cache.remove((String) data);
                    }
                }
            }
        }else {

            //init deploy to cxf from xmlobject by config path
            bridge = (IBridge) getObjectById("bridge");
            deployXMLObject2Cxf(input, config);
        }
        return null;
    }

    void deployXMLObject2Cxf(Map input, Map config) throws Exception {
        //init get ServiceInfo;
        if (null != config && null == input) {
            CXFUtil wrap = new CXFUtil();
            hostport = (String) config.get("host");
            keypath = (String)config.get("key");
            pwd = (String)config.get("pwd");
            storepwd = (String)  config.get("storepwd");
            String develop = (String) config.get("isdevelop");
            isThrowException = StringUtils.isTrue((String) config.get("isthrowexception"));
            if (StringUtils.isNotBlank(develop)) {
                isdevelop = StringUtils.isTrue(develop);
            }
            if (StringUtils.isBlank(compilepath)) {
                compilepath = (String) config.get("compilepath");
            }

            //List<String> list = (List) config.get("servicelist"); // move out to GeneratorWSClass
            //List<XMLDoObject> sers = getDeployServices(list);  // // move out to GeneratorWSClass
            if ( StringUtils.isNotBlank(hostport)) {
                Map<String,Class> cls = (Map)config.get("classList");
                Iterator its = cls.keySet().iterator();
                while(its.hasNext()){
                    String srvid = (String)its.next();
                    Class serClass = cls.get(srvid);
                    if(null != srvid && null != serClass) {
                        wrap.addService(hostport, srvid, serClass, serClass, keypath, pwd, storepwd);
                        log.debug("CXF:" + srvid);
                        cache.add(srvid);
                    }
                }
                /*  // move out to GeneratorWSClass
                if (null != sers && sers.size()>0) {
                    for (XMLDoObject ser : sers) {
                        if (!cache.contains(ser.getXML().getId())) {
                            String pack = ser.getXML().getProperties().getProperty("package");
                            if(ser.getXML().getId().startsWith("(")) continue;// if tenant server , do not create cxf api
                            if (StringUtils.isNotBlank(pack) && pack.lastIndexOf(".")>0) {
                                pack = pack.substring(0, pack.lastIndexOf("."));
                            }
                            Class serClass = null;
                            if (isdevelop) {
                                serClass = Desc.getServiceWrapClassByDesc(ser.getXML().getId(), ser.getInvokeDescStructure(), compilepath, this.getClass().getName(), "invoke", isThrowException);
                            } else {
                                if (null != pack) {
                                    try {
                                        serClass = Class.forName(pack + "." + "proxy_" + ser.getXML().getId());
                                    } catch (Exception e) {
                                        log.error("not find class [" + pack + "." + "proxy_" + ser.getXML().getId() + "]", e);
                                    }
                                } else {
                                    try {
                                        serClass = Class.forName("proxy_" + ser.getXML().getId());
                                    } catch (Exception e) {
                                        log.error("not find class [" + "proxy_" + ser.getXML().getId() + "]", e);
                                    }
                                }
                            }
                            //deploy cxf web service
                            if (null != serClass) {
                                wrap.addService(hostport, ser.getXML().getId(), serClass, serClass,keypath,pwd,storepwd);
                                log.debug("CXF:" + ser.getXML().getId());
                                cache.add(ser.getXML().getId());
                            } else {
                                log.error("can not deploy web service "+ser.getXML().getId());
                            }
                        }
                    }

                }
                */

            }
        }
    }

    //filter loaded xmldoobject by path
    List<XMLDoObject> getDeployServices(List<String> name) {
        Map<String, XMLObject> all = getXMLObjectContainer();
        Iterator<String> its = all.keySet().iterator();
        List<XMLDoObject> ret = new ArrayList();
        while (its.hasNext()) {
            String m = its.next();
            try {
                if (all.get(m) instanceof XMLDoObject) {
                    Map stru = ((XMLDoObject) all.get(m)).getInvokeDescStructure();
                    if (null != stru && StringUtils.isNotBlank(stru.get("package"))) {
                        String path = (String) stru.get("package");
                        if(null != name && name.size()>0) {
                            for (String n : name) {
                                if (path.startsWith(n))
                                    ret.add((XMLDoObject) all.get(m));
                            }
                        }else{
                            ret.add((XMLDoObject) all.get(m));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ret;
    }


    @Override
    public ResultCheck checkReturn(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return new ResultCheck(true, ret);
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config,Object ret,Exception e) throws Exception {
        return false;
    }


}
