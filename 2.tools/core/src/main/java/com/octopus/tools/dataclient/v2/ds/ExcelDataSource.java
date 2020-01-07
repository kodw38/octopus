package com.octopus.tools.dataclient.v2.ds;

import com.octopus.tools.dataclient.v2.IDataSource;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.ds.*;
import com.octopus.utils.ds.TableBean;
import com.octopus.utils.file.impl.excel.ExcelReader;
import com.octopus.utils.file.impl.excel.ExcelWriter;
import com.octopus.utils.safety.RC2;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;

import java.io.File;
import java.io.FileInputStream;
import java.util.*;

/**
 * Created by robai on 2017/9/5.
 */
public class ExcelDataSource extends XMLDoObject implements IDataSource {
    Properties getProperties(XMLMakeup xml)throws Exception{
        Properties properties=new Properties();
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
        return properties;
    }
    public ExcelDataSource(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }
    ExcelReader getReader()throws Exception{
        Properties por = getProperties(getXML());
        //List<FileInfo> in = FileUtils.getAllProtocolFiles(por.getProperty("url"),null,false);
        String url = (String)XMLParameter.getExpressValueFromMap(por.getProperty("url"),getEmptyParameter(),this);
        FileInputStream in = new FileInputStream(new File(url));
        if(null != in ) {
            ExcelReader reader = new ExcelReader(in);
            return reader;
        }
        return null;
    }
    ExcelWriter getWriter(XMLParameter env)throws Exception{
        Properties por = getProperties(getXML());
        Map map = (Map)env.getMapValueFromParameter(por,this);
        ExcelWriter writer = new ExcelWriter((String)map.get("url"));
        return writer;
    }

    @Override
    public List<Map<String, Object>> query(String tradeId,String file, String[] queryFields, List<Condition> fieldValues, Map<String, String> outs, int start, int end, com.octopus.utils.ds.TableBean tb) throws Exception {
        ExcelReader r = getReader();
        List<Map<String,String>> data = r.getSheepData(file);
        return findRows(data,queryFields,fieldValues);
    }
    List<Map<String, Object>> findRows(List<Map<String,String>> data,String[] queryFields,List<Condition> fieldValues)throws Exception{
        if(null != data){
            List ret =new LinkedList();

            for (Map d : data) {
                if(null != fieldValues) {
                    boolean b=true;
                    for (Condition c : fieldValues) {
                        if("^Non".equals(c.getValues()))continue;
                        if(c.getOp().equals(Condition.OP_EQUAL)){
                            if(!c.getValues().equals(d.get(c.fieldName))){
                                b=false;
                                break;
                            }
                        }else if(c.getOp().equals(Condition.OP_LIKE)){
                            if(!((String)c.getValues()).contains((String) d.get(c.fieldName))){
                                b=false;
                                break;
                            }
                        }else{
                            throw new Exception("not support the Excel operate");
                        }
                    }
                    if(b){
                        Map m = getFieldValue(d,queryFields);
                        if(null!= m && m.size()>0) {
                            ret.add(m);
                        }
                    }
                }else {
                    Map m = getFieldValue(d, queryFields);
                    if(null != m && m.size()>0) {
                        ret.add(m);
                    }
                }
            }
            return ret;
        }
        return null;
    }
    public Map getFieldValue(Map m,String[] fs){
        if(null == fs || fs.length==0)return m;
        HashMap map = new HashMap();
        for(String s:fs){
            map.put(s, m.get(s));

        }
        return map;
    }

    @Override
    public int getCount(String tradeId,String file, String[] queryFields, List<Condition> fieldValues, com.octopus.utils.ds.TableBean tb) throws Exception {
        throw new Exception("not support the Excel operate [getCount]");
    }

    @Override
    public List<Map<String, String>> queryAsString(String tradeId,String file, String[] queryFields, List<Condition> fieldValues, Map<String, String> outs, int start, int end, com.octopus.utils.ds.TableBean tb) throws Exception {
        throw new Exception("not support the Excel operate [getCount]");
    }

    @Override
    public List<Map<String, Object>> query(String tradeId,String sql, Map map, int start, int end) throws Exception {
        return null;
    }

    @Override
    public Object addRecord(XMLParameter env, String tradeId, String taskId, String file, Map fieldValues) throws Exception {
        ExcelWriter w = getWriter(env);
        w.getSheet(file).append(fieldValues);
        w.save();
        return true;
    }

    @Override
    public boolean addRecords(XMLParameter env, String tradeId, String taskId, String file, List fieldValues) throws Exception {
        ExcelWriter w = getWriter(env);
        w.getSheet(file).append(fieldValues);
        w.save();
        return true;
    }

    @Override
    public boolean insertRecord(XMLParameter env, String tradeId, String taskId, String file, Map<String, Object> fieldValues, int insertPosition) throws Exception {
        throw new Exception("not support the Excel operate [insertRecord]");
    }

    @Override
    public boolean insertRecords(XMLParameter env, String tradeId, String taskId, String file, List<Map<String, Object>> fieldValues, int insertPosition) throws Exception {
        throw new Exception("not support the Excel operate [insertRecords]");
    }

    @Override
    public boolean delete(XMLParameter env, String tradeId, String taskId, String file, List<Condition> fieldValues, com.octopus.utils.ds.TableBean tb) throws Exception {
        ExcelWriter w = getWriter(env);
        boolean ret = w.getSheet(file).remove(fieldValues);
        w.save();
        return ret;
    }

    @Override
    public boolean update(XMLParameter env, String tradeId, String taskId, String file, List<Condition> fieldValues, Map<String, Object> updateData,TableBean tb) throws Exception {
        ExcelWriter w = getWriter(env);
        w.getSheet(file).update(fieldValues,updateData);
        w.save();
        return true;
    }

    @Override
    public IDataSource getDataSource(String name) {
        return null;
    }

    @Override
    public boolean exist(String tradeId,String tableName) throws Exception {
        return true;
    }

    @Override
    public long getNextSequence(String name) throws Exception {
        return 0;
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(null != input){
            String op = (String)input.get("op");
            if(StringUtils.isNotBlank(op)){
                String table = (String)input.get("table");
                Object data = input.get("datas");
                Map conds = (Map)input.get("conds");
                ArrayList<Condition> cds = new ArrayList<Condition>();
                if(null != conds) {
                    Iterator<String> its = ((Map) conds).keySet().iterator();
                    while (its.hasNext()) {
                        String f = its.next();
                        if (StringUtils.isNotBlank(f)) {
                            Condition cd = Condition.createCondition(null,f, ((Map) conds).get(f));
                            cds.add(cd);
                        }
                    }
                }
                if("add".equals(op)){
                    if(StringUtils.isNotBlank(table)){
                        if(data instanceof List) {
                            addRecords(env, null, null, table, (List) data);
                        }else if(data instanceof Map){
                            addRecord(env,null,null,table,(Map)data);
                        }
                    }
                    return true;
                }else if("query".equals(op)){
                    List<String> fields = (List)input.get("fields");
                    if(null != fields) {
                        return query(null!=env?env.getTradeId():null,table, fields.toArray(new String[0]), cds, null, 0, 0,null);
                    }else{
                        return query(null!=env?env.getTradeId():null,table, null, cds, null, 0, 0,null);
                    }
                }else if("delete".equals(op)){
                    return delete(env,null,null,table,cds,null);
                }else if("exist".equals(op)){
                    return exist(null!=env?env.getTradeId():null,table);
                }else if("update".equals(op)){
                    return update(env,null,null,table,cds,(Map)data,null);
                }
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
}
