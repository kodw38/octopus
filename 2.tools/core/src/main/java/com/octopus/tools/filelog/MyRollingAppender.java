package com.octopus.tools.filelog;
/**
 * @program: treasurebag
 * @description: add
 * @author: liu yan
 * @create: 2021-05-04 08:01
 */

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.file.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

import java.io.*;

/**
 *@program: treasurebag
 *@description: add
 *@author: wangfeng2
 *@create: 2021-05-04 08:01
 */
public class MyRollingAppender extends FileAppender {
    transient static Log log = LogFactory.getLog(MyRollingAppender.class);
    private static final java.util.Timer TIMER = new java.util.Timer(true);

    String curDir;
    String hisDir;
    RollEvent rollEvent;
    int remainSize;
    String curFileName;
    LogFileName logFileName;


    public MyRollingAppender(Layout layout, String curDir, String hisDir, LogFileName logFileName
            , RollEvent rollEvent, int remainSize) throws Exception
    {
        this.curDir = curDir.replaceAll("\\\\","/");
        this.hisDir=hisDir.replaceAll("\\\\","/");
        this.logFileName =logFileName;
        this.rollEvent=rollEvent;
        this.remainSize=remainSize;

        this.layout = layout;
        curFileName = getCurFileName(curDir);
        setFile(curFileName, true, false, this.bufferSize);

    }

    public boolean isFirstLog(){
        return rollEvent.isFirst();
    }

    public String getCurFileName(String curDir)throws Exception{
        String oldName = logFileName.getCurrentFileName();
        if(StringUtils.isNotEmpty(oldName)) {
            boolean isover = rollEvent.isStartOver(curDir+"/"+oldName);
            if(!isover){
                return curDir+"/"+oldName;
            }else{
                moveHis(new File(curDir+"/"+oldName));
            }
        }
        return getNewFileName();

    }
    public String getNewFileName()throws Exception{
        return curDir+"/"+ logFileName.getNewFileName();
    }




    synchronized void rollOver()
            throws Exception
    {


        String filename = getNewFileName();

        if (filename.equals(curFileName)) {
            return;
        }

        closeFile();
        curFileName=filename;
        try
        {
            setFile(this.curFileName, true, this.bufferedIO, this.bufferSize);
        } catch (IOException e) {
            this.errorHandler.error("setFile(" + this.fileName + ", false) call failed.");
        }

        File dir = new File(this.curFileName).getAbsoluteFile().getParentFile();

        File[] listFiles = dir.listFiles();



        if ((listFiles != null) && (listFiles.length > 0)) {
            for (int j = 0; j < listFiles.length; ++j) {
                String name = listFiles[j].getPath().replaceAll("\\\\", "/");
                if(name.contains("..")){
                    name = name.substring(name.indexOf(".."));
                }
                moveHis(listFiles[j]);
            }
        }
    }

    void moveHis(File srcFile){
        if (!logFileName.isHistoryName(srcFile.getName()))
        {
            try {
                String hisfilename = this.hisDir + "/" + logFileName.getHistoryName(srcFile.getName());
                FileUtils.makeDirectoryPath(this.hisDir);
                File f = new File(hisfilename);
                if(srcFile.length()==0){
                    boolean b = srcFile.delete();
                }else {
                    if (srcFile.renameTo(f)) {
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
    protected synchronized void subAppend(LoggingEvent event){
        rollEvent.doOne(event);
        if (rollEvent.isOver(curFileName)) {
            if(null != event && layout instanceof LogLayout && event instanceof LogEvent && null !=((LogEvent)event).getHeader()){
                ((LogLayout)layout).setHeader(((LogEvent)event).getHeader()+"\r\n");
            }
            try
            {
                rollOver();
            } catch (Exception ioe) {
                log.error("rollOver() failed.", ioe);
            }
        }
        if(null != event)
            super.subAppend(event);
    }

    public synchronized void setFile(String fileName, boolean append, boolean bufferedIO, int bufferSize)
            throws IOException
    {
        File f = new File(fileName);
        log.debug("setFile called: " + fileName + ", " + append);
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
        log.debug("setFile ended");
    }
}
