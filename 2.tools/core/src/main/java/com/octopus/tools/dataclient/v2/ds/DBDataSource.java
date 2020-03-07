package com.octopus.tools.dataclient.v2.ds;

import com.octopus.tools.dataclient.v2.IDataSource;
import com.octopus.tools.dataclient.v2.ISequence;
import com.octopus.tools.mbeans.RunTimeMonitor;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.ds.*;
import com.octopus.utils.safety.RC2;
import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.time.DateTimeUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import com.sun.xml.internal.ws.model.RuntimeModeler;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * User: wfgao_000
 * Date: 15-8-14
 * Time: 下午5:15
 */
public class DBDataSource extends XMLDoObject implements IDataSource {
    transient static Log log = LogFactory.getLog(DBDataSource.class);
    static String ResultString ="result2string";
    BasicDataSource dataSource;
    Properties properties;
    Map<String,Connection> tradObjectList = new HashMap();
    public DBDataSource(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        properties=new Properties();
        XMLMakeup[] xls = xml.getChild("property");
        if(null != xls){
            String name,value;
            for (XMLMakeup x:xls){
                name = x.getProperties().getProperty("name");
                value = x.getProperties().getProperty("value");
                if(null != name && name.equalsIgnoreCase("password") && StringUtils.isNotBlank(value)){
                    if(value.startsWith("{RC2}")){
                        RC2 rc = new RC2();
                        value=rc.decrypt(value.substring(5));
                    }
                }
                if(StringUtils.isNotBlank(name) && StringUtils.isNotBlank(value)){
                    properties.put(name,value);
                }
            }
        }
        try{
            dataSource = (BasicDataSource) BasicDataSourceFactory.createDataSource(properties);
            String readonly = xml.getProperties().getProperty("isreadonly");
            if(StringUtils.isNotBlank(readonly)) {
                dataSource.setDefaultReadOnly(StringUtils.isTrue(readonly));
            }
        }catch (Exception e){
            log.error(properties.toString(),e);
        }
    }


