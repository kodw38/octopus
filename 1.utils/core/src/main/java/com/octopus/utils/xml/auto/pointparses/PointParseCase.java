package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-10-15
 * Time: 下午5:08
 * case (v,[{"",""}])
 */
public class PointParseCase implements IPointParse {
    transient static Log log = LogFactory.getLog(PointParseCase.class);
    @Override
    public String parse(String str, Map data,XMLObject obj) {
        try{
            String tm =str.substring("case(".length(), str.length() - 1);
            int m = tm.indexOf(",");
            String v = tm.substring(0,m);
            String v2 = tm.substring(m+1);
            List<List> l = StringUtils.convert2ListJSONObject(v2);
            l = XMLParameter.getValueListFromMapDataByJsonMapExp(data,l,null);
            if(null != l){
                String def="";
                for(int i=0;i<l.size();i++){
                    if(v.equals(l.get(i).get(0))){
                        if(l.get(i).size()>1)
                            return (String)l.get(i).get(1);
                        return "";
                    }
                    if("default".equals(l.get(i).get(0))){
                        if(l.get(i).size()>1)
                            def= (String)l.get(i).get(1);
                    }
                }
                if(StringUtils.isNotBlank(def))
                return def;

            }
        }catch (Exception e){
            log.error("PointParseCase ["+str+"]",e);
        }
        return str;
    }
}