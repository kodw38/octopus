package com.octopus.octopus.actions;

import com.octopus.isp.ds.RequestParameters;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.exception.ISPException;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

/**
 * User: wfgao_000
 * Date: 15-9-2
 * Time: 上午9:41
 */
public class UserLogin extends XMLDoObject {
    transient static Log log = LogFactory.getLog(UserLogin.class);
    Properties users;
    XMLDoObject userauth;
    String configuserauth=null;
    public UserLogin(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        Properties p = new Properties();
        p.load(this.getClass().getClassLoader().getResourceAsStream("user.properties"));
        users=p;
    }


    XMLDoObject getConfigUserAuth(){
        if(StringUtils.isNotBlank(configuserauth)){
            return  (XMLDoObject) getObjectById(configuserauth);
        }
        return null;
    }

    @Override
    public Object doSomeThing(String xmlid,XMLParameter env, Map input, Map output,Map config) throws Exception {
        log.debug("do userlogin....");

        if(null != input && input.containsKey("UserName") && input.containsKey("UserPwd") && (null == input.get("op") || "exist".equals(input.get("op")))){
            String key = (String)input.get("UserName")+"_"+(String)input.get("UserPwd");
            if(users.containsKey(key)) {
                log.debug("login true");
                String info = (String)users.get(key);
                if(StringUtils.isNotBlank(info)) {
                    /*String[] ss = StringUtils.split(info,"|");
                    if(null != ss) {
                        for(String s:ss) {
                            String[] is = StringUtils.split(s,":");
                            if(null != is && is.length==2) {
                                if(null != is[0] && "tenant".equals(is[0]) && StringUtils.isNotBlank(is[1])) {
                                    String tenant = is[1];
                                    if (env instanceof RequestParameters) {
                                        Object o = ((RequestParameters) env).getRequestData();
                                        if (o != null && o instanceof Map) {
                                            ((Map) o).put("tenant", tenant);
                                        }
                                    }
                                }
                            }
                        }
                    }*/
                    return true;
                }

            }

            if(null != userauth){
                //if default auth is not null , check user exist, fist use this
                boolean is  = checkUser(input,userauth);
                if(is){
                    return is;
                }
            }
            if(null == configuserauth){
                log.info("not set configuserauth");
            }
            if(null != configuserauth){
                boolean is=  checkUser(input,getConfigUserAuth());
                if(is) return is;
            }
            if(userauth==null && null == configuserauth){
                throw new Exception("not find service [userauth] , so can not login, please check app file Login");
            }
            throw new ISPException("401","login error,please check account,UserName or Password.");

        }else if(null != input && (null !=input.get("login_result")|| "getUserInfo".equals(input.get("op")))){
            if((input.get("login_result") instanceof Boolean && (Boolean)input.get("login_result")) || "getUserInfo".equals(input.get("op")) ){
                String userName;
                String pwd;
                if(null != input.get("session")) {
                    userName = (String) ((Map) input.get("session")).get(input.get("login_name"));
                    pwd = (String) ((Map) input.get("session")).get(input.get("login_pwd"));
                }else{
                    userName=(String)input.get("UserName");
                    pwd=(String)input.get("UserPwd");
                }

                String s = users.getProperty(userName+"_"+pwd);
                if(StringUtils.isNotBlank(s)){
                    Map map = new HashMap();
                    String[] phares = s.split("\\|");
                    if(null != phares && phares.length>0){
                       for(String p:phares){
                           String[] pv = p.split("\\:");
                           if(null != pv && pv.length==2){
                               if(null != pv[0] && null != pv[1] && !pv[0].equalsIgnoreCase("services")) {
                                   map.put(pv[0], pv[1]);
                               }
                               /*if(pv[0].equals("usermodel")){
                                  return pv[1];
                               }*/
                           }
                       }
                    }
                    if(log.isDebugEnabled()) {
                        log.debug("get auth info:" + map);
                    }
                    return ObjectUtils.convertMap2String(map);
                }else if(null != userauth || null != configuserauth){
                    HashMap map = new HashMap();
                    map.putAll(input);
                    map.put("op","getUserInfo");
                    map.put("UserName",userName);
                    map.put("UserPwd",pwd);
                    RequestParameters par = new RequestParameters();
                    par.setRequestHeaders((Hashtable) env.get("${requestHeaders}"));
                    map.put("OUT_SYSTEM_ID",par.getRequestHeaders().get("sysLoginID"));
                    map.put("SessionId",par.getRequestHeaders().get("sessionID"));
                    par.addParameter("^${input}",map);
                    par.addParameter("${session}",env.get("${session}"));
                    if(null != getConfigUserAuth())
                        getConfigUserAuth().doThing(par,null);
                    else
                        userauth.doThing(par,null);
                    Object r = par.getResult();
                    if(null != r && null != ((ResultCheck)r).getRet()){
                        Object ret=null;
                        if(((ResultCheck)r).getRet() instanceof Map){
                            ret = ObjectUtils.convertMap2String((Map)((ResultCheck)r).getRet());
                        }else {
                             ret = ((ResultCheck) r).getRet().toString();
                        }
                        if(log.isDebugEnabled()) {
                            log.debug("get auth info:" + ret);
                        }
                        return ret;
                    }
                }
            }
            return null;
        }else if(null != input && "getAuthSrv".equalsIgnoreCase((String)input.get("op"))){
            //log.error("------------------load user service auth ---------------------");
            String userName= (String)input.get("login_name");
            String pwd= (String)input.get("login_pwd");
            String s = users.getProperty(userName+"_"+pwd);
            if(StringUtils.isNotBlank(s)){
                Map map = new HashMap();
                String[] phares = s.split("\\|");
                if(null != phares && phares.length>0){
                    for(String p:phares){
                        String[] pv = p.split("\\:");
                        if(null != pv && pv.length==2){
                            if(null != pv[0] && null != pv[1] && pv[0].equals("services")) {
                                if(pv[1].startsWith("{")){
                                    map=StringUtils.convert2MapJSONObject(pv[1]);
                                }
                            }
                        }
                    }
                }
                return map;
            }
            if(null != configuserauth){
                Object o = getSrvs(getConfigUserAuth(),input,userName);
                if(null != o){
                    return o;
                }
            }
            if(null != userauth){
                return getSrvs(userauth,input,userName);
            }
        }
        return null;
    }

