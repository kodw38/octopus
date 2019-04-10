package com.octopus.tools.dataclient.dataquery;

import com.octopus.tools.dataclient.v2.DataClient2;
import com.octopus.tools.dataclient.v2.DataRouter;
import com.octopus.utils.ds.TableBean;
import com.octopus.tools.dataclient.v2.ds.TableContainer;
import com.octopus.utils.ds.TableField;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * User: wfgao_000
 * Date: 15-7-24
 * Time: 上午9:13
 */
public class AngleConfig extends XMLObject {
    static transient Log log = LogFactory.getLog(AngleConfig.class);
    DataClient2 dc;
    TableContainer tablecontainer;
    DataRouter router;
    public AngleConfig(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);

    }
    public void initial(){
        try {
            Map cfg = StringUtils.convert2MapJSONObject(getXML().getProperties().getProperty("config"));
            dc = (DataClient2) getObjectById((String) cfg.get("dataclient"));
        /*XMLMakeup x = (XMLMakeup)ArrayUtils.getFirst(xml.getChild("dataquery"));
        List<Map<String,Object>> data=null;
        if(null != x){
            data =(List<Map<String,Object>>)dc.doSomeThing(null,null,StringUtils.convert2MapJSONObject(x.getProperties().getProperty("input")),null,null);
        }else{
            data = getConfig(xml.getChild("property"));
        }*/
            router = (DataRouter) getObjectById((String) cfg.get("dbSourceRouter"));
            Map<String, TableBean> tbs = tablecontainer.getAllTables();
            initFromTableContainer(tbs);
        }catch (Exception e){
            log.error("init error",e);
        }
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

    List<Map<String,Object>> getConfig(XMLMakeup[] xs){
        if(null != xs && xs.length>0){
            List ret = new ArrayList();
            for(XMLMakeup x:xs){
                if(null != x)
                    ret.add(x.getProperties());
            }
            return ret;
        }
        return null;

    }

    /*public static Connection getConnection(DBStru stru){
        try{
            Class.forName("org.gjt.mm.mysql.Driver");
            Connection conn=null;
            if(null == stru){
                conn= DriverManager.getConnection("jdbc:mysql://localhost:3306/mysql", "root", "");
                //Connection conn= DriverManager.getConnection("jdbc:mysql://" + dbIP + ":" + dbPort + "/" + dbName + "?rewriteBatchedStatements=true&amp;cachePrepStmts=true&amp;useServerPrepStmts=true&amp;useUnicode=true&amp;characterEncoding=UTF-8&amp;autoReconnect=true&amp;failOverReadOnly=false", dbUserName, dbUserPwd);
            }else{
                conn= DriverManager.getConnection("jdbc:mysql://"+stru.getIp()+":"+stru.getPort()+"/"+stru.getDb()+"", stru.getUser(), stru.getPwd());
            }
            return conn;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }*/
    /*public static List<Map<String,Object>> query(DBStru stru,String sql,HashMap cond){
        Connection conn = null;
        try{
            conn = getConnection(stru);
            LinkedList<String> fieldlist = new LinkedList();
            List<Map<String,Object>> ret = new ArrayList<Map<String, Object>>();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSetMetaData metaData = ps.getMetaData();
            for(int i=0;i<metaData.getColumnCount();i++){
                String fieldName = metaData.getColumnName(i+1);
                fieldlist.add(fieldName.toUpperCase());
            }
            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                Map map = new HashMap();
                for(int i=0;i<fieldlist.size();i++){
                    map.put(fieldlist.get(i),rs.getObject(fieldlist.get(i)));
                }
                ret.add(map);
            }
            return ret;
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(null != conn) try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }*/

    Map<String,List<String>> tableIndexs  = new HashMap<String, List<String>>();
    Map<String,List<String>> tableCacheIndexs  = new HashMap<String, List<String>>();
    Map<String,Map<String,List<String>>> tableOutIndexs = new HashMap<String, Map<String, List<String>>>();
    Map<String,Map<String,List<String>>> tableCacheOutIndexs = new HashMap<String, Map<String, List<String>>>();
    Map<String,Map<String,List<String>>> tableOutLikes = new HashMap<String, Map<String, List<String>>>();
    Map<String,String> tablePk = new HashMap<String, String>();
    Map<String,List<String>> queryFields = new HashMap<String, List<String>>();
    Map<String,List<String>> tableLikes = new HashMap<String, List<String>>();
    Map<String,Map<String,String>> tableFieldNum = new HashMap();
    Map<String,String> tableNum = new HashMap();
    List<String> tables = new ArrayList<String>();
    List<String> cacheTables = new ArrayList<String>();
    //Map<String,String> ds = new HashMap<String,String>();
    Map<String,Map<String,String>> wildSplitChar = new HashMap<String, Map<String, String>>();

    public void initFromTableContainer(Map<String,TableBean> tbs)throws Exception{
        /*if(null == data)
            data = query(null,"select * from angle_config where state=1",null);*/
        String table,field,dbsrc,split;
        String tablenum,fieldnum;
        Iterator<String> its = tbs.keySet().iterator();
        while(its.hasNext()){
            String k = its.next();
            TableBean m = tbs.get(k);
            table = (String)m.getTableName();
            List<TableField> tfs = m.getTableFields();
            for(TableField tf:tfs) {
                if(null == tf.getUsedTypes()) continue;
                if(null == tf.getField()){
                    throw new Exception("do not config Filed for table "+table);
                }
                field = (String) tf.getField().getFieldCode();
                String[] types = tf.getUsedTypes().toArray(new String[0]);

                tablenum = tf.getTableNum();
                fieldnum = tf.getField().getFieldNum();
                //dbsrc = tablecontainer.getTableRouters().get(table).get(0).getDataSource();
                split = null;//(String) tf..get("SPLIT_CHAR");
                if (!tables.contains(table))
                    tables.add(table);
                if(tf.isCache() &&!cacheTables.contains(table)){
                    cacheTables.add(table);
                }
                for (String type : types) {
                    if (type.equals("W") && StringUtils.isNotBlank(split)) {
                        if (!wildSplitChar.containsKey(table)) wildSplitChar.put(table, new HashMap<String, String>());
                        if (!wildSplitChar.get(table).containsKey(field))
                            wildSplitChar.get(table).put(field, split);
                    }
                    /*if (!ds.containsKey(table))
                        ds.put(table, dbsrc);*/
                    if (!tableNum.containsKey(table)) tableNum.put(table, tablenum);
                    if (!tableFieldNum.containsKey(table)) tableFieldNum.put(table, new HashMap<String, String>());
                    if (!tableFieldNum.get(table).containsKey(field)) tableFieldNum.get(table).put(field, fieldnum);
                    if (type.equals("I")) {
                        if (!tableIndexs.containsKey(table)) tableIndexs.put(table, new ArrayList<String>());
                        if (!tableIndexs.get(table).contains(field))
                            tableIndexs.get(table).add(field);
                    }
                    if (type.equals("I") && tf.isCache()) {
                        if (!tableCacheIndexs.containsKey(table)) tableCacheIndexs.put(table, new ArrayList<String>());
                        if (!tableCacheIndexs.get(table).contains(field))
                            tableCacheIndexs.get(table).add(field);
                    }
                    if (type.equals("P")) {
                        tablePk.put(table, field);
                    }
                    if (type.equals("O")) {
                        if (!tableOutIndexs.containsKey(table)) {
                            tableOutIndexs.put(table, new HashMap<String, List<String>>());
                        }
                        if(tf.isCache() && !tableCacheOutIndexs.containsKey(table)){
                            tableCacheOutIndexs.put(table, new HashMap<String, List<String>>());
                        }
                        if (!tableOutLikes.containsKey(table))
                            tableOutLikes.put(table, new HashMap<String, List<String>>());
                        List<String> inx = null;
                        if (tableIndexs.containsKey(table)) {
                            inx = tableIndexs.get(table);
                        } else {
                            inx = new ArrayList<String>();
                            tableIndexs.put(table, inx);
                        }
                        if(tf.isCache()){
                            if (tableCacheIndexs.containsKey(table)) {
                                inx = tableCacheIndexs.get(table);
                            } else {
                                inx = new ArrayList<String>();
                                tableCacheIndexs.put(table, inx);
                            }
                        }
                        tableOutIndexs.get(table).put(field, inx);
                        if(tf.isCache()){
                            tableCacheOutIndexs.get(table).put(field, inx);
                        }
                        List<String> linx = null;
                        if (tableLikes.containsKey(table)) {
                            linx = tableLikes.get(table);
                        } else {
                            linx = new ArrayList<String>();
                            tableLikes.put(table, linx);
                        }
                        tableOutLikes.get(table).put(field, linx);
                    }
                    if (type.equals("Q")) {
                        if (!queryFields.containsKey(table)) queryFields.put(table, new LinkedList<String>());
                        if (!queryFields.get(table).contains(field))
                            queryFields.get(table).add(field);
                    }
                    if (type.equals("W")) {
                        if (!tableLikes.containsKey(table)) tableLikes.put(table, new ArrayList<String>());
                        if (!tableLikes.get(table).contains(field))
                            tableLikes.get(table).add(field);
                    }
                }
            }
        }

    }
    public boolean isAngleQuery(Map data){
        Map cond = (Map)data.get("conds");
        Boolean b = (Boolean)data.get("isforcedb");
        if(data.containsKey("isforcedb") && null != b && b ){
            return false;
        }
        if(null == data.get("fields")){
            return false;
        }
        String table = (String)data.get("table");
        if(null != cond){
            Iterator<String> its = cond.keySet().iterator();
            int n=0;
            while(its.hasNext()){
                String k = its.next();
                String t = table;
                String f = k;
                if(k.contains(".")){
                    t = k.substring(0,k.indexOf("."));
                    f = k.substring(k.indexOf(".")+1);
                }
                if(StringUtils.isNotBlank(t) && StringUtils.isNotBlank(f) && null != tablecontainer.getAllTables().get(t)) {
                    List<TableField> ts = tablecontainer.getAllTables().get(t).getTableFields();
                    if(null != ts){
                        for(TableField tt:ts){
                            if(tt.getField().getFieldCode().equals(f) && tt.isCache()){
                                n++;
                                break;
                            }
                        }
                    }
                }
            }
            if(n==cond.size()){
                return true;
            }
        }
        return false;
    }
    public String getWildChar(String table,String field){
        if(wildSplitChar.containsKey(table))
            return wildSplitChar.get(table).get(field);
        return null;
    }
    /*DBStru getStru(String s){
        try{
            if(StringUtils.isNotBlank(s)){
        DBStru d = new DBStru();
        String[] ss= s.split("/");
        String[] up = ss[0].split("\\@");
        if(up.length>0)
        d.setUser(up[0].trim());
                if(up.length>1)
        d.setPwd(up[1].trim());
        String[] ip = ss[1].split("\\:");
        d.setIp(ip[0].trim());
        d.setPort(ip[1].trim());
        d.setDb(ss[2].trim());
        return d;
            }else{
                return null;
            }
        }catch (Exception e){
            System.out.println(s);
            e.printStackTrace();
            return null;
        }
    }*/
    class DBStru{
        String user,pwd,ip,port,db;

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getPwd() {
            return pwd;
        }

        public void setPwd(String pwd) {
            this.pwd = pwd;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public String getPort() {
            return port;
        }

        public void setPort(String port) {
            this.port = port;
        }

        public String getDb() {
            return db;
        }

        public void setDb(String db) {
            this.db = db;
        }
    }
    public List<String[]> getDBSource(XMLParameter env,String table,Map data){
        List<String[]> ret =null;
        try {
            ret = router.getInsTableByFieldData(env, table, data);
        }catch (Exception e){
            log.error("find dbsource error",e);
        }
        if(null == ret||ret.size()==0) {
            String dbs = tablecontainer.getTableRouters().get(table).get(0).getDataSource();
            ret = new ArrayList<String[]>();
            ret.add(new String[]{dbs,table});
        }
        return ret;
    }
    public List<String> getTables(){
        return tables;
    }
    public List<String> getCacheTables(){
        return cacheTables;
    }
    public List<String> getOutLikes(String table,String outfield){
        return tableOutLikes.get(table).get(outfield);
    }
    public List<String> getLikes(String table){
        return tableLikes.get(table);
    }
    public String getTableNum(String table){
        return tableNum.get(table);
    }
    public String getTableFieldNum(String table,String field){
        return tableFieldNum.get(table).get(field);
    }

    /**
     * 从查询条件中获取某个表外键字段关联的索引  table.field
     * @param table
     * @param field
     * @return
     */
    public List<String> getOutIndexs(String table,String field){
        if(tableOutIndexs.containsKey(table))
            return tableOutIndexs.get(table).get(field);
        return null;
    }
    public Set<String> getOut(String table){
        if(tableOutIndexs.containsKey(table)){
            return tableOutIndexs.get(table).keySet();
        }
        return null;
    }
    public Set<String> getCacheOut(String table){
        if(tableCacheOutIndexs.containsKey(table)){
            return tableCacheOutIndexs.get(table).keySet();
        }
        return null;
    }
    public String getPk(String table){
        return tablePk.get(table);
    }
    public List<String> getIndexs(String table){
        return tableIndexs.get(table);
    }
    public List<String> getCacheIndexs(String table){
        return tableCacheIndexs.get(table);
    }
    public Map<String,List<String>> getQueryFields(){
        return queryFields;
    }
}
