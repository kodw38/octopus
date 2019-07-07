package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Administrator on 2019/7/4.
 */
public class PointParseNotNullPath implements IPointParse {
    @Override
    public String parse(String str, Map data,XMLObject obj) {
        try{
            String tm =str.substring("isnotnullbypath(".length(), str.length() - 1);
            if(StringUtils.isNotBlank(tm)){
                if(tm.startsWith("${")){
                    Object o = ObjectUtils.getValueWithArrayNullByPath(data, tm);
                    if(o==null)
                        return "false";
                    else if(o instanceof String && StringUtils.isBlank((String)o))
                        return "false";
                    if(o instanceof Collection){
                        if(((Collection)o).size()==0)
                            return "false";
                        boolean b = false;
                        Iterator its = ((Collection)o).iterator();
                        while(its.hasNext()){
                            Object n = its.next();
                            b= getEndNotNull(n);
                            if(!b){
                                return "false";
                            }
                        }
                    }
                    if(o.getClass().isArray()){
                        if(((Object[])o).length==0)
                            return "false";
                        boolean b = false;
                        for(Object n:(Object[])o){
                            b= getEndNotNull(n);
                            if(!b){
                                return "false";
                            }
                        }
                    }
                }
                if(StringUtils.isBlank(tm))
                    return "false";
                return "true";
            }
            return "false";
        }catch (Exception e){
            e.printStackTrace();
            return "false";
        }
    }

    boolean getEndNotNull(Object o){
        if(null == o || (o instanceof String && "".equals(o))) return false;
        if(o  instanceof Collection){
            Iterator its = ((Collection)o).iterator();
            while(its.hasNext()){
                Object n = its.next();
                if(null == n || (n instanceof String && "".equals(n)))return false;
                if(n instanceof Collection || n.getClass().isArray()){
                    boolean b =getEndNotNull(n);
                    if(!b){
                        return false;
                    }
                }
            }
        }
        if(o.getClass().isArray()){
            for(Object n:(Object[])o){
                if(null == n || (n instanceof String && "".equals(n)))return false;
                if(n instanceof Collection || n.getClass().isArray()){
                    boolean b= getEndNotNull(n);
                    if(!b){
                        return false;
                    }
                }
            }
        }
        return true;
    }
}