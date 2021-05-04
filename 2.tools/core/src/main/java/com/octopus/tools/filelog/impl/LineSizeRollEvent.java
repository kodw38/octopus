package com.octopus.tools.filelog.impl;
/**
 * @program: treasurebag
 * @description: add
 * @author: liu yan
 * @create: 2021-05-04 09:17
 */

import com.octopus.tools.filelog.RollEvent;
import com.octopus.utils.file.FileUtils;
import org.apache.log4j.spi.LoggingEvent;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

/**
 *@program: treasurebag
 *@description: add
 *@author: wangfeng2
 *@create: 2021-05-04 09:17
 */
public class LineSizeRollEvent implements RollEvent {
    SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmm");
    int maxLineSize;
    AtomicLong size = new AtomicLong(0);
    public LineSizeRollEvent(int maxLineSize){
        this.maxLineSize=maxLineSize;
    }
    @Override
    public void doOne(LoggingEvent o) {
        size.incrementAndGet();
    }

    @Override
    public boolean isOver(String curFileName) {
        String ss = format.format(new Date());
        if(size.get()>maxLineSize||(null != curFileName && !curFileName.contains(ss))) {
            size.set(1);
            return true;
        }
        return false;
    }

    @Override
    public boolean isStartOver(String curFilePath) {
        long line =  FileUtils.getLineNumber(new File(curFilePath));
        if(line<maxLineSize) {
            size.set(line);
        }
        return line >=maxLineSize;
    }

    @Override
    public boolean isFirst() {
        return size.longValue()<=1;
    }

}
