package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;

import java.util.Map;

/**
 * Created by robai on 2018/1/22.
 */
public class PointParseGetAllParameters implements IPointParse {
    static String[] keys = new String[]{"${input_data}","${targetNames}","${requestDate}","${clientInfo}","^iserror","^Exception","${isredo}","${return}","${env}"};
    @Override
    public String parse(String str, Map data,XMLObject obj) {
        if(null != data) {
            return ObjectUtils.convertKeyWithoutThreadNameMap2String(data,keys);
        }
        return null;
    }
}