    PreparedStatement getStatement(Connection conn,String sql,Map map)throws SQLException{
        List<String> keys = this.analyseSql(sql);
        Object[] pars = setSqlObject(map, keys);
        //PreparedStatement st = conn.prepareStatement("select distinct s_code from stock_data where c_date>'2015-08-18'");
        String s = getSql(sql,keys.toArray(new String[0]));
        PreparedStatement st = conn.prepareStatement(s);
        if(null != pars){
            for(int i=1;i<=pars.length;i++){
                if("".equals(pars[i-1])){
                    st.setNull(i,Types.VARCHAR);
                }else {
                    /*if(pars[i-1] instanceof Collection){
                        Object[] aa = ((Collection)pars[i-1]).toArray(new Object[0]);
                        st.setObject(i,convert2String(aa));
                    }else {*/
                        st.setObject(i, pars[i - 1]);
                    //}
                }
            }
        }
        return st;
    }
    String convert2String(Object[] aa){
        StringBuffer sb = new StringBuffer();
        if(aa[0] instanceof String){
           for(int i=0;i<aa.length;i++){
               if(sb.length()!=0)
                   sb.append(",");
               sb.append("'").append(aa[i]).append("'");
           }
        }
        if(aa[0] instanceof Integer){
            for(int i=0;i<aa.length;i++){
                if(sb.length()!=0)
                    sb.append(",");
                sb.append(aa[i]);
            }
        }
        return sb.toString();
    }
    ResultSet getResult(Connection conn,String sql,Map map) throws SQLException {
        long l = System.currentTimeMillis();
        PreparedStatement st = getStatement(conn,sql,map);
        return st.executeQuery();
    }
    @Override
    public List query(String tradeId,String file, String[] queryFields, List<Condition> fieldValues, Map<String, String> outs, int start, int end, com.octopus.utils.ds.TableBean tb) throws Exception {
        HashMap map = new HashMap();
        String sql = getSqlAndParameters(file,queryFields,fieldValues,outs,start,end,map,tb);
        Connection conn=null;
        try{
            conn = getConnection(null,null);
            conn.setAutoCommit(true);
            if(log.isInfoEnabled()) {
                log.info("sql:" + sql + "\n" + map);
            }
            ResultSet rs = getResult(conn,sql,map);

            //System.out.println("query cost:"+(System.currentTimeMillis()-l) +" ms");
            if(null != outs && outs.containsKey(ResultString)&&StringUtils.isTrue(outs.get(ResultString))){
                return getDataFromResultSet2String(rs);
            }else{
                return getDataFromResultSet(rs);
            }
        }catch (Exception e){
            log.error("query",e);
        }finally {
            if(null != conn)conn.close();
        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<Map<String,Object>> query(String tradeId,String sql,Map map,int start,int end) throws SQLException {
        Connection conn=null;
        try{
            StringBuffer sb = new StringBuffer(sql);
            sql = getPagingSQL(sb, start, end, map);
            conn = getConnection(null,null);
            conn.setAutoCommit(true);
            if(log.isInfoEnabled()) {
                log.info("sql:" + sql + "\n" + map);
            }
            ResultSet rs = getResult(conn,sql,map);
            //System.out.println("query cost:"+(System.currentTimeMillis()-l) +" ms");
            return getDataFromResultSet(rs);
        }catch (Exception e){
            log.error(e);
        }finally {
            if(null != conn)conn.close();
        }
        return null;
    }

    @Override
    public int getCount(String tradeId,String file, String[] queryFields, List<Condition> fieldValues, com.octopus.utils.ds.TableBean tb) throws Exception {
        HashMap map = new HashMap();
        String sql = getSqlAndParameters(file,queryFields,fieldValues,null,0,0,map,tb);
        sql = getCountSql(sql);
        Connection conn=null;
        try{
            conn = getConnection(null,null);
            conn.setAutoCommit(true);
            /*String[] keys = this.analyseSql(sql);
            Object[] pars = this.setSqlObject(map, keys);
            String esql = getCountSql(sql,keys);
            PreparedStatement st = conn.prepareStatement(esql);
            if(null != pars){
                for(int i=1;i<=pars.length;i++){
                    st.setObject(i, pars[i-1]);
                }
            }
            ResultSet rs = st.executeQuery();*/
            if(log.isInfoEnabled()) {
                log.info("sql:" + sql + "\n" + map);
            }
            ResultSet rs = getResult(conn,sql,map);
            int c=0;
            while(rs.next()){
                c=rs.getInt(1);
            }
            rs.close();
            return c;
        }catch (Exception e){
            log.error("get count error.",e);
        }finally {
            if(null != conn)conn.close();

        }
        return 0;
    }

    public List<Map<String, String>> queryAsString(String tradeId,String file, String[] queryFields, List<Condition> fieldValues, Map<String, String> outs, int start, int end, com.octopus.utils.ds.TableBean tb) throws Exception {
        HashMap map = new HashMap();
        String sql = getSqlAndParameters(file,queryFields,fieldValues,outs,start,end,map,tb);
        Connection conn=null;
        try{
            conn = getConnection(null,null);
            conn.setAutoCommit(true);
            /*String[] keys = this.analyseSql(sql);
            Object[] pars = this.setSqlObject(map, keys);
            PreparedStatement st = conn.prepareStatement(getSql(sql,keys));
            if(null != pars){
                for(int i=1;i<=pars.length;i++){
                    st.setObject(i, pars[i-1]);
                }
            }*/
            if(log.isInfoEnabled()) {
                log.info("sql:" + sql + "\n" + map);
            }
            ResultSet rs = getResult(conn,sql,map);
            return getDataFromResultSet2String(rs);
        }catch (Exception e){
            log.error(e);
        }finally {
            if(null != conn)conn.close();
        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object addRecord(XMLParameter env,String tradeId,String taskId,String file, Map<String, Object> fieldValues) throws Exception{
        Connection conn = null;
        StringBuffer sb=null;
        try{
            sb = new StringBuffer("insert into "+file);
            Iterator<String> its = fieldValues.keySet().iterator();
            sb.append("(");
            int n=0;
            StringBuffer que = new StringBuffer();
            while(its.hasNext()){
                String f = its.next();
                if(n==0){
                    sb.append("`").append(f).append("`");
                    que.append("?");
                }else{
                    sb.append(",").append("`").append(f).append("`");
                    que.append(",").append("?");
                }
                n++;
            }
            conn= getConnection(tradeId,taskId);
            conn.setAutoCommit(false);
            String pk = getTablePk(conn,file);
            Object pkv = null;
            if(StringUtils.isNotBlank(pk) ){
                if(!fieldValues.containsKey(pk)){
                    sb.append(",").append(pk);
                    que.append(",?");
                    pkv = getNextSequence(file);
                }else if(null == fieldValues.get(pk)){
                    pkv = getNextSequence(file);
                    fieldValues.put(pk,pkv);
                }
            }else{
                pkv = fieldValues.get(pk);
            }
            sb.append(")").append(" values (").append(que.toString()).append(")");
            if(log.isInfoEnabled()){
                log.info("sql:"+sb.toString());
            }
            PreparedStatement ps = conn.prepareStatement(sb.toString());
            Iterator im = fieldValues.values().iterator();
            int m=0;
            while(im.hasNext()){
                Object o = im.next();
                ps.setObject(++m,o);
                if(log.isInfoEnabled()){
                    log.info("set:"+m+"="+o);
                }
            }
            if(null != pkv){
                ps.setObject(++m,pkv);
                fieldValues.put(pk,pkv);
                if(log.isInfoEnabled()){
                    log.info("set pk:"+m+"="+pkv);
                }
            }
            ps.execute();
            if(StringUtils.isBlank(tradeId)||StringUtils.isBlank(taskId)){
                conn.commit();
            }
            return pkv;
        }catch (Exception e){
            if(StringUtils.isBlank(tradeId)||StringUtils.isBlank(taskId)){
                if(null != conn)
                    conn.rollback();
            }
            log.error("insert error:"+getXML().getId()+" "+sb.toString()+"\n"+fieldValues,e);
            throw e;
        }finally {
            if(StringUtils.isBlank(tradeId)||StringUtils.isBlank(taskId)){
                if(null != conn){
                    conn.close();
                }
            }else{
                //env.addTradeConsole(taskId,conn);
                addTradeConsole(tradeId,taskId,conn);
            }
        }
    }
    HashMap<String,String> tablepks = new HashMap();
    String getTablePk(Connection conn,String table) throws SQLException {
        if(tablepks.containsKey(table)){
            return tablepks.get(table);
        }else{
            ResultSet md = conn.prepareStatement("SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE table_name='"+table+"' AND COLUMN_KEY='PRI' ").executeQuery();
            String field=null;
            while(md.next()){
                field=md.getString(1);
            }
            md.close();
            tablepks.put(table,field);
            return field;
        }
    }

    @Override
    public boolean addRecords(XMLParameter env,String tradeId,String taskId,String file, List<Map<String, Object>> fieldValues) throws Exception{
        Connection conn = null;
        StringBuffer sb=null;
        try{
            if(fieldValues==null|| fieldValues.size()==0)return false;
            conn= getConnection(tradeId,taskId);
            conn.setAutoCommit(false);
            String pk = getTablePk(conn,file);
            if (StringUtils.isNotBlank(pk)) {
                for(Map m:fieldValues) {
                    if (!m.containsKey(pk)) {
                        m.put(pk,getNextSequence(file));
                    }
                }
            }

            sb = new StringBuffer("insert into "+file);
            Map tm = null;
            for(Map f:fieldValues){
                if(tm==null)tm = f;
                if(f.size()>tm.size()) tm = f;
            }
            Iterator<String> its = tm.keySet().iterator();
            sb.append("(");
            int n=0;
            StringBuffer que = new StringBuffer();
            LinkedList<String> fs = new LinkedList();
            while(its.hasNext()){
                String f = its.next();
                fs.add(f);
                if(n==0){
                    sb.append(f);
                    que.append("?");
                }else{
                    sb.append(",").append(f);
                    que.append(",").append("?");
                }
                n++;
            }
            sb.append(")");

            long l = System.currentTimeMillis();
            PreparedStatement ps=null;
            Map<String,Integer> meta = getSingleMetaData(null!=env?env.getTradeId():null,file,fieldValues.get(0).keySet());
            if(dataSource.getDriverClassName().contains("mysql")){
                sb.append(" values ");
                for(int i=0;i<fieldValues.size();i++){
                    Map<String,Object> vs = fieldValues.get(i);
                    sb.append("(");
                    for(int k=0;k<fs.size();k++){
                        String sk = fs.get(k);
                        sb.append(toSqlString(chgValue(meta, sk, vs.get(sk))));
                        if(k!=fs.size()-1)
                           sb.append(",");

                    }
                    if(i!=fieldValues.size()-1)
                    sb.append("),");
                    else
                        sb.append(")");
                }
                ps = conn.prepareStatement("");
                if(log.isInfoEnabled()) {
                    log.info("sql:" + sb.toString());
                }
                ps.addBatch(sb.toString());
                ps.executeBatch();
                if(log.isInfoEnabled()) {
                    log.info(new Date() + " " + Thread.currentThread().getName() + " batch insert values cost:" + (System.currentTimeMillis() - l) + " count:" + fieldValues.size());
                }

            }else{
                sb.append(" values (").append(que.toString()).append(")");

                ps = conn.prepareStatement(sb.toString());

                int count=0;

                for(Map<String,Object> vs:fieldValues){
                    int m=0;
                    for(String k:fs){
                        ps.setObject(++m,chgValue(meta, k, vs.get(k)));
                    }
                    ps.addBatch();

                    count++;
                    if(count%3000==0) {
                        ps.executeBatch();
                    }

                }
                ps.executeBatch();
                if(log.isInfoEnabled()) {
                    log.info(new Date() + " " + Thread.currentThread().getName() + " batch insert objects cost:" + (System.currentTimeMillis() - l));
                }
            }
            if(StringUtils.isBlank(tradeId)||StringUtils.isBlank(taskId)){
                conn.commit();
            }
            ps.close();
            return true;
        }catch (Exception e){
            log.error(file+" "+e.getMessage()+"\n"+(null!=sb?sb.toString():""),e);
            if(null != conn && (StringUtils.isBlank(tradeId)||StringUtils.isBlank(taskId))){
                conn.rollback();
            }
            throw e;
        }finally {
            if(StringUtils.isBlank(tradeId)||StringUtils.isBlank(taskId)){
                if(null != conn){
                     conn.close();
                }
            }else{
                //env.addTradeConsole(taskId,conn);
                addTradeConsole(tradeId,taskId,conn);
            }
        }
    }

    static String toSqlString(Object o) throws Exception {
        if(o==null)
            return "null";
        if(o instanceof String)
            return "'"+StringUtils.replace((String) o,"'","\\\'")+"'";
        if(o instanceof Timestamp || o instanceof Date) {
            SimpleDateFormat format = DateTimeUtils.getSimpleDateFormate("yyyy-MM-dd HH:mm:ss");
            synchronized (format) {
                return "'" + DateTimeUtils.getSimpleDateFormate("yyyy-MM-dd HH:mm:ss").format((Date) o) + "'";
            }
        }else
            return o.toString();
    }
    static Object chgValue(Map<String,Integer> metaData,String field,Object value){
        /*if(field.equals("EDUCATION_LEVEL")){
            System.out.println();
        }*/
        //93 datetime    bigint -5
        if(null == value)return null;
        if(metaData.containsKey(field)
                && ( metaData.get(field)==-5 || metaData.get(field)==10 || metaData.get(field)==11 || metaData.get(field)==12 || metaData.get(field)==93)
                && value instanceof String && StringUtils.isBlank((String)value)){
            if("".equals(((String) value).trim())){
                //System.out.println(field+":"+value);
                return value;
            }else {
                //System.out.println(field+":null");
                return null;
            }
        }else if(metaData.containsKey(field) && metaData.get(field)==4 && value instanceof String && StringUtils.isBlank((String)value)){//int type
            //System.out.println(field+":"+0);
            return 0;
        }else{
            //System.out.println(field+":"+value);
            return value;
        }
    }

    @Override
    public boolean insertRecord(XMLParameter env,String tradeId,String taskId,String file, Map<String, Object> fieldValues, int insertPosition) throws Exception{
        return null!=addRecord(env,tradeId,taskId,file,fieldValues);
    }

    @Override
    public boolean insertRecords(XMLParameter env,String tradeId,String taskId,String file, List<Map<String, Object>> fieldValues, int insertPosition) throws Exception{
        return addRecords(env,tradeId,taskId,file,fieldValues);
    }

    @Override
    public boolean delete(XMLParameter env,String tradeId,String taskId,String file, List<Condition> fieldValues, com.octopus.utils.ds.TableBean tb)throws Exception{
        Connection conn = null;
        try{
            conn = getConnection(tradeId,taskId);
            conn.setAutoCommit(false);
            StringBuffer sb = new StringBuffer("delete from ");
            sb.append(file);
            HashMap map = new HashMap();
            if(null != fieldValues){
                sb.append(" where ").append(getWhere(fieldValues,map,tb));
            }
            String sql = sb.toString();
            if(log.isInfoEnabled()){
                log.info("sql:"+sql+"\n"+map);
            }
            PreparedStatement st = getStatement(conn,sql,map);
            st.execute();
            if(null != conn && (StringUtils.isBlank(tradeId)||StringUtils.isBlank(taskId))){
                conn.commit();
            }
            st.close();
            return true;
        }catch (Exception e){
            if(StringUtils.isBlank(tradeId)||StringUtils.isBlank(taskId)){
                conn.rollback();
            }
            throw e;
        }finally {
            if(null != conn && (StringUtils.isBlank(tradeId)||StringUtils.isBlank(taskId))){
                if(null != conn)conn.close();
            }else{
                //env.addTradeConsole(taskId,conn);
                addTradeConsole(tradeId,taskId,conn);
            }
        }
    }
    Map filterData(Map m){
        if(null != m){
            HashMap ret = new HashMap();
            Iterator it = m.keySet().iterator();
            while(it.hasNext()){
                String k = (String)it.next();
                Object v = m.get(k);
                if(!(null != v && v instanceof String && XMLParameter.isHasRetainChars((String)v))){
                    ret.put(k,v);
                }
            }
            return ret;
        }
        return null;
    }
    @Override
    public boolean update(XMLParameter env,String tradeId,String taskId,String file, List<Condition> fieldValues, Map<String, Object> updateData, com.octopus.utils.ds.TableBean tb)throws Exception{
        Connection conn = null;
        String sql=null;
        try{
            updateData = filterData(updateData);
            conn = getConnection(tradeId,taskId);
            conn.setAutoCommit(false);
            StringBuffer sb = new StringBuffer("update ");
            sb.append(file).append(" set ");
            HashMap map = new HashMap();
            appendSet(sb,updateData,map);
            if(null != fieldValues){
                sb.append(" where ").append(getWhere(fieldValues,map,tb));
            }
            sql = sb.toString();
            Map<String,Integer> metaData = getSingleMetaData(null!=env ?env.getTradeId():null,file,updateData.keySet());

            List<String> keys = this.analyseSql(sql);
            Object[] pars = this.setSqlObject(map, keys);
            String[] ks = keys.toArray(new String[0]);
            String s = getSql(sql,ks);
            if(log.isInfoEnabled()){
                log.info("sql:"+s);
            }
            PreparedStatement st = conn.prepareStatement(s);
            if(null != pars){
                for(int i=1;i<=pars.length;i++){
                    Object o = chgValue(metaData, ks[i - 1].substring(1), pars[i - 1]);
                    st.setObject(i, o);
                    if(log.isInfoEnabled()){
                        log.info("set "+i+"="+o);
                    }
                }
            }
            st.execute();
            if(null != conn && (StringUtils.isBlank(tradeId)||StringUtils.isBlank(taskId))){
                conn.commit();
            }
            st.close();
            return true;
        }catch (Exception e){
            if(null != conn && (StringUtils.isBlank(tradeId)||StringUtils.isBlank(taskId))){
                conn.rollback();
            }
            throw new SQLException(sql,e);
        }finally {
            if(null != conn && (StringUtils.isBlank(tradeId)||StringUtils.isBlank(taskId))){
                if(null != conn)conn.close();
            }else{
                //env.addTradeConsole(taskId,conn);
                addTradeConsole(tradeId,taskId,conn);
            }
        }
    }
    boolean upadd(XMLParameter env,String tradeId,String taskId,String file, List<String> keyfields, Object updateData, com.octopus.utils.ds.TableBean tb)throws Exception{
        if(updateData instanceof Collection){
             Iterator it = ((Collection) updateData).iterator();
            while(it.hasNext()){
                Map m = (Map)it.next();
                upaddOne(env,tradeId,taskId,file,keyfields,(Map)m,tb);
            }
            return true;
        }else  if(updateData instanceof Map){
            return upaddOne(env,tradeId,taskId,file,keyfields,(Map)updateData,tb);
        }else{
            throw new Exception("now not support the value type:"+updateData);
        }
    }
    boolean upaddOne(XMLParameter env,String tradeId,String taskId,String file, List<String> keyfields, Map<String, Object> updateData, com.octopus.utils.ds.TableBean tb)throws Exception{
        List<Condition> conds = new ArrayList();
        for(String k:keyfields){
            conds.add(Condition.createEqualCondition(k,updateData.get(k)));
        }
        int n = getCount(null!=env?env.getTradeId():null,file,keyfields.toArray(new String[0]),conds,tb);
        if(n>0){
            //update
            return update(env,tradeId,taskId,file,conds,updateData,tb);
        }else{
            //insert
            return insertRecord(env,tradeId,taskId,file,updateData,-1);
        }
    }

    public Connection getConnection(String tradeid,String taskId) throws SQLException {
        if(StringUtils.isNotBlank(tradeid) && null != tradObjectList.get(tradeid)){
            return tradObjectList.get(tradeid);
        }else {

            if(null != dataSource) {
                return dataSource.getConnection();
            }else{
                throw new SQLException("can not create database connect ["+properties+"]");
            }
        }
    }

    @Override
    public IDataSource getDataSource(String name) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long getNextSequence(String name) throws Exception{
        ISequence s = (ISequence)getObjectById("sequence");
        return s.getNextSequence(name);
    }

    void appendSet(StringBuffer sb,Map<String,Object> upvalue,Map<String,Object> pars) {
        if(null != upvalue){
            int n=0;
            Iterator<String> its = upvalue.keySet().iterator();
            while(its.hasNext()){
                String field = its.next();
                String k = ":"+field;
                pars.put(k,upvalue.get(field));
                if(n==0){
                   sb.append("`").append(field).append("`").append("=").append(k);
                }else{
                    sb.append(",").append("`").append(field).append("`").append("=").append(k);
                }
                n++;
            }
        }
    }
    String getSqlAndParameters(String table,String[] queryFields,List<Condition> conds,Map<String,String> out,int start,int end,HashMap map, com.octopus.utils.ds.TableBean tb){
        StringBuffer sb = new StringBuffer("select ");
        if(null != queryFields)
            sb.append(StringUtils.join(queryFields,","));
        else
            sb.append(" * ");
        sb.append(" from ").append(table);
        if(null != conds && conds.size()>0){
            sb.append( " where ");
            sb.append(getWhere(conds,map,tb));
        }
        if(null != out && out.size()>0){
            Iterator<String> its = out.keySet().iterator();
            while(its.hasNext()){
                String k = its.next();
                if(k.equals(ResultString)) continue;
                sb.append(" ").append(k).append(" ").append(out.get(k));
            }
        }
        return getPagingSQL(sb, start, end, map);
    }

    String getWhere(List<Condition> conds,Map map, com.octopus.utils.ds.TableBean tb){
        StringBuffer sb = new StringBuffer(" 1=1 ");
        for(int i=0;i<conds.size();i++){
                String s = conds.get(i).toString(map,tb);
                if(StringUtils.isNotBlank(s)) {
                    sb.append(" and ").append(s);
                }

        }
        return sb.toString();
    }

    public Object exeSql(XMLParameter env,String tradeId,String taskId,String sql)throws Exception{
        Connection conn = null;
        ResultSet res = null;
        try{
            conn= getConnection(tradeId,taskId);
            conn.setAutoCommit(false);
            Statement st = conn.createStatement();
            if(sql.contains(";")){
                String[] ss = sql.split(";");
                for(String s:ss){
                    if(StringUtils.isNotBlank(s)) {
                        st.addBatch(s);
                    }
                }
                st.executeBatch();
            }else{
                st.execute(sql);
            }
            if(null != conn && (StringUtils.isBlank(tradeId)||StringUtils.isBlank(taskId))){
                conn.commit();
            }
            st.close();
            return true;
        }catch (Exception e){
            log.error(sql+"\n"+e.getMessage(), e);
            if(null != conn && (StringUtils.isBlank(tradeId)||StringUtils.isBlank(taskId))){
                conn.rollback();
            }
            throw e;
        }finally {
            if(null != conn && (StringUtils.isBlank(tradeId)||StringUtils.isBlank(taskId))){
                if(null != conn)conn.close();
            }else{
                //env.addTradeConsole(taskId,conn);
                addTradeConsole(tradeId,taskId,conn);
            }
        }
    }

    Object querySql(String tradeId,String sql,Map parameters,int startIndex,int endIndex) throws SQLException, NoSuchFieldException, IllegalAccessException {
        Connection conn = null;
        ResultSet res = null;
        try{
            conn= getConnection(null,null);
            conn.setAutoCommit(true);
            sql=getPagingSQL(new StringBuffer(sql),startIndex,endIndex,parameters);
            res = getResult(conn,sql,parameters);
            return getDataFromResultSet(res);
        }catch(Exception e){
            throw new SQLException(sql,e);
        }finally {
            if(null != res)res.close();
            if(null != conn)conn.close();
        }
    }
     Map<String,Integer> getSingleMetaData(String tradeId,String table,Collection fields)throws SQLException{
        StringBuffer sql= new StringBuffer("select ");
        if(null != fields){
            Iterator<String> ts = fields.iterator();
            while(ts.hasNext()){
                sql.append(ts.next()).append(",");
            }
            sql.deleteCharAt(sql.length()-1);
        }else{
            sql.append(" * ");
        }
        sql.append(" from ").append(table);
        ResultSetMetaData m =  getMetaData(tradeId,sql.toString(),null);
        HashMap<String,Integer> ret = new HashMap();
       for(int i=1;i<=m.getColumnCount();i++){
           ret.put(m.getColumnName(i),m.getColumnType(i));
       }
         return ret;
    }
    ResultSetMetaData getMetaData(String tradeId,String sql,Map parameters) throws SQLException {
        Connection conn = null;
        ResultSet res = null;
        PreparedStatement st=null;
        try{
            conn= getConnection(null,null);
            conn.setAutoCommit(true);
            List<String> keys = this.analyseSql(sql);
            Object[] pars = this.setSqlObject(parameters, keys);
            st = conn.prepareStatement("select * from ("+getSql(sql,keys.toArray(new String[0]))+") ax_wf where 1=2");
            if(null != pars){
                for(int i=1;i<=pars.length;i++){
                    if("".equals(pars[i-1])){
                        st.setNull(i,Types.VARCHAR);
                    }else {
                        st.setObject(i, pars[i - 1]);
                    }

                }
            }
            res = st.executeQuery();
            ResultSetMetaData m = res.getMetaData();
            return m;
        }finally {
            if(null != res)res.close();
            if(null != st)st.close();
            if(null != conn)conn.close();
        }
    }

    private String getPagingSQL(StringBuffer realSQL, int startIndex,int endIndex, Map map) {
        if (null == map)
            map = new HashMap();
        if(dataSource.getDriverClassName().contains("oracle")){
            if (endIndex > 0) {
                realSQL.insert(0,
                        "select * from ( select row_.*, rownum rownum_ from ( ");
                realSQL
                        .append(" ) row_ where rownum <= :P_END_INDEX ) where rownum_ >= :P_START_INDEX ");
                map.put(":P_END_INDEX", new Long(endIndex));
                map.put(":P_START_INDEX", new Long(startIndex));
            }
        }
        if(dataSource.getDriverClassName().contains("mysql")){
            if (endIndex > 0) {
                startIndex = startIndex;
                int size = endIndex -startIndex;
                realSQL.append("  LIMIT :P_START_INDEX , :P_END_INDEX ");
                map.put(":P_END_INDEX", new Long(size));
                map.put(":P_START_INDEX", new Long(startIndex));
            }
        }
        return realSQL.toString();
    }
    private String getSql(String sql,String[] fields){
        String exesql = sql;
        List<String> ls = Arrays.asList(fields);
        Collections.sort(ls,new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.length()>o2.length()?-1:1;
            }
        });
        for(String field:ls){
            exesql = StringUtils.replace(exesql,field, "?");
        }
        /*if(log.isInfoEnabled())
        log.info("sql:"+exesql);
        */
        return exesql;

    }
    private String getCountSql(String sql){
        return "select count(0) from ( "+sql+" ) as a";
    }
    private Object[][] setSqlObject(Map[] maps,List<String> fields){

        List ret = new LinkedList();
        List fs = new LinkedList();
        for(Map map:maps){
            List li = new ArrayList();
            for(int i=0;i<fields.size();i++){
                if(map.containsKey(fields.get(i))){
                    Object o = map.get(fields.get(i));
                    li.add(o);
                    fs.add(fields.get(i));
                }
            }
            if(li.size()>0){
                ret.add(li.toArray());
            }
            fields.clear();
            fields.addAll(fs);
        }
        return (Object[][])ret.toArray();
    }
    private Object[] setSqlObject(Map map,List<String> fields){
        List li = new LinkedList();
        List<String> fs = new LinkedList();

        for(int i=0;i<fields.size();i++){
            if(map.containsKey(fields.get(i))){
                Object o = map.get(fields.get(i));
                //if("".equals(o))o = "''";
                li.add(o);
                fs.add(fields.get(i));
            }
        }
        fields.clear();
        fields.addAll(fs);
        /*if(log.isInfoEnabled()){
            log.info("\n fields "+fields+"\n values "+li);
        }*/
        if(li.size()>0){
            return li.toArray();
        }
        return null;
    }
    private List<String> analyseSql(String sql){
        String s = sql;
        List li = new LinkedList();
        while(s.indexOf(":")>0){
            int n = s.indexOf(":");
            for(int i=n;i<s.length();i++){
                if(s.charAt(i)==' ' || s.charAt(i)==',' || i==s.length()-1){
                    String ke=null;
                    if(i==s.length()-1)
                        ke = s.substring(n);
                    else
                        ke= s.substring(n,i);
                    li.add(ke.trim());
                    s = s.substring(0,n)+s.substring(i);
                    break;
                }
            }
        }
        /*if(log.isInfoEnabled()){
            log.info("sql:"+sql);

        }*/
        //li = ArrayUtils.sortByLen(li,ArrayUtils.DESC);
        return li;
    }
    private List<Map<String,Object>> getDataFromResultSet(ResultSet rs) throws SQLException {
        List<String> cols = new ArrayList();
        for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
            cols.add(rs.getMetaData().getColumnLabel(i));
        }
        List<Map<String,Object>> datas = new LinkedList<Map<String,Object>>();
        while (rs.next()) {
            Map<String,Object> ds = new HashMap();
            for (int i = 0; i < cols.size(); i++) {
                ds.put(cols.get(i).toUpperCase(), rs.getObject((String)cols.get(i)));
            }
            datas.add(ds);

        }
        if (datas.size() > 0)
            return datas;
        else
            return null;
    }
    private List<Map<String,String>> getDataFromResultSet2String(ResultSet rs) throws SQLException {
        List<String> cols = new ArrayList();
        for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
            cols.add(rs.getMetaData().getColumnLabel(i).toUpperCase());
        }
        List<Map<String,String>> datas = new LinkedList<Map<String,String>>();
        while (rs.next()) {
            Map<String,String> ds = new HashMap();
            for (int i = 0; i < cols.size(); i++) {
                ds.put(cols.get(i), rs.getString((String) cols.get(i)));
            }
            datas.add(ds);

        }
        if (datas.size() > 0)
            return datas;
        else
            return null;
    }
    Object exeSql(XMLParameter env,String tradeId,String xmlid,String sql,int start,int end)throws Exception {
        if(null != env)
            sql = (String)env.getExpressValueFromMap(sql,this);
        sql = StringUtils.replace(sql,"\\'","'");
        Object ret=null;
        long l = System.currentTimeMillis();
        if(sql.startsWith("select") || sql.startsWith("SELECT")){
            Object r=null;
            log.info("execute sql:" + sql);
            if(null != env)
                ret = querySql(null != env ? env.getTradeId() : null, sql, env.getReadOnlyParameter(), start, end);
            else
                ret= querySql(null!=env?env.getTradeId():null,sql,new HashMap(),start,end);
        }else
            ret= exeSql(env,tradeId,xmlid,sql);

        RunTimeMonitor.addSqlStaticInfo(sql,System.currentTimeMillis()-l,null,env);
        return ret;
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        String op=null,table=null;
        int start=0,end=0;
        Object conds=null;
        List fields = null;
        Object datas=null;
        Map formate= null;
        List<String> sqls=null;
        String tradeId=null;
        List<Map> structure = null;
        com.octopus.utils.ds.TableBean tb =null;
        if(null != env)
            tradeId=env.getTradeId();
        if(null != input){
            op = (String)input.get("op");
            conds = input.get("conds");
            datas = input.get("datas");
            table = (String)input.get("table");
            tb = (com.octopus.utils.ds.TableBean)input.get("tableDefine");
            fields=(List)input.get("fields");
            String st = (String)input.get("start");
            structure = (List)input.get("structure") ;
            if(StringUtils.isNotBlank(st))
                start=Integer.parseInt(st);
            String et = (String)input.get("end");
            if(StringUtils.isNotBlank(et))
                end=Integer.parseInt(et);

            formate= (Map)input.get("format");
            sqls = (List)input.get("sqls");
        }
        if(null != sqls){
            List ret = null;
            if(sqls.size()==1){
                return exeSql(env,tradeId,xmlid,sqls.get(0),start,end);
            }else{
                ret =new LinkedList();
                LinkedList ps = new LinkedList();
                for(String s:sqls){
                    ps.add(new Object[]{env,tradeId,xmlid,s,start,end});
                }
                Object[] rs = ExecutorUtils.multiWorkSameObjectWaiting(this,"exeSql",new Class[]{XMLParameter.class,String.class,String.class,String.class,int.class,int.class},ps);
                for(Object o:rs){
                    ret.add(o);
                }
            }
            if(null != ret && ret.size()>0){
                Map pre=null;
                boolean iscombain=false;
                int n=0,j=0;
                for(int i=0;i<ret.size();i++){
                    Object o = ret.get(i);
                    if(null != o) {
                        if (o instanceof List) {
                            n++;
                            j=i;
                            if (null != o && o instanceof List && ((List) o).size() > 0 && ((List) o).size() > 0 && null != ((List) o).get(0) && ((List) o).get(0) instanceof Map) {
                                if (null != pre) {
                                    if (pre.keySet().containsAll(((Map) ((List) o).get(0)).keySet())) {
                                        iscombain = true;
                                    } else {
                                        iscombain = false;
                                        break;
                                    }
                                } else {
                                    pre = (Map) ((List) o).get(0);
                                }
                            }
                        }
                    }
                }
                if(!iscombain){
                    if(n==0){
                        return null;
                    }else if(n==1){
                        return ((List)ret).get(j);
                    }else {
                        return ret;
                    }
                }else{
                    List rm =null;
                    for(Object o:ret){
                        if(null != o && o instanceof List && ((List)o).size()>0 &&((List)o).size()>0&& null !=((List)o).get(0) && ((List)o).get(0) instanceof Map){
                            if(rm==null){
                                rm =(List)o;
                            }else{
                                rm.addAll((List)o);
                            }
                        }
                    }
                    return rm;
                }
            }

        }else if("createTable".equals(op)){
            return createTable(table,structure);
        }else if("truncate".equals(op)){
            return truncateTable(env,tradeId,xmlid,table);
        }else if("exist".equals(op)){
            return exist(null != env?env.getTradeId():null,table);
        }else if("delete".equals(op)){
            if(null != conds){
                if(conds instanceof Map) {
                    List<Condition> cds = new ArrayList<Condition>();
                    Iterator<String> its = ((Map)conds).keySet().iterator();
                    while (its.hasNext()) {
                        String f = its.next();
                        if (StringUtils.isNotBlank(f)) {
                            Condition cd = Condition.createCondition(env,f, ((Map)conds).get(f));
                            cds.add(cd);
                        }
                    }
                    return delete(env, tradeId, xmlid, table, cds,tb);
                }
            }else {
                throw new Exception("not set delete table["+table+"] conds");
            }

        }else if("add".equals(op)){
            if(datas instanceof List){
                return addRecords(env,tradeId,xmlid,table,(List<Map<String,Object>>)datas);
            }else if(datas instanceof Map){
                return addRecord(env,tradeId,xmlid,table,(Map)datas);
            }else{
                throw new Exception("not support add data type is "+datas.getClass());
            }


        }else if("update".equals(op)){
            if(null != conds && conds instanceof Map){
                List<Condition> cds = new ArrayList<Condition>();
                Iterator<String> its = ((Map)conds).keySet().iterator();
                while(its.hasNext()){
                    String f = its.next();
                    if(StringUtils.isNotBlank(f)){
                        Condition cd = Condition.createCondition(env,f,((Map)conds).get(f));
                        cds.add(cd);
                    }
                }
                if(null != datas)
                    return update(env,tradeId,xmlid,table,cds,(Map)datas,tb);
                else
                    throw new Exception("not set update value");
            }else {
                throw new Exception("not set update table["+table+"] without conds");
            }

        }else if("upadd".equals(op)){
            if(null !=input.get("keyfields")){
                List kf = (List) input.get("keyfields");
                if (null != datas)
                    return upadd(env, tradeId, xmlid, table, kf, datas, tb);
                else
                    throw new Exception("not set update value");
            }else{
                throw new Exception("if upadd , need to set [keyfields] property");
            }
        }else if("query".equals(op)){
            List<Map<String,Object>> ls=null;
            if(null== conds || conds instanceof Map){
                return queryOne(env,table,fields,(Map)conds,formate,start,end,tb);
            }else if(conds instanceof Collection){
                Iterator its = ((Collection) conds).iterator();
                List<Map<String,Object>> ret = new LinkedList<Map<String, Object>>();
                while(its.hasNext()){
                    Object o = its.next();
                    if(o instanceof Map){
                        List m = queryOne(env,table,fields,(Map)o,formate,start,end,tb);
                        if(null != m){
                            ret.addAll(m);
                        }
                    }
                }
                return ret;
            }

        }else if("count".equals(op)){
            List<Condition> cds=null;
            if(null != conds && conds instanceof Map){
                cds = new ArrayList<Condition>();
                Iterator<String> its = ((Map)conds).keySet().iterator();
                while(its.hasNext()){
                    String f = its.next();
                    if(StringUtils.isNotBlank(f)){
                        Condition cd = Condition.createCondition(env,f,((Map)conds).get(f));
                        cds.add(cd);
                    }
                }

            }
            int ret=0;
            if(null != fields)
                 ret =getCount(null!=env?env.getTradeId():null,table,(String[])fields.toArray(new String[0]),cds,tb);
            else
                ret= getCount(null!=env?env.getTradeId():null,table,null,cds,tb);
            return ret;

        }else if("getNextSeq".equals(op)){
            return getNextSequence((String)input.get("table"));
        }

        return null;
    }
    List<Map> queryOne(XMLParameter env,String table,List fields,Map conds,Map formate,int start,int end, com.octopus.utils.ds.TableBean tb)throws Exception{
        List<Condition> cds=null;
        if(null != conds && conds instanceof Map){
            cds = new ArrayList<Condition>();
            Iterator<String> its = ((Map)conds).keySet().iterator();
            while(its.hasNext()){
                String f = its.next();
                if(StringUtils.isNotBlank(f)){
                    Condition cd = Condition.createCondition(env,f,((Map)conds).get(f));
                    cds.add(cd);
                }
            }

        }
        long l = System.currentTimeMillis();
        List ret=null;
        if(null != fields && fields.size()>0 && fields.get(0) instanceof String)
            ret= query(null!=env?env.getTradeId():null,table,(String[])fields.toArray(new String[0]),cds,formate,start,end,tb);
        else
            ret= query(null!=env?env.getTradeId():null,table,null,cds,formate,start,end,tb);
        RunTimeMonitor.addSqlStaticInfo(table,System.currentTimeMillis()-l,conds,env);
        return ret;
    }
    public boolean exist(String tradeId,String tableName) throws Exception {
        Connection conn = null;
        try{
            conn= getConnection(null,null);
            ResultSet rs = conn.getMetaData().getTables(null,null,tableName,null);
            boolean is=false;
            if(rs.next()){
                is=true;
            }else{
                is=false;
            }
            rs.close();
            return is;
        }catch(SQLException e){
            log.error(getXML(),e);
            throw e;
        }finally {
            if(null != conn)conn.close();
        }

    }
    boolean createTable(String tableName ,List<Map> structure) throws SQLException {
        Connection conn = null;
        try{
            List<String> li = new LinkedList<String>();
            StringBuffer sql = new StringBuffer();
            conn= getConnection(null,null);
            if(dataSource.getDriverClassName().contains("mysql")){
                sql.append("create table "+tableName+" (");
                boolean isaddedField =false;
                List pks = new LinkedList();
                for(Map m:structure){
                    Map fd = (Map)m.get("field");

                    boolean notNull = false;
                    if(m.get("notNull") instanceof String) {
                       notNull= StringUtils.isTrue((String) m.get("notNull"));
                    }else{
                        notNull=(Boolean)m.get("notNull");
                    }
                    if(null != fd){
                        /*fd.get("fieldName");
                        fd.get("fieldCode");
                        fd.get("fieldType");
                        fd.get("DBFieldType");
                        fd.get("fieldLen");*/
                        if(!isaddedField){
                            sql.append("`").append((String.valueOf(fd.get("fieldCode"))).toUpperCase()).append("` ").append(getType(String.valueOf(fd.get("DBFieldType")),String.valueOf(fd.get("fieldLen")))).append(" ").append(notNull?"NOT NULL":"NULL");
                            isaddedField=true;
                        }else
                            sql.append(",").append("`").append((String.valueOf(fd.get("fieldCode"))).toUpperCase()).append("` ").append(getType(String.valueOf(fd.get("DBFieldType")),String.valueOf(fd.get("fieldLen")))).append(" ").append(notNull?"NOT NULL":"NULL");
                        List<String> usetypes = (List)m.get("usedTypes");
                        if(null != usetypes){

                            for(String t:usetypes){
                                if("P".equals(t)){
                                    pks.add(((String) fd.get("fieldCode")).toUpperCase());
                                }else if("I".equals(t)){// index field
                                    li.add("create index IDX_"+(String)fd.get("fieldCode")+" on "+tableName+" ("+(String)fd.get("fieldCode")+"); ");
                                }else if("Q".equals(t)){//query field

                                }else if("O".equals(t)){// out key field

                                }
                            }

                        }
                    }

                }
                if(pks.size()>0){
                    if(pks.size()==1) {
                        sql.append(", PRIMARY KEY (`").append(pks.get(0)).append("`)  ");
                    }else{
                        sql.append(", PRIMARY KEY (").append(ArrayUtils.toJoinString(pks)).append(")  ");
                    }
                }
                sql.append(") ENGINE=InnoDB  DEFAULT CHARSET=utf8;");
                li.add(0,sql.toString());
            }
            log.info(getXML().getId()+" create table:\n"+sql.toString());
            PreparedStatement ps = conn.prepareStatement("");
            for(String s:li)
                ps.addBatch(s);
            int[] iss = ps.executeBatch();
            boolean is=true;
            for(int i:iss){
               if(i<0)
                   return false;
            }
            return is;
        }finally {
            if(null != conn)conn.close();
        }
    }
    boolean truncateTable(XMLParameter env,String tradeId,String taskId,String table )throws SQLException {
        Connection conn = null;
        try{
            conn= getConnection(tradeId,taskId);
            conn.setAutoCommit(false);
            boolean ret= conn.createStatement().execute("truncate table "+table);
            if(StringUtils.isBlank(tradeId)|| StringUtils.isBlank(taskId)){
                conn.commit();
            }
            return ret;
        }catch (Exception e){
            if(null != conn &&(StringUtils.isBlank(tradeId)||StringUtils.isBlank(taskId))){
                conn.rollback();
            }
            return false;
        }finally{
            if(null != conn && (StringUtils.isBlank(tradeId)||StringUtils.isBlank(taskId))) {
                if (null != conn) conn.close();
            }else{
                return addTradeConsole(tradeId,taskId,conn);
            }
        }
    }

