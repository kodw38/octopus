package com.octopus.isp.bridge;

import com.octopus.isp.ds.RequestParameters;

/**
 * User: Administrator
 * Date: 14-9-29
 * Time: 上午9:20
 */
public interface IBridge {

    public String getInstanceId();

    public ILauncher getLauncher(String launcherName);

    public Object evaluate(RequestParameters parameters);


}
