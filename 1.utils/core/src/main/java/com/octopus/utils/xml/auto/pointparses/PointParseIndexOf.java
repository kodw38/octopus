package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.Map;

/**
 * User: wfgao_000
 * Date: 16-1-28
 * Time: 上午9:08
 */
public class PointParseIndexOf implements IPointParse {
    @Override
    public String parse(String str, Map data,XMLObject obj) {
        if(str.startsWith("indexof(") && str.indexOf("${")<0){
            String t = str.substring("indexof(".length(), str.length() - 1);
            if(XMLParameter.isHasRetainChars(t)){
                return str;
            }
            String[] m = t.split(",");
            if(m.length==3){
                String sp = StringUtils.toHtmlInput(m[1]);
                if(m[2].equals("-1")) {
                    return String.valueOf(m[0].lastIndexOf(sp));

                }else if("0".equals(m[2])){
                     return String.valueOf(StringUtils.indexOf(m[0],sp));
                }else {
                    return String.valueOf(StringUtils.ordinalIndexOf(m[0],sp,Integer.parseInt(m[2])));
                }
            }
        }
        return str;
    }
}
