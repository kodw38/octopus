package com.octopus.tools.filelog;
/**
 * @program: treasurebag
 * @description: add
 * @author: liu yan
 * @create: 2021-05-04 08:35
 */

/**
 *@program: treasurebag
 *@description: add
 *@author: wangfeng2
 *@create: 2021-05-04 08:35
 */
public interface LogFileName {
    public String getNewFileName()  throws Exception;

    public boolean isHistoryName(String name);

    public String getHistoryName(String fileName);

    public String getCurrentFileName();
}
