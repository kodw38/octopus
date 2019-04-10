package com.octopus.tools.dataclient.ds;

import com.octopus.tools.dataclient.ds.store.FieldCondition;
import com.octopus.tools.dataclient.ds.store.FieldValue;

/**
 * User: Administrator
 * Date: 14-9-19
 * Time: 下午2:16
 */
public class UpdateData {
    FieldCondition[] conditions;
    FieldValue[] fieldValues;

    public FieldCondition[] getConditions() {
        return conditions;
    }

    public void setConditions(FieldCondition[] conditions) {
        this.conditions = conditions;
    }

    public FieldValue[] getFieldValues() {
        return fieldValues;
    }

    public void setFieldValues(FieldValue[] fieldValues) {
        this.fieldValues = fieldValues;
    }
}
