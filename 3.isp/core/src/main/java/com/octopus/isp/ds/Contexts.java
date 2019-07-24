package com.octopus.isp.ds;

import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.exception.ISPException;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.XMLParameter;

/**
 * User: Administrator
 * Date: 14-9-3
 * Time: 上午8:53
 */
public class Contexts extends XMLObject {
    public Contexts(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
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


    public Context getContext(RequestParameters requestData)throws ISPException {
        //todo find a context by user requestData
        XMLMakeup xs = (XMLMakeup)ArrayUtils.getFirst(getXML().getChild("context"));
        //to generator a Context
        return (Context)XMLParameter.newInstance(xs,Context.class,requestData.getRequestData(),true,this);

    }

}
