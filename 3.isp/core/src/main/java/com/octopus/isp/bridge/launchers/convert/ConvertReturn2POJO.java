package com.octopus.isp.bridge.launchers.convert;

import com.octopus.isp.actions.ISPDictionary;
import com.octopus.isp.bridge.launchers.IConvert;
import com.octopus.utils.cls.POJOUtil;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.*;

/**
 * User: Administrator
 * Date: 14-9-29
 * Time: 下午7:51
 */
public class ConvertReturn2POJO extends XMLObject implements IConvert{


    public ConvertReturn2POJO(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
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
        ISPDictionary dictionary = (ISPDictionary)getObjectById("Dictionary");
        if(null != par && null != dictionary){
            if( par instanceof Collection){
                List ls = new LinkedList();
                Iterator its = ((Collection)par).iterator();
                while(its.hasNext()){
                    ls.add(convert(env,its.next()));
                }
                return ls.toArray();
            }else if(par instanceof Map){

                return POJOUtil.convertMap2POJO((Map)par,dictionary.getClassByFields(((Map) par).keySet()) ,null);
            }else {
                return par;
            }
        }else{
            return null;
        }

    }
}
