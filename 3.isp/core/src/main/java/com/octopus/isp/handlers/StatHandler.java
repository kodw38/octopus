package com.octopus.isp.handlers;

import com.octopus.isp.bridge.impl.Bridge;
import com.octopus.isp.ds.Contexts;
import com.octopus.isp.ds.RequestParameters;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.bftask.BFParameters;
import com.octopus.utils.cls.proxy.IMethodAddition;
import com.octopus.utils.net.NetUtils;
import com.octopus.utils.si.jvm.JVMUtil;
import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.time.DateTimeUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import jdk.internal.util.EnvUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by kod on 2017/3/26.
 */
public class StatHandler extends XMLDoObject implements IMethodAddition {
    static transient Log log = LogFactory.getLog(StatHandler.class);
    static Map<String,AtomicLong> INVOKE_COUNT=new HashMap();//调用成功次数
    static Map<String,AtomicLong> INVOKE_COST_TIME=new HashMap();//调用耗费总时常 mic second
    static Map<String,Long> INVOKE_TIME_TEMP=new HashMap();//调用耗费总时常 mic second
    static Map<String,AtomicLong> INVOKE_ERROR_COUNT=new HashMap();//调用错误次数
    static Map<String,AtomicLong> INVOKE_THROUGH_DATA_SIZE=new HashMap();//通过数据大小 byte
    XMLDoObject loghandler;//上传统计的信息
    List<String> methods = new ArrayList();
    boolean isWaitBefore;
    boolean isWaitAfter;
    boolean isWaitResult;
    boolean isNextInvoke;
    String logpathparent;
    String instanceId;
    String serverpath;
    XMLDoObject remote;
    String ip;
    public StatHandler(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        isWaitBefore= StringUtils.isTrue(xml.getProperties().getProperty("iswaitebefore"));
        isWaitAfter= StringUtils.isTrue(xml.getProperties().getProperty("iswaitafter"));
        isWaitResult= StringUtils.isTrue(xml.getProperties().getProperty("iswaitresult"));
        isNextInvoke= StringUtils.isTrue(xml.getProperties().getProperty("isnextinvoke"));
        logpathparent = xml.getProperties().getProperty("logpathparent");
        serverpath = xml.getProperties().getProperty("serverpath");
        addSystemLoadInitAfterAction(this,"init",null,null);
    }

    private void init(){
        try {
            if (null != loghandler) {
                Bridge b = (Bridge) getObjectById("bridge");
                if (null != b) {
                    instanceId = b.getInstanceId();
                    ip = NetUtils.getip();
                    HashMap map = new HashMap();
                    map.put("op", "getChildrenData");
                    map.put("path", logpathparent);
                    //get all services invoke info
                    Map<String, String> data = (Map) loghandler.doSomeThing(null, null, map, null, null);
                    if (null != data) {
                        Iterator<String> its = data.keySet().iterator();
                        //获取所有服务统计日志
                        while (its.hasNext()) {
                            String k = its.next();
                            log.debug("reload pre stat info:"+k+"  "+data.get(k));
                            //过滤出本主机的服务统计信息
                            if(k.contains("."+instanceId+".")) {// filter services invoke info in this instance , key is service name
                                String id = k.substring(k.indexOf(instanceId)+instanceId.length()+1,k.length()-1);
                                XMLObject obj = getObjectById(id);

                                Map desc = getDescStructure(id);
                                if(null != obj && null != desc) {
                                    String sd = data.get(k);
                                    //之前的统计信息
                                    Map d = StringUtils.convert2MapJSONObject(sd);
                                    //初始化当前计数
                                    initSetCount(id,d);

                                    try {
                                        HashMap in = new HashMap();
                                        in.put("op", "delete");
                                        in.put("path", logpathparent + "/" + k);
                                        loghandler.doSomeThing(null, null, in, null, null);

                                        setStatToCenter(obj,desc,id,obj.isActive());

                                    } catch (Exception e) {
                                        log.error("reset stat info because changed ip error", e);
                                    }
                                }

                            }
                        }
                    }
                    timerUpload();

                }
            }
        }catch (Exception e){
            log.error("StatHandler init error",e);
        }
    }


