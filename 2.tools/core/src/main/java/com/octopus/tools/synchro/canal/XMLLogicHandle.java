package com.octopus.tools.synchro.canal;

import com.alibaba.otter.canal.protocol.CanalEntry;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.XMLParameter;
import com.octopus.utils.xml.auto.logic.XMLLogic;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * User: wfgao_000
 * Date: 15-11-21
 * Time: 下午9:21
 */
public class XMLLogicHandle extends XMLLogic implements ICanalMessageHandler {
    boolean issql=false;
    boolean istransaction=false;
    static transient Log log = LogFactory.getLog(XMLLogicHandle.class);
    boolean isshowtype = true;
    boolean nottrim = false;
    public XMLLogicHandle(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        if(null != getXML() && getXML().getProperties().containsKey("config") && StringUtils.isNotBlank(getXML().getProperties().getProperty("config"))){
            Map map = StringUtils.convert2MapJSONObject(getXML().getProperties().getProperty("config"));
            if(null != map){
                if("SQL".equalsIgnoreCase((String) map.get("return"))) {
                    issql = true;
                }
                if("transaction".equalsIgnoreCase((String) map.get("return"))) {
                    istransaction = true;
                }
                if(null != map.get("showtype") && StringUtils.isNotBlank(map.get("showtype")) && !StringUtils.isTrue((String)map.get("showtype"))) {
                    isshowtype = false;
                }
                if(null != map.get("nottrim") && StringUtils.isNotBlank(map.get("nottrim")) && StringUtils.isTrue((String)map.get("nottrim"))) {
                    nottrim = true;
                }
            }

        }
    }
    @Override
    public void handle(XMLParameter env,List<CanalEntry.Entry> entrys) throws Exception {
        log.debug("----receive mysql binlog entrys");
        HashMap transmap = null;
        Map splitTables = (Map)env.get("${canal_split_tables}");
        for (CanalEntry.Entry entry : entrys) {
            if (entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONBEGIN) {
                continue;
            }
            try {

                CanalEntry.RowChange rowChage = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
                CanalEntry.EventType eventType = rowChage.getEventType();
                Map oldData = new LinkedHashMap();
                Map newData = new LinkedHashMap();
                Map update_chg = new LinkedHashMap();
                Map update_unchg = new LinkedHashMap();
                for (CanalEntry.RowData rowData : rowChage.getRowDatasList()) {

                    if (eventType == CanalEntry.EventType.DELETE) {
                        log.debug("----deal binlog data DELETE");
                        for (CanalEntry.Column column : rowData.getBeforeColumnsList()) {
                            if(!column.getIsNull()) {
                                if(nottrim){
                                    oldData.put(column.getName().toUpperCase(), column.getValue());
                                }else {
                                    oldData.put(column.getName().toUpperCase(), StringUtils.trimNoSeeChar(column.getValue()));
                                }
                            }else{
                                oldData.put(column.getName().toUpperCase(),null);
                            }
                            if(isshowtype) {
                                oldData.put(column.getName().toUpperCase() + "#Type", getType(column.getSqlType()));
                            }
                        }
                    } else if (eventType == CanalEntry.EventType.INSERT) {
                        log.debug("----deal binlog data INSERT");
                        for (CanalEntry.Column column : rowData.getAfterColumnsList()) {
                            if(!column.getIsNull()){
                                if(nottrim){
                                    newData.put(column.getName().toUpperCase(), column.getValue());
                                }else {
                                    newData.put(column.getName().toUpperCase(), StringUtils.trimNoSeeChar(column.getValue()));
                                }
                            }else{
                                newData.put(column.getName().toUpperCase(),null);
                            }
                            if(isshowtype) {
                                newData.put(column.getName().toUpperCase() + "#Type", getType(column.getSqlType()));
                            }
                        }
                    } else {
                        log.debug("----deal binlog data UPDATE");
                        for (CanalEntry.Column column : rowData.getBeforeColumnsList()) {
                            if(!column.getIsNull()) {
                                if(nottrim){
                                    oldData.put(column.getName().toUpperCase(), column.getValue());
                                }else {
                                    oldData.put(column.getName().toUpperCase(), StringUtils.trimNoSeeChar(column.getValue()));
                                }
                            }else{
                                oldData.put(column.getName().toUpperCase(),null);
                            }
                            if(isshowtype) {
                                oldData.put(column.getName().toUpperCase() + "#Type", getType(column.getSqlType()));
                            }
                        }
                        for (CanalEntry.Column column : rowData.getAfterColumnsList()) {
                            String key = column.getName().toUpperCase();
                            if(!column.getIsNull()) {
                                if(nottrim){
                                    newData.put(key, column.getValue());
                                }else {
                                    newData.put(key, StringUtils.trimNoSeeChar(column.getValue()));
                                }
                            }else{
                                newData.put(key,null);
                            }
                            if((newData.get(key)==null && oldData.get(key)==null) || (null != newData.get(key) && oldData.get(key)!=null && newData.get(key).equals(oldData.get(key)))){
                                update_unchg.put(key,newData.get(key));
                            }else{
                                update_chg.put(key,newData.get(key));
                            }
                            if(isshowtype) {
                                newData.put(column.getName().toUpperCase() + "#Type", getType(column.getSqlType()));
                            }
                        }
                    }
                    if(!istransaction) {
                        XMLParameter par = new XMLParameter();
                        par.addParameter("${op}", eventType.toString());
                        par.addParameter("${schema}", entry.getHeader().getSchemaName());
                        par.addParameter("${table}", entry.getHeader().getTableName().toUpperCase());
                        if (null != splitTables){
                            String tab = (String)splitTables.get(entry.getHeader().getSchemaName().toUpperCase()+"."+entry.getHeader().getTableName().toUpperCase());
                            if(tab.contains(".")){
                                tab = tab.substring(tab.indexOf(".")+1);
                            }
                            par.addParameter("${originalTable}", tab);
                        }
                        if (issql) {
                            par.addParameter("${sql}", convertSql(eventType.toString(), entry.getHeader().getTableName().toUpperCase(), oldData, newData));
                        } else {
                            par.addParameter("${olddata}", oldData);
                            par.addParameter("${newdata}", newData);
                            par.addParameter("${data}", newData);
                            par.addParameter("${update_chg}", update_chg);
                            par.addParameter("${update_unchg}", update_unchg);
                        }
                        if (log.isDebugEnabled())
                            log.debug(entry.getHeader().getSchemaName() + " " + eventType.toString() + " " + entry.getHeader().getTableName());
                        doThing(par, null);
                    }else{
                        if(null == transmap) transmap = new HashMap();
                        transmap.put(entry.getHeader().getTableName().toUpperCase()+".old",oldData);
                        transmap.put(entry.getHeader().getTableName().toUpperCase()+".new",newData);
                        transmap.put(entry.getHeader().getTableName().toUpperCase()+".op",eventType.toString());
                    }
                }
            } catch (Exception e) {
                log.error(e);
                throw new RuntimeException("ERROR ## parser of eromanga-event has an error , data:" + entry.toString(),
                        e);
            }
            if(entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONEND){
                if(istransaction){
                    XMLParameter par = new XMLParameter();
                    par.put("${data}",transmap);
                    doThing(par, null);
                    transmap.clear();

                }
            }
        }


    }
    String getType(int sqltype){
        if(sqltype==12 || sqltype==2005 || sqltype==1){
            return "varchar";
        }

        return "";
    }
    String convertSql(String op,String table,Map oldData,Map newData){
        if("INSERT".equals(op)){
            StringBuffer sb = new StringBuffer("insert into "+table);
            Iterator<String> its = newData.keySet().iterator();
            sb.append("(");
            int n=0;
            StringBuffer que = new StringBuffer();
            while(its.hasNext()){
                String f = its.next();
                if(f.endsWith("#Type"))
                    continue;
                if(n==0){
                    sb.append("`").append(f).append("`");
                    que.append(newData.get(f));
                }else{
                    sb.append(",").append("`").append(f).append("`");
                    que.append(",").append(getValue(newData.get(f),newData.get(f+"#Type")));
                }
                n++;
            }
            sb.append(")").append(" values (").append(que.toString()).append(")");
            sb.append(";");
            return sb.toString();
        }else if("UPDATE".equals(op)){
            StringBuffer sb = new StringBuffer("update "+table +" set ");
            Iterator<String> its = newData.keySet().iterator();
            boolean isbegin=true;
            while(its.hasNext()){
                String f = its.next();
                if(f.endsWith("#Type"))
                    continue;
                if(isbegin) {
                    sb.append("`").append(f).append("`").append("=").append(getValue(newData.get(f),newData.get(f+"#Type")));
                    isbegin=false;
                }else{
                    sb.append(" , ").append(f).append("=").append(getValue(newData.get(f),newData.get(f+"#Type")));
                }
            }
            sb.append(" where ");
            its = oldData.keySet().iterator();
            isbegin=true;
            while(its.hasNext()){
                String f = its.next();
                if(f.endsWith("#Type"))
                    continue;
                if(isbegin) {
                    sb.append("`").append(f).append("`").append("=").append(getValue(oldData.get(f),oldData.get(f+"#Type")));
                    isbegin=false;
                }else{
                    sb.append(" and ").append(f).append("=").append(getValue(oldData.get(f),oldData.get(f+"#Type")));
                }
            }
            sb.append(";");
            return sb.toString();
        }else if("DELETE".equals(op)){
            StringBuffer sb = new StringBuffer("delete from "+table +" where ");
            Iterator<String> its = oldData.keySet().iterator();
            boolean isbegin=true;
            while(its.hasNext()){
                String f = its.next();
                if(f.endsWith("#Type"))
                    continue;
                if(isbegin) {
                    sb.append("`").append(f).append("`").append("=").append(getValue(oldData.get(f),oldData.get(f+"#Type")));
                    isbegin=false;
                }else{
                    sb.append(" and ").append(f).append("=").append(getValue(oldData.get(f),oldData.get(f+"#Type")));
                }
            }
            sb.append(";");
            return sb.toString();
        }
        return null;
    }
    String getValue(Object o,Object type){
        if(type.equals("varchar")){
            return "'"+o.toString()+"'" ;
        }else{
            return o.toString();
        }
    }
}
