package com.octopus.utils.cls.proxy;

/**
 * User: Administrator
 * Date: 14-12-30
 * Time: 上午11:06
 */
public interface IProxyHandler {
    public Object handle(IMethodAddition[] additions,Object impl,  String m,Class[] parclasses,Object[] args)throws Exception;
}
