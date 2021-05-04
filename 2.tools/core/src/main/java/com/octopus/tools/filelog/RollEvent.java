package com.octopus.tools.filelog;/**
 * @program: treasurebag
 * @description: add
 * @author: liu yan
 * @create: 2021-05-04 08:09
 */

import org.apache.log4j.spi.LoggingEvent;

/**
 *@program: treasurebag
 *@description: add
 *@author: wangfeng2
 *@create: 2021-05-04 08:09
 */
public interface RollEvent {
    public void doOne(LoggingEvent o);
    public boolean isOver(String curFileName);
    public boolean isStartOver(String curFilePath);

    public boolean isFirst();
}
