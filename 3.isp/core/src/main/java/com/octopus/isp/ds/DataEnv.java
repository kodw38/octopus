package com.octopus.isp.ds;

import com.octopus.isp.bridge.impl.Bridge;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.exception.Logger;
import com.octopus.utils.file.FileUtils;
import com.octopus.utils.net.NetUtils;
import com.octopus.utils.si.jvm.JVMUtil;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * User: Administrator
 * Date: 14-9-4
 * Time: 上午9:58
 */
public class DataEnv extends XMLDoObject {
    transient static Log log = LogFactory.getLog(DataEnv.class);
    static Env par= new Env();
    public DataEnv(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        setRuntiePar(par);
        par.putAll(System.getProperties());
        XMLParameter.setProperties(par, getXML(), null, true, this);
        par.setCycleused(true);
        XMLMakeup[] xmls = getXML().contansProperty("json");
        if(null != xmls && xmls.length>0){
            for(XMLMakeup x:xmls){
                if(null != x){
                    String txt = FileUtils.getProtocolFile(x.getProperties().getProperty("json"));
                    if(StringUtils.isNotBlank(txt)) {
                        Map m = StringUtils.convert2MapJSONObject(txt);
                        if(null != m){
                            Iterator its = m.keySet().iterator();
                            while(its.hasNext()) {
                                String k = (String)its.next();
                                par.addGlobalParameter(k,m.get(k));
                            }
                        }
                    }
                }
            }
        }
        log.info("env parameters:\n"+par.toString());
    }

    public Env getEnv(){
        return par;
    }

    public void doInitial(){

    }
    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        /*if(par==null){
            par = (Env)XMLParameter.newInstance(getXML(),Env.class,env,true);
            par.setCycleused(true);
            par.putAll(System.getProperties());
            setEnvProperty(par, getXML());
            setRuntiePar(par);
            log.info("env parameters:\n"+par.toString());
        }*/
        return par;
    }
    void setRuntiePar(Env env){
        try {
            env.addGlobalParameter("${pid}", JVMUtil.getPid());
        }catch (Exception e){

        }
        env.addGlobalParameter("${ip}", NetUtils.getip());
        Bridge b = (Bridge)getObjectById("bridge");
        env.addGlobalParameter("${local_instanceId}", b.getInstanceId());
    }

    void setEnvProperty(XMLParameter par,XMLMakeup xml){
        List<XMLMakeup> ps = xml.getChildren();
        if(null != ps && null != par){
            for(XMLMakeup p:ps){
                String  k = (String)p.getProperties().get("key");
                String v = (String)p.getText();
                if(StringUtils.isNotBlank(v) && StringUtils.isNotBlank(k)){
                    par.addGlobalParameter(k,par.getExpressValueFromMap(v,this));
                }
            }
        }
    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResultCheck checkReturn(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return new ResultCheck(true,ret);  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config,Object ret,Exception e) throws Exception {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
