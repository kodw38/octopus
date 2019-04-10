package com.octopus.tools.dataclient.dataquery;

import com.octopus.tools.dataclient.dataquery.redis.RedisClient;
import com.octopus.tools.dataclient.v2.DataClient2;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import redis.clients.jedis.Jedis;

import java.util.*;

/**
 * User: wfgao_000
 * Date: 15-8-12
 * Time: 下午1:55
 */
public class AngleQueryObject extends XMLDoObject{
    AngleQuery query = new AngleQuery();
    String key =null;
    AngleConfig angleconfig;
    Map cfg;
    AngleLoader loader = new AngleLoader();
    public AngleQueryObject(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        cfg = StringUtils.convert2MapJSONObject(xml.getProperties().getProperty("config"));
        key  = (String)xml.getFirstCurChildText("property","name","redis.key");
        DataClient2 dc = (DataClient2)getObjectById((String)cfg.get("dataclient"));
        //XMLMakeup[] x = xml.getChild("dataquery");
        query.setConfig(angleconfig);
        query.setDataClient(dc);
        //init load all config table data to redis
        /*String init = xml.getFirstCurChildText("property","name","initdata");
        if(StringUtils.isTrue(init)) {
            addInitAction(this,"init",new Class[]{DataClient2.class},new Object[]{dc});
        }*/


    }

    public void doInitial()throws Exception{
        String init = getXML().getFirstCurChildText("property","name","initdata");
        if(StringUtils.isTrue(init)) {
            DataClient2 dc = (DataClient2) getObjectById((String) cfg.get("dataclient"));
            try {
                RedisClient rc = (RedisClient) this.getObjectById((String) cfg.get("cacheobj"));
                loader.loadData(dc, angleconfig, rc.getRedis(key));
            } catch (Exception e) {
                log.error("init angleQuery", e);
            }
        }

    }
    public Map<String,Collection<String>> findDoTablePks(String[] tables,Map<String, String> rels,Map conds)throws Exception{
        RedisClient rc = (RedisClient) this.getObjectById((String) cfg.get("cacheobj"));
        Jedis jedis = rc.getRedis(key);
        try {
            return query.findDoTablePks(jedis, tables, rels, conds);
        }finally {
            jedis.close();
        }
    }
    public Boolean isAngleQuery(Map data){
        return Boolean.valueOf(angleconfig.isAngleQuery(data));
    }
    @Override
    public Object doSomeThing(String xmlid,XMLParameter env, Map input, Map output, Map cnf) throws Exception {
        RedisClient rc = (RedisClient)this.getObjectById((String)cfg.get("cacheobj"));

        Object ret=null;
        Jedis jedis = rc.getRedis(key);
        String tableCode = (String)input.get("table");
        String op = (String)input.get("op");
        Map<String,String> newData = (Map)input.get("newdata");
        Map<String,String> oldData = (Map)input.get("olddata");
        try{
            if("isAngleQuery".equals(op)){
                return isAngleQuery(input);
            }else if("INSERT".equals(op)){
                AngleLoader.appendData(angleconfig,jedis,tableCode,newData,null,null);
                return true;
            }else  if("DELETE".equals(op)){
                AngleLoader.deleteData(angleconfig,jedis,tableCode, oldData);
                return true;
            }else if("UPDATE".equals(op)){
                AngleLoader.updateData(angleconfig,jedis,tableCode, oldData, newData,null,null);
                return true;
            }else{
                String[] fields = (String[]) ((List) input.get("fields")).toArray(new String[0]);
                HashMap<String, String> mapp= (HashMap)input.get("fieldMapping");

                HashMap<String, List<String>> vv = new HashMap<String, List<String>>();
                Map<String,Object> vs = (Map) input.get("conds");
                Iterator<String> its = vs.keySet().iterator();
                while(its.hasNext()){
                    String k = its.next();
                    Object o = vs.get(k);
                    if(o instanceof String) {
                        List li = new ArrayList();
                        li.add(o);
                        vv.put(k, li);
                    }else if(o instanceof List){
                        vv.put(k, (List)o);
                    }else{
                        List li = new ArrayList();
                        li.add(ObjectUtils.toString(o));
                        vv.put(k, li);
                    }
                }
                ret = query.query(jedis, fields, mapp, vv, null);
                return ret;
            }
        }finally {
            jedis.close();
        }
    }

    @Override
    public boolean checkInput(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;
    }

    @Override
    public ResultCheck checkReturn(String xmlid,XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return new ResultCheck(true,ret);
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config,Object ret,Exception e) throws Exception {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
