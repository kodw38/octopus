package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.exception.ExceptionUtil;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.Map;

/**
 * Created by robai on 2018/1/22.
 */
public class PointParseGetErrorTrace implements IPointParse {
    @Override
    public String parse(String str, Map data,XMLObject obj) {
        if(data instanceof XMLParameter){
            String s = ExceptionUtil.getString(((XMLParameter) data).getException());
            if(null == s){
                return "";
            }else{
                return s;
            }
        }else{
            return "";
        }
    }
}
