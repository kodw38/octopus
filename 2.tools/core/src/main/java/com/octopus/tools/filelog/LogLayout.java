package com.octopus.tools.filelog;

import org.apache.log4j.PatternLayout;
import org.apache.log4j.spi.LoggingEvent;

/**
 * User: wfgao_000
 * Date: 16-6-4
 * Time: 上午8:20
 */
public class LogLayout extends PatternLayout {
    String header;
    public void setHeader(String header){
        this.header=header;
    }
    public String getHeader(){
        return header;
    }
    public String format(LoggingEvent event)
    {
        try{
            //String s = StringUtils.trumUTF8(super.format(event));
            String s = super.format(event);
            //String ts=new String(s.getBytes("utf-8"),"WINDOWS-1250");
        //    System.out.println(s+ "    "+ts);
        return s;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
