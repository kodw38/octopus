package com.octopus.isp.ds;

import com.octopus.isp.bridge.launchers.impl.pageframe.SessionManager;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.flow.FlowParameters;

import java.io.IOException;
import java.util.*;

/**
 * User: Administrator
 * Date: 14-9-29
 * Time: 上午9:51
 */
public class RequestParameters extends FlowParameters{

    public static String KEY_REQUESTID = "RequestId";
    Env env;           //系统环境信息
    Context context=null;   //符合该用户、请求的系统上下文信息
    Session session=null;   //会话数据
    ClientInfo clientInfo = new ClientInfo();  //请求客户端信息
    Hashtable<String,String> requestHeaders =new Hashtable(); //请求数据头
    Hashtable<String,String> requestCookies =new Hashtable(); //请求数据头
    String requestRUL=null;  //请求全路径
    String requestURI=null;   //请求路径
    String queryString=null;  //查询String
    HashMap queryStringMap=null;  //查询String 的map
    Object requestData=null;
    Date requestDate=null;    //请求开始时间
    String requestId=null;    //请求唯一编号
    String[] targetNames;  //请求入口cell的名称，可以多个cell并行调用
    HashMap requestProperties = new HashMap(); //请求属性
    String instanceid;

    public String getInstanceid() {
        return instanceid;
    }

    public void setInstanceid(String instanceid) {
        this.instanceid = instanceid;
    }

    public RequestParameters(){
        addGlobalParameter("${clientInfo}",clientInfo);
        addGlobalParameter("${requestHeaders}",requestHeaders);
        addGlobalParameter("${requestCookies}",requestCookies);
        addGlobalParameter("${requestProperties}",requestProperties);
    }
    public HashMap getRequestProperties() {
        return requestProperties;
    }

    public void setRequestProperties(HashMap requestProperties) {
        addParameter("${requestProperties}",requestProperties);
    }

    public boolean isThisInstance(String usedInstancs){
        if(StringUtils.isBlank(usedInstancs)|| ArrayUtils.isLikeStringArray(usedInstancs.split(","),getInstanceid())) {
            String tarid = getHeader("targetinsid");
            if (StringUtils.isBlank(tarid) || StringUtils.isBlank(usedInstancs) || ArrayUtils.isLikeStringArray(usedInstancs.split(","),tarid)) {
                return true;
            }
        }
        return false;
    }
    public void addRequestProperties(String key,Object value){
        requestProperties.put(key,value);
    }
    public String getRequestURI() {
        return (String)getParameter("${requestURI}");
    }

    public void setRequestURI(String requestURI) {
        addGlobalParameter("${requestURI}",requestURI);
    }

    public void setProtocol(String protocol){
        addGlobalParameter("${protocol}",protocol);
    }
    public String getProtocol(){
        String o = (String)getGlobalParameter("${protocol}");
        if(o==null){
            o = "json";
        }
        return o;
    }
    public void setRequestResourceName(String name){
        addParameter("${requestResourceName}",name);
    }

    public String getRequestResourceName(){
        return (String)getParameter("${requestResourceName}");
    }

    public Hashtable getRequestHeaders() {
        return (Hashtable)getParameter("${requestHeaders}");
    }

    public void setRequestHeaders(Hashtable requestHeaders) {
        addGlobalParameter("${requestHeaders}",requestHeaders);
    }

    public Hashtable<String,String> getRequestCookies() {
        return (Hashtable)getParameter("${requestHeaders}");
    }

    public void setRequestCookies(Hashtable requestHeaders) {
        addGlobalParameter("${requestCookies}",requestHeaders);
    }

    public HashMap getQueryStringMap() {
        return (HashMap)getParameter("${queryStringMap}");
    }

    public void setQueryStringMap(HashMap queryStringMap) {
        addGlobalParameter("${queryStringMap}",queryStringMap);
    }


    public Object getRequestData() {
        return getInputParameter();
    }

    public void setRequestData(Object requestData) {
        setInputParameter(requestData);
    }

    public Date getRequestDate() {
        return (Date)getParameter("${requestDate}");
    }

    public void setRequestDate(Date requestDate) {
        addGlobalParameter("${requestDate}",requestDate);
    }

    public String getRequestId() {
        return (String)getParameter("${requestId}");
    }

    public void setRequestId(String requestId) {
        addGlobalParameter("${requestId}",requestId);
    }

    public void setRequestDataSize(long size){
        addGlobalParameter("${requestDataSize}",size);
    }
    public long getRequestDataSize(){
        Object o = getParameter("${requestDataSize}");
        if(null != o){
            return (Long)o;
        }else{
            return 0;
        }
    }
    public void setResponseDataSize(long size){
        addParameter("${responseDataSize}",size);
    }
    public long getResponseDataSize(){
        Object o = getParameter("${responseDataSize}");
        if(null != o){
            return (Long)o;
        }else{
            return 0;
        }
    }


    public void setConstant(Map constant){
        addGlobalParameter("${constant}",constant);
    }
    public void setSessionManager(SessionManager sessionManager){
        addStaticParameter("${sessionManager}",sessionManager);
    }
    public static SessionManager getSessionManager(){
        return (SessionManager)getStaticParameter("${sessionManager}");
    }
    public Session getSession() {
        return (Session)getParameterWithoutThreadName("${session}");
        //return (Session)getGlobalParameter("${session}");
    }

    public void setSession(Session session) {
        addParameter("${session}", session);
        //addGlobalParameter("${session}", session);
    }

    public String getRequestURL() {
        return (String)getParameter("${requestURL}");
    }

    public void setRequestURL(String requestURL) {
        addGlobalParameter("${requestURL}",requestURL);
    }

    public String getQueryString() {
        return (String)getParameter("${queryString}");
    }

    public void setQueryString(String queryString) {
        addGlobalParameter("${queryString}",queryString);
    }

    public void addClientInfo(String key,String value) throws IOException {
        clientInfo.put(key, value);
    }

    public ClientInfo getClientInfo() {
        return (ClientInfo)getParameter("${clientInfo}");
    }

    public void addHeader(String key,String value) throws IOException {
        requestHeaders.put(key, value);
    }
    public void addAllHeader(Map map) throws IOException {
        requestHeaders.putAll(map);
    }
    public String getHeader(String key){
        return requestHeaders.get(key);
    }


}
