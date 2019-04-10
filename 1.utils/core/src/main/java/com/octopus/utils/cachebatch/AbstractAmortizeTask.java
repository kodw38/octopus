package com.octopus.utils.cachebatch;

import java.util.List;

/**
 * Amortize数据处理的任务抽象类
 * 通过该类处理数据，该类是单独的线程处理数据的，处理完线程结束
 * 具体的任务类需要继承该类
 * User: robai
 * Date: 2009-9-9
 * Time: 20:34:35
 */
public abstract class AbstractAmortizeTask extends Thread{
	
    private List li ;
    String taskName = null;
    public AbstractAmortizeTask(){
        setDaemon(true);
    }
    public void setTaskName(String name){
        this.taskName = name;
    }
    public String getTaskName(){
        return taskName;
    }
    /**
     * @Function: doTask
     * @Description: 任务方法
     * @param datas
     * @return：返回结果描述
     * @author: robai
     * @date: 2011-3-26 下午09:49:04
     */
    public void doTask(List datas){
        li = datas;
        this.start();
    }
    /**
     * 执行方法
     */
    public void run(){
        work(li);
        li=null;
    }

    /**
     * @Function: work
     * @Description: 抽象的数据处理方法，由具体的业务实现。
     * @param datas
     * @throws Exception
     * @return：返回结果描述
     * @author: robai
     * @date: 2011-3-26 下午09:50:27
     */
    public abstract void work(List datas);
}
