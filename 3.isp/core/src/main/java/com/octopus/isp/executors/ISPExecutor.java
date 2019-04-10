package com.octopus.isp.executors;

import com.octopus.isp.ds.Context;
import com.octopus.isp.ds.Env;
import com.octopus.isp.ds.RequestParameters;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.bftask.BFParameters;
import com.octopus.utils.bftask.IBFExecutor;
import com.octopus.utils.exception.ISPException;
import com.octopus.utils.file.FileUtils;
import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.XMLUtil;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-8-12
 * Time: 上午11:22
 */
public class ISPExecutor extends XMLDoObject implements IBFExecutor {
    String simreturndir;
    Map simReturn = new HashMap();
    Map simInput = new HashMap();
    static transient Log log = LogFactory.getLog(ISPExecutor.class);
    public ISPExecutor(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        if(StringUtils.isNotBlank(xml.getProperties().getProperty("simreturndir"))) {
            simreturndir = (String) getEmptyParameter().getValueFromExpress(xml.getProperties().getProperty("simreturndir"),this);
        }
    }

    public void doInitial()throws Exception{
        try{
            //load sim return
            if(StringUtils.isNotBlank(simreturndir)) {
                List<String> ls = FileUtils.getAllFileNames(simreturndir, null);
                for(String l:ls){
                    String r = FileUtils.getFileContentString(l);
                    l = l.replaceAll("\\\\","/");
                    String name = l.substring(l.lastIndexOf("/")+1,l.lastIndexOf("."));
                    Object o = null;
                    if(r.startsWith("{")) {
                        o = StringUtils.convert2MapJSONObject(r);
                    }else if(r.startsWith("[")){
                        o = StringUtils.convert2ListJSONObject(r);
                    }
                    if(null != o){
                        if(name.startsWith("return_")) {
                            simReturn.put(name, o);
                        }else if (name.startsWith("input_")){
                            simInput.put(name,o);
                        }
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @Override
    public void execute(XMLMakeup xml, String action, BFParameters parameters, Throwable error) throws Exception {
        String[] actions=null;
        Env env=null;
        Context context=null;
        if(parameters instanceof RequestParameters){
            RequestParameters par = (RequestParameters)parameters;
            if(null != action && action instanceof String && action.equals("${targetNames}")){
                String[] pars = par.getTargetNames();
                if(null != pars){
                    actions=pars;
                    if(null != par.getInputParameter() && par.getInputParameter() instanceof Map && ((Map)par.getInputParameter()).containsKey("op")){
                        Object ac = getAction(actions[0],parameters);
                        if (ac instanceof XMLObject) {
                            Object desc=null;
                            if("USDL".equals(((Map)par.getInputParameter()).get("op"))) {
                                desc = ((XMLObject) ac).getDescStructure();
                            }else if("USDL-INVOKE-INPUT".equals(((Map)par.getInputParameter()).get("op"))){
                                desc = ((XMLObject) ac).getDescStructure().get("input");
                            }else if("USDL-INVOKE-OUTPUT".equals(((Map)par.getInputParameter()).get("op"))){
                                desc = ((XMLObject) ac).getDescStructure().get("output");
                            }else if("USDL-INVOKE-CONFIG".equals(((Map)par.getInputParameter()).get("op"))){
                                desc = ((XMLObject) ac).getDescStructure().get("config");
                            }
                            if (null != desc) {
                                ResultCheck rc = new ResultCheck();
                                rc.setSuccess(true);
                                rc.setRet(desc);
                                parameters.setResult(rc);
                                return;
                            }
                        }
                    }
                }
                //由web指定${targetNames}跳转的action用原有的action配置
                xml=null;
            }
            env = par.getEnv();
            context=par.getContext();
        }
        if(null == actions) {
            actions = new String[]{(String)action};

        }
        XMLDoObject[] xmlActions = new XMLDoObject[actions.length];
        for(int i=0;i<actions.length;i++){
            Object ac = getAction(actions[i],parameters);
            if(null != ac && ac instanceof XMLDoObject){
                xmlActions[i]=(XMLDoObject)ac;
            }else{
                throw new ISPException("S-404","not find can execute object["+actions[i]+"]");
            }
        }
        if(xmlActions.length==1){
            if(null != env && null != env.get("isSimReturn") && StringUtils.isTrue((String)env.get("isSimReturn")) && simReturn.containsKey("return_"+actions[0])){
                parameters.setResult(new ResultCheck(true,simReturn.get("return_"+(String)actions[0])));
            }else {
                ExecutorUtils.synWork(xmlActions[0], "doThing", new Class[]{XMLParameter.class, XMLMakeup.class}, new Object[]{parameters, xml});
            }
        }else{
            ExecutorUtils.multiWorkSameParWaiting(xmlActions,"doThing",new Class[]{XMLParameter.class,XMLMakeup.class},new Object[]{parameters,xml});
        }
    }

    XMLDoObject getAction(String actionName,XMLParameter env){
        if(null != env) {
            //set only check input parameter rule by input data
            Object in = env.getParameter("${queryStringMap}");
            if(null != in && in instanceof Map && "input".equals(((Map)in).get("^OnlyRuleCheck")) && ArrayUtils.isContainStringArray(env.getTargetNames(),actionName)){
                env.setOnlyInputCheck();
            }

            Map m = (Map) env.get("${session}");
            if (null != m) {
                String tenant = (String) m.get("TENANT_CODE");
                if (StringUtils.isNotBlank(tenant)) {
                    Object o = this.getObjectById("("+tenant + ")" + actionName);
                    if (null != o) {
                        if(log.isDebugEnabled()) {
                            log.debug(env.getTargetNames()[0] + " used tenant object :(" + tenant + ")" + actionName);
                        }
                        return (XMLDoObject) o;
                    } else {
                        return (XMLDoObject) this.getObjectById(actionName);
                    }
                }
            }
        }
        return (XMLDoObject)this.getObjectById(actionName);
    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;
    }
    Object getReturnStructure(String name){
        XMLObject o = getXMLObjectContainer().get(name);
        if(null != o){
            try {
                return o.getInvokeDescStructure().get("output");

            }catch (Exception e){}
        }
        return null;
    }
    Object getInputStructure(String name){
        XMLObject o = getXMLObjectContainer().get(name);
        if(null != o){
            try {
                return o.getInvokeDescStructure().get("input");

            }catch (Exception e){}
        }
        return null;
    }
    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(null != input ){
            String op = (String)input.get("op");
            if("getReturnStructure".equals(op) && StringUtils.isNotBlank(simreturndir)){
                Object ret = getReturnStructure((String) input.get("name"));
                Object sim = simReturn.get("return_"+(String) input.get("name"));
                if(null != sim){
                    return sim;
                }else {
                    return ret;
                }
            }if("getInputStructure".equals(op) && StringUtils.isNotBlank(simreturndir)){
                Object ret = getInputStructure((String) input.get("name"));
                Object sim = simInput.get("input_"+(String) input.get("name"));
                if(null != sim){
                    return sim;
                }else {
                    return ret;
                }
            }else if("isSimReturn".equals(op) && StringUtils.isNotBlank(simreturndir)){
                String name = "return_"+(String)input.get("name");
                if(StringUtils.isNotBlank(name)){
                    return simReturn.containsKey(name);
                }
                return false;
            }else if("getSimReturn".equals(op) && StringUtils.isNotBlank(simreturndir)){
                String name = "return_"+(String)input.get("name");
                if(StringUtils.isNotBlank(name)){
                    return simReturn.get(name);
                }
                return null;
            }else if("saveSimReturn".equals(op) && StringUtils.isNotBlank(simreturndir)){
                Object o = input.get("data");
                String name = "return_"+(String)input.get("name");
                String savepath = (String)input.get("savepath");
                if(StringUtils.isBlank(savepath)){
                    savepath = simreturndir;
                }
                FileUtils.makeDirectoryPath(savepath);
                StringBuffer sb = new StringBuffer();
                List ids = new ArrayList();
                ObjectUtils.appendObject2StringBuffer(sb, null, o);
                FileUtils.saveStringBufferFile(sb,savepath+"/"+name+".txt",false);
                simReturn.put(name,o);

                return true;
            }else if("removeSimReturn".equals(op) && StringUtils.isNotBlank(simreturndir)){
                String name = "return_"+(String)input.get("name");
                String savepath = (String)input.get("savepath");
                if(StringUtils.isBlank(savepath)){
                    savepath = simreturndir;
                }
                if(StringUtils.isNotBlank(name)) {
                    FileUtils.removeFile(savepath+"/"+name+".txt");
                    simReturn.remove(name);
                }
                return true;
            }else if("isSimInput".equals(op) && StringUtils.isNotBlank(simreturndir)){
                String name = "input_"+(String)input.get("name");
                if(StringUtils.isNotBlank(name)){
                    return simInput.containsKey(name);
                }
                return false;
            }else if("getSimInput".equals(op) && StringUtils.isNotBlank(simreturndir)){
                String name = "input_"+(String)input.get("name");
                if(StringUtils.isNotBlank(name)){
                    return simInput.get(name);
                }
                return null;
            }else if("saveSimInput".equals(op) && StringUtils.isNotBlank(simreturndir)){
                Object o = input.get("data");
                String name = "input_"+(String)input.get("name");
                String savepath = (String)input.get("savepath");
                if(StringUtils.isBlank(savepath)){
                    savepath = simreturndir;
                }
                FileUtils.makeDirectoryPath(savepath);
                StringBuffer sb = new StringBuffer();
                List ids = new ArrayList();
                ObjectUtils.appendObject2StringBuffer(sb, null, o);
                FileUtils.saveStringBufferFile(sb,savepath+"/"+name+".txt",false);
                simInput.put(name,o);

                return true;
            }else if("removeSimInput".equals(op) && StringUtils.isNotBlank(simreturndir)){
                String name = "input_"+(String)input.get("name");
                String savepath = (String)input.get("savepath");
                if(StringUtils.isBlank(savepath)){
                    savepath = simreturndir;
                }
                if(StringUtils.isNotBlank(name)) {
                    FileUtils.removeFile(savepath+"/"+name+".txt");
                    simInput.remove(name);
                }
                return true;
            }else if("execute".equals(op)){
                Object o  =input.get("exe_id");
                if(null == o){
                    o = env.getTargetNames();
                }
                String actionid=null;
                if(null != o) {
                    if (o.getClass().isArray()) {
                        actionid = ((String[]) o)[0];
                    } else {
                        actionid = (String)o;
                    }
                    Object m = input.get("exe_xml");
                    if(null == m && null != env && null != env.get("^${input}") && env.get("^${input}") instanceof Map){
                        m = ((Map)env.get("^${input}")).get("exe_xml");
                    }
                    if(m instanceof String){
                        m = XMLUtil.getDataFromString((String) m);
                    }
                    Exception e=null;
                    if(null != input.get("exe_error") && input.get("exe_error") instanceof Exception){
                        e = (Exception)input.get("exe_error");
                    }
                    execute((XMLMakeup)m,actionid , (BFParameters) env, e);
                    return env.getResult();
                }
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
