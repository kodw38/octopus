package com.octopus.utils.alone.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * User: Administrator
 * Date: 15-1-12
 * Time: 下午8:29
 */
public class StructInfo {
    static transient Log log = LogFactory.getLog(StructInfo.class);
    String name;
    Class type=null;
    boolean isBinary;
    boolean isArray;
    boolean isPrimitive;
    boolean isGeneric;
    int modifier;
    StructInfo parent;
    List<StructInfo> children = new LinkedList<StructInfo>();
    public StructInfo(){

    }
    public StructInfo(String name,Class type,List<StructInfo> children){
        if(null != name)
            this.name=name;
        if(null != type)
            this.type=type;
        if(null != children)
            this.children=children;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Class getType() {
        return type;
    }

    public void setType(Class type) {
        this.type = type;

    }

    public boolean isBinary() {
        return isBinary;
    }

    public void setBinary(boolean binary) {
        isBinary = binary;
    }

    public boolean isArray() {
        return isArray;
    }

    public void setArray(boolean array) {
        isArray = array;
    }

    public boolean isPrimitive() {
        return isPrimitive;
    }

    public void setPrimitive(boolean primitive) {
        isPrimitive = primitive;
    }

    public boolean isGeneric() {
        return isGeneric;
    }

    public void setGeneric(boolean generic) {
        isGeneric = generic;
    }

    public int getModifier() {
        return modifier;
    }

    public void setModifier(int modifier) {
        this.modifier = modifier;
    }

    public StructInfo getParent() {
        return parent;
    }

    public void setParent(StructInfo parent) {
        this.parent = parent;
    }

    public List<StructInfo> getChildren() {
        return children;
    }

    public void setChildren(List<StructInfo> children) {
        this.children = children;
    }

    public Object getNewObject(String path){
        String[] sep = path.split("\\.");
        StructInfo tem = this;
        if(tem.getName().equals(sep[0])){
            int i;
            for(i=1;i<sep.length;i++){
                if(tem.getChildren().size()>0){
                    for(StructInfo s:tem.getChildren()){
                        if(s.getName().equals(sep[i])){
                            tem=s;
                            break;
                        }
                    }
                }

            }
            if(i==sep.length){
                try{
                    return tem.getType().newInstance();
                }catch (Exception e){
                   log.error(e);
                }
            }
        }
        return null;
    }
}
