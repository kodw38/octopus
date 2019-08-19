package com.octopus.isp.handlers;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cls.proxy.IMethodAddition;
import com.octopus.utils.exception.ExceptionUtil;
import com.octopus.utils.exception.ISPException;
import com.octopus.utils.rule.RuleUtil;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * Created by Administrator on 2018/9/23.
 */
public class CheckServiceInputRuleHandler  extends XMLDoObject implements IMethodAddition {
    transient static Log log = LogFactory.getLog(CheckServiceInputRuleHandler.class);
    //Handler must exist these properties
    List<String> methods;
    boolean isWaitBefore;
    boolean isWaitAfter;
    boolean isWaitResult;
    boolean isNextInvoke;
    Map in=null;
    XMLDoObject cache_srv_rule;
    XMLDoObject cache_srv_rule_return;

    int level;
    static Map defineRuleElement=null;//在constant中定义的serviceinputcheckrules
    public CheckServiceInputRuleHandler(XMLMakeup xml, XMLObject parent, Object[] containers) throws Exception {
        super(xml, parent, containers);
        isWaitBefore= StringUtils.isTrue(xml.getProperties().getProperty("iswaitebefore"));
        isWaitAfter= StringUtils.isTrue(xml.getProperties().getProperty("iswaitafter"));
        isWaitResult= StringUtils.isTrue(xml.getProperties().getProperty("iswaitresult"));
        isNextInvoke= StringUtils.isTrue(xml.getProperties().getProperty("isnextinvoke"));

    }

    @Override
    public Object beforeAction(Object impl, String m, Object[] args) throws Exception {
        if(null != impl) {
            Map data = getDataMap(args);
            String id =((XMLObject) impl).getXML().getId();
            List<Map> rules = getRuleByActionId((XMLParameter)args[1],id);
            checkRule(rules, data, (XMLParameter) args[1],id);
            //log.error(((XMLObject) impl).getXML().getId() +" "+m+ " do check input...");
        }
        return null;
    }

    void checkRule(List<Map> rules,Map data,XMLParameter env,String id) throws Exception {
        if(null != rules) {
            for(Map rule:rules) {
                if (null !=rule && StringUtils.isNotBlank(rule.get("RULE"))) {
                    if (null != data) {
                        String mp = (String)rule.get("PARAMETER_MAPPING");
                        XMLParameter e = env;
                        if(StringUtils.isNotBlank(mp) && data instanceof XMLParameter){
                            Map d = ((XMLParameter)data).getMapValueFromParameter(StringUtils.convert2MapJSONObject(mp),this);
                            if(log.isDebugEnabled()){
                                log.debug("check rule:\ndata:"+d);
                            }
                            if(null == e){
                                e = getEmptyParameter();
                            }
                            if(null != d){
                                e.putAll(d);
                            }

                        }

                        Object rul = e.getExpressValueFromMap((String)rule.get("RULE"),this);
                        if(rul instanceof String) {
                            if(log.isDebugEnabled()){
                                log.debug("check rule:\nrule:"+rul+"\ndata:"+data);
                            }
                            try {
                                Object o = RuleUtil.doRule((String) rul, data);
                                if (null != o && o instanceof Boolean && !(Boolean) o) {
                                    Object s = env.getValueFromExpress(rule.get("NOT_CHECK_MESSAGE"), this);
                                    if (null != s) {
                                        if((s instanceof String && ((String) s).startsWith("{"))){
                                            throw new ISPException(null,(String)s);
                                        }else {
                                            s = s.toString();
                                            s = StringUtils.replace((String) s, "\"", "\\\"");
                                            s = StringUtils.replace((String) s, "\\\\\"", "\\\\\\\"");
                                            throw new ISPException("600", "check [" + id + "] input parameters fail: " + s);
                                        }
                                    }

                                }
                            }catch(Exception ex){
                                throw ex;
                            }
                        }
                    }
                }
            }
        }
    }

    //根据服务名称获取对应的入参检查规则
    List<Map> getRuleByActionId(XMLParameter env,String s){
        try {
            if(null != cache_srv_rule) {
                HashMap in = new HashMap();
                //in.put("cache", "cache_srv_rule");
                in.put("op", "get");
                in.put("key", s);
                List ret = (List) cache_srv_rule.doSomeThing(null, env, in, null, null);
                if (null != ret) {
                    return ret;
                } else {
                    return null;
                }
            }
        }catch (Exception e){
            log.error("get rule by serviceId fail",e);
        }
        return null;
    }

