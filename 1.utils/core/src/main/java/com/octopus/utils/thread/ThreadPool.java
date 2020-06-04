package com.octopus.utils.thread;

import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.exception.ExceptionUtil;
import com.octopus.utils.thread.ds.InvokeTask;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * User: Administrator
 * Date: 14-9-18
 * Time: 上午10:34
 */
public class ThreadPool {
    transient static Log log = LogFactory.getLog(ThreadPool.class);
    public AtomicInteger MAX_THREAD_NUMBER;
    ConcurrentLinkedQueue<Thread> threads = new ConcurrentLinkedQueue<Thread>();
    DefaultThreadFactory threadFactory=null;
    static List<ThreadPool> subPoolList = new ArrayList<ThreadPool>();
    static ThreadPool instance = new ThreadPool();
    ReentrantLock takeLock = new ReentrantLock();
    Condition lock = takeLock.newCondition();

    public static ThreadPool getInstance(){
        return instance;
    }
    String name;
    ThreadPool(){
        name="Thread pool root";
        threadFactory=new DefaultThreadFactory(false);
        String ts = System.getProperty("maxthreads");
        if(StringUtils.isNotBlank(ts)){
            MAX_THREAD_NUMBER = new AtomicInteger(Integer.parseInt(ts));
        }else{
            MAX_THREAD_NUMBER = new AtomicInteger(Runtime.getRuntime().availableProcessors()*2000);
        }
        System.out.println("system thread pool size is "+MAX_THREAD_NUMBER);

    }

    ThreadPool(String na,int count){
        this.name=na;
        threadFactory=new DefaultThreadFactory(true);
        MAX_THREAD_NUMBER= new AtomicInteger(count);

    }
    public String getName(){
        return name;
    }
    public void returnThreadPool(ThreadPool pool){
        if(log.isDebugEnabled()){
            log.debug("when returnThreadPool, release "+pool.getName() + " threadpool "+pool.MAX_THREAD_NUMBER.intValue());
        }
        pool.destroy(null);
        subPoolList.remove(pool);
        MAX_THREAD_NUMBER.addAndGet(pool.MAX_THREAD_NUMBER.intValue());
    }
    public void destroy(List<String> interrupts){
        for(Thread t:threads){
            while(true) {
                try {
                    if (!t.isAlive() || t.getName().equals(Thread.currentThread().getName())) {
                        threads.remove(t);
                        break;
                    } else {
                        if(null != interrupts) {
                            StackTraceElement[] es = t.getStackTrace();
                            if (null != es) {
                                for (StackTraceElement e : es) {
                                    if (ArrayUtils.isInStringArray(interrupts, e.getClassName())) {
                                        t.stop();
                                        log.info("interrupt thread "+t.getName()+" for"+e.getClassName());
                                        break;
                                    }
                                }
                            }
                        }
                        Thread.sleep(100);
                        log.info("ThreadPool destroy waiting thread["+t.getName()+"] release."+ ExceptionUtil.getStackTraceString(t.getStackTrace()));
                    }
                }catch (Exception e){
                    log.error("ThreadPool destroy error:",e);
                }
            }
        }
    }
    public ThreadPool getThreadPool(String name,int count){
        while(MAX_THREAD_NUMBER.intValue()-threads.size()<count){
            if(log.isDebugEnabled()||name.equals("Thread pool root"))
            log.error("ThreadPool ["+name+"] is full,the max thread count is "+MAX_THREAD_NUMBER +" and current used "+threads.size()+" and new need "+count);
            try {
                for(Thread t:threads){
                    if(!t.isAlive()){
                        if(log.isDebugEnabled()){
                            log.debug("when getThreadPool, release thread "+t.getName());
                        }
                        threads.remove(t);
                    }
                }
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        MAX_THREAD_NUMBER.set(MAX_THREAD_NUMBER.intValue() - count);
        if(log.isDebugEnabled())
            log.error("has borrow out threadpool "+count);
        ThreadPool tp = new ThreadPool(name,count);
        subPoolList.add(tp);
        return tp;
    }




    public Executor getExecutor(){
        return getExecutor(1)[0];
    }


    public void waitfinished(){
        while(threads.size()!=0){
            try {
                for(Thread t:threads){
                    if(!t.isAlive()){
                        if(log.isDebugEnabled()){
                            log.debug("then waitfinished, release thread "+t.getName());
                        }
                        threads.remove(t);
                    }
                }
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    public Executor[] getExecutor(int count){
        //long l = System.currentTimeMillis();
        while(MAX_THREAD_NUMBER.intValue()-threads.size()<count){
            if(log.isDebugEnabled()|| name.equals("Thread pool root"))
                log.error("ThreadPool["+name+"] is full,the max thread count is "+MAX_THREAD_NUMBER +" and current used "+threads.size()+" and new need "+count);
            try {
                takeLock.lock();
                lock.await();
                takeLock.unlock();
                //for(Thread t:threads){
                //    if(!t.isAlive()){
                //        if(log.isDebugEnabled()){
                //            log.debug("then getExecutor,release thread "+t.getName());
                //        }
                //        threads.remove(t);
                //    }
                //}
                //Thread.sleep(1);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        MyExecutor[] ret = new MyExecutor[count];
        for(int i=0;i<ret.length;i++){
            ret[i]=new MyExecutor(this);
            Thread t = threadFactory.newThread(ret[i]);
            ret[i].setThread(t);
            threads.add(t);
        }
        //System.out.println("=thread time:"+Thread.currentThread().getName()+"  "+(System.currentTimeMillis()-l));
        return ret;
    }

    public synchronized static ExecutorService getExecutorService(int count){
        return Executors.newFixedThreadPool(count);
    }

    public class MyExecutor implements Executor,Runnable{
        Thread thread;
        Runnable run;
        ThreadPool pool;
        public String getThreadName(){
            return thread.getName();
        }
        MyExecutor(ThreadPool p){
            this.pool=p;
        }
        public void setThread(Thread t){
            this.thread=t;

        }
        @Override
        public void execute(Runnable command) {
            this.run=command;
            thread.start();
        }

        public void execute(InvokeTask command,int timeout) throws TimeoutException {
            this.run=command;
            thread.start();
            try {
                command.waitFor(timeout * 1000);
            } catch (InterruptedException e) {
                throw new TimeoutException("run time out over "+timeout+" seconds");
            }
        }
        public void join() throws InterruptedException {
            thread.join();
        }

        @Override
        public void run() {
            run.run();
            pool.takeLock.lock();
            pool.threads.remove(thread);
            pool.lock.signal();
            pool.takeLock.unlock();
        }
    }



}
