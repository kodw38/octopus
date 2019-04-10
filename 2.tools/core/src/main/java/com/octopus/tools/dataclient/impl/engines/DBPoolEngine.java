package com.octopus.tools.dataclient.impl.engines;

import com.octopus.tools.dataclient.IDataEngine;
import com.octopus.tools.dataclient.ds.DelCnd;
import com.octopus.tools.dataclient.ds.QueryCondition;
import com.octopus.tools.dataclient.ds.UpdateData;
import com.octopus.tools.dataclient.ds.field.TableDef;
import com.octopus.tools.dataclient.ds.field.TableDefContainer;
import com.octopus.tools.dataclient.ds.store.FieldCondition;
import com.octopus.tools.dataclient.ds.store.FieldValue;
import com.octopus.tools.dataclient.ds.store.TableValue;
import com.octopus.tools.dataclient.impl.engines.impl.DBPool;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;

import java.util.HashMap;
import java.util.Map;

/**
 * User: Administrator
 * Date: 14-9-23
 * Time: 上午11:21
 */
public class DBPoolEngine extends XMLObject implements IDataEngine {
    DCS dcs;
    HashMap<String,IPool> pools;
    ITyper typer;
    public DBPoolEngine(XMLMakeup xml,XMLObject parent,Object[] containers) throws Exception {
        super(xml,parent,containers);

    }

    @Override
    public void notifyObject(String op, Object obj) throws Exception {

    }

    @Override
    public void addTrigger(Object cond, InvokeTaskByObjName task) throws Exception {

    }

    @Override
    public void destroy() throws Exception {

    }

    @Override
    public void initial() throws Exception {

    }

    IPool getPool(String key)throws Exception{
/*
        Iterator its = pools.values().iterator();
        IPool pool;
        IPool defaultPool=null;
        while(its.hasNext()){
            pool=(IPool)its.next();

            if(pool.isMatch(dc,env)){
                return pool;
            }else if(defaultPool == null && pool.isDefault()){
                defaultPool=pool;
            }
        }
        return defaultPool;
*/
        return pools.get(key);
    }

    @Override
    public Object add(String opId, Map[] data,Object env) throws Exception {
        /*DC dc = dcs.getDC(opId);
        if(null == dc){
            throw new Exception("not find dc by dc id["+opId+"]");
        }
        IPool pool = getPool(dc,env);
        Object ret = ((DBPool)pool).exeSql((String)dc.get(DC.KEY_OP_STR),data);
        if(null != ret && null != typer)
            ret = typer.typer(ret);
        return ret;*/
        return null;
    }

    @Override
    public boolean add(TableValue[] tableValues,Object env) throws Exception {
        for(TableValue v:tableValues){
            //DC dc = dcs.getDC(v.getTableDef().getDataSource());
            String k = v.getTableDef().getDataSource();//v.getTableDef().getName().split("/")[0];
            if(StringUtils.isBlank(k) || "null".equals(k))k="mysql";
            IPool pool = getPool(k);
            if(null != pool)
                ((DBPool)pool).addTable(v);
            else
                throw new Exception("not find db pool by table name ["+k+"]");
        }
        return true;
    }

    @Override
    public TableValue query(QueryCondition cnd, Object env) throws Exception {
        //目前实现为单表查询
        TableDef[] tf = TableDefContainer.getBelongTables(cnd.getQueryField());
        if(null != tf){
            String k = tf[0].getName().split("/")[0];
            IPool pool = getPool(k);
            if(null != pool)
                return ((DBPool)pool).query(cnd);
            else
                throw new Exception("not find db pool by table name ["+k+"]");
        }
        throw new Exception(" not find tabledef by queryCondition["+cnd.toString()+"]");
    }

    public void exeSql(String db,String sql) throws Exception {
        String k = db.split("/")[0];
        IPool pool = getPool(k);
        if(null != pool)
            ((DBPool)pool).exeSql(sql,null);
    }

    public TableValue queryByTableName(String tableName,Object env)throws Exception{
        if(null != tableName){
            String[] k = tableName.split("/");
            IPool pool = getPool(k[0]);
            if(null != pool)
                return ((DBPool)pool).queryTable(k[1]);
            else
                throw new Exception("not find db pool by table name ["+k+"]");
        }
        return null;
    }

