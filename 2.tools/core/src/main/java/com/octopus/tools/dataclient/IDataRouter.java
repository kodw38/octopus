package com.octopus.tools.dataclient;

/**
 * User: Administrator
 * Date: 14-9-22
 * Time: 下午3:58
 */
public interface IDataRouter {
    public IDataEngine[] getRouter(String datasource,Object env);
}
