package com.octopus.isp.cell;

import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.Map;

/**
 * User: Administrator
 * Date: 14-9-4
 * Time: 下午3:49
 */
public class RuleAction extends XMLDoObject {
    public RuleAction(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    @Override
    public Object doSomeThing(String xmlid,XMLParameter env,Map input,Map output,Map cfg) {
        //To change body of implemented methods use File | Settings | File Templates.
        return null;
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid,XMLParameter env, Map input,Map output, Map cfg) throws Exception {
        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResultCheck checkReturn(String xmlid,XMLParameter env, Map input, Map output,Map cfg, Object ret) throws Exception {
        if(null != ret && ret instanceof ResultCheck){
            return (ResultCheck)ret;
        }else{
            return new ResultCheck(true,ret);  //To change body of implemented methods use File | Settings | File Templates.
        }
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config,Object ret,Exception e) throws Exception {
        throw new Exception("now support rollback");
    }
}
