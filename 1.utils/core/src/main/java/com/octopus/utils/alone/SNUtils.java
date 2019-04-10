package com.octopus.utils.alone;



import com.octopus.utils.net.NetUtils;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;


public class SNUtils {

	private static Properties sortCodeProperties = null;
	private static double radom = Math.random();
    static Random rm = new Random();
    private static AtomicLong point = new AtomicLong(0);
    private static String ip=null;

	public static synchronized String generatorSimpleUnionCode (String regionId, String module,int numlen){
		StringBuffer buffer = new StringBuffer();	
		long pre = Math.round(radom*100);
		if(pre<10){
			pre = pre*10;
		}
        buffer.append(String.valueOf(pre));
		buffer.append(regionId);        		
		buffer.append(module);
		String tmpStr = getNewId();
		int len = numlen - tmpStr.length();
		if(len>0){
            while (len > 0) {
                buffer.append("0");
                len = len - 1;
            }
        }else{
            tmpStr = tmpStr.substring(tmpStr.length()-numlen);
        }

		buffer.append(tmpStr);
		return buffer.toString().toUpperCase();
	}

    public static synchronized String generatorRandomNum(int numlen){
        double pross = (1 + rm.nextDouble()) * Math.pow(10, numlen);
        String fixLenthString = String.valueOf(pross);
        return fixLenthString.substring(1, numlen + 1);
    }

	
	public static String getUUID(){   
        String s = UUID.randomUUID().toString();   
        return s.substring(0,8)+s.substring(9,13)+s.substring(14,18)+s.substring(19,23)+s.substring(24);
    }  		
	

    public static Timestamp getSysDate(){       
       return new Timestamp(new Date().getTime());       
    }
    
    public static synchronized String getNewId(){
        if(ip==null){
            ip=NetUtils.getip();
            ip = ip.replaceAll("\\.","");
        }

    	return ""+ip+"_"+Thread.currentThread().getName()+"_"+System.currentTimeMillis()+"_"+getSequenceNum();
    }
    public static synchronized String getNewId(String splitChar){
        if(ip==null){
            ip=NetUtils.getip();
        }

        return ""+ip+splitChar+Thread.currentThread().getName()+splitChar+System.currentTimeMillis()+splitChar+getSequenceNum();
    }
    public static long getSequenceNum(){
        long l = point.addAndGet(1);
        if(l >999999999){
            point.set(0);
        }
        return l;
    }

    public static void main(String[] args){
        try{
            String s = SNUtils.generatorRandomNum(6);
            System.out.println(s);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
