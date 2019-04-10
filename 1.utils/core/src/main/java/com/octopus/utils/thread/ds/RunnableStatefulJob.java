package com.octopus.utils.thread.ds;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * User: wfgao_000
 * Date: 16-8-4
 * Time: 下午1:52
 */
public class RunnableStatefulJob implements org.quartz.StatefulJob {
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        ((Runnable)jobExecutionContext.getTrigger().getJobDataMap().get("Runnable")).run();
    }
}
