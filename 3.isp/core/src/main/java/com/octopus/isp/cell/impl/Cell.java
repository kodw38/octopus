package com.octopus.isp.cell.impl;

import com.octopus.isp.cell.ICellListener;
import com.octopus.isp.ds.DataEnv;
import com.octopus.isp.ds.Env;
import com.octopus.isp.tools.IDataGet;
import com.octopus.tools.dataclient.IDataClient;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.flow.FlowParameters;
import com.octopus.utils.flow.IFlowMgr;
import com.octopus.utils.namespace.INamespace;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

/**
 * User: Administrator
 * Date: 14-9-2
 * Time: 下午1:46
 */
public class Cell extends XMLDoObject{
    //属性获取
    IDataGet prosget;
    //存放cell.xml中properties属性的数据
    protected Properties properties;
    //存放action每个action，就是一个flow
    HashMap actions;
    //存放listener
    HashMap<String,ICellListener> listeners;
    INamespace namespace;
    IDataClient dataclient;
    IFlowMgr flows;
    HashMap mappings;

    public Cell(XMLMakeup xml,XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        //set properties
        DataEnv dataEnv = (DataEnv)getPropertyObject("env");
        Env env=null;
        if(null != dataEnv)
            env=dataEnv.getEnv();

        XMLMakeup[] xs = xml.getChild("properties");
        if(ArrayUtils.isNotEmpty(xs)){
            XMLMakeup[] vs = xs[0].getChild("property");
            if(ArrayUtils.isNotEmpty(vs)){
                for(XMLMakeup v:vs){
                    properties.put(v.getProperties().getProperty("name"),v.getText());
                }
            }
        }
        if(null != prosget){
            Map ps = (Map)prosget.getData("query_property",env);
            if(null != ps){
                Iterator ks = ps.keySet().iterator();
                while(ks.hasNext()){
                    String k = (String)ks.next();
                    properties.put(k,ps.get(k));
                }
            }
        }

    }

    @Override
    public Object doSomeThing(String xmlid,XMLParameter env, Map input, Map output,Map config) throws Exception {

            //FlowParameters p = new FlowParameters();
            //p.getReadOnlyParameter().putAll(env.getReadOnlyParameter());
        if(null == env) env = new FlowParameters();
        String flowid = null;
        if(null != input){
             flowid = (String)input.get("flowid");
        }
        if(null != input)
            env.addParameter("^${input}",input);
        if(null != output)
            env.addParameter("^${output}",output);
        if(null != config)
            env.addParameter("^${config}",config);
        /*if(null != xmlid)
            env.addParameter("xmlid",xmlid);*/
        if(StringUtils.isBlank(flowid)){
            flows.doFirstFlow((FlowParameters) env);
        }else
        flows.doFlow(flowid,(FlowParameters)env);
        env.removeParameter("^${input}");
        env.removeParameter("^${output}");
        env.removeParameter("^${config}");
        //env.removeParameter("xmlid");
        return env.getResult();

    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid,XMLParameter env, Map input,Map output, Map config) throws Exception {
        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResultCheck checkReturn(String xmlid,XMLParameter env, Map input, Map output,Map config, Object ret) throws Exception {
        if(null != ret && ret instanceof ResultCheck){
            return (ResultCheck)ret;
        }else{
            if(ret instanceof Throwable){
                return new ResultCheck(false,ret);
            }else{
                return new ResultCheck(true,ret);
            }
        }
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config,Object ret,Exception e) throws Exception {
        //return true;
        throw new Exception("now support rollback");
    }

}
