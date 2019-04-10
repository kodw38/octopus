package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;

import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-10-15
 * Time: 下午4:02
 */
public class PointParseSubStrNoTag implements IPointParse {
    @Override
    public String parse(String str, Map data,XMLObject obj) {
        if(str.startsWith("substrnotag(") && str.indexOf("${")<0){
            String[] m = str.substring("substr(".length(), str.length() - 1).split(",");
            return m[0].substring(m[0].indexOf(m[1])+m[1].length(),m[0].indexOf(m[2]));
        }
        return str;
    }
}