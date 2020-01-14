package com.octopus.isp.actions;

import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-9-26
 * Time: 下午8:59
 */
public class SetValue extends XMLDoObject {
    public SetValue(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    @Override
    public Object doSomeThing(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        Object o = input.get("obj");
        if(null == o || o instanceof String){
            log.error("set value to obj fault, obj is not Object "+o);
            return null;
        }else {
            String type=(String)input.get("type");
            synchronized (o) {
                String p = (String) input.get("path");
                Object v = input.get("value");
                if (StringUtils.isNotBlank(p)) {
                    if(StringUtils.isNotBlank(type) && "remove".equals(type) && StringUtils.isNotBlank(p) && null!=o && o instanceof Map){
                        return ((Map)o).remove(p);
                    }else if("addList".equals(type) && null != o && o instanceof Map){
                        if(!((Map)o).containsKey(p)){
                            ((Map)o).put(p,new ArrayList());
                        }
                        ((List)((Map)o).get(p)).add(v);
                    }else {
                        return ObjectUtils.setValueByPath(o, p, v);
                    }
                } else if (o instanceof Collection) {
                    if(StringUtils.isNotBlank(type) && "different".equals(type)){
                        if(!((List)o).contains(v) ){
                            return ((List)o).add(v);
                        }
                    }else if(StringUtils.isNotBlank(type) && "addAll".equals(type) && v instanceof Collection){
                        ((Collection)o).addAll((Collection)v);
                    }else {
                        return ((Collection) o).add(v);
                    }
                }else if(null != o && o instanceof Map  && null!=v && v instanceof Map ){
                    if(StringUtils.isNotBlank(type) && "append".equals(type)){
                        ((Map) v).putAll((Map) o);
                        return v;
                    }else {
                        ((Map) o).putAll((Map) v);
                        return o;
                    }
                }else if(null ==o && null!=v && v instanceof Map){
                    return v;
                }
                return null;
            }
        }
    }

    @Override
    public void doInitial() throws Exception {

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
        return true;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config,Object ret,Exception e) throws Exception {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
