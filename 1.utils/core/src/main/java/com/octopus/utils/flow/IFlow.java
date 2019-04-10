package com.octopus.utils.flow;

import com.octopus.utils.bftask.IBFTask;

/**
 * User: Administrator
 * Date: 14-8-25
 * Time: 下午8:51
 */
public interface IFlow {

    public boolean isExist(double  seq);

    public void doFlow(FlowParameters parameters) throws Exception;

    public void doNode(IBFTask node,FlowParameters parameters) throws Exception;

}
