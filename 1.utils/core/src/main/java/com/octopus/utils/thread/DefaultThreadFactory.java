package com.octopus.utils.thread;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: wfgao_000
 * Date: 16-1-19
 * Time: 上午9:03
 */
public class DefaultThreadFactory implements ThreadFactory {
    public static String ISP_THREAD_NAME_PREX="TISP";
    final static AtomicInteger poolNumber = new AtomicInteger(1);
    final ThreadGroup group;
    final AtomicInteger threadNumber = new AtomicInteger(1);
    final String namePrefix;
    public DefaultThreadFactory(boolean isSub) {
        SecurityManager s = System.getSecurityManager();
        group = (s != null)? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        if(isSub){
            namePrefix = ISP_THREAD_NAME_PREX +"-S"+poolNumber.getAndIncrement();
        }else{
            namePrefix = ISP_THREAD_NAME_PREX +"-"+poolNumber.getAndIncrement();
        }
    }



    public Thread newThread(Runnable r) {
        String n = Thread.currentThread().getName();
        String name;
        if(n.contains(ISP_THREAD_NAME_PREX))
            name=n+"." + threadNumber.getAndIncrement();
        else
            name = n+"."+namePrefix +"-"+ threadNumber.getAndIncrement();
        Thread t = new Thread(group, r,name,0);
        if (t.isDaemon())
            t.setDaemon(false);
        if (t.getPriority() != Thread.NORM_PRIORITY)
            t.setPriority(Thread.NORM_PRIORITY);

        return t;
    }
}
