package com.octopus.utils.time;

import java.util.Date;

/**
 * User: Administrator
 * Date: 14-10-29
 * Time: 下午5:04
 */
public interface IWorkTime {

    public void addExpression(String expression);
    public boolean isWorkTime();
    public boolean isWorkTime(Date date);
    public long getTheTimesSurplusSecond();
    public long getTheTimesSurplusSecond(Date date);
    public long getTheTimesUsedSecond();
    public long getTheTimesUsedSecond(Date date);
    public Date getNextWorkTime();
    public Date getNextWorkTime(Date date);
    public long getWholeWorkSecondBetweenDates(Date begin,Date end);
}
