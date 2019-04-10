package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 修改原有变量
 * User: wfgao_000
 * Date: 16-5-14
 * Time: 上午11:27
 */
public class PointParseVarCal implements IPointParse {
    transient static Log log = LogFactory.getLog(PointParseVarCal.class);
    @Override
    public String parse(String str, Map data,XMLObject obj) {
        try{
            String tm =str.substring("varcal(".length(), str.length() - 1);
            String[] ts = tm.split(",");
            if(ts.length==2){
                if(ts[0].startsWith("${")){
                    Object o = ObjectUtils.getValueByPath(data,ts[0]);
                    if(null != o){
                        if(o instanceof AtomicInteger && StringUtils.isNotBlank(ts[1]) && StringUtils.isNumeric(ts[1])){
                            int r = ((AtomicInteger)o).getAndAdd(Integer.parseInt(ts[1]));
                            return ""+r;
                        }
                    }
                }
            }
        }catch (Exception e){
            log.error("not support format result", e);
        }
        return str;
    }
}
