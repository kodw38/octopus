package com.octopus.tools.dataclient.v2.ds;

import com.octopus.tools.dataclient.v2.IDataSource;
import com.octopus.tools.dataclient.v2.ISequence;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.ds.*;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.*;

/**
 * Created by robai on 2017/11/6.
 */
public class HBaseDataSource extends XMLDoObject implements IDataSource {
    Configuration configuration=null;
    Connection connection=null;
    TableContainer tablecontainer;
    HashMap<String , List<Map>> tradeDataList = new HashMap();
    public HBaseDataSource(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        if(null != xml) {
            Properties p = xml.getPropertiesByChildNameValue();
            configuration = new Configuration();
            Iterator its = p.keySet().iterator();
            while (its.hasNext()) {
                Object o = its.next();
                configuration.set((String) o, p.getProperty((String) o));
            }
        }

    }

    public void doInitial(){
        try {
            //log.error("classloader:"+this.getClass().getClassLoader());
            //log.error("classloader:"+ConnectionFactory.class.getClassLoader());
            connection = ConnectionFactory.createConnection(configuration);
            //dropTable("FLOW_INTERRUPT_DATA");
            //createTable("FLOW_INTERRUPT_DATA","id","data","svinfo");
        }catch (Exception e){
            log.error("initial HBase Client error",e);
        }
    }


    @Override
    public List<Map<String, Object>> query(String tradeId,String file, String[] queryFields, List<Condition> fieldValues, Map<String, String> outs, int start, int end, com.octopus.utils.ds.TableBean tb) throws Exception {
        throw new Exception("not support query method");
    }

    @Override
    public int getCount(String tradeId,String file, String[] queryFields, List<Condition> fieldValues, com.octopus.utils.ds.TableBean tb) throws Exception {
        throw new Exception("not support getCount method");
    }

    @Override
    public List<Map<String, String>> queryAsString(String tradeId,String file, String[] queryFields, List<Condition> fieldValues, Map<String, String> outs, int start, int end, com.octopus.utils.ds.TableBean tb) throws Exception {
        throw new Exception("not support queryAsString method");
    }

    @Override
    public List<Map<String, Object>> query(String tradeId,String sql, Map map, int start, int end) throws Exception {
        throw new Exception("not support query method");
    }

    @Override
    public Object addRecord(XMLParameter env, String tradeId, String taskId, String file, Map<String, Object> fieldValues) throws Exception {
        if(null != tablecontainer && null != fieldValues){
            String kr = tablecontainer.getPkField(file);
            String rowKey = (String)fieldValues.get(kr);
            Table table=null;
            try {
                table = connection.getTable(TableName.valueOf(file));
                Put put = new Put(Bytes.toBytes(rowKey));
                Iterator its = fieldValues.keySet().iterator();
                while(its.hasNext()) {
                    String c = (String)its.next();
                    String o = (String)fieldValues.get(c);
                    put.addColumn(Bytes.toBytes(c), Bytes.toBytes(c), Bytes.toBytes(o));
                }
                table.put(put);
                return true;
            }finally {
                if(null != table)
                    table.close();
            }

        }else{
            return false;
        }

    }

    @Override
    public boolean addRecords(XMLParameter env, String tradeId, String taskId, String file, List<Map<String, Object>> fieldValues) throws Exception {
        throw new Exception("not support addRecords method");
    }

    @Override
    public boolean insertRecord(XMLParameter env, String tradeId, String taskId, String file, Map<String, Object> fieldValues, int insertPosition) throws Exception {
        throw new Exception("not support insertRecord method");
    }

    @Override
    public boolean insertRecords(XMLParameter env, String tradeId, String taskId, String file, List<Map<String, Object>> fieldValues, int insertPosition) throws Exception {
        throw new Exception("not support insertRecords method");
    }

    @Override
    public boolean delete(XMLParameter env, String tradeId, String taskId, String file, List<Condition> fieldValues, com.octopus.utils.ds.TableBean tb) throws Exception {
        throw new Exception("not support delete method");
    }

    @Override
    public boolean update(XMLParameter env, String tradeId, String taskId, String file, List<Condition> fieldValues, Map<String, Object> updateData, com.octopus.utils.ds.TableBean tb) throws Exception {
        return null !=addRecord(env,tradeId,taskId,file,updateData);
    }

    @Override
    public IDataSource getDataSource(String name) throws Exception{
        throw new Exception("not support getDataSource method");
    }

    @Override
    public boolean exist(String tradeId,String tableName) throws Exception {
        try {
            Admin admin = connection.getAdmin();
            if (admin.tableExists(TableName.valueOf(tableName))) {
                return true;
            }
        }catch(Exception e){
            log.error("exist "+tableName+" in hbase error",e);
        }
        return false;
    }

    @Override
    public long getNextSequence(String name) throws Exception {
        ISequence s = (ISequence)getObjectById("sequence");
        return s.getNextSequence(name);
    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        String op=null,table=null;
        int start=0,end=0;
        Object conds=null;
        List<String> fields = null;
        Object datas=null;
        Map formate= null;
        List<String> sqls=null;
        String tradeId=null;
        List<Map> structure = null;
        if(null != input) {
            structure = (List)input.get("structure") ;
            conds = input.get("conds");
            datas = input.get("datas");
            table = (String)input.get("table");
            fields=(List)input.get("fields");
            String st = (String)input.get("start");
            if(StringUtils.isNotBlank(st))
                start=Integer.parseInt(st);
            String et = (String)input.get("end");
            if(StringUtils.isNotBlank(et))
                end=Integer.parseInt(et);
            formate= (Map)input.get("format");
            sqls = (List)input.get("sqls");

            op = (String) input.get("op");
            if("createTable".equals(op)){
                if(null != structure && StringUtils.isNotBlank(table)) {
                    List fieldNames = new ArrayList();
                    for (Map m : structure) {

                        if (m.containsKey("fieldName")){
                            fieldNames.add(m.get("fieldName"));
                        }else{
                            String fd = (String)((Map)m.get("field")).get("fieldCode");
                            if(StringUtils.isNotBlank(fd)) {
                                fieldNames.add(fd);
                            }else{
                                throw new Exception("not fieldName or fieldCode value in createtable map "+m);
                            }
                        }

                    }
                    createTable(table, (String[])fieldNames.toArray(new String[0]));
                }
            }else if("truncate".equals(op)){

            }else if("dropTable".equals(op)){
                dropTable(table);
            }else if("exist".equals(op)){
                return exist(null!=env?env.getTradeId():null,table);
            }else if("delete".equals(op)){
                List<HbaseConditionEntity> dc = getConditions((Map)conds);
                List<HbaseDataEntity> ret = queryDataByConditions(null,table,dc,fields);
                deleteDataList(env,xmlid,ret);
            }else if("add".equals(op)){
                if(datas instanceof Map) {
                    TableContainer tablecontainer = (TableContainer)input.get("^tablecontainer");
                    String id = null;
                    if(null != tablecontainer){
                        if(null != tablecontainer.getAllTables().get(table) && null !=tablecontainer.getAllTables().get(table).getPkField()) {
                            String f = tablecontainer.getAllTables().get(table).getPkField().getFieldCode();
                            if(null != f) {
                                id = (String) ((Map) datas).get(f);
                                if(StringUtils.isBlank(id)){
                                    id = String.valueOf(getNextSequence(table));
                                }
                            }
                        }
                    }
                    if(StringUtils.isBlank(id) ){
                        throw new Exception("new id is null "+datas);
                    }
                    if(!exist(table,id)) {
                        Map md = getHBaseMap((Map) datas);
                        return addDataForTable(env,xmlid,table, id, md);
                    }else{
                        throw new Exception("the pk "+id+" existed in "+table);
                    }
                }else if(datas instanceof List){
                    List<HbaseDataEntity> list = getListData((List)datas);
                    insertDataList(env,xmlid,list);
                }else{
                    throw new Exception(this.getClass().getName()+" not support "+datas.getClass().getName());
                }
            }else if("update".equals(op)){
                List<HbaseConditionEntity> dc = getConditions((Map)conds);
                List<HbaseDataEntity> ret = queryDataByConditions(null,table,dc,fields);
                updateDataList(env,xmlid,ret, (Map) datas);
            }else if("upadd".equals(op)){

            }else if("query".equals(op)){

                List<HbaseConditionEntity> dc = getConditions((Map) conds);
                List<HbaseDataEntity> ret = queryDataByConditions(null, table, dc,fields);
                return getResult(ret);

            }else if("count".equals(op)){

            }
        }
        return null;
    }

