package com.octopus.tools.filelog;/**
 * @program: treasurebag
 * @description: add
 * @author: liu yan
 * @create: 2021-05-04 08:48
 */

import com.octopus.tools.filelog.impl.ExpLogFileName;
import com.octopus.tools.filelog.impl.LineSizeRollEvent;
import com.octopus.utils.alone.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.AppenderAttachableImpl;

import java.io.IOException;
import java.util.Properties;

/**
 *@program: treasurebag
 *@description: add
 *@author: wangfeng2
 *@create: 2021-05-04 08:48
 */
public class FileLogger extends Logger {
    transient static Log log = LogFactory.getLog(FileLogger.class);
    private AppenderAttachableImpl aai = new AppenderAttachableImpl();
    MyRollingAppender fileAppender;
    String code;

    public static FileLogger getInstance()throws Exception{
        try {
            Properties p = new Properties();
            p.load(FileLogger.class.getClassLoader().getResourceAsStream("filelog.properties"));
            String code = p.getProperty("code");
            String logDir = p.getProperty("curLogDir");
            String hisDir = p.getProperty("hisLogDir");
            String curKeyWords = p.getProperty("curKeyWordsInName");
            String hisKeyWords = p.getProperty("hisKeyWordsInName");
            String maxFileLine = p.getProperty("maxLineNumber");
            ExpLogFileName fileName = new ExpLogFileName(code,logDir,curKeyWords,hisKeyWords);
            LineSizeRollEvent event = new LineSizeRollEvent(Integer.parseInt(maxFileLine));
            FileLogger ret = new FileLogger(code,logDir,hisDir,fileName,event,0,"UTF-8",null);

            return ret;
        }catch (Exception e){
            throw e;
        }

    }

    public FileLogger(String code, String curDir, String hisDir, LogFileName logFileName
            , RollEvent rollEvent, int remainSize, String fileEncoding, String headerLine) throws Exception{
        super(code);
        this.code=code;
        setLevel(Level.INFO);

        LogLayout layout = new LogLayout();
        layout.setHeader(headerLine);
        layout.setConversionPattern("%m%n");
        try
        {
            fileAppender = new MyRollingAppender(layout,curDir,hisDir,logFileName,rollEvent,remainSize);
            if(StringUtils.isNotBlank(fileEncoding))
                this.fileAppender.setEncoding(fileEncoding);
        } catch (IOException e) {
            log.error("Setting DailyRollingFileAppender Error: ", e);
        }
        this.fileAppender.setName("FileAppender");
        this.aai.addAppender(this.fileAppender);
    }
    public boolean isFirstLog(){
        return fileAppender.isFirstLog();
    }
    public void addLog(String content,String header)
    {
        LogEvent event = new LogEvent(name, this, Level.INFO, content, null,header);
        synchronized (this) {
            if (this.aai != null)
                this.aai.appendLoopOnAppenders(event);
        }
    }
    public void addLog(String content)
    {
        LogEvent event = new LogEvent(name, this, Level.INFO, content, null,null);
        synchronized (this) {
            if (this.aai != null)
                this.aai.appendLoopOnAppenders(event);
        }
    }
    public void addLog(Object data)
    {
        LogEvent event = new LogEvent(code, this, Level.INFO, data.toString(), null,null);
        synchronized (this) {
            if (this.aai != null)
                this.aai.appendLoopOnAppenders(event);
        }
    }
}
