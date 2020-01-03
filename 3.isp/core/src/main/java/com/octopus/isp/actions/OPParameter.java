package com.octopus.isp.actions;

import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

/**
 * User: wfgao_000
 * Date: 16-2-3
 * Time: 下午5:40
 */
public class OPParameter extends XMLDoObject {
    public OPParameter(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(null != input){
            String op = (String)input.get("op");
            if(StringUtils.isNotBlank(op)){
                if(op.equals("add") && StringUtils.isNotBlank((String)input.get("key")) && null != input.get("value")){
                    env.addParameter("${"+(String)input.get("key")+"}",input.get("value"));
                }else if(op.equals("set") && StringUtils.isNotBlank((String)input.get("key")) && null != input.get("value")){
                    env.addParameter((String)input.get("key"),input.get("value"));
                }else if(op.equals("setGlobal") && StringUtils.isNotBlank((String)input.get("key")) && null != input.get("value")){
                    env.addGlobalParameter((String)input.get("key"),input.get("value"));
                }else if("clearAll".equals(op)){
                    env.getReadOnlyParameter().clear();
                }
            }
        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
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
