package com.octopus.utils.pool;

import org.apache.log4j.Logger;


/**
 * User: wf
 * Date: 2008-8-20
 * Time: 14:49:01
 */
public class JBSThread extends Thread{
    private boolean run = true;
    private ThreadPool tp;
    private Runnable runnable = null;

    private static final Logger log = Logger.getLogger(JBSThread.class);

    public JBSThread(ThreadPool tp, ThreadGroup threadGroup, String name,int prio, boolean isDaemon) {
        this(tp, threadGroup, name, prio, isDaemon, null);
    }

    public JBSThread(ThreadPool tp, ThreadGroup threadGroup, String name,int prio, boolean isDaemon, Runnable runnable) {
        super(threadGroup, name);
        this.tp = tp;
        this.runnable = runnable;
        setPriority(prio);
        setDaemon(isDaemon);
    }

    void shutdown() {
        run = false;
    }

    public void run(Runnable newRunnable) {
        synchronized(this) {
            if(runnable != null) {
                throw new IllegalStateException("Already running a Runnable!");
            }
            runnable = newRunnable;
            this.notifyAll();
        }
    }

    public void run() {
        boolean runOnce = (runnable != null);
        boolean ran = false;
        while (run) {
            try {
                synchronized(this) {
                    while (runnable == null && run) {
                        this.wait(500);
                    }
                }
                if (runnable != null) {
                    ran = true;
                    runnable.run();
                    log.info("has execute job "+runnable.getClass().getName());
                }
            } catch (InterruptedException unblock) {
                    log.error("worker threat got 'interrupt'ed.", unblock);
            } catch (Exception exceptionInRunnable) {
                    log.error("Error while executing the Runnable: ",exceptionInRunnable);
            } finally {
                runnable = null;
                if(getPriority() != tp.getThreadPriority()) {
                    setPriority(tp.getThreadPriority());
                }
                if (runOnce) {
                    run = false;
                } else if(ran) {
                    ran = false;
                    tp.makeAvailable(this);
                }

            }
        }
        log.debug("WorkerThread is shutting down");
    }

}