    Object getSrvs(XMLDoObject userauth, Map input,String userName) throws Exception{
        //log.debug("----get user's service--- from db:"+userName);
        HashMap map = new HashMap();
        map.putAll(input);
        map.put("op","getAuthSrv");
        map.put("UserName",userName);
        XMLParameter par = new XMLParameter();
        par.addParameter("^${input}",map);
        userauth.doThing(par,null);
        Object r = par.getResult();
        if(null != r && null != ((ResultCheck)r).getRet()){
            Object ret=null;
            if(((ResultCheck)r).getRet() instanceof Map){
                ret = (Map)((ResultCheck)r).getRet();
            }else {
                ret = ((ResultCheck) r).getRet().toString();

            }
            //log.error("---result:"+ret);
            return ret;
        }
        return null;
    }

    boolean checkUser(Map input,XMLDoObject userauth)throws Exception{
        HashMap map = new HashMap();
        map.putAll(input);
        map.put("op","exist");
        XMLParameter par = new XMLParameter();
        par.addParameter("^${input}",map);
        userauth.doThing(par,null);
        Object r = par.getResult();
        if(null != r && (((r instanceof ResultCheck && null != ((ResultCheck)r).getRet() && StringUtils.isTrue(((ResultCheck)r).getRet().toString())))
                || StringUtils.isTrue(r.toString()))){
            return true;
        }else {
            return false;
        }
    }
    @Override
    public void doInitial() throws Exception {
        try {
            String cfg = getXML().getProperties().getProperty("config");
            if (StringUtils.isNotBlank(cfg)) {
                Map m = StringUtils.convert2MapJSONObject(cfg);
                if (null != m && m.containsKey("userauth")) {
                    configuserauth = (String) m.get("userauth");
                }
            }
        }catch (Exception e){

        }
    }

    @Override
    public boolean checkInput(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;
    }

    @Override
    public ResultCheck checkReturn(String xmlid,XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return new ResultCheck(true,ret);  //To change body of implemented methods use File | Settings | File Templates.
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
