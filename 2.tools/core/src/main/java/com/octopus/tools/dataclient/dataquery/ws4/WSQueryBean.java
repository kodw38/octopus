package com.octopus.tools.dataclient.dataquery.ws4;

import com.octopus.tools.dataclient.dataquery.FieldMapping;
import com.octopus.tools.dataclient.dataquery.FieldValue;

/**
 * User: wfgao_000
 * Date: 15-8-5
 * Time: 下午4:38
 */
public class WSQueryBean {
    String[] queryFields;
    FieldMapping[] fieldMapping;
    FieldValue[] fieldValue;

    public String[] getQueryFields() {
        return queryFields;
    }

    public void setQueryFields(String[] queryFields) {
        this.queryFields = queryFields;
    }

    public FieldMapping[] getFieldMapping() {
        return fieldMapping;
    }

    public void setFieldMapping(FieldMapping[] fieldMapping) {
        this.fieldMapping = fieldMapping;
    }

    public FieldValue[] getFieldValue() {
        return fieldValue;
    }

    public void setFieldValue(FieldValue[] fieldValue) {
        this.fieldValue = fieldValue;
    }

}
