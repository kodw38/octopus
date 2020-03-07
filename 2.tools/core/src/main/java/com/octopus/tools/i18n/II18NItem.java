package com.octopus.tools.i18n;

import java.util.Properties;

/**
 * User: Administrator
 * Date: 14-8-27
 * Time: 下午8:35
 */
public interface II18NItem {
    public String getType();
    public boolean matchLocaleString(String locale);

    public Object getLocaleValue(Object systemValue,Properties locale);
    public Object getLocaleValue(Properties locale);
    public Object getSystemValue(Object localeValue,Properties locale);
    public Object exportAll();
    public Object changeFrom(String typeValue,Object obj);
}
