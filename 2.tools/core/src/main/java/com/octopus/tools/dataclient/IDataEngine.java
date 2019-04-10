package com.octopus.tools.dataclient;

import com.octopus.tools.dataclient.ds.DelCnd;
import com.octopus.tools.dataclient.ds.QueryCondition;
import com.octopus.tools.dataclient.ds.UpdateData;
import com.octopus.tools.dataclient.ds.store.TableValue;

import java.util.Map;

/**
 * 只负责数据的增、删、改、查操作
 * User: Administrator
 * Date: 14-8-25
 * Time: 下午2:09
 */
public interface IDataEngine {
    public boolean add(TableValue[] tableValues,Object env)throws Exception;
    public TableValue query(QueryCondition cnd,Object env)throws Exception;
    public TableValue queryByTableName(String tableName,Object env)throws Exception;
    public boolean update(UpdateData updateData)throws Exception;

    public Object add(String opId,Map[] data,Object env)throws Exception;

    public Object delete(DelCnd delData,Object env)throws Exception;
    public Object update(String opId,Map cnd,Map data,Object env)throws Exception;

    public Object rollbackAdd(String opId,Map[] data,Object env)throws Exception;
    public Object rollbackDelete(String opId,Map cnd,Object env)throws Exception;
    public Object rollbackUpdate(String opId,Map cnd,Map data,Object env)throws Exception;

    public Object query(String opId,Map cnd,int startIndex,int endIndex,Object env)throws Exception;

    public Object getMetaData(String opId,Map cnd,Object env)throws Exception;

}