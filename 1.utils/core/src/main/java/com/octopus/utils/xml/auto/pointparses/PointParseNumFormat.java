package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;
import com.octopus.utils.xml.auto.XMLParameter;

import java.text.DecimalFormat;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 16-3-6
 * Time: 下午2:12
 */
public class PointParseNumFormat implements IPointParse {

    @Override
    public String parse(String str, Map data,XMLObject obj) {
        String t = str.substring("numformat(".length(),str.length()-1);
        if(XMLParameter.isHasRetainChars(t, XMLParameter.NestTagsBegin)){
            return str;
        }else{
            String[] ts = t.split(",");
            DecimalFormat df = new DecimalFormat(ts[1]);
            return df.format(Double.parseDouble(ts[0]));
        }

    }
}