    static List<HbaseConditionEntity> getConditions(Map map){
        if(null != map && map.size()>0) {
            List ret = new ArrayList();
            Iterator its = map.keySet().iterator();
            while (its.hasNext()) {
                String k = (String) its.next();
                String v = (String) map.get(k);
                HbaseConditionEntity t = new HbaseConditionEntity();
                t.setColumn(Bytes.toBytes(k));
                t.setFamilyColumn(Bytes.toBytes(k));
                t.setValue(Bytes.toBytes(v));
                t.setCompareOp(CompareFilter.CompareOp.EQUAL);
                ret.add(t);
            }
            return ret;
        }else{
            return null;
        }
    }

    List<Map> getResult(List<HbaseDataEntity> ls){
        List list = new LinkedList();
        if(null != ls){
            for(HbaseDataEntity t:ls){
                Map h = new HashMap();
                Map m = t.getColumns();
                Iterator its = m.keySet().iterator();
                while(its.hasNext()){
                    Object o = m.get(its.next());
                    if(o instanceof Map){
                        h.putAll((Map)o);
                    }
                }
                list.add(h);

            }
        }
        return list;
    }

    Map getHBaseMap(Map map){
        Iterator its = map.keySet().iterator();
        HashMap m = new HashMap();
        while(its.hasNext()){
            String key = (String)its.next();
            Object v = map.get(key);
            if(null != v) {
                if (v instanceof Map) {
                    v = ObjectUtils.convertMap2String((Map) v);
                }else {
                    v = v.toString();
                }
            }
            Map t = new HashMap();
            t.put(key,v);
            m.put(key,t);
        }
        return m;
    }

    List<HbaseDataEntity> getListData(List list)throws Exception{
        throw new Exception("not support now");
    }

    @Override
    public ResultCheck checkReturn(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return new ResultCheck(true,ret);
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        String tradid = env.getTradeId();
        String id = tradid+"->"+xmlid;
        if(StringUtils.isNotBlank(id)) {
            tradeDataList.remove(id);
        }
        return true;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret, Exception e) throws Exception {
        if(null != env){
            String tradid = env.getTradeId();
            String id = tradid+"->"+xmlid;
            if(StringUtils.isNotBlank(id)){
                List<Map> thisTradeDatalist = tradeDataList.get(id);
                if(null != thisTradeDatalist){
                    for(Map d:thisTradeDatalist){
                        String op  = (String)d.get("op");
                        Map data  = (Map)d.get("data");
                        String table   = (String)d.get("table");
                        if(StringUtils.isNotBlank(op)){
                            if("delete".equals(op)){
                                addRecord(env,null,null,table,data);
                            }else if("update".equals(op)){
                                update(env,null,null,table,null,data,null);
                            }else if("add".equals(op)){
                                String pk = tablecontainer.getPkField(table);
                                String rowKey = (String)data.get(pk);
                                delete(table,rowKey);
                            }
                        }
                    }
                }
                tradeDataList.remove(id);
            }
            return true;
        }
        return false;
    }

    /**
     * 创建表
     * @param tableName 表名
     * @param familyNames 列族名
     * */
    public void createTable(String tableName, String... familyNames) throws IOException {
        Admin admin = connection.getAdmin();
        if (admin.tableExists(TableName.valueOf(tableName))) {
            log.error("table "+tableName +" has existed");
            return;
        }
        //通过HTableDescriptor类来描述一个表，HColumnDescriptor描述一个列族
        HTableDescriptor tableDescriptor = new HTableDescriptor(TableName.valueOf(tableName));
        for (String familyName : familyNames) {
            tableDescriptor.addFamily(new HColumnDescriptor(familyName));
        }
        admin.createTable(tableDescriptor);
    }
    /**
     * 删除表
     * @param tableName 表名
     * */
    public void dropTable(String tableName) throws IOException {
        Admin admin = connection.getAdmin();
        //删除之前要将表disable
        if (!admin.isTableDisabled(TableName.valueOf(tableName))) {
            admin.disableTable(TableName.valueOf(tableName));
        }
        admin.deleteTable(TableName.valueOf(tableName));

    }

    /**
     * 指定行/列中插入数据
     * @param tableName 表名
     * @param rowKey 主键rowkey
     * @param family 列族
     * @param column 列
     * @param value 值
     * TODO: 批量PUT
     */
    public void insert(String tableName, String rowKey, String family, String column, String value) throws IOException {
        Table table=null;
        try {
            table = connection.getTable(TableName.valueOf(tableName));
            Put put = new Put(Bytes.toBytes(rowKey));
            put.addColumn(Bytes.toBytes(family), Bytes.toBytes(column), Bytes.toBytes(value));
            table.put(put);
        }finally {
            if(null != table)
                table.close();
        }
    }

    /**
     * 往表里面添加数据
     *
     * @param
     * @param rowkey
     * @param columnValues
     * @return
     */
    public int addDataForTable(XMLParameter env,String taskid,String name, String rowkey,
                                      Map<String, Map<String, String>> columnValues) throws Exception{
        Table htable=null;
        try {
            Put put = new Put(Bytes.toBytes(rowkey));
            TableName tableName = TableName.valueOf(name);
            htable = connection.getTable(tableName);
            HashMap tm = new HashMap();
            HColumnDescriptor[] columnFamilies = htable.getTableDescriptor().getColumnFamilies();// 获取所有的列名
            for (HColumnDescriptor hColumnDescriptor : columnFamilies) {
                String familyName = hColumnDescriptor.getNameAsString();
                Map<String, String> columnNameValueMap = columnValues.get(familyName);
                if (columnNameValueMap != null) {
                    for (String columnName : columnNameValueMap.keySet()) {
                        put.addColumn(Bytes.toBytes(familyName), Bytes
                                .toBytes(columnName), Bytes
                                .toBytes(columnNameValueMap.get(columnName)));
                        tm.put(columnName,columnNameValueMap.get(columnName));
                    }
                }
            }
            if(StringUtils.isNotBlank(env.getTradeId())){
                List l = new ArrayList();
                HashMap m = new HashMap();
                m.put("op","add");
                m.put("data",tm);
                m.put("table",name);
                l.add(m);
                if(null == tradeDataList.get(env.getTradeId()+"->"+taskid)) {
                    tradeDataList.put(env.getTradeId() + "->" + taskid, l);
                }else{
                    ((List)tradeDataList.get(env.getTradeId()+"->"+taskid)).addAll(l);
                }

            }
            htable.put(put);
            return put.size();
        } catch (IOException e) {
            log.error("",e);
            throw e;
        }finally {
            if(null != htable){
                htable.close();
            }
        }
    }

