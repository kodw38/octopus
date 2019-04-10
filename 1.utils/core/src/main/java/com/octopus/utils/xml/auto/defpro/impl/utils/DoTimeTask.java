package com.octopus.utils.xml.auto.defpro.impl.utils;

import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.thread.ds.TimeTask;
import com.octopus.utils.time.WorkTimeUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.TimerTask;

/**
 * Created by Administrator on 2019/3/12.
 */
public class DoTimeTask extends TimerTask {
    static transient Log log = LogFactory.getLog(DoTimeTask.class);
    Object obj ;
    String methodName;
    Class[] parClasses;
    Object[] parObjs;
    public DoTimeTask (Object obj,String methodName,Class[] parclasses,Object[] pars){
        this.obj=obj;
        this.methodName=methodName;
        this.parClasses=parclasses;
        this.parObjs=pars;
    }
    @Override
    public void run() {
        try{
            log.error("--------start run time task----------"+obj+"|"+methodName);
            ExecutorUtils.synWork(this.obj, methodName, parClasses, parObjs);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
