package com.octopus.tools.filelog;

import org.apache.log4j.Category;
import org.apache.log4j.Priority;
import org.apache.log4j.spi.LoggingEvent;

/**
 * User: wfgao_000
 * Date: 16-6-4
 * Time: 上午8:25
 */
public class LogEvent extends LoggingEvent {
    String header;
    public LogEvent(String fqnOfCategoryClass, Category logger, Priority level, Object message, Throwable throwable,String header) {
        super(fqnOfCategoryClass, logger, level, message, throwable);
        this.header=header;
    }
    public String getHeader(){
        return header;
    }
}
