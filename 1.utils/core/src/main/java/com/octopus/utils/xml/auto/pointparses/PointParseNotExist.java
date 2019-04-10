package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-6-8
 * Time: 下午6:38
 */
public class PointParseNotExist implements IPointParse {
    static transient Log log = LogFactory.getLog(PointParseNotExist.class);
    @Override
    public String parse(String str, Map data,XMLObject obj) {
        try{

            String tm =str.substring("notexist(".length(), str.length() - 1);
            String[] ts = tm.split(",");
            if(null!=ts && (ts.length==2)){
                if(ts[0].contains("${")){
                    Object o = ObjectUtils.getValueByPath(data,ts[0]);
                    if(null != o){
                        if(o.getClass().isArray()){
                            Object[] os = (Object[])o;
                            for(Object o1:os){
                                if(!compare(o1,ts[1])){
                                    return "true";
                                }
                            }
                            return "false";
                        }else if(o instanceof Collection){
                            Iterator its = ((Collection)o).iterator();
                            while(its.hasNext()){
                                if(compare(its.next(),ts[1])) {
                                    return "false";
                                }
                            }
                            return "true";
                        }else if(o instanceof Map){
                            if(((Map)o).containsKey(ts[1]))
                                return "false";
                            else
                                return "true";
                        }
                    }else{
                        return str;
                    }
                }else{
                    return String.valueOf(ts[0].contains(ts[1]));
                }
            }else if(null !=ts && ts.length==3){
                if(ts[0].contains("${")){
                    Object o = ObjectUtils.getValueByPath(data,ts[0]);
                    if(null != o){
                        if(o.getClass().isArray()){
                            Object[] os = (Object[])o;
                            for(Object o1:os){
                                if(!compare(o1,ts[1],ts[2])){
                                    return "true";
                                }
                            }
                            return "false";
                        }else if(o instanceof Collection){
                            Iterator its = ((Collection)o).iterator();
                            while(its.hasNext()){
                                if(compare(its.next(),ts[1],ts[2])) {
                                    return "false";
                                }
                            }
                            return "true";
                        }else if(o instanceof Map){
                            if(((Map)o).containsKey(ts[1])) {
                                if(((Map)o).get(ts[1]).equals(ts[2]))
                                   return "false";
                                else
                                    return "true";
                            }else
                                return "true";
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
                log.error(e);
                return "false";
        }


    }
    boolean compare(Object o,String t)throws Exception{
        if(o instanceof String) return o.equals(t);
        throw new Exception(this.getClass().getName()+" now not support compare type:"+o.getClass());
    }
    boolean compare(Object o,String k,Object v)throws Exception{
        if(o instanceof Map){
        if(null != ((Map)o).get(k) && ((Map)o).get(k).equals(v)){
            return true;
        }
        return false;
        }else {
            throw new Exception("not support compare "+o.getClass().getName()) ;
        }
    }
}
