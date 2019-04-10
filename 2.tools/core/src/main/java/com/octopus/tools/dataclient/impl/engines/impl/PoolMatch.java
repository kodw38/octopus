package com.octopus.tools.dataclient.impl.engines.impl;

import com.octopus.tools.dataclient.impl.engines.DC;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cls.ClassUtils;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * User: Administrator
 * Date: 14-9-24
 * Time: 下午3:33
 */
public class PoolMatch extends XMLObject {
    HashMap evns = new HashMap();
    HashMap dcs = new HashMap();
    public PoolMatch(XMLMakeup xml,XMLObject parent,Object[] containers) throws Exception {
        super(xml,parent,containers);
        List<XMLMakeup> ls = getXML().getChildren();
        String p,v;
        for(XMLMakeup l:ls){
            if(l.getName().equals("evn")){
                p = l.getProperties().getProperty("parameter");
                v = l.getProperties().getProperty("value");
                if(StringUtils.isNotBlank(v) && StringUtils.isNotBlank(p)){
                    evns.put(p,v);
                }
            }
            if(l.getName().equals("dc")){
                p = l.getProperties().getProperty("parameter");
                v = l.getProperties().getProperty("value");
                if(StringUtils.isNotBlank(v) && StringUtils.isNotBlank(p)){
                    dcs.put(p,v);
                }
            }
        }
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

    public boolean isMatch(DC dc,Object env){
        if(dcs.size()>0){
            Iterator ks = dcs.keySet().iterator();
            while(ks.hasNext()){
                try{
                    Object k= ks.next();
                    String v = (String)dcs.get(k);
                    if(v.startsWith("$")){
                        v = getPropertyByReferPath(v.substring(1));
                    }
                    if(!dc.get(k).equals(v)){
                        return false;
                    }
                }catch (Exception e){
                    return false;
                }
            }
        }
        if(evns.size()>0){
            Iterator ks = evns.keySet().iterator();
            while(ks.hasNext()){
                try{
                    String k= (String)ks.next();
                    String v = (String)evns.get(k);
                    if(v.startsWith("$")){
                        v = getPropertyByReferPath(v.substring(1));
                    }
                    String envV= (String)ClassUtils.getFieldValue(env,k,false);
                    if(!envV.equals(v)){
                        return false;
                    }
                }catch (Exception e){
                    return false;
                }
            }
        }
        return true;
    }
}