    void timerUpload(){
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Iterator its = INVOKE_COUNT.keySet().iterator();
                try {
                    XMLParameter env = getEmptyParameter();
                    while(its.hasNext()) {
                        String id = (String)its.next();
                        uploadzk(id, env,getObjectById(id).isActive());
                    }
                }catch (Exception e) {

                }
            }
        },60000,60000);
    }

    void setStatToCenter(XMLObject obj,Map desc,String id,boolean isable) throws Exception {
        String pack = (String) desc.get("package");
        String redo = null;
        boolean isobj = false;
        if (null != desc.get("redo") && desc.get("redo") instanceof String) {
            redo = (String) desc.get("redo");
        }
        if (null != desc.get("redo") && desc.get("redo") instanceof Map) {
            redo = ObjectUtils.convertMap2String((Map) desc.get("redo"));
            isobj = true;
        }
        String path = (String) desc.get("path");
        String opType = (String) desc.get("opType");
        String share = (String) desc.get("share");
        String date = (String) desc.get("date");
        String author = (String) desc.get("author");
        String loadDate=null;
        if (null != ((Contexts) getObjectById("contexts")) && null !=((Contexts) getObjectById("contexts")).getDefaultContext()){
            loadDate = ((Contexts) getObjectById("contexts")).getDefaultContext().getSystemDate();
        }
        if(null == loadDate)loadDate= DateTimeUtils.getCurrDate();
        String isalarm="";
        if(obj instanceof XMLDoObject) {
            isalarm = String.valueOf(((XMLDoObject)obj).isAlarm());
        }
        //更新
        HashMap in= new HashMap();
        in.put("op", "onlySetData");
        String pid = JVMUtil.getPid();
        in.put("path", logpathparent + "/." + instanceId + "." + id + ".");
        in.put("data", "{\"IP\":\"" + ip + "\",\"package\":\""+pack+ "\",\"isable\":\""+String.valueOf(isable)+"\",\"redo\":"+(isobj?redo:("\""+redo+ "\""))+",\"share\":\""+share+"\",\"author\":\""+author+ "\",\"isalarm\":\""+isalarm+ "\",\"path\":\""+path+ "\",\"opType\":\""+opType+ "\",\"date\":\""+date+"\",\"INS_ID\":\"" + instanceId + "\" ,\"INS_STATUS\":\"RUNNING\",\"PID\":" + pid +",\"REGISTER_DATE\":\""+loadDate+"\",\"INVOKE_COUNT\":" + INVOKE_COUNT.get(id) + ",\"INVOKE_ERROR_COUNT\":" + INVOKE_ERROR_COUNT.get(id) + ",\"INVOKE_COST_TIME\":" + INVOKE_COST_TIME.get(id) + ",\"INVOKE_SIZE\":" + INVOKE_THROUGH_DATA_SIZE.get(id) + "}");
        ExecutorUtils.work(loghandler, "doSomeThing", new Class[]{String.class, XMLParameter.class, Map.class, Map.class, Map.class}, new Object[]{null, null, in, null, null});
        log.debug("register service "+id+" to zookeeper "+in.get("path"));

    }
    void initSetCount(String id,Map d){
        setINVOKE_COUNTData(d,id,"INVOKE_COUNT");
        setINVOKE_COUNTData(d,id,"INVOKE_COST_TIME");
        setINVOKE_COUNTData(d,id,"INVOKE_ERROR_COUNT");
        setINVOKE_COUNTData(d,id,"INVOKE_SIZE");
        /*if (null != d.get("INVOKE_COUNT") && !"null".equals(d.get("INVOKE_COUNT"))) {
            if(d.get("INVOKE_COUNT") instanceof String) {
                if("".equals(d.get("INVOKE_COUNT"))){
                    INVOKE_COUNT.put(id, new AtomicLong(0));
                }else {
                    INVOKE_COUNT.put(id, new AtomicLong(Long.valueOf((String) d.get("INVOKE_COUNT"))));
                }
            }else if(d.get("INVOKE_COUNT") instanceof Integer){
                INVOKE_COUNT.put(id, new AtomicLong((Integer) d.get("INVOKE_COUNT")));
            }
        }*/
        /*if (null != d.get("INVOKE_COST_TIME") && !"null".equals(d.get("INVOKE_COST_TIME")))
            INVOKE_COST_TIME.put(id, new AtomicLong(Long.valueOf((String) d.get("INVOKE_COST_TIME"))));
        if (null != d.get("INVOKE_ERROR_COUNT") && !"null".equals(d.get("INVOKE_ERROR_COUNT")))
            INVOKE_ERROR_COUNT.put(id, new AtomicLong(Long.valueOf((String) d.get("INVOKE_ERROR_COUNT"))));
        if (null != d.get("INVOKE_SIZE") && !"null".equals(d.get("INVOKE_SIZE")))
            INVOKE_THROUGH_DATA_SIZE.put(id, new AtomicLong((Long.valueOf((String) d.get("INVOKE_SIZE")))));*/
    }
    void setINVOKE_COUNTData(Map d ,String id,String key){
        if (null != d.get(key) && !"null".equals(d.get(key)) && !"".equals(d.get(key))) {
            if(d.get(key) instanceof String) {
                if("".equals(d.get(key))){
                    INVOKE_COUNT.put(id, new AtomicLong(0));
                }else {
                    INVOKE_COUNT.put(id, new AtomicLong(Long.valueOf((String) d.get(key))));
                }
            }else if(d.get(key) instanceof Integer){
                INVOKE_COUNT.put(id, new AtomicLong((Integer) d.get(key)));
            }else if(d.get(key) instanceof Long){
                INVOKE_COUNT.put(id, new AtomicLong((Long) d.get(key)));
            }
        }
    }
    void add(Map<String,AtomicLong> m,String k,long n){
        if(!m.containsKey(k)){
            m.put(k,new AtomicLong());
        }
        m.get(k).addAndGet(n);
    }
    @Override
    public Object beforeAction(Object impl, String m, Object[] args) throws Exception {
        if(args[0] instanceof RequestParameters && null != ((RequestParameters)args[0]).getRequestId()) {
            INVOKE_TIME_TEMP.put(((RequestParameters)args[0]).getRequestId()+"-"+((XMLObject)impl).getXML().getId(), System.currentTimeMillis());
        }

        return null;
    }

    @Override
    public Object afterAction(Object impl, String m, Object[] args, boolean isInvoke, boolean isSuccess, Throwable e, Object result) {
        if(null == e){
            if(null != impl && impl instanceof XMLObject){
                add(INVOKE_COUNT,((XMLObject)impl).getXML().getId(),1);
            }
        }else{
            if(null != impl && impl instanceof XMLObject){
                add(INVOKE_ERROR_COUNT,((XMLObject)impl).getXML().getId(),1);
            }
        }

        if(args[0] instanceof RequestParameters && null != ((RequestParameters)args[0]).getRequestId()) {
            Long l = INVOKE_TIME_TEMP.get(((RequestParameters) args[0]).getRequestId()+"-"+((XMLObject)impl).getXML().getId());
            if (null != l) {
                add(INVOKE_COST_TIME, ((XMLObject) impl).getXML().getId(), System.currentTimeMillis() - l);
                INVOKE_TIME_TEMP.remove(((RequestParameters) args[0]).getRequestId()+"-"+((XMLObject)impl).getXML().getId());
            }
        }
        /*if(impl instanceof XMLDoObject){
            System.out.println("=="+((XMLDoObject)impl).getXML().getId());
        }*/
        return null;
    }

    @Override
    public Object resultAction(Object impl, String m, Object[] args, Object result) {
        return null;
    }

    @Override
    public  int getLevel() {
        return 0;
    }

    @Override
    public boolean isWaiteBefore() {
        return isWaitBefore;
    }

    @Override
    public boolean isWaiteAfter() {
        return isWaitAfter;
    }

    @Override
    public boolean isWaiteResult() {
        return isWaitResult;
    }

    @Override
    public boolean isNextInvoke() {
        return isNextInvoke;
    }

    @Override
    public void setMethods(List<String> methods) {
        this.methods=methods;
    }

    @Override
    public List<String> getMethods() {
        return methods;
    }

    @Override
    public void doInitial() throws Exception {
        //init();
    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(null != input){
            if("init".equals(input.get("op"))) {
                init();
            }else if("getStatInfo".equals(input.get("op"))){
                String id = (String)input.get("srvId");
                if(null == env) {
                    env= getEmptyParameter();
                }
                String pid = (String)((Map) env.get("${env}")).get("${pid}");
                String ip = (String)((Map) env.get("${env}")).get("${ip}");
                if(StringUtils.isNotBlank(id)){
                    long ic = 0;
                    if(INVOKE_COUNT.containsKey(id)) {
                        ic = INVOKE_COUNT.get(id).longValue();
                    }
                    long it = 0;
                    if(INVOKE_COST_TIME.containsKey(id)) {
                        it= INVOKE_COST_TIME.get(id).longValue();
                    }
                    long ec = 0;
                    if(INVOKE_ERROR_COUNT.containsKey(id)) {
                        ec = INVOKE_ERROR_COUNT.get(id).longValue();
                    }
                    long si = 0;
                    if(INVOKE_THROUGH_DATA_SIZE.containsKey(id)){
                        si = INVOKE_THROUGH_DATA_SIZE.get(id).longValue();
                    }
                    HashMap map = new HashMap();
                    map.put("INVOKE_COUNT",ic);
                    map.put("IS_PUBLISH", "N");
                    map.put("INS_STATUS", "RUNNING");
                    map.put("INVOKE_COST_TIME",it);
                    map.put("INVOKE_ERROR_COUNT",ec);
                    map.put("INVOKE_SIZE",si);
                    Bridge b = (Bridge)getObjectById("bridge");
                    map.put("INS_ID",b.getInstanceId());
                    map.put("IP",ip);
                    map.put("PID",pid);
                    return map;
                }
            }else if("addRequestDataSize".equals(input.get("op"))
                    && null != input && StringUtils.isNotBlank(input.get("srvId"))
                    && env instanceof RequestParameters && ((RequestParameters)env).getRequestDataSize()>0){
                    add(INVOKE_THROUGH_DATA_SIZE, (String)input.get("srvId"), ((RequestParameters)env).getRequestDataSize());

            }else if("deleteStatInfo".equals(input.get("op"))){
                //when remove xmlobject
                try {
                    if(null == env){
                        env = getEmptyParameter();
                    }
                    String id = (String)input.get("name");
                    HashMap in = new HashMap();
                    in.put("op","delete");
                    in.put("path", logpathparent + "/." + instanceId +"." + id + ".");
                    ExecutorUtils.work(loghandler, "doSomeThing", new Class[]{String.class, XMLParameter.class, Map.class, Map.class, Map.class}, new Object[]{null, null, in, null, null});
                }catch (Throwable e){
                    log.error("log stat error",e);
                }
            }else if("initStatInfo".equals(input.get("op"))) {
                //when add xmlobject
                try {
                    if(null == env){
                        env = getEmptyParameter();
                    }
                    if(null == instanceId){
                        Bridge b = (Bridge) getObjectById("bridge");
                        instanceId = b.getInstanceId();
                    }
                    String pid = (String)((Map) env.get("${env}")).get("${pid}");
                    String ip = (String)((Map) env.get("${env}")).get("${ip}");
                    String id = (String)input.get("name");
                    XMLObject obj = getObjectById(id);
                    Map desc = getDescStructure(id);
                    if(null != obj && null != desc) {
                        HashMap in = new HashMap();
                        in.put("path", logpathparent + "/." + instanceId + "." + id + ".");
                        in.put("op", "getData");
                        String sd = (String) loghandler.doSomeThing(null, null, in, null, null);
                        if (null != sd) {
                            Map d = StringUtils.convert2MapJSONObject(sd);
                            initSetCount(id,d);
                        }

                        setStatToCenter(obj,desc,id,obj.isActive());
/*
                        String pack = (String)desc.get("package");
                        String redo = (String)desc.get("redo");
                        String path = (String)desc.get("path");
                        String opType = (String)desc.get("opType");
                        String date = (String)desc.get("date");
                        String share = (String)desc.get("share");
                        String author = (String)desc.get("author");
                        String isalarm="";
                        if(obj instanceof XMLDoObject) {
                            isalarm = String.valueOf(((XMLDoObject)obj).isAlarm());
                        }
                        in.put("op", "onlySetData");
                        AtomicLong count = INVOKE_COUNT.get(id);
                        AtomicLong er = INVOKE_ERROR_COUNT.get(id);
                        AtomicLong time = INVOKE_COST_TIME.get(id);
                        AtomicLong size = INVOKE_THROUGH_DATA_SIZE.get(id);
                        in.put("data", "{\"IP\":\"" + ip + "\",\"package\":\""+pack+ "\",\"redo\":\""+redo+ "\",\"share\":\""+share+"\",\"author\":\""+author+ "\",\"isalarm\":\""+isalarm+ "\",\"path\":\""+path+ "\",\"opType\":\""+opType+ "\",\"date\":\""+date+"\",\"INS_ID\":\"" + insid + "\",\"INS_STATUS\":\"RUNNING\",\"PID\":" + pid + ",\"INVOKE_COUNT\":" + (count == null ? 0 : count.longValue()) + ",\"INVOKE_ERROR_COUNT\":" + (er == null ? 0 : er.longValue()) + ",\"INVOKE_COST_TIME\":" + (time == null ? 0 : time.longValue()) + ",\"INVOKE_SIZE\":" + (size == null ? 0 : size.longValue())  + "}");
                        log.debug("initStatInfo:" + in.get("data"));
                        ExecutorUtils.work(loghandler, "doSomeThing", new Class[]{String.class, XMLParameter.class, Map.class, Map.class, Map.class}, new Object[]{null, null, in, null, null});
*/

                    }
                }catch (Throwable e){
                    log.error("log stat error",e);
                }
            }else if("isStatExist".equals(input.get("op"))){
                String id = (String)input.get("name");
                Bridge b = (Bridge)getObjectById("bridge");
                String insid = b.getInstanceId();
                if(null == env){
                    env = getEmptyParameter();
                }
                String ip = ((String)((Map)env.get("${env}")).get("${ip}"));
                HashMap in = new HashMap();
                in.put("op","getData");
                in.put("path", logpathparent + "/."  + insid +"." + id + ".");
                try {
                    Object o = loghandler.doSomeThing(null, null, in, null, null);
                    log.error("==isExist  :"+o);
                    if(null != o && o instanceof String && StringUtils.isNotBlank(o)){
                        return Boolean.TRUE;
                    }else{
                        return Boolean.FALSE;
                    }
                }catch (Exception e){
                    log.error("isStatExist error",e);
                    return Boolean.FALSE;
                }
            }else if("addResponseDataSize".equals(input.get("op"))
                    && null != input && StringUtils.isNotBlank(input.get("srvId"))
                    && env instanceof RequestParameters){
                add(INVOKE_THROUGH_DATA_SIZE, (String)input.get("srvId"), ((RequestParameters)env).getResponseDataSize());
                if(null != loghandler) {
                    String id = (String)input.get("srvId");
                    uploadzk(id, env,true);
                }

            }else if("setSrvDisable".equals(input.get("op"))){
                String id = (String)input.get("name");
                if(StringUtils.isNotBlank(id)){
                    uploadzk(id,env,false);
                }
            }else if("setSrvAble".equals(input.get("op"))){
                String id = (String)input.get("name");
                if(StringUtils.isNotBlank(id)){
                    uploadzk(id,env,true);
                }
            }else if("setInsStatusStopped".equals(input.get("op"))){
                String id = (String)input.get("insId");
                if("console".equalsIgnoreCase(id)){
                    return null;
                }
                if(StringUtils.isNotBlank(id)){
                    HashMap map = new HashMap();
                    map.put("op", "getChildrenData");
                    map.put("path", logpathparent);
                    Map<String,String> m = (Map)loghandler.doSomeThing(null,null,map,null,null);
                    if(null != m){
                        Iterator<String> its = m.keySet().iterator();
                        while(its.hasNext()){
                            String k = its.next();
                            if(k.contains("."+id+".")){
                                String s = m.get(k);
                                Map sm = StringUtils.convert2MapJSONObject(s);
                                sm.put("INS_STATUS","STOPPED");
                                sm.put("PID","");
                                //s = StringUtils.replace(s,"RUNNING","STOPPED");
                                s = ObjectUtils.convertMap2String(sm);
                                HashMap mt = new HashMap();
                                mt.put("op", "onlySetData");
                                mt.put("isLeaderDo", "true");
                                mt.put("path", logpathparent+"/"+k);
                                mt.put("data",s);
                                loghandler.doSomeThing(null,null,mt,null,null);
                            }
                        }
                        log.info("has stop all services in instance["+id+"]");
                    }
                }
            }else if("setInsStatusRunning".equals(input.get("op"))){

                String id = (String)input.get("insId");
                if("console".equalsIgnoreCase(id)){
                    return null;
                }
                if(log.isInfoEnabled()) {
                    log.info("setInsStatusRunning "+id+"....");
                }
                if(StringUtils.isNotBlank(id)){
                    HashMap map = new HashMap();
                    map.put("op","getData");
                    if(StringUtils.isNotBlank(serverpath)) {
                        map.put("path", serverpath + "/" + id);
                        Object t = loghandler.doSomeThing(null, null, map, null, null);
                        if(log.isDebugEnabled()) {
                            log.debug("setInsStatusRunning  " + t);
                        }
                        if (null != t && !"".equals(t)) {
                            if(t instanceof String){
                                t = StringUtils.convert2MapJSONObject((String)t);
                            }
                            String pid = (String) ((Map) t).get("pid");
                            if(log.isDebugEnabled()) {
                                log.debug("setInsStatusRunning  pid:" + pid);
                            }
                            map.clear();
                            map.put("op", "getChildrenData");
                            map.put("path", logpathparent);
                            Map<String, String> m = (Map) loghandler.doSomeThing(null, null, map, null, null);
                            if (null != m) {
                                Iterator<String> its = m.keySet().iterator();
                                while (its.hasNext()) {
                                    String k = its.next();
                                    if (k.contains("." + id + ".")) {
                                        if(k.length()>(k.indexOf(id)+id.length()+1) && checkRemoteSrvExist(id,k.substring(k.indexOf(id)+id.length()+1,k.length()-1))) {
                                            String s = m.get(k);
                                            Map sm = StringUtils.convert2MapJSONObject(s);

                                            sm.put("INS_STATUS", "RUNNING");
                                            sm.put("PID", pid);
                                            //s = StringUtils.replace(s,"RUNNING","STOPPED");
                                            s = ObjectUtils.convertMap2String(sm);
                                            HashMap mt = new HashMap();
                                            mt.put("op", "onlySetData");
                                            mt.put("isLeaderDo", "true");
                                            mt.put("path", logpathparent + "/" + k);
                                            mt.put("data", s);
                                            loghandler.doSomeThing(null, null, mt, null, null);
                                        }
                                    }
                                }
                                log.info("all services are ready in instance[" + id + "]");
                            }
                        }
                    }
                }

            }else if("uploadStat".equals(input.get("op"))){
                timerUpload();
            }
        }
        return null;
    }
    boolean checkRemoteSrvExist(String insId,String srvName){
        try {
            if(null !=remote) {
                BFParameters p = new BFParameters(false);
                p.addParameter("${targetNames}", new String[]{"isExistService"});
                HashMap inmap = new HashMap();
                inmap.put("name", srvName);
                p.addParameter("${input_data}", inmap);
                p.addParameter("${insid}", insId);
                remote.doThing(p, null);
                Object o = p.getResult();
                if (null != o) {
                    if (o instanceof ResultCheck) {
                        o = ((ResultCheck) o).getRet();
                    }
                    if (null != o && o instanceof Boolean) {
                        log.debug("remote ["+insId+"] check srv ["+srvName+"] return "+o);
                        return (Boolean) o;
                    }
                }
            }
        }catch (Exception e){
            log.error("",e);
            log.debug("remote ["+insId+"] check srv ["+srvName+"] return false");
            return false;
        }
        log.debug("remote ["+insId+"] check srv ["+srvName+"] return false");
        return false;
    }
    synchronized void uploadzk(String id,XMLParameter env,boolean isable) throws IOException {
        HashMap in = new HashMap();
        XMLObject obj = getObjectById(id);
        Map desc = getDescStructure(id);
        in.put("op","onlySetData");
        Bridge b = (Bridge)getObjectById("bridge");
        if(null != b && null != obj && null != desc) {
         try{
             setStatToCenter(obj,desc,id,isable);
         }catch (Exception e){

         }
            /*String pack = (String)desc.get("package");
            String redo = (String)desc.get("redo");
            String share = (String)desc.get("share");
            String path = (String)desc.get("path");
            String opType = (String)desc.get("opType");
            String date = (String)desc.get("date");
            String author = (String)desc.get("author");
            String isalarm="";
            if(obj instanceof XMLDoObject) {
                isalarm = String.valueOf(((XMLDoObject)obj).isAlarm());
            }
            try {
                String pid = (String) ((Map)env.get("${env}")).get("${pid}");
                String insid = b.getInstanceId();
                String ip = (String) ((Map)env.get("${env}")).get("${ip}");
                in.put("path", logpathparent + "/." + insid + "." + id + ".");
                in.put("data", "{\"IP\":\"" + ip + "\",\"package\":\""+pack+ "\",\"redo\":\""+redo+ "\",\"share\":\""+share+"\",\"author\":\""+author+"\",\"isalarm\":\""+isalarm+ "\",\"path\":\""+path+ "\",\"opType\":\""+opType+ "\",\"date\":\""+date+"\",\"INS_ID\":\"" + insid + "\",\"INS_STATUS\":\"RUNNING\",\"PID\":" + pid + ",\"INVOKE_COUNT\":" + INVOKE_COUNT.get(id) + ",\"INVOKE_ERROR_COUNT\":" + INVOKE_ERROR_COUNT.get(id) + ",\"INVOKE_COST_TIME\":" + INVOKE_COST_TIME.get(id) + ",\"INVOKE_SIZE\":" + INVOKE_THROUGH_DATA_SIZE.get(id) + "}");
                if (log.isDebugEnabled()) {
                    log.debug("upload stat info:" + in.get("path")+"  "+in.get("data"));
                }

                ExecutorUtils.work(loghandler, "doSomeThing", new Class[]{String.class, XMLParameter.class, Map.class, Map.class, Map.class}, new Object[]{null, null, in, null, null});
            } catch (Throwable e) {
                log.error("log stat error", e);
            }
*/
        }

    }

    @Override
    public ResultCheck checkReturn(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return new ResultCheck(true,ret);
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