    /**
     * 批量添加数据
     *
     * @param list
     */
    public void insertDataList(XMLParameter env,String taskid,List<HbaseDataEntity> list) throws Exception{
        List<Put> puts = new ArrayList<Put>();
        Table table = null;
        Put put;
        try {
            List li = new ArrayList();
            for (HbaseDataEntity entity : list) {
                TableName tableName = TableName.valueOf(entity.getTableName());
                table = connection.getTable(tableName);
                HashMap t = new HashMap();
                put = new Put(entity.getMobileKey().getBytes());// 一个PUT代表一行数据，再NEW一个PUT表示第二行数据,每行一个唯一的ROWKEY
                for (String columnfamily : entity.getColumns().keySet()) {
                    for (String column : entity.getColumns().get(columnfamily)
                            .keySet()) {
                        put.addColumn(
                                columnfamily.getBytes(),
                                column.getBytes(),
                                entity.getColumns().get(columnfamily)
                                        .get(column).getBytes());
                        t.put(column,entity.getColumns().get(columnfamily).get(column));
                    }
                }
                HashMap m = new HashMap();
                m.put("data",t);
                m.put("op","add");
                m.put("table",tableName);
                li.add(m);
                puts.add(put);
            }
            if(StringUtils.isNotBlank(env.getTradeId())){
                if(null == tradeDataList.get(env.getTradeId()+"->"+taskid)) {
                    tradeDataList.put(env.getTradeId() + "->" + taskid, li);
                }else{
                    ((List)tradeDataList.get(env.getTradeId()+"->"+taskid)).addAll(li);
                }
            }
            table.put(puts);
        } catch (Exception e) {
            log.error("",e);
            throw e;
        } finally {
            if(null != table)
                table.close();
        }
    }

    /**
     * 更新表中的一列
     *
     * @param name
     * @param rowKey
     * @param familyName
     * @param columnName
     * @param value
     */
    public void updateTable(String name, String rowKey,
                                   String familyName, String columnName, String value) throws Exception{
        Table table=null;
        try {
            TableName tableName = TableName.valueOf(name);
            table = connection.getTable(tableName);

            Put put = new Put(Bytes.toBytes(rowKey));
            put.addColumn(Bytes.toBytes(familyName), Bytes.toBytes(columnName),
                    Bytes.toBytes(value));

            table.put(put);
            table.close();

        } catch (IOException e) {
            log.error("",e);
            throw e;
        }finally {
            if(null != table)
                table.close();
        }
    }

    public void updateDataList(XMLParameter env,String taskId,List<HbaseDataEntity> list,Map data)throws Exception{
        Table table = null;
        List<Put> puts = new ArrayList<Put>();
        try {
            List ls = new ArrayList();
            for (HbaseDataEntity entity : list) {

                TableName tableName = TableName.valueOf(entity.getTableName());
                table = connection.getTable(tableName);

                Put put = new Put(Bytes.toBytes(entity.getMobileKey()));
                Map d = new HashMap();
                for (String columnfamily : entity.getColumns().keySet()) {
                    for (String column : entity.getColumns().get(columnfamily).keySet()) {
                        if(data.containsKey(column)) {
                            put.addColumn(columnfamily.getBytes(), column.getBytes(), Bytes.toBytes((String) data.get(column)));
                            log.debug("update set value="+data.get(column)+" where columnfamily="+columnfamily+" and column="+column);
                            d.put(column,data.get(column));
                        }
                    }
                }
                if(put.size()>0) {
                    HashMap m = new HashMap();
                    m.put("data",d);
                    m.put("op","update");
                    m.put("table",entity.getTableName());
                    ls.add(m);
                    puts.add(put);
                }
            }

            if(puts.size()>0) {
                if(StringUtils.isNotBlank(env.getTradeId())){
                    if(null == tradeDataList.get(env.getTradeId()+"->"+taskId)) {
                        tradeDataList.put(env.getTradeId() + "->" + taskId, ls);
                    }else{
                        ((List)tradeDataList.get(env.getTradeId()+"->"+taskId)).addAll(ls);
                    }
                }
                table.put(puts);

            }
        } catch (Exception e) {
            log.error("",e);
            throw e;
        } finally {
            if(null != table)
            table.close();
        }
    }

    /**
     * 批量删除数据
     *
     * @param list
     */
    public void deleteDataList(XMLParameter env,String taskid,List<HbaseDataEntity> list)throws Exception{
        Table table = null;
        List<Delete> deletes = new ArrayList<Delete>();
        List predata =null;
        try {
            for (HbaseDataEntity entity : list) {

                TableName tableName = TableName.valueOf(entity.getTableName());
                table = connection.getTable(tableName);

                Delete delete = new Delete(Bytes.toBytes(entity.getMobileKey()));
                /*for (String columnfamily : entity.getColumns().keySet()) {
                    for (String column : entity.getColumns().get(columnfamily).keySet()) {
                        delete.addColumn(columnfamily.getBytes(),column.getBytes());
                    }
                }*/
                deletes.add(delete);
                if(StringUtils.isNotBlank(env.getTradeId())){
                    Map m = getDataByMobileKey(table,entity.getMobileKey());
                    if(null == predata) predata=new ArrayList();
                    HashMap d = new HashMap();
                    d.put("data",d);
                    d.put("table",entity.getTableName());
                    d.put("op","delete");
                    predata.add(d);

                }

            }
            if(StringUtils.isNotBlank(env.getTradeId())) {
                if(null == tradeDataList.get(env.getTradeId() + "->" + taskid)) {
                    tradeDataList.put(env.getTradeId() + "->" + taskid, predata);
                }else{
                    ((List)tradeDataList.get(env.getTradeId() + "->" + taskid)).addAll(predata);
                }
            }
            if(null != deletes && deletes.size()>0) {
                table.delete(deletes);

            }


        } catch (Exception e) {
            log.error("",e);
            throw e;
        } finally {
            if(null != table){
                table.close();
            }
        }
    }
    Map getDataByMobileKey(Table tb ,String key) throws IOException {
        Filter fi = new RowFilter(CompareFilter.CompareOp.EQUAL,new BinaryComparator(key.getBytes()));
        Scan s = new Scan();
        s.setFilter(fi);
        Map<String,String> familyMap = new HashMap();
        ResultScanner resultScanner = tb.getScanner(s);
        for (Result result : resultScanner) {
            for (Cell cell : result.rawCells()) {
                familyMap.put(new String(cell.getQualifierArray(), cell.getQualifierOffset(), cell.getQualifierLength()),new String(cell.getValueArray(), cell.getValueOffset(), cell.getValueLength()));
            }
            if(familyMap.size()>0)
            return familyMap;

        }
        return null;
    }
    /**
     * 删除指定的列
     *
     * @param name
     * @param rowKey
     * @param familyName
     * @param columnName
     */
    public void deleteColumn(String name, String rowKey,
                                    String familyName, String columnName) throws Exception{
        Table table=null;
        try {

            TableName tableName = TableName.valueOf(name);
            table = connection.getTable(tableName);

            Delete delete = new Delete(Bytes.toBytes(rowKey));
            delete.addColumn(Bytes.toBytes(familyName),
                    Bytes.toBytes(columnName));

            table.delete(delete);

            table.close();
        } catch (IOException e) {
            log.error("",e);
            throw e;
        }finally {
            if(null != table){
                table.close();
            }
        }
    }

