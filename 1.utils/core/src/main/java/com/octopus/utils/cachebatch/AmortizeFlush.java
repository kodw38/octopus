package com.octopus.utils.cachebatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * 该类是主功能类
 * 管理批量定时保存的数据对象，并定时调用任务保存这些对象，之后清空这些对象中的数据。
 * 定时任务启动条件为，间隔（分钟），和对象容器中的记录条数。
 * 一条总线程控制数据是否满足执行条件和管理这些数据容器，满足条件的数据交给单独子线程执行，子线程在一次执行结束后结束。
 * User: robai
 * Date: 2009-9-9
 * Time: 21:10:51
 */
public class AmortizeFlush extends Thread{
	
	private static final Log logger = LogFactory.getLog(AmortizeFlush.class);

    HashMap amortizeContainerList = new HashMap();
    
    /**
     * @Function: addAmortizeContainer
     * @Description: 增加一个数据容器，key保持唯一
     * @param key
     * @param container
     * @return：返回结果描述
     * @author: robai
     * @date: 2011-3-26 下午08:47:51
     */
    public void addAmortizeContainer(String key,AmortizeContainer container){
    	synchronized (amortizeContainerList) {
    		amortizeContainerList.put(key,container);    
		}        
    }
    
    /**
     * @Function: getAmortizeContainer
     * @Description: 获取一个数据容器
     * @param key
     * @return
     * @return：返回结果描述
     * @author: robai
     * @date: 2011-3-26 下午09:10:20
     */
    
    public AmortizeContainer getAmortizeContainer(String key){
    	synchronized (amortizeContainerList) {
    		return (AmortizeContainer)amortizeContainerList.get(key);
		}        
    }

    /**
     * @Function: flushData
     * @Description: 一个数据容器中的数据到指定任务中执行。
     * @param key
     * @return：返回结果描述
     * @author: robai
     * @date: 2011-3-26 下午09:10:47
     */
    public void flushData(String key){
        synchronized(this){
            AmortizeContainer container = (AmortizeContainer)amortizeContainerList.get(key);
            if(null == container){
            	logger.error("The amortizeContainer key:"+key+"  is not exist.");
//            	LogUtil.error(log, "The amortizeContainer key:"+key+"  is not exist.", null, null);
            }
            doData(key,container);
        }
    }

    /**
     * @Function: flushAll
     * @Description: 把所有容器数据交给任务立即执行
     * @return
     * @throws Exception
     * @return：返回结果描述
     * @author: robai
     * @date: 2011-3-26 下午09:17:27
     */
    public int flushAll(){
        if(null != amortizeContainerList){
            Iterator its = amortizeContainerList.keySet().iterator();
            String key ;
            while(its.hasNext()){
                key = (String)its.next();
                AmortizeContainer container = (AmortizeContainer)amortizeContainerList.get(key);
                if(container.getDatas().size()>0){
                    return doData(key,container);
                }
            }
        }
        return 0;
    }

    /**
     * 定时执行任务，按时间间隔（分），记录数
     */
    public void run(){
        while(true){
            synchronized(amortizeContainerList){            	
	            Iterator keys = amortizeContainerList.keySet().iterator();
	            Date currentDate = new Date();
	            while(keys.hasNext()){
                    String key = (String)keys.next();
	            	AmortizeContainer ac = (AmortizeContainer)amortizeContainerList.get(key);
	                if(ac.getDatas().size()>0){
	                    if((DateTimeUtil.addOrMinusMinutes(ac.getStartTime().getTime(),ac.getTimespace())).before(currentDate) ){
	                    	doData(key,ac);
	                    }
	                }
	            	
	            }
            }
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {

            }
        }
    }

    /**
     * @Function: doData
     * @Description: 对一个数据容器执行任务调用
     * @param data
     * @return
     * @return：返回结果描述
     * @author: robai
     * @date: 2011-3-26 下午09:29:27
     */
    public static int doData(String key,AmortizeContainer data){
        int ret = 0;
        Date currentDate = new Date();
        List li =  new ArrayList();
        synchronized(data){
            li.addAll(data.getDatas());
            ret = li.size();
            data.clearDatas();
            data.setStartTime(currentDate);
        }
        
        try {
            AbstractAmortizeTask task = ((AbstractAmortizeTask)data.getWorkTaskClass().newInstance());
            task.setTaskName(key);
            task.doTask(li);
		} catch (InstantiationException e) {
			logger.error(data.getWorkTaskClass()+" is not "+AbstractAmortizeTask.class.getName()+" instance.",e);
//			LogUtil.error(log, data.getWorkTaskClass()+" is not "+AbstractAmortizeTask.class.getName()+" i;nstance.", null,e);
		} catch (IllegalAccessException e) {
			logger.error(data.getWorkTaskClass()+" is not "+AbstractAmortizeTask.class.getName()+" instance.",e);
//			LogUtil.error(log, data.getWorkTaskClass()+" is not "+AbstractAmortizeTask.class.getName()+" instance.", null,e);
		}
        
        return ret;
    }
    
}
