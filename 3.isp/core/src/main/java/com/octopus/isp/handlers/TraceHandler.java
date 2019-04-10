package com.octopus.isp.handlers;

import com.octopus.isp.ds.RequestParameters;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cls.proxy.IMethodAddition;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * Created by kod on 2017/4/12.
 */
public class TraceHandler extends XMLDoObject implements IMethodAddition {
    static transient Log log = LogFactory.getLog(TraceHandler.class);
    //Handler must exist these properties
    List<String> methods = new ArrayList();
    boolean isWaitBefore=false;
    boolean isWaitAfter=false;
    boolean isWaitResult=false;
    boolean isNextInvoke=false;
    Map in=null;


    XMLDoObject store;
    Map tempDate = new HashMap();
    long curtime=0;
    boolean isstart=false;
    int expire=180;//seconds


    public TraceHandler(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        String input = xml.getProperties().getProperty("input");
        if(StringUtils.isNotBlank(input)){
            in = StringUtils.convert2MapJSONObject(input);
        }
        isWaitBefore= StringUtils.isTrue(xml.getProperties().getProperty("iswaitebefore"));
        isWaitAfter= StringUtils.isTrue(xml.getProperties().getProperty("iswaitafter"));
        isWaitResult= StringUtils.isTrue(xml.getProperties().getProperty("iswaitresult"));
        isNextInvoke= StringUtils.isTrue(xml.getProperties().getProperty("isnextinvoke"));
    }
    void listenerState(){
        if(curtime>0 && (System.currentTimeMillis()-curtime)/1000>expire){
            isstart=false;
        }
    }
    Map getInfo(String method ,String type,RequestParameters req,XMLMakeup xml,XMLObject impl,Object data,boolean issuccess,Throwable er){
        Map map = new HashMap();
        //long l = System.currentTimeMillis();
        //request data
        map.put("RequestId",req.getRequestId());
        map.put("Method",method);
        map.put("Position",type);
        if(xml==null)xml = impl.getXML();
        List<XMLMakeup> cs = xml.getChildren();
        if(null!=cs) {
            List li = new ArrayList();
            for(XMLMakeup c:cs){
                li.add(c.getId());
            }
            if(li.size()>0) {
                map.put("Children", li);
            }
        }
        map.put("RequestDate",req.getRequestDate());
        map.put("SrvName",xml.getId());
        if(null != xml.getParent()) {
            map.put("Path", xml.getParent().getId());
        }else{
            map.put("Path","");
        }
        Date curDate = new Date();
        map.put("CurDate",curDate);

        map.put("State",issuccess);
        if(null != er) {
            String msg = ExceptionUtils.getFullStackTrace(er);
            msg = msg.replaceAll("\r\n","</br>");
            msg = msg.replaceAll("\t","");
            map.put("Error", msg);
        }else if(null != data) {
            map.put("Data", data);
        }
        if(tempDate.containsKey(req.getRequestId())){
            map.put("Cost",curDate.getTime()-(Long)tempDate.get(req.getRequestId()));
        }else {
            tempDate.put(req.getRequestId(), curDate.getTime());
        }
        /*long dateSize = 0;
        if (null != data) {
            try {
                dateSize = SIUtil.getSizeOfJavaObject(data);
            }catch (Exception e){}
        }*/
        //map.put("DataSize",dateSize);
        //Thread info
        map.put("ThreadName",Thread.currentThread().getName());

        map.put("ThreadState",Thread.currentThread().getState().toString());
        map.put("ThreadGroup",Thread.currentThread().getThreadGroup().toString());
        map.put("ThreadPriority", Thread.currentThread().getPriority());
        //StackTraceElement[] stes = Thread.currentThread().getStackTrace();
        //machine
        map.put("IP",req.getEnv().get("${ip}"));
        try {
            map.put("PID",req.getEnv().get("${pid}"));
        }catch (Exception xe){}
        //System.out.println("S:"+(System.currentTimeMillis()-l));
        log.debug("trace:"+map);
        return map;
    }
    @Override
    public Object beforeAction(Object impl, String m, Object[] args) throws Exception {
        if(isstart) {
            if (null != m && "doChildren".equals(m)) return null;
            if (null != store) {
                if (impl instanceof XMLObject && args[0] instanceof RequestParameters && null != ((RequestParameters) args[0]).getRequestId()) {
                    Object data = ((RequestParameters) args[0]).get("${this_input}");
                    if(data ==null){
                        data = ((RequestParameters) args[0]).get("${input_data}");
                    }
                    Map d = getInfo(m, "Before", (RequestParameters) args[0],(XMLMakeup)args[1], (XMLObject) impl, data, true, null);
                    HashMap it = new HashMap();
                    it.put("key", d.get("RequestId"));
                    it.put("value", ObjectUtils.convertMap2String(d));
                    if (null != in) {
                        Map ol = RequestParameters.getMapValueFromParameter((RequestParameters) args[0], in,this);
                        if (null != ol) {
                            it.putAll(ol);
                        }
                    }
                    //Thread.sleep(10);
                    log.debug("before:"+((XMLMakeup)args[1]).getId());
                    store.doSomeThing(null, (RequestParameters) args[0], it, null, null);
                }
            }
            listenerState();
        }
        return null;
    }

