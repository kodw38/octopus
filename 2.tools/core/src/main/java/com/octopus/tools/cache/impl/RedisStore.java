package com.octopus.tools.cache.impl;

import com.octopus.tools.dataclient.dataquery.redis.RedisClient;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import redis.clients.jedis.Jedis;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * containsKey,put,get,remove,clear,keySet,size
 * User: wfgao_000
 * Date: 15-9-29
 * Time: 上午9:35
 */
public class RedisStore extends XMLObject implements Map {
    RedisClient rs;
    String db;
    public RedisStore(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        rs = (RedisClient) getObjectById("RedisClient");
        db = xml.getFirstCurChildText("redis");
        if(StringUtils.isBlank(db))
            throw new Exception(" RedisStore not set redis");
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

    Jedis getRedis()throws Exception{
        return rs.getRedis(db);
    }

    @Override
    public int size() {
        Jedis jedis=null;
        try{
            jedis =  getRedis();
        return jedis.dbSize().intValue();
        }catch (Exception e){
            return 0;
        }finally {
            if(null != jedis)jedis.close();
        }
    }

    @Override
    public boolean isEmpty() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean containsKey(Object key) {
        Jedis jedis=null;
        try{
            jedis =  getRedis();
            Set ss = jedis.keys((String) key);
            if(ss.size()>0)
                return true;
            return false;
        }catch (Exception e){
            return false;
        }finally {
            if(null != jedis)jedis.close();
        }

    }

    @Override
    public boolean containsValue(Object value) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object get(Object key) {
        Jedis jedis=null;
        try{
            jedis =  getRedis();
            String ss = jedis.get((String)key);
            if(ss.startsWith("{")){
                return StringUtils.convert2MapJSONObject(ss);
            }
            return ss;
        }catch (Exception e){
            return null;
        }finally {
            if(null != jedis)jedis.close();
        }

    }

    @Override
    public Object put(Object key, Object value) {
        Jedis jedis=null;
        try{
            jedis =  getRedis();
            if(value instanceof Map){
                value = ObjectUtils.convertMap2String((Map)value);
            }
            String ss = jedis.set((String)key,(String)value);
            return ss;
        }catch (Exception e){
            return null;
        }finally {
            if(null != jedis)jedis.close();
        }
    }

    @Override
    public Object remove(Object key) {
        Jedis jedis=null;
        try{
            jedis =  getRedis();
            long ss = jedis.del((String)key);
            return ss;
        }catch (Exception e){
            return null;
        }finally {
            if(null != jedis)jedis.close();
        }
    }

    @Override
    public void putAll(Map m) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void clear() {
        Jedis jedis=null;
        try{
            jedis =  getRedis();
            jedis.flushDB();
        }catch (Exception e){
        }finally {
            if(null != jedis)jedis.close();
        }
    }

    @Override
    public Set keySet() {
        Jedis jedis=null;
        try{
            jedis =  getRedis();
            Set ks = jedis.keys("*");
            return ks;
        }catch (Exception e){
            return null;
        }finally {
            if(null != jedis)jedis.close();
        }
    }

    @Override
    public Collection values() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Set entrySet() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
