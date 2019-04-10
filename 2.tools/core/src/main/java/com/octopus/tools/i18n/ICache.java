package com.octopus.tools.i18n;

import java.util.Properties;

/**
 * User: wangfeng2
 * Date: 14-8-18
 * Time: 下午11:51
 */
public interface ICache {
    public Object get(Properties locale);

    public void add(Object o,Properties locale);

    public Object getAll();
}
