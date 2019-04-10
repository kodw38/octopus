package com.octopus.tools.dataclient.impl.engines.impl;

import com.octopus.tools.dataclient.ds.QueryCondition;
import com.octopus.tools.dataclient.ds.field.FieldContainer;
import com.octopus.tools.dataclient.ds.field.FieldDef;
import com.octopus.tools.dataclient.ds.field.TableDef;
import com.octopus.tools.dataclient.ds.field.TableDefContainer;
import com.octopus.tools.dataclient.ds.store.FieldCondition;
import com.octopus.tools.dataclient.ds.store.TableValue;
import com.octopus.tools.dataclient.impl.engines.DC;
import com.octopus.tools.dataclient.impl.engines.IPool;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.*;
import java.util.*;

/**
 * User: Administrator
 * Date: 14-9-24
 * Time: 下午3:22
 */
public class DBPool extends XMLObject implements IPool {
    static transient Log log = LogFactory.getLog(XMLObject.class);
    Properties properties=new Properties();
    boolean isDefault;
    PoolMatch match;
    BasicDataSource dataSource;
    public DBPool(XMLMakeup xml,XMLObject parent,Object[] containers) throws Exception {
        super(xml,parent,containers);
        XMLMakeup o = (XMLMakeup)ArrayUtils.getFirst(getXML().getChild("conn"));
        if(null != o){
            XMLMakeup[] xls = o.getChild("property");
            if(null != xls){
                String name,value;
                for (XMLMakeup x:xls){
                    name = x.getProperties().getProperty("name");
                    value = x.getProperties().getProperty("value");
                    if(StringUtils.isNotBlank(name) && StringUtils.isNotBlank(value)){
                        properties.put(name,value);
                    }
                }
            }
        }
        XMLMakeup m = (XMLMakeup)ArrayUtils.getFirst(getXML().getChild("conn"));
        if(m==null || m.getChildren().size()==0){
            isDefault=true;
        }
        try{
            dataSource = (BasicDataSource) BasicDataSourceFactory.createDataSource(properties);
        }catch (Exception e){
            e.printStackTrace();
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

    @Override
    public void initial() throws Exception {

    }

    @Override
    public Properties getConnProperties() {
        return properties;
    }

    @Override
    public boolean isMatch(DC dc, Object evn) {
        return match.isMatch(dc,evn);
    }

    @Override
    public boolean isDefault() {
        return isDefault;
    }

    public Object exeSql(String sql,Map[] parameters)throws SQLException{
        Connection conn = null;
        try{
            Object obj = null;
            conn= dataSource.getConnection();
            conn.setAutoCommit(false);
            Object[][] pars=null;
            if(null != parameters && parameters.length>0){
                String[] keys = this.analyseSql(sql);
                pars = this.setSqlObject(parameters, keys);
                sql = getSql(sql,keys);
            }
            log.error("exe sql:"+sql);
            PreparedStatement ps = conn.prepareStatement(sql);
            if(null != pars){
                for(int i=0;i<pars.length;i++){
                    for(int j=0;j<pars[i].length;j++){
                        ps.setObject(j+1, pars[i][j]);
                    }
                    ps.addBatch();
                }
                obj= ps.executeBatch();
            }else{
                obj= ps.execute();
            }
            conn.commit();
            return obj;
        }catch (SQLException e){
            conn.rollback();
        }finally {
            if(null != conn){
                conn.close();
            }
        }
        return null;
    }

    public boolean addTable(TableValue tableValue)throws SQLException{
        Connection conn = null;
        try{
            conn= dataSource.getConnection();
            conn.setAutoCommit(false);
            StringBuffer sb = new StringBuffer("insert into ");
            sb.append(tableValue.getTableDef().getName().substring(tableValue.getTableDef().getName().lastIndexOf("/")+1)).append(" (");
            StringBuffer cls = new StringBuffer();
            StringBuffer qus = new StringBuffer();
            FieldDef[] fs = tableValue.getTableDef().getFieldDefs();
            for(FieldDef f:fs){
                if(cls.length()>0)cls.append(",");
                if(qus.length()>0)qus.append(",");
                cls.append(f.getFieldCode());
                qus.append("?");
            }
            sb.append(cls.toString()).append(")").append(" ").append("values").append("(").append(qus.toString()).append(")");
            PreparedStatement ps = conn.prepareStatement(sb.toString());
            for(int i=0;i<tableValue.getRecordValues().size();i++){
                for(int j=0;j<tableValue.getRecordValues().get(i).length;j++){
                    ps.setObject(j + 1, tableValue.getRecordValues().get(i)[j]);
                }
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
        }catch (SQLException e){
            e.printStackTrace();
            conn.rollback();
        }finally {
            if(null != conn){
                conn.close();
            }
        }
        return true;
    }


    public TableValue query(QueryCondition cond) throws SQLException {
        Connection conn = null;
        try{
            conn= dataSource.getConnection();
            //conn.setAutoCommit(false);
            FieldDef[] fs = cond.getQueryField();

            TableDef[] tds = TableDefContainer.getBelongTables(fs);
            LinkedList<FieldDef> list = new LinkedList();
            List<Object[]> retList = new ArrayList<Object[]>();
            if(null != tds){
                for(TableDef td:tds){
                    StringBuffer sb = new StringBuffer();
                    sb.append("select ").append(getQueryField(cond.getQueryField())).append(" from ")
                            .append(td.getName().substring(td.getName().lastIndexOf("/")+1)).append(" where 1=1")
                            .append(getWhere(cond.getFieldCondition())).append(" ").append(getOrder(cond.getOrderFields()));
                    PreparedStatement ps = conn.prepareStatement(sb.toString());
                    ResultSetMetaData metaData = ps.getMetaData();
                    for(int i=0;i<metaData.getColumnCount();i++){
                        String fieldName = metaData.getColumnName(i+1);
                        FieldDef fd = FieldContainer.getField(fieldName.toUpperCase());
                        if(null == fd){
                            throw new SQLException("not find fieldDef["+fieldName+"] in dictionary.");
                        }
                        list.add(fd);
                    }
                    ResultSet rs = ps.executeQuery();
                    while(rs.next()){
                        Object[] os = new Object[list.size()];
                        for(int i=0;i<list.size();i++){
                            os[i]=rs.getObject(list.get(i).getFieldCode());
                        }
                        retList.add(os);
                    }
                }
            }
            TableValue tv = new TableValue();
            TableDef t = new TableDef();
            t.setFieldDefs(list.toArray(new FieldDef[list.size()]));
            tv.setTableDef(t);
            tv.setRecordValues(retList);
            if(tv.getRecordValues().size()>0)
                return tv;
        }catch (Exception e){
            e.printStackTrace();
            //conn.rollback();
        }finally {
            if(null != conn){
                //conn.commit();
                conn.close();
            }
        }
        return null;
    }

    public TableValue queryTable(String tableName)throws Exception{
        Connection conn = null;
        try{
            conn= dataSource.getConnection();
            conn.setAutoCommit(false);
            StringBuffer sb = new StringBuffer();
            sb.append("select * from ").append(tableName);
            PreparedStatement ps = conn.prepareStatement(sb.toString());
            ResultSetMetaData metaData = ps.getMetaData();
            List<Object[]> retList = new ArrayList<Object[]>();
            LinkedList<FieldDef> list = new LinkedList();
            for(int i=0;i<metaData.getColumnCount();i++){
                String fieldName = metaData.getColumnName(i+1);
                FieldDef fd = new FieldDef();
                fd.setFieldCode(fieldName);
                list.add(fd);
            }
            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                Object[] os = new Object[list.size()];
                for(int i=0;i<list.size();i++){
                    os[i]=rs.getObject(list.get(i).getFieldCode());
                }
                retList.add(os);
            }
            TableValue tv = new TableValue();
            TableDef t = new TableDef();
            t.setFieldDefs(list.toArray(new FieldDef[list.size()]));
            tv.setTableDef(t);
            tv.setRecordValues(retList);
            if(tv.getRecordValues().size()>0)
                return tv;
        }catch (Exception e){
            e.printStackTrace();
            conn.rollback();
        }finally {
            if(null != conn){
                conn.commit();
                conn.close();
            }
        }
        return null;
    }

    String getQueryField(FieldDef[] fd){
        StringBuffer sb = new StringBuffer();
        for(FieldDef f:fd){
            if(sb.length()>0)sb.append(",");
            sb.append(f.getFieldCode());
        }
        return sb.toString();
    }

    String getWhere(FieldCondition[] fc){
        StringBuffer sb = new StringBuffer();
        if(null != fc){
            for(FieldCondition f:fc){
                sb.append(" and ");
                sb.append(f.toString());
            }
        }
        return sb.toString();
    }

    String getOrder(FieldDef[] fs){
        if(null != fs && fs.length>0){
            StringBuffer sb = new StringBuffer();
            for(FieldDef f:fs){
                if(sb.length()>0)sb.append(",");
                sb.append(f.getFieldCode());
            }
            return sb.toString();
        }
        return "";
    }


    public Object querySql(String sql,Map parameters,int startIndex,int endIndex) throws SQLException, NoSuchFieldException, IllegalAccessException {
        Connection conn = null;
        ResultSet res = null;
        try{
            conn= dataSource.getConnection();
            conn.setAutoCommit(true);
            sql=getPagingSQL(new StringBuffer(sql),startIndex,endIndex,parameters);
            String[] keys = this.analyseSql(sql);
            Object[] pars = this.setSqlObject(parameters, keys);
            PreparedStatement st = conn.prepareStatement(getSql(sql,keys));
            if(null != pars){
                for(int i=1;i<=pars.length;i++){
                    st.setObject(i, pars[i-1]);
                }
            }
            res = st.executeQuery();
            return getDataFromResultSet(res);
        }finally {
            if(null != res)res.close();
            if(null != conn)conn.close();
        }
    }

    public Object getMetaData(String sql,Map parameters) throws SQLException {
        Connection conn = null;
        ResultSet res = null;
        PreparedStatement st=null;
        try{
            conn= dataSource.getConnection();
            conn.setAutoCommit(true);
            String[] keys = this.analyseSql(sql);
            Object[] pars = this.setSqlObject(parameters, keys);
            st = conn.prepareStatement("select * from ("+getSql(sql,keys)+") ax_wf where 1=2");
            if(null != pars){
                for(int i=1;i<=pars.length;i++){
                    st.setObject(i, pars[i-1]);
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
    private String getPagingSQL(StringBuffer realSQL, int startIndex,int endIndex, Map map) throws NoSuchFieldException, IllegalAccessException {
        if (null == map)
            map = new HashMap();
        if("oracle".equals(getXML().getProperties().getProperty("type"))){
            if (endIndex > 0) {
                realSQL.insert(0,
                        "select * from ( select row_.*, rownum rownum_ from ( ");
                realSQL
                        .append(" ) row_ where rownum <= :P_END_INDEX ) where rownum_ >= :P_START_INDEX ");
                map.put("P_END_INDEX", new Long(endIndex));
                map.put("P_START_INDEX", new Long(startIndex));
            }
        }
        if("mysql".equals(getXML().getProperties().getProperty("type"))){
            if (endIndex > 0) {
                startIndex = startIndex-1;
                int size = endIndex -startIndex;
                realSQL.append("  LIMIT :P_START_INDEX , :P_END_INDEX ");
                map.put("P_END_INDEX", new Long(size));
                map.put("P_START_INDEX", new Long(startIndex));
            }
        }
        return realSQL.toString();
    }
    private String getSql(String sql,String[] fields){
        String exesql = sql;
        for(String field:fields){
            exesql = exesql.replaceAll( ":"+field, "?");
        }
        return exesql;

    }
    private Object[][] setSqlObject(Map[] maps,String[] fields){

        List ret = new ArrayList();
        for(Map map:maps){
            List li = new ArrayList();
            for(int i=0;i<fields.length;i++){
                if(map.containsKey(fields[i])){
                    Object o = map.get(fields[i]);
                    li.add(o);
                }
            }
            if(li.size()>0){
                ret.add(li.toArray());
            }
        }
        return (Object[][])ret.toArray();
    }
    private Object[] setSqlObject(Map map,String[] fields){
        List li = new ArrayList();
        for(int i=0;i<fields.length;i++){
            if(map.containsKey(fields[i])){
                Object o = map.get(fields[i]);
                li.add(o);
            }
        }
        if(li.size()>0){
            return li.toArray();
        }
        return null;
    }
    private String[] analyseSql(String sql){
        String s = sql;
        List li = new ArrayList();
        while(s.indexOf(":")>0){
            int n = s.indexOf(":");
            for(int i=n;i<s.length();i++){
                if(s.charAt(i)==' ' || s.charAt(i)==',' || i==s.length()-1){
                    String ke=null;
                    if(i==s.length()-1)
                        ke = s.substring(n+1);
                    else
                        ke= s.substring(n+1,i);
                    li.add(ke.trim());
                    s = s.substring(0,n)+s.substring(i);
                    break;
                }
            }
        }
        return (String[])li.toArray(new String[0]);
    }
    private Map[] getDataFromResultSet(ResultSet rs) throws SQLException {
        List cols = new ArrayList();
        for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
            cols.add(rs.getMetaData().getColumnLabel(i));
        }
        List<Map> datas = new ArrayList();
        while (rs.next()) {
            Map ds = new HashMap();
            for (int i = 0; i < cols.size(); i++) {
                ds.put(cols.get(i), rs.getObject((String)cols.get(i)));
            }
            datas.add(ds);

        }
        if (datas.size() > 0)
            return datas.toArray(new Map[datas.size()]);
        else
            return null;
    }

}
