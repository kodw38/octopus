package com.octopus.utils.thread.ds;

/**
 * User: Administrator
 * Date: 14-10-29
 * Time: 上午11:28
 */
public abstract class ParameterRun implements Runnable {
    Object parameter;
    public ParameterRun(){}
    public ParameterRun(Object par){
         this.parameter=par;
    }
    @Override
    public void run() {
        work(parameter);
    }
    public abstract void  work(Object parameter);
}
