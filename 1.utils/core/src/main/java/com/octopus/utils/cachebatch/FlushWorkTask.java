package com.octopus.utils.cachebatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;

public abstract class FlushWorkTask
        implements Runnable
{
    private static transient Log log = LogFactory.getLog(FlushWorkTask.class);
    protected Map data = null;
    String name;
    Object other;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public void setOther(Object other){
        this.other = other;
    }

    public Object getOther() {
        return other;
    }

    public void setData(Map data)
    {
        this.data = data;
    }

    public void run()
    {
        try
        {
            work(this.data);
        }
        catch (Throwable ex) {
            log.error("FlushWorkTask error", ex);
        }
    }

    public abstract void work(Map paramMap)
            throws Exception;
}