package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.safety.Security;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;

/**
 * Created by Administrator on 2018/3/1.
 */
public class PointParseDecrypt implements IPointParse {
    static transient Log log = LogFactory.getLog(PointParseDecrypt.class);
    @Override
    public String parse(String str, Map data,XMLObject obj) {
        String p = str.substring("decrypt(".length(),str.length()-1);
        String p1 = p.substring(0,p.lastIndexOf(","));
        String t = p.substring(p.lastIndexOf(",")+1);
        if(StringUtils.isNotBlank(p1) && StringUtils.isNotBlank(t)){
            if("RC2".equals(t)){
                return Security.decryptRC2(p1);
            }
            log.error("not support decrypt "+t);
        }

        return str;
    }
}
