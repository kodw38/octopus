package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;

import java.util.Map;

/**
 * Created by Administrator on 2018/5/26.
 */
public class PointParseClassType implements IPointParse {
    @Override
    public String parse(String str, Map data, XMLObject obj) {
        String p = str.substring("classType(".length(),str.length()-1);
        String[] vs = p.split(",");
        if(null !=vs && vs.length==2){
            String objpath = vs[0];
            Object o = ObjectUtils.getValueByPath(data,objpath);
            if(null !=o ){
                return o.getClass().getName();
            }
        }
        return null;
    }
}