    boolean addTradeConsole(String tradeId,String taskId,Connection conn){
        if(StringUtils.isNotBlank(tradeId) && null != conn) {
            Connection c = tradObjectList.get(tradeId);
            if (null == c) {
                //list = new ArrayList();
                tradObjectList.put(tradeId,conn);
            }
            //list.add(conn);
        }
        return true;
    }

    String getType(String name,String len){
        if("Date".equalsIgnoreCase(name)||"DateTime".equalsIgnoreCase(name)||"double".equalsIgnoreCase(name))
            return name;
        else {
            int l = 0;
            if(null != len)
                l = Integer.parseInt(len);
            if(l>0) {
                if(name.equalsIgnoreCase("LONGTEXT")){
                    return name;
                }
                return name + "(" + len + ")";
            }
            return name;

        }
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResultCheck checkReturn(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config,Object ret)throws Exception{
        try {
            if (null != env && StringUtils.isNotBlank(env.getTradeId())) {
                Connection os = tradObjectList.get(env.getTradeId());
                if (null != os) {
                    //for (Object o : os) {
                    if (os instanceof Connection && !((Connection) os).isClosed()) {
                        try {
                            ((Connection) os).commit();

                        } finally {
                            ((Connection) os).close();
                        }
                    }
                    //}
                    //os.clear();
                }

                return true;
            } else {
                return false;
            }
        }finally {
            tradObjectList.remove(env.getTradeId());
        }
    }
    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config,Object ret,Exception e) throws Exception {
        try {
            if (null != env && StringUtils.isNotBlank(env.getTradeId())) {
                Connection os = tradObjectList.get(env.getTradeId());
                if (null != os) {
                    //for (Object o : os) {
                    if (os instanceof Connection && !((Connection) os).isClosed()) {
                        try {
                            ((Connection) os).rollback();
                        } finally {
                            ((Connection) os).close();
                        }
                    }
                    //}
                    //os.clear();
                }
                return true;  //To change body of implemented methods use File | Settings | File Templates.
            } else {
                return false;
            }
        }finally{
            tradObjectList.remove(env.getTradeId());
        }
    }
}
