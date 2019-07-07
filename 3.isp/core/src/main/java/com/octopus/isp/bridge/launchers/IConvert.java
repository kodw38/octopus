package com.octopus.isp.bridge.launchers;

import com.octopus.utils.xml.auto.XMLParameter;

/**
 * User: Administrator
 * Date: 14-9-29
 * Time: 上午11:11
 */
public interface IConvert {
    public Object convert(XMLParameter env,Object par) throws Exception;
}
