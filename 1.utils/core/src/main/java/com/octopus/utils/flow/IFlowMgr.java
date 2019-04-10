package com.octopus.utils.flow;

/**
 * User: Administrator
 * Date: 14-8-25
 * Time: 下午8:50
 */
public interface IFlowMgr {

    public boolean isExist(String flowid);

    public void doFlow(String flowid,FlowParameters parameters)throws Exception;

    public void  doFirstFlow (FlowParameters parameters)throws Exception;

}
