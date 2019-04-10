package com.octopus.tools.filelog;

/**
 * User: wfgao_000
 * Date: 15-11-20
 * Time: 下午10:22
 */

import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

public class LoggerMgr extends XMLDoObject{
    static transient Log log = LogFactory.getLog(LoggerMgr.class);
    static HashMap<String, SaveLogger> logmgr = new HashMap();

    public LoggerMgr(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        try{
            List logs = xml.getChildren();
            Iterator i$ = logs.iterator();
            String def = xml.getProperties().getProperty("default");
            while (true) {
                if (!(i$.hasNext())) break ;
                XMLMakeup log = (XMLMakeup)i$.next();
                if(isEnable(log)) {
                    Properties p = log.getProperties();
                    Map m = getEmptyParameter().getMapValueFromParameter(p,this);
                    String code = (String) m.get("code");
                    if (StringUtils.isBlank(code) && StringUtils.isNotBlank(def))
                        code = def;
                    addLogger(code, (String) m.get("runningPath"), (String) m.get("hisPath"), (String) m.get("fileName"), (String) m.get("suffix"), (String) m.get("pattern"), Integer.parseInt((String) m.get("remainTime")), Integer.parseInt((String) m.get("splitTime")), (String) m.get("header"), (String) m.get("encoding"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    void addLogger(String code,String runningPath,String hisPath,String fileName,String suffix,String pattern,int remainTime,int splitTime,String header,String encoding){
        SaveLogger logger = SaveLogger.getMyLogger(code,runningPath,hisPath ,fileName ,suffix ,pattern ,remainTime , splitTime,header,encoding);
        if ((null == code) || ("".equals(code)))
            code = "default";
        logmgr.put(code, logger);
        logmgr=ObjectUtils.sortMapByKeyLength(logmgr);
    }

    public static void addLog(String code, String data,String header)
    {
        SaveLogger logger = null;
        Iterator its = logmgr.keySet().iterator();
        String c = null;
        while(its.hasNext()){
            c = (String)its.next();
            if(code.indexOf(c)>=0){
                logger = (SaveLogger)logmgr.get(c);
                break;
            }
        }
        if (null != logger){
            logger.addLog(data,header);
        }else
            log.error("not find filelog code:" + code);
    }


    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(StringUtils.isNotBlank(input.get("code")) && "exist".equals(input.get("op"))){
            return logmgr.containsKey(input.get("code"));
        }else if(null !=input.get("data") && StringUtils.isNotBlank(input.get("code"))){
            Object v = input.get("data");
            Object h = input.get("header");
            Object subFileName = input.get("subFileName");
            String va = null,ha=null;
            if(v instanceof Map){
                va = ObjectUtils.convertMap2String((Map)v);
            }else{
                va = v.toString();
            }
            Map a = (Map)input.get("append");
            if(null != a){
                va = ObjectUtils.convertMap2String(a)+" "+va;
            }
            if(null != h){
                if(h instanceof Map){
                    ha = ObjectUtils.convertMap2String((Map)h);
                }else{
                    ha = h.toString();
                }
            }
            String code= (String)input.get("code");
            if(StringUtils.isNotBlank(subFileName)){
                String subcode=code+"."+subFileName;
                if(!logmgr.containsKey(subcode)){
                    List<XMLMakeup> logs = getXML().getChildren();
                    for(XMLMakeup m:logs) {
                        if(code.equals(m.getProperties().getProperty("code"))) {
                            Properties p = m.getProperties();
                            if(env==null){
                                env = getEmptyParameter();
                            }
                            Map<String,String> tm = env.getMapValueFromParameter(p,this);
                            addLogger(subcode, tm.get("runningPath"), tm.get("hisPath"), tm.get("fileName")+"_"+subFileName, tm.get("suffix"), tm.get("pattern"), Integer.parseInt(tm.get("remainTime")), Integer.parseInt(tm.get("splitTime")), tm.get("header"), tm.get("encoding"));
                            code = subcode;
                            break;
                        }
                    }
                }else{
                    code = subcode;
                }
            }
            addLog(code,va,ha);
        }
        return null;
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;  //To change body of implemented methods use File | Settings | File Templates.
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
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}