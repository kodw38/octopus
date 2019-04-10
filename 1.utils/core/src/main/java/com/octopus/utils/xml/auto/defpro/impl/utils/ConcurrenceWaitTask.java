package com.octopus.utils.xml.auto.defpro.impl.utils;

import com.octopus.utils.thread.ThreadPool;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * User: wfgao_000
 * Date: 15-5-6
 * Time: 下午3:24
 */
public class ConcurrenceWaitTask implements Runnable{
    static transient Log log = LogFactory.getLog(ConcurrenceWaitTask.class);
    int total =0;
    AtomicInteger finishedsize=new AtomicInteger(0);
    AtomicInteger handleSize = new AtomicInteger(0);
    AtomicInteger addSeize = new AtomicInteger(0);
    int threadNum;
    boolean iswait;
    List<Runnable> finishedListeners = new ArrayList<Runnable>();
    Object[] queue = null;
    Object lock = new Object();
    private final ReentrantLock takeLock = new ReentrantLock();
    //private final Condition notEmpty = takeLock.newCondition();
    String mainThreadName;
    ThreadPool threadPool;
    String threadPoolName;
    //ExecutorService threadPool;
    public ConcurrenceWaitTask(String name,int size,int threadnum,boolean iswait){
        threadPoolName=name;
        total=size;
        queue = new Object[total+1];
        //System.out.println(Thread.currentThread().getName()+" queue new "+total);
        threadNum=threadnum;
        this.iswait = iswait;
    }

    public void put(Object o){
        //mainThreadName=Thread.currentThread().getName();
        if(addSeize.intValue()<total){
            queue[addSeize.intValue()]=o;
            addSeize.addAndGet(1);
            //System.out.println(Thread.currentThread().getName()+" queue put "+addSeize.intValue());
        /*    takeLock.lock();
            try{
                notEmpty.signalAll();
            } catch (Exception e) {
                log.error(e);
            }finally {
                takeLock.unlock();
            }*/
        }

        try{
            if(iswait && total==addSeize.intValue()){
                synchronized (lock){
                    //log.error("---==================");
                    lock.wait();
                }
            }
        }catch (Exception e){
             e.printStackTrace();
        }
    }
    public void start(){
        threadPool = ThreadPool.getInstance().getThreadPool(threadPoolName,threadNum+1);
        try {
            threadPool.getExecutor().execute(this);
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        //threadPool.getExecutor().execute(this);
    }

    @Override
    public void run() {
        try{
            while(true){
                //System.out.println(Thread.currentThread().getName()+addSeize.intValue()+" "+handleSize+" "+queue[handleSize]);
                if(addSeize.intValue()>handleSize.intValue()){
                    Object o = queue[handleSize.intValue()];
                    if(o instanceof String && o.equals("^exit")){
                        addSeize.set(0);
                        total=0;
                        finishedsize.set(0);
                        handleSize.set(0);
                        threadNum=0;
                        queue=null;
                        if(iswait){
                            synchronized (lock){
                                lock.notifyAll();
                                //log.error("addSeize finished");
                            }
                            //threadPool.shutdown();
                        }

                        doFinishedListener();
                        iswait=false;
                        //System.out.println(Thread.currentThread().getName()+"--R--break");
                        break;
                    }
                    //log.error("defeat "+handleSize.intValue());
                    //new Thread(new exerun(this,(Object[])o)).start();
                    threadPool.getExecutor().execute(new exerun(this,(Object[])o));
                    handleSize.incrementAndGet();
                }else{
                    //takeLock.lock();
                    Thread.sleep(1);
                    //log.error("addSeize:"+addSeize.intValue()+" handleSize:"+handleSize.intValue()+" total:"+total);
                    //takeLock.unlock();
                }
            }
        }catch (Exception e){
            log.error("concurrent error",e);
        }finally {
            ThreadPool.getInstance().returnThreadPool(threadPool);
        }
    }
    void finished(){
        queue[addSeize.intValue()]="^exit";
        addSeize.addAndGet(1);
        //notEmpty.signalAll();

    }
    class exerun extends ConcurrenceExeRun{
        ConcurrenceWaitTask parent;
        public exerun(ConcurrenceWaitTask o,Object[] os) {
            super(os);
            parent=o;
            //this.ex=e;
        }

        public void run(){
            try{
                doAction();
            }catch (Exception e){
                log.error("concurrent wait task error",e);
            }finally {
                takeLock.lock();
                try{
                    finishedsize.addAndGet(1);
                    //System.out.println("=="+finishedsize.intValue());
                    if(finishedsize.intValue()==total){
                        finished();
                    }
                }finally {
                    takeLock.unlock();
                }

            }
        }
    }

    public void addFinishedListener(Runnable run){
        finishedListeners.add(run);
    }
    void doFinishedListener(){
        for(Runnable r:finishedListeners){
            r.run();
        }
    }
}
