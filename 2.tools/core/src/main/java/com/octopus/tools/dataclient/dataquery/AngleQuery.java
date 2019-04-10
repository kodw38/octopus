package com.octopus.tools.dataclient.dataquery;

import com.octopus.tools.dataclient.v2.DataClient2;
import com.octopus.tools.dataclient.v2.DefaultSequence;
import com.octopus.tools.dataclient.v2.ISequence;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.SNUtils;
import com.octopus.utils.alone.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import redis.clients.jedis.Jedis;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 目前只支持交集的运算，只支持=和in
 * User: wfgao_000
 * Date: 15-7-22
 * Time: 下午3:41
 */
public class AngleQuery {
    transient static Log log = LogFactory.getLog(AngleQuery.class);
    static String TABLE_FLAG="T";
    static String FIELD_FLAG="F";
    static String INDEX_FLAG="I";
    static String PRIMARY_FLAG="P";
    static String OUTKEY_FLAG="O";
    static String LIKE_FLAG="W";
    public static String TABLE_FIELD_SPLIT=".";
    static AtomicLong tempcount = new AtomicLong();
    static AngleConfig config = null;
    static DataClient2 dc;
    public static String getTempId(){
        return SNUtils.getUUID()+tempcount.incrementAndGet();
    }

    public static void setConfig(AngleConfig config){
        AngleQuery.config = config;
    }
    public static void setDataClient(DataClient2 dc){
        AngleQuery.dc=dc;
    }


