package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;

import java.util.Collection;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-9-11
 * Time: 下午5:04
 */
public class PointParseNotNull implements IPointParse {
    @Override
    public String parse(String str, Map data,XMLObject obj) {
        try{
            String tm =str.substring("isnotnull(".length(), str.length() - 1);
            if(StringUtils.isNotBlank(tm)){
                if(tm.startsWith("${")){
                    Object o = ObjectUtils.getValueByPath(data, tm);
                    if(o==null)
                        return "false";
                    else if(o instanceof String && StringUtils.isBlank((String)o))
                        return "false";
                    if(o instanceof Collection){
                        if(((Collection)o).size()==0)
                            return "false";
                    }
                    if(o.getClass().isArray()){
                        if(((Object[])o).length==0)
                            return "false";
                    }
                }else if(tm.startsWith("@{")){
                    if(null != obj) {
                        if(obj.getObjectById(tm.substring(2, tm.length() - 1))==null)
                            return "false";
                        else
                            return "true";
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
}
