package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.exception.ISPException;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;

import java.net.URLEncoder;
import java.util.Map;

/**
 * Created by admin on 2020/1/3.
 */
public class PointParseURLEncode implements IPointParse {
    @Override
    public String parse(String str, Map data, XMLObject obj) throws ISPException {
        String tm =str.substring("urlencode(".length(), str.length() - 1);
        return URLEncoder.encode(tm);
    }
}
