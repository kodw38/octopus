package com.octopus.tools.filelog;

/**
 * User: wfgao_000
 * Date: 15-11-20
 * Time: 下午10:15
 */

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.helpers.AppenderAttachableImpl;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Properties;

public class SaveLogger extends Logger
{
    private static Log log = LogFactory.getLog(SaveLogger.class);
    public static String ConfigFile = "logfile.properties";
    private static String FILE_SUFFIX = "pattern";
    private static String FILE_NAME = "fileName";
    private static String RUNING_DIR = "runingDirectory";
    private static String RECORD_TEMPLATE = "record";
    private static String REMAIN_TIME = "remaintime";
    private static String SPLIT_TIME = "splittime";
    public static String HIS_DIR = "historyDirectory";
    private ConsoleAppender consoleAppender = new ConsoleAppender();
    private DailyNoRollingFileAppender fileAppender =null;
    private AppenderAttachableImpl aai = new AppenderAttachableImpl();
    protected static String DD_PATTERN = "'.'yyyy-MM-dd";
    protected static String AMPM_PATTERN = "'.'yyyy-MM-dd-a";
    protected static String HH_PATTERN = "'.'yyyy-MM-dd-HH";
    protected static String MM_PATTERN = "'.'yyyy-MM-dd-HH-mm";
    public static int logRemain = 180;
    private static int interval = -1;
    private Boolean isInit = Boolean.FALSE;
    String code;
    private SaveLogger(String name)
    {
        super(name);
        setLevel(Level.INFO);
        if (log.isDebugEnabled())
            this.aai.addAppender(this.consoleAppender);
    }

    private void initConfiguration(String code,String runningPath, String hisPath, String fileName, String pattern, int remainTime, int splitTime,String header,String fileEncoding)
    {
        Properties p = null;
        this.code=code;
        if (StringUtils.isBlank(pattern)) {
            pattern = System.getProperty(FILE_SUFFIX);
            if (StringUtils.isBlank(pattern))
                try {
                    p = new Properties();
                    p.load(getClass().getClassLoader().getResourceAsStream(ConfigFile));
                    pattern = p.getProperty(FILE_SUFFIX);
                } catch (Exception e) {
                    log.error(e);
                }
        }

        if (StringUtils.isBlank(fileName)) {
            fileName = System.getProperty(FILE_NAME);
            if (StringUtils.isBlank(fileName))
                try {
                    if (null == p) {
                        p = new Properties();
                        p.load(getClass().getClassLoader().getResourceAsStream(ConfigFile));
                    }
                    fileName = p.getProperty(FILE_NAME);
                } catch (Exception e) {
                    log.error(e);
                }
        }

        if (StringUtils.isBlank(runningPath)) {
            runningPath = System.getProperty(RUNING_DIR);
            if (StringUtils.isBlank(runningPath))
                try {
                    if (null == p) {
                        p = new Properties();
                        p.load(getClass().getClassLoader().getResourceAsStream(ConfigFile));
                    }
                    runningPath = p.getProperty(RUNING_DIR);
                } catch (Exception e) {
                    log.error(e);
                }
        }

        if (StringUtils.isBlank(hisPath)) {
            hisPath = System.getProperty(HIS_DIR);
            if (StringUtils.isBlank(hisPath))
                try {
                    if (null == p) {
                        p = new Properties();
                        p.load(getClass().getClassLoader().getResourceAsStream(ConfigFile));
                    }
                    hisPath = p.getProperty(HIS_DIR);
                } catch (Exception e) {
                    log.error(e);
                }
        }

        if (remainTime < 0) {
            if (!(StringUtils.isBlank(System.getProperty(REMAIN_TIME))))
                remainTime = Integer.parseInt(System.getProperty(REMAIN_TIME));

            if (remainTime < 0)
                try {
                    if (null == p) {
                        p = new Properties();
                        p.load(getClass().getClassLoader().getResourceAsStream(ConfigFile));
                    }
                    if (!(StringUtils.isBlank(p.getProperty(REMAIN_TIME))))
                        remainTime = Integer.parseInt(p.getProperty(REMAIN_TIME));
                }
                catch (Exception e) {
                    log.error(e);
                }
        }

        if (splitTime < 0) {
            if (!(StringUtils.isBlank(System.getProperty(SPLIT_TIME))))
                splitTime = Integer.parseInt(System.getProperty(SPLIT_TIME));

            if (splitTime < 0)
                try {
                    if (null == p) {
                        p = new Properties();
                        p.load(getClass().getClassLoader().getResourceAsStream(ConfigFile));
                    }
                    if (!(StringUtils.isBlank(p.getProperty(SPLIT_TIME))))
                        splitTime = Integer.parseInt(p.getProperty(SPLIT_TIME));
                }
                catch (Exception e) {
                    log.error(e);
                }
        }

        fileName = runningPath + File.separator + fileName;

        String appServerName = System.getProperty("appframe.server.name", "");
        if (StringUtils.isNotBlank(appServerName)) {
            fileName = fileName + "." + appServerName;
        }

        if (!(StringUtils.isBlank(super.getName()))) {
            fileName = fileName + "." + super.getName();
        }

        if (this.isInit.equals(Boolean.FALSE)) {
            LogLayout layout = new LogLayout();
            layout.setHeader(header);
            layout.setConversionPattern("%m%n");
            if (log.isDebugEnabled()) {
                this.consoleAppender.setName("ConsoleAppender");
                this.consoleAppender.setLayout(layout);
                this.consoleAppender.setWriter(new OutputStreamWriter(System.out));
            }

            try
            {
                this.fileAppender = new DailyNoRollingFileAppender(layout, fileName, pattern, splitTime, remainTime,hisPath);
                if(StringUtils.isNotBlank(fileEncoding))
                    this.fileAppender.setEncoding(fileEncoding);
            } catch (IOException e) {
                log.error("Setting DailyRollingFileAppender Error: ", e);
            }
            this.fileAppender.setName("FileAppender");
            this.aai.addAppender(this.fileAppender);
            this.isInit = Boolean.TRUE;
        }
    }