    /**
     * 删除所有列
     *
     * @param name
     * @param rowKey
     */
    public void deleteAllColumns(String name, String rowKey) throws Exception{
        Table table=null;
        try {

            TableName tableName = TableName.valueOf(name);
            table = connection.getTable(tableName);

            Delete delete = new Delete(Bytes.toBytes(rowKey));

            table.delete(delete);


        } catch (IOException e) {
            log.error("",e);
            throw e;
        }finally {
            if(null!=table){
                table.close();
            }
        }
    }

    /**
     * 删除表中的指定行
     * @param tableName 表名
     * @param rowKey rowkey
     * TODO: 批量删除
     */
    public void delete(String tableName, String rowKey) throws IOException {
        Table table =null;
        try {
            table = connection.getTable(TableName.valueOf(tableName));
            Delete delete = new Delete(Bytes.toBytes(rowKey));
            table.delete(delete);
        }finally {
            if(null != table){
                table.close();
            }
        }
    }

    public boolean exist(String tradeId,String tableName, String rowKey) throws IOException {
        Table table =null;
        try {
            table = connection.getTable(TableName.valueOf(tableName));
            Get g = new Get(Bytes.toBytes(rowKey));
           return table.exists(g);
        }finally {
            if(null != table){
                table.close();
            }
        }
    }


    /**
     * 获取所有的数据
     * @param name
     * @param size
     * @return
     */
    public List<HbaseDataEntity> getResultScans(String name, int size) throws Exception{

        Scan scan = new Scan();
        ResultScanner resultScanner = null;
        Table table=null;
        List<HbaseDataEntity> list = new ArrayList<HbaseDataEntity>();
        try {
            TableName tableName = TableName.valueOf(name);
            table = connection.getTable(tableName);

            long beiginTime = System.currentTimeMillis();
            resultScanner = table.getScanner(scan);
            long endTime = System.currentTimeMillis();
            double spentTime = (endTime - beiginTime) / 1000.0;
            //System.out.println("cost:===" + spentTime + "s");

            for (Result result : resultScanner) {
                // System.out.println("获得到rowkey:" + new
                // String(result.getRow()));
                HbaseDataEntity entity = new HbaseDataEntity();

                entity.setTableName(name);
                entity.setMobileKey(new String(result.getRow()));

                Map<String, Map<String, String>> familyMap = new HashMap<String, Map<String, String>>();
                for (Cell cell : result.rawCells()) {

                    if (familyMap.get(new String(cell.getFamilyArray())) == null) {
                        Map<String, String> columnsMap = new HashMap<String, String>();
                        columnsMap.put(
                                new String(cell.getQualifierArray(), cell.getQualifierOffset(), cell.getQualifierLength()),
                                new String(cell.getValueArray(), cell.getValueOffset(), cell.getValueLength()));
                        familyMap.put(new String(cell.getFamilyArray(), cell.getFamilyOffset(), cell.getFamilyLength()), columnsMap);
                    } else {
                        familyMap.get(
                                new String(cell.getFamilyArray(), cell.getFamilyOffset(), cell.getFamilyLength())).put(
                                new String(cell.getQualifierArray(),cell.getQualifierOffset(),cell.getQualifierLength()),
                                new String(cell.getValueArray(), cell.getValueOffset(), cell.getValueLength()));
                    }

                    // System.out.println("列：" + new
                    // String(cell.getFamilyArray(), cell.getFamilyOffset(),
                    // cell.getFamilyLength())
                    // + "====值:" + new
                    // String(cell.getValueArray(),cell.getValueOffset(),cell.getValueLength()));
                }

                entity.setColumns(familyMap);
                list.add(entity);

                if (size == list.size()) {
                    break;
                }
            }


            return list;
        } catch (IOException e) {
            log.error("",e);
            throw e;
        } finally {
            if(null != resultScanner)
            resultScanner.close();
            if(null != table)
                table.close();

        }

    }

    /**
     * 组合条件查询 and
     *
     * @param nameSpace
     *            命名空间
     * @param name
     *            表名
     * @param parameters
     *            格式是：columnFamily,columnName,columnValue
     */
    public List<HbaseDataEntity> queryDataByConditionsAnd(String nameSpace, String name, List<String> parameters) throws Exception{
        ResultScanner rs = null;
        Table table = null;
        List<HbaseDataEntity> list = new ArrayList<HbaseDataEntity>();
        try {
            TableName tableName = TableName.valueOf(name);
            table = connection.getTable(tableName);
            // 参数的格式：columnFamily,columnName,columnValue
            List<Filter> filters = new ArrayList<Filter>();
            for (String parameter : parameters) {
                String[] columns = parameter.split(",");
                SingleColumnValueFilter filter = new SingleColumnValueFilter(
                        Bytes.toBytes(columns[0]), Bytes.toBytes(columns[1]),
                        CompareFilter.CompareOp.valueOf(columns[2]),
                        Bytes.toBytes(columns[3]));
                filter.setFilterIfMissing(true);
                filters.add(filter);
            }

            FilterList filterList = new FilterList(filters);

            Scan scan = new Scan();
            scan.setFilter(filterList);
            rs = table.getScanner(scan);
            for (Result r : rs) {
                //System.out.println("获得到rowkey:" + new String(r.getRow()));
                HbaseDataEntity entity = new HbaseDataEntity();
                entity.setNameSpace(nameSpace);
                entity.setTableName(name);
                entity.setMobileKey(new String(r.getRow()));
                Map<String, Map<String, String>> familyMap = new HashMap<String, Map<String, String>>();
                for (Cell cell : r.rawCells()) {
                    if (familyMap.get(new String(cell.getFamilyArray(), cell.getFamilyOffset(), cell.getFamilyLength())) == null) {
                        Map<String, String> columnsMap = new HashMap<String, String>();
                        columnsMap.put(
                                new String(cell.getQualifierArray(), cell
                                        .getQualifierOffset(), cell
                                        .getQualifierLength()),
                                new String(cell.getValueArray(), cell
                                        .getValueOffset(), cell
                                        .getValueLength()));
                        familyMap.put(
                                new String(cell.getFamilyArray(), cell
                                        .getFamilyOffset(), cell
                                        .getFamilyLength()), columnsMap);
                    } else {
                        familyMap.get(
                                new String(cell.getFamilyArray(), cell
                                        .getFamilyOffset(), cell
                                        .getFamilyLength())).put(
                                new String(cell.getQualifierArray(),
                                        cell.getQualifierOffset(),
                                        cell.getQualifierLength()),
                                new String(cell.getValueArray(), cell
                                        .getValueOffset(), cell
                                        .getValueLength()));
                    }
                }

                entity.setColumns(familyMap);
                list.add(entity);
            }


            return list;
        } catch (Exception e) {
            log.error("",e);
            throw e;
        }finally {
            if(null != rs)
                rs.close();
            if(null != table)
                table.close();
        }

    }

