package com.octopus.isp.ds;

import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 2018/2/28.
 */
public class Constant extends XMLObject {
    static transient Log log = LogFactory.getLog(Constant.class);
    Map ret;
    public Constant(XMLMakeup xml, XMLObject parent, Object[] containers) throws Exception {
        super(xml, parent, containers);
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

    public Map getConstant(){
        if(null != ret){
            return ret;
        }else {
            try {
                XMLMakeup xs = getXML().getFirstChildrenEndWithName("constants");
                if (null != xs) {
                    ret = xs.toMap();
                    return ret;
                } else {
                    return null;
                }
            }catch(Exception e){
                log.error("",e);
            }
        }
        return null;
    }
}
