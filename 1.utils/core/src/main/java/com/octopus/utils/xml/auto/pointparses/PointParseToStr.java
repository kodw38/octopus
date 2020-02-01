package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.exception.ISPException;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;

import java.util.Map;

/**
 * Created by admin on 2020/1/13.
 */
public class PointParseToStr implements IPointParse {
    @Override
    public String parse(String str, Map data, XMLObject obj) throws ISPException {
        String t =str.substring("tostr(".length(), str.length() - 1);
        Object o = ObjectUtils.getValue(data,t,false);
        if(null != o ){
            return o.toString();
        }
        return str;
    }
}
