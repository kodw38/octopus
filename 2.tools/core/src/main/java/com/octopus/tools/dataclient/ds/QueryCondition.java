package com.octopus.tools.dataclient.ds;

import com.octopus.tools.dataclient.ds.field.FieldDef;
import com.octopus.tools.dataclient.ds.store.FieldCondition;

import java.util.ArrayList;

/**
 * User: Administrator
 * Date: 14-9-19
 * Time: 下午2:18
 */
public class QueryCondition {
    ArrayList<FieldCondition> queryCondition;
    ArrayList<FieldDef> queryFields;
    ArrayList<FieldDef> orderFields;

    public FieldDef[] getOrderFields() {
        if(null != orderFields)
        return orderFields.toArray(new FieldDef[orderFields.size()]);
        return null;
    }

    public void setOrderFields(ArrayList<FieldDef> orderFields) {
        if(this.orderFields ==null) this.orderFields=new ArrayList<FieldDef>();
        if(null != orderFields){
            for(FieldDef c:orderFields)
                this.orderFields.add(c);
        }
    }
    public void addOrderField(FieldDef fd){
        if(null == orderFields)  this.orderFields=new ArrayList<FieldDef>();
        if(null != fd)
            orderFields.add(fd);
    }

    public FieldCondition[] getFieldCondition() {
        if(null != queryCondition && queryCondition.size()>0)
        return queryCondition.toArray(new FieldCondition[queryCondition.size()]);
        return null;
    }

    public void setFieldCondition(FieldCondition[] queryCondition) {
        if(this.queryCondition ==null) this.queryCondition=new ArrayList<FieldCondition>();
        if(null != queryCondition){
            for(FieldCondition c:queryCondition)
                this.queryCondition.add(c);
        }
    }
    public void addFieldCondition(FieldCondition condition){
        if(null == queryCondition)  this.queryCondition=new ArrayList<FieldCondition>();
        if(null != condition)
            queryCondition.add(condition);
    }

    public FieldDef[] getQueryField() {
        return queryFields.toArray(new FieldDef[queryFields.size()]);
    }

    public void setQueryField(FieldDef[] queryField) {
        if(this.queryFields == null) this.queryFields = new ArrayList<FieldDef>();
        if(null != queryField){
            for(FieldDef f:queryField)
                this.queryFields.add(f);
        }
    }

    public void addQueryField(FieldDef fd){
        if(null == queryFields)  this.queryFields=new ArrayList<FieldDef>();
        this.queryFields.add(fd);
    }
}
