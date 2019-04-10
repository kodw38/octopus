package com.octopus.utils.ds;

import java.io.Serializable;

/**
 * User: wf
 * Date: 2008-8-25
 * Time: 23:00:56
 */
public class FieldBean implements Serializable{
    private long fieldId;
    private String fieldName;
    private String fieldCode;
    private String fieldType;
    private String realFieldType;
    private String DBFieldType;
    private String i18n;
    private int fieldLen;
    private String fieldNum; //字段代表数字，压缩用

    public String getDBFieldType() {
        return DBFieldType;
    }

    public void setDBFieldType(String DBFieldType) {
        this.DBFieldType = DBFieldType;
    }

    public String getFieldNum() {
        return fieldNum;
    }

    public String getRealFieldType() {
        return realFieldType;
    }

    public void setRealFieldType(String realFieldType) {
        this.realFieldType = realFieldType;
    }

    public void setFieldNum(String fieldNum) {
        this.fieldNum = fieldNum;
    }

    public long getFieldId() {
        return fieldId;
    }

    public void setFieldId(long fieldId) {
        this.fieldId = fieldId;
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
        this.fieldCode = fieldCode;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public int getFieldLen() {
        return fieldLen;
    }

    public void setFieldLen(int fieldLen) {
        this.fieldLen = fieldLen;
    }

    public String getI18n() {
        return i18n;
    }

    public void setI18n(String i18n) {
        this.i18n = i18n;
    }

    public String toString(){
        StringBuffer sb = new StringBuffer();
        sb.append(fieldId);
        sb.append(",");
        sb.append(fieldName);
        sb.append(",");
        sb.append(fieldCode);
        sb.append(",");
        sb.append(fieldType);
        sb.append(",");
        sb.append(realFieldType);
        sb.append(",");
        sb.append(fieldLen);
        sb.append(",");
        sb.append(fieldNum);
        sb.append(",");
        sb.append(i18n);
        return sb.toString();
    }
}
