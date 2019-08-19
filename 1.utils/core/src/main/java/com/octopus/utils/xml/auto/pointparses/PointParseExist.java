package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.exception.ISPException;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-6-8
 * Time: 下午4:50
 */
public class PointParseExist implements IPointParse {
    static transient Log log = LogFactory.getLog(PointParseExist.class);
    @Override
    public String parse(String str, Map data,XMLObject obj) {
        try{
        String tm =str.substring("exist(".length(), str.length() - 1);
        String[] ts = tm.split(",");
        if(null!=ts && (ts.length==2)){
            if(ts[0].contains("${")){

                Object o = ObjectUtils.getValueByPath(data,ts[0]);

                if(log.isDebugEnabled()){
                    log.debug("get exist data by key["+ts[0]+"] ,data ["+o+"]");
                    if(data instanceof Map){
                        Iterator its = ((Map)data).keySet().iterator();
                        while(its.hasNext()){
                            log.debug("get exist data key "+its.next() +" this thread:"+Thread.currentThread().getName());
                        }
                    }
                }
                if(null != o){
                    if(o.getClass().isArray()){
                        Object[] os = (Object[])o;
                        for(Object o1:os){
                            if(compare(o1,ts[1])){
                                return "true";
                            }
                        }
                        return "false";
                    }else if(o instanceof Collection){
                        Iterator its = ((Collection)o).iterator();
                        while(its.hasNext()){
                            if(compare(its.next(),ts[1])) return "true";
                        }
                        return "false";
                    }else if(o instanceof Map && XMLParameter.isHasRetainChars(ts[1])){//判断Map中的元素是否满足,参数二的表达式
                        if(isMapElementExist((Map)o,ts[1],data,obj)){
                            return "true";
                        }else{
                            return "false";
                        }
                    }
                }else{
                    return str;
                }
            }else{
                return String.valueOf(ts[0].contains(ts[1]));
            }
        }
        return "false";
        }catch (Exception e){
            log.error(str,e);
            e.printStackTrace();
            return "false";
        }
    }

    boolean compare(Object o,String t)throws Exception{
        if(o instanceof String) return o.equals(t);
        throw new Exception(this.getClass().getName()+" now not support compare type:"+o.getClass());
    }
    //判断Map中的元素是否满足,参数二的表达式
    boolean isMapElementExist(Map m,String exp,Map data,XMLObject obj)throws ISPException {
        if(null != m){
            Iterator its = m.keySet().iterator();
            while(its.hasNext()){
                Object key = its.next();
                Object r = m.get(key);
                data.put("${each}",r);
                data.put("${each_key}",key);
                String t = StringUtils.replace(exp, "~", "");
                log.debug("exist expression:"+t);
                Object ov = ObjectUtils.getExpressValueFromMap(t, XMLParameter.NestTagsBegin,XMLParameter.NestTagsEnd,data,XMLParameter.PointParses,obj);
                data.remove("${each}");
                data.remove("${each_key}");
                if(null != ov && StringUtils.isTrue(ov.toString())){
                    return true;
                }

            }
        }
        return false;
    }
}
