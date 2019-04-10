package com.octopus.utils.time;

import com.octopus.utils.thread.ds.RunnableJob;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Iterator;

/**
 * User: Administrator
 * Date: 14-10-29
 * Time: 下午8:30
 */
public class WorkTimeUtil {
    static SchedulerFactory schedulerFactory = new StdSchedulerFactory();
    public static void work(String expression,Runnable runnable) throws Exception {
        CronTrigger cronTrigger = new CronTrigger("if_stock_trigger"+runnable.hashCode(), "tgroup");
        cronTrigger.getJobDataMap().put("Runnable",runnable);
        JobDetail detail = new JobDetail("job_"+runnable.hashCode(),"job_group",RunnableJob.class);
        CronExpression cexp = new CronExpression(expression);
        cronTrigger.setCronExpression(cexp);
        Scheduler scheduler = schedulerFactory.getScheduler();
        scheduler.scheduleJob(detail, cronTrigger);
        scheduler.start();
    }

    public static void clearAllWork() throws SchedulerException {
        Iterator<Scheduler> ls = schedulerFactory.getAllSchedulers().iterator();
        while(ls.hasNext()){
            ls.next().shutdown(true);
        }
    }

}
