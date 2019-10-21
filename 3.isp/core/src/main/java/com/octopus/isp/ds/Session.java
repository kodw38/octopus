package com.octopus.isp.ds;

import com.octopus.isp.bridge.launchers.impl.pageframe.channel.SessionData;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * User: Administrator
 * Date: 14-9-3
 * Time: 上午10:12
 */
public class Session extends HashMap<String,Object>{

    public static String KEY_USER="user";
    public static String KEY_SESSION_ID="KEY_SESSION_ID";
    public static String KEY_SESSION_CREATE_DATE="KEY_SESSION_CREATE_DATE";
    public static String KEY_SESSION_LAST_ACCESS_DATE="KEY_SESSION_LAST_ACCESS_DATE";
    public static String KEY_SESSION_MAX_INACTIVE_INTERVAL="KEY_SESSION_MAX_INACTIVE_INTERVAL";

    public String getSessionId(){
        String sessionid= (String)get(KEY_SESSION_ID);
        if(null == sessionid){
            sessionid=(String)get("SESSION_ID");
        }
        return sessionid;
    }
    public void setSessionId(String id){
        put(KEY_SESSION_ID,id);
    }
    public void setCreateDate(long time){
        put(KEY_SESSION_CREATE_DATE,time);
    }
    public Date getCreateDate(){
        Long l = (Long)get(KEY_SESSION_CREATE_DATE);
        if(null != l){
            return new Date(l);
        }
        return null;
    }
    public void setLastAccessDate(long time){
        put(KEY_SESSION_LAST_ACCESS_DATE,time);
    }
    public Date getLastAccessDate(){
        Long l = (Long)get(KEY_SESSION_LAST_ACCESS_DATE);
        if(null != l){
            return new Date(l);
        }
        return null;
    }
    public void setMaxInactiveInterval(int interval){
        put(KEY_SESSION_MAX_INACTIVE_INTERVAL,interval);
    }
    public int getMaxInactiveInterval(){
        return (Integer)get(KEY_SESSION_MAX_INACTIVE_INTERVAL);
    }
    public Map getUser(){
        Map map = (Map)get(KEY_USER);
        if(null != map){
            map = new HashMap();
            put(KEY_USER,map);
        }
        return map;
    }

    public boolean isAdmin(){
        if("ADMIN".equals(get("USER_TYPE"))){
            return true;
        }else{
            return false;
        }
    }
    public String getUserName(){
        if(null != SessionData.loginFields){
            for(Map m:SessionData.loginFields){
                if(this.containsKey(m.get("name"))){
                    return (String)get(m.get("name"));
                }
            }
        }
        return null;
    }

}
