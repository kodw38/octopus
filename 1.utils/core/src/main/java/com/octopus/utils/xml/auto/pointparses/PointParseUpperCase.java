package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.exception.ISPException;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;

import java.util.Map;

public class PointParseUpperCase implements IPointParse {
    @Override
    public String parse(String str, Map data, XMLObject obj) throws ISPException {
        String tm =str.substring("upper(".length(), str.length() - 1);
        if(StringUtils.isNotBlank(tm)){
            return tm.toUpperCase();
        }
        return null;
    }
}
