package com.octopus.utils.xml.auto.defpro;

import com.octopus.utils.xml.XMLMakeup;
import org.quartz.SchedulerException;

/**
 * User: Administrator
 * Date: 15-1-12
 * Time: 上午9:18
 */
public interface IObjectInitProperty extends IExeProperty {
    public void destroy(XMLMakeup xml) throws Exception;
}
