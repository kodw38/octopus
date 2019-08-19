package com.octopus.utils.cls.proxy;

import java.util.ArrayList;
import java.util.List;

/**
 * User: robai
 * Date: 2010-3-8
 * Time: 22:10:44
 */
public interface IMethodAddition {

    public Object beforeAction(Object impl, String m, Object[] args)throws Exception;

    public Object afterAction(Object impl, String m, Object[] args,boolean isInvoke,boolean isSuccess,Throwable e,Object result) throws Exception;

    public Object resultAction(Object impl, String m, Object[] args,Object result);


    public int getLevel();

    public boolean isWaiteBefore();
    public boolean isWaiteAfter();
    public boolean isWaiteResult();

    public boolean isNextInvoke();

    public void setMethods(List<String> methods);

    public List<String> getMethods();

}
