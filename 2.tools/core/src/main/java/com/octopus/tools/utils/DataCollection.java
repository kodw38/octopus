package com.octopus.tools.utils;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by robai on 2017/8/31.
 */
public class DataCollection extends XMLDoObject {
    public DataCollection(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
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
        if(null != input) {
            List<Map> datas = (List<Map>) input.get("datas");
            String parentField = (String) input.get("parentField");
            String parentTitle = (String) input.get("parentTitle");
            String childField = (String) input.get("childField");
            String childTitle = (String) input.get("childTitle");
            if(null != datas && StringUtils.isNotBlank(parentField)){
                Map ret = new LinkedHashMap();
                if(StringUtils.isNotBlank(parentField)) {
                    for (Map m : datas) {
                        Object o = m.get(parentField);
                        if(ret.containsKey(o)){
                            ((List)ret.get(o)).add(m);
                        }else{
                            ret.put(o,new LinkedList<Map>());
                            ((List)ret.get(o)).add(m);
                        }
                    }
                }
                return ret;
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
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret, Exception e) throws Exception {
        return false;
    }
}
