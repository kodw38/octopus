package com.octopus.isp.executors;

import com.octopus.isp.ds.RequestParameters;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.bftask.BFParameters;
import com.octopus.utils.bftask.IBFExecutor;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: Administrator
 * Date: 14-8-29
 * Time: 下午3:09
 */
public class RuleBFExecutor extends XMLObject implements IBFExecutor {
    public RuleBFExecutor(XMLMakeup xml,XMLObject parent,Object[] containers) throws Exception {
        super(xml,parent,containers);
    }

    @Override
    public void notifyObject(String op, Object obj) throws Exception {

    }

    @Override
    public void addTrigger(Object cond, InvokeTaskByObjName task) throws Exception {

    }

    @Override
    public void destroy() throws Exception {

    }

    @Override
    public void initial() throws Exception {

    }

    static Map ObjectMap=null;
    Map getObjects(){
        if(null == ObjectMap){
            List  ls = getAllUpPropertyObjects();
            if(null != ls){
                HashMap map = new HashMap();
                for(int i=0;i<ls.size();i++){
                    Object o = ls.get(i);
                    map.put(((XMLObject)o).getXML().getName(),o);
                }
                ObjectMap=map;
            }
        }
        return ObjectMap;
    }
    @Override
    public void execute(XMLMakeup xml,String action, BFParameters parameters, Throwable error) throws Exception {
        //action为规则编号，规则作为property存放在cell的properties属性中
        RequestParameters par = ((RequestParameters)parameters);
        String ruleTxt=null;
        if(!action.equals("parameterRule")){
            XMLMakeup[] properties = getXML().getChild("properties");
            if(null != properties){
                for(XMLMakeup x:properties){
                    if(action.equals(x.getProperties().getProperty("key"))){
                        ruleTxt = x.getText();
                        break;
                    }
                }
            }
        }else{
            String ruleCode = (String)par.getQueryStringMap().get("rule");
            if(StringUtils.isNotBlank(ruleCode)){
                XMLMakeup[] properties = getXML().getChild("properties");
                if(null != properties){
                    for(XMLMakeup x:properties){
                        if(action.equals(x.getProperties().getProperty("key"))){
                            ruleTxt = x.getText();
                            break;
                        }
                    }
                }
                if(null == ruleTxt){
                    throw new Exception("the rule["+ruleCode+"] is not exist.");
                }
            }
        }
        if(null == ruleTxt) ruleTxt=action;
        if(StringUtils.isNotBlank(ruleTxt)){
            HashMap map = new HashMap();
            map.put("env",par.getEnv());
            map.put("context",par.getContext());
            map.put("session",par.getSession());
            map.put("client",par.getClientInfo());
            map.put("data",par.getRequestData());
            map.put("query",par.getQueryStringMap());

            Map tem = getObjects();
            if(null != tem && tem.size()>0)
                map.putAll(tem);

            map.put("top",par);
            /*XMLMakeup x = par.getXmlParameter();
            Object obj;
            try{
                String sx = xml.getProperties().getProperty("xpar");
                if(StringUtils.isNotBlank(sx)){
                    XMLMakeup xx= XMLUtil.getDataFromString(sx);
                    par.setXmlParameter(xx);
                }
                obj = RuleUtil.doRule(ruleTxt,map);
            }finally {
                par.setXmlParameter(x);
            }
            if(null != obj)
                parameters.setResult(obj);
                */
        }
    }
}
