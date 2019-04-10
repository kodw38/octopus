package com.octopus.tools.dataclient.ds.field;

/**
 * User: wf
 * Date: 2008-8-26
 * Time: 10:18:22
 */
public class FieldRel {
    public static final int REL_TYPE_NOTNULL =1;
    public static final int REL_TYPE_NULL = 2;
    private FieldDef relField;
    private int relType;


    public FieldRel(){}

    public FieldRel(FieldDef relField,int type){
        this.relField = relField;
        this.relType = type;
    }

    public FieldDef getRelField() {
        return relField;
    }

    public void setRelField(FieldDef relField) {
        this.relField = relField;
    }

    public int getRelType() {
        return relType;
    }

    public void setRelType(int relType) {
        this.relType = relType;
    }
}
