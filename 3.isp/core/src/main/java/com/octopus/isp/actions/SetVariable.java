package com.octopus.isp.actions;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import com.octopus.utils.xml.desc.Desc;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-9-28
 * Time: 下午1:17
 */
public class SetVariable extends XMLDoObject {
    public SetVariable(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    @Override
    public Object doSomeThing(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(null != input) {
            String op = (String)input.get("op");
            if(StringUtils.isNotBlank(op)){
                if("newList".equals(op)){
                    return new LinkedList();
                }
                if("newMap".equals(op)){
                    return new LinkedHashMap();
                }
            }
            Object r = input.get("value");
            Object o = input.get("data");
            if(null != r && r instanceof String && null != o && o instanceof Map){
                return XMLParameter.getExpressValueFromMap((String)r,(Map)o,this);
            }
            if(null != r && r instanceof String && XMLParameter.isHasRetainChars((String)r) ){
                if(((String) r).startsWith("{")){
                    Map m = StringUtils.convert2MapJSONObject((String)r);
                    if(Desc.isDescriptionService(m)){
                        return r;
                    }
                }

                return r;
            }else{
                return r;
            }
        }
        return null;
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
