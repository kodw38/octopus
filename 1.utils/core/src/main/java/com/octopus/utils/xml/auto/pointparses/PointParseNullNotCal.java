package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2018/7/3.
 */
public class PointParseNullNotCal implements IPointParse {
    @Override
    public String parse(String str, Map data,XMLObject obj) {
        try{
            String tm =str.substring("isnull(".length(), str.length() - 1);
            if(StringUtils.isNotBlank(tm)){
                if(tm.startsWith("${")||tm.startsWith("^${")){
                    Object o = ObjectUtils.getValueByPath(data, tm);
                    if(o==null)
                        return "true";
                    else if(o instanceof String && StringUtils.isBlank((String)o))
                        return "true";
                    if(o instanceof List){
                        if(((List)o).size()==0) return "true";
                        int i=0;
                        for(;i<((List)o).size();i++){
                            if(null !=((List)o).get(i)) break;
                        }
                        if(i==((List)o).size()){
                            return "true";
                        }
                    }else if(o instanceof Map){
                        if(((Map)o).size()==0) return "true";
                    }
                    return "false";
                }else if(XMLParameter.isHasRetainChars(tm)){
                    return "true";
                }else{
                    return "false";
                }
            }

            return "false";
        }catch (Exception e){
            e.printStackTrace();
            return "false";
        }
    }
}
