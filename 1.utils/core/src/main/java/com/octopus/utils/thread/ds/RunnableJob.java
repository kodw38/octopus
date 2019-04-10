package com.octopus.utils.thread.ds;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * User: Administrator
 * Date: 14-10-29
 * Time: 下午9:02
 */
public class RunnableJob implements Job {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        ((Runnable)jobExecutionContext.getTrigger().getJobDataMap().get("Runnable")).run();
    }
}
