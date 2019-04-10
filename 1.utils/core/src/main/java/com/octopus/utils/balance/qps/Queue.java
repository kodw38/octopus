package com.octopus.utils.balance.qps;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Administrator on 2018/11/6.
 */
public class Queue {
    MyBlockingQueue queue=null;
    TaskData maxCostTask;
    long waitingTime;
    IProcess process;
    AtomicBoolean finish=new AtomicBoolean(true);
    Stat stat=null;
    Thread worker=null;
    Map processingDataCaches;
    public Queue(int size,IProcess process,Stat stat,Map processingDataCaches){
        queue = new MyBlockingQueue(size);
        this.process=process;
        this.stat=stat;
        this.processingDataCaches=processingDataCaches;
        start();
    }

    public boolean isEmpty(){
        return queue.isEmpty();
    }

    public long getWaitingTime(){
        return waitingTime;
    }

    public TaskData getMaxCostTask(){
        return maxCostTask;
    }

    public void put(TaskData data) throws InterruptedException {
        if(maxCostTask==null)maxCostTask=data;
        if(data.getCost()>maxCostTask.getCost())maxCostTask=data;
        waitingTime+=data.getCost();
        queue.put(data);
    }

    public boolean remove(Object o){
        return queue.remove(o);
    }

    public int size(){
        return queue.size();
    }

    public TaskData getLast(){
        return (TaskData)queue.popup();
    }

    void start(){
        worker =new Thread(new Task(this));
        worker.start();
    }
    class Task implements Runnable{
        Queue q;

        transient Log log = LogFactory.getLog(Task.class);
        public Task(Queue q){
            this.q = q;
        }
        @Override
        public void run() {
            while(true) {
                TaskData td=null;
                try {
                    td = (TaskData)q.queue.take();
                    q.finish.set(false);
                    td.setTakeQueueTime(System.currentTimeMillis());
                    q.maxCostTask=null;
                    //process
                    process.process(td.getObj());
                }catch (Exception e){
                    log.error("do with task error",e);
                }finally {
                    if(null != td) {
                        if(null != processingDataCaches){
                            processingDataCaches.remove(td.getId());
                        }
                        q.waitingTime -= td.getCost();
                        td.setFinishedTime(System.currentTimeMillis());
                        stat.putCost(td.getKindKey(),td.getFinishedTime()-td.getTakeQueueTime());
                    }

                    q.finish.set(true);
                    log.error("finished "+td.getId()+" "+q.finish.get()+" "+q.hashCode());
                }
            }
        }
    }
    public boolean isFinished(){
        if(queue.isEmpty() && finish.get()){
            return true;
        }
        return false;
    }
    public boolean waitingFinished(){
        while(!isFinished()){
            try {
                Thread.sleep(1000);
            }catch (Exception e){
                return false;
            }
        }
        return true;
    }
    public boolean shutdown(){
        if(finish.get() && null != worker){
            worker.interrupt();
            return true;
        }
        return false;
    }

}
