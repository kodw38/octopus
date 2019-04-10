package com.octopus.tools.dataclient.dataquery;

/**
 * User: wfgao_000
 * Date: 15-8-5
 * Time: 下午5:19
 */
public class FieldValue {
    String field;
    String[] values;

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String[] getValues() {
        return values;
    }

    public void setValues(String[] values) {
        this.values = values;
    }
}
