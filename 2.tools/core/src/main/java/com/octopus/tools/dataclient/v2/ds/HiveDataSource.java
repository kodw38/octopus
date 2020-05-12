package com.octopus.tools.dataclient.v2.ds;

import com.octopus.tools.dataclient.v2.IDataSource;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.ds.Condition;
import com.octopus.utils.ds.TableBean;
import com.octopus.utils.exception.ISPException;
import com.octopus.utils.safety.RC2;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

/**
 * Created by admin on 2020/3/9.
 */
public class HiveDataSource extends XMLDoObject implements IDataSource {
    static transient Log log = LogFactory.getLog(HiveDataSource.class);
    Properties properties;
    Connection conn=null;
    Statement st = null;
    public HiveDataSource(XMLMakeup xml, XMLObject parent, Object[] containers) throws Exception {
        super(xml, parent, containers);
    }

        @Override
    public void doInitial() throws Exception {
            properties=new Properties();
            XMLMakeup[] xls = getXML().getChild("property");
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
                Class.forName(properties.getProperty("driverClassName"));
                conn = DriverManager.getConnection(properties.getProperty("url"),properties.getProperty("username"),properties.getProperty("password"));
                st = conn.createStatement();
                /*dataSource = (BasicDataSource) BasicDataSourceFactory.createDataSource(properties);
                String readonly = xml.getProperties().getProperty("isreadonly");
                if(StringUtils.isNotBlank(readonly)) {
                    dataSource.setDefaultReadOnly(StringUtils.isTrue(readonly));
                }*/
            }catch (Exception e){
                log.error(properties.toString(),e);
            }
    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(null != input){
            String op = (String)input.get("op");
            List<String> sqls = (List)input.get("sqls");
            if("query".equals(op)){
                if(null != sqls && sqls.size()>0) {
                    List ret = new LinkedList();
                    for(String sql:sqls) {
                        ResultSet rs = st.executeQuery(sql);
                        int n = rs.getMetaData().getColumnCount();
                        while(rs.next()){
                            Map m = new HashMap();
                            for(int i=1;i<=n;i++){
                                m.put(rs.getMetaData().getColumnName(i),rs.getObject(i));
                            }
                            ret.add(m);
                        }
                    }
                    return ret;
                }
            }else if("create".equals(op)){
                boolean b = true;
                if(null != sqls && sqls.size()>0) {
                    List ls = new ArrayList();
                    for (String sql : sqls) {
                        boolean r = st.execute(sql);
                        if(!r){
                            b=r;
                            ls.add(sql);
                        }
                    }
                    if(!b){
                        return new ISPException("NOT_ALL_SUCCESS", ArrayUtils.toJoinString(ls));
                    }
                    return true;
                }
                return false;
            }else if("load".equals(op)){
                boolean b = true;
                if(null != sqls && sqls.size()>0) {
                    List ls = new ArrayList();
                    for (String sql : sqls) {
                        boolean r = st.execute(sql);
                        r = st.execute(sql);
                        if(!r){
                            b=r;
                            ls.add(sql);
                        }
                    }
                    if(!b){
                        return new ISPException("NOT_ALL_SUCCESS", ArrayUtils.toJoinString(ls));
                    }
                    return true;
                }
                return false;
            }
        }
        return null;
    }

    @Override
    public ResultCheck checkReturn(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return new ResultCheck(true,ret);
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret, Exception e) throws Exception {
        return false;
    }

    @Override
    public List<Map<String, Object>> query(String tradeId, String file, String[] queryFields, List<Condition> fieldValues, Map<String, String> outs, int start, int end, TableBean tb) throws Exception {
        return null;
    }

    @Override
    public int getCount(String tradeId, String file, String[] queryFields, List<Condition> fieldValues, TableBean tb) throws Exception {
        return 0;
    }

    @Override
    public List<Map<String, String>> queryAsString(String tradeId, String file, String[] queryFields, List<Condition> fieldValues, Map<String, String> outs, int start, int end, TableBean tb) throws Exception {
        return null;
    }

    @Override
    public List<Map<String, Object>> query(String tradeId, String sql, Map map, int start, int end) throws Exception {
        return null;
    }

    @Override
    public Object addRecord(XMLParameter env, String tradeId, String taskId, String file, Map<String, Object> fieldValues) throws Exception {
        return null;
    }

    @Override
    public boolean addRecords(XMLParameter env, String tradeId, String taskId, String file, List<Map<String, Object>> fieldValues) throws Exception {
        return false;
    }

    @Override
    public boolean insertRecord(XMLParameter env, String tradeId, String taskId, String file, Map<String, Object> fieldValues, int insertPosition) throws Exception {
        return false;
    }

    @Override
    public boolean insertRecords(XMLParameter env, String tradeId, String taskId, String file, List<Map<String, Object>> fieldValues, int insertPosition) throws Exception {
        return false;
    }

    @Override
    public boolean delete(XMLParameter env, String tradeId, String taskId, String file, List<Condition> fieldValues, TableBean tb) throws Exception {
        return false;
    }

    @Override
    public boolean update(XMLParameter env, String tradeId, String taskId, String file, List<Condition> fieldValues, Map<String, Object> updateData, TableBean tb) throws Exception {
        return false;
    }

    @Override
    public IDataSource getDataSource(String name) throws Exception {
        return null;
    }

    @Override
    public boolean exist(String tradeId, String tableName) throws Exception {
        return false;
    }

    @Override
    public long getNextSequence(String name) throws Exception {
        return 0;
    }

    public void createDatabase(String database) throws Exception{
        String sql = "create database database";
        st.execute(sql);
    }
}
