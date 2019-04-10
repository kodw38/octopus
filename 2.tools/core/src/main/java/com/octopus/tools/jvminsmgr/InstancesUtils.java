package com.octopus.tools.jvminsmgr;

import com.octopus.tools.jvminsmgr.ds.SynResults;
import com.octopus.tools.jvminsmgr.ds.WaitResults;

/**
 * User: Administrator
 * Date: 14-9-18
 * Time: 下午5:27
 */
public class InstancesUtils {
    /**
     * 根据历史，调用所有曾经调用过的远程接口
     * @param className
     * @param methodName
     * @param parameters
     * @param parametersClass
     * @return
     */
    public static SynResults remoteSynInvokeAllInstances(String className,String methodName,Object parameters,Class[] parametersClass){
        return null;
    }


    public static WaitResults remoteWaitInvokeAllInstances(String className,String methodName,Object parameters,Class[] parametersClass){
        return null;
    }


    /**
     * 计算本地主机资源，做扩展调用
     * @param className
     * @param methodName
     * @param parameters
     * @param parametersClass
     * @return
     */
    public static Object remoteWaitCalLocalResourceExtendInvoke(String className,String methodName,Object parameters,Class[] parametersClass){
        return null;
    }

    public static String getCurrentInstance(){
        return "";
    }

    public static WaitResults remoteWaitInvokeInstances(String instance,String className,String methodName,Object parameters,Class[] parametersClass){
        return null;
    }
}
