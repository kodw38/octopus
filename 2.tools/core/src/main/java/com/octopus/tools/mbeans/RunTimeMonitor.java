package com.octopus.tools.mbeans;

import com.octopus.isp.bridge.impl.Bridge;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cls.ClassUtils;
import com.octopus.utils.exception.ExceptionUtil;
import com.octopus.utils.net.NetUtils;
import com.octopus.utils.si.jvm.JVMUtil;
import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.time.DateTimeUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.quartz.SchedulerException;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by admin on 2019/12/7.
 */
public class RunTimeMonitor extends XMLDoObject implements RunTimeMonitorMBean {
    static TreeMap<String,Map<String,AtomicLong>> apiCount = new TreeMap();
    static TreeMap<String,TreeMap<String,AtomicInteger>> apiDurTime = new TreeMap();
    static TreeMap<String,TreeMap> userApi = new TreeMap();
    static TreeMap<String,TreeMap> sqlCount = new TreeMap();
    static LinkedList<Map> runingSql = new LinkedList();
    static TreeMap<String,TreeMap> outapiCount = new TreeMap();
    static LinkedList<Map> runingOutapi = new LinkedList();
    static boolean isStart=false;
    static AtomicInteger count=new AtomicInteger(5);
    static int MAX_RECORDS=1000;
    static QueuedThreadPool workThreadPool;
    static List<String> exclude=new ArrayList();
    static List traceLikes;
    Map config;
    public RunTimeMonitor(XMLMakeup xml, XMLObject parent, Object[] containers) throws Exception {
        super(xml, parent, containers);
        String c =xml.getProperties().getProperty("config");
        if(StringUtils.isNotBlank(c)){
            config = StringUtils.convert2MapJSONObject(c);
        }
        isStart=true;
        if(null != config){
            exclude = (List)config.get("exclude");
        }
        if(null != config){
            Object m = config.get("max");
            if(null != m && m instanceof Integer){
                MAX_RECORDS=(Integer) m;
            }
            Object t = config.get("traceLikes");
            if(null != m && m instanceof List){
                traceLikes=(List) t;
            }
        }
    }

    static boolean isLog(String apiName){
        return (isStart && !exclude.contains(apiName));
    }
    @Override
    public String getCountApiInvoke() {
        return ObjectUtils.convertMap2String(apiCount);
    }
    static void addApiInvokeTime(String apiName){
        if(isLog(apiName)) {
            if (!apiCount.containsKey(apiName)) {
                apiCount.put(apiName, new HashMap());
                apiCount.get(apiName).put("Count", new AtomicLong(0));
            }
            if (apiCount.get(apiName).get("Count").longValue() >= Long.MAX_VALUE) {
                apiCount.get(apiName).get("Count").set(0);
            }
            apiCount.get(apiName).get("Count").addAndGet(1);
            if (apiCount.size() > MAX_RECORDS) {
                apiCount.remove(apiCount.lastKey());
            }
        }
    }

    @Override
    public String getCountApiInvokeDurTime() {
        return ObjectUtils.convertMap2String(apiDurTime);
    }
    static void setApiInvokeDurTime(String apiName){
        if(isLog(apiName)) {
            if (!apiDurTime.containsKey(apiName)) {
                apiDurTime.put(apiName, new TreeMap<String, AtomicInteger>());
            }
            Calendar c= Calendar.getInstance();
            c.setTimeInMillis(System.currentTimeMillis());
            String time = c.get(Calendar.HOUR_OF_DAY)+":"+c.get(Calendar.MINUTE);
            if (!apiDurTime.get(apiName).containsKey(time)) {
                apiDurTime.get(apiName).put(time, new AtomicInteger(0));
            }
            apiDurTime.get(apiName).get(time).getAndAdd(1);
            if (apiDurTime.get(apiName).size() > 10) {
                apiDurTime.get(apiName).remove(apiDurTime.get(apiName).lastKey());
            }
            if (apiDurTime.size() > MAX_RECORDS) {
                apiDurTime.remove(apiDurTime.lastKey());
            }
        }
    }

    @Override
    public String getUserApiInvoke() {
        return ObjectUtils.convertMap2String(userApi);
    }
    static void setUserApiInvoke(String user, String loginTime, Date expireTime, String apiName,Map client){
        if(isLog(apiName)) {
            if (!userApi.containsKey(user)) {
                TreeMap t = new TreeMap();
                t.put("loginTime", loginTime);
                userApi.put(user, t);
            }
            if (null != expireTime && new Date().after(expireTime)) {
                userApi.remove(user);
            } else {
                userApi.get(user).put("expireTime", expireTime);
                if (!userApi.get(user).containsKey(apiName)) {
                    userApi.get(user).put(apiName, new AtomicLong(0));
                }
                ((AtomicLong) userApi.get(user).get(apiName)).addAndGet(1);
                if(null != client && null==userApi.get(user).get("Client"))
                    userApi.get(user).put("Client", ObjectUtils.convertMap2String(client));
                if (userApi.get(user).size() > 100) {
                    userApi.get(user).remove(userApi.get(user).lastKey());
                }
            }
            if (userApi.size() > MAX_RECORDS) {
                userApi.remove(userApi.lastKey());
            }
        }
    }

