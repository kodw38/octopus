package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.safety.Security;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;

/**
 * Created by Administrator on 2018/5/19.
 */
public class PointParseEncrypt implements IPointParse {
    static transient Log log = LogFactory.getLog(PointParseDecrypt.class);
    @Override
    public String parse(String str, Map data,XMLObject obj) {
        String p = str.substring("encrypt(".length(),str.length()-1);
        String p1 = p.substring(0,p.lastIndexOf(","));
        String t = p.substring(p.lastIndexOf(",")+1);
        if(StringUtils.isNotBlank(p1) && StringUtils.isNotBlank(t)){
            if("RC2".equals(t)){
                return Security.encryptRC2(p1);
            }else if("MD5".equals(t)){
                return Security.encryptMD5(p1);
            }else if("MD5_2".equals(t)){
                return Security.encryptMD5_2(p1);
            }
            log.error("not support decrypt "+t);
        }

        return str;
    }
}
