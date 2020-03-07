package com.octopus.tools.dataclient.v2;

import com.octopus.tools.dataclient.dataquery.AngleQuery;
import com.octopus.tools.dataclient.v2.ds.RouteResultBean;
import com.octopus.utils.ds.Condition;
import com.octopus.utils.ds.TableBean;
import com.octopus.tools.dataclient.v2.ds.TableContainer;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.exception.ISPException;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * User: wfgao_000
 * Date: 15-8-14
 * Time: 上午10:42
 */
public class DataClient2 extends XMLDoObject{
    static transient Log log = LogFactory.getLog(DataClient2.class);
    HashMap<String,IDataSource> datasources;
    ISequence sequence;
    DataRouter router;
    static List splitTables=null;
    XMLDoObject anglequery;
    TableContainer tablecontainer;
    public DataClient2(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    List<RouteResultBean> getRouteResultBean(XMLParameter env,Map input,Map config)throws Exception{
        List<RouteResultBean> rs=new ArrayList();
        if(null != router) {
            rs = router.getTableRouteResults(env, input, config);
        }else {
            rs = new ArrayList<RouteResultBean>();
            RouteResultBean b = new RouteResultBean();
            if(StringUtils.isBlank((String)input.get("ds")))
                b.setDataSource(getXML().getProperties().getProperty("base"));
            else
                b.setDataSource((String)input.get("ds"));
            b.setTableName((String)input.get("table"));
            b.setOp((String)input.get("op"));
            b.setConds(input.get("conds"));
            b.setDatas(input.get("datas"));
            b.setKeyfields((List)input.get("keyfields"));
            if(StringUtils.isNotBlank(input.get("start")))
                b.setStart(Integer.parseInt((String)input.get("start")));
            if(StringUtils.isNotBlank(input.get("end")))
                b.setEnd(Integer.parseInt((String)input.get("end")));
            b.setSqls((List)input.get("sqls"));
            b.setQueryFields((List) input.get("fields"));
            b.setFormat((Map) input.get("format"));
            b.setFieldsMapping((Map)input.get("fieldMapping"));
            b.setStructure((List)input.get("structure"));
            rs.add(b);
        }
        if(rs.size()>0)
            return rs;
        else
            return null;
    }
    @Override
    public Object doSomeThing(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        long l =0;
            if(log.isDebugEnabled()){
            l = System.currentTimeMillis();
        }
        try{
            if(null == splitTables)
                splitTables = new ArrayList();
            if(null != input && "getDS".equals(input.get("op"))){
                XMLMakeup[] ms = getXML().find("datasource");
                List ret = new ArrayList();
                if(null != ms){
                    for(XMLMakeup m:ms){
                        Map t = new HashMap();
                        t.put("ds",m.getProperties().get("key"));
                        t.put("desc",m.getProperties().get("desc"));
                        ret.add(t);
                    }
                    return ret;
                }
                return null;
            }
            if(null != input && "migration".equals(input.get("op"))){
                migration(env,(String)input.get("src_ds"),(String)input.get("src_table"),(List)input.get("sqls"),(String[])input.get("fields"),(Map)input.get("conds"),(String)input.get("target_ds"),(String)input.get("target_table"));
                return true;
            }
            if(null != router && null != input && "getDefTables".equals(input.get("op"))){
                return router.getDefTables();
            }
            List<RouteResultBean> rs = getRouteResultBean(env,input,config);

            if(null != rs){
                Map<String,List<Map<String,String>>> result = new LinkedHashMap<String, List<Map<String, String>>>();
                Map rest = new LinkedHashMap();
                for(RouteResultBean r:rs){
                    HashMap in = new HashMap();
                    in.put("conds",r.getConds());
                    in.put("datas",r.getDatas());
                    in.put("op",r.getOp());
                    in.put("fields",r.getQueryFields());
                    in.put("table",r.getTableName());
                    if(null != r.getTableName()) {
                        TableBean tb= tablecontainer.getAllTables().get(r.getTableName().toUpperCase());
                        in.put("tableDefine",tb);
                    }
                    in.put("start",String.valueOf(r.getStart()));
                    in.put("end",String.valueOf(r.getEnd()));
                    in.put("format",r.getFormat());
                    in.put("sqls",r.getSqls());
                    in.put("isforcedb",r.isForceDB());
                    in.put("keyfields",r.getKeyfields());
                    in.put("fieldMapping",r.getFieldsMapping());
                    in.put("structure",r.getStructure());
                    in.put("^tablecontainer",tablecontainer);
                    HashMap t = new HashMap();
                    t.putAll(in);
                    t.put("op","isAngleQuery");
                    if(null != anglequery) {
                        Boolean isAngleQuery = (Boolean) anglequery.doSomeThing(null, null, t, null, null);
                        if (null != tablecontainer && null != isAngleQuery && isAngleQuery) {
                            return anglequery.doSomeThing(xmlid, env, in, output, config);
                        }
                    }
                    XMLDoObject ds = (XMLDoObject)datasources.get(r.getDataSource());
                    log.debug("used datasource "+r.getDataSource());
                    if(null != ds){
                        if(!"query".equals(r.getOp()) && !"count".equals(r.getOp()) && !splitTables.contains(r.getTableName())){
                            if(!isExist(ds,r.getTableName()) && null !=r.getOriginalTableName() && !r.getOriginalTableName().equals(r.getTableName())){
                                createTable(ds, r.getOriginalTableName(), r.getTableName());
                            }
                            splitTables.add(r.getTableName());
                        }
                        Object ret=null;
                        if("query".equals(r.getOp())){
                            ret = ds.doSomeThing(xmlid,env,in,output,config);
                            if(log.isDebugEnabled()){
                                log.debug("DataClient result\n"+ret);
                            }
                            if (ret instanceof List) {
                                if (!result.containsKey(r.getTableName()))
                                    result.put(r.getTableName(), new LinkedList<Map<String, String>>());
                                result.get(r.getTableName()).addAll((List) ret);
                            }
                        }else{
                            if("createTable".equals(r.getOp()) && null!=in && null==in.get("structure")){
                                TableBean ben = tablecontainer.getAllTables().get(r.getTableName().toUpperCase());
                                if(null != ben) {
                                    in.put("structure",ben.getStructure());
                                }
                            }

                            ret = ds.doSomeThing(xmlid,env,in,output,config);

                        }
                        if(rs.size()==1)
                            return ret;
                        else{
                            rest.put(r.getTableName(),ret);
                        }
                    }else {
                        throw new ISPException("DB.NOT_FIND_DS","DataClient not find datasource [(ds)] by table [(table)]",new String[]{r.getDataSource(),r.getTableName()});
                    }
                }
                if("query".equals(input.get("op"))){
                    AngleQuery.filterResult(result);
                    return AngleQuery.compose(null,result);
                }else{if(rest.size()==0) {
                        return null;
                    }else if(rest.size()==1){
                        return rest.get(rest.keySet().iterator().next());
                    }else{
                        return rest;
                    }
                }

            }else{
                throw new Exception("DataClient not find datasource by "+input);
            }

        }catch (Exception e){
            throw e;
        }finally {
            if(log.isDebugEnabled()){
                log.debug("query ["+input+"] lost:"+(System.currentTimeMillis()-l));
            }
        }
    }

    List<Condition> getCond(XMLParameter env,Map conds)throws Exception{
        if(null != conds){
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
                return cds;
            }
        }
        return null;
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;
    }

