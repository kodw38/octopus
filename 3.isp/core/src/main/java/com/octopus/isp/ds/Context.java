package com.octopus.isp.ds;

import com.octopus.isp.ds.data.DefaultDataFormat;
import com.octopus.isp.ds.data.IDataFormat;
import com.octopus.tools.i18n.II18N;
import com.octopus.tools.i18n.impl.I18N;
import com.octopus.tools.i18n.impl.I18NItem;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.exception.ISPException;
import com.octopus.utils.time.DateTimeUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.auto.XMLParameter;
import com.sun.org.apache.xml.internal.security.utils.I18n;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * User: Administrator
 * Date: 14-9-3
 * Time: 上午8:51
 */
public class Context extends XMLParameter {
    static transient Log log = LogFactory.getLog(Context.class);
    I18N i18n;
    SimpleDateFormat systemtime=null;
    Locale locale=null;
    XMLMakeup root;
    Context defaultContext=null;
    IDataFormat defaultFormat = new DefaultDataFormat();
    Map<String,IDataFormat> formats = new HashMap();

    public Locale getLocale(){
        if(null == locale){
            if(null != getParameter("language"))
            locale = new Locale((String)getParameter("language"));
        }
        if(null != locale)
            return locale;
        else
            return Locale.CHINESE;
    }
    public void setRootXMLMakeup(XMLMakeup xml){
        root = xml;
        List<XMLMakeup> cs = root.getChildren();
        if(null != cs){
            for(XMLMakeup c:cs){
                String key = c.getProperties().getProperty("key");
                String format = c.getProperties().getProperty("format");
                String text = c.getText();
                if(StringUtils.isNotBlank(key) && StringUtils.isNotBlank(format) && StringUtils.isNotBlank(text)){
                    try {
                        Class ci = Class.forName(format);
                        if(IDataFormat.class.isAssignableFrom(ci)){
                            IDataFormat f = (IDataFormat)ci.getConstructor(String.class,Context.class).newInstance(text,this);
                            formats.put(key,f);
                        }
                    }catch (Exception e){
                        log.error("",e);
                    }
                }
            }
        }
    }
    public void setDefaultContext(Context t){
        defaultContext=t;
    }

    public String getSystemDate(long time){
         try {

             if(systemtime==null && null != getParameter("datetimeformat")){
                 systemtime=DateTimeUtils.getDateFormat((String)getParameter("datetimeformat"),getLocale());
             }
             if(null != systemtime)
                 return DateTimeUtils.getZoneTime(systemtime, new Date(time));
             else
                 return DateTimeUtils.getCurrDateTime();
         }catch (Exception e){
             e.printStackTrace();
         }
        return null;
    }

    public String getSystemDate(){
        return getSystemDate(new Date().getTime());
    }


    public I18N getI18n(){
        return i18n;
    }
    public void setI18n(I18N i18n){
        this.i18n=i18n;
    }


    public boolean checkFormat(String type,Object value) throws ISPException{
        if(StringUtils.isNotBlank(type) && null != value && !"".equals(value)){
            if(!type.startsWith("check")){
                type="check."+type;
            }
            String format = (String)getParameter(type);
            if(null != format) {
                boolean b =  StringUtils.regExpress(value.toString(), format);
                if(!b && null!= root){
                    XMLMakeup xml = root.getFirstChildById(type);
                    if(null !=xml) {
                        String error = xml.getProperties().getProperty("error");
                        if (StringUtils.isNotBlank(error)) {
                            Map m = StringUtils.convert2MapJSONObject(error);
                            if (null != m && m.keySet().size()>1) {
                                String[] it = (String[])m.keySet().toArray(new String[0]);
                                String code = (String) m.get(it[0]);
                                String msg = (String) m.get(it[1]);
                                throw new ISPException(code, msg);
                            }
                        }
                    }
                }
                return b;
            }
        }
        return true;
    }

    public IDataFormat getUserDatetimeFormat(){
        IDataFormat f =  formats.get("datetimeformat");
        if(null!=f){
            return f;
        }else{
            return defaultFormat;
        }
    }

    /**
     *
     * @param i18nType
     * @return user config type value of the type, example en of language. if user does not config the i18n type , use the default config of system
     */
    public String getI18nTypeValue(String i18nType){
        return (String)getParameter(i18nType);
    }

    /**
     * get system default i18n setting
     * @param i18nType
     * @return
     */
    public String getDefaultI18nTypeValue(String i18nType){
        if(null != defaultContext){
            return (String)defaultContext.get(i18nType);
        }
        return null;
    }
    /**
     *
     * @param i18nType
     * @param curValueI18n
     * @param curValue
     * @param defautValue
     * @return
     */
    public Object getI18nValue(String i18nType,String curValueI18n,Object curValue,String defautValue){
        I18NItem item = i18n.getItem(i18nType,getI18nTypeValue(i18nType));
        if(null != item) {
            Object ret = item.changeFrom(curValueI18n, curValue);
            if (null != ret) {
                return ret;
            } else if (null != defautValue) {
                return defautValue;
            } else {
                return curValue;
            }
        }else if(StringUtils.isNotBlank(defautValue)){
            return defautValue;
        }else{
            return curValue;
        }
    }
}
