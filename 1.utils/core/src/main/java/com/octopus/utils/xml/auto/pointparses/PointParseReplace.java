package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.exception.ISPException;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;

import java.util.Map;

/**
 * Created by admin on 2020/1/31.
 */
public class PointParseReplace implements IPointParse {
    @Override
    public String parse(String str, Map data, XMLObject obj) throws ISPException {
        try {
            String tm = str.substring("replace(".length(), str.length() - 1);
            String[] ts = null;
            if(tm.startsWith("'")){
                ts = tm.split("','");
                for(int i=0;i<ts.length;i++){
                    ts[i]=ts[i].substring(1);
                }
            }else{
                ts = tm.split(",");
            }
            if (ts.length == 3) {
                return StringUtils.replace(ts[0],ts[1],ts[2]);
            }else{
                return str;
            }
        }catch (Exception e){
            return str;
        }
    }
}
