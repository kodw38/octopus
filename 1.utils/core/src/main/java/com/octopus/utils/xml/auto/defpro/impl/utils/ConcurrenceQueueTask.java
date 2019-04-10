package com.octopus.utils.xml.auto.defpro.impl.utils;

import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.thread.ThreadPool;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.BlockingQueue;

/**
 * User: wfgao_000
 * Date: 15-4-22
 * Time: 下午1:44
 */
public class ConcurrenceQueueTask implements Runnable {
    transient static Log log = LogFactory.getLog(ConcurrenceQueueTask.class);
    BlockingQueue queue;
    ThreadPool threadPool;
    public ConcurrenceQueueTask(BlockingQueue queue, ThreadPool threadPool){
       this.queue=queue;
        this.threadPool=threadPool;
        ExecutorUtils.work(this);
    }


    @Override
    public void run() {
        while (true) {
            try {
                //JSONObject proValue, XMLDoObject obj, XMLParameter parameter,Map par, JSONObject jsonPar
                Object[] os = (Object[]) queue.take();
                threadPool.getExecutor().execute(new ConcurrenceExeRun(os));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
