package com.octopus.utils.thread;/**
 * Created by admin on 2020/6/1.
 */

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @ClassName ThreadPoolQueue
 * @Description ToDo
 * @Author Kod Wong
 * @Date 2020/6/1 9:38
 * @Version 1.0
 **/
public class ThreadPoolQueue {
    ThreadPool pool;
    LinkedBlockingQueue queue = new LinkedBlockingQueue();
    public ThreadPoolQueue(final ExecutorService pool){
        pool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Runnable o = (Runnable) queue.take();
                    pool.execute(o);
                    run();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        });
    }
    public LinkedBlockingQueue getQueue(){
        return queue;
    }
}
