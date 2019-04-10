package com.octopus.utils.bftask;

/**
 * User: Administrator
 * Date: 14-8-25
 * Time: 下午8:45
 */
public interface IBFTaskMgr {

    public boolean isExist(String taskkey);

    public void doTask(String taskkey,BFParameters parameters) throws Exception;
}
