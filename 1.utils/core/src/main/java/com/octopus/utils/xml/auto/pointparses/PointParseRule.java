package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.rule.RuleUtil;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;

/**
 * User: wfgao_000
 * Date: 16-8-8
 * Time: 上午10:55
 */
public class PointParseRule implements IPointParse {
    transient static Log log = LogFactory.getLog(PointParseRule.class);
    @Override
    public String parse(String str, Map data,XMLObject obj) {
        try{
            String tm =str.substring("#(".length(), str.length() - 1);
            Object o = RuleUtil.doRule(tm,data);
            return o.toString();
        }catch (Exception e){
            log.error(e);
        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
