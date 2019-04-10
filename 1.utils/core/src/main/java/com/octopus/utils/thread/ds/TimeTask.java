package com.octopus.utils.thread.ds;

/**
 * User: Administrator
 * Date: 14-10-29
 * Time: 下午6:04
 */
public class TimeTask {
    String timeExpression;
    Object object;

    public TimeTask(){}

    public TimeTask(String timeexpression,Object impl){
        this.timeExpression=timeexpression;
        this.object=impl;
    }

    public String getTimeExpression() {
        return timeExpression;
    }

    public void setTimeExpression(String timeExpression) {
        this.timeExpression = timeExpression;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }
}
