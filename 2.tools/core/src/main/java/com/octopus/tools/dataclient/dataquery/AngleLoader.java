package com.octopus.tools.dataclient.dataquery;

import com.octopus.tools.dataclient.v2.DataClient2;
import com.octopus.tools.dataclient.v2.ds.RouteResultBean;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.thread.ThreadPool;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import redis.clients.jedis.Jedis;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

/**
 * User: wfgao_000
 * Date: 15-7-24
 * Time: 上午10:43
 */
public class AngleLoader {

    /*public void setDc(DataClient2 dc) {
        this.dc = dc;
    }

    public void setJedis(Jedis jedis) {
        this.jedis = jedis;
    }

    public void setConfig(AngleConfig config) {
        this.config = config;
    }*/

    static synchronized void sadd(Jedis jedis,String key,String vs){
        if(null != key && null != vs){
            jedis.sadd(key,vs);
        }
    }
    static synchronized void add(Jedis jedis,String key,String vs){
        //Jedis jedis = getJedis();
        //jedis.set(key, vs);
    }

    /**
     * @todo
     * 根据id解析ds和表名称
     * @param v
     * @return 0 ds,1 tablename , 2 pkv
     */
    public static String[] parseDsAndTableNameById(String v){
        if(v.contains(".")){
            return v.split("\\.");
        }
        return null;
    }
    static synchronized String get(Jedis jedis,String key){
        return jedis.get(key);
    }
    public static void main(String[] args){
        try{
            AngleLoader loader = new AngleLoader();
            loader.loadData(null,null,null);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    class OneTable implements Runnable{
        transient Log log = LogFactory.getLog(OneTable.class);
        String sql,table,datasource;
        DataClient2 dc;
        AngleConfig config;
        Jedis jedis;
        public OneTable(DataClient2 dc,AngleConfig config,Jedis jedis,String table){
            this.table=table;
            this.sql=sql;
            this.dc = dc;
            this.config=config;
            this.jedis=jedis;
        }
        @Override
        public void run() {
            try {
                List<RouteResultBean> ls = config.router.getAllStoreByTableName(table);
                if (null != ls) {
                    for (RouteResultBean l : ls) {
                        loadSql(dc, config, jedis, table, l.getDataSource(), l.getTableName());
                    }
                }
            }catch (Exception e){
                log.error("load AngleQuery data error",e);
            }
        }
    }
    public void loadData(DataClient2 dc,AngleConfig config,Jedis jedis){
        List<String> tables = config.getCacheTables();
        ThreadPool tp= ThreadPool.getInstance().getThreadPool(dc.getXML().getId(),tables.size());

        for(String t:tables){
            //String sql = getSql(config,t);
            tp.getExecutor().execute(new OneTable(dc,config,jedis,t));
            //new OneTable(dc,config,jedis,t).run();
        }
    }


    List<String> getCacheFields(AngleConfig config,String table)throws Exception{
        List<String> field = new ArrayList<String>();
        String pk = config.getPk(table);
        if(StringUtils.isBlank(pk))
            throw new Exception("table "+table+ " not config primarkey field");
        if(!field.contains(pk)){
            field.add(pk);
        }
        List<String> is = config.getCacheIndexs(table);
        if(null != is){
            for(String i:is){
                if(!field.contains(i)){
                    field.add(i);
                }
            }
        }
        Set<String> o = config.getCacheOut(table);
        if(null != o){
            for(String i:o){
                if(!field.contains(i)){
                    field.add(i);
                }
            }
        }
        return field;
    }

    String getSql(AngleConfig config,String table){
        StringBuffer sb  = new StringBuffer();
        List<String> field = new ArrayList<String>();
        String pk = config.getPk(table);
        if(!field.contains(pk)){
            field.add(pk);
        }
        List<String> is = config.getIndexs(table);
        if(null != is){
            for(String i:is){
                if(!field.contains(i)){
                    field.add(i);
                }
            }
        }
        Set<String> o = config.getOut(table);
        if(null != o){
            for(String i:o){
                if(!field.contains(i)){
                    field.add(i);
                }
            }
        }

        for(int i=0;i<field.size();i++){
            if(i==0)
                sb.append(field.get(i));
            else
                sb.append(",").append(field.get(i));
        }
        return "select "+sb.toString()+" from "+table;
    }
    class RunPage implements Runnable{
        String table=null;
        String datasource;
        String realTable;
        int start,end;
        Connection conn = null;
        DataClient2 dc;
        AngleConfig config;
        Jedis jedis;
        RunPage(DataClient2 dc,AngleConfig config,Jedis jedis,String table,int start,int end,String ds,String realTableName){
            this.table=table;
            this.start=start;
            this.end=end;
            this.dc=dc;
            this.config=config;
            this.jedis=jedis;
            this.datasource=ds;
            this.realTable=realTableName;
        }

        @Override
        public void run() {
            try{
                //conn = AngleConfig.getConnection(config.getDBSource(table));
                exePage(dc,config,jedis,table, start, end,datasource,realTable);
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                if(null != conn) try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    void  loadSql(DataClient2 dc,AngleConfig config,Jedis jedis,String table,String ds,String realTableName){
        long l  = System.currentTimeMillis();
        Connection conn = null;
        try{
            HashMap map = new HashMap();
            map.put("table",realTableName);
            map.put("ds",ds);
            map.put("fields",getCacheFields(config, table));
            map.put("op","count");
            map.put("isforcedb",true);

            /*if(table.equals("INS_OFFER_571")) {
                HashMap m = new HashMap();
                m.put("USER_ID","307100000068");
                map.put("conds",m);
            }else if(table.equals("INS_USER_571")){
                HashMap m = new HashMap();
                m.put("USER_ID","307100000068");
                map.put("conds",m);
            }*/

            int count=(Integer)dc.doSomeThing(null,null,map,null,null);
            int start=0,end=0;
            int len=10000;
            ThreadPool p = ThreadPool.getInstance().getThreadPool(dc.getXML().getId(),10);
            while(end<count){
                start=end;
                end+=len;
                p.getExecutor().execute(new RunPage(dc,config,jedis,table, start, end,ds,realTableName));
            }
            start=end;
            end = count;
            //rs.close();
            if(end>start)
                exePage(dc,config,jedis,table, start, end,ds,realTableName);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(null != conn) try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            System.out.println(table+" load lost:"+(System.currentTimeMillis()-l));
        }
    }
    void exePage(DataClient2 dc,AngleConfig config,Jedis jedis,String table,int start,int end,String ds,String realTableName) throws Exception {
        HashMap map = new HashMap();
        map.put("op","query");
        map.put("table",table);
        map.put("isforcedb",true);

        /*if(table.equals("INS_OFFER_571")) {
            HashMap m = new HashMap();
            m.put("USER_ID","307100000068");
            map.put("conds",m);
        }else if(table.equals("INS_USER_571")){
            HashMap m = new HashMap();
            m.put("USER_ID","307100000068");
            map.put("conds",m);
        }
*/
        List<String> fs = getCacheFields(config,table);
        if(null != fs && fs.size()>0) {
            map.put("fields", fs);
            map.put("ds",ds);
            map.put("table",realTableName);
            map.put("start", String.valueOf(start));
            map.put("end", String.valueOf(end));
            Map t = new HashMap();
            t.put("result2string", "true");
            map.put("format", t);
            List<Map<String, String>> ret = (List<Map<String, String>>) dc.doSomeThing(null, null, map, null, null);
            for (Map<String, String> d : ret) {
                appendData(config, jedis, table, d,realTableName,ds);
            }
        }
    }
    /*void exePageSql(String table,Connection conn,String sql,int start,int end)throws Exception{
        LinkedList<String> fieldlist = new LinkedList();
        String esql = sql+" limit "+start+","+end;
        PreparedStatement ps = conn.prepareStatement(esql);
        ResultSetMetaData metaData = ps.getMetaData();
        for(int i=0;i<metaData.getColumnCount();i++){
            String fieldName = metaData.getColumnName(i+1);
            fieldlist.add(fieldName.toUpperCase());
        }
        ResultSet rs = ps.executeQuery();
        while(rs.next()){
            Map map = new HashMap();
            for(int i=0;i<fieldlist.size();i++){
                map.put(fieldlist.get(i),rs.getString(fieldlist.get(i)));
            }
            appendData(config,jedis,table,map);
        }
        rs.close();
    }*/

    public static void appendData(AngleConfig config,Jedis jedis,String table,Map<String,String> data,String realTableName,String ds){

        try{
        if(null != config.getPk(table)){
            String p = config.getPk(table);
            if(StringUtils.isNotBlank(p)){
                String pv = data.get(p);
                //make pkv with ds and table info
                if(StringUtils.isNotBlank(realTableName) && StringUtils.isNotBlank(ds)){
                    pv = ds.concat(".").concat(realTableName).concat(".").concat(pv);
                }
                if(null != pv){
                    //pk - value
                    //String str = ObjectUtils.convertMap2String(data);
                    //String key = AngleQuery.getPKKey(table,p,pv);
                    //add(jedis,key, str);

                    //index - pk
                    List<String> inds = config.getIndexs(table);
                    if(null != inds && inds.size()>0){
                        for(String ind:inds){
                            String iv = data.get(ind);
                            if(StringUtils.isNotBlank(iv)){
                                String ik = AngleQuery.getIndexKey(table,ind,iv);
                                sadd(jedis,ik,pv);
                            }
                        }
                    }

                    //模糊查询字段 like - pk
                    List<String> lks = config.getLikes(table);
                    if(null != lks){
                        for(String l:lks){
                            String d = data.get(l);
                            if(StringUtils.isNotBlank(d)){
                                String[] lins = getSplit(config,table,l,d);
                                if(null != lins){
                                    for(String li:lins){
                                        String ik = AngleQuery.getLikeKey(table,l,li);
                                        sadd(jedis,ik,pv);
                                    }
                                }
                            }
                        }
                    }

                    //index - out
                    Set<String> o = config.getOut(table);
                    if(null != o){
                    Iterator<String> its = o.iterator();
                        while(its.hasNext()) {
                            String ok = its.next();
                            String ov = data.get(ok);
                            if(StringUtils.isNotBlank(ov)){
                                List<String> ris = config.getOutIndexs(table,ok);
                                for(String ri:ris){
                                    String riv = data.get(ri);
                                    if(StringUtils.isNotBlank(riv)){
                                        String iok = AngleQuery.getIndex2OutKey(table,ri,ok,riv);
                                        sadd(jedis,iok,ov);
                                    }
                                }
                            }
                            //like - out
                            List<String> lis = config.getOutLikes(table,ok);
                            if(null != lis){
                                for(String l:lis){
                                    String d = data.get(l);
                                    if(StringUtils.isNotBlank(d)){
                                        String[] lins = getSplit(config,table,l,d);
                                        if(null != lins){
                                            for(String li:lins){
                                                String ik = AngleQuery.getLike2OutKey(table,l,ok,li);
                                                sadd(jedis,ik,ov);
                                            }
                                        }
                                    }
                                }
                            }
                            //table - out
                            String otk = AngleQuery.getOut(table,ok);
                            sadd(jedis,otk,ov);
                            //out = pk
                            String opk = AngleQuery.getOutKey(table,ok,ov);
                            sadd(jedis,opk,pv);
                        }

                    }
                }
            }
        }
        }finally {

        }
    }

    public static void deleteData(AngleConfig config,Jedis jedis,String table,Map<String,String> data){
        try{
        String pk = config.getPk(table);
        String pv = data.get(pk);
        jedis.del(AngleQuery.getIndexKey(table,pk,pv));
        //del index
        List<String> indx = config.getIndexs(table);
        if(null !=indx){
            HashMap<String,String> ls = new HashMap<String,String>();
            for(String in:indx){
                String v = data.get(in);
                String k = AngleQuery.getIndexKey(table, in, v);
                ls.put(k,pv);
            }
            if(ls.size()>0){
                Iterator<String> its = ls.keySet().iterator();
                while(its.hasNext()){
                    String k = its.next();
                    jedis.srem(k,ls.get(k));
                    if(jedis.scard(k)==0)
                        jedis.del(k);
                }

            }
        }
        //del like key
        List<String> lks = config.getLikes(table);
        if(null !=lks){
            HashMap<String,String> ls = new HashMap<String,String>();
            for(String lk:lks){
                String w = data.get(lk);
                String[] ws = getSplit(config,table,lk,w);
                if(null != ws){
                    for(String wi:ws){
                        String k = AngleQuery.getIndexKey(table, lk, wi);
                        ls.put(k,pv);
                    }
                }
            }
            if(ls.size()>0) {
                Iterator<String> its = ls.keySet().iterator();
                while(its.hasNext()){
                    String k = its.next();
                    jedis.srem(k,ls.get(k));
                    if(jedis.scard(k)==0)
                        jedis.del(k);
                }
            }
        }
        //del out key
        Set<String> outs = config.getOut(table);
        if(null != outs){
            HashMap<String,String> ls1 = new HashMap<String,String>();
            HashMap<String,String> ls2 = new HashMap<String,String>();
            HashMap<String,String> ls3 = new HashMap<String,String>();
            for(String out:outs){
                String ov= data.get(out);
                ls1.put(AngleQuery.getOutKey(table,out,ov),pv);
                List<String> ois = config.getOutIndexs(table,out) ;
                for(String oi:ois){
                    ls2.put(AngleQuery.getIndex2OutKey(table,oi,out,data.get(oi)),ov);
                }
                List<String> ols = config.getOutLikes(table,out);
                for(String ol:ols){
                    String w = data.get(ol);
                    String[] ws = getSplit(config,table,ol,w);
                    if(null != ws){
                        for(String wi:ws)
                            ls3.put(AngleQuery.getLike2OutKey(table, ol, out,wi ),ov);
                    }
                }
            }
            //del out key
            if(ls1.size()>0){
                Iterator<String> its = ls1.keySet().iterator();
                while(its.hasNext()){
                    String k = its.next();
                    jedis.srem(k,ls1.get(k));
                    if(jedis.scard(k)==0)
                        jedis.del(k);
                }
            }
            //del index-out
            if(ls2.size()>0){
                Iterator<String> its = ls2.keySet().iterator();
                while(its.hasNext()){
                    String k = its.next();
                    jedis.srem(k,ls2.get(k));
                    if(jedis.scard(k)==0)
                        jedis.del(k);
                }
            }
            //del like-out
            if(ls3.size()>0) {
                Iterator<String> its = ls3.keySet().iterator();
                while(its.hasNext()){
                    String k = its.next();
                    jedis.srem(k,ls3.get(k));
                    if(jedis.scard(k)==0)
                        jedis.del(k);
                }
            }
        }
        }finally {

        }
    }
    /*Map<String,String> getChg(Map<String,String> data,Map<String,String> newdata){
        HashMap<String,String> map = new HashMap();
        Iterator<String> its = data.keySet().iterator();
        while(its.hasNext()){
            String s = its.next();
            if(!((null == data.get(s) && null == newdata.get(s)) || (null != data.get(s) && null != newdata.get(s) && data.get(s).equals(newdata.get(s)))) ){
                map.put(s,newdata.get(s));
            }
        }
        return map;
    }*/
    public static void updateData(AngleConfig config,Jedis jedis,String table,Map<String,String> data,Map<String,String> newdata,String ds,String realTableName){
        deleteData(config,jedis,table,data);
        appendData(config,jedis,table,newdata,realTableName,ds);
    }
    static String[] getSplit(AngleConfig config,String table,String field,String s){
        String split = config.getWildChar(table,field);
        if(StringUtils.isNotBlank(split)){
            List<String> li  = new ArrayList<String>();
            String[] ss =  StringUtils.split(s,split);
            for(int i=0;i<ss.length;i++){
                if(StringUtils.isNotBlank(ss[i]))
                    li.add(ss[i].trim().toLowerCase()) ;
            }
            return li.toArray(new String[0]);
        }
       return null;
    }
}