    static void setSucessApiCost(String name,long cost){
        if(isLog(name)) {
            Map m = (Map) apiCount.get(name);
            if (null != m) {
                Long l = (Long) m.get("Cost");
                if (null==l || l > Long.MAX_VALUE - 100000) {
                    l = new Long(0);
                }
                m.put("Cost", l + cost);
            }
        }
    }
    static void setErrorCount(String name){
        if(isLog(name)) {
            Map m = (Map) apiCount.get(name);
            if (null != m) {
                Long l = (Long) m.get("ErrorCount");
                if (null==l || l > Long.MAX_VALUE - 100000) {
                    l = new Long(0);
                }
                m.put("ErrorCount", l + 1);
            }
        }
    }

    @Override
    public String getSqls() {
        return ObjectUtils.convertMap2String(sqlCount);
    }
    public String getRunningSql(){
        return ObjectUtils.convertList2String(runingSql);
    }
    public static void addSqlStaticInfo(String sql,long cost,Map parameters,XMLParameter env){
        if(null != env) {
            String apiName = null, userCode = null, inspath = null;
            String[] tns = env.getTargetNames();
            if (null != tns && tns.length > 0) {
                apiName = tns[0];
                if (isLog(apiName)) {
                    Map m = env.getAuthInfo();
                    inspath = env.getTraceNodes();

                    if (null != m) {
                        userCode = (String) m.get("UserName");
                    }

                    if (!sqlCount.containsKey(sql)) {
                        sqlCount.put(sql, new TreeMap());
                        sqlCount.get(sql).put("Count", new AtomicLong(0));
                    }
                    if (((AtomicLong) sqlCount.get(sql).get("Count")).longValue() >= Long.MAX_VALUE) {
                        ((AtomicLong) sqlCount.get(sql).get("Count")).set(0);
                    }
                    ((AtomicLong) sqlCount.get(sql).get("Count")).addAndGet(1);
                    if (sqlCount.size() > MAX_RECORDS) {
                        sqlCount.remove(sqlCount.lastKey());
                    }
                    Long pre = (Long) sqlCount.get(sql).get("Cost");
                    if (null == pre) {
                        pre = new Long(0);
                    }
                    sqlCount.get(sql).put("Cost", pre + cost);
                    if (runingSql.size() > MAX_RECORDS) {
                        runingSql.remove(runingSql.getLast());
                    }
                    Map map = new HashMap();
                    map.put("UserCode", userCode);
                    map.put("InsPath", inspath);
                    map.put("APIName", apiName);
                    map.put("Cost", cost);
                    map.put("Sql", sql);
                    if (null != parameters) {
                        map.put("SqlParameters", ObjectUtils.convertMap2String(parameters));
                    } else {
                        map.put("SqlParameters", "{}");
                    }

                    runingSql.add(map);
                }
            }
        }

    }

    @Override
    public String getOutApis() {
        return ObjectUtils.convertMap2String(outapiCount);
    }
    public String getRunningOutApis(){
        return ObjectUtils.convertList2String(runingOutapi);
    }
    public static void addOutApiStaticInfo(String outApiName,long cost,Map parameters,XMLParameter env,Exception e){
        String apiName=null,userCode=null, inspath=null;
        if(null !=env) {
            String[] tns = env.getTargetNames();
            if (null != tns && tns.length > 0) {
                apiName = tns[0];
                if (isLog(apiName)) {
                    Map m = env.getAuthInfo();
                    inspath = env.getTraceNodes();

                    if (null != m) {
                        userCode = (String) m.get("UserName");
                    }
                    if (!outapiCount.containsKey(outApiName)) {
                        outapiCount.put(outApiName, new TreeMap());
                        outapiCount.get(outApiName).put("Count", new AtomicLong(0));
                        outapiCount.get(outApiName).put("ErrorCount", new AtomicLong(0));
                    }
                    if (null == e) {
                        if (((AtomicLong) outapiCount.get(outApiName).get("Count")).longValue() >= Long.MAX_VALUE) {
                            ((AtomicLong) outapiCount.get(outApiName).get("Count")).set(0);
                        }
                        ((AtomicLong) outapiCount.get(outApiName).get("Count")).addAndGet(1);
                        if (outapiCount.size() > MAX_RECORDS) {
                            outapiCount.remove(outapiCount.lastKey());
                        }
                        Long pre = (Long) outapiCount.get(outApiName).get("Cost");
                        if (null == pre) {
                            pre = new Long(0);
                        }
                        outapiCount.get(outApiName).put("Cost", pre + cost);
                        if (runingOutapi.size() > MAX_RECORDS) {
                            runingOutapi.remove(runingOutapi.getLast());
                        }
                    } else {
                        ((AtomicLong) outapiCount.get(outApiName).get("ErrorCount")).addAndGet(1);
                    }
                    Map map = new HashMap();
                    map.put("UserCode", userCode);
                    map.put("InsPath", inspath);
                    map.put("APIName", apiName);
                    map.put("Cost", cost);
                    map.put("OutApi", outApiName);
                    if (null != parameters) {
                        map.put("InputParameters", ObjectUtils.convertMap2String(parameters));
                    } else {
                        map.put("InputParameters", "{}");
                    }

                    runingOutapi.add(map);
                }
            }
        }
    }