    /**
     * 组合条件查询 or
     *
     * @param nameSpace
     *            命名空间
     * @param name
     *            表名
     * @param parameters
     *            格式是：columnFamily,columnName,columnValue
     * @return
     */
    public List<HbaseDataEntity> queryDataByConditionsOr(String nameSpace, String name, List<String> parameters) throws Exception{
        ResultScanner rs = null;
        Table table=null;
        List<HbaseDataEntity> list = new ArrayList<HbaseDataEntity>();
        try {
            TableName tableName = TableName.valueOf(name);
            table = connection.getTable(tableName);
            // 参数的额格式：columnFamily,columnName,columnValue
            List<Filter> filters = new ArrayList<Filter>();
            Scan scan = new Scan();
            byte[] columnFamily = null;
            byte[] columnName = null;

            for (String parameter : parameters) {
                String[] columns = parameter.split(",");
                columnFamily = Bytes.toBytes(columns[0]);
                columnName = Bytes.toBytes(columns[1]);
                SingleColumnValueFilter filter = new SingleColumnValueFilter(
                        Bytes.toBytes(columns[0]), Bytes.toBytes(columns[1]),
                        CompareFilter.CompareOp.valueOf(columns[2]),
                        Bytes.toBytes(columns[3]));
                filter.setFilterIfMissing(true);
                filters.add(filter);
            }

            FilterList filterList = new FilterList(FilterList.Operator.MUST_PASS_ONE, filters);
            scan.setFilter(filterList);
            rs = table.getScanner(scan);
            for (Result r : rs) {
                if (r.containsColumn(columnFamily, columnName)) {
                    //System.out.println("获得到rowkey:" + new String(r.getRow()));
                    HbaseDataEntity entity = new HbaseDataEntity();
                    entity.setNameSpace(nameSpace);
                    entity.setTableName(name);
                    entity.setMobileKey(new String(r.getRow()));
                    Map<String, Map<String, String>> familyMap = new HashMap<String, Map<String, String>>();
                    for (Cell cell : r.rawCells()) {
                        if (familyMap.get(new String(cell.getFamilyArray(), cell.getFamilyOffset(), cell.getFamilyLength())) == null) {
                            Map<String, String> columnsMap = new HashMap<String, String>();
                            columnsMap.put(
                                    new String(cell.getQualifierArray(), cell
                                            .getQualifierOffset(), cell
                                            .getQualifierLength()),
                                    new String(cell.getValueArray(), cell
                                            .getValueOffset(), cell
                                            .getValueLength()));
                            familyMap.put(
                                    new String(cell.getFamilyArray(), cell
                                            .getFamilyOffset(), cell
                                            .getFamilyLength()), columnsMap);
                        } else {
                            familyMap.get(
                                    new String(cell.getFamilyArray(), cell
                                            .getFamilyOffset(), cell
                                            .getFamilyLength())).put(
                                    new String(cell.getQualifierArray(),
                                            cell.getQualifierOffset(),
                                            cell.getQualifierLength()),
                                    new String(cell.getValueArray(), cell
                                            .getValueOffset(), cell
                                            .getValueLength()));
                        }
                    }

                    entity.setColumns(familyMap);
                    list.add(entity);
                }
            }

            return list;
        } catch (Exception e) {
            log.error("",e);
            throw e;
        }finally {
            if(null != rs)
                rs.close();
            if(null != table)
                table.close();
        }
    }

    /**
     * 组合条件查询 or
     *
     * @param nameSpace
     *            命名空间
     * @param name
     *            表名
     * @param hbaseConditions
     *            格式是：columnFamily,columnName,columnValue
     * @return
     */
    public List<HbaseDataEntity> queryDataByConditions(String nameSpace,String name, List<HbaseConditionEntity> hbaseConditions,List<String> fields) throws Exception{

        ResultScanner rs = null;
        Table table=null;
        List<HbaseDataEntity> list = new ArrayList<HbaseDataEntity>();
        try {
            TableName tableName = TableName.valueOf(name);
            table = connection.getTable(tableName);
            // 参数的额格式：columnFamily,columnName,columnValue
            // List<Filter> filters = new ArrayList<Filter>();
            Scan scan = new Scan();

            FilterList filterList = null;
            FilterList.Operator operator = null;
            if(null != hbaseConditions) {
                for (HbaseConditionEntity hbaseCondition : hbaseConditions) {

                    SingleColumnValueFilter filter = new SingleColumnValueFilter(
                            hbaseCondition.getFamilyColumn(),
                            hbaseCondition.getColumn(),
                            hbaseCondition.getCompareOp(),
                            hbaseCondition.getValue());
                    filter.setFilterIfMissing(true);

                    if (hbaseCondition.getOperator() != null) {
                        if (operator == null) {
                            operator = hbaseCondition.getOperator();
                            filterList = new FilterList(hbaseCondition.getOperator());
                            filterList.addFilter(filter);
                        } else if (operator.equals(hbaseCondition.getOperator())) {
                            filterList.addFilter(filter);
                        } else {
                            filterList.addFilter(filter);
                            FilterList addFilterList = new FilterList(hbaseCondition.getOperator());
                            addFilterList.addFilter(filterList);
                            filterList = addFilterList;
                        }

                    } else {
                        if (filterList == null) {
                            filterList = new FilterList(FilterList.Operator.MUST_PASS_ALL);// 默认只有一个条件的时候
                        }
                        filterList.addFilter(filter);
                    }

                }
            }
            if(null != filterList) {
                scan.setFilter(filterList);
            }
            rs = table.getScanner(scan);
            list = convertEntity(nameSpace,name,rs,fields);

            return list;
        } catch (Exception e) {
            log.error(e);
            throw e;
        }finally {
            if(null != rs){
                rs.close();
            }
            if(null != table)
                table.close();
        }
    }

    List<HbaseDataEntity> convertEntity(String nameSpace,String table,ResultScanner rs,List<String> fields){
        List<HbaseDataEntity> ret=new LinkedList<HbaseDataEntity>();
        for (Result r : rs) {
            //System.out.println("获得到rowkey:" + new String(r.getRow()));
            HbaseDataEntity entity = new HbaseDataEntity();
            entity.setNameSpace(nameSpace);
            entity.setTableName(table);
            entity.setMobileKey(new String(r.getRow()));
            Map<String, Map<String, String>> familyMap = new HashMap<String, Map<String, String>>();
            for (Cell cell : r.rawCells()) {
                String name = new String(cell.getQualifierArray(), cell.getQualifierOffset(), cell.getQualifierLength());
                if(null == fields || fields.contains(name)) {
                    if (familyMap.get(new String(cell.getFamilyArray(), cell.getFamilyOffset(), cell.getFamilyLength())) == null) {
                        Map<String, String> columnsMap = new HashMap<String, String>();
                        columnsMap.put(
                                name,
                                new String(cell.getValueArray(), cell
                                        .getValueOffset(), cell
                                        .getValueLength()));
                        familyMap.put(
                                new String(cell.getFamilyArray(), cell
                                        .getFamilyOffset(), cell
                                        .getFamilyLength()), columnsMap);
                    } else {
                        familyMap.get(
                                new String(cell.getFamilyArray(), cell
                                        .getFamilyOffset(), cell
                                        .getFamilyLength())).put(
                                name,
                                new String(cell.getValueArray(), cell
                                        .getValueOffset(), cell
                                        .getValueLength()));
                    }
                }
            }

            entity.setColumns(familyMap);
            ret.add(entity);
        }
        return ret;
    }