    static Map<String,Map<String,List<String>>> convertValues(Map<String,List<String>> fieldValues){
        if(null != fieldValues){
            Map<String,Map<String,List<String>>> tablefieldValues=new HashMap<String, Map<String, List<String>>>();
            Iterator<String> its = fieldValues.keySet().iterator();
            while(its.hasNext()){
                String f = its.next();
                Object o  = fieldValues.get(f);
                List<String> cs=null;
                if(o instanceof List){
                    cs = (List)o;
                }else{
                    cs = new ArrayList();
                    cs.add(ObjectUtils.toString(o));
                }
                if(null != f) {
                    String[] fs = StringUtils.split(f, TABLE_FIELD_SPLIT);
                    if (!tablefieldValues.containsKey(fs[0]))
                        tablefieldValues.put(fs[0], new HashMap<String, List<String>>());
                    if (fs.length>1 && !tablefieldValues.get(fs[0]).containsKey(fs[1]))
                        tablefieldValues.get(fs[0]).put(fs[1], new ArrayList<String>());
                    for (String c : cs) {
                        if (fs.length>1 && !tablefieldValues.get(fs[0]).get(fs[1]).contains(c))
                            tablefieldValues.get(fs[0]).get(fs[1]).add(c);
                    }
                }
            }
            return tablefieldValues;
        }
        return null;
    }
    static  Map<String,List<String>> getPkValues(Map<String,List<String>> fieldValues){
        Map<String,List<String>> pkcond = new HashMap<String,List<String>>();
        if(null != fieldValues){
            Iterator<String> its = fieldValues.keySet().iterator();
            while(its.hasNext()){
                String ik = its.next();
                String[] k=StringUtils.split(ik, TABLE_FIELD_SPLIT);
                if(config.getPk(k[0]).equals(k[1])){
                    if(!pkcond.containsKey(k[0])) pkcond.put(k[0],new ArrayList<String>());
                    pkcond.get(k[0]).addAll(fieldValues.get(ik));
                }
            }
        }
        return pkcond;
    }
    static Map<String, List<String>> getIndexValues(Map<String,List<String>> fieldValues){
        Map<String,List<String>> indexConds = new HashMap<String, List<String>>();
        if(null != fieldValues){
            Iterator<String> its = fieldValues.keySet().iterator();
            while(its.hasNext()){
                String k = its.next();
                List<String> vs = null;
                Object o = fieldValues.get(k);
                if(o instanceof List){
                    vs = (List)o;
                }else{
                    vs = new ArrayList();
                    vs.add(ObjectUtils.toString(o));
                }
                String[] ks=StringUtils.split(k,TABLE_FIELD_SPLIT);
                if(config.getIndexs(ks[0]).contains(ks[1])){
                    if(!indexConds.containsKey(ks[0]))indexConds.put(ks[0],new ArrayList<String>());
                    for(String v:vs){
                        if(v.startsWith("%") && v.endsWith("%")){
                            v = v.substring(1,v.length()-1);
                            String[] ss = AngleLoader.getSplit(config,ks[0],ks[1],v);
                            if(null != ss){
                                for(String si:ss) {
                                    String in =getLikeKey(ks[0], ks[1], si.toLowerCase());
                                    if(!indexConds.get(ks[0]).contains(in))
                                        indexConds.get(ks[0]).add(in);
                                }
                            }

                        }else{
                            String in =getIndexKey(ks[0],ks[1],v);
                            if(!indexConds.get(ks[0]).contains(in))
                                indexConds.get(ks[0]).add(in);
                        }
                    }
                }
            }
        }
        return indexConds;
    }
    static void fillOutkeyValues(Map<String,List<String[]>> mp,Map<String,String> fieldMapping,Map<String,Map<String,List<String>>> outkeyValues,Map<String,Map<String,List<String>>> tablefieldValues){
        if(null != fieldMapping && fieldMapping.size()>0){
            Iterator<String> its = fieldMapping.keySet().iterator();
            while(its.hasNext()){
                String ks = its.next();
                String vs = fieldMapping.get(ks);
                String[] ktf = StringUtils.split(ks, TABLE_FIELD_SPLIT);
                String[] vtf = StringUtils.split(vs,TABLE_FIELD_SPLIT);
                if(null !=mp){
                    appmap(mp,ktf,vtf);
                    appmap(mp,vtf,ktf);
                }
                addRelate(outkeyValues,tablefieldValues,ktf,vtf);
                addRelate(outkeyValues,tablefieldValues,vtf,ktf);
            }
        }
    }
    static Map<String,Collection<String>> getTablePks(Jedis jedis,String[] tables,Map<String,List<String>> fieldValues,Map<String,Map<String,List<String>>> outkeys,Map<String,List<String>> indexConds,Map<String,List<String>> pkcond){
        Map<String,Collection<String>> result = new HashMap<String,Collection<String>>();
        for(String table:tables){
            //with outkey
            if(outkeys.containsKey(table)){
                Map<String,List<String>> outks = outkeys.get(table);
                Iterator<String> outs = outks.keySet().iterator();
                List<String> ohs = new ArrayList<String>();
                Set<String> mpks=null;
                while(outs.hasNext()){
                    String ok = outs.next();
                    String opks = getTempId();
                    //把关联的其他表的这个外键的key一起加进来
                    //jedis.zrangeByScore("key","[","(");
                    List<String> rels = outks.get(ok);
                    //set outkey value
                    String t = null;
                    if(null != fieldValues){
                        List<String> ovs = fieldValues.get(table+TABLE_FIELD_SPLIT+ok);
                        if(null != ovs){
                            t = "T_"+opks;
                            jedis.sadd(t,ovs.toArray(new String[0]));
                            rels.add(t);
                        }
                    }
                    if(rels.size()>0){
                        //获取一个外键的keys
                        Set<String> set = jedis.sinter(rels.toArray(new String[0]));
                        if(null != t){
                            jedis.del(t);
                        }
                        if(null != set && set.size()>0) {
                            List<String> pks=new ArrayList<String>();
                            Iterator<String> its = set.iterator();
                            while(its.hasNext()){
                                String ov = its.next();
                                pks.add(getOutKey(table, ok, ov));
                            }
                            if(outks.size()>1){
                                jedis.sunionstore(opks,pks.toArray(new String[0]));
                                ohs.add(opks);
                            }else{
                                mpks = jedis.sunion(pks.toArray(new String[0]));
                            }
                        }
                    }
                }
                if(ohs.size()>0){
                    //根据多个外键的keys求pkey的交集
                    if(null != indexConds.get(table) && indexConds.get(table).size()>0)
                        ohs.addAll(indexConds.get(table));
                    mpks = jedis.sinter((String[])ohs.toArray(new String[0]));
                    jedis.del(ohs.toArray(new String[0]));
                }
                if(null != pkcond.get(table) && pkcond.get(table).size()>0)
                    mpks.retainAll(pkcond.get(table));
                /*List<String> ret=null;
                if(null !=mpks && mpks.size()>0){
                    List<String> ids = new ArrayList<String>();
                    for(String s:mpks){
                        ids.add(getPKKey(table,config.getPk(table),s));
                    }
                    ret = jedis.mget(ids.toArray(new String[0]));
                }
                if(null != ret&& ret.size()>0){
                    List<Map<String,String>> res = getFieldsValue(queryField.get(table),config.getOut(table), ret);
                    result.put(table,ret);
                }*/
                if(null != mpks && mpks.size()>0) {
                    result.put(table, mpks);
                }
            }else if(indexConds.containsKey(table)){//index
                List<String> inpk = indexConds.get(table);
                String tempid=null;
                if(pkcond.containsKey(table)){
                    tempid = getTempId();
                    List<String> pv = pkcond.get(table);
                    jedis.sadd(tempid,pv.toArray(new String[0]));
                    inpk.add(tempid);
                }
                Set<String> pks = jedis.sinter(inpk.toArray(new String[0]));
                if(null!=tempid){
                    jedis.del(tempid);
                }
                //List<String> ret=jedis.mget(pks.toArray(new String[0]));
                //List<Map<String,String>> res = getFieldsValue(queryField.get(table),config.getIndexs(table), ret);
                result.put(table,pks);
            }else if(pkcond.containsKey(table)){//pk
                //List<String> ret=jedis.mget(pkcond.get(table).toArray(new String[0]));
                //List<Map<String,String>> res = getFieldsValue(queryField.get(table),null, ret);
                result.put(table,pkcond.get(table));
            }
        }
        if(result.size()>0)
            return result;
        return null;
    }
    public boolean isAngleQuery(Map data){
        return config.isAngleQuery(data);
    }
    public static Map<String,Collection<String>> findDoTablePks(Jedis jedis,String[] tables,Map<String,String> fieldMapping,Map<String,List<String>> fieldValues){
        //convert values
        Map<String,Map<String,List<String>>> tablefieldValues  = convertValues(fieldValues);

        //pkvalues
        Map<String,List<String>> pkcond = getPkValues(fieldValues);

        //indexvalue
        Map<String,List<String>> indexConds = getIndexValues(fieldValues);

        //outkeyvalue
        Map<String,Map<String,List<String>>> outkeyValues=new HashMap<String, Map<String, List<String>>>();
        fillOutkeyValues(null,fieldMapping,outkeyValues,tablefieldValues);

        return getTablePks(jedis,tables,fieldValues,outkeyValues,indexConds,pkcond);
    }

