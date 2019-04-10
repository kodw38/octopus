package com.octopus.utils.cls.pojo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 描述一个方法的输入参数，返回参数的简单报文
 * User: wangfeng2
 * Date: 14-4-15
 * Time: 下午5:59
 */
public class MethodParameterProperty implements Serializable {
    String methodName;
    String className;
    List<PropertyInfo> inputParameter;
    PropertyInfo returnParameter;

    public MethodParameterProperty(){
        inputParameter = new ArrayList<PropertyInfo>();
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }


    public List<PropertyInfo> getInputParameter() {
        return inputParameter;
    }

    public void setInputParameter(List<PropertyInfo> inputParameter) {
        this.inputParameter = inputParameter;
    }

    public PropertyInfo getReturnParameter() {
        return returnParameter;
    }

    public void setReturnParameter(PropertyInfo returnParameter) {
        this.returnParameter = returnParameter;
    }
}
