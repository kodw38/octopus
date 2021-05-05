package com.octopus.tools.utils;/**
 * Created by admin on 2020/8/29.
 */

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.time.DateTimeUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

/**
 * @ClassName DateUtils
 * @Description ToDo
 * @Author Kod Wong
 * @Date 2020/8/29 18:24
 * @Version 1.0
 **/
public class DateUtils extends XMLDoObject {
    public DateUtils(XMLMakeup xml, XMLObject parent, Object[] containers) throws Exception {
        super(xml, parent, containers);
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(null != input){
            String op = (String)input.get("op");
            if(StringUtils.isNotBlank(op)){
                if("nextSleepSeconds".equals(op)){
                    String st = (String)input.get("startTime");
                    String[] first=null;
                    for(String s:st.split("\\,")) {
                        String[] hm = s.split("\\:");
                        if(first==null)first=hm;
                        //String et = (String)input.get("endTime");
                        Date d = new Date();
                        Calendar c = Calendar.getInstance();
                        c.setTime(d);
                        c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hm[0]));
                        c.set(Calendar.MINUTE, Integer.parseInt(hm[1]));
                        c.set(Calendar.SECOND, Integer.parseInt(hm[2]));
                        Date n = c.getTime();
                        if (n.after(d)) {
                            return (n.getTime() - d.getTime()) / 1000;
                        }
                    }
                    if(null != first) {
                        Date d = new Date();
                        Calendar c = Calendar.getInstance();
                        c.setTime(d);
                        c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(first[0]));
                        c.set(Calendar.MINUTE, Integer.parseInt(first[1]));
                        c.set(Calendar.SECOND, Integer.parseInt(first[2]));
                        Date n = c.getTime();
                        n = DateTimeUtils.addOrMinusDays(n.getTime(), 1);
                        return (n.getTime() - d.getTime()) / 1000;
                    }

                }else if("isBetween".equals(op)){
                    String[] sts = ((String)input.get("startTime")).split("\\,");
                    String[] ets = ((String)input.get("endTime")).split("\\,");
                    for(int i=0;i<sts.length;i++) {
                        String[] s = sts[i].split("\\:");
                        String[] e = ets[i].split("\\:");
                        Date d = new Date();
                        Calendar c = Calendar.getInstance();
                        c.setTime(d);
                        c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(s[0]));
                        c.set(Calendar.MINUTE, Integer.parseInt(s[1]));
                        c.set(Calendar.SECOND, Integer.parseInt(s[2]));
                        Date ss = c.getTime();
                        c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(e[0]));
                        c.set(Calendar.MINUTE, Integer.parseInt(e[1]));
                        c.set(Calendar.SECOND, Integer.parseInt(e[2]));
                        Date ee = c.getTime();
                        Date cc = new Date();
                        if (cc.after(ss) && cc.before(ee)) {
                            return "true";
                        }
                    }
                    return "false";


                }
            }
        }
        return null;
    }

    @Override
    public ResultCheck checkReturn(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return new ResultCheck(true,ret);
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret, Exception e) throws Exception {
        return false;
    }
}
