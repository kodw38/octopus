package com.octopus.tools.alarm;

import java.util.Date;
import java.util.List;

/**
 * User: Administrator
 * Date: 14-8-25
 * Time: 下午2:49
 */
public interface IAlarm {

    public boolean alarm(List<String> receivers,List<String> cc,String title,String message,Date sendDate,int sendtype);

}