    /**
     * 分页的复合条件查询
     * @param nameSpace
     *        命名空间
     * @param name
     *        表名
     * @param hbaseConditions
     *        复合条件
     * @param pageSize
     *        每页显示的数量
     * @param lastRow
     *        当前页的最后一行
     * @return
     */
    public List<HbaseDataEntity> queryDataByConditionsAndPage(
            String nameSpace, String name,
            List<HbaseConditionEntity> hbaseConditions, int pageSize,
            byte[] lastRow) throws Exception{
        final byte[] POSTFIX = new byte[] { 0x00 };

        ResultScanner rs = null;
        List<HbaseDataEntity> list = new ArrayList<HbaseDataEntity>();
        Table table=null;
        try {

            TableName tableName = TableName.valueOf(name);
            table = connection.getTable(tableName);

            Scan scan = new Scan();

            FilterList filterList = null;
            FilterList.Operator operator = null;
            for (HbaseConditionEntity hbaseCondition : hbaseConditions) {

                SingleColumnValueFilter filter = new SingleColumnValueFilter(
                        hbaseCondition.getFamilyColumn(),
                        hbaseCondition.getColumn(),
                        hbaseCondition.getCompareOp(),
                        hbaseCondition.getValue());
                filter.setFilterIfMissing(true);

                if (hbaseCondition.getOperator() != null) {

                    if (operator == null) {
                        operator = hbaseCondition.getOperator();
                        filterList = new FilterList(
                                hbaseCondition.getOperator());
                        filterList.addFilter(filter);
                        //System.out.println("filterList==1" + filterList);
                    } else if (operator.equals(hbaseCondition.getOperator())) {
                        filterList.addFilter(filter);
                    } else {
                        filterList.addFilter(filter);
                        //System.out.println("filterList==2" + filterList);
                        FilterList addFilterList = new FilterList(
                                hbaseCondition.getOperator());
                        addFilterList.addFilter(filterList);
                        //System.out.println("addFilterList==1" + addFilterList);
                        filterList = addFilterList;
                        //System.out.println("filterList==3" + filterList);
                    }

                } else {
                    if (filterList == null) {
                        filterList = new FilterList(FilterList.Operator.MUST_PASS_ALL);// 默认只有一个条件的时候
                    }
                    filterList.addFilter(filter);
                }

            }

            FilterList pageFilterList = new FilterList(FilterList.Operator.MUST_PASS_ALL);// 默认只有一个条件的时候
            Filter pageFilter = new PageFilter(pageSize);
            pageFilterList.addFilter(pageFilter);
            pageFilterList.addFilter(filterList);
            if (lastRow != null) {
                // 注意这里添加了POSTFIX操作，不然死循环了
                byte[] startRow = Bytes.add(lastRow, POSTFIX);
                scan.setStartRow(startRow);
            }

            //System.out.println(pageFilterList + ":pageFilterList");
            scan.setFilter(pageFilterList);

            rs = table.getScanner(scan);
            for (Result r : rs) {
                //System.out.println("获得到rowkey:" + new String(r.getRow()));
                HbaseDataEntity entity = new HbaseDataEntity();
                entity.setNameSpace(nameSpace);
                entity.setTableName(name);
                entity.setMobileKey(new String(r.getRow()));
                Map<String, Map<String, String>> familyMap = new HashMap<String, Map<String, String>>();
                for (Cell cell : r.rawCells()) {
                    if (familyMap.get(new String(cell.getFamilyArray(), cell
                            .getFamilyOffset(), cell.getFamilyLength())) == null) {
                        Map<String, String> columnsMap = new HashMap<String, String>();
                        columnsMap.put(
                                new String(cell.getQualifierArray(), cell
                                        .getQualifierOffset(), cell
                                        .getQualifierLength()),
                                new String(cell.getValueArray(), cell
                                        .getValueOffset(), cell
                                        .getValueLength()));
                        familyMap.put(
                                new String(cell.getFamilyArray(), cell
                                        .getFamilyOffset(), cell
                                        .getFamilyLength()), columnsMap);
                    } else {
                        familyMap.get(
                                new String(cell.getFamilyArray(), cell
                                        .getFamilyOffset(), cell
                                        .getFamilyLength())).put(
                                new String(cell.getQualifierArray(),
                                        cell.getQualifierOffset(),
                                        cell.getQualifierLength()),
                                new String(cell.getValueArray(), cell
                                        .getValueOffset(), cell
                                        .getValueLength()));
                    }
                }

                entity.setColumns(familyMap);
                list.add(entity);
            }
            return list;
        }catch (Exception e) {
            log.error("",e);
            throw e;
        }finally {
            if(null != rs){
                rs.close();
            }
            if(null != table){
                table.close();
            }
        }
    }

    /**
     * 复合条件分页查询
     * @param name
     * @param pageSize
     * @param lastRow
     * @return
     */
    public List<HbaseDataEntity> getHbaseDatasByPage(String name,
                                                            int pageSize, byte[] lastRow) throws Exception{
        final byte[] POSTFIX = new byte[] { 0x00 };

        Scan scan = new Scan();
        ResultScanner resultScanner = null;
        Table table = null;
        List<HbaseDataEntity> list = new ArrayList<HbaseDataEntity>();

        try {

            TableName tableName = TableName.valueOf(name);
            table = connection.getTable(tableName);

            Filter filter = new PageFilter(pageSize);
            scan.setFilter(filter);
            if (lastRow != null) {
                // 注意这里添加了POSTFIX操作，不然死循环了
                byte[] startRow = Bytes.add(lastRow, POSTFIX);
                scan.setStartRow(startRow);
            }
            resultScanner = table.getScanner(scan);

            for (Result result : resultScanner) {
                HbaseDataEntity entity = new HbaseDataEntity();
                entity.setTableName(name);
                entity.setMobileKey(new String(result.getRow()));
                Map<String, Map<String, String>> familyMap = new HashMap<String, Map<String, String>>();
                for (Cell cell : result.rawCells()) {
                    if (familyMap.get(new String(cell.getFamilyArray(), cell
                            .getFamilyOffset(), cell.getFamilyLength())) == null) {
                        Map<String, String> columnsMap = new HashMap<String, String>();
                        columnsMap.put(
                                new String(cell.getQualifierArray(), cell
                                        .getQualifierOffset(), cell
                                        .getQualifierLength()),
                                new String(cell.getValueArray(), cell
                                        .getValueOffset(), cell
                                        .getValueLength()));
                        familyMap.put(
                                new String(cell.getFamilyArray(), cell
                                        .getFamilyOffset(), cell
                                        .getFamilyLength()), columnsMap);
                    } else {
                        familyMap.get(
                                new String(cell.getFamilyArray(), cell
                                        .getFamilyOffset(), cell
                                        .getFamilyLength())).put(
                                new String(cell.getQualifierArray(),
                                        cell.getQualifierOffset(),
                                        cell.getQualifierLength()),
                                new String(cell.getValueArray(), cell
                                        .getValueOffset(), cell
                                        .getValueLength()));
                    }
                }
                entity.setColumns(familyMap);
                list.add(entity);
            }

            return list;
        } catch (IOException e) {
            log.error("",e);
            throw e;
        } finally {
            resultScanner.close();
            if(null != table)
                table.close();
        }

    }

    public int getDataByPage(String name, int pageSize) throws Exception{

        final byte[] POSTFIX = new byte[] { 0x00 };
        TableName tableName = TableName.valueOf(name);
        Table table=null;
        int totalRows = 0;
        try {
            table = connection.getTable(tableName);
            Filter filter = new PageFilter(pageSize);
            byte[] lastRow = null;

            while (true) {
                Scan scan = new Scan();
                scan.setFilter(filter);
                if (lastRow != null) {
                    // 注意这里添加了POSTFIX操作，不然死循环了
                    byte[] startRow = Bytes.add(lastRow, POSTFIX);
                    scan.setStartRow(startRow);
                }

                ResultScanner scanner = table.getScanner(scan);
                int localRows = 0;
                Result result;
                while ((result = scanner.next()) != null) {
                    //System.out.println(localRows++ + ":" + result);
                    totalRows++;
                    lastRow = result.getRow();
                }

                scanner.close();
                if (localRows == 0)
                    break;
            }
        } catch (IOException e) {
           log.error("",e);
            throw e;
        }finally {
            if(null !=table)
                table.close();
        }
        return totalRows;
    }

