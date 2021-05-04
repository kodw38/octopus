package com.octopus.tools.dataclient.dataquery.redis;

import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cls.ClassUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: wfgao_000
 * Date: 15-8-13
 * Time: 下午2:22
 */
public class RedisClient extends XMLDoObject{
    transient static Log log = LogFactory.getLog(RedisClient.class);

    static Map<String,List<JedisPool>> redises = new ConcurrentHashMap<String, List<JedisPool>>();
    static Map<String,List<JedisPool>> prepareShareds = new ConcurrentHashMap<String, List<JedisPool>>();
    static Map<String,AtomicInteger> times = new HashMap();
    static Map<Integer,XMLMakeup> rel = new HashMap<>();

    public RedisClient(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        XMLMakeup[] gs=xml.getChild("cluster");
        if(null != gs){
            for(XMLMakeup g:gs){
                String id = g.getId();
                XMLMakeup[] rs = g.getChild("redis");
                List<JedisPool> ls = new ArrayList<JedisPool>();
                if(null != rs){
                    for(XMLMakeup r:rs){
                        JedisPoolConfig config = new JedisPoolConfig();
                        if(r.getProperties().containsKey("maxtotal") && StringUtils.isNotBlank(r.getProperties().getProperty("maxtotal")))
                            config.setMaxTotal(Integer.parseInt(r.getProperties().getProperty("maxtotal")));
                        if(r.getProperties().containsKey("minIdle") && StringUtils.isNotBlank(r.getProperties().getProperty("minIdle")))
                            config.setMaxIdle(Integer.parseInt(r.getProperties().getProperty("minIdle")));

                        if(r.getProperties().containsKey("maxwaitmillis") && StringUtils.isNotBlank(r.getProperties().getProperty("maxwaitmillis")))
                            config.setMaxWaitMillis(Long.parseLong(r.getProperties().getProperty("maxwaitmillis")));
                        int timeout=0;
                        if(r.getProperties().containsKey("timeoutmillis") && StringUtils.isNotBlank(r.getProperties().getProperty("timeoutmillis")))
                            timeout = Integer.parseInt(r.getProperties().getProperty("timeoutmillis"));
                        String password=null;
                        if(r.getProperties().containsKey("password") && StringUtils.isNotBlank(r.getProperties().getProperty("password")))
                            password = r.getProperties().getProperty("password");
                        JedisPool p=null;
                        config.setTestOnBorrow(true);
                        config.setTestOnReturn(true);

                        if(timeout>0)
                            if(StringUtils.isNotBlank(password))
                                p = new JedisPool(config,r.getProperties().getProperty("ip"),Integer.parseInt(r.getProperties().getProperty("port")),timeout,password);
                            else
                                p = new JedisPool(config,r.getProperties().getProperty("ip"),Integer.parseInt(r.getProperties().getProperty("port")),timeout);
                        else
                            p = new JedisPool(config,r.getProperties().getProperty("ip"),Integer.parseInt(r.getProperties().getProperty("port")));
                        if(null !=p) {
                            rel.put(p.hashCode(),r);
                            ls.add(p);
                        }

                    }
                }
                if(ls.size()>0){
                    redises.put(id,ls);
                    times.put(id,new AtomicInteger(0));
                }
            }
        }
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if(prepareShareds.size()>0){
                    Iterator it = prepareShareds.keySet().iterator();
                    while(it.hasNext()){
                        String k = (String)it.next();
                        List<JedisPool> ls = prepareShareds.get(k);
                        if(null != ls && ls.size()>0){
                            for(int i=ls.size()-1;i>=0;i--){
                                Jedis d=null;
                                try {
                                    d = ls.get(i).getResource();
                                    JedisPool jp = ls.get(i);
                                    ls.remove(i);
                                    synchronized (redises){
                                        redises.get(k).add(jp);
                                    }
                                }catch (Exception e){

                                }finally {
                                    if(null != d)d.close();
                                }
                            }
                        }
                    }
                }

            }
        },10000,10000);
    }

    Jedis getAnyRedis(String key,List<JedisPool> shards,int cyclecount)throws Exception{
        cyclecount++;
        if(cyclecount>shards.size()){
            throw new Exception("all redis of the group["+key+"] can not connect. current :"+cyclecount);
        }
        int count = shards.size();
        for(int i=0;i<count ;i++){
            int l = times.get(key).incrementAndGet();
            int c = l%shards.size();
            if(c==0){
                times.get(key).set(0);
            }
            JedisPool j=shards.get(c);
            if(null!=j) {
                Jedis d = null;
                try {
                    d = j.getResource();
                    XMLMakeup xs = rel.get(j.hashCode());
                    if (null != xs) {
                        String db = xs.getProperties().getProperty("db");
                        if (StringUtils.isNotBlank(db)) {
                            d.select(Integer.parseInt(db));
                        }
                    }

                    return d;
                } catch (JedisConnectionException ex) {
                    log.error("active:" + j.getNumActive() + " idle:" + j.getNumIdle() + " waiter:" + j.getNumWaiters(), ex);
                    if (null != d) {
                        d.close();
                    }
                    shards.remove(j);
                    synchronized (prepareShareds) {
                        if (null == prepareShareds.get(key)) prepareShareds.put(key, new ArrayList<JedisPool>());
                        prepareShareds.get(key).add(j);
                    }
                    return getAnyRedis(key, shards, cyclecount);
                }
            }

        }
        throw new Exception("There are no JedisPool available ");
    }

    static AtomicInteger atomicInteger = new AtomicInteger(0);
    //Jedis jedis = new Jedis("127.0.0.1",6379);
    public Jedis getRedis(String key)throws Exception{
        List<JedisPool> ps = redises.get(key);
        if(null != ps){
             Jedis j= getAnyRedis(key,ps,0);
            return j;
        }
        return null;
    }

    @Override
    public Object doSomeThing(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        String db = (String)input.get("db");
        String op = (String)input.get("op");
        String key = (String)input.get("key");
        Object eo = input.get("expire");
        Integer expire=null;
        if(eo instanceof Integer) {
            expire = (Integer) eo;
        }
        if(eo instanceof String){
            expire = Integer.parseInt((String)eo);
        }
        List<String> keys = (List)input.get("keys");
        Object v = input.get("value");
        if(StringUtils.isNotBlank(op) && op.equals("getRedis") && StringUtils.isNotBlank(key)){
            return getRedis(key);
        }
        if(StringUtils.isNotBlank(db)
                && StringUtils.isNotBlank(op)
                ){
            Jedis jedis = null;
            Jedis toj=null;
            try{

                if("return".equals(op)){
                    jedis = (Jedis)input.get("obj");
                    if(null != jedis){
                        jedis.close();
                        return null;
                    }
                }
                jedis = getRedis(db);
                if(jedis==null)throw new Exception("not get redis "+db);

                if("borrow".equals(op)){
                    return jedis;
                }else if(op.equals("add") && null != v){
                    jedis.set(key, v.toString());
                }else if(op.equals("get") && null != key){
                    return jedis.get(key);
                }else if("lpush".equals(op)){

                    jedis.lpush(key,v.toString());
                    Integer remain = ObjectUtils.getInteger(input.get("remain"));
                    if(null != remain && remain>0){
                        long n = jedis.llen(key);
                        if(n>remain){
                            jedis.ltrim(key,n-remain,n);
                        }
                    }
                }else if("rpush".equals(op) && null != v){
                    jedis.rpush(key,v.toString());
                }else if("contains".equals(op)){
                    return jedis.exists(key);
                }else if("remove".equals(op)){
                    if(null != v){
                        if(v.equals(jedis.get(key))){
                            jedis.del(key);
                        }
                    }else{
                        jedis.del(key);
                    }
                }else if("keys".equals(op)){
                    Set<String> ret = jedis.keys("*"+v.toString()+"*");
                    if(null != ret){
                        List li = new ArrayList();
                        li.addAll(ret);
                        return li;
                    }
                }else if("clear".equals(op)){
                    return jedis.flushDB();
                }else if("lget".equals(op)){
                    List<String> ls = jedis.lrange(key,0,-1);
                    return ls;
                }else if("syn".equals(op)){
                    String todb = (String)input.get("todb");
                    toj = getRedis(todb);
                    syn(jedis,keys,toj);
                }else if("hset".equals(op)){
                    String m = (String)input.get("v_key");
                    if(StringUtils.isBlank(m)){
                        throw new Exception("[hset] must have v_key property value");
                    }
                    jedis.hset(key,m,v.toString());
                }else if("hget".equals(op)){
                    String m = (String)input.get("v_key");
                    if(StringUtils.isBlank(m)){
                        throw new Exception("[hget] must have v_key property value");
                    }
                    String o = jedis.hget(key,m);
                    return o;
                }
                if(null != key && null != expire && expire>0 && jedis.exists(key) && jedis.ttl(key)<0){
                    jedis.expire(key,expire);

                }
            }catch (Exception e){
                log.error("RedisClient",e);
            }finally {
                if(!"borrow".equals(op) && !"return".equals(op)) {
                    if (null != jedis) jedis.close();
                }
                if(null != toj)toj.close();
            }
        }
        if(null !=env){
            return env.getResult();
        }
        return null;
    }

    void syn(Jedis j,List<String> ks,Jedis toj)throws Exception{
        if(null == ks || ks.size()==0){
//            Set<String> s = j.keys("*");
            ScanParams pars = new ScanParams();
            pars.count(100000);
            pars.match("*");
            String cursor=null;
            List list=null;
            int count=0;
            long total=0;
            while(cursor==null || !cursor.equals("0")){
                count++;
                if(null == cursor)cursor="0";
                ScanResult sr = j.scan(cursor, pars);
                list=sr.getResult();
                if(log.isErrorEnabled())
                System.out.println("get key [*] scan times "+count+" count:"+list.size());
                synkey(j,list,toj);
                total+=list.size();
                cursor = sr.getStringCursor();
            }
            System.out.println("has syn * "+total);
        }else{
            for(String k:ks){
                ScanParams pars = new ScanParams();
                pars.count(1000000);
                pars.match(k);
                String cursor=null;
                List list=null;
                int count=0;
                long total=0;
                while(cursor==null || !cursor.equals("0")){
                    count++;
                    if(null == cursor)cursor="0";
                    ScanResult sr = j.scan(cursor, pars);
                    list=sr.getResult();
                    if(log.isErrorEnabled())
                    System.out.println("get key ["+k+"] scan times "+count+" count:"+list.size());
                    synkey(j,list,toj);
                    total+=list.size();
                    cursor = sr.getStringCursor();
                }

                System.out.println("has syn  "+k+" "+total);
                /*Set<String> s = j.keys(k);
                System.out.println("get key ["+k+"] count:"+s.size());
                synkey(j,s,toj);*/

            }

        }

    }

    void synkey(Jedis j,List s,Jedis toj)throws Exception{
        Iterator<String> its = s.iterator();
        String t,k;
        while(its.hasNext()){
            k = its.next();
            t=j.type(k);

            if("string".equals(t)){
                toj.set(k,j.get(k));
            }else if("list".equals(t)){
                List<String> ls = j.lrange(k,0,-1);
                toj.rpush(k,ls.toArray(new String[0]));
            }else if("hash".equals(t)){
                Map<String,String> d = j.hgetAll(k);
                toj.hmset(k,d);
            }else if("set".equals(t)){
                Set<String> r = j.smembers(k);
                toj.sadd(k,r.toArray(new String[0]));
            }else if("zset".equals(t)){
                Set<String> c = j.zrange(k,0,-1);
                Iterator it = c.iterator();
                double d=0;
                while(it.hasNext()){
                    String o = (String)it.next();
                    toj.zadd(k,d,o);
                    d++;
                }
            }else if("none".equals(t)){
                continue;
            } else
                throw new Exception("not support type "+t);
        }
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
        if(ret instanceof ResultCheck)
            return (ResultCheck)ret;
        else
            return new ResultCheck(true,ret);
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config,Object ret,Exception e) throws Exception {
        throw new Exception("now not support roolback",e);
    }
}
