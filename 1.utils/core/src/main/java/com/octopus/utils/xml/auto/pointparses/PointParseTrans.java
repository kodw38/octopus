package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.exception.ISPException;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.Map;

/**
 * Created by Administrator on 2018/10/11.
 */
public class PointParseTrans implements IPointParse {
    @Override
    public String parse(String str, Map data, XMLObject obj)throws ISPException {
        String t =str.substring("trans(".length(), str.length() - 1);
        Object ret = XMLParameter.getExpressValueFromMap(t,data,obj);
        if(null != ret && ret instanceof String){
            return (String)ret;
        }else{
            return str;
        }
    }
}
