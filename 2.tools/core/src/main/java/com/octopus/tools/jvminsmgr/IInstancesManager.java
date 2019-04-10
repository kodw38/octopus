package com.octopus.tools.jvminsmgr;

import com.octopus.tools.jvminsmgr.ds.SynResults;

/**
 * User: Administrator
 * Date: 14-9-17
 * Time: 下午4:32
 */
public interface IInstancesManager {
    public SynResults remoteSynInvokeAllInstances(String className,Object parameters,Class[] parametersClass);
}
