package com.octopus.tools.dataclient.v2;

import com.octopus.tools.dataclient.dataquery.AngleQuery;
import com.octopus.tools.dataclient.dataquery.AngleQueryObject;
import com.octopus.tools.dataclient.v2.ds.RouteResultBean;
import com.octopus.utils.ds.TableBean;
import com.octopus.tools.dataclient.v2.ds.TableContainer;
import com.octopus.tools.dataclient.v2.ds.TableRouterBean;
import com.octopus.tools.synchro.canal.impl.DefaultTableNameSplit;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.*;

/**
 * 根据传入的数据找到操作的目标表或文件
 * User: wfgao_000
 * Date: 15-12-4
 * Time: 下午6:22
 */
public class DataRouter extends XMLObject {
    TableContainer tablecontainer;
    AngleQueryObject anglequery;
    public DataRouter(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
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

    /*
    先根据routeExpress筛选出可以操作的datasource，在根据splitExpress 获取具体的表名称
    目前支持:
       1.input中有表名称的
          1.1 单表查询 input={op,table,fields,conds,formate}
          1.2 单表增加 input={op,table,datas}
          1.2 单表修改 input={op,table,conds,datas}
          1.2 单表删除 input={op,table,conds}
       2.根据字段匹配
          2.1 单表查询 input={op,fields,conds,formate}
          2.2 单表新增 input={op,datas}
          2.2 单表修改 input={op,conds,datas}
          2.2 单表删除 input={op,conds}
       3. 多表操作 field 中有表名称 ，多表查询需要用到AngleQuery模块，查询表之间的关系。
          3.1 多表查询  input={op,fields,conds,formate}
          3.1 多表增加  input={op,datas}
          3.1 多表修改  input={op,conds,datas}
          3.1 多表删除  input={op,conds}
     */
    public List<RouteResultBean> getTableRouteResults(XMLParameter env,Map input,Map config)throws Exception{
        List<RouteResultBean> ret=null;
        if(input.containsKey("table") && StringUtils.isNotBlank(input.get("table"))){
            ret = getInsSingleTable(env,(String)input.get("ds"),(String)input.get("table"),(String)input.get("op"),(List)input.get("fields"),input.get("datas"),(Map)input.get("conds"),(Map)input.get("format"),(List)input.get("keyfields"),input.get("start"),input.get("end"),(Boolean)input.get("isforcedb"),(List)input.get("structure"));
        }else{
            String[] fs = getMatchFields((List)input.get("fields"),input.get("datas"));
            if(null != fs){
                String table= tablecontainer.getTableNameByFields(fs);
                if(StringUtils.isBlank(table))
                    throw new Exception("not find tableName ");
                return getInsSingleTable(env,null,table,(String)input.get("op"),(List)input.get("fields"),input.get("datas"),(Map)input.get("conds"),(Map)input.get("format"),(List)input.get("keyfields"),input.get("start"),input.get("end"),(Boolean)input.get("isforcedb"),(List)input.get("structure"));
            }else if(((StringUtils.isNotBlank(input.get("ds"))) || (StringUtils.isNotBlank(getDefaultDataSource()))) && null !=input.get("sqls")){
                String ds =  (String)input.get("ds");
                if(StringUtils.isBlank(ds)){
                    ds = getDefaultDataSource();
                }
                RouteResultBean rr = new RouteResultBean();
                rr.setDataSource(ds);
                rr.setSqls((List)input.get("sqls"));
                List<RouteResultBean> rs = new ArrayList();
                rs.add(rr);
                return rs;
            }else{
                //multi table
                if(null != anglequery && anglequery.isAngleQuery(input)){// is Angle Query
                    return getAngleRouterBean(env,input,config);
                }else {
                    //multi table ops
                    Boolean b = (Boolean)input.get("isforcedb");
                    if(null == b){
                        b=Boolean.FALSE;
                    }
                    ret = getInsMultiTable(env, (String) input.get("op"), (List) input.get("fields"), input.get("datas"), (Map) input.get("conds"), (Map) input.get("format"), input.get("start"), input.get("end"),b,(List)input.get("structure"));
                }
            }
        }
        if(null != ret && ret.size()>0)
            return ret;
        throw new Exception("not route data source by :"+input);

    }
    String getDefaultDataSource(){
        return getXML().getParent().getProperties().getProperty("base");
    }
    public List<RouteResultBean> getAngleRouterBean(XMLParameter env,Map input,Map config){
        List<RouteResultBean> ret = new ArrayList<RouteResultBean>();
        RouteResultBean r = new RouteResultBean();
        r.setConds((Map) input.get("conds"));
        r.setDatas(input.get("datas"));
        r.setOp((String) input.get("op"));
        r.setQueryFields((List) input.get("fields"));
        r.setFormat((Map) input.get("format"));
        r.setOriginalTableName((String) input.get("table"));
        String start =  (String)input.get("start");
        String end =  (String)input.get("end");
        if(null != start && StringUtils.isNotBlank(start))
            r.setStart(Integer.valueOf((String)start));
        if(null != end && StringUtils.isNotBlank(end))
            r.setEnd(Integer.valueOf((String)end));
        Map<String,String> mapping = getAngleMapping((Map) input.get("conds"),(List) input.get("fields"));
        r.setFieldsMapping(mapping);
        ret.add(r);
        return ret;
    }

    /**
     * 根据原始表名称获取所有分库分表的信息
     * @param orginalTableName
     * @return
     */
    public List<RouteResultBean> getAllStoreByTableName(String orginalTableName)throws Exception{
        List<TableRouterBean> ls = tablecontainer.getTableRouters().get(orginalTableName);
        if(null != ls){
            List<RouteResultBean> ret = new ArrayList<RouteResultBean>();
            for(TableRouterBean b:ls){

                //String re = b.getRouteExpress();
                String st = b.getSplitExpress();
                List<String> rts = parseAllTable(orginalTableName,st,b.getSplitRange());
                if(null != rts) {
                    for(String rt:rts) {
                        RouteResultBean r = new RouteResultBean();
                        r.setDataSource(b.getDataSource());
                        r.setTableName(rt);
                        r.setOriginalTableName(orginalTableName);
                        ret.add(r);
                    }
                }

            }
            return ret;
        }
        return null;
    }
    List<String> parseAllTable(String orgTable,String splitExpress,String splitValueRange)throws Exception{
        List<String> ret = new ArrayList();
        if(StringUtils.isBlank(splitExpress)){
            ret.add(orgTable);
            return ret;
        }else{
            String[] ss = DefaultTableNameSplit.splitTableName(orgTable,splitValueRange,null,"");
            if(null != ss){
                ret.addAll(Arrays.asList(ss));
                return ret;
            }
            throw new Exception("not support the split table rule ["+splitExpress+"] or SPLIT_RANGE ["+splitValueRange+"] is null now ");
        }
    }

    public static void main(String[] args){
        try {
            String[] ss = DefaultTableNameSplit.splitTableName("ww", "20120101-20201231", null, "");
            System.out.println(Arrays.asList(ss));
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    /**
     * @param cond
     * @param field
     * @return Map<String,String>
     */
    Map<String,String> getAngleMapping(Map cond,List<String> field){
        List<String> tables = new ArrayList();
        if(null != cond){
            Iterator<String> its = cond.keySet().iterator();
            while(its.hasNext()){
                String k = its.next();
                Object v = cond.get(k);
                if(k.contains(".")){
                    String[] t = k.split("\\.");
                    if(!tables.contains(t[0])){
                        tables.add(t[0]);
                    }
                    if(v instanceof String && ((String)v).contains(".")){
                        String[] vs = ((String)v).split("\\.");
                        if(tablecontainer.getTableRelation().containsKey(vs[0]) && tablecontainer.getTableRelation().get(vs[0]).containsKey(vs[1])){
                            if(!tables.contains(vs[0])) {
                                tables.add(vs[0]);
                            }
                        }
                    }

                }
            }
        }
        if(null != field){
            for(String s:field){
                if(s.contains(".")){
                    String[] ss =s.split("\\.");
                    if(!tables.contains(ss[0])){
                        tables.add(ss[0]);
                    }
                }
            }

        }

        Map<String,String> map = getOneWayRelation(tables);
        if(null != map && map.size()>0){
            return map;
        }

        return null;
    }
    Map<String,String> getOneWayRelation(List<String> tables){
        Map<String,String> ret = new HashMap();
        if(null != tables) {
            for(int i=0;i<tables.size();i++) {
                Iterator<String> its = tablecontainer.getTableRelation().get(tables.get(i)).keySet().iterator();
                while(its.hasNext()){
                    String f = its.next();
                    for(int j=i+1;j<tables.size();j++){
                        List<String[]> rs = tablecontainer.getTableRelation().get(tables.get(i)).get(f);
                        for(String[] r:rs){
                           if(r[0].equalsIgnoreCase(tables.get(j))){
                               ret.put(tables.get(i).concat(".").concat(f), r[0].concat(".").concat(r[1]));
                           }
                        }
                    }
                }

            }
        }
        return ret;
    }
    String[] getMatchFields(List<String> fields,Object datas){
        if(null != datas){
            if(datas instanceof List){
                Iterator<String> it = ((Map)((List)datas).get(0)).keySet().iterator();
                while(it.hasNext()){
                    if(it.next().contains("."))
                        return null;
                    else
                        break;
                }
                return (String[])((Map)((List)datas).get(0)).keySet().toArray(new String[0]);
            }else{
                Iterator<String> it = ((Map)datas).keySet().iterator();
                while(it.hasNext()){
                    if(it.next().contains("."))
                        return null;
                    else
                        break;
                }
                return (String[])((Map)datas).keySet().toArray(new String[0]);
            }
        }
        if(null != fields && !fields.get(0).contains(".")){
            return (String[])fields.toArray(new String[0]);
        }
        return null;
    }
    List<RouteResultBean> getInsMultiTable(XMLParameter env,String op,List<String> fields,Object datas,Map conds,Map format,Object start,Object end,boolean isforcedb,List<Map> structure)throws Exception{
        Map<String,List<String>> tableFields = new HashMap<String, List<String>>();
        String[] tables=null;
        //get tables name from fields info. field format is TABLE_NAME.FIELD_NAME
        if(null != fields){
            Iterator its = fields.iterator();
            String[] tf=null;
            while(its.hasNext()){
                tf = StringUtils.split((String)its.next(), AngleQuery.TABLE_FIELD_SPLIT);
                if(!tableFields.containsKey(tf[0])) tableFields.put(tf[0],new ArrayList<String>());
                tableFields.get(tf[0]).add(tf[1]);
            }
            tables = tableFields.keySet().toArray(new String[0]);
        }
        //multi table data, will in one map. key is TABLE_NAME.FIELD_NAME
        Map<String,Map> tableData=new HashMap();
        if(null != datas){
            Map first =null;
            if(datas instanceof Map){
                first=(Map)datas;
            }
            if(datas instanceof List){
                first=(Map)((List)datas).get(0);
            }
            if(null != first){
                Iterator its = first.keySet().iterator();
                String[] tf=null;
                while(its.hasNext()){
                    String k = (String)its.next();
                    tf = StringUtils.split(k, AngleQuery.TABLE_FIELD_SPLIT);
                    if(!tableData.containsKey(tf[0])) tableData.put(tf[0],new HashMap());
                    tableData.get(tf[0]).put(tf[1],first.get(k));
                }
            }
        }


        if("add".equals(op)){
            return getInsMultiTableByFieldData(env,op, tableData.keySet().toArray(new String[0]),null,tableData,null,format,start,end,isforcedb,structure);
        }else if("update".equals(op)){
            return getInsMultiTableByFieldData(env,op,tableData.keySet().toArray(new String[0]),tableFields,tableData, conds,format,start,end,isforcedb,structure);
        }else if("delete".equals(op)){
            return getInsMultiTableByFieldData(env,op,tableFields.keySet().toArray(new String[0]),tableFields,null, conds,format,start,end,isforcedb,structure);
        }else if("query".equals(op)){
            return getInsMultiTableByFieldData(env,op,tables,tableFields,null, conds,format,start,end,isforcedb,structure);
        }else if("migration".equals(op)){
            return null;
        }
        throw new Exception("not support the op["+op+"] "+tableData);
    }
    Map<String, String> filterTableRelate(Map<String, Map<String, List<String[]>>> rels,String[] ts){
        Map<String, String> ret = new HashMap<String, String>();
        if(null != ts && ts.length>0) {
            for (String t : ts) {
                Map<String, List<String[]>> fp = rels.get(t);
                if (null != fp) {
                    Iterator<String> its = fp.keySet().iterator();
                    while (its.hasNext()) {
                        String f = its.next();
                        List<String[]> rts = fp.get(f);
                        for (String[] rt : rts) {
                            if (ArrayUtils.isInStringArray(ts, rt[0])) {
                                ret.put(t.concat(AngleQuery.TABLE_FIELD_SPLIT).concat(f), rt[0].concat(AngleQuery.TABLE_FIELD_SPLIT).concat(rt[1]));
                            }
                        }
                    }
                }
            }
        }
        if(ret.size()>0)
            return ret;
        return null;
    }
    List<RouteResultBean> getInsMultiTableByFieldData(XMLParameter env,String op,String[] tables,Map<String,List<String>> tableFields
            ,Map<String,Map> tableData,Map conds,Map format,Object start,Object end,boolean isforcedb,List<Map> structure)throws Exception{
        // table ,cnd

        Map<String,String> fieldmapping = filterTableRelate(tablecontainer.getTableRelation(),tables);

        Map<String,Map> tem = getRelTableAndCnd(tables,fieldmapping,conds);

        if(null != tem){
            List<RouteResultBean> ret = new ArrayList<RouteResultBean>();

            Iterator its = tem.keySet().iterator();
            while(its.hasNext()){
                String t= (String)its.next();
                List fs = null;
                if(null !=tableFields){
                    fs = tableFields.get(t);
                }
                List<RouteResultBean> rt  = getInsSingleTable(env,null,t,op,fs,tableData.get(t),tem.get(t),format,null,start,end,isforcedb,structure);
                if(null != rt && rt.size()>0){
                    ret.addAll(rt);
                }
            }
            if(ret.size()>0)
                return ret;
        }
        throw new Exception("not parse multi table info");
    }
    //  table   conds
    Map<String,Map> getRelTableAndCnd(String[] tables,Map<String, String> rels,Map conds)throws Exception{
        if(null !=anglequery) {
            Map<String, Collection<String>> tablePs = anglequery.findDoTablePks(tables, rels, conds);
            if (null != tablePs) {
                Map<String, Map> ret = new HashMap<String, Map>();
                Iterator<String> is = tablePs.keySet().iterator();
                while (is.hasNext()) {
                    String s = is.next();
                    Map m = new HashMap();
                    m.put(tablecontainer.getAllTables().get(s).getPkField().getFieldCode(), tablePs.get(s));
                    ret.put(s, m);
                }
                if (ret.size() > 0)
                    return ret;
                return null;
            }else{
                Map map = new HashMap();
                for(String s:tables){
                    if(null == rels) {
                        if (null != conds) {
                            map.put(s, conds);
                        } else {
                            map.put(s, new HashMap());
                        }
                    }
                }
                if(map.size()>0){
                    return map;
                }
            }
        }else {
            if(null !=tables) {
                Map map = new HashMap();
                for(String s:tables){
                    if(null == rels) {
                        if (null != conds) {
                            map.put(s, conds);
                        } else {
                            map.put(s, new HashMap());
                        }
                    }
                }
                if(map.size()>0){
                    return map;
                }
            }
        }
        return null;
    }
    List<RouteResultBean> getInsSingleTable(XMLParameter env,String ds,String table,String op,List fields,Object datas,Map conds,Map format,List<String> keyfields,Object start,Object end,Boolean isforcedb,List<Map> structure)throws Exception{
        List<RouteResultBean> ret = new ArrayList();
        //单表操作
        List<String[]> rs = getInsTable(env, table, op, datas, conds);
        if(null != rs){
            for(String[] s:rs){
                RouteResultBean r = new RouteResultBean();
                r.setConds(conds);
                r.setDatas(datas);
                r.setOp(op);
                r.setKeyfields(keyfields);
                r.setQueryFields(fields);
                r.setFormat(format);
                r.setForceDB(isforcedb);
                r.setStructure(structure);
                r.setOriginalTableName(table);
                if(null != start && StringUtils.isNotBlank(start))
                r.setStart(Integer.valueOf((String)start));
                if(null != end && StringUtils.isNotBlank(end))
                    r.setEnd(Integer.valueOf((String)end));
                if(StringUtils.isNotBlank(ds)){
                    r.setDataSource(ds);
                }else{
                    if(s.length>0 && StringUtils.isNotBlank(s[0]))
                        r.setDataSource(s[0]);
                    else
                        r.setDataSource(getXML().getParent().getProperties().getProperty("base"));
                }
                if(s.length>1 && StringUtils.isNotBlank(s[1]))
                    r.setTableName(s[1]);
                else
                    r.setTableName(table);
                ret.add(r);
            }
        }
        if(ret.size()==0){
            RouteResultBean r = new RouteResultBean();
            r.setConds(conds);
            r.setDatas(datas);
            r.setOp(op);
            r.setQueryFields(fields);
            r.setFormat(format);
            r.setKeyfields(keyfields);
            r.setStructure(structure);
            if(null != start && StringUtils.isNotBlank(start))
            r.setStart(Integer.valueOf((String)start));
            if(null != end && StringUtils.isNotBlank(end))
                r.setEnd(Integer.valueOf((String)end));
            if(StringUtils.isBlank(r.getTableName()))
                r.setTableName(table);
            if(StringUtils.isNotBlank(ds))
                r.setDataSource(ds);
            else
                if(StringUtils.isBlank(r.getDataSource()))
                    r.setDataSource(getXML().getParent().getProperties().getProperty("base"));
            ret.add(r);
        }
        if(ret.size()>0)
            return ret;
        return null;
    }
    //single table
    List<String[]> getInsTable(XMLParameter env,String table ,String op,Object data,Map cond)throws Exception{
        if("add".equals(op)){
            //single add
            if(data instanceof Map){
                return getInsTableByFieldData(env, table, (Map) data);
            }
            //batch add
            if(data instanceof List){
                return getInsTableByFieldData(env, table, (Map) ((List) data).get(0));
            }
            if(data.getClass().isArray()){
                return getInsTableByFieldData(env, table, (Map) ((Object[]) data)[0]);
            }
        }else{
            return getInsTableByFieldData(env, table, cond);
        }
        throw new Exception("not support the op["+op+"]"+data.getClass().getName()+"\n"+data);
    }

    /**
     * 根据RouteExpress获取数据库配置信息，根据splitExpress获取分表名称
     * @param env
     * @param table
     * @param data
     * @return
     * @throws Exception
     */
    public List<String[]> getInsTableByFieldData(XMLParameter env,String table,Map data)throws Exception{
        List<TableRouterBean> trs = tablecontainer.getTableRouters().get(table);
        if(null != trs){
            List ret = new ArrayList();
            if(null == env)
                env = new XMLParameter();
            try{
                env.addParameter("${data}",data);
                env.addParameter("${table}",table);
                for(TableRouterBean r:trs){

                    if ((r.getTableName().equalsIgnoreCase(table) && StringUtils.isBlank(r.getRouteExpress() ))
                            || (StringUtils.isNotBlank(r.getRouteExpress()) && ObjectUtils.isTrue(env.getExpressValueFromMap(r.getRouteExpress(),this)) ) ) {
                        //get split table name
                        if(StringUtils.isNotBlank(r.getSplitExpress())) {
                            String tn = getRealTableName(r.getTableName(), r.getSplitExpress(), env, data);
                            if (StringUtils.isNotBlank(tn)) {
                                ret.add(new String[]{r.getDataSource(), tn});
                                //delete condition
                                if (null != env.getParameter("${this_input}")) {
                                    Map m = ((Map) ((Map) env.getParameter("${this_input}")).get("conds"));
                                    if (null != m) {
                                        Iterator<String> its = m.keySet().iterator();
                                        while (its.hasNext()) {
                                            String k = its.next();
                                            if (null != r.getSplitExpress() && r.getSplitExpress().contains(k) && m.get(k) instanceof Map &&
                                                    null != ((Map) m.get(k)).get("M")
                                                    && ((String) ((Map) m.get(k)).get("M")).endsWith("00:00:00")
                                                    && ((String) ((Map) m.get(k)).get("L")).endsWith("23:59:59")) {
                                                m.remove(k);
                                            }
                                        }
                                    }
                                }
                            } else {
                                throw new Exception("get real table[" + table + "] name error.");
                            }
                        }else{
                            ret.add(new String[]{r.getDataSource(), r.getTableName()});
                        }
                    }
                }
            }finally {
                if(null != env){
                    env.removeParameter("${data}");
                    env.removeParameter("${table}");
                }
            }
            if(ret.size()>0) {
                return ret;
            }
        }
        return null;
    }
    String getRealTableName(String tableName,String splitExpress,XMLParameter env,Map data)throws Exception{
        try{
            env.addParameter("${table}",tableName);
            env.addParameter("${data}",data);
            String tn = (String)env.getExpressValueFromMap(splitExpress,this);
            if(StringUtils.isNotBlank(tn))
                return tn;
            else
                return tableName;
        }finally{
            env.removeParameter("${table}");
            env.removeParameter("${data}");
        }
    }

    public Map<String,TableBean> getDefTables(){
        return tablecontainer.getAllTables();
    }

}
