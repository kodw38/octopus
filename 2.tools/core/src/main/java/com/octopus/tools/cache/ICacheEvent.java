package com.octopus.tools.cache;

/**
 * User: Administrator
 * Date: 14-9-17
 * Time: 下午1:44
 */
public interface ICacheEvent{

    public boolean doCacheEvent(String method,String key,Object value);

}
