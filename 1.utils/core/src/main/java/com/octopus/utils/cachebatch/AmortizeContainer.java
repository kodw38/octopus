package com.octopus.utils.cachebatch;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Amortize数据容器
 * 用于内存中临时存放数据的容器
 * 主要数据有：执行任务满足的记录数、执行任务满足的间隔时间（分钟）、执行的任务类
 * User: robai
 * Date: 2009-9-9
 * Time: 19:33:36
 */
public class AmortizeContainer {
    int timespace;//间隔分钟数
    int fullRecCount; //记录数
    Class workTaskClass; //执行任务类
    List datas = new ArrayList();//记录容器
    Date startTime; //初始时间
    String name ;


    /**
     * 构造方法
     * @param timespace //间隔分钟数
     * @param fullRecCount //记录数
     * @param workTaskClass //执行任务类
     * @throws Exception
     */
    public AmortizeContainer(String name,int timespace, int fullRecCount, Class workTaskClass){
        this.timespace = timespace;
        this.fullRecCount = fullRecCount;
        this.workTaskClass = workTaskClass;
        this.name=name;
    }

    /**
     * @Function: isAddData
     * @Description: 判断容器是否还能够增加数据，如果已有数据达到保存记录的2倍记录数，该容器就不能再增加数据了
     * @return
     * @return：返回结果描述
     * @author: robai
     * @date: 2011-3-26 下午09:59:29
     */
    public boolean isAddData() {
            return (datas.size()>=fullRecCount*2)?false:true;
    }


    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    /**
     * @Function: addObject
     * @Description: 向容器中增加数据
     * @param data
     * @throws Exception
     * @return：返回结果描述
     * @author: robai
     * @date: 2011-3-26 下午10:03:23
     */
    public void addObject(Object data) {
        synchronized (this) {
        	if(isAddData()){
        		datas.add(data);
                if(datas.size()>=getFullRecCount()){
                    AmortizeFlush.doData(name,this);
                }
        	}
        }
    }

    public void clearDatas() {
        datas.clear();
    }


    public List getDatas() {
        return datas;
    }

    public void setDatas(List datas) {
        this.datas = datas;
    }

    public int getFullRecCount() {
        return fullRecCount;
    }

    public void setFullRecCount(int fullRecCount) {
        this.fullRecCount = fullRecCount;
    }

    public int getTimespace() {
        return timespace;
    }

    public void setTimespace(int timespace) {
        this.timespace = timespace;
    }

    public Class getWorkTaskClass() {
        return workTaskClass;
    }

    public void setWorkTaskClass(Class workTaskClass) {
        this.workTaskClass = workTaskClass;
    }
}
