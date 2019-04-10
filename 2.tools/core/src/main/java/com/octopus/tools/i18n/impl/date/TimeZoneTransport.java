package com.octopus.tools.i18n.impl.date;

import com.octopus.tools.i18n.ITransport;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cls.ClassUtils;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.time.DateTimeUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;

import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

/**
 * User: wangfeng2
 * Date: 14-8-21
 * Time: 下午4:35
 */
public class TimeZoneTransport extends XMLObject implements ITransport {
    public TimeZoneTransport(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
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

    }

    @Override
    public Object transport(Object o,Properties locale) {
        if(StringUtils.isNotBlank(locale.getProperty("timezone"))){
            setTimeZone();
            Date d = (Date)o;
            String timezoneid=locale.getProperty("timezone");
            if(null != timezoneid && !timezoneid.equals(TimeZone.getDefault().getID()))
                return DateTimeUtils.translateZoneTime(d,TimeZone.getDefault(),TimeZone.getTimeZone(timezoneid));
        }
        return o;
    }

    @Override
    public Object transport2System(Object o, Properties locale) {
        if(StringUtils.isNotBlank(locale.getProperty("timezone"))){
            setTimeZone();
            Date d = (Date)o;
            String timezoneid=locale.getProperty("timezone");
            if(null != timezoneid && !timezoneid.equals(TimeZone.getDefault().getID()))
                return DateTimeUtils.translateZoneTime(d,TimeZone.getTimeZone(timezoneid),TimeZone.getDefault());
        }
        return o;
    }

    void setTimeZone(){
        try{
            Object obj = ClassUtils.getFieldValue(this.getParent(),"getter",false);
            if(null != obj){
                String basetimezone = (String)ClassUtils.getFieldValue(obj,"basetimezone",false);
                if(!TimeZone.getDefault().getID().equals(basetimezone)){
                    TimeZone.setDefault(TimeZone.getTimeZone(basetimezone));
                }
            }
        }catch (Exception e){

        }
    }
}
