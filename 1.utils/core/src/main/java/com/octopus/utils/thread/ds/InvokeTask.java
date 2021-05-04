package com.octopus.utils.thread.ds;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cls.ClassUtils;
import com.octopus.utils.time.WorkTimeUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.TimeoutException;

/**
 * User: Administrator
 * Date: 14-9-18
 * Time: 上午10:39
 */
public class InvokeTask implements Runnable{
    private static transient Log log = LogFactory.getLog(InvokeTask.class);
    Object impl;
    String methodName;
    Class[] parsClass;
    Object[] pars;
    Object result;
    boolean isSuccess;
    Throwable exception;
    boolean istimeout=true;
    Object w = null;
    Class staticClass=null;
    public InvokeTask(Object impl,String methodName,Class[] parsClass,Object[] pars){
        this.impl=impl;
        this.methodName=methodName;
        this.pars=pars;
        this.parsClass=parsClass;
    }
    public InvokeTask(Class staticClass,String methodName,Class[] parsClass,Object[] pars){
        this.staticClass=staticClass;
        this.methodName=methodName;
        this.pars=pars;
        this.parsClass=parsClass;
    }
    public void waitFor(int miseconds) throws InterruptedException, TimeoutException {
        w = new Object();
        synchronized (w) {
            w.wait(miseconds);
            if(istimeout){
                throw new TimeoutException("Task is out over time "+miseconds+" Millis Seconds");
            }
        }
    }

    @Override
    public void run() {
        try{
            if(impl instanceof TimeTask){
                if(StringUtils.isBlank(((TimeTask) this.impl).timeExpression))
                    result=ClassUtils.invokeMethod(((TimeTask)this.impl).getObject(),methodName,parsClass,pars);
                else{
                    //一次性的在本线程执行，往复的另起线程
                    WorkTimeUtil.work(((TimeTask) this.impl).timeExpression,new InvokeTask(((TimeTask)this.impl).getObject(),methodName,parsClass,pars));
                }
            }else{
                if(null != staticClass && null == impl){
                    result=ClassUtils.invokeStaticMethod(staticClass,methodName,parsClass,pars);
                }else {
                    result = ClassUtils.invokeMethod(this.impl, methodName, parsClass, pars);
                }
            }

            isSuccess=true;
        }catch (Throwable e){
            log.error("invoke error:",e);
            isSuccess=false;
            exception=e;
        }finally {
            if(null != w) {
                synchronized (w) {
                    istimeout = false;
                    w.notify();

                }
            }
        }
    }

    public Object getResult() {
        return result;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public Throwable getException() {
        return exception;
    }
}