    /**
     * 当前不支持分页查询，有最大返回记录数限制
     * @param queryFields   查询字段
     * @param fieldMapping  关联字段的映射关系
     * @param fieldValues   字段的值
     * @param filterResultValues 结果过滤排序
     * @return
     * @throws Exception
     */

    public List<Map<String,String>> query(Jedis jedis,String[] queryFields,Map<String,String> fieldMapping,Map<String,List<String>> fieldValues,Map<String,String> filterResultValues)throws Exception {
        try{
            //convert values
            Map<String,Map<String,List<String>>> tablefieldValues  = convertValues(fieldValues);

            //   table       field
            Map<String,List<String>> queryField = convertArray2Map(queryFields);

            //   table       outorindex     outkeylist
            Map<String,Map<String,List<String>>> outkeys=new HashMap<String, Map<String, List<String>>>();
            //table-table   [{field,field},...]
            Map<String,List<String[]>> mp=new HashMap<String, List<String[]>>();
            fillOutkeyValues(mp,fieldMapping,outkeys,tablefieldValues);

            //   table       indexvs
            Map<String,List<String>> indexConds = getIndexValues(fieldValues);

            //   table       pkvs
            Map<String,List<String>> pkcond = getPkValues(fieldValues);


            //query
            Map<String,Collection<String>> result = getTablePks(jedis,(String[])queryField.keySet().toArray(new String[0]),fieldValues,outkeys,indexConds,pkcond);//new HashMap<String, Collection<String>>();

            if(null != result && result.size()>0){
                Map<String, List<Map<String, String>>> queryResult=null;
                if(isQueryPkFields(queryField)){
                    queryResult = convertToResult(result);
                }else {
                    queryResult = getDBResult(result, queryField);
                }
                filterResult(queryResult);
                return compose(mp,queryResult);
            }else{
                return null;
            }
        }finally {

        }
    }
    static Map<String,List<Map<String, String>>> convertToResult(Map<String,Collection<String>> pkvs){
        Iterator<String> its = pkvs.keySet().iterator();
        Map<String,List<Map<String, String>>> ret = new HashMap<String,List<Map<String, String>>>();
        while(its.hasNext()) {
            String t = its.next();
            if(!ret.containsKey(t)){
                ret.put(t,new ArrayList());
            }
            Collection c = pkvs.get(t);
            Iterator vts = c.iterator();
            while(vts.hasNext()){
                HashMap map = new HashMap();
                String k = (String)vts.next();
                if(k.contains(".")){
                    k = k.split("\\.")[2];
                }
                map.put(config.getPk(t),k);
                ret.get(t).add(map);
            }

        }
        return ret;
    }
    static boolean isQueryPkFields(Map<String,List<String>> queryField ){
        Iterator<String> its = queryField.keySet().iterator();
        int n=0;
        while(its.hasNext()){
            String t = its.next();
            if(queryField.get(t).size()==1 && queryField.get(t).get(0).equals(config.getPk(t))){
                n++;
            }
        }
        if(n>0 && n==queryField.size()){
            return true;
        }
        return false;
    }
    static Map<String,List<String>> convertArray2Map(String[] queryFields){
        Map<String,List<String>> queryField = new HashMap<String, List<String>>();
        if(null != queryFields){
            for(String s:queryFields){
                String[] tfn = StringUtils.split(s,TABLE_FIELD_SPLIT);
                if(!queryField.containsKey(tfn[0])) queryField.put(tfn[0],new ArrayList<String>());
                if(tfn.length>1 && !queryField.get(tfn[0]).contains(tfn[1]))
                    queryField.get(tfn[0]).add(tfn[1]);
            }
        }
        return queryField;
    }
    static void appmap(Map<String,List<String[]>> mp,String[] ks,String[] vs){
        String k = ks[0].toUpperCase()+"-"+vs[0].toUpperCase();
        if(!mp.containsKey(k)) mp.put(k,new ArrayList<String[]>());
        mp.get(k).add(new String[]{ks[1].toUpperCase(),vs[1].toUpperCase()});
    }
    public Map<String,List<Map<String,String>>> getDBResult(Map<String,Collection<String>> tablePks,Map<String,List<String>> queryField) throws Exception {
        Map<String,List<Map<String,String>>> ret = new HashMap<String, List<Map<String, String>>>();
        Iterator its = tablePks.keySet().iterator();
        while(its.hasNext()){
            String k = (String)its.next();
            if(null == tablePks.get(k) || tablePks.get(k).size()==0)return null;
            List vs = new ArrayList();
            vs.addAll(tablePks.get(k));
            //根据主键值解析该主键存在的ds和table名称
            Collection<PkDsTable> ps = parsePkDsTable(k,vs);
            if(null != ps && ps.size()>0) {
                for(PkDsTable p:ps) {
                    List<String> qfs = queryField.get(k);
                    appendQueryField(k, qfs);
                    HashMap in = new HashMap();
                    HashMap kc = new HashMap();
                    kc.put(config.getPk(k), p.getPkv());
                    in.put("table", p.getTableName());
                    in.put("ds",p.getDs());
                    in.put("fields", qfs);
                    in.put("conds", kc);
                    in.put("op", "query");
                    in.put("isforcedb", true);
                    List<Map<String, String>> data = (List<Map<String, String>>) dc.doSomeThing(null, null, in, null, null);
                    //List<Map<String,String>> data = querySingleTableByPk(k,queryField.get(k),tablePks.get(k));
                    if (ret.containsKey(k)) {
                        ret.get(k).addAll(data);
                    } else {
                        ret.put(k, data);
                    }
                }
            }
            //System.out.println(k+"  "+tablePks.get(k));
        }
        return ret;
    }

