package com.octopus.isp.bridge.launchers.convert;

import com.octopus.isp.bridge.launchers.IConvert;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.alone.impl.MappingInfo;
import com.octopus.utils.cls.POJOUtil;
import com.octopus.utils.exception.ExceptionUtil;
import com.octopus.utils.exception.ISPException;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * User: wfgao_000
 * Date: 15-8-27
 * Time: 下午2:35
 */
public class ConvertObject2JSONString extends XMLDoObject implements IConvert {
    static transient Log log = LogFactory.getLog(ConvertObject2JSONString.class);
    static Map<Class,Map> exceptionMsg = new HashMap();
    public ConvertObject2JSONString(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);

    }

    @Override
    public Object convert(Object par) throws IOException {
        if(null != par){
            List ids = new ArrayList();
            String res =null;
            if(par instanceof JSONArray){
                res= ((JSONArray)par).toString();
            }else if(par instanceof JSONObject){
                res= ((JSONObject)par).toString();
            }else if(par.getClass().isArray()){
                res= JSONArray.fromObject(par).toString();
            }else if(par instanceof Collection){
                StringBuffer ret = new StringBuffer();
                ObjectUtils.appendObject2StringBuffer(ret,null,par);
                res= ret.toString();
                //return JSONArray.fromObject(par).toString();
            }else if(par instanceof Exception){
                log.error("",(Exception)par);
                //log.error("return exception:",(Exception)par);
                Throwable r = ExceptionUtil.getRootCase((Exception)par);
                if(null != r) {
                    res = getException(r);
                    if(null == res) {
                        String code, msg;
                        if (r instanceof ISPException) {
                            code = ((ISPException) r).getCode();
                            msg = ((ISPException) r).getRealMsg();
                            if (null == msg) {
                                msg = ((ISPException) r).getMsg();
                            }
                        } else {
                            code = "B-000";
                            msg = r.getMessage();
                            msg = StringUtils.replace(msg, "\n", "<br/>");
                            msg = StringUtils.replace(msg, "\r", "");
                            msg = StringUtils.replace(msg, "\t", "");
                        }
                        res = getErrorMsg(code, msg);
                        if (StringUtils.isBlank(res)) {
                            res = "{\"is_error\":\"true\",\"errorcode\":\"" + code + "\",\"msg\":\"" + msg + "\"}";
                        }
                    }
                }
            }else if(Map.class.isAssignableFrom(par.getClass())){
                if((StringUtils.isTrue((String)((Map)par).get("is_error")) && ((Map)par).containsKey("errorcode") && ((Map)par).containsKey("msg"))){
                    res = getErrorMsg((String)((Map)par).get("errorcode"),(String)((Map)par).get("msg"));
                    if(StringUtils.isBlank(res)){
                        res = ObjectUtils.convertMap2String((Map) par);
                    }
                }else {
                    if(par instanceof com.alibaba.fastjson.JSONObject){
                        res = ((com.alibaba.fastjson.JSONObject)par).toJSONString();
                    }else if(par instanceof JSONObject){
                        res = ((JSONObject)par).toString();
                    }else {
                        res = ObjectUtils.convertMap2String((Map) par);
                    }
                }
            }else if(List.class.isAssignableFrom(par.getClass())){
                StringBuffer sb = new StringBuffer();
                ObjectUtils.appendObject2StringBuffer(sb,null,par);
                res= sb.toString();
            }
            else if(!POJOUtil.isPrimitive(par.getClass().getName())){
                //res= JSONObject.fromObject(par).toString();
                AtomicLong size = new AtomicLong();
                try {
                    Map m = POJOUtil.convertPojo2Map(par, size);
                    res= ObjectUtils.convertMap2String(m);
                }catch (Exception e){

                }
            }else{
                res= par.toString();
                if(res.contains("is_error") && res.contains("errorcode") && res.startsWith("{")){
                    Map m= StringUtils.convert2MapJSONObject(res);
                    String re = getErrorMsg((String)m.get("errorcode"),(String)m.get("msg"));
                    if(StringUtils.isNotBlank(re)){
                        res = re;
                    }
                }
            }
            if(log.isDebugEnabled()){
                log.debug(res);
            }
            return res;
        }
        return null;
    }

    String getException(Throwable e){
        if(null != exceptionMsg) {
            Iterator<Class> its = exceptionMsg.keySet().iterator();
            while(its.hasNext()) {
                Class c = its.next();
                if(c.isAssignableFrom(e.getClass())){
                    Map map = exceptionMsg.get(c);
                    if (null != map) {
                        Map m = ObjectUtils.getObjectMapping2Map(e, map);
                        ObjectUtils.appendDeepMapNotReplaceKey(map,m);
                        if (null != m) {
                            return ObjectUtils.convertMap2String(m);
                        }
                    }
                }

            }
        }
        return null;
    }
    String getErrorMsg(String code,String msg){
        if(StringUtils.isNotBlank(code)){
            XMLMakeup[] xs = getXML().getChild("errors");
            if(null != xs && xs.length>0) {
                XMLMakeup[] es = xs[0].getChild("error");
                if(null != es && es.length>0) {
                    for(XMLMakeup e:es) {
                        if(null != e && code.equals(e.getProperties().getProperty("code")) &&
                                (null == e.getProperties().getProperty("isenable") || StringUtils.isTrue(e.getProperties().getProperty("isenable")) )
                                ) {
                            HashMap map = new HashMap();
                            map.put("${code}", code);
                            map.put("${msg}", msg);
                            String v = e.getProperties().getProperty("output");
                            if(StringUtils.isNotBlank(v)) {
                                Object o = XMLParameter.getExpressValueFromMap(v, map, null);
                                return o.toString();
                            }

                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Object doSomeThing(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(env.getResult() instanceof ResultCheck){
            return convert(((ResultCheck)env.getResult()).getRet());
        }else{
            return convert(env.getResult());
        }
    }

    @Override
    public void doInitial() throws Exception {
        XMLMakeup[] xs = getXML().getChild("exceptions");
        if(null != xs && xs.length>0) {
            XMLMakeup[] es = xs[0].getChild("exception");
            if(null != es && es.length>0) {
                for(XMLMakeup x:es){
                    if(x.getProperties().containsKey("classname") && x.getProperties().containsKey("mapping") && !(x.getProperties().containsKey("isenable") && !StringUtils.isTrue(x.getProperties().getProperty("isenable")))){
                        try {
                            exceptionMsg.put(Class.forName(x.getProperties().getProperty("classname")), StringUtils.convert2MapJSONObject(x.getProperties().getProperty("mapping")));
                        }catch (Exception e){
                            log.error(e);
                        }
                    }
                }

            }
        }
    }

    @Override
    public boolean checkInput(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;
    }

    @Override
    public ResultCheck checkReturn(String xmlid,XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
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
