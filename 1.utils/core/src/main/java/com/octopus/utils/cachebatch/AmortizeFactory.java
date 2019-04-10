package com.octopus.utils.cachebatch;

import java.util.Date;

/**
 * User: robai
 * Date: 2009-9-9
 * Time: 20:12:35
 */
public class AmortizeFactory {
    private static final AmortizeFlush flush = new AmortizeFlush();
    static{
        flush.start();/**初始化时启动容器管理器 */
    }
    
    private AmortizeFactory(){}

    /**
     * @Function: addAmortizeContainer
     * @Description: 增加一个数据容器
     * @param key
     * @param timespace
     * @param fullRecCount
     * @param workTaskClass
     * @throws Exception
     * @return：返回结果描述
     * @author: robai
     * @date: 2011-3-26 下午10:07:45
     */
    public static void addAmortizeContainer(String key,int timespace,int fullRecCount,Class workTaskClass)throws Exception{
        if(null == workTaskClass){
            throw new Exception("Amortize Task Class dot not set.");
        }

        if(!AbstractAmortizeTask.class.isAssignableFrom(workTaskClass)){
            throw new Exception(workTaskClass+" is not extends "+ AbstractAmortizeTask.class);
        }
        AmortizeContainer amortizeContainer =new AmortizeContainer(key,timespace,fullRecCount,workTaskClass);
        amortizeContainer.setStartTime(new Date());
        flush.addAmortizeContainer(key,amortizeContainer);
    }


    /**
     * @Function: addData
     * @Description: 向一个容器中增加数据
     * @param key
     * @param data
     * @throws Exception
     * @return：返回结果描述
     * @author: robai
     * @date: 2011-3-26 下午10:08:59
     */
    public static void addData(String key,Object data) throws Exception {
        AmortizeContainer container = (flush.getAmortizeContainer(key.trim()));
        if(null != container){
            container.addObject(data);
        }
    }

    /**
     * @Function: isAmortizeExist
     * @Description: 判断一个关键字容器是否存在
     * @param key
     * @throws Exception
     * @return：返回结果描述
     * @author: robai
     * @date: 2011-3-26 下午10:09:54
     */
    public static boolean isAmortizeExist(String key)throws Exception{        
    	AmortizeContainer container = (flush.getAmortizeContainer(key.trim()));
        if(null != container){
            return true;
        }else{
            return false;
        }
    }

    public static AmortizeContainer getAmortizeContainer(String key)throws Exception{
        return flush.getAmortizeContainer(key) ;   
    }

    public static void flushData(String key)throws Exception{
        flush.flushData(key);
    }

    public static Date getStartDate(String key)throws Exception{
        AmortizeContainer container = (flush.getAmortizeContainer(key.trim()));
        if(null != container){
            return container.getStartTime();
        }else{
            return null;
        }
    }

    /**
     * @Function: flushAllData
     * @Description: 执行所有的数据容器任务
     * @return
     * @throws Exception
     * @return：返回结果描述
     * @author: robai
     * @date: 2011-3-26 下午10:11:01
     */
    public static int flushAllData()throws Exception{
        return flush.flushAll();
    }

}
