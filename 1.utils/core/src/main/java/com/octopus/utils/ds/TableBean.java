package com.octopus.utils.ds;

import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.ObjectUtils;

import java.io.Serializable;
import java.util.List;

/**
 * User: Administrator
 * Date: 14-10-22
 * Time: 下午11:11
 */
public class TableBean implements Serializable{
    String tableName;
    List<TableField> tableFields;
    FieldBean pk=null;
    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<TableField> getTableFields() {
        return tableFields;
    }

    public void setTableFields(List<TableField> tableFields) {
        this.tableFields = tableFields;
    }
    public FieldBean getPkField(){
       if(null == pk){
           for(TableField f:tableFields){
               if(ArrayUtils.isInStringArray(f.getUsedTypes(),TableField.FIELD_USED_TYPE_PK)){
                   pk=f.getField();
                   break;
               }
           }
       }
        return pk;
    }
    public boolean existField(String fieldcode){
        if(null != tableFields){
            for(TableField f:tableFields){
                if(f.getField().getFieldCode().equalsIgnoreCase(fieldcode)){
                    return true;
                }
            }
        }
        return false;
    }
    public List getStructure() {
        return ObjectUtils.convertBeanList2MapList(tableFields);
    }
}
