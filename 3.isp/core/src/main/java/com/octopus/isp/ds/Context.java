package com.octopus.isp.ds;

import com.octopus.isp.ds.data.DefaultDataFormat;
import com.octopus.isp.ds.data.IDataFormat;
import com.octopus.tools.i18n.II18N;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.exception.ISPException;
import com.octopus.utils.time.DateTimeUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.auto.XMLParameter;
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
    II18N i18n;
    SimpleDateFormat systemtime=null;
    Locale locale=null;
    XMLMakeup root;
    IDataFormat defaultFormat = new DefaultDataFormat();
    Map<String,IDataFormat> formats = new HashMap();

    public Locale getLocale(){
        if(null == locale){
            if(null != getParameter("session.user.language"))
            locale = new Locale((String)getParameter("session.user.language"));
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

    public String getSystemDate(long time){
         try {

             if(systemtime==null && null != getParameter("session.user.datetimestyle")){
                 systemtime=DateTimeUtils.getDateFormat((String)getParameter("session.user.datetimestyle"),getLocale());
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


    public II18N getI18n(){
        return i18n;
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
        IDataFormat f =  formats.get("session.user.datetimestyle");
        if(null!=f){
            return f;
        }else{
            return defaultFormat;
        }
    }
}
