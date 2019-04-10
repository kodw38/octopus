package com.octopus.utils.cachebatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.TimerTask;

public class FlushTimerTask extends TimerTask
{
    private static transient Log log = LogFactory.getLog(FlushTimerTask.class);
    private AsynContainer objAsynContainer = null;

    public FlushTimerTask(AsynContainer objAsynContainer)
    {
        this.objAsynContainer = objAsynContainer;
    }

    public void run() {
        this.objAsynContainer.flush();
    }
}