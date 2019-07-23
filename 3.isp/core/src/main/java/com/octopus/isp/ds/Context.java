package com.octopus.isp.ds;

import com.octopus.tools.i18n.II18N;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.exception.ISPException;
import com.octopus.utils.time.DateTimeUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.auto.XMLParameter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * User: Administrator
 * Date: 14-9-3
 * Time: 上午8:51
 */
public class Context extends XMLParameter {
    II18N i18n;
    SimpleDateFormat systemtime=null;
    Locale locale=null;
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
            if(!type.startsWith("format")){
                type="format."+type;
            }
            String format = (String)getParameter(type);
            if(null != format) {
                return StringUtils.regExpress(value.toString(), format);
            }
        }
        return true;
    }
}
