package com.octopus.tools.dataclient.utils;

import java.util.List;
import java.util.Map;

/**
 * User: Administrator
 * Date: 14-10-28
 * Time: 下午1:54
 */
public interface IDataMapping {

    public List<Map> mapping(Object data)throws Exception;
}