    List<Map> getReturnRuleByActionId(XMLParameter env,String s){
        try {
            if(null != cache_srv_rule_return) {
                HashMap in = new HashMap();
                //in.put("cache", "cache_srv_rule");
                in.put("op", "get");
                in.put("key", s);
                List ret = (List) cache_srv_rule_return.doSomeThing(null, env, in, null, null);
                if (null != ret) {
                    return ret;
                } else {
                    return null;
                }
            }
        }catch (Exception e){
            log.error("get rule by serviceId fail",e);
        }
        return null;
    }
    //装载定义的规则元素
    Map getDataMap(Object[] args){
        if(args.length>1) {
            if(args[1] instanceof Map) {
                Map m = (Map) args[1];
                if(null == defineRuleElement){
                    defineRuleElement=initDefineRuleElement(m);
                }
                if(null != defineRuleElement) {
                    m.putAll(defineRuleElement);
                }
                return m;
            }
        }
        return null;
    }
    Map initDefineRuleElement(Map m){
        Map c = (Map)m.get("${constant}");
        if(null != c){
            HashMap ret = new HashMap();
            Map s = (Map)c.get("serviceinputcheckrules");
            if(null != s){
                Iterator it = s.keySet().iterator();
                while(it.hasNext()){
                    String k = (String)it.next();
                    String v = (String)s.get(k);
                    if(StringUtils.isNotBlank(k) && StringUtils.isNotBlank(v)){

                            try {
                                Object re = Class.forName(v).newInstance();
                                ret.put(k,re);
                            } catch (InstantiationException e) {
                                log.error(e);
                            } catch (IllegalAccessException e) {
                                log.error(e);
                            } catch (ClassNotFoundException e) {
                                log.error(e);
                            }

                    }
                }
            }
            if(ret.size()>0) return ret;
        }
        return null;
    }

    Map initDefineReturnRuleElement(Map m){
        Map c = (Map)m.get("${constant}");
        if(null != c){
            HashMap ret = new HashMap();
            Map s = (Map)c.get("serviceoutputcheckrules");
            if(null != s){
                Iterator it = s.keySet().iterator();
                while(it.hasNext()){
                    String k = (String)it.next();
                    String v = (String)s.get(k);
                    if(StringUtils.isNotBlank(k) && StringUtils.isNotBlank(v)){

                        try {
                            Object re = Class.forName(v).newInstance();
                            ret.put(k,re);
                        } catch (InstantiationException e) {
                            log.error(e);
                        } catch (IllegalAccessException e) {
                            log.error(e);
                        } catch (ClassNotFoundException e) {
                            log.error(e);
                        }

                    }
                }
            }
            if(ret.size()>0) return ret;
        }
        return null;
    }
    Map getReturnDataMap(Object[] args,Object ret){
        if(args.length>1) {
            if(args[1] instanceof Map) {
                Map m = (Map) args[1];
                if(null == defineRuleElement){
                    defineRuleElement=initDefineReturnRuleElement(m);
                }
                if(null != defineRuleElement) {
                    m.putAll(defineRuleElement);
                }
                /*if(log.isDebugEnabled()){
                    Iterator its = m.keySet().iterator();
                    while(its.hasNext()){
                        Object k = its.next();
                        log.debug("get return "+k);
                        if(k.toString().startsWith("${return}")){
                            log.debug("get return data:"+m.get(k));
                        }
                        if(k.toString().startsWith("${result}")){
                            log.debug("get return data:"+m.get(k));
                        }
                    }
                    //log.debug("get return "+ret);
                }*/
                if(null != ret && ret instanceof ResultCheck){
                    ret = ((ResultCheck)ret).getRet();
                }
                m.put("${return}",ret);
                return m;
            }
        }
        return null;
    }

    @Override
    public Object afterAction(Object impl, String m, Object[] args, boolean isInvoke, boolean isSuccess, Throwable e, Object result) throws Exception {

        if(null != impl ) {
            String id =((XMLObject) impl).getXML().getId();
            List<Map> rules = getReturnRuleByActionId((XMLParameter) args[1], id);
            if(null != rules) {
                log.debug("get return rule "+rules.size()+" by id:"+id+" result:"+result);
                Map data = getReturnDataMap(args,result);
                checkRule(rules, data, (XMLParameter) args[1], id);
            }
            //log.error(((XMLObject) impl).getXML().getId() +" "+m+ " do check input...");
        }
        return null;
    }

    @Override
    public Object resultAction(Object impl, String m, Object[] args, Object result) {
        return null;
    }

    @Override
    public int getLevel() {
        return level;
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

    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;
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
