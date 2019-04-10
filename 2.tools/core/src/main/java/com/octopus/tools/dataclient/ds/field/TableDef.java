package com.octopus.tools.dataclient.ds.field;

import java.io.Serializable;

/**
 * User: Administrator
 * Date: 14-10-22
 * Time: 下午11:11
 */
public class TableDef implements Serializable{
    String dataSource;
    String name;
    FieldDef[] fieldDefs;
    FieldDef[] mustFields;

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FieldDef[] getFieldDefs() {
        return fieldDefs;
    }

    public void setFieldDefs(FieldDef[] fieldDefs) {
        this.fieldDefs = fieldDefs;
    }

    public FieldDef[] getMustFields() {
        return mustFields;
    }

    public void setMustFields(FieldDef[] mustFields) {
        this.mustFields = mustFields;
    }
}
