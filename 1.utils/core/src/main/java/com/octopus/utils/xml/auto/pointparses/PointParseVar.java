package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;

/**
 * Created by robai on 2018/1/25.
 */
public class PointParseVar implements IPointParse{
    transient static Log log = LogFactory.getLog(PointParseRange.class);
    @Override
    public String parse(String str, Map data,XMLObject obj) {
        try {
            String tm = str.substring("getvar(".length(), str.length() - 1);
            String ret = ObjectUtils.getStringValueByPath(data, tm);
            if (null != ret) {
                return ret;
            } else {
                return str;
            }
        }catch (Exception e){
            log.error("PointParseVar",e);
        }
        return str;
    }
}
