package com.octopus.tools.dataclient.ds.field;

import java.io.Serializable;

/**
 * User: wf
 * Date: 2008-8-25
 * Time: 23:00:56
 */
public class FieldDef implements Serializable{
    private String fieldName;
    private String fieldCode;
    private String fieldType;
    private int typeLen;
    private boolean ispk = false;
    private boolean isUpdate = true;


    public FieldDef(){}

    public FieldDef(String fieldName, String fieldCode, String fieldType, int typeLen){
        this.fieldName = fieldName;
        this.fieldCode = fieldCode;
        this.fieldType =fieldType;
        this.typeLen = typeLen;
    }

    public FieldDef(String fieldName, String fieldCode, String fieldType, int typeLen, boolean isUpdate){
        this.fieldName = fieldName;
        this.fieldCode = fieldCode;
        this.fieldType =fieldType;
        this.typeLen = typeLen;
        this.isUpdate = isUpdate;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldCode() {
        return fieldCode;
    }

    public void setFieldCode(String fieldCode) {
        if(!isUpdate){
            throw new IllegalArgumentException(" The class "+this.getClass().getName()+" state is can't update.");
        }
        this.fieldCode = fieldCode;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        if(!isUpdate){
            throw new IllegalArgumentException(" The class "+this.getClass().getName()+" state is can't update.");
        }
        this.fieldType = fieldType;
    }

    public int getTypeLen() {
        return typeLen;
    }

    public void setTypeLen(int typeLen) {
        if(!isUpdate){
            throw new IllegalArgumentException(" The class "+this.getClass().getName()+" state is can't update.");
        }
        this.typeLen = typeLen;
    }


    public boolean isIspk() {
        return ispk;
    }

    public void setIspk(boolean ispk) {
        this.ispk = ispk;
    }


}