    @Override
    public Object delete(DelCnd delData,Object env) throws Exception {
        String tableName = delData.getConditions().get(0).getTableName();
        if(null != tableName){
            String[] k = tableName.split("/");
            IPool pool = getPool(k[0]);
            if(null != pool)
                return ((DBPool)pool).exeSql(getDelSql(delData),null);
            else
                throw new Exception("not find db pool by table name ["+k+"]");
        }
        return true;
    }
    public String getDelSql(DelCnd d){
        StringBuffer sb = new StringBuffer();
        sb.append("delete from ");
        if(StringUtils.isNotBlank(d.getConditions().get(0).getTableName())){
            sb.append(d.getConditions().get(0).getTableName().split("/")[1]);
        }
        sb.append(" ").append(" where 1=1 ");
        for(FieldCondition fc:d.getConditions()){
            sb.append(" and ");
            sb.append(fc.getField().getFieldCode());
            sb.append(fc.getValueCond().getCondTypeStr());
        }
        return sb.toString();
    }

    public boolean update(UpdateData updateData)throws Exception{
        String tableName = updateData.getConditions()[0].getTableName();
        if(null != tableName){
            String[] k = tableName.split("/");
            IPool pool = getPool(k[0]);
            if(null != pool){
                String sql = getUpdateSql(updateData);
                if(StringUtils.isNotBlank(sql))
                    return (Boolean)((DBPool)pool).exeSql(sql,null);
            }else
                throw new Exception("not find db pool by table name ["+k+"]");
        }
        return true;
    }
    String getUpdateSql(UpdateData updateData){
        String tableName = updateData.getConditions()[0].getTableName();
        if(null != tableName){
            String[] k = tableName.split("/");
            StringBuffer sb = new StringBuffer();
            sb.append("update ").append(k[1]).append(" set ");
            for(int i=0;i<updateData.getFieldValues().length;i++){
                FieldValue f = updateData.getFieldValues()[i];
                if(i==0)
                    sb.append(f.getFieldDef().getFieldCode()).append("=").append(f.getValue().toString());
                else
                    sb.append(",").append(f.getFieldDef().getFieldCode()).append("=").append(f.getValue().toString());
            }
            sb.append(" where 1=1 ");
            for(int i=0;i<updateData.getConditions().length;i++){
                sb.append(" and ");
                sb.append(updateData.getConditions()[i].getField().getFieldCode());
                if(updateData.getConditions()[i].getValueCond().getCondType()==FieldCondition.EQUAL){
                    sb.append("='").append(updateData.getConditions()[i].getValueCond().getPar1().toString()).append("'");
                }
            }
            return sb.toString();
        }
        return null;
    }
    @Override
    public Object update(String opId, Map cnd, Map data,Object env) throws Exception {
        return add(opId,new Map[]{cnd},env);
    }

    @Override
    public Object rollbackAdd(String opId, Map[] data,Object env) throws Exception {
        /*DC dc = dcs.getDC(opId);
        if(null == dc){
            throw new Exception("not find dc by dc id["+opId+"]");
        }
        IPool pool = getPool(dc,env);
        Object ret = ((DBPool)pool).exeSql((String)dc.get(DC.KEY_ROLLBACK_OP_STR),data);
        if(null != ret && null != typer)
            ret = typer.typer(ret);
        return ret;*/
        return null;
    }

    @Override
    public Object rollbackDelete(String opId, Map cnd,Object env) throws Exception {
        return rollbackAdd(opId,new Map[]{cnd},env);
    }

    @Override
    public Object rollbackUpdate(String opId, Map cnd, Map data,Object env) throws Exception {
        return rollbackAdd(opId,new Map[]{cnd},env);
    }

    @Override
    public Object query(String opId, Map cnd, int startIndex, int endIndex,Object env) throws Exception {
        /*DC dc = dcs.getDC(opId);
        if(null == dc){
            throw new Exception("not find dc by dc id["+opId+"]");
        }
        IPool pool = getPool(dc,env);
        Object ret = ((DBPool)pool).querySql((String)dc.get(DC.KEY_ROLLBACK_OP_STR),cnd,startIndex,endIndex);
        if(null != ret && null != typer)
            ret = typer.typer(ret);
        return ret;*/
        return null;
    }

    @Override
    public Object getMetaData(String opId, Map cnd,Object env) throws Exception {
        /*DC dc = dcs.getDC(opId);
        if(null == dc){
            throw new Exception("not find dc by dc id["+opId+"]");
        }
        IPool pool = getPool(dc,env);
        Object ret = ((DBPool)pool).getMetaData((String)dc.get(DC.KEY_ROLLBACK_OP_STR),cnd);
        if(null != ret && null != typer)
            ret = typer.typer(ret);
        return ret;*/
        return null;
    }


}
