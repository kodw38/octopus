package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;

import java.util.List;
import java.util.Map;

/**
 * Created by robai on 2017/9/15.
 */
public class PointParseStartwith implements IPointParse {
    @Override
    public String parse(String str, Map data,XMLObject obj) {
        if(str.startsWith("startwith(") && str.indexOf("${")<0) {
            String t = str.substring("startwith(".length(), str.length() - 1);
            String s = t.substring(0,t.indexOf(","));
            String m = t.substring(t.indexOf(",")+1);
            List<String> ls = StringUtils.convert2ListJSONObject(m);
            for(String a:ls){
                if(s.startsWith(a))
                    return "true";
            }
            return "false";
        }
        return str;
    }
}