    public static void main(String[] args) {
        try {
            // 1、Create table

            // /String tableName = "caoShuaiTest09";

        /*
         * List<String> columnFamilyName = new ArrayList<String>();
         * columnFamilyName.add("info"); columnFamilyName.add("address");
         * columnFamilyName.add("score");
         *
         * createTable(tableName, columnFamilyName);
         */

            // 2、Insert data into table

        /*
         * String roeKey01 = "LiMing"; Map<String, Map<String, String>>
         * familyColumnMap01 = new HashMap<String, Map<String, String>>();
         * Map<String, String> columnMap01 = new HashMap<String, String>();
         * columnMap01.put("age", "23"); columnMap01.put("phone",
         * "13854285991"); familyColumnMap01.put("info", columnMap01);
         *
         * Map<String, String> columnMap02 = new HashMap<String, String>();
         * columnMap02.put("province", "shandong"); columnMap02.put("city",
         * "beijing"); familyColumnMap01.put("address", columnMap02);
         *
         * Map<String, String> columnMap03 = new HashMap<String, String>();
         * columnMap03.put("english", "80"); columnMap03.put("chinese", "100");
         * familyColumnMap01.put("score", columnMap03); int result01 =
         * addDataForTable(tableName, roeKey01, familyColumnMap01);
         * System.out.println("==result01==:" + result01);
         */

            // 3、获取结果 getResult(tableName, roeKey01);

        /*
         * String roeKey02 = "WangNing"; Map<String, Map<String, String>>
         * familyColumnMap01 = new HashMap<String, Map<String,String>>();
         * Map<String, String> columnMap01 = new HashMap<String,String>();
         * columnMap01.put("age", "50"); columnMap01.put("phone",
         * "13854285991"); familyColumnMap01.put("info", columnMap01);
         *
         * Map<String, String> columnMap02 = new HashMap<String,String>();
         * columnMap02.put("province", "shandong");
         * columnMap02.put("city","beijing"); familyColumnMap01.put("address",
         * columnMap02);
         *
         * Map<String, String> columnMap03 = new HashMap<String,String>();
         * columnMap03.put("english", "40"); columnMap03.put("chinese","70");
         * familyColumnMap01.put("score", columnMap03); int result01 =
         * addDataForTable(tableName, roeKey02, familyColumnMap01);
         * System.out.println("==result01==:" + result01);
         */
            // 4
            // getResultScan(tableName);

        /*
         * List<String> parameters = new ArrayList<String>();
         * parameters.add("info,age,EQUAL,23");
         * parameters.add("score,english,GREATER_OR_EQUAL,40");
         * QueryDataByConditionsAnd(null, tableName, parameters);
         */

            // 5

        /*
         * String newTableName = "caoShuaiTest04";
         *
         * List<HbaseDataEntity> hbaseDatas = getResultScans(newTableName);
         *
         * System.out.println("hbaseDatas===" + hbaseDatas); for
         * (HbaseDataEntity hbaseData : hbaseDatas) {
         *
         * String rowKey = hbaseData.getMobileKey();
         *
         * Map<String, Map<String, String>> maps = hbaseData.getColumns();
         *
         * for (String key : maps.keySet()) {
         *
         * System.out.println("key===" + key); Map<String, String> columnsMap =
         * maps.get(key);
         *
         * for (String columnsKey : columnsMap.keySet()) {
         * System.out.println("columnsKey===" + columnsKey);
         *
         * //updateTable("caoShuaiTest01", rowKey, key, columnsKey,columnsKey);
         * //deleteColumn("caoShuaiTest04", rowKey, key, columnsKey); }
         *
         * } }
         */

        /*
         * long beginTime = System.currentTimeMillis();
         * System.out.println("begin:" + beginTime);
         *
         * for (int i = 0; i < 100000000; i++) {
         *
         * long startTime = System.currentTimeMillis();
         *
         * String tableName = "caoShuaiTest06";
         *
         * String roeKey01 = "LiMing" + i; Map<String, Map<String, String>>
         * familyColumnMap01 = new HashMap<String, Map<String, String>>();
         * Map<String, String>
         *
         * columnMap01 = new HashMap<String, String>(); int age = i % 100 + 1;
         * columnMap01.put("age", String.valueOf(age)); columnMap01.put("phone",
         * "13854285991"); columnMap01.put("province", "shandong");
         * columnMap01.put("city", "beijing"); columnMap01.put("chinese",
         * "100"); familyColumnMap01.put("info", columnMap01);
         *
         * int result01 = addDataForTable(tableName, roeKey01,
         * familyColumnMap01); long finishedTime = System.currentTimeMillis();
         *
         * double smallTime = (finishedTime - startTime)/1000.0;
         * System.out.println("第" + i + "个花费时间" + smallTime + "s" ); }
         *
         * long endTime = System.currentTimeMillis();
         *
         * System.out.println("end:" + endTime);
         *
         * double time = (endTime - beginTime)/1000.0;
         *
         * System.out.println("all spent time:" + time + "s");
         */

        /*
         * String tableName = "caoShuaiTest10"; List<HbaseDataEntity> hbaseDatas
         * = new ArrayList<HbaseDataEntity>(); long startTime =
         * System.currentTimeMillis(); int k = 0; for (int i = 1; i <=
         * 100000000; i++) { HbaseDataEntity hbaseData = new HbaseDataEntity();
         * hbaseData.setTableName(tableName);
         * hbaseData.setMobileKey(String.valueOf(i)); Map<String, Map<String,
         * String>> familyMaps = new HashMap<String, Map<String,String>>();
         * Map<String, String> columnMaps = new HashMap<>(); int age = i % 100 +
         * 1; columnMaps.put("age", String.valueOf(age));
         * columnMaps.put("phone", "13854285991"); columnMaps.put("province",
         * "shandong"); columnMaps.put("city", "beijing");
         * columnMaps.put("chinese", "100"); familyMaps.put("info", columnMaps);
         * hbaseData.setColumns(familyMaps);
         *
         * hbaseDatas.add(hbaseData);
         *
         * if(i%10000 == 0) { k ++; long time1 = System.currentTimeMillis();
         * insertDataList(hbaseDatas); hbaseDatas.clear(); long time2 =
         * System.currentTimeMillis(); double time = (time2 - time1)/1000.0;
         * System.out.println(k + "万条数据存入hbase花费时间" + time + "s" );
         *
         * } } long finishedTime = System.currentTimeMillis(); double smallTime
         * = (finishedTime - startTime)/1000.0; System.out.println("组装数据花费时间" +
         * smallTime + "s" );
         */

        /*
         * long beiginTime = System.currentTimeMillis();
         * insertDataList(hbaseDatas); long endTime =
         * System.currentTimeMillis(); double spentTime = (endTime -
         * beiginTime)/1000.0; System.out.println("数据花费时间" + spentTime + "s" );
         */

        /*
         * long beiginTime = System.currentTimeMillis(); String tableName =
         * "caoShuaiTest04"; String hbaseTableName =
         * "customer_portrait_library"; List<HbaseDataEntity> datas =
         * getResultScans(tableName, 10000); //System.out.println(datas); long
         * endTime = System.currentTimeMillis(); double spentTime = (endTime -
         * beiginTime)/1000.0; System.out.println("数据花费时间" + spentTime + "s" );
         *
         * //System.out.println("hbaseDatas===" + datas);
         *
         * List<HbaseDataEntity> newDatas = new ArrayList<HbaseDataEntity>();
         *
         * long time1 = System.currentTimeMillis(); for (HbaseDataEntity
         * hbaseData : datas) {
         *
         * String rowKey = hbaseData.getMobileKey();
         *
         *
         * HbaseDataEntity newData = new HbaseDataEntity();
         * newData.setMobileKey(rowKey); newData.setTableName(hbaseTableName);
         *
         * Map<String, Map<String, String>> maps = hbaseData.getColumns();
         * Map<String, Map<String, String>> newMaps = new HashMap<String,
         * Map<String,String>>();
         *
         * for (String key : maps.keySet()) {
         *
         * Map<String, String> columnsMap = maps.get(key); Map<String, String>
         * newColumnsMap = new HashMap<String, String>(); for (String columnsKey
         * : columnsMap.keySet()) {
         *
         * newColumnsMap.put(columnsKey, columnsKey);
         * updateTable(hbaseTableName, rowKey, key, columnsKey, columnsKey);
         * deleteColumn(tableName, rowKey, key, columnsKey); } newMaps.put(key,
         * newColumnsMap);
         *
         * } newData.setColumns(newMaps); newDatas.add(newData);
         *
         * }
         *
         * if(newDatas != null && newDatas.size() > 0) { long insertTime1 =
         * System.currentTimeMillis(); insertDataList(newDatas); long
         * insertTime2 = System.currentTimeMillis(); double insertTime =
         * (insertTime2 - insertTime1)/1000.0; System.out.println("修改数据时间" +
         * insertTime + "s" );
         *
         * long deleteTime1 = System.currentTimeMillis(); deleteDataList(datas);
         * long deleteTime2 = System.currentTimeMillis(); double deleteTime =
         * (deleteTime2 - deleteTime1)/1000.0; System.out.println("删除数据时间" +
         * deleteTime + "s" ); } long time2 = System.currentTimeMillis(); double
         * time = (time2 - time1)/1000.0; System.out.println("组装时间" + time + "s"
         * );
         */

        /*
         * List<String> list = new ArrayList<String>(); long insertTime1 =
         * System.currentTimeMillis(); for(int i=0; i<100000000; i++) {
         * list.add("abcdef" + i); } long insertTime2 =
         * System.currentTimeMillis(); double insertTime = (insertTime2 -
         * insertTime1)/1000.0; System.out.println("修改数据时间" + insertTime + "s"
         * );
         */

            // 7、复合条件查询
            /*HBaseDataSource dc = new HBaseDataSource(null, null,null);
            String tableName = "caoShuaiTest01";
            List<HbaseConditionEntity> hbaseConditions = new ArrayList<HbaseConditionEntity>();
            hbaseConditions.add(new HbaseConditionEntity(Bytes.toBytes("info"),
                    Bytes.toBytes("age"), Bytes.toBytes("23"),
                    FilterList.Operator.valueOf("MUST_PASS_ALL"), CompareFilter.CompareOp.valueOf("EQUAL")));

            hbaseConditions.add(new HbaseConditionEntity(Bytes.toBytes("score"),
                    Bytes.toBytes("english"), Bytes.toBytes("80"),
                    FilterList.Operator.valueOf("MUST_PASS_ALL"), CompareFilter.CompareOp.valueOf("EQUAL")));

            hbaseConditions.add(new HbaseConditionEntity(Bytes.toBytes("score"),
                    Bytes.toBytes("english"), Bytes.toBytes("80"),
                    FilterList.Operator.valueOf("MUST_PASS_ONE"), CompareFilter.CompareOp.valueOf("EQUAL")));

            hbaseConditions.add(new HbaseConditionEntity(
                    Bytes.toBytes("address"), Bytes.toBytes("city"),
                    Bytes.toBytes("beijing"), null, CompareFilter.CompareOp.valueOf("EQUAL")));

            hbaseConditions.add(new HbaseConditionEntity(Bytes.toBytes("score"),
                    Bytes.toBytes("english"), Bytes.toBytes("70"), null,
                    CompareFilter.CompareOp.valueOf("EQUAL")));

            List<HbaseDataEntity> datas = dc.queryDataByConditionsAndPage(null, tableName, hbaseConditions, 2, null);*/
            //List<HbaseDataEntity> datas = QueryDataByConditions(null, tableName, hbaseConditions); //联合条件查询 String tableName = "caoShuaiTest01";
        /*  List<String> parameters = new ArrayList<String>();
          parameters.add("info,age,EQUAL,23");
          parameters.add("score,english,EQUAL,80");
          parameters.add("address,city,EQUAL,beijing"); String pm =
          "score,english,EQUAL,78";
          List<HbaseDataEntity> datas = QueryDataByConditions(null, tableName, parameters, pm);*/
            //System.out.println(datas);


            // 分页
        /*int pageSize = 1;
        String key = null;
        int dataCount = pageSize;
        String tableName = "caoShuaiTest01";

        while (dataCount == pageSize) {
            byte[] mobileKey = null;

            if (key != null) {
                mobileKey = key.getBytes();
            }

            List<HbaseDataEntity> hbaseDatas = getHbaseDatasByPage(tableName,
                    pageSize, mobileKey);
            if (hbaseDatas != null && hbaseDatas.size() > 0) {
                System.out.println(hbaseDatas);
                dataCount = hbaseDatas.size();
                key = hbaseDatas.get(dataCount - 1).getMobileKey();
                System.out.println("Key:" + key);
            } else {
                break;
            }
        }*/

            Configuration conf = HBaseConfiguration.create();
            conf.set("hbase.zookeeper.quorum","10.11.20.115:9101");
            conf.set("hbase.zookeeper.property.clientPort","9101");
            conf.set("hbase.master","10.11.20.115:16010");
            HTable table = new HTable(conf, "ISP_SV_INTERRUPT");
            System.out.println("scanning full table:");
            Scan scan = new Scan();
            scan.setFilter(new FirstKeyOnlyFilter());
            ResultScanner scanner = table.getScanner(scan);
            for (Result rr : scanner) {
                System.out.println(new String(rr.getRow()));

            }

            /*Configuration configuration = new Configuration();
            configuration.set("hbase.zookeeper.quorum","10.11.20.115:9101");
            configuration.set("hbase.zookeeper.property.clientPort","9101");
            configuration.set("hbase.master","10.11.20.115:16010");
            Connection connection = ConnectionFactory.createConnection(configuration);*/
            try {
                /*HBaseDataSource d = new HBaseDataSource(null,null,null);
                Map conds= new HashMap();
                d.connection=connection;
                List<HbaseConditionEntity> dc = getConditions((Map) conds);
                List fields = new ArrayList();
                fields.add("REQ_ID");
                List<HbaseDataEntity> ret = d.queryDataByConditions(null, "ISP_SV_INTERRUPT", dc,fields);
                List<Map> rs = d.getResult(ret);
                if(null != rs){
                    for(Map r:rs){
                        System.out.println(r);
                    }
                }*/

                //Delete delete = new Delete(Bytes.toBytes("11WFTEST283_40"));
                //table.delete(delete);
                System.out.println("successs");
            }finally {
                /*if(null != table){
                    table.close();
                }*/
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