    @Override
    public Object afterAction(Object impl, String m, Object[] args, boolean isInvoke, boolean isSuccess, Throwable e, Object result) {
        if(isstart) {
            if (null != m && "doChildren".equals(m)) return null;
            if (null != store) {
                if (impl instanceof XMLObject && args[0] instanceof RequestParameters && null != ((RequestParameters) args[0]).getRequestId()) {
                    try {
                        if (null == result) {
                            //result=((RequestParameters) args[0]).get("${return}");
                            result = ((ResultCheck) ((RequestParameters) args[0]).getResult()).getRet();
                        }
                        Map d = getInfo(m, "After", (RequestParameters) args[0],(XMLMakeup)args[1], (XMLObject) impl, result, isSuccess, e);

                        HashMap it = new HashMap();
                        it.put("key", d.get("RequestId"));
                        it.put("value", ObjectUtils.convertMap2String(d));
                        if (null != in) {
                            Map ol = RequestParameters.getMapValueFromParameter((RequestParameters) args[0], in,this);
                            if (null != ol) {
                                it.putAll(ol);
                            }
                        }
                        //Thread.sleep(10);
                        log.debug("after:"+((XMLMakeup)args[1]).getId());
                        store.doSomeThing(null, (RequestParameters) args[0], it, null, null);
                    } catch (Exception ee) {

                    }

                }
            }
            listenerState();
        }
        return null;
    }

    @Override
    public Object resultAction(Object impl, String m, Object[] args, Object result) {
        return null;
    }

    @Override
    public int getLevel() {
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
    public void notifyObject(String op, Object obj) throws Exception {

    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(null != input){
            if(null != input.get("op") && "start".equals(input.get("op"))){
                curtime = System.currentTimeMillis();
                if(null != input.get("expire")){
                    expire=(Integer)input.get("expire");
                }
                isstart = true;
                log.info("start trace.....");
            }
            if(null != input.get("op") && "stop".equals(input.get("op"))){
                curtime = 0;
                isstart = false;
                Map map = new HashMap();
                if(null != in) {
                    map.putAll(in);
                }
                map.put("op","clear");
                store.doSomeThing(null,null,map,null,null);
                log.info("end trace.....");
            }else if("getTraceList".equals(input.get("op"))){
                Map map = new HashMap();
                if(null != in) {
                    map.putAll(in);
                }
                map.put("op","keys");
                map.put("value",input.get("data"));
                return store.doSomeThing(null,null,map,null,null);
            }else if("getTrace".equals(input.get("op"))){
                Map map = new HashMap();
                if(null != in) {
                    map.putAll(in);
                }
                map.put("op","lget");
                map.put("key",input.get("data"));
                return store.doSomeThing(null,null,map,null,null);
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
