package com.octopus.tools.dataclient.impl;

import com.octopus.tools.dataclient.IDataClient;
import com.octopus.tools.dataclient.IDataEngine;
import com.octopus.tools.dataclient.IDataRouter;
import com.octopus.tools.dataclient.ds.*;
import com.octopus.tools.dataclient.ds.field.*;
import com.octopus.tools.dataclient.ds.store.DataOperatorContainer;
import com.octopus.tools.dataclient.ds.store.FieldCondition;
import com.octopus.tools.dataclient.ds.store.TableValue;
import com.octopus.tools.dataclient.impl.engines.DBPoolEngine;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * User: Administrator
 * Date: 14-10-8
 * Time: 上午10:36
 */
public class DataClient extends XMLDoObject implements IDataClient {
    transient static Log log = LogFactory.getLog(DataClient.class);
    IDataRouter router;
    HashMap engines;
    public DataClient(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    @Override
    public boolean store(DataOperatorContainer sc,Object env) throws Exception{
        //System.out.println("----------dataclient store---------");
        if(sc.getAddDataMap().size()>0 || sc.getAddTableList().size()>0){
            Map<FieldDef,List> add = sc.getAddDataMap();
            List<TableValue> tvs= new ArrayList<TableValue>();
            if(null != add){
                TableValue[] tableValues = convertTable(add);
                if(ArrayUtils.isNotEmpty(tableValues)){
                    for(TableValue t:tableValues)
                        tvs.add(t);
                }
            }
            if(null !=sc.getAddTableList() && sc.getAddTableList().size()>0)
                tvs.addAll(sc.getAddTableList());
            if(tvs.size()>0){
                batchAdd(tvs.toArray(new TableValue[tvs.size()]),env);

            }
        }
        if(sc.getDeleteDataList().size()>0){
            List<DelCnd> ds = sc.getDeleteDataList();
            for(DelCnd d:ds){
                delete(d,env);
            }
        }
        if(sc.getUpdateDataMap().size()>0){
            List<UpdateData> ul = sc.getUpdateDataMap();
            if(ul.size()>0){
                if(ul.size()==1)
                    update(ul.get(0));
                else
                    updateBatch(ul.toArray(new UpdateData[0]));
            }
        }
        return true;
    }
    //把字段->字段值的Map转变为TableValue[]
    TableValue[] convertTable(Map<FieldDef,List> add)throws Exception{
        check(add);
        return getTableFrom(add);
    }
    //检查字段的依赖关系，有关系的字段不在ds中将报出。
    void check(Map<FieldDef,List> ds)throws Exception{
        FieldDef[] fs = ds.keySet().toArray(new FieldDef[0]);
        //every field must depend fields in the fs
        for(FieldDef f:fs){
            List<FieldRel> rs = FieldRelContainer.getRelField(f);
            if(null != rs){
                for(FieldRel r:rs){
                    if(r.getRelType()==FieldRel.REL_TYPE_NOTNULL){
                        if(!ArrayUtils.isInObjectArray(fs,r.getRelField())){
                            throw new Exception("the field["+f.toString()+"] depend field["+r.getRelField().toString()+"] not exist in this operate data.");
                        }
                    }
                }
            }
        }
    }

    TableValue[] getTableFrom(Map<FieldDef,List> add)throws Exception{
        FieldDef[] fs = add.keySet().toArray(new FieldDef[0]);
        TableDef[] ts = getTable(fs);
        return getTableValue(ts,add);
    }
    //从字段定义中获取表定义
    TableDef[] getTable(FieldDef[] fs)throws Exception{
        if(null != fs){
            List<TableDef> li = new ArrayList<TableDef>();
            for(FieldDef f:fs){
                TableDef[] tds = TableDefContainer.getBelongTables(fs);
                if(ArrayUtils.isEmpty(tds))throw new Exception("not find tableDef by fields");
                for(TableDef td:tds){
                    FieldDef[] mf = td.getMustFields();
                    if(null != mf){
                        for(FieldDef m:mf){
                            if(!ArrayUtils.isInObjectArray(fs,m)){
                                throw new Exception("the field["+m.getFieldCode()+"] of table["+td.getName()+"] is not exist in parameters.");
                            }
                        }
                    }
                    if(!li.contains(td))
                        li.add(td);
                }
            }
            if(li.size()>0)
                return li.toArray(new TableDef[0]);
        }
        return null;
    }
    //根据表定义和字段值，合并表数据
    TableValue[] getTableValue(TableDef[] ts,Map<FieldDef,List> add)throws Exception{
        if(null != ts){
            List<TableValue> ret = new ArrayList<TableValue>();
            HashMap<FieldDef,List> one = new HashMap<FieldDef,List>();
            for(TableDef t:ts){
                for(FieldDef f:t.getFieldDefs()){
                    Iterator<FieldDef> ks = add.keySet().iterator();
                    while(ks.hasNext()){
                        FieldDef af = ks.next();
                        if(af.equals(f)){
                            one.put(f,add.get(af));
                        }
                    }
                }
                if(one.size()>0){
                    TableValue v = getTableValue(t, one);
                    if(null != v)
                        ret.add(v);
                }
            }
            return ret.toArray(new TableValue[0]);
        }
        return null;
    }
    //合并一张表数据
    TableValue getTableValue(TableDef td, Map<FieldDef,List> fvs){
        int maxLen= 0;
        Iterator ks = fvs.keySet().iterator();
        while(ks.hasNext()){
            int m = fvs.get(ks.next()).size();
            if(m>maxLen)
                maxLen=m;
        }
        List<Object[]> cls = new ArrayList();
        for(int i=0;i<maxLen;i++){
            Object[] vs = new Object[td.getFieldDefs().length];
            for(int j=0;j<vs.length;j++){
                if(null != fvs.get(td.getFieldDefs()[j]) && fvs.get(td.getFieldDefs()[j]).size()>i){
                    vs[j]=fvs.get(td.getFieldDefs()[j]).get(i);
                }
                //为空设置为上一行的值
                if(vs[j]==null && cls.size()>0){
                    vs[j]= cls.get(cls.size()-1)[j];
                }
            }
            cls.add(vs);
        }
        TableValue tv = new TableValue();
        tv.setTableDef(td);
        tv.setRecordValues(cls);
        return tv;
    }
    //保存表数据到环境数据匹配的数据源中
    boolean batchAdd(TableValue[] tableValues,Object env) throws Exception {
        if(null != tableValues){
            Map<String,List<TableValue>> map = new HashMap<String, List<TableValue>>();
            for(TableValue t:tableValues){
                if(!map.containsKey(t.getTableDef().getDataSource())){
                    map.put(t.getTableDef().getDataSource(),new ArrayList<TableValue>());
                }
                map.get(t.getTableDef().getDataSource()).add(t);
            }
            Iterator its = map.keySet().iterator();
            while(its.hasNext()){
                //先默认用datasource作为路由要素
                String dc = (String)its.next();
                if(dc==null||"null".equals(dc))dc = "local";
                IDataEngine[] engine = router.getRouter(dc,env);
                if(null == engine)throw new Exception("not find dataEngine by dataSource="+dc);
                for(IDataEngine end:engine)
                    end.add(tableValues,env);

            }
            return true;
        }
        return false;
    }
    //查询条件中Field肯定都是存在关系的
    public TableValue query(QueryCondition cnd,Object env)throws Exception{
        //请求FieldValue关系交集
        if(null != cnd){
            //先不考虑跨datasource,跨表操作
                //先默认用datasource作为路由要素
            TableDef[] tf = TableDefContainer.getBelongTables(cnd.getQueryField());
            if(null == tf)throw new Exception("not find tableDef by queryCondition["+cnd.toString()+"]");

            String dc = tf[0].getDataSource();
            IDataEngine[] engine = router.getRouter(dc, env);
            if(null == engine)throw new Exception("not find dataEngine by dataSource="+dc);
            List<TableValue>ret = new ArrayList<TableValue>();
            for(IDataEngine end:engine){
               TableValue r =  end.query(cnd, env);
               if(null != r)
                   ret.add(r);
            }
            //合并结果集
            if(ret.size()>0){
                TableValue result = ret.get(0);
                if(ret.size()>1){
                    for(int i=1;i<ret.size();i++){
                        result.merge(ret.get(i));
                    }
                }
                return result;
            }


        }
        return null;
    }

    @Override
    public boolean add(AddData addData) throws Exception {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean delete(DelCnd delData,Object env) throws Exception {
            String tableName = delData.getConditions().get(0).getTableName();
            TableDef td = TableDefContainer.getTableDef(tableName);
            IDataEngine[] engine = router.getRouter(td.getDataSource(), env);
            if(null == engine)throw new Exception("not find dataEngine by dataSource="+td.getDataSource());
            for(IDataEngine end:engine){
                end.delete(delData, env);
            }
            return true;
    }

    @Override
    public boolean update(UpdateData updateData) throws Exception {
        if(updateData.getConditions().length>0){
            String tableName= updateData.getConditions()[0].getTableName();
            TableDef td = TableDefContainer.getTableDef(tableName);
            IDataEngine[] engine = router.getRouter(td.getDataSource(), null);
            if(null == engine)throw new Exception("not find dataEngine by dataSource="+td.getDataSource());
            for(IDataEngine end:engine){
                end.update(updateData);
            }
            return true;
        }
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object query(QueryCondition queryData) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object getMetaData(MetaCnd cnd) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object getMetaDataBatch(MetaCnd[] metaCnd)throws Exception{
        return null;
    }

    @Override
    public boolean addBatch(AddData[] datas) throws Exception {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean deleteBatch(DelCnd[] delCnds) throws Exception {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean updateBatch(UpdateData[] batches) throws Exception {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object queryBatch(QueryCondition[] batches) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * jsonpar="{op:'delete',db:'mysql/STOCK_DATA',where:['C_DATE<date(now,-12,day)']}"
     * @param env
     * @return
     */
    @Override
    public Object doSomeThing(String xmlid,XMLParameter env,Map input,Map output,Map cfg) {
        try{
            String op=null,db=null;
            List conds=null;
            List fields = null;
            Map datas=null;
            Map formate= null;
            List<String> sqls=null;
            if(null != input){
                op = (String)input.get("op");
                db = (String)input.get("db");
                conds = (List)input.get("conds");
                datas = (Map)input.get("datas");
                fields=(List)input.get("fields");
                formate= (Map)input.get("formate");
                sqls = (List)input.get("sqls");
            }
            if(null != sqls){
                IDataEngine[] engine = router.getRouter(db, env);
                for(String sql:sqls){
                    if(StringUtils.isNotBlank(sql))
                        ((DBPoolEngine)engine[0]).exeSql(db,sql);
                }
            }else if("delete".equals(op)){
                DataOperatorContainer doc = new DataOperatorContainer();
                List<FieldCondition> ls = new ArrayList<FieldCondition>();
                if(null!=conds){
                    DelCnd delCnd = new DelCnd();
                    for(int i=0;i<conds.size();i++){
                        String cond = (String)conds.get(i);
                        FieldCondition c = new FieldCondition(db,cond,null);
                        ls.add(c);
                    }
                    delCnd.setConditions(ls);
                    doc.deleteData(delCnd);
                }
                store(doc, env);
            }else if("add".equals(op)){
                DataOperatorContainer ad = new DataOperatorContainer();
                ad.addData(datas);
                store(ad,env);
            }else if("update".equals(op)){

            }else if("query".equals(op)){

                    QueryCondition cnd = new QueryCondition();
                    for(int i=0;i<fields.size();i++){
                        cnd.addQueryField(FieldContainer.getField((String)fields.get(i)));
                    }
                    if(null != conds){
                        for(int i=0;i<conds.size();i++){
                            cnd.addFieldCondition(new FieldCondition(db,(String)conds.get(i),null));
                        }
                    }
                    TableValue tv = query(cnd,env);
                    return tv;
                    /*if(null != formate && null != tv){
                        Object o = Class.forName((String)formate.get("clazz")).newInstance();
                        if(o instanceof Map){
                            Map map = (Map)o;
                            String key = (String)formate.get("map_key");
                            Object value = formate.get("map_value");
                            Class z=null;
                            HashMap<String,String> temp=null;
                            if(value instanceof JSONObject){
                                z = Class.forName((String)((JSONObject)value).get("clazz"));
                                temp = new HashMap<String, String>();
                                Iterator its = ((JSONObject)value).keys();
                                while(its.hasNext()){
                                    String k = (String)its.next();
                                    temp.put((String)((JSONObject)value).get(k),k);
                                }
                            }
                            List<Object[]> os = tv.getRecordValues();
                            FieldDef[] fd = tv.getTableDef().getFieldDefs();
                            for(Object[] to:os){
                                Object tkey =null;
                                Object tvalue=null;
                                if(null != z){
                                    tvalue = z.newInstance();
                                }
                                for(int i=0;i<fd.length;i++){
                                    if(fd[i].getFieldCode().equals(key)){
                                        tkey=to[i];
                                    }
                                    if(null != tvalue){
                                        String name = (String)temp.get(fd[i].getFieldCode());
                                        if(StringUtils.isNotBlank(name))
                                            ClassUtils.setFieldValue(tvalue,name,to[i],false);
                                    }else{
                                       if(fd[i].getFieldCode().equals(value)){
                                           tvalue=to[i];
                                       }
                                    }
                                }
                                if(map.containsKey(tkey)){
                                    ((List)map.get(tkey)).add(tvalue);
                                }else{
                                    List li = new ArrayList();
                                    li.add(tvalue);
                                    map.put(tkey,li);
                                }
                            }
                            return map;
                        }else{
                            HashMap<String,String> temp = new HashMap<String, String>();
                            Iterator its = formate.keySet().iterator();
                            while(its.hasNext()){
                                String k = (String)its.next();
                                temp.put((String)formate.get(k),k);
                            }
                            ArrayList ret = new ArrayList();
                            List<Object[]> os = tv.getRecordValues();
                            FieldDef[] fd = tv.getTableDef().getFieldDefs();
                            for(Object[] to:os){
                                for(int i=0;i<fd.length;i++){
                                    String name = (String)temp.get(fd[i].getFieldCode());
                                    if(StringUtils.isNotBlank(name))
                                        ClassUtils.setFieldValue(o,name,to[i],false);
                                }
                                ret.add(o);
                            }
                            return ret;
                        }
                    }*/

            }

            return null;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid,XMLParameter env, Map input,Map output, Map cfg) throws Exception {
        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResultCheck checkReturn(String xmlid,XMLParameter env, Map input,Map output, Map cfg, Object ret) throws Exception {
        if(log.isDebugEnabled()){
            System.out.println("dataclient return:"+ret);
        }
        return new ResultCheck(true,ret);  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config,Object ret,Exception e) throws Exception {
        throw new Exception("now support rollback");
    }

}
