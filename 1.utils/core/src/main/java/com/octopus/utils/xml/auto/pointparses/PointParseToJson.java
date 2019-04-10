package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-11-21
 * Time: 下午11:13
 */
public class PointParseToJson implements IPointParse {
    transient static Log log = LogFactory.getLog(PointParseToJson.class);
    @Override
    public String parse(String str, Map data,XMLObject obj) {
        String t =str.substring("tojson(".length(), str.length() - 1);
        if(XMLParameter.isHasRetainChars(t, XMLParameter.NestTagsBegin)){
            return str;
        }
        if(t.startsWith("${")){
            if(log.isDebugEnabled()){
                log.debug(t+" "+data.get(t));
            }
            Object o = ObjectUtils.getValueByPath(data,t);
            if(null !=o){
                if(o instanceof Map){
                    if(log.isDebugEnabled()){
                        log.debug("convertMap2String "+o);
                    }

                    return ObjectUtils.convertMap2String((Map) o);

                }else {
                    return o.toString();
                }
            }
        }
        return str;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
