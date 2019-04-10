package com.octopus.utils.thread.ds;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cls.ClassUtils;
import com.octopus.utils.time.WorkTimeUtil;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.TimeoutException;

/**
 * Created by Administrator on 2019/3/12.
 */
public class InvokeTaskByObjName implements Runnable {
    private static transient Log log = LogFactory.getLog(InvokeTask.class);
    String name;
    String methodName;
    Class[] parsClass;
    Object[] pars;
    Object result;
    boolean isSuccess;
    Throwable exception;
    boolean istimeout = true;
    XMLObject obj;
    Object w = null;

    public String getName(){
        return name;
    }
    public InvokeTaskByObjName(String objName, String methodName, Class[] parsClass, Object[] pars) {
        this.name = objName;
        this.methodName = methodName;
        this.pars = pars;
        this.parsClass = parsClass;
    }

    public void waitFor(int miseconds) throws InterruptedException, TimeoutException {
        w = new Object();
        synchronized (w) {
            w.wait(miseconds);
            if (istimeout) {
                throw new TimeoutException("Task is out over time " + miseconds + " Millis Seconds");
            }
        }
    }

    public void setXMlObject(XMLObject object){
        obj=object;
    }
    @Override
    public void run() {
        try {
            if(null != pars && null != obj) {
                if(null != obj) {
                    result = ClassUtils.invokeMethod(obj, methodName, parsClass, pars);
                    isSuccess = true;
                }
            }else{
                throw new Exception("not set XMLObject before run");
            }
        } catch (Throwable e) {
            log.error("invoke error:", e);
            isSuccess = false;
            exception = e;
        } finally {
            if (null != w) {
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