    /**
     *
     * 根据主键值解析该条记录存在的位置
     * @param pks
     * @return
     */
    Collection<PkDsTable> parsePkDsTable(String k,List<String> pks){
        Map<String,PkDsTable> ret = new HashMap<String,PkDsTable>();
        Iterator<String> its = pks.iterator();
        while(its.hasNext()){
            String v = its.next();
            String[] r = AngleLoader.parseDsAndTableNameById(v);
            if(null != r && r.length==3){
                String tk = r[0].concat(".").concat(r[1]);
                if(!ret.containsKey(tk)){
                    PkDsTable p = new PkDsTable();
                    p.setDs(r[0]);
                    p.setTableName(r[1]);
                    p.setPkv(new ArrayList());
                    p.getPkv().add(r[2]);
                    ret.put(tk,p);
                }else{
                    ret.get(tk).getPkv().add(r[2]);
                }
            }else{
                PkDsTable p = new PkDsTable();
                p.setDs(null);
                p.setTableName(k);
                p.setPkv(pks);
                ret.put(k,p);
            }
        }
        if(ret.size()>0){
            return ret.values();
        }else{
            return null;
        }
    }



    static void appendQueryField(String table,List<String> fields){
        fields.addAll(config.getOut(table));
    }
    static String getSql(String table,List<String> fields,Collection ids){
        StringBuffer sb = new StringBuffer("select ");
        fields.addAll(config.getOut(table));
        Iterator<String> its = fields.iterator();
        boolean isf=true;
        while(its.hasNext()){
            if(isf){
                sb.append(its.next());
                isf=false;
            }else{
                sb.append(",").append(its.next());
            }
        }
        sb.append(" from ").append(table).append(" where ").append(config.getPk(table)).append(" in ").append("(");
        its= ids.iterator();
        isf=true;
        while(its.hasNext()){
            if(isf){
                sb.append(its.next());
                isf=false;
            }else{
                sb.append(",").append(its.next());
            }
        }
        sb.append(")") ;
        return sb.toString();
    }
    /*static List<Map<String,String>> querySingleTableByPk(String table,List<String> fields,Collection<String> ids){
        Connection conn = null;
        String sql=null;
        try{

            sql = getSql(table,fields,ids);
            conn = AngleConfig.getConnection(config.getDBSource(table).getDb());
            ResultSet rs = conn.createStatement().executeQuery(sql);
            List<Map<String,String>> ret = new LinkedList<Map<java.lang.String, java.lang.String>>();
            while(rs.next()){
                Map m = new HashMap();
                Iterator<String> its = fields.iterator();
                while(its.hasNext()){
                    String k = its.next();
                    m.put(k,rs.getString(k));
                }
                ret.add(m);
            }
            rs.close();
            return ret;
        }catch (Exception e){
            log.error(sql,e);
        }finally {
            if(null != conn) try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }*/
    //table.outkey
    public static String getOut(String table,String out){
        return OUTKEY_FLAG+TABLE_FIELD_SPLIT+config.getTableNum(table)+TABLE_FIELD_SPLIT+config.getTableFieldNum(table,out);
    }
    public static String getLike2OutKey(String table,String field,String out,String value){
        return LIKE_FLAG+OUTKEY_FLAG+TABLE_FIELD_SPLIT+config.getTableNum(table)+TABLE_FIELD_SPLIT+config.getTableFieldNum(table,field)+"-"+config.getTableFieldNum(table,out)+"_"+value;
    }
    public static String getLikeKey(String table,String field,String value){
        return LIKE_FLAG+TABLE_FIELD_SPLIT+config.getTableNum(table)+TABLE_FIELD_SPLIT+config.getTableFieldNum(table,field)+"_"+value;
    }
    /*public static String getIndex2OutKey(String tallefield,String out,String fieldvalue){
        return INDEX_FLAG+OUTKEY_FLAG+TABLE_FIELD_SPLIT+tallefield+"-"+out+"_"+fieldvalue;
    }*/
    public static String getIndex2OutKey(String table,String field,String out,String fieldvalue){
        return INDEX_FLAG+OUTKEY_FLAG+TABLE_FIELD_SPLIT+config.getTableNum(table)+TABLE_FIELD_SPLIT+config.getTableFieldNum(table,field)+"-"+config.getTableFieldNum(table,out)+"_"+fieldvalue;
    }
    public static String getIndexKey(String table,String index,String value){
        return INDEX_FLAG+TABLE_FIELD_SPLIT+config.getTableNum(table)+TABLE_FIELD_SPLIT+config.getTableFieldNum(table,index)+"_"+value;
    }
    public static String getPKKey(String table,String pk,String value){
        return PRIMARY_FLAG+TABLE_FIELD_SPLIT+config.getTableNum(table)+TABLE_FIELD_SPLIT+config.getTableFieldNum(table,pk)+"_"+value;
    }
    public static String getOutKey(String table,String index,String value){
        return OUTKEY_FLAG+PRIMARY_FLAG+TABLE_FIELD_SPLIT+config.getTableNum(table)+TABLE_FIELD_SPLIT+config.getTableFieldNum(table,index)+"_"+value;
    }
    static void addRelate(Map<String,Map<String,List<String>>> outkeys,Map<String,Map<String,List<String>>> tablefieldValues,String[] kvs,String[] rel){
        if(!outkeys.containsKey(kvs[0])) outkeys.put(kvs[0],new HashMap<String, List<String>>());
        if(!outkeys.get(kvs[0]).containsKey(kvs[1])) outkeys.get(kvs[0]).put(kvs[1],new ArrayList<String>());
        outkeys.get(kvs[0]).get(kvs[1]).add(getOut(kvs[0], kvs[1]));
        outkeys.get(kvs[0]).get(kvs[1]).add(getOut(rel[0], rel[1]));
        if(null != tablefieldValues && tablefieldValues.size()>0){
            List<String> inds = config.getOutIndexs(kvs[0], kvs[1]);
            addIndex2Out(outkeys,kvs[0],kvs[1],tablefieldValues.get(kvs[0]),inds,kvs[0],kvs[1]);
            inds = config.getOutIndexs(rel[0], rel[1]);
            addIndex2Out(outkeys,rel[0],rel[1],tablefieldValues.get(rel[0]),inds,kvs[0],kvs[1]);
        }

    }
    static void addIndex2Out(Map<String,Map<String,List<String>>> outkeys,String indxtable,String indexout,Map<String,List<String>> fieldValues,List<String> inds,String table,String out){
        if(null != inds && inds.size()>0 && null != fieldValues && fieldValues.size()>0){
            Iterator<String> its = fieldValues.keySet().iterator();
            while(its.hasNext()){
                String field = its.next();
                if(inds.contains(field)){
                    List<String> indvs = fieldValues.get(field);
                    if(null != indvs && indvs.size()>0){
                        for(String s:indvs){
                            if(s.startsWith("%")&&s.endsWith("%")){
                                s = s.substring(1,s.length()-1);
                                String[] ss = AngleLoader.getSplit(config,indxtable,field,s);
                                if(null != ss){
                                    for(String si:ss)
                                        outkeys.get(table).get(out).add(getLike2OutKey(indxtable,field,indexout,si.toLowerCase()));
                                }
                            }else{
                                outkeys.get(table).get(out).add(getIndex2OutKey(indxtable,field,indexout,s));
                            }
                        }
                    }
                }
            }
        }
    }

