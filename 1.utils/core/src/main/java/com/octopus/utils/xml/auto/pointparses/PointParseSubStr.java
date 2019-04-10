package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.alone.impl.SplitStruct;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.Map;

/**
 *
 * User: wfgao_000
 * Date: 15-6-8
 * Time: 下午6:11
 */
public class PointParseSubStr implements IPointParse{
    @Override
    public String parse(String str, Map data,XMLObject obj) {
        if(str.startsWith("substr(") && str.indexOf("${")<0){
            String t = str.substring("substr(".length(), str.length() - 1);
            if(XMLParameter.isHasRetainChars(t)){
                return str;
            }
            try{
                SplitStruct fs = StringUtils.getSplitFromRight(t,',',"Number",2);
                if(null != fs){
                    String[] m;
                    if(fs.getPoints().size()==0)
                        m = StringUtils.split(t,";");
                    if(fs.getPoints().size()==2){
                       int s =  Integer.parseInt((String)fs.getPoints().get(1));
                       int e = Integer.parseInt((String)fs.getPoints().get(0));
                        if(e>s)
                            return StringUtils.substring(fs.getSupplus(),s,e);
                        else
                            return fs.getSupplus();
                    }if(fs.getPoints().size()==1){
                        int n = Integer.parseInt((String)fs.getPoints().get(0));
                        if(n>0)
                            return StringUtils.substring(fs.getSupplus(),n);
                    }
                    return fs.getSupplus();
                }
            }catch (Exception e){

            }
        }
        return str;
    }
}
