package com.octopus.tools.i18n.impl;

import com.octopus.tools.i18n.*;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;

import java.util.Properties;

/**
 * User: wangfeng2
 * Date: 14-8-18
 * Time: 下午5:17
 */
public class I18NItem extends XMLObject implements II18NItem {
    IExistStyle existstyle;
    IFormat formate;
    IGetter getter;
    ITransport transport;
    ICache cache;
    String type;
    String locale;

    public I18NItem(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    @Override
    public void notifyObject(String op, Object obj) throws Exception {

    }

    @Override
    public void addTrigger(Object cond, InvokeTaskByObjName task) throws Exception {

    }

    @Override
    public void destroy() throws Exception {

    }

    @Override
    public void initial() throws Exception {
        if(type==null && locale==null){
            String id = getXML().getId();
            String[] ids = id.split("\\|");
            type=ids[0];
            locale=ids[1];
        }
    }


    public String getType() {
        return type;
    }

    public boolean matchLocaleString(String locale){
        //如果没有配置local属性，认为可以匹配任何条件
        if(StringUtils.isBlank(this.locale)){
            return true;
        }
        //配置的this.locale为locale中的一段完全匹配，则认为匹配。
        if(("."+locale+".").contains("."+this.locale+".")){
            return true;
        }
        return false;
    }

    public Object getLocaleValue(Object systemValue,Properties locale){
        if(null == locale)return systemValue;
        if(null != systemValue){
            if(null != transport){
                systemValue = transport.transport(systemValue,locale);
            }
            if(null != formate){
                systemValue = formate.formate(systemValue);
            }
            return systemValue;
        }
        return null;
    }

    public Object getLocaleValue(Properties locale){
        if(null ==  locale)return null;
        Object ret = null;
        if(null != cache){
            ret = cache.get(locale);
        }
        if(null == ret){
            if(null != getter){
                Object v = getter.get(locale);
                if(null != v){
                    if(null != transport){
                        v = transport.transport(v,locale);
                    }
                    if(null != formate){
                        v = formate.formate(v);
                    }
                    if(null != cache){
                        synchronized (cache){
                            cache.add(v,locale);
                        }
                    }
                    ret= v;
                }
            }
        }
        return ret;
    }

    public Object getSystemValue(Object localeValue,Properties locale){
        if(null == locale)return localeValue;
        if(null != localeValue){
            if(null != formate){
                localeValue = formate.formate2System(localeValue);
            }
            if(null != transport){
                localeValue = transport.transport2System(localeValue,locale);
            }
            if(null != formate){
                localeValue = formate.formate(localeValue);
            }
            return localeValue;
        }
        return null;
    }

    public Object exportAll(){
        return existstyle.export(cache.getAll());
    }

    @Override
    public Object changeFrom(String typeValue, Object obj) {
        if(StringUtils.isNotBlank(typeValue) && null != obj) {
            return null;
        }
        return null;
    }
}