    public static int getPrefixLength(String filename)
    {
        if (filename == null)
            return -1;

        int len = filename.length();
        if (len == 0)
            return 0;

        char ch0 = filename.charAt(0);
        if (ch0 == ':')
            return -1;

        if (len == 1) {
            if (ch0 == '~')
                return 2;

            return ((isSeparator(ch0)) ? 1 : 0);
        }
        if (ch0 == '~') {
            int posUnix = filename.indexOf(47, 1);
            int posWin = filename.indexOf(92, 1);
            if ((posUnix == -1) && (posWin == -1))
                return (len + 1);

            posUnix = (posUnix == -1) ? posWin : posUnix;
            posWin = (posWin == -1) ? posUnix : posWin;
            return (Math.min(posUnix, posWin) + 1);
        }
        char ch1 = filename.charAt(1);
        if (ch1 == ':') {
            ch0 = Character.toUpperCase(ch0);
            if ((ch0 >= 'A') && (ch0 <= 'Z')) {
                if ((len == 2) || (!(isSeparator(filename.charAt(2)))))
                    return 2;

                return 3;
            }
            return -1;
        }
        if ((isSeparator(ch0)) && (isSeparator(ch1))) {
            int posUnix = filename.indexOf(47, 2);
            int posWin = filename.indexOf(92, 2);
            if (((posUnix == -1) && (posWin == -1)) || (posUnix == 2) || (posWin == 2))
                return -1;

            posUnix = (posUnix == -1) ? posWin : posUnix;
            posWin = (posWin == -1) ? posUnix : posWin;
            return (Math.min(posUnix, posWin) + 1);
        }
        return ((isSeparator(ch0)) ? 1 : 0);
    }

    public static int indexOfLastSeparator(String filename)
    {
        if (filename == null)
            return -1;

        int lastUnixPos = filename.lastIndexOf(47);
        int lastWindowsPos = filename.lastIndexOf(92);
        return Math.max(lastUnixPos, lastWindowsPos);
    }

    public static String getPrefix(String filename)
    {
        if (filename == null)
            return null;

        int len = getPrefixLength(filename);
        if (len < 0)
            return null;

        if (len > filename.length())
            return filename + '/';

        return filename.substring(0, len);
    }

    private static boolean isSeparator(char ch) {
        return ((ch == '/') || (ch == '\\'));
    }

    private static String getFullPath(String filename, boolean includeSeparator)
    {
        if (filename == null)
            return null;

        int prefix = getPrefixLength(filename);
        if (prefix < 0)
            return null;

        if (prefix >= filename.length()) {
            if (includeSeparator)
                return getPrefix(filename);

            return filename;
        }

        int index = indexOfLastSeparator(filename);
        if (index < 0)
            return filename.substring(0, prefix);

        int end = index + ((includeSeparator) ? 1 : 0);
        return filename.substring(0, end);
    }

    public static String getName(String filename)
    {
        if (filename == null)
            return null;

        int index = indexOfLastSeparator(filename);
        return filename.substring(index + 1);
    }

    public static SaveLogger getMyLogger(String code,String runningPath, String hisPath, String fileName, String suffix, String pattern, int remainTime, int splitTime,String header,String encoding)
    {
        SaveLogger t = new SaveLogger(suffix);
        t.initConfiguration(code,runningPath, hisPath, fileName, pattern, remainTime, splitTime,header,encoding);
        return t;
    }

    public static String[] getParamFromString(String aSourceString, String aStartStr, String aEndStr) {
        aSourceString = aSourceString + aEndStr;
        String strSource = aSourceString;
        ArrayList strKey = new ArrayList();
        int iStartIndex = strSource.indexOf(aStartStr);
        int iStartLength = aStartStr.length();
        int iEndLength = aEndStr.length();
        String strTemp = "";
        strTemp = strSource.substring(iStartIndex + iStartLength, strSource.length());
        int iEndIndex = strTemp.indexOf(aEndStr) + strSource.substring(0, iStartIndex + iStartLength).length();
        if (iEndIndex == iStartIndex)
            strKey.add(strTemp);

        while ((iStartIndex != -1) && (iEndIndex != -1) && (iStartIndex < iEndIndex)) {
            strTemp = strSource.substring(iStartIndex + iStartLength, iEndIndex);
            strKey.add(strTemp);
            strSource = strSource.substring(iEndIndex + iEndLength, strSource.length());
            iStartIndex = strSource.indexOf(aStartStr);
            strTemp = strSource.substring(iStartIndex + iStartLength, strSource.length());
            iEndIndex = strTemp.indexOf(aEndStr) + strSource.substring(0, iStartIndex + iStartLength).length();
        }
        return ((String[])(String[])strKey.toArray(new String[0]));
    }

    public void addLog(String content,String header)
    {
        LogEvent event = new LogEvent(code, this, Level.INFO, content, null,header);
        synchronized (this) {
            if (this.aai != null)
                this.aai.appendLoopOnAppenders(event);
        }
    }


}