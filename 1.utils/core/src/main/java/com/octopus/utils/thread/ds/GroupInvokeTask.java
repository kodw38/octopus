package com.octopus.utils.thread.ds;

import java.util.List;

/**
 * User: wfgao_000
 * Date: 15-8-19
 * Time: 下午5:09
 */
public class GroupInvokeTask implements Runnable {
    List<InvokeTask> tasks;
    public GroupInvokeTask(List<InvokeTask> tasks){
        this.tasks=tasks;
    }

    @Override
    public void run() {
        for(InvokeTask v:tasks)
            v.run();
    }

}
