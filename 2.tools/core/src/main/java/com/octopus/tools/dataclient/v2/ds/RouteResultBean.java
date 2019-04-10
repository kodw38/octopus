package com.octopus.tools.dataclient.v2.ds;

import java.util.List;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-12-6
 * Time: 下午12:44
 */
public class RouteResultBean {
    String dataSource;
    String tableName;
    String originalTableName;
    String op;
    List<String> queryFields;
    List<String> keyfields;
    List<Map> structure;
    Object datas;
    Object conds;
    Map format;
    int start;
    int end;
    Boolean isForceDB;
    Map fieldsMapping;

    public List<Map> getStructure() {
        return structure;
    }

    public void setStructure(List<Map> structure) {
        this.structure = structure;
    }

    public Map getFieldsMapping() {
        return fieldsMapping;
    }

    public void setFieldsMapping(Map fieldsMapping) {
        this.fieldsMapping = fieldsMapping;
    }

    public Boolean isForceDB() {
        return isForceDB;
    }

    public void setForceDB(Boolean isForceDB) {
        this.isForceDB = isForceDB;
    }

    List<String> sqls;

    public List<String> getKeyfields() {
        return keyfields;
    }

    public String getOriginalTableName() {
        return originalTableName;
    }

    public void setOriginalTableName(String originalTableName) {
        this.originalTableName = originalTableName;
    }

    public void setKeyfields(List<String> keyfields) {
        this.keyfields = keyfields;
    }

    public List<String> getSqls() {
        return sqls;
    }

    public void setSqls(List<String> sqls) {
        this.sqls = sqls;
    }

    public String getOp() {
        return op;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public String getDataSource() {
        return dataSource;
    }

    public Map getFormat() {
        return format;
    }

    public void setFormat(Map format) {
        this.format = format;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<String> getQueryFields() {
        return queryFields;
    }

    public void setQueryFields(List<String> queryFields) {
        this.queryFields = queryFields;
    }

    public Object getDatas() {
        return datas;
    }

    public void setDatas(Object datas) {
        this.datas = datas;
    }

    public Object getConds() {
        return conds;
    }

    public void setConds(Object conds) {
        this.conds = conds;
    }
}
