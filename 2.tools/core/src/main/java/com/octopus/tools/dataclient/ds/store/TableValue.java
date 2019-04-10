package com.octopus.tools.dataclient.ds.store;


import com.octopus.tools.dataclient.ds.field.TableDef;

import java.io.Serializable;
import java.util.List;

/**
 * User: wf
 * Date: 2008-8-28
 * Time: 12:48:04
 */
public class TableValue implements Serializable{
    private Long cacheMaxPk;
    private Long cacheMinPk;
    private Long resultMaxPk;
    private Long resultMinPk;
    private List recordValues;
    private TableDef tableDef;
    private FieldCondition[] queryCondition;
    public static final char SPLIT_CHAR_DBTABLE =6;
    public static final char SPLIT_CHAR_FIELDS =7;

    public TableDef getTableDef() {
        return tableDef;
    }


    public void setTableDef(TableDef tableDef) {
        this.tableDef = tableDef;
    }

    public Long getCacheMaxPk() {
        return cacheMaxPk;
    }

    public void setCacheMaxPk(Long cacheMaxPk) {
        this.cacheMaxPk = cacheMaxPk;
    }

    public Long getCacheMinPk() {
        return cacheMinPk;
    }

    public void setCacheMinPk(Long cacheMinPk) {
        this.cacheMinPk = cacheMinPk;
    }

    public Long getResultMaxPk() {
        return resultMaxPk;
    }

    public void setResultMaxPk(Long resultMaxPk) {
        this.resultMaxPk = resultMaxPk;
    }

    public Long getResultMinPk() {
        return resultMinPk;
    }

    public void setResultMinPk(Long resultMinPk) {
        this.resultMinPk = resultMinPk;
    }


    public List<Object[]> getRecordValues() {
        return recordValues;
    }

    public void setRecordValues(List recordValues) {
        this.recordValues = recordValues;
    }

    public FieldCondition[] getQueryCondition() {
        return queryCondition;
    }

    public void setQueryCondition(FieldCondition[] queryCondition) {
        this.queryCondition = queryCondition;
    }

    public void merge(TableValue tv){
        getRecordValues().addAll(tv.getRecordValues());
    }

    public StringBuffer toStringBuffer(){
        StringBuffer sb = new StringBuffer();
        for(int i=0;i<recordValues.size();i++){
            if(i==0){
                //dbid:tablename
                sb.append(tableDef.getDataSource()+SPLIT_CHAR_DBTABLE+tableDef.getName()+"\n");
                //field info
                for(int k=0;k<tableDef.getFieldDefs().length;k++){
                    if(k==0){
                        sb.append(tableDef.getFieldDefs()[k].getFieldCode());
                    }else{
                        sb.append(SPLIT_CHAR_FIELDS+tableDef.getFieldDefs()[k].getFieldCode());
                    }
                }
                sb.append("\n");
            }
            //record info
            Object[] oneRecord = (Object[])recordValues.get(i);
            for(int k=0;k<oneRecord.length;k++){
                if(k==0){
                    sb.append(oneRecord[k].toString());
                }else{
                    sb.append(","+oneRecord[k].toString());
                }
            }
            sb.append("\n");
        }
        return sb;
    }
    
}
