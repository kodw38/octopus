package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.Map;

/**
 * User: wfgao_000
 * Date: 16-6-16
 * Time: 下午6:06
 */
public class PointParseIfNull implements IPointParse {
    @Override
    public String parse(String str, Map data,XMLObject obj) {
        try{
            String tm =str.substring("ifnull(".length(), str.length() - 1);
            if(StringUtils.isNotBlank(tm)){

                String[] ts = StringUtils.splitExcludeToken(tm,",",new char[][]{{'(',')'},{'{','}'}},false);
                /*if(StringUtils.isBlank(ts[0]))
                    return ts[1];
                else if(XMLParameter.isHasRetainChars(ts[0])){
                    return ts[1];
                } else{
                    return ts[0];
                }*/
                for(String s:ts){
                    if(StringUtils.isNotBlank(s) && !XMLParameter.isHasRetainChars(s))
                        return s;
                }
            }

            return "";
        }catch (Exception e){
            e.printStackTrace();
            return "";
        }
    }
}