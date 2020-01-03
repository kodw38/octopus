package com.octopus.tools.dataclient.v2;

import com.octopus.utils.ds.Condition;
import com.octopus.utils.ds.TableBean;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.List;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-8-14
 * Time: 下午4:56
 */
public interface IDataSource {
    /**
     * 一种文件连接查询
     * @param file  文件名，表名
     * @param queryFields  查询字段
     * @param fieldValues  字段条件
     * @param outs         其他要求
     * @param start
     * @param end
     * @return
     */
    public List<Map<String, Object>> query(String tradeId,String file, String[] queryFields, List<Condition> fieldValues, Map<String, String> outs, int start, int end,TableBean tb) throws Exception;

    public int getCount(String tradeId,String file, String[] queryFields, List<Condition> fieldValues,TableBean tb)throws Exception;

    public List<Map<String, String>> queryAsString(String tradeId,String file, String[] queryFields, List<Condition> fieldValues, Map<String, String> outs, int start, int end,TableBean tb) throws Exception;

    public List<Map<String,Object>> query(String tradeId,String sql,Map map,int start,int end)throws Exception;
    public boolean addRecord(XMLParameter env,String tradeId,String taskId,String file,Map<String,Object> fieldValues) throws Exception;

    public boolean addRecords(XMLParameter env,String tradeId,String taskId,String file,List<Map<String,Object>> fieldValues) throws Exception;

    public boolean insertRecord(XMLParameter env,String tradeId,String taskId,String file,Map<String,Object> fieldValues,int insertPosition) throws Exception;

    public boolean insertRecords(XMLParameter env,String tradeId,String taskId,String file,List<Map<String,Object>> fieldValues,int insertPosition) throws Exception;

    public boolean delete(XMLParameter env,String tradeId,String taskId,String file,List<Condition> fieldValues,TableBean tb) throws Exception;

    public boolean update(XMLParameter env,String tradeId,String taskId,String file,List<Condition> fieldValues,Map<String,Object> updateData,TableBean tb)throws Exception;

    public IDataSource getDataSource(String name) throws Exception;

    public boolean exist(String tradeId,String tableName) throws Exception;

    public long getNextSequence(String name)throws Exception;
}