    @Override
    public ResultCheck checkReturn(String xmlid,XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
/*
        Map m = env.getTradeConsoles();
        if(null != m && StringUtils.isNotBlank(m.get(xmlid))){
            ((Connection)m.get(xmlid)).commit();
            ((Connection)m.get(xmlid)).close();
        }
*/
        return new ResultCheck(true,ret);
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        if(null != input && ("migration".equals(input.get("op")) ||"getDS".equals(input.get("op")) ||"getDefTables".equals(input.get("op")) )){
            return true;
        }else {
            List<RouteResultBean> rs = getRouteResultBean(env, input, config);
            if (null != rs) {
                if (rs.size() > 0) {
                    for (RouteResultBean r : rs) {
                        XMLDoObject o = (XMLDoObject) datasources.get(r.getDataSource());
                        o.commit(xmlid, env, input, output, config, ret);
                    }
                }
            } else {
                String ds = (String) input.get("ds");
                if (StringUtils.isNotBlank(ds)) {
                    XMLDoObject o = (XMLDoObject) datasources.get(ds);
                    o.commit(xmlid, env, input, output, config, ret);
                }
            }
            return true;
        }
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config,Object ret,Exception e) throws Exception {
        /*Map m = env.getTradeConsoles();
        if(null != m && StringUtils.isNotBlank(m.get(xmlid))){
            ((Connection)m.get(xmlid)).rollback();
            ((Connection)m.get(xmlid)).close();
        }*/
        if(null != input && ("migration".equals(input.get("op")) ||"getDS".equals(input.get("op")) ||"getDefTables".equals(input.get("op")) )){
            return true;
        }else {
            List<RouteResultBean> rs = getRouteResultBean(env, input, config);
            if (null != rs) {
                if (rs.size() > 0) {
                    for (RouteResultBean r : rs) {
                        XMLDoObject o = (XMLDoObject) datasources.get(r.getDataSource());
                        if (null != o) {
                            o.rollback(xmlid, env, input, output, config, ret, e);
                        }
                    }
                }
            } else {
                String ds = (String) input.get("ds");
                if (StringUtils.isNotBlank(ds)) {
                    XMLDoObject o = (XMLDoObject) datasources.get(ds);
                    if (null != o) {
                        o.rollback(xmlid, env, input, output, config, ret, e);
                    }
                }
            }
            return true;
        }
    }
    public boolean isExist(XMLDoObject ds,String name) throws Exception {
        if(null != ds) {
            HashMap input = new HashMap();
            input.put("op", "exist");
            input.put("table", name);
            return (Boolean) ds.doSomeThing(null, null, input, null, null);
        }
        return true;
    }
    public boolean createTable(XMLDoObject ds,String original,String name) throws Exception {
        TableBean tb= router.getDefTables().get(original);
        if(null == tb)
            throw new Exception("not find field define by table name:"+original+" so can not create table:"+name);
        HashMap input = new HashMap();
        input.put("op","createTable");
        input.put("table",name);
        input.put("structure", ObjectUtils.convertBeanList2MapList((List)tb.getTableFields()));
        return (Boolean)ds.doSomeThing(null,null,input,null,null) ;
    }

    void migration(XMLParameter env,String src_ds,String src_table,List<String> sqls,String[] queryFields,Map conds,String target_ds,String target_table){
        try {
            IDataSource src = datasources.get(src_ds);

            IDataSource tar = datasources.get(target_ds);
            int count=0;
            int size=5000;
            int start=0;
            int end=0;
            do {
                end =start+size;
                List<Map<String, Object>> srcdata=null;
                if(null != sqls && sqls.size()>0){
                    srcdata = src.query(null!=env?env.getTradeId():null,sqls.get(0),conds,start,end);
                }else {
                    List<Condition> cds = getCond(env,conds);
                    srcdata = src.query(null!=env?env.getTradeId():null,src_table, queryFields, cds, null, start, end, null);
                }
                if(null != srcdata){
                    count=srcdata.size();
                    tar.addRecords(null,null,null,target_table,srcdata);
                    start =end;
                }else{
                    count=0;
                }

            }while(count>=size);
        }catch (Exception e){
            log.error("",e);
        }
    }
}