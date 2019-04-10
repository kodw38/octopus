package com.octopus.isp.bridge.launchers.impl.pageframe.channel;

import com.octopus.isp.actions.ISPDictionary;
import com.octopus.isp.bridge.launchers.impl.pageframe.ISessionDataGet;
import com.octopus.isp.bridge.launchers.impl.pageframe.SessionManager;
import com.octopus.isp.bridge.launchers.impl.pageframe.util.HttpUtils;
import com.octopus.isp.ds.RequestParameters;
import com.octopus.isp.ds.Session;
import com.octopus.tools.dataclient.v2.DataClient2;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cls.POJOUtil;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * User: wfgao_000
 * Date: 15-8-25
 * Time: 下午2:00
 */
public class SessionData extends XMLDoObject {
    transient static Log log = LogFactory.getLog(SessionData.class);
    Map<String,List<Map<String,Object>>> map = new HashMap<String, List<Map<String,Object>>>();
    Map<String,List<Properties>> sessionDataByXmlObject = new HashMap<String, List<Properties>>();
    public static String LoginUserNameKey,LoginUserPwdKey,ApiUserNameKey,ApiUserPwdKey,ApiSessionTimeoutKey;
    public SessionData(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        LoginUserNameKey = xml.getFirstCurChildText("property","name","LoginUserNameKey");
        LoginUserPwdKey = xml.getFirstCurChildText("property","name","LoginUserPwdKey");
        ApiUserNameKey = xml.getFirstCurChildText("property","name","ApiUserNameKey");
        ApiUserPwdKey = xml.getFirstCurChildText("property","name","ApiUserPwdKey");
        ApiSessionTimeoutKey = xml.getFirstCurChildText("property","name","ApiSessionTimeoutKey");

        XMLMakeup[] sessionconfigs = xml.getRoot().getChild("sessions");
        if(null != sessionconfigs && sessionconfigs.length>0){
            XMLMakeup[] ds = sessionconfigs[0].getChild("datas");
            if(null != ds && ds.length>0){
                for(XMLMakeup d:ds){
                    List<XMLMakeup> ls = d.getChildren();
                    if(null != ls && ls.size()>0){
                        for(XMLMakeup l:ls){
                            if(StringUtils.isNotBlank(l.getProperties().getProperty("KEY_CODE")) && StringUtils.isNotBlank(l.getProperties().getProperty("SERVICE_NAME")) && StringUtils.isNotBlank(l.getProperties().getProperty("ACTION_NAME"))){
                                if(null ==sessionDataByXmlObject.get(l.getProperties().getProperty("SERVICE_NAME")))
                                    sessionDataByXmlObject.put(l.getProperties().getProperty("SERVICE_NAME"),new ArrayList<Properties>());
                                sessionDataByXmlObject.get(l.getProperties().getProperty("SERVICE_NAME")).add(l.getProperties());
                            }
                        }
                    }
                }
            }
        }
        XMLMakeup x = (XMLMakeup)ArrayUtils.getFirst(xml.getChild("dataquery"));
        if(null != x){
            DataClient2 dc = (DataClient2)getObjectById("DataClient");
            List<Map<String,Object>> result = (List<Map<String,Object>>)dc.doSomeThing(null,null,StringUtils.convert2MapJSONObject(x.getProperties().getProperty("input")),null,null);
            if(null != result){
                for(Map<String,Object> m:result){
                    if(StringUtils.isNotBlank(m.get("KEY_CODE"))){
                        if(!map.containsKey(m.get("SERVICE_NAME"))){
                            String[] svs = ((String)m.get("SERVICE_NAME")).split(",");
                            for(String sv:svs){
                                if(!map.containsKey(sv))
                                    map.put(sv,new ArrayList<Map<String, Object>>()) ;
                                map.get(sv).add(m);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public Object doSomeThing(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        RequestParameters par = (RequestParameters)env;
        ISPDictionary dictionary = (ISPDictionary)getObjectById("Dictionary");
        String[] as = par.getTargetNames();
        if(null != as){
            String sessionid=((RequestParameters)env).getRequestCookies().get("JSESSIONID");
            String pageframeId=((RequestParameters)env).getRequestCookies().get("PAGEFRAMEID");
            if(log.isDebugEnabled()){
                log.debug("SessionData input:"+input);
            }
            if(null == sessionid && null != pageframeId){
                if(log.isDebugEnabled()){
                    log.debug("JSESSIONID is null , used PAGEFRAMEID "+pageframeId);
                }
                sessionid=pageframeId;
            }
            if(input.containsKey("SessionId")){
                sessionid=(String)input.get("SessionId");
            }

            SessionManager sm = (SessionManager)getObjectById("SessionManager");
            if(null != sm){
                Boolean t = (Boolean)env.get("${return}");
                if(log.isDebugEnabled()){
                    log.debug("login result:"+t);
                }
                for(String a:as){
                    String cl = dictionary.getOpType(a);
                    if(null ==cl){
                        XMLObject to = getObjectById(a);
                        if(null != to){
                            cl = to.getXML().getProperties().getProperty("opType");
                        }
                    }
                    if(StringUtils.isNotBlank(cl) && cl.equals(ISPDictionary.SERVICE_CLASS_LOGIN)){
                        ResultCheck b = (ResultCheck)env.getResult();
                        if((null != b && null != b.getRet() && b.isSuccess())||(null != t && t)){
                            Object o = par.getRequestData();
                            if(o instanceof Map){
                                Map in = (Map)o;
                                String userName = (String)in.get(LoginUserNameKey);
                                String userPwd = (String)in.get(LoginUserPwdKey);
                                Object expire1 = in.get("expire");
                                String expire=null;
                                if(null !=expire1 && expire1 instanceof String) {
                                    expire = (String)expire1;
                                }else if(null != expire1 && expire1 instanceof Integer){
                                    expire=String.valueOf((Integer)expire1);
                                }else{
                                    expire=(String)in.get(ApiSessionTimeoutKey);
                                }
                                if(StringUtils.isBlank(userName) && StringUtils.isBlank(userPwd)){
                                    userName=(String)in.get(ApiUserNameKey);
                                    userPwd=(String)in.get(ApiUserPwdKey);
                                    expire=(String)in.get(ApiSessionTimeoutKey);
                                }
                                if(StringUtils.isNotBlank(userName) && StringUtils.isNotBlank(userPwd)){
                                    Session session = sm.createSession(userName,userPwd,expire,sessionid);
                                    HashMap dd  = new HashMap();
                                    dd.put(LoginUserNameKey,userName) ;
                                    dd.put(LoginUserPwdKey,userPwd);

                                    sm.addSessionAttributes(session.getSessionId(),dd);
                                    par.setSession(session);
                                    if(log.isDebugEnabled()){
                                        log.debug("create session :"+session);
                                    }
                                    HttpUtils hu = (HttpUtils)getObjectById("HttpUtils");
                                    hu.setSession2Cookie(par,session.getSessionId());
                                }
                            }
                        }
                        break;

                    }
                    if(StringUtils.isNotBlank(cl) && cl.equals(ISPDictionary.SERVICE_CLASS_LOGOUT)){

                        sm.deleteSession(par.getSession().getSessionId(),sessionid);
                        return env.getResult();

                    }
                }
                if(log.isDebugEnabled()){
                    log.debug("sessionId:"+sessionid);
                }
                //get user info and set session, session will been set each request parameters  // remove && StringUtils.isNotBlank(sessionid)
                if((null != par.getResult() || (null != t && t)) && null != par.getSession() ){
                    Map<String,String> d = null;
                    ResultCheck rc = (ResultCheck)env.getResult();
                    //Map authSrvs = null;
                    Object ret = null;
                    if(null != rc) {
                        ret = rc.getRet();
                    }
                    for(String a:as){
                        if(sessionDataByXmlObject.containsKey(a)){
                            List<Properties> lp = (List)sessionDataByXmlObject.get(a);
                            if(null == d) d=new HashMap<String, String>();
                            for(Properties l:lp){
                                XMLDoObject ao = (XMLDoObject)getObjectById(l.getProperty("ACTION_NAME"));
                                if(null != ao){
                                    Map in = new HashMap();
                                    in.put("login_name",LoginUserNameKey);
                                    in.put("login_pwd",LoginUserPwdKey);
                                    if(null != ret ) {
                                        if(ret instanceof Boolean) {
                                            in.put("login_result", ret);
                                        }else{
                                            in.put("login_result",Boolean.TRUE);
                                        }
                                    }else if(null != t){
                                        in.put("login_result",t);
                                    }

                                    in.put("session",sm.getSessionById(par.getSession().getSessionId(),sessionid));
                                    //get uer info
                                    Object e = ao.doSomeThing(null,env,in, null,null);
                                    if(log.isDebugEnabled()){
                                        log.debug("get user info :"+e+"  by :"+in);
                                    }
                                    if(null != e){
                                        if(e instanceof String) {
                                            Map m = StringUtils.convert2MapJSONObject((String)e);
                                            if (null != m) {
                                                d.putAll(m);
                                            } else {
                                                d.put(l.getProperty("KEY_CODE"), (String)e);
                                            }
                                        }
                                    }else if(e instanceof Map){
                                        d.putAll((Map)e);
                                    }
                                    //add auth services to redis
                                    /*in = new HashMap();
                                    in.put("login_name",LoginUserNameKey);
                                    in.put("login_pwd",LoginUserPwdKey);
                                    in.put("op","getAuthSrv");
                                    in.put("session",sm.getSessionById(par.getSession().getSessionId(),((RequestParameters)env).getRequestCookies().get("JSESSIONID")));
                                    //get uer info
                                    authSrvs = (Map)ao.doSomeThing(null,env,in, null,null);*/

                                }
                            }
                        }
                        if(map.containsKey(a) && null != ret){
                            List<Map<String,Object>> cs = map.get(a);
                            if(d==null)d =new HashMap<String, String>();
                            for(Map<String,Object> c:cs){
                                if(StringUtils.isNotBlank(c.get("GET_CLAZZ"))){
                                       ISessionDataGet get = (ISessionDataGet)Class.forName((String)c.get("GET_CLAZZ")).newInstance();
                                    String data = get.getData(ret);
                                    d.put((String)c.get("KEY_CODE"),data);
                                }else if(StringUtils.isNotBlank(c.get("DATA_PATH"))){
                                    Object o = ObjectUtils.getValueByPath(ret,(String)c.get("DATA_PATH"));
                                    if(null != o){
                                        d.put((String)c.get("KEY_CODE"),o.toString());
                                    }
                                }else{
                                    if(ret instanceof String){
                                        d.put((String)c.get("KEY_CODE"), (String)ret);
                                    }else if(ret instanceof Map){
                                        d.put((String)c.get("KEY_CODE"), ObjectUtils.convertMap2String((Map)ret));
                                    }else if(ret instanceof Collection){
                                        d.put((String)c.get("KEY_CODE"),net.sf.json.JSONArray.fromObject(d).toString());
                                    }else if(!POJOUtil.isSimpleType(ret.getClass().getName())){
                                        d.put((String)c.get("KEY_CODE"), ObjectUtils.convertMap2String(POJOUtil.convertPojo2Map(ret,null)));
                                    }

                                }
                            }
                        }
                    }
                    if(null!=d && d.size()>0) {
                        sm.addSessionAttributes(par.getSession().getSessionId(), d);
                        par.getSession().putAll(d);
                        if(log.isDebugEnabled()){
                            log.debug("append data to session:"+d);
                        }
                    }
                    //add authSrvs to redis
/*
                    if(null != authSrvs){
                        sm.addAuthServices(par.getSession().getSessionId(),authSrvs);
                    }
*/
                }

            }
        }
        return par.getResult();
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
        return new ResultCheck(true,ret);
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
