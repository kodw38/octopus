package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Date;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 2016/11/11
 * Time: 10:17
 */
public class PointParsePrint implements IPointParse {
    transient  static Log log = LogFactory.getLog(PointParsePrint.class);
    @Override
    public String parse(String str, Map data,XMLObject obj) {
        try{
            String msg =str.substring("print(".length(), str.length() - 1);
            if(StringUtils.isNotBlank(msg)){
                Date d = new Date();
                try{
                    int n = 0;
                    if(msg instanceof String && ((String)msg).startsWith("{")){
                        Map o = StringUtils.convert2MapJSONObject(msg);
                        Map v = XMLParameter.getMapValueFromParameter(data,o,null);

                        log.info(("["+Thread.currentThread().getName()+"] ["+d +" "+d.getTime()+"] "+v.toString()+"\n"));
                    }else{
                        log.info(("["+Thread.currentThread().getName()+"] ["+d +" "+d.getTime()+"] "+msg+"\n"));
                    }
                }catch (Exception e){
                    try{
                        log.error(("["+Thread.currentThread().getName()+"] ["+d +" "+d.getTime()+"] "+"\n"),e);
                    }catch (Exception ex){}
                }
            }

            return "";
        }catch (Exception e){
            return "";
        }
    }
}