    static List<Map<String,String>> getFieldsValue(List<String> fields,Collection<String> outs,List<String> data){
        if(null != data && data.size()>0){
            List<Map<String,String>> ret = new ArrayList<Map<String, String>>();
            Iterator<String> its = data.iterator();
            while(its.hasNext()){
                String c = its.next();
                Map map =StringUtils.convert2MapJSONObject(c);
                HashMap d = new HashMap();
                for(String s:fields){
                    d.put(s,map.get(s));
                }
                for(String o:outs){
                    if(!d.containsKey(o))
                        d.put(o,map.get(o));
                }
                ret.add(d);
            }
            return ret;
        }
        return null;
    }
    public static void filterResult(Map<String,List<Map<String,String>>> data){

    }
    public static List<Map<String,String>> compose(Map<String,List<String[]>> mp,Map<String,List<Map<String,String>>> data){
        //System.out.println(data);
        if(null != data){
            List<Map<String,String>> ret= new ArrayList<Map<String, String>>();
            Iterator<String> its = data.keySet().iterator();
            String maxTable=null;
            int max=0;
            while(its.hasNext()){
                String table = its.next();
                if(data.size()==1){
                    ret=data.get(table);
                    break;
                }
                if(data.get(table).size()>max){
                    max=data.get(table).size();
                    maxTable=table;
                }
            }
            if(null != maxTable){
                ret = data.get(maxTable);
                its = data.keySet().iterator();
                while(its.hasNext()){
                    String s = its.next();
                    if(s.equals(maxTable))continue;
                    List<String[]> oks = mp.get(maxTable+"-"+s);
                    if(null != oks){
                        List<Map<String,String>> ls = data.get(s);
                        for(Map<String,String> t:ret){
                            append(t,oks,ls);
                        }
                    }
                }
            }
            return ret;
        }
        return null;
    }
    static void append(Map data,List<String[]> m,List<Map<String,String>> ls){
        boolean ism=true;
        for(Map<String,String> l:ls){
            for(String[] mi:m){
                if(!(null!= data.get(mi[0]) && data.get(mi[0]).equals(l.get(mi[1])))){
                    ism=false;
                    break;
                }
            }
            if(ism){
                data.putAll(l);
                break;
            }
        }
    }

}
