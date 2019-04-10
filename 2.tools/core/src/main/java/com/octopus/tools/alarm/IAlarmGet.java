package com.octopus.tools.alarm;

import java.util.Map;

/**
 * 放在业务模块中的获取告警信息的探针
 * User: Administrator
 * Date: 14-9-28
 * Time: 下午2:37
 */
public interface IAlarmGet {
    public boolean addAlarm(String type,String msgSrc,Map<String,String> msg);

}
