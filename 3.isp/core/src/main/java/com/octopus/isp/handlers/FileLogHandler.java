package com.octopus.isp.handlers;

import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cls.proxy.IMethodAddition;
import com.octopus.utils.exception.ExceptionUtil;
import com.octopus.utils.exception.Logger;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.util.*;

/**
 * Created by kod on 2017/4/23.
 */
public class FileLogHandler extends XMLDoObject implements IMethodAddition {
    XMLDoObject logger;
    //Handler must exist these properties
    List<String> methods;
    boolean isWaitBefore;
    boolean isWaitAfter;
    boolean isWaitResult;
    boolean isNextInvoke;
    Map in=null;

    public FileLogHandler(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        isWaitBefore= StringUtils.isTrue(xml.getProperties().getProperty("iswaitebefore"));
        isWaitAfter= StringUtils.isTrue(xml.getProperties().getProperty("iswaitafter"));
        isWaitResult= StringUtils.isTrue(xml.getProperties().getProperty("iswaitresult"));
        isNextInvoke= StringUtils.isTrue(xml.getProperties().getProperty("isnextinvoke"));
    }

    @Override
    public Object beforeAction(Object impl, String m, Object[] args) throws Exception {
        try {
            HashMap in = new HashMap();
            in.put("code", "DETAIL_LOG");
            in.put("op","exist");
            Object o = logger.doSomeThing(null, null, in, null, null);
            if(null != o && o instanceof Boolean && (Boolean)o) {
                in.remove("op");
                String head = null;
                XMLMakeup cur = null;
                String reqid = null;
                String pars = null;
                if (args.length == 2 && null != args[1]) {
                    cur = (XMLMakeup) args[1];
                } else {
                    cur = ((XMLObject) impl).getXML();
                }
                if (args.length == 2 && null != args[0]) {
                    synchronized (args[0]) {
                        reqid = (String) ((XMLParameter) args[0]).get("${requestId}");
                        pars = " - input parameters: " + ((Map) args[0]).get("${input_data}");
                        Object ns = ((XMLParameter) args[0]).get("${targetNames}");
                        if (null != ns) {
                            if (Collection.class.isAssignableFrom(ns.getClass()))
                                in.put("subFileName", ArrayUtils.toJoinString((Collection) ns));
                            else if (ns.getClass().isArray()) {
                                in.put("subFileName", ArrayUtils.toJoinString((Object[]) ns));
                            }
                        } else {
                            in.put("subFileName", cur.getId());
                        }
                    }
                } else {
                    reqid = "";
                    in.put("subFileName", cur.getId());
                }
                //head = "RequestId:" + reqid + "|SrvID:" + in.get("subFileName") + "|CurSrv" + cur.getId() + "|Method:" + m + "|CurDate:" + new Date();
                head  = Logger.getString((XMLParameter)args[0],(String)in.get("subFileName"),"",this.getClass().getName(),null);
                in.put("data", head + " - Type: Input " + pars );

                logger.doSomeThing(null, null, in, null, null);
            }
        }catch (Exception ex){
            log.error("",ex);
        }

        return null;
    }

    @Override
    public Object afterAction(Object impl, String m, Object[] args, boolean isInvoke, boolean isSuccess, Throwable e, Object result) {
        if(null != e && impl instanceof XMLObject && null != logger){
            try {
                HashMap in = new HashMap();
                in.put("code", "DETAIL_LOG");
                in.put("op","exist");
                String head = null;
                XMLMakeup cur = null;
                String reqid = null;
                String pars = null;
                Object ns=null;
                if (args.length == 2 && null != args[1]) {
                    cur = (XMLMakeup) args[1];
                } else {
                    cur = ((XMLObject) impl).getXML();
                }
                if (args.length == 2 && null != args[0]) {
                    reqid = (String) ((XMLParameter) args[0]).get("${requestId}");
                    ns =  ((XMLParameter) args[0]).get("${targetNames}");
                    if (null != ns) {
                        if(Collection.class.isAssignableFrom(ns.getClass()))
                            in.put("subFileName", ArrayUtils.toJoinString((Collection)ns));
                        else if(ns.getClass().isArray()){
                            in.put("subFileName", ArrayUtils.toJoinString((Object[])ns));
                        }
                    }else{
                        in.put("subFileName", cur.getId());
                    }
                } else {
                    reqid = "";
                    in.put("subFileName", cur.getId());
                }

                Object o = logger.doSomeThing(null, null, in, null, null);
                if(null != o && o instanceof Boolean && (Boolean)o) {
                    in.remove("op");
                    //head = "RequestId:" + reqid +"|SrvID:" + ((XMLObject) impl).getXML().getId() + "|Method:" + m + "|CurDate:" + new Date() ;
                    head  = Logger.getString((XMLParameter)args[0],((XMLObject) impl).getXML().getId(),"",this.getClass().getName(),e);
                    in.put("data", head + " - Type: Output" + (null!=e?" - Error: "+ ExceptionUtils.getRootCause(e.getCause()):""));

                    logger.doSomeThing(null, null, in, null, null);
                }
                if(null == ((XMLParameter)args[0]).get("^isPrintThrowError")) {
                    in.put("code", "SERVICE_ERROR_LOG");
                    in.put("op", "exist");
                    o = logger.doSomeThing(null, null, in, null, null);
                    if (null != o && o instanceof Boolean && (Boolean) o) {
                        in.remove("op");
                        //head = "RequestId:" + reqid +"|SrvID:" + ((XMLObject) impl).getXML().getId() + "|Method:" + m + "|CurDate:" + new Date() ;
                        head = Logger.getString((XMLParameter) args[0], ((XMLObject) impl).getXML().getId(), "",this.getClass().getName(),e);
                        in.put("data", head + "\n - Type Output \n" + " - input parameter:" + ((Map) args[0]).get("${input_data}") + "\n" + ExceptionUtil.getString(e));
                        logger.doSomeThing(null, null, in, null, null);
                        ((XMLParameter)args[0]).putGlobal("^isPrintThrowError","Y");
                    /*List<String> ns = (List) ((XMLParameter) args[0]).get("${targetNames}");
                    if (null != ns) {
                        for (String n : ns) {
                            head = "SrvID:" + n + "|Method:" + m + "|CurDate:" + new Date() + "|Pars:" + ((Map) args[0]).get("${input_data}");
                            in.put("data", head + "\n" + ExceptionUtil.getRootString(e));
                            in.put("subFileName", n);
                            logger.doSomeThing(null, null, in, null, null);
                        }
                    }*/
                    }
                }
            }catch (Exception ex){
                log.error("",ex);
            }
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
        return false;
    }

    @Override
    public boolean isWaiteAfter() {
        return false;
    }

    @Override
    public boolean isWaiteResult() {
        return false;
    }

    @Override
    public boolean isNextInvoke() {
        return true;
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
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config,Object ret,Exception e) throws Exception {
        return false;
    }
}
