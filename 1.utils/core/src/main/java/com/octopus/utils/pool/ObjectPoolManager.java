package com.octopus.utils.pool;


import org.apache.log4j.Logger;

import java.util.HashMap;

/**
 * User: wf
 * Date: 2008-8-23
 * Time: 15:24:56
 */
public class ObjectPoolManager {

    private static final Logger log = Logger.getLogger(ObjectPoolManager.class);
    private static HashMap map = new HashMap();


    public  static void createObjectPool(String poolid,int count,Object[] os)throws Exception{
        if(null == poolid || "".equals(poolid.trim())){
            throw new IllegalArgumentException("poolid is must input.");
        }
        ObjectPool pool = new ObjectPool(count,os);
        if(map.containsKey(poolid)){throw new IllegalArgumentException("the "+poolid+" has exist in ObjectPoolManager.");}
        map.put(poolid,pool);
    }

    public static ObjectPool getObjectPool(String poolid){
        if(map.containsKey(poolid)){
            return (ObjectPool)map.get(poolid);
        }else{
            throw new IllegalArgumentException("the ObjectPoolManager is not exist "+poolid+" ObjectPool.");            
        }
    }

    public static void remove(String poolid){
        map.remove(poolid);
    }

    public static void clear(){
        map.clear();
    }
}
