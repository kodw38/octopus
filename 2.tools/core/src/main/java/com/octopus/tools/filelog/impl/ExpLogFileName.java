package com.octopus.tools.filelog.impl;/**
 * @program: treasurebag
 * @description: add
 * @author: liu yan
 * @create: 2021-05-04 09:20
 */

import com.octopus.tools.filelog.LogFileName;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.file.FileUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 *@program: treasurebag
 *@description: add
 *@author: wangfeng2
 *@create: 2021-05-04 09:20
 */
public class ExpLogFileName implements LogFileName {
    String curKey;
    String hisKey;
    String curDir;
    SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmm");
    AtomicLong point = new AtomicLong(0);
    String curyyyymmdd=null;
    public ExpLogFileName(String curDir,String curKey, String hisKey){
        this.curKey=curKey;
        this.hisKey=hisKey;
        curyyyymmdd = format.format(new Date());
        this.curDir=curDir;
        List<File> fs = FileUtils.getSortFiles(curDir,"desc");
        for(File f:fs){
            String name = f.getName();
            if(name.contains(curyyyymmdd) && name.contains(hisKey)){
                int s = name.indexOf(curyyyymmdd)+curyyyymmdd.length()+1;
                int e = name.indexOf(hisKey)-1;
                if(e>s){
                    String n = name.substring(s,e);
                    point.set(Long.parseLong(n));
                    break;
                }
            }
        }
    }
    //Bill_IDIDIDID_YYYYMMDD_XXXXXXXX.bill.tmp
    @Override
    public String getNewFileName() throws Exception{
        String day = format.format(new Date());
        if(!curyyyymmdd.equals(day)){
            point.set(0);
            curyyyymmdd=day;
        }
        return "Bill_IDIDIDID_"+curyyyymmdd+"_"+ StringUtils.leftPad(""+point.addAndGet(1),8,"0") +".bill.tmp";

    }
    @Override
    public String getCurrentFileName(){
        String date = format.format(new Date());
        List<File> fs = FileUtils.getSortFiles(curDir,"desc");
        for(File f:fs){
            if(f.getName().contains(date) && f.getName().indexOf(curKey)>0) {
                return f.getName();
            }
        }
        return null;
    }

    @Override
    public boolean isHistoryName(String name) {
        if(name.indexOf(curKey)>0)
        return false;
        else
            return true;
    }

    @Override
    public String getHistoryName(String fileName) {

        return StringUtils.replace(fileName,curKey,hisKey);
    }
}
