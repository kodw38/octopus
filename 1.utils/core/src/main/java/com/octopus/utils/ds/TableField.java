package com.octopus.utils.ds;

import java.util.List;

/**
 * User: wfgao_000
 * Date: 15-11-16
 * Time: 上午11:27
 */
public class TableField  {
    public static final String FIELD_USED_TYPE_PK = "P";    //主键
    public static final String FIELD_USED_TYPE_QUERY = "Q"; //一般查询字段
    public static final String FIELD_USED_TYPE_INDEX = "I"; //索引字段
    public static final String FIELD_USED_TYPE_LIKE = "W";   //模糊收缩
    public static final String FIELD_USED_TYPE_OUTKEY = "O"; //外键

    FieldBean field;
    List usedTypes;//多个用","分割
    boolean notNull;
    String tableNum; //表代表数字，压缩用
    boolean isCache;//缓存字段，可以通过这些字段做cache搜索，解决分表分库查询

    public FieldBean getField() {
        return field;
    }


    public boolean isCache() {
        return isCache;
    }

    public void setCache(boolean isCache) {
        this.isCache = isCache;
    }

    public void setField(FieldBean field) {
        this.field = field;
    }

    public List<String> getUsedTypes() {
        return usedTypes;
    }

    public void setUsedTypes(List<String> usedTypes) {
        this.usedTypes = usedTypes;
    }

    public boolean isNotNull() {
        return notNull;
    }

    public void setNotNull(boolean notNull) {
        this.notNull = notNull;
    }

    public String getTableNum() {
        return tableNum;
    }

    public void setTableNum(String tableNum) {
        this.tableNum = tableNum;
    }


    public String toString(){
        StringBuffer sb = new StringBuffer();
        sb.append(field);
        sb.append(",");
        sb.append(usedTypes);//多个用","分割
        sb.append(",");
        sb.append(notNull);
        sb.append(",");
        sb.append(tableNum); //表代表数字，压缩用
        sb.append(",");
        return sb.toString();
    }
}
