package com.octopus.utils.pool;


import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * User: wf
 * Date: 2008-8-20
 * Time: 14:45:37
 */
public class ThreadPool {

    private static final HashMap selfRegister = new HashMap();

    private int count = 10;

    private int prio = Thread.NORM_PRIORITY;

    private boolean isShutdown = false;

    private boolean handoffPending = false;

    private boolean inheritLoader = true;

    private boolean inheritGroup = true;

    private boolean makeThreadsDaemons = false;

    private ThreadGroup threadGroup;

    private final Object nextRunnableLock = new Object();

    private List workers;
    private LinkedList availWorkers = new LinkedList(); //Idle list
    private LinkedList busyWorkers = new LinkedList();  //useing list

    private String threadPoolNamePrefix = "com.jbs.common.soulimpl.ThreadPool";
    private String threadNamePrefix="";

    private final Logger log = Logger.getLogger(getClass());


    public ThreadPool() {
        String poolName =  threadPoolNamePrefix+"-"+ selfRegister.size();
        threadNamePrefix = poolName;
        selfRegister.put(poolName,this);
        initialize();
    }

    public ThreadPool(int threadCount, int threadPriority) {
        this.count = threadCount;
        this.prio = threadPriority;
        String poolName =  threadPoolNamePrefix+"-"+ selfRegister.size();
        threadNamePrefix = poolName;
        selfRegister.put(poolName,this);
        initialize();
    }

    public int getPoolSize() {
        return this.count;
    }
    public void setThreadCount(int count) {
        this.count = count;
    }

    public int getAvailThreadCount(){
       return availWorkers.size();
    }
    public int getThreadCount() {
        return count;
    }

    public void setThreadPriority(int prio) {
        this.prio = prio;
    }
    public int getThreadPriority() {
        return prio;
    }

    public void setThreadNamePrefix(String prfx) {
        this.threadNamePrefix = prfx;
    }

    public String getThreadNamePrefix() {
        return threadNamePrefix;
    }

    public boolean isThreadsInheritContextClassLoaderOfInitializingThread() {
        return inheritLoader;
    }

    public void setThreadsInheritContextClassLoaderOfInitializingThread( boolean inheritLoader) {
        this.inheritLoader = inheritLoader;
    }

    public boolean isThreadsInheritGroupOfInitializingThread() {
        return inheritGroup;
    }

    public void setThreadsInheritGroupOfInitializingThread(boolean inheritGroup) {
        this.inheritGroup = inheritGroup;
    }

    public boolean isMakeThreadsDaemons() {
        return makeThreadsDaemons;
    }

    public void setMakeThreadsDaemons(boolean makeThreadsDaemons) {
        this.makeThreadsDaemons = makeThreadsDaemons;
    }

    public void initialize() {
        if (count <= 0) { throw new IllegalArgumentException("Thread count must be > 0");}
        if (prio <= 0 || prio > 9) {throw new IllegalArgumentException("Thread priority must be > 0 and <= 9");}

        if(isThreadsInheritGroupOfInitializingThread()) {
            threadGroup = Thread.currentThread().getThreadGroup();
        } else {
            // follow the threadGroup tree to the root thread group.
            threadGroup = Thread.currentThread().getThreadGroup();
            ThreadGroup parent = threadGroup;
            while (!parent.getName().equals("main") ) {
                threadGroup = parent;
                parent = threadGroup.getParent();
            }
            threadGroup = new ThreadGroup(parent,threadNamePrefix);
            if (isMakeThreadsDaemons()){
                threadGroup.setDaemon(true);
            }
        }

        if (isThreadsInheritContextClassLoaderOfInitializingThread()) {
            log.info("Job execution threads will use class loader of thread: "+ Thread.currentThread().getName());
        }
        // create the worker threads and start them
        Iterator workerThreads = createWorkerThreads(count).iterator();
        while(workerThreads.hasNext()) {
            JBSThread wt = (JBSThread) workerThreads.next();
            wt.start();
            availWorkers.add(wt);
        }
    }


    protected List createWorkerThreads(int count) {
        workers = new LinkedList();
        for (int i = 1; i<= count; ++i) {
            JBSThread wt = new JBSThread(this, threadGroup,getThreadNamePrefix() + "-" + i,getThreadPriority(),isMakeThreadsDaemons());
            if (isThreadsInheritContextClassLoaderOfInitializingThread()) {
                wt.setContextClassLoader(Thread.currentThread().getContextClassLoader());
            }
            workers.add(wt);
        }
        return workers;
    }

    public void shutdown() {
        shutdown(true);
    }

    public void shutdown(boolean waitForJobsToComplete) {
        synchronized (nextRunnableLock) {
            isShutdown = true;
            // signal each worker thread to shut down
            Iterator workerThreads = workers.iterator();
            while(workerThreads.hasNext()) {
                JBSThread wt = (JBSThread) workerThreads.next();
                wt.shutdown();
                availWorkers.remove(wt);
            }
            // Give waiting (wait(1000)) worker threads a chance to shut down.
            // Active worker threads will shut down after finishing their
            // current job.
            nextRunnableLock.notifyAll();
            if (waitForJobsToComplete == true) {
                // wait for hand-off in runInThread to complete...
                while(handoffPending) {
                    try { nextRunnableLock.wait(100); } catch(Throwable t) {}
                }
                // Wait until all worker threads are shut down
                while (busyWorkers.size() > 0) {
                    JBSThread wt = (JBSThread) busyWorkers.getFirst();
                    try {
                        log.debug("Waiting for thread " + wt.getName()+ " to shut down");
                        // note: with waiting infinite time the
                        // application may appear to 'hang'.
                        nextRunnableLock.wait(2000);
                    } catch (InterruptedException ex) {}
                }

                int activeCount = threadGroup.activeCount();
                if (activeCount > 0) {
                    log.info("There are still " + activeCount + " worker threads active."+ " See javadoc runInThread(Runnable) for a possible explanation");
                }
                log.debug("shutdown complete");
            }
        }
    }

    public boolean runInThread(Runnable runnable) {
        if (runnable == null) {
            return false;
        }
        synchronized (nextRunnableLock) {
            handoffPending = true;
            // Wait until a worker thread is available
            while ((availWorkers.size() < 1) && !isShutdown) {
                log.info("no avail thread may use ;please waiting...");
                try {nextRunnableLock.wait(500);} catch (InterruptedException ignore) {}
            }
            if (!isShutdown) {
                JBSThread wt = (JBSThread)availWorkers.removeFirst();
                busyWorkers.add(wt);
                wt.run(runnable);
            } else {
                // If the thread pool is going down, execute the Runnable
                // within a new additional worker thread (no thread from the pool).
                JBSThread wt = new JBSThread(this, threadGroup,this.threadNamePrefix+"-LastJob", prio, isMakeThreadsDaemons(), runnable);
                busyWorkers.add(wt);
                workers.add(wt);
                wt.start();
            }
            nextRunnableLock.notifyAll();
            handoffPending = false;
        }
        return true;
    }

    public int blockForAvailableThreads() {
        synchronized(nextRunnableLock) {
            while((availWorkers.size() < 1 || handoffPending) && !isShutdown) {
                try {
                    nextRunnableLock.wait(500);
                } catch (InterruptedException ignore) {
                }
            }
            return availWorkers.size();
        }
    }


    protected void makeAvailable(JBSThread t) {
        synchronized(nextRunnableLock) {
            if(!isShutdown) {
                availWorkers.add(t);
            }
            busyWorkers.remove(t);
            nextRunnableLock.notifyAll();
        }
    }

    public static HashMap getSelfRegister() {
        return selfRegister;
    }
    
}
