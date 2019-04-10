package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;
import sun.misc.BASE64Encoder;

import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-6-8
 * Time: 下午4:33
 */
public class PointParseBase64 implements IPointParse {
    @Override
    public String parse(String str, Map data,XMLObject obj) {
        if(!str.contains("${")){
            String tm =str.substring("base64_encode(".length(), str.length() - 1);
            return org.apache.commons.lang.StringUtils.deleteSpaces(new BASE64Encoder().encode(tm.getBytes()));
        }
        return str;

    }
}
