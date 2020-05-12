package com.octopus.isp.bridge.impl;

import com.octopus.isp.bridge.IBridge;
import com.octopus.isp.bridge.ILauncher;
import com.octopus.isp.cell.ICell;
import com.octopus.isp.ds.*;
import com.octopus.tools.mbeans.RunTimeMonitor;
import com.octopus.utils.alone.SNUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.exception.Logger;
import com.octopus.utils.flow.IFlow;
import com.octopus.utils.time.DateTimeUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * User: Administrator
 * Date: 14-9-29
 * Time: 上午11:58
 */
public class Bridge extends XMLDoObject implements IBridge {
    static transient Log log = LogFactory.getLog(Bridge.class);

    HashMap<String,ILauncher> launchers;  //所有启动类
    HashMap<String,ICell> cells;          //管理的cell
    HashMap<String,XMLMakeup> instances;  //主机实例信息
    DataEnv env;                         //环境变量
    Contexts contexts;                //用户上下文信息
    XMLDoObject sessionmgr;                 //用户session信息
    Constant constants;                 //用户session信息
    IFlow flow;                          //处理管道流程
    Date startTime;
    String instanceid;                 //该实例名称，根据该名称装载cell
    //ICellsGet cellsget;                  //cell获取

    public Bridge(XMLMakeup xml,XMLObject parent,Object[] containers) throws Exception {
        super(xml,parent,containers);
        String id = System.getProperty("tb-instanceid");// can set by start script
        if(StringUtils.isNotBlank(id)){
            this.instanceid = id;
        }
        if(StringUtils.isBlank(instanceid)){
           throw new RuntimeException("this instance name is null. please set in bridge.xml");
        }
        if(!instanceid.startsWith("INS-")){
            throw new RuntimeException("this instance name is must start with [INS-]");
        }

        /*XMLMakeup cs = (XMLMakeup)ArrayUtils.getFirst(getXML().getChild("cells"));
        if(null != cs){
            XMLMakeup[] xs = cs.getChild("cell");
            if(ObjectUtils.isNotNull(xs)){
                for(XMLMakeup x:xs){
                    String instanceIds = x.getProperties().getProperty("instanceid");
                    if(null != x && (StringUtils.isNotBlank(instanceIds) && ArrayUtils.isInStringArray(instanceIds.split(","),getInstanceId()))){
                        XMLMakeup c = cellsget.getCell(x);
                        if(null != c){
                            this.addXML(c);
                            log.debug("load cell:"+x);
                        }
                    }
                }
            }
        }*/
    }

    @Override
    public String getInstanceId() {
        return instanceid;
    }

    @Override
    public ILauncher getLauncher(String launcherName) {
        ILauncher launcher = launchers.get(launcherName);
        if(null == launcher)
            log.error("Launcher["+launcherName+"] is not exist.");
        return launcher;
    }

    @Override
    public Object evaluate(RequestParameters parameters) {
        long l = System.currentTimeMillis();
        try{

            parameters.setInstanceid(getInstanceId());
            if(null != constants){
                parameters.setConstant(constants.getConstant());
            }
            if(null != env)
                parameters.setEnv(env.getEnv());
            if(null != contexts) {
                Context context = contexts.getContext(parameters);
                if (null == context) throw new Exception("not find context from the parameters.");
                parameters.setContext(context);
            }
            parameters.setRequestDate(new Date());
            if(StringUtils.isBlank(parameters.getRequestId())) {
                parameters.setRequestId(SNUtils.getNewId());
            }
            RunTimeMonitor.addBridgeBefore(parameters);

            flow.doFlow(parameters);

            if(log.isInfoEnabled()) {
                log.info(Logger.getTraceString(parameters,null));
            }
            RunTimeMonitor.addBridgeAfter(parameters,null,System.currentTimeMillis()-l);
            return parameters.getResult();

        }catch (Exception e){
            log.error("",e);
            ResultCheck ret = new ResultCheck();
            //log.error(this.getClass().getName()+" evaluate error",e);
            Logger.error(this.getClass(),parameters, getXML().getId(),"error happened",e);
            ret.setSuccess(false);
            ret.setRet(e);
            RunTimeMonitor.addBridgeAfter(parameters,e,System.currentTimeMillis()-l);
            if(log.isInfoEnabled()) {
                log.info(Logger.getTraceString(parameters,e));
            }
            return ret;
        }


    }

    public DataEnv getEnv() {
        return env;
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter pars, Map input, Map output, Map config) throws Exception {
        if(null != input){
            if("getSystemParameters".equalsIgnoreCase((String)input.get("op"))){
                RequestParameters parameters = new RequestParameters();
                if(null != env)
                parameters.setEnv(env.getEnv());
                if(null != contexts) {
                    Context context = contexts.getContext(parameters);
                    if (null == context) throw new Exception("not find context from the parameters.");
                        parameters.setContext(context);
                }
                return parameters;
            }
        }
        return null;
    }

    @Override
    public void doInitial() throws Exception {
        startTime = new Date();
    }
    public String getStartTime(){
        try {
            if (null != contexts) {
                return contexts.getDefaultContext().getSystemDate(startTime.getTime());
            }else {
                return DateTimeUtils.date2String(startTime);
            }
        }catch (Exception e) {
            return DateTimeUtils.date2String(startTime);
        }
    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;
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
