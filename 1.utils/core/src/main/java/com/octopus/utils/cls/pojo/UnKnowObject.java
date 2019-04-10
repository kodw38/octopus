package com.octopus.utils.cls.pojo;

/**
 * User: wangfeng2
 * Date: 14-4-16
 * Time: 下午5:01
 */
public class UnKnowObject {
    String className;
    String propertyName;
    String propertyType;

    public String toString(){
        return className+"@"+propertyType+"@"+propertyName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public String getPropertyType() {
        return propertyType;
    }

    public void setPropertyType(String propertyType) {
        this.propertyType = propertyType;
    }
}
