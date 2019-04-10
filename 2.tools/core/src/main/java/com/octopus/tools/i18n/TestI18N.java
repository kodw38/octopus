package com.octopus.tools.i18n;

import com.octopus.utils.time.DateTimeUtils;
import com.octopus.utils.xml.XMLObject;
import junit.framework.TestCase;

import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

/**
 * User: wangfeng2
 * Date: 14-8-19
 * Time: 上午12:02
 */
public class TestI18N extends TestCase{
    public TestI18N(){
        super();
    }
    public void setUp(){

    }
    public void testDateI18n(){
        try{
            II18N i18n = (II18N)XMLObject.loadApplication("classpath:com/octopus/tools/i18n/i18n.xml", null,true,false);
            Properties properties = new Properties();
            properties.put("language","en");
            properties.put("country","us");
            properties.put("timezone","US/Aleutian");
            String k = (String)i18n.getLocaleValue("date",properties);
            System.out.println(k+"   "+i18n.getSystemValue("date",properties,k));
            properties.put("language", "zh");
            properties.put("country", "cn");
            System.out.println(i18n.getLocaleValue("date", properties));
            properties.put("language", "en");
            properties.put("country", "us");
            k = (String)i18n.getLocaleValue("datetime",properties);
            System.out.println();
            System.out.println(k+"   "+i18n.getSystemValue("datetime",properties,k));
            properties.put("language","zh");
            properties.put("country","cn");
            System.out.println(i18n.getLocaleValue("datetime", properties));

            System.out.println(i18n.getLocaleValue("datetime", properties, DateTimeUtils.addOrMinusDays(new Date().getTime(), -2)));
            System.out.println(TimeZone.getDefault().getID());
            /*String[] strings=TimeZone.getAvailableIDs();
            for(String s:strings)
                System.out.println(s);*/
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void testDateTimeUtil(){
        Date c = new Date();
        System.out.println(c);
        Date d = DateTimeUtils.translateZoneTime(c,TimeZone.getTimeZone("Asia/Shanghai"),TimeZone.getTimeZone("US/Aleutian"));
        System.out.println("US:"+d);
        Date d2 = DateTimeUtils.translateZoneTime(d,TimeZone.getTimeZone("US/Aleutian"),TimeZone.getTimeZone("Asia/Shanghai"));
        System.out.println("Shanghai:"+d2);
    }
}
