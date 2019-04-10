package com.octopus.tools.dataclient;

import com.octopus.tools.dataclient.ds.*;
import com.octopus.tools.dataclient.ds.store.DataOperatorContainer;
import com.octopus.tools.dataclient.ds.store.TableValue;

/**
 * 对使用者屏蔽文件放哪，怎么或取。只要关心需要的数据。
 * User: Administrator
 * Date: 14-8-25
 * Time: 下午1:29
 */
public interface IDataClient {

    public boolean store(DataOperatorContainer sc,Object env) throws Exception;
    public TableValue query(QueryCondition cnd,Object env)throws Exception;

    public boolean add(AddData addData)throws Exception;
    public boolean delete(DelCnd delCnd,Object env)throws Exception;
    public boolean update(UpdateData updateData)throws Exception;
    public Object query(QueryCondition queryData)throws Exception;

    public Object getMetaData(MetaCnd metaCnd)throws Exception;
    public Object getMetaDataBatch(MetaCnd[] metaCnd)throws Exception;

    public boolean addBatch(AddData[] datas)throws Exception;
    public boolean deleteBatch(DelCnd[] delCnds)throws Exception;
    public boolean updateBatch(UpdateData[] batches)throws Exception;
    public Object queryBatch(QueryCondition[] batches)throws Exception;

}
