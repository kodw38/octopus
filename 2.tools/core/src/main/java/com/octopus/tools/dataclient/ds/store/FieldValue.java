package com.octopus.tools.dataclient.ds.store;

import com.octopus.tools.dataclient.ds.field.FieldDef;

/**
 * User: wf
 * Date: 2008-8-25
 * Time: 23:39:35
 */
public class FieldValue {
    FieldDef fieldDef;
    private Object value;
    private Long rowId ;

    public FieldValue(){

    }

    public void setValue(Object value) {
        this.value = value;
    }

    public FieldDef getFieldDef() {
        return fieldDef;
    }

    public void setFieldDef(FieldDef fieldDef) {
        this.fieldDef = fieldDef;
    }

    public FieldValue(Object value,Long rowid){
        this.value = value;
        this.rowId = rowid;
    }

    public Object getValue() {
        return value;
    }

    public Long getRowId() {
        return rowId;
    }

}
