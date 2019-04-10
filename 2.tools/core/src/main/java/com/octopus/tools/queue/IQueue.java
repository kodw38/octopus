package com.octopus.tools.queue;

/**
 * User: Administrator
 * Date: 14-10-29
 * Time: 上午8:56
 */
public interface IQueue {
    public void put(Object o) throws InterruptedException;

    public void setThreadNum(int num);
    public void setQueueMax(int num);

    public Object take();

    public void start();
    public void suspend() ;

    public void active();
}
