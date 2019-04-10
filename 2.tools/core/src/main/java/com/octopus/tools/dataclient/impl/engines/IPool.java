package com.octopus.tools.dataclient.impl.engines;

import java.util.Properties;

/**
 * User: Administrator
 * Date: 14-9-24
 * Time: 下午12:38
 */
public interface IPool {

    public Properties getConnProperties();

    public boolean isMatch(DC dc,Object evn);

    public boolean isDefault();
}
