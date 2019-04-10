package com.octopus.tools.security;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.safety.Security;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.Map;

/**
 * User: wfgao_000
 * Date: 16-4-21
 * Time: 下午3:22
 */
public class SecurityObject extends XMLDoObject {
    public SecurityObject(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(null != input){
            String data = (String)input.get("data");
            String type = (String)input.get("type");
            if(StringUtils.isNotBlank(data) && StringUtils.isNotBlank(type)){
                if("MD5".equals(type)){
                    if("encrypt".equals(input.get("op"))){
                        return Security.encryptMD5(data);
                    }
                }
                if("RC2".equals(type)){
                    if("encrypt".equals(input.get("op"))){
                        return Security.encryptRC2(data);
                    }
                }
                throw new Exception("not support "+type+" now!");
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