    @Override
    public String getQuartzInfo() {
            try {
                Map m = ExecutorUtils.getWorkTimeTaskInfo();
                if(null !=m) {
                    m.put("IP",NetUtils.getip());
                    m.put("PID",JVMUtil.getPid());
                    Bridge b = (Bridge) getObjectById("bridge");
                    if(null !=b) {
                        m.put("InsId",b.getInstanceId());
                    }
                    return ObjectUtils.convertMap2String(m);
                }
                return "{}";
            } catch (SchedulerException e) {
                return "{}";
            }

    }


    /**
     *
     * @return {threadname:{starttime:'',client:'',user:'',input:'',servicename:'',trace:'',fullPath:'',sql:'',sqlcost:'',outapi:'',outapicost:''},...}
     */
    @Override
    public String getActiveThreadLoad() {
        if (isStart && null != workThreadPool) {
            List runningThread = new ArrayList();
            String pid = JVMUtil.getPid();
            String ip = NetUtils.getip();
            try {
                Set<Thread> st = (Set)ClassUtils.getFieldValue(workThreadPool, "_threads", false);
                if(null != st) {
                    Iterator<Thread> is = st.iterator();
                    while(is.hasNext()) {
                        Thread t = is.next();
                        if(t.isAlive()) {
                            try {
                                Object tt = ClassUtils.getFieldValue(t, "target", false);
                                if(null != tt) {
                                    //log.error("====:"+tt);
                                    HashMap map = new HashMap();
                                    map.put("ThreadName", t.getName());
                                    map.put("IP",ip);
                                    map.put("PID",pid);
                                    /*if (null != clientInfo) {
                                        map.put("ClientInfo", ObjectUtils.convertMap2String(clientInfo));
                                    }
                                    map.put("UserCode", userCode);
                                    map.put("ServiceName", apiName);
                                    map.put("InsPath", fullpath);*/
                                    StackTraceElement[] items = t.getStackTrace();
                                    if (null != items)
                                        map.put("Trace", getMessage(items,traceLikes));
                                    runningThread.add(map);
                                }
                            }catch (Exception e){}
                        }
                    }
                    return ObjectUtils.convertList2String(runningThread);
                }
            }catch (Exception e){

            }

        }
        return "[]";

    }
    static String getMessage(StackTraceElement[] items,List traceLikes){
        if(null != items && null != traceLikes){
            int c=30;
            if(items.length<c){
                c=items.length;
            }
            StringBuffer sb = new StringBuffer();
            for(int i= 0;i< c;i++){
                if(ArrayUtils.isLikeArrayInString(items[i].getClassName(),traceLikes)) {
                    sb.append(" ").append(items[i].toString()).append("\n");
                }
            }
            return sb.toString();
        }
        return "";
    }

