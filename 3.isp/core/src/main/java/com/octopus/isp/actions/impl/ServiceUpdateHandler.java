package com.octopus.isp.actions.impl;

import com.octopus.tools.synchro.canal.AbstractMessageHandler;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.HashMap;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-10-20
 * Time: 上午8:45
 */
public class ServiceUpdateHandler extends AbstractMessageHandler {
    public ServiceUpdateHandler(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);

    }

    @Override
    public void doRecord(XMLParameter env,String type, String tableCode, Map oldData, Map newData) throws Exception {
        tableCode=tableCode.substring(tableCode.lastIndexOf(".")+1);
        XMLDoObject parent = (XMLDoObject)getObjectById("LoadDefineActions");
        if(type.equals("INSERT")){
            try {
                HashMap input = new HashMap();
                input.put("op","add");
                input.put("data",newData);
                parent.doCheckThing(null,env,input,null,null,null);
            }finally {

            }
        }else if(type.equals("DELETE")){
            try{
                HashMap input = new HashMap();
                input.put("op","delete");
                input.put("data",newData);
                parent.doCheckThing(null,env,input,null,null,null);
            }finally {
            }
        }else if(type.equals("UPDATE")){
            try{
                HashMap input = new HashMap();
                input.put("op","update");
                input.put("olddata",oldData);
                input.put("newdata",newData);
                parent.doCheckThing(null,env,input,null,null,null);
            }finally {
            }
        }

    }

    @Override
    public Object doSomeThing(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResultCheck checkReturn(String xmlid,XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
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
