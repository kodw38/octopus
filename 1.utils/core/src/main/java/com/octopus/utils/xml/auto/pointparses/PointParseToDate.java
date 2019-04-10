package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.cls.ClassUtils;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;

/**
 * Created by robai on 2018/1/8.
 */
public class PointParseToDate implements IPointParse {
    static transient Log log  = LogFactory.getLog(PointParseToDate.class);
    @Override
    public String parse(String str, Map data,XMLObject obj) {
        String t =str.substring("todate(".length(), str.length() - 1);
        if(XMLParameter.isHasRetainChars(t, XMLParameter.NestTagsBegin)){
            return str;
        }
        String[] exs = t.split(",");
        if(exs.length==1){
            if(null != data){
                Object context = data.get("${context}");
                if(null != context) {
                    Object d=null;
                    if(exs[0].startsWith("${")){
                        d = ObjectUtils.getValueByPath(data,exs[0]);
                    }else if(XMLParameter.isHasRetainChars(exs[0])) {
                        d = XMLParameter.getExpressValueFromMap(exs[0], data,obj);
                    }
                    if(d instanceof String) {
                        try {
                            return (String)ClassUtils.invokeMethod(context, "getSystemDate", new Class[]{long.class}, new Object[]{Long.parseLong((String) d)});
                        }catch (Exception e){
                            log.error("todate error:",e);
                        }
                    }
                }
            }
        }else{
            log.error("not support toDate parameter "+t);
        }


        return str;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
