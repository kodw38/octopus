package com.octopus.isp.actions;

import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.lang.StringUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 16-4-18
 * Time: 下午3:19
 */
public class MapToString extends XMLDoObject {
    public MapToString(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    String chgSplitChar(String s){
       if(s.equals("\\r")){
           return "\r";
       }else if(s.equals("\\t")){
           return "\t";
        }else{
           return s;
       }
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(null != input ){
            Map m = (Map)input.get("data");
            String type= (String)input.get("type");
            String s = (String)config.get("startquo");
            String e = (String)config.get("endquo");
            Map typequo = (Map)config.get("typequo");
            List<String> fields = (List)input.get("fields");
            Map appendData = (Map)input.get("appendData");
            if(null != m){
                String splitChar ="";
                if(null != config && StringUtils.isNotBlank((String)config.get("splitchar"))){
                    splitChar = chgSplitChar((String)config.get("splitchar"));

                }
                StringBuffer sb = new StringBuffer();
                if("json".equalsIgnoreCase(type))
                    sb.append("{");
                boolean is=true;
                if(null != fields && fields.size()>0){
                    for(String k:fields){
                        if(!is){
                            sb.append(splitChar).append(getQuo(type,s,e,k,m,appendData,(String)m.get(k+"#Type"),typequo));
                        }else{
                            sb.append(getQuo(type,s,e,k,m,appendData,(String)m.get(k+"#Type"),typequo));
                            is=false;
                        }
                    }
                }else{
                    Iterator it = m.keySet().iterator();
                    while(it.hasNext()){
                        String k = (String)it.next();
                        if(k.endsWith("#Type")) continue;
                        if(sb.length()!=0){
                            sb.append(splitChar).append(getQuo(type,s,e,k,m,appendData,(String)m.get(k+"#Type"),typequo));
                        }else{
                            sb.append(getQuo(type,s,e,k,m,appendData,(String)m.get(k+"#Type"),typequo));
                        }
                    }
                }
                if("json".equalsIgnoreCase(type))
                    sb.append("}");

                return sb.toString();
            }
        }
        return null;
    }
    String getQuo(String type,String squo,String equo,String key,Map data,Map appendData, String column_type,Map typequo){
        if("keys".equals(type)){
            return key;
        }
        Object value=null;
        if(null != appendData && appendData.containsKey(key))
             value = appendData.get(key);
        else
            value=data.get(key);
        if(null == value) value="";
        if(null != typequo && null != typequo.get(column_type)){
            List<String> s = (List)typequo.get(column_type);
            if("json".equalsIgnoreCase(type)){
                return key.concat(":").concat(s.get(0).concat(value.toString()).concat(s.get(1)));
            }else
            return s.get(0).concat(value.toString()).concat(s.get(1));
        }else if(StringUtils.isNotBlank(squo) && StringUtils.isNotBlank(equo)){
            if("json".equalsIgnoreCase(type)){
                return key.concat(":").concat(squo.concat(value.toString()).concat(equo));
            }else
            return   squo.concat(value.toString()).concat(equo);
        } else {
            if("json".equalsIgnoreCase(type)){
                return key.concat(":").concat(value.toString());
            }else
            return   value.toString();
        }
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
