package com.octopus.tools.dataclient.v2;

import com.octopus.tools.dataclient.v2.ds.*;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.XMLDoObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * User: wfgao_000
 * Date: 15-9-18
 * Time: 下午6:02
 */
public class DefaultSequence extends XMLObject implements ISequence {

    static ConcurrentHashMap<String, AtomicLong> add = new ConcurrentHashMap();
    static ConcurrentHashMap<String, Long> curmax = new ConcurrentHashMap();
    transient static Log log = LogFactory.getLog(DefaultSequence.class);
    Map fielsMapping = new HashMap();
    String sequenceName=null;
    TableContainer tablecontainer;
    public DefaultSequence(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        String s = xml.getProperties().getProperty("fields");
        if(StringUtils.isNotBlank(s)){
            fielsMapping = StringUtils.convert2MapJSONObject(s);
        }
        sequenceName = xml.getProperties().getProperty("sequenceName");
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
    public long getNextSequence(String seqname) throws Exception{
        if (!(curmax.containsKey(seqname)))
            curmax.put(seqname, new Long(0L));

        synchronized ((Long)curmax.get(seqname)) {
            if ((!(add.containsKey(seqname))) || (((AtomicLong)add.get(seqname)).longValue() <= 0L)) {
                long[] topSeg = getNextSegment(seqname);
                add.put(seqname, new AtomicLong(topSeg[1]));
                curmax.put(seqname, topSeg[0]);
            }
        }
        long curmaxVal = ((Long)curmax.get(seqname)).longValue();
        long next = add.get(seqname).decrementAndGet();

        return curmaxVal - next;

    }



    public long[] getNextSegment(String tableName)throws Exception{
        synchronized (this) {
            try{
                String table = getXML().getProperties().getProperty("table");
                String seqName = "sequence_name";
                String incrementField = "increment";
                String lastField = "last_number";
                if (null != fielsMapping && fielsMapping.size() > 0) {
                    if (fielsMapping.containsKey("sequence_name")) {
                        seqName = (String) fielsMapping.get("sequence_name");
                    }
                    if (fielsMapping.containsKey("increment")) {
                        incrementField = (String) fielsMapping.get("increment");
                    }
                    if (fielsMapping.containsKey("last_number")) {
                        lastField = (String) fielsMapping.get("last_number");
                    }
                }
                String sequence = tableName;
                if (null != sequenceName) {
                    sequence = sequenceName.replaceAll("\\$\\{TABLE_NAME\\}", tableName.toUpperCase());
                }
                //get sequence info
                XMLObject parent = getParent();

                XMLObject obj = getObjectById(parent.getXML().getProperties().getProperty("base"));


                if(obj instanceof DBDataSource) {
                    return getSequenceFromDB(obj,table,incrementField,lastField,seqName,sequence);
                }else if(obj instanceof XMLDoObject){
                    return getSequenceFromXMLDoObject(obj,table,incrementField,lastField,seqName,sequence);
                }
                throw new Exception("not support get sequence "+obj);
            }catch (Exception e) {
                log.error("fetch sequence [" + sequenceName + "] error", e);
                throw e;
            }

        }

    }

    long[] getSequenceFromXMLDoObject(XMLObject obj,String table,String incrementField,String lastField,String seqName,String sequence)throws Exception{
        long increment = 0;
        long last_number = 0;
        long next_number = 0;
        Map input = new HashMap();
        Map cond = new HashMap();
        cond.put(seqName,sequence);
        input.put("conds",cond);
        input.put("op","query");
        List<Map> ret = (List)((XMLDoObject)obj).doSomeThing(null,null,input,null,null);
        if(null != ret){
            if(ret.get(0).get(incrementField) instanceof String) {
                increment = Long.parseLong((String)ret.get(0).get(incrementField));
            }else if(ret.get(0).get(incrementField) instanceof Long){
                increment = (Long)ret.get(0).get(incrementField);
            }
            if(ret.get(0).get(lastField) instanceof String) {
                last_number = Long.parseLong((String)ret.get(0).get(lastField));
            }else if(ret.get(0).get(lastField) instanceof Long){
                increment = (Long)ret.get(0).get(incrementField);
            }
            input.put("op","update");
            input.put("conds",cond);
            Map data = new HashMap();
            data.put(last_number,last_number + increment);
            input.put("datas",data);
            ((XMLDoObject)obj).doSomeThing(null,null,input,null,null);
        }else{
            input.put("op","add");
            Map data = new HashMap();
            data.put(last_number,last_number + increment);
            input.put("datas",data);
            ((XMLDoObject)obj).doSomeThing(null,null,input,null,null);
        }
        if (last_number == 0L)
            next_number = increment;
        else
            next_number = last_number + increment;
        return new long[]{next_number, increment};
    }

    long[] getSequenceFromDB(XMLObject obj,String table,String incrementField,String lastField,String seqName,String sequence)throws Exception{
        Connection conn=null;
        try {
            com.octopus.tools.dataclient.v2.ds.DBDataSource db = (com.octopus.tools.dataclient.v2.ds.DBDataSource) obj;
            conn = db.getConnection(null,null);
            conn.setAutoCommit(false);
            long increment = 0;
            long last_number = 0;
            long next_number = 0;
            PreparedStatement ptmt = conn.prepareStatement("select " + incrementField + "," + lastField + " from " + table + " where " + seqName + "='" + sequence + "' FOR UPDATE");
            ResultSet rs = ptmt.executeQuery();
            if (rs.next()) {
                increment = rs.getInt(1);
                last_number = rs.getLong(2);
            }

            if (increment < 0L) {
                throw new Exception("increment can not be null");
            }

            if (last_number == 0L)
                next_number = increment;
            else
                next_number = last_number + increment;

            ptmt = conn.prepareStatement("UPDATE " + table + " SET " + lastField + "= " + next_number + " WHERE " + seqName + "= '" + sequence + "' ");
            ptmt.executeUpdate();

            return new long[]{next_number, increment};
        }catch (Exception e) {
            log.error("fetch sequence [" + sequenceName + "] error", e);
            throw e;
        }finally {
            if(null != conn){
                conn.commit();
                conn.close();
            }
        }
    }
}
