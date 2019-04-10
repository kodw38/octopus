package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Administrator on 2018/4/27.
 */
public class PointParseTimes implements IPointParse {
    transient static Log log = LogFactory.getLog(PointParseTimes.class);
    static Map<String,AtomicInteger> cache = new HashMap<String, AtomicInteger>();
    @Override
    public String parse(String str, Map data,XMLObject obj) {
        try{
            if(StringUtils.isNotBlank(str)) {
                String tm = str.substring("times(".length(), str.length()-1);
                log.debug("times "+tm);
                int n = tm.indexOf(",");
                String key = tm.substring(0,n);
                int p2 = tm.indexOf(",",n+1);
                String num = tm.substring(n+1,p2);
                String exp = tm.substring(p2+1);
                Object o = XMLParameter.getExpressValueFromMap(exp,data,obj);
                if(null !=o && StringUtils.isTrue((String)o)){
                    if(cache.containsKey(key)){
                        int t = cache.get(key).addAndGet(1);
                        log.debug("times "+key+" current "+t+" limit "+num);
                        if(t>Integer.parseInt(num)){
                            cache.get(key).set(0);
                            return "true";
                        }
                    }else{
                        cache.put(key,new AtomicInteger(1));
                    }
                }else{
                    if(cache.containsKey(key)){
                        cache.get(key).set(0);
                    }
                }
            }
            return "false";
        }catch (Exception e){
            log.error(str,e);
            e.printStackTrace();
            return "false";
        }
    }
}
