package com.octopus.isp.bridge.launchers.convert;

import com.octopus.isp.bridge.launchers.IConvert;
import com.octopus.utils.cls.POJOUtil;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.XMLParameter;

/**
 * User: wfgao_000
 * Date: 15-8-26
 * Time: 下午4:28
 */
public class ConvertPOJO2Map extends XMLObject implements IConvert {
    public ConvertPOJO2Map(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    @Override
    public void notifyObject(String op, Object obj) throws Exception {

    }

    @Override
    public void addTrigger(Object cond, InvokeTaskByObjName task) throws Exception {

    }

    @Override
    public void destroy() throws Exception {

    }

    @Override
    public void initial() throws Exception {

    }

    @Override
    public Object convert(XMLParameter env,Object par) throws Exception {
        if(null == par)return null;
        if(!POJOUtil.isSimpleType(par.getClass().getName()))
            return POJOUtil.convertPojo2Map(par,null);
        else
            return par;
    }
}