    /**
     *
     * @return {ip:'',port:'',insname:'',startTime:'',cpu:'',mem:'',disk:'',jetttWorkThreadMaxCount:'',jettyWorkThreadCount:'',activeThreadCount:'',maxThreadCount:''}
     */
    @Override
    public String getInstance() {
        Map instance= new HashMap();
        instance.put("IP",NetUtils.getip());
        instance.put("PID",JVMUtil.getPid());
        Bridge b = (Bridge) getObjectById("bridge");
        if(null !=b) {
            instance.put("InsId",b.getInstanceId());
        }
        if(null != b.getEnv() && null != b.getEnv().getEnv()) {
            Object webport = b.getEnv().getEnv().get("webport");
            if (null != webport) {
                instance.put("WebPort",webport);
            }
            Object ishttps = b.getEnv().getEnv().get("ishttps");
            Object ssl_port = b.getEnv().getEnv().get("ssl_port");
            if (null != ssl_port && StringUtils.isTrue(ishttps.toString())) {
                instance.put("SSLPort",ssl_port);
            }
            if(null != getObjectById("cxf_webservice")){
                Object ws_host = b.getEnv().getEnv().get("ws_host");
                if (StringUtils.isNotBlank(ws_host)) {
                    if(ws_host.toString().contains(":")) {
                        String wsport = ws_host.toString().substring(ws_host.toString().lastIndexOf(":")+1);
                        instance.put("WSPort", wsport);
                    }
                }
            }
        }
        instance.put("StartTime",b.getStartTime());
        if(null != workThreadPool) {
            Map tp = new HashMap();
            tp.put("BusyThreads",workThreadPool.getBusyThreads());
            tp.put("IdleThreads",workThreadPool.getIdleThreads());
            tp.put("IdleTimeout",workThreadPool.getIdleTimeout());
            tp.put("MaxThreads",workThreadPool.getMaxThreads());
            tp.put("MinThreads",workThreadPool.getMinThreads());
            tp.put("QueueSize",workThreadPool.getQueueSize());
            tp.put("ReservedThreads",workThreadPool.getReservedThreads());
            instance.put("ThreadPool", tp);
        }
        return ObjectUtils.convertMap2String(instance);
    }


    public static void addBridgeBefore(XMLParameter env){
        if(isStart) {
            if(null ==workThreadPool){
                if(count.intValue()>0) {
                    count.decrementAndGet();
                    try {
                        Object o = ClassUtils.getFieldValue(Thread.currentThread(), "target", false);
                        if (null != o && "org.eclipse.jetty.util.thread.QueuedThreadPool$2".equals(o.getClass().getName())) {
                            Object oo = ClassUtils.getFieldValue(o, "this$0", false);
                            if (null != oo && oo instanceof QueuedThreadPool)
                                workThreadPool = (QueuedThreadPool) oo;
                        }
                    } catch (NoSuchFieldException e) {

                    } catch (IllegalAccessException e) {

                    }
                }
            }
            String[] targetNames = env.getTargetNames();
            if (null != targetNames) {
                for (String name : targetNames) {
                    if(!exclude.contains(name)) {
                        addApiInvokeTime(name);
                        setApiInvokeDurTime(name);
                        Map user = (Map) env.getAuthInfo();
                        if (null != user) {
                            String userName = (String) user.get("UserName");
                            Date sessionCreate = null;
                            if (StringUtils.isNotBlank(user.get("KEY_SESSION_CREATE_DATE"))) {
                                Object t = user.get("KEY_SESSION_CREATE_DATE");
                                if (t instanceof String)
                                    sessionCreate = new Date(Long.parseLong((String) user.get("KEY_SESSION_CREATE_DATE")));
                                else if (t instanceof Long)
                                    sessionCreate = new Date((Long) user.get("KEY_SESSION_CREATE_DATE"));
                            }
                            Date expire = null;
                            if (StringUtils.isNotBlank(user.get("KEY_SESSION_ACTIVE_DATE")) && StringUtils.isNotBlank(user.get("expire"))) {
                                Object t = user.get("expire");
                                Object p = user.get("KEY_SESSION_ACTIVE_DATE");
                                if(null !=p && p instanceof Long) {
                                    if (t instanceof String) {
                                        expire = DateTimeUtils.addOrMinusSecond((Long)p, Integer.parseInt((String) user.get("expire")));
                                    } else if (t instanceof Integer)
                                        expire = DateTimeUtils.addOrMinusSecond((Long)p, (Integer) user.get("expire"));
                                    else if (t instanceof Long)
                                        expire = DateTimeUtils.addOrMinusSecond((Long)p, ((Long) user.get("expire")).intValue());
                                }
                                if (StringUtils.isNotBlank(userName)) {
                                    Map client =null;
                                    Object c = env.get("${clientInfo}");
                                    if (null != c && c instanceof Map) {
                                        client = (Map) c;
                                    }
                                    if(null != sessionCreate)
                                        setUserApiInvoke(userName, env.getContext().getUserDatetimeFormat().format(sessionCreate), expire, name,client);
                                    else{
                                        setUserApiInvoke(userName, null, expire, name,client);
                                    }
                                }
                            }

                        }
                    }
                }
            }
        }

    }
    public static void addBridgeAfter(XMLParameter env,Exception e,long cost){
        if(isStart) {
            String[] targetNames = env.getTargetNames();
            if (null != targetNames) {
                for (String name : targetNames) {
                    if(!exclude.contains(name)) {
                        if (null == e) {
                            setSucessApiCost(name, cost);
                        } else {
                            setErrorCount(name);
                        }
                    }
                }
            }
        }
    }
    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return false;
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return null;
    }

    @Override
    public ResultCheck checkReturn(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return null;
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret, Exception e) throws Exception {
        return false;
    }
}
