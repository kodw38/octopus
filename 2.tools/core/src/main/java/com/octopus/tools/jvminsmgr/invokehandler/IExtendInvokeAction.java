package com.octopus.tools.jvminsmgr.invokehandler;

import com.octopus.utils.xml.XMLMakeup;

/**
 * User: Administrator
 * Date: 14-9-17
 * Time: 下午10:20
 */
public interface IExtendInvokeAction {

    public void setXml(XMLMakeup xml);

    public boolean isExecute(String xmlid,String id,String methodName);

    public void doBeforeInvoke(Object impl, String m, Object[] args)throws Exception;
    public void doAfterInvoke(Object impl, String m, Object[] args, boolean isSuccess, Throwable e, Object result);
    public Object doResult(Object result);


}
