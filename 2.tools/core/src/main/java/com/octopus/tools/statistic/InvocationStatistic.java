package com.octopus.tools.statistic;

import com.octopus.utils.cls.proxy.IMethodAddition;

import java.util.List;

/**
 * User: Administrator
 * Date: 14-8-27
 * Time: 下午4:16
 */
public class InvocationStatistic implements IMethodAddition {

    @Override
    public Object beforeAction(Object impl, String m, Object[] args) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object afterAction(Object impl, String m, Object[] args, boolean isInvoke, boolean isSuccess, Throwable e, Object result) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object resultAction(Object impl, String m, Object[] args, Object result) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getLevel() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isWaiteBefore() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isWaiteAfter() {
        return false;
    }

    @Override
    public boolean isWaiteResult() {
        return false;
    }

    @Override
    public boolean isNextInvoke() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setMethods(List<String> methods) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<String> getMethods() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
