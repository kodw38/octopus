package com.octopus.utils.bftask;

import com.octopus.utils.xml.XMLMakeup;

/**
 * User: wangfeng2
 * Date: 14-8-22
 * Time: 下午1:12
 */
public interface IBFExecutor {
    public void execute(XMLMakeup xml,String actionId,BFParameters parameters,Throwable error)throws Exception;
}
