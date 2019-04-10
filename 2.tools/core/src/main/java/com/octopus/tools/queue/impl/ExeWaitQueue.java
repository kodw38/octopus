package com.octopus.tools.queue.impl;

import com.octopus.tools.queue.IQueue;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.thread.ThreadPool;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * User: Administrator
 * Date: 14-10-29
 * Time: 上午9:53
 */
public class ExeWaitQueue extends XMLObject implements IQueue {
    LinkedBlockingQueue queue ;
    ThreadPool threadPool;
    boolean isStart;
    boolean isInterrupt;
    public ExeWaitQueue(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        String s = getXML().getProperties().getProperty("size");
        String tn = getXML().getProperties().getProperty("threadnum");
        if(StringUtils.isNotBlank(s)){
             queue=new LinkedBlockingQueue(Integer.parseInt(s));
        }else
            queue=new LinkedBlockingQueue();
        int th;
        if(StringUtils.isNotBlank(tn)){
            th = Integer.parseInt(tn);
        }else
            th=1;

        threadPool= ExecutorUtils.getFixedThreadPool(xml.getId(),th);
    }

    @Override
    public void notifyObject(String op, Object obj) throws Exception {

    }

    @Override
    public void addTrigger(Object cond, InvokeTaskByObjName task) throws Exception {

    }

    @Override
    public void destroy() throws Exception {

    }

    @Override
    public void initial() throws Exception {

    }

    @Override
    public void put(Object o) throws InterruptedException {
        queue.put(o);
    }

    @Override
    public void setThreadNum(int num) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setQueueMax(int num) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object take() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void start() {
        if(!isStart){
            isStart=true;
            isInterrupt=false;
            ExecutorUtils.work(new Runnable() {
                @Override
                public void run() {
                    while (!isInterrupt) {
                        try {
                            threadPool.getExecutor().execute((Runnable) queue.take());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }

    }

    @Override
    public void suspend() {
        isInterrupt=true;
    }

    public void active(){
        isInterrupt=false;
    }
}
