package com.octopus.tools.cache.impl;

import com.octopus.utils.cachebatch.FlushWorkTask;
import com.octopus.utils.xml.auto.XMLDoObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * User: wfgao_000
 * Date: 15-11-5
 * Time: 上午12:09
 */
public class AmortizeTask extends FlushWorkTask {
    transient static Log log = LogFactory.getLog(AmortizeTask.class);

    public AmortizeTask(){}
    @Override
    public void work(Map paramMap) throws Exception {
        Iterator<List> its = paramMap.values().iterator();
        List li = new ArrayList();
        while(its.hasNext()){
            Object o = its.next();
            if(Collection.class.isAssignableFrom(o.getClass()))
                li.addAll((Collection)o);
            else
                li.add(o);
            //System.out.println("AmortizeTask:"+((Map)li.get(li.size()-1)).get("s_code"));
        }
        Map map = new HashMap();
        map.put("op","add");
        map.put("datas",li);
        if(li.size()>0){
            log.debug("batch save size:"+li.size()+"\n about:\n"+li.get(0));
            ((XMLDoObject)getOther()).doSomeThing(null, null, map, null, null);
        }
    }
}
