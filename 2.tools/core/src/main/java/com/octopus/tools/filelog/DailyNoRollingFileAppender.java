package com.octopus.tools.filelog;

/**
 * User: wfgao_000
 * Date: 15-11-20
 * Time: 下午10:15
 */

import com.octopus.utils.file.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.TimerTask;

public class DailyNoRollingFileAppender extends FileAppender
{
    transient static Log log = LogFactory.getLog(DailyNoRollingFileAppender.class);
    private static final java.util.Timer TIMER = new java.util.Timer(true);
    static final int TOP_OF_TROUBLE = -1;
    static final int TOP_OF_MINUTE = 0;
    static final int TOP_OF_HOUR = 1;
    static final int HALF_DAY = 2;
    static final int TOP_OF_DAY = 3;
    static final int TOP_OF_WEEK = 4;
    static final int TOP_OF_MONTH = 5;
    private String datePattern = "'.'yyyy-MM-dd";
    private String scheduledFilename;
    private long nextCheck = System.currentTimeMillis() - 1L;
    Date now = new Date();
    SimpleDateFormat sdf;
    RollingCalendar rc = new RollingCalendar(gmtTimeZone, Locale.ENGLISH);
    int checkPeriod = -1;
    static final TimeZone gmtTimeZone = TimeZone.getTimeZone("GMT");
    protected String fileNameOrig = null;
    protected int logRemain = 180;
    protected int interval = -1;
    private String hisDir;

    public DailyNoRollingFileAppender(Layout layout, String fileNameOrig, String datePattern, int logRemain)
            throws IOException
    {
        this.fileNameOrig = fileNameOrig;
        this.sdf = new SimpleDateFormat(datePattern);

        this.fileName = fileNameOrig + this.sdf.format(new Date());

        this.layout = layout;
        setFile(this.fileName, true, false, this.bufferSize);

        this.datePattern = datePattern;
        this.logRemain = logRemain;

        activateOptions();
        startListener(datePattern);
    }

    public DailyNoRollingFileAppender(Layout layout, String fileNameOrig, String datePattern, int interval, int logRemain, String hisDir) throws IOException
    {
        this.fileNameOrig = fileNameOrig.replaceAll("\\\\","/");
        this.sdf = new SimpleDateFormat(datePattern);

        this.fileName = fileNameOrig + this.sdf.format(new Date());

        this.hisDir = hisDir;

        this.layout = layout;
        setFile(this.fileName, true, false, this.bufferSize);

        this.datePattern = datePattern;
        this.logRemain = logRemain;
        this.interval = interval;
        activateOptions();
        startListener(datePattern);
    }

    private void startListener(String datePattern){
        TIMER.schedule(new TimerTask(){
            @Override
            public void run() {
                try{
                    subAppend(null);
                }catch (Exception e){

                }
            }
        },60 * 1000L, 60 * 1000L);

    }
    public void setDatePattern(String pattern)
    {
        this.datePattern = pattern;
    }

    public String getDatePattern()
    {
        return this.datePattern;
    }

    public void activateOptions()
    {
        if ((this.datePattern != null) && (this.fileName != null)) {
            this.now.setTime(System.currentTimeMillis());
            int type = computeCheckPeriod();
            printPeriodicity(type);
            this.rc.setType(type);

            this.scheduledFilename = this.fileName + this.sdf.format(new Date());
        } else {
            LogLog.error("Either File or DatePattern options are not set for appender [" + this.name + "].");
        }
    }

    void printPeriodicity(int type) {
        switch (type)
        {
            case 0:
                LogLog.debug("Appender [" + this.name + "] to be rolled every minute.");
                break;
            case 1:
                LogLog.debug("Appender [" + this.name + "] to be rolled on top of every hour.");
                break;
            case 2:
                LogLog.debug("Appender [" + this.name + "] to be rolled at midday and midnight.");
                break;
            case 3:
                LogLog.debug("Appender [" + this.name + "] to be rolled at midnight.");
                break;
            case 4:
                LogLog.debug("Appender [" + this.name + "] to be rolled at start of week.");
                break;
            case 5:
                LogLog.debug("Appender [" + this.name + "] to be rolled at start of every month.");
                break;
            default:
                LogLog.warn("Unknown periodicity for appender [" + this.name + "].");
        }
    }

