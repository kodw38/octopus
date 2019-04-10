package com.octopus.tools.i18n;

import java.util.Properties;

/**
 * User: Administrator
 * Date: 14-8-27
 * Time: 下午8:33
 */
public interface II18N {
    public Object getLocaleValue(String type,Properties locale,Object key);
    public Object getLocaleValue(String type,Properties locale);
    public Object getSystemValue(String type,Properties locale,Object key);

}
