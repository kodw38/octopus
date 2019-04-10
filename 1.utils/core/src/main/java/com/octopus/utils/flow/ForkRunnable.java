package com.octopus.utils.flow;

import com.octopus.utils.bftask.IBFTask;
import com.octopus.utils.flow.impl.Flow;

/**
 * User: Administrator
 * Date: 14-8-23
 * Time: 下午6:24
 */
public class ForkRunnable implements Runnable {
    IFlow flow;
    FlowParameters parameters;
    IBFTask task;
    boolean isSuccess=true;

    public ForkRunnable(){
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public IFlow getFlow() {
        return flow;
    }

    public IBFTask getTask() {
        return task;
    }

    public void setTask(IBFTask task) {
        this.task = task;
    }

    public void setFlow(Flow flow) {
        this.flow = flow;
    }

    public FlowParameters getParameters() {
        return parameters;
    }

    public void setParameters(FlowParameters parameters) {
        this.parameters = parameters;
    }

    @Override
    public void run() {
        try {
            flow.doNode(task,parameters);
        } catch (Exception e) {
            e.printStackTrace();
            isSuccess=false;
        }
    }
}