    int computeCheckPeriod()
    {
        RollingCalendar rollingCalendar = new RollingCalendar(gmtTimeZone, Locale.ENGLISH);

        Date epoch = new Date(0L);
        if (this.datePattern != null)
            for (int i = 0; i <= 5; ++i) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(this.datePattern);
                simpleDateFormat.setTimeZone(gmtTimeZone);
                String r0 = simpleDateFormat.format(epoch);
                rollingCalendar.setType(i);
                Date next = new Date(rollingCalendar.getNextCheckMillis(epoch));
                String r1 = simpleDateFormat.format(next);

                if ((r0 != null) && (r1 != null) && (!(r0.equals(r1))))
                    return i;
            }


        return -1;
    }

    synchronized void rollOver()
            throws IOException
    {
        if (this.datePattern == null) {
            this.errorHandler.error("Missing DatePattern option in rollOver().");
            return;
        }

        String datedFilename = this.fileNameOrig + this.sdf.format(this.now);

        if (this.scheduledFilename.equals(datedFilename)) {
            return;
        }

        closeFile();

        this.fileName = datedFilename;
        try
        {
            setFile(this.fileName, true, this.bufferedIO, this.bufferSize);
        } catch (IOException e) {
            this.errorHandler.error("setFile(" + this.fileName + ", false) call failed.");
        }
        this.scheduledFilename = datedFilename;

        Date dateToRm = this.rc.getDeltaDate(this.now, -this.logRemain);
        String toRemoveBefore = this.fileNameOrig + this.sdf.format(dateToRm);
        log.info("before "+toRemoveBefore+" file will moved to his");
        File dir = new File(this.fileName).getAbsoluteFile().getParentFile();

        File[] current = dir.listFiles();

        FileUtils.makeDirectoryPath(this.hisDir);

        if ((current != null) && (current.length > 0)) {
            for (int j = 0; j < current.length; ++j) {
                String name = current[j].getPath().replaceAll("\\\\", "/");
                if(name.contains("..")){
                    name = name.substring(name.indexOf(".."));
                }
                log.debug("will move to his fileName:"+name+"  OrigName:"+this.fileNameOrig+" compareFileName "+toRemoveBefore+" if_result:"+((name.startsWith(this.fileNameOrig)) && (name.compareTo(toRemoveBefore) < 0)));
                if ((name.startsWith(this.fileNameOrig)) && (name.compareTo(toRemoveBefore) < 0))
                {
                    try {
                        String filename = this.hisDir + "/" + current[j].getName();
                        File f = new File(filename);
                        if(current[j].length()==0){
                            boolean b = current[j].delete();
                            log.debug("remove  " + current[j].getName() + " result "+b);
                        }else {
                            if (current[j].renameTo(f)) {
                                log.debug("removed " + name + " to his dir");
                            } else {
                                log.debug("remove " + name + " to his dir fault.");
                            }
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    protected synchronized void subAppend(LoggingEvent event){
        long n = System.currentTimeMillis();
        if (n >= this.nextCheck) {
            if(null != event && layout instanceof LogLayout && event instanceof LogEvent && null !=((LogEvent)event).getHeader()){
                ((LogLayout)layout).setHeader(((LogEvent)event).getHeader()+"\r\n");
            }
            this.now.setTime(n);
            if (this.interval > 0)
                this.nextCheck = this.rc.getNextCheckDate(this.now, this.interval).getTime();
            else
                this.nextCheck = this.rc.getNextCheckMillis(this.now);
            try
            {
                rollOver();
            } catch (IOException ioe) {
                LogLog.error("rollOver() failed.", ioe);
            }
        }
        if(null != event)
            super.subAppend(event);
    }

    public synchronized void setFile(String fileName, boolean append, boolean bufferedIO, int bufferSize)
            throws IOException
    {
        File f = new File(fileName);
        LogLog.debug("setFile called: " + fileName + ", " + append);
        if (bufferedIO) {
            setImmediateFlush(false);
        }
        reset();
        FileOutputStream ostream = null;
        try
        {
            ostream = new FileOutputStream(f, append);
        }
        catch (FileNotFoundException ex)
        {
            String parentName = f.getParent();
            if (parentName != null)
            {
                File parentDir = new File(parentName);
                if ((!parentDir.exists()) && (parentDir.mkdirs())) {
                    ostream = new FileOutputStream(f, append);
                } else {
                    throw ex;
                }
            }
            else
            {
                throw ex;
            }
        }
        Writer fw = createWriter(ostream);
        if (bufferedIO) {
            fw = new BufferedWriter(fw, bufferSize);
        }
        setQWForFiles(fw);
        this.fileName = fileName;
        this.fileAppend = append;
        this.bufferedIO = bufferedIO;
        this.bufferSize = bufferSize;
        if(f.exists() && FileUtils.isEmpty(f))
            writeHeader();
        LogLog.debug("setFile ended");
    }
}
