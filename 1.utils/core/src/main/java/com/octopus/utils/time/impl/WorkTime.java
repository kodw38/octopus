package com.octopus.utils.time.impl;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.time.IWorkTime;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * User: Administrator
 * Date: 14-10-29
 * Time: 下午5:04
 */
public class WorkTime extends XMLObject implements IWorkTime {
    List<String> workTimeExpressionList = new ArrayList<String>();

    public WorkTime(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        String s = getXML().getProperties().getProperty("expression");
        if(StringUtils.isNotBlank(s)){
            String[] ss = s.split(",");
            for(String si:ss){
                if(checkExpression(si.trim()))
                    workTimeExpressionList.add(si.trim());
            }
        }
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

    boolean checkExpression(String expression){
        return true;
    }
    @Override
    public void addExpression(String expression) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isWorkTime() {
        Date t = new Date();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY,9);
        cal.add(Calendar.MINUTE,30);

        Calendar bcal = Calendar.getInstance();
        bcal.add(Calendar.HOUR_OF_DAY,11);
        bcal.add(Calendar.MINUTE,30);

        Calendar ca3 = Calendar.getInstance();
        ca3.add(Calendar.HOUR_OF_DAY,13);

        Calendar bca3 = Calendar.getInstance();
        bca3.add(Calendar.HOUR_OF_DAY,15);

        Calendar cal1 = Calendar.getInstance();
        cal1.add(Calendar.HOUR_OF_DAY,t.getHours());
        cal1.add(Calendar.MINUTE,t.getMinutes());

        Calendar wcal = Calendar.getInstance();
        wcal.setTime(t);
        int week = wcal.get(Calendar.DAY_OF_WEEK);

        if( ((cal1.getTimeInMillis()>=cal.getTimeInMillis() && cal1.getTimeInMillis()<=bcal.getTimeInMillis())
                || (cal1.getTimeInMillis()>=ca3.getTimeInMillis() && cal1.getTimeInMillis()<=bca3.getTimeInMillis()))
                && (week!=Calendar.SATURDAY && week!=Calendar.SUNDAY)){
            return true;
        }
        return false;
    }

    @Override
    public boolean isWorkTime(Date date) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long getTheTimesSurplusSecond() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long getTheTimesSurplusSecond(Date date) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long getTheTimesUsedSecond() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long getTheTimesUsedSecond(Date date) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Date getNextWorkTime() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Date getNextWorkTime(Date date) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long getWholeWorkSecondBetweenDates(Date begin, Date end) {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
