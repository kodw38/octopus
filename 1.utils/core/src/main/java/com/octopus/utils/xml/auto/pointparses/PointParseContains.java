package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;

import java.util.Map;

/**
 * Created by Administrator on 2018/3/3.
 */
public class PointParseContains implements IPointParse {
    @Override
    public String parse(String str, Map data,XMLObject obj) {
        String s = str.substring("contains(".length(),str.length()-1);
        int n = s.lastIndexOf(",");
        if(n>0) {
            String s1 = s.substring(0, n);
            String s2 = s.substring(n + 1);
            if (s1.contains(s2)) {
                return "true";
            } else {
                return "false";
            }
        }
        return str;
    }
}
