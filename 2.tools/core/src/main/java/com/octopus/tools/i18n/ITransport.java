package com.octopus.tools.i18n;

import java.util.Properties;

/**
 * User: wangfeng2
 * Date: 14-8-18
 * Time: 下午5:16
 */
public interface ITransport {
    public Object transport(Object o,Properties locale);

    public Object transport2System(Object o,Properties locale);
}
