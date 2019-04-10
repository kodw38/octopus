package com.octopus.utils.cachebatch;

import EDU.oswego.cs.dl.util.concurrent.BoundedBuffer;
import EDU.oswego.cs.dl.util.concurrent.PooledExecutor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class AsynContainer implements AsynContainerMBean,Serializable
{
    private static transient Log log = LogFactory.getLog(AsynContainer.class);
    private final Timer TIMER = new Timer(true);
    private PooledExecutor objPooledExecutor = null;
    private ConcurrentHashMap data = null;
    private long limitLength = 0L;
    private Class workTaskClass = null;
    private boolean isFullDiscard = false;
    String name;
    Object other;
    private AtomicLong count = new AtomicLong(0L);

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public void setOther(Object other){
        this.other=other;
    }

    public AsynContainer(long intervalSeconds, long limitLength, Class workTaskClass)
    {
        init(intervalSeconds, limitLength, workTaskClass, 1000, 300000L, 1, 5, 1, false);
    }

    public AsynContainer(long intervalSeconds, long limitLength, Class workTaskClass, boolean isFullDiscard)
    {
        init(intervalSeconds, limitLength, workTaskClass, 1000, 300000L, 1, 5, 1, isFullDiscard);
    }

    public AsynContainer(long intervalSeconds, long limitLength, Class workTaskClass, int boundedBufferSize, long keepAliveTime, int minThread, int maxThread, int createThread)
    {
        init(intervalSeconds, limitLength, workTaskClass, boundedBufferSize, keepAliveTime, minThread, maxThread, createThread, false);
    }

    private void init(long intervalSeconds, long limitLength, Class workTaskClass, int boundedBufferSize, long keepAliveTime, int minThread, int maxThread, int createThread, boolean isFullDiscard)
    {
        try{
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            ObjectName name = new ObjectName("com.octopus.utils.cachelog:type=AsynContainer"+"."+workTaskClass.getName());
            mbs.registerMBean(this, name);
        }catch (Exception e){

        }
        this.limitLength = limitLength;
        this.workTaskClass = workTaskClass;
        this.isFullDiscard = isFullDiscard;

        this.data = new ConcurrentHashMap(32);

        TIMER.schedule(new FlushTimerTask(this), intervalSeconds * 1000L, intervalSeconds * 1000L);
        if (this.objPooledExecutor == null)
            synchronized (this) {
                if (this.objPooledExecutor == null) {
                    this.objPooledExecutor = new PooledExecutor(new BoundedBuffer(boundedBufferSize));
                    this.objPooledExecutor.setKeepAliveTime(keepAliveTime);
                    this.objPooledExecutor.setMinimumPoolSize(minThread);
                    this.objPooledExecutor.setMaximumPoolSize(maxThread);
                    this.objPooledExecutor.createThreads(createThread);
                }
            }
    }


    public  void insert(Object object)
    {
        if(object==null) return;
        this.hashCode();
        if (this.data.size() >= this.limitLength){
            if (this.isFullDiscard) {
                return;
            }
        }
        long key = this.count.incrementAndGet();
        this.data.put(new Long(key), object);
        if(log.isDebugEnabled())
           log.debug("add obj:"+object);

        if (this.data.size() >= this.limitLength)
            flush();
    }

    public long getTotalCount()
    {
        return this.count.get();
    }

    public synchronized void flush()
    {
        try
        {
            if (this.data.size() > 0)
                    if (this.data.size() > 0) {
                        FlushWorkTask objFlushWorkTask = (FlushWorkTask)this.workTaskClass.newInstance();
                        Map map = new HashMap();
                        /*synchronized (data){*/
                            map.putAll(this.data);
                            Set keys = data.keySet();
                            for (Iterator iter = keys.iterator(); iter.hasNext(); ) {
                                Object item = iter.next();
                                this.data.remove(item);
                            }
                            count.set(0);
                        //}
                        objFlushWorkTask.setName(name);
                        objFlushWorkTask.setOther(other);
                        objFlushWorkTask.setData(map);
                        this.objPooledExecutor.execute(objFlushWorkTask);
                    }

        }
        catch (Throwable ex)
        {
            log.error("com.ai.appframe2.complex.util.collection.asyn.AsynContainer.insert_Refresh_error", ex);
        }
    }



}