package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;

import java.util.List;
import java.util.Map;

/**
 * Created by admin on 2020/1/20.
 */
public class PointParseEndWith implements IPointParse {
    @Override
    public String parse(String str, Map data, XMLObject obj) {
        if(str.startsWith("endwith(")) {
            String t = str.substring("endwith(".length(), str.length() - 1);
            String s = t.substring(0,t.indexOf(","));
            String m = t.substring(t.indexOf(",")+1);
            if(s.endsWith(m)){
                return "true";
            }else{
                return "false";
            }
        }
        return str;
    }
}
