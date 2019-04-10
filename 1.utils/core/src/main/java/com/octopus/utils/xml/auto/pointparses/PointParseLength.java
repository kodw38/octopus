package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collection;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-11-4
 * Time: 上午8:57
 */
public class PointParseLength implements IPointParse {
    transient static Log log = LogFactory.getLog(PointParseLength.class);
    @Override
    public String parse(String str, Map data,XMLObject obj) {
        String t = str.substring("len(".length(),str.length()-1);
        if(XMLParameter.isHasRetainChars(t, XMLParameter.NestTagsBegin)){
            return str;
        }
        Object o = ObjectUtils.getValueByPath(data,t);
        if(null != o){
            if(o.getClass().isArray()){
                return String.valueOf(((Object[])o).length);
            }else if(o instanceof Collection)
                return String.valueOf(((Collection) o).size());
            else if(o instanceof Map)
                return String.valueOf(((Map)o).size());
            else if(o instanceof String)
                return String.valueOf(((String)o).length());
        }else if(!XMLParameter.isHasRetainChars(t)){
            if(t instanceof String)
                return String.valueOf(((String)t).length());
        }
        return "0";
    }
}
