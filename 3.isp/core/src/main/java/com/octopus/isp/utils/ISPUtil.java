package com.octopus.isp.utils;

import com.octopus.utils.alone.SNUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.thread.ds.TimeTask;
import com.octopus.utils.xml.XMLObject;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Administrator
 * Date: 14-10-25
 * Time: 下午4:45
 */
public class ISPUtil {

    public static List<TimeTask[]>[] getDependsDelFrom(List list){
        List<Object[]> li = new ArrayList();
        TimeTask[] os1 = new TimeTask[1];
        TimeTask[] os2 = new TimeTask[1];
        TimeTask[] os3 = new TimeTask[1];
        for(int i=list.size()-1;i>=0;i--){
            Object o = list.get(i);
            if(o instanceof XMLObject){
                String ds = ((XMLObject) o).getXML().getProperties().getProperty("key");
                if( StringUtils.isNotBlank(ds) && (ds.equals("initstockinfofromdb"))){
                    list.remove(o);
                    os1[0]=new TimeTask(((XMLObject) o).getXML().getProperties().getProperty("worktime"),o);
                }
                if(StringUtils.isNotBlank(ds) && ds.equals("getstockinfo")){
                    list.remove(o);
                    os2[0]=new TimeTask(((XMLObject) o).getXML().getProperties().getProperty("worktime"),o);
                }
                if(StringUtils.isNotBlank(ds) && ds.equals("getstockdata")){
                    list.remove(o);
                    os3[0]=new TimeTask(((XMLObject) o).getXML().getProperties().getProperty("worktime"),o);
                }
            }
        }
        if(null != os1[0])
        li.add(os1);
        if(null != os2[0])
        li.add(os2);
        if(null != os3[0])
        li.add(os3);

        return new List[]{li};

        /*Map<String,List<String>> dks = new HashMap<String, List<String>>();
        for(int i=0;i<list.size();i++){
            Object o = list.get(i);
            if(o instanceof XMLObject){
                String ds = ((XMLObject) o).getXML().getProperties().getProperty("depends");
                if( StringUtils.isNotBlank(ds)){
                    String k = ((XMLObject) o).getXML().getProperties().getProperty("key");
                    if(StringUtils.isBlank(k)){
                        k = ((XMLObject) o).getXML().getProperties().getProperty("id");
                    }
                    if(StringUtils.isNotBlank(k)){
                        if(!dks.containsKey(k)) dks.put(k,new ArrayList<String>());
                        String[] sds = ds.split(",");
                        for(String d:sds)
                            dks.get(k).add(d);
                    }
                }
            }
        }*/

    }
    static String split="`";
    public static String getRequestId(String type,String insid,String user,String srvName){
        StringBuffer sb = new StringBuffer();

        sb.append(type).append(split).append(insid).append(split).append(user).append(split).append(srvName).append(split).append(SNUtils.getNewId(split));
        return sb.toString();
        /*if(StringUtils.isNotBlank(user)){
            user=user+"-";
        }else{
            user="";
        }
        if(StringUtils.isNotBlank(srvname)){
            srvname=srvname+"-";
        }*/

    }


}
