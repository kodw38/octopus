package com.octopus.tools.jvminsmgr;

import com.octopus.tools.jvminsmgr.ds.InstanceInfo;
import com.octopus.tools.jvminsmgr.ds.InstanceUsedInfo;

/**
 * User: Administrator
 * Date: 14-9-28
 * Time: 下午12:26
 */
public interface IInstanceManager {

    /**
     * 实例管理启动
     */
    public void start();

    /**
     * 监控所有配置的主机和本实例主机信息，给出状态。
     */
    void monitor();

    /**
     * 扩展实例
     * @param instanceInfo
     * @return
     */
    boolean extendInstance(InstanceInfo instanceInfo);

    /**
     * 回收实例
     * @param instanceInfo
     * @return
     */
    boolean reBack(InstanceInfo instanceInfo);

    /**
     * 记录实例使用信息
     * @param instanceUsedInfo
     */
    void logUsed(InstanceUsedInfo instanceUsedInfo);

    /**
     * 增加实例配置，用于IInstanceInfoGet,
     * @return
     */
    public boolean addInstanceInfo(String xmlid,String id,InstanceInfo instanceInfo);

    /**
     * 根据使用历史路由具体实例
     * @param clazz
     * @param method
     * @param parameters
     * @return
     */
    InstanceInfo[] router(String clazz,String method,Object[] parameters);

    /**
     * 获取使用过的实例，用于invocationhandler接口调用那个。
     * @param clazz
     * @param method
     * @param parameters
     * @return
     */
    public InstanceUsedInfo[] getUsedInstanceInfo(String clazz,String method,Object[] parameters);

    /**
     * 实例接口远程调用
     * @param instanceInfo
     * @param clazz
     * @param method
     * @param parameters
     * @return
     */
    public Object invokeInstance(InstanceInfo instanceInfo,String clazz,String method,Object[] parameters);
}
