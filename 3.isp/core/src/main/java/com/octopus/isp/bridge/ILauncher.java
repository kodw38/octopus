package com.octopus.isp.bridge;

/**
 * User: Administrator
 * Date: 14-9-29
 * Time: 上午9:21
 */
public interface ILauncher {
    public void start() throws Exception;

    public boolean addEnv(String key,Object value);

    public Object invoke(Object obj) throws Exception;

}
