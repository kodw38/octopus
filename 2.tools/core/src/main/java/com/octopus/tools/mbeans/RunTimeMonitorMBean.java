package com.octopus.tools.mbeans;

/**
 * Created by admin on 2019/12/7.
 */
public interface RunTimeMonitorMBean {
    /**
     * get count number of each api invoked
     * @return {apiname:count,...}
     */
    public String getCountApiInvoke();

    /**
     *  onley one hour
     * @return {apiname:{hhmm:count,....},....}
     */
    public String getCountApiInvokeDurTime();

    /**
     * 1000 busy user
     * @return {user:{logintime:'',apiname:count,...},...}
     */
    public String getUserApiInvoke();
    /**
     *
     * @return {threadname:{starttime:'',client:'',user:'',input:'',servicename:'',trace:'',fullPath:'',sql:'',sqlcost:'',outapi:'',outapicost:''},...}
     */
    public String getActiveThreadLoad();

    /**
     *
     * @return {ip:'',port:'',insname:'',startTime:'',cpu:'',mem:'',disk:'',jetttWorkThreadMaxCount:'',jettyWorkThreadCount:'',activeThreadCount:'',maxThreadCount:''}
     */
    public String getInstance();

    /**
     * 10000
     * @return {sql:{avecost:'',count:''},...}
     */
    public String getSqls();

    /**
     * 10000
     * @return {outapiname:{avecost:'',count:''}}
     */
    public String getOutApis();
    public String getRunningOutApis();

    /**
     *
     * @return {taskapiname:{cron:'',count:'',failcount:''}}
     */
    public String getQuartzInfo();

    public String getRunningSql();


}
