package com.octopus.utils.cls.pojo;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 表述一个对象的属性树状结构
 * User: wangfeng2
 * Date: 14-4-15
 * Time: 下午2:00
 */
public class PropertyInfo extends Properties {
    PropertyInfo parent;
    List<PropertyInfo> children = new ArrayList();
    String v_name="name";
    String v_type="type";
    String v_isBinary="isbinary";
    String v_isArray="isarray";
    String v_isPrimitive="isprimitive";
    String v_isGeneric="generic";
    String v_modifier="modifier";

    public PropertyInfo getParent() {
        return parent;
    }

    public void set(String typeName,String variableName,boolean isBinary,boolean isArray,boolean isPrimitive,int modifier){
        setType(typeName);
        setBinary(isBinary);
        setArray(isArray);
        setPrimitive(isPrimitive);
        setName(variableName);
        setModifier(modifier);
    }

    public PropertyInfo addChild(){
        if(null == children) children = new ArrayList<PropertyInfo>();
        PropertyInfo info = new PropertyInfo();
        children.add(info);
        return info;
    }

    public void setModifier(int modifier){
        if(modifier>0)
            put(v_modifier,modifier);
    }
    public int getModifier(){
        return null==get(v_modifier)?0:(Integer)get(v_modifier);
    }

    public void setParent(PropertyInfo parent) {
        this.parent = parent;
    }

    public List<PropertyInfo> getChildren() {
        return children;
    }

    public void setChildren(List<PropertyInfo> children) {
        this.children = children;
    }

    public boolean isGeneric() {
        return null==this.get(v_isGeneric)?false: (Boolean)this.get(v_isGeneric);
    }

    public void setGeneric(boolean generic) {
        if(generic)
            put(v_isGeneric,generic);
    }

    public String getName() {
        return (String)this.get(v_name);
    }

    public void setName(String name) {
        put(v_name,name);
    }

    public String getType() {
        return (String)this.get(v_type);
    }

    public void setType(String typeName) {
        put(v_type,typeName);
    }

    public boolean isBinary() {
        return null==this.get(v_isBinary)?false:(Boolean)this.get(v_isBinary);
    }

    public void setBinary(boolean binary) {
        if(binary)
            put(v_isBinary,binary);
    }

    public boolean isArray() {
        return null==this.get(v_isArray)?false:(Boolean)this.get(v_isArray);
    }

    public void setArray(boolean array) {
        if(array)
            put(v_isArray,array);
    }

    public boolean isPrimitive() {
        return null==this.get(v_isPrimitive)?false:(Boolean)this.get(v_isPrimitive);
    }

    public void setPrimitive(boolean primitive) {
        if(primitive)
            put(v_isPrimitive,primitive);
    }
}
