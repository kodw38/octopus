package com.octopus.isp.handlers;

import com.octopus.isp.bridge.impl.Bridge;
import com.octopus.isp.ds.Contexts;
import com.octopus.isp.ds.RequestParameters;
import com.octopus.tools.dataclient.v2.DataClient2;
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
public class StatHandler2 extends XMLDoObject implements IMethodAddition {
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
    String instanceId;
    XMLDoObject remote;
    String ip;
    public StatHandler2(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        isWaitBefore= StringUtils.isTrue(xml.getProperties().getProperty("iswaitebefore"));
        isWaitAfter= StringUtils.isTrue(xml.getProperties().getProperty("iswaitafter"));
        isWaitResult= StringUtils.isTrue(xml.getProperties().getProperty("iswaitresult"));
        isNextInvoke= StringUtils.isTrue(xml.getProperties().getProperty("isnextinvoke"));
        addAfterApplicationInitialAction(this,"init",null,null);
    }

    static List statFields = Arrays.asList(new String[]{"SRV_NAME","INVOKE_COUNT","INVOKE_COST_TIME","INVOKE_ERROR_COUNT","INVOKE_SIZE"});
    private void init(){
        try {
            if (null != loghandler) {
                Bridge b = (Bridge) getObjectById("bridge");
                if (null != b) {
                    instanceId = b.getInstanceId();
                    ip = NetUtils.getip();
                    //load history statistic ddata
                    if(null != loghandler && loghandler instanceof DataClient2){
                        DataClient2 d = (DataClient2)loghandler;
                        HashMap input = new HashMap();
                        input.put("op","query");
                        input.put("table","ISP_SRV_STAT_LOG");
                        input.put("fields",statFields);
                        HashMap cond = new HashMap();
                        cond.put("INSTANCE_NAME",instanceId);
                        input.put("conds",cond);
                        List<Map> his = (List)d.doSomeThing(null,null,input,null,null);
                        if(null != his){
                            for(Map m:his){
                                //initial each service previous statistic info when system loud up
                                initSetCount((String)m.get("SRV_NAME"),m);
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
                try {
                    setStatToCenter();
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        },60000,60000);
    }

    void setStatToCenter() throws Exception {
        if(null != loghandler && loghandler instanceof DataClient2) {
            DataClient2 d = (DataClient2) loghandler;
            HashMap m = new HashMap();
            XMLObject o = getRoot();
            String insId="";
            if(o instanceof Bridge){
                insId=((Bridge) o).getInstanceId();
            }
            m.put("op","add");
            m.put("table","isp_srv_stat_log");
            List ls = new ArrayList();
            Iterator<String> its = INVOKE_COUNT.keySet().iterator();
            while(its.hasNext()){
                String k = its.next();
                long[] count = getStat(k);
                Map dd = new HashMap();
                dd.put("INSTANCE_NAME",insId);
                dd.put("SRV_NAME",k);
                dd.put("INVOKE_COUNT",count[0]);
                dd.put("INVOKE_COST_TIME",count[1]);
                dd.put("INVOKE_ERROR_COUNT",count[2]);
                dd.put("INVOKE_SIZE",count[3]);
                if(count[0]!=0||count[1]!=0||count[2]!=0||count[3]!=0)
                    ls.add(dd);
            }
            m.put("datas",ls);
            if(ls.size()>0) {
                //先清除该实例的数据
                Map dm = new HashMap();
                Map cond = new HashMap();
                cond.put("INSTANCE_NAME",insId);
                dm.put("op","delete");
                dm.put("table","isp_srv_stat_log");
                dm.put("conds",cond);
                d.doSomeThing(null,null,dm,null,null);
                //插入最新的数据
                d.doSomeThing(null, null, m, null, null);
            }
        }
    }
    void initSetCount(String id,Map d){
        add(INVOKE_COUNT,id,(Integer)d.get("INVOKE_COUNT"));
        add(INVOKE_COST_TIME,id,(Integer)d.get("INVOKE_COST_TIME"));
        add(INVOKE_ERROR_COUNT,id,(Integer)d.get("INVOKE_ERROR_COUNT"));
        add(INVOKE_THROUGH_DATA_SIZE,id,(Integer)d.get("INVOKE_SIZE"));

    }

    void add(Map<String,AtomicLong> m,String k,long n){
        if(!m.containsKey(k)){
            m.put(k,new AtomicLong());
        }
        m.get(k).addAndGet(n);
    }
    long[] getStat(String k){
        long[] ret = new long[4];
        AtomicLong a = INVOKE_COUNT.get(k);
        if(null != a) ret[0]=a.get(); else ret[0]=0;
        a = INVOKE_COST_TIME.get(k);
        if(null != a) ret[1]=a.get(); else ret[1]=0;
        a = INVOKE_ERROR_COUNT.get(k);
        if(null != a) ret[2]=a.get(); else ret[2]=0;
        a = INVOKE_THROUGH_DATA_SIZE.get(k);
        if(null != a) ret[3]=a.get(); else ret[3]=0;
        return ret;
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

            }else if("initStatInfo".equals(input.get("op"))) {
                //when add xmlobject

            }else if("isStatExist".equals(input.get("op"))){

            }else if("addResponseDataSize".equals(input.get("op"))
                    && null != input && StringUtils.isNotBlank(input.get("srvId"))
                    && env instanceof RequestParameters){
                add(INVOKE_THROUGH_DATA_SIZE, (String)input.get("srvId"), ((RequestParameters)env).getResponseDataSize());

            }else if("setSrvDisable".equals(input.get("op"))){

            }else if("setSrvAble".equals(input.get("op"))){

            }else if("setInsStatusStopped".equals(input.get("op"))){

            }else if("setInsStatusRunning".equals(input.get("op"))){

            }else if("uploadStat".equals(input.get("op"))){
                timerUpload();
            }
        }
        return null;
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
