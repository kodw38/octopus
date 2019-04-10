package com.octopus.tools.cache.impl;

import com.octopus.tools.cache.ICache;
import com.octopus.tools.cache.ICacheListener;
import com.octopus.tools.cache.ICacheManager;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;

/**
 * User: Administrator
 * Date: 14-9-18
 * Time: 上午10:56
 *
 */
public class CacheManager extends XMLDoObject implements ICacheManager {
    transient static Log log = LogFactory.getLog(CacheManager.class);
    //HashMap<String,XMLDoObject> caches;
//    ArrayList<ICacheListener> listeners;
    public CacheManager(XMLMakeup xml,XMLObject parent,Object[] containers) throws Exception {
        super(xml,parent,containers);
/*
        if(null != listeners && listeners.size()>0 )
            ExecutorUtils.multiWorkSamePar(listeners.toArray(new ICacheListener[0]), "doListener", new Class[]{Object.class}, new Object[]{caches});
*/
    }

    @Override
    public boolean addCache(ICache cache) {
/*
        if(null == cache)return false;
        if(caches.containsKey(cache.getId()))return false;
        caches.put(cache.getId(),cache);
*/
        return true;
    }

    @Override
    public ICache getCache(String name) {
        Object obj = getObjectById(name);
        if(null != obj){
            if(ICache.class.isAssignableFrom(obj.getClass())) {
                return (ICache)obj;
            }
        }
        return null;
    }

    @Override
    public void addListener(ICacheListener cacheListener) {
/*
        if(null != listeners)
            listeners.add(cacheListener);
*/
    }
    public void clearAllCache(){
/*
        Iterator its = caches.keySet().iterator();
        while(its.hasNext()){
            String s = (String)its.next();
            caches.get(s).clear(false);
            caches.get(s).clear(true);
        }
*/
//        caches.clear();
        log.error("has cleared all cache data");
        Runtime.getRuntime().gc();
    }

    @Override
    public Object doSomeThing(String xmlid,XMLParameter env, Map input,Map output, Map config) throws Exception {
        if(null != input){
            String cache = (String)input.get("cache");
            if(null != input && "clearAll".equals(input.get("op"))){
                XMLMakeup[] xs = getXML().getChild("cache");
                if(log.isDebugEnabled())
                    log.debug("start clear all cache...");
                if(null != xs){
                    for(XMLMakeup x:xs){
                        ICache c = getCache(x.getId());
                        if(null !=c) {
                            if(log.isDebugEnabled())
                                log.debug("start to clear cache "+x.getId());
                            c.clear(true);
                            c.clear(false);
                        }
                    }
                    System.gc();
                }
            }else if(StringUtils.isNotBlank(cache)){
                try{
                    //log.error("----beg"+input);
                    ICache ch = (ICache)getObjectById(cache);
                    if(null ==ch)
                        throw new Exception("not find cache "+cache);
                    /*if("addList".equals(input.get("op")) && StringUtils.isNotBlank(input.get("unique"))){
                        //log.error("----111:");
                        Object  unique = input.get("unique");
                        if(null != unique){
                            Object data = input.get("value");
                            Object o2 = ObjectUtils.getValueByPath(data, unique);
                            //log.error("----222:"+o2);
                            if(null != o2){
                                if(ch.existListObjectByUnique((String)input.get("key"),unique,o2))
                                    return null;
                            }
                        }
                    }*/
                    //long l = System.currentTimeMillis();
//                    caches.get(cache).doCheckThing(xmlid,env,input,output,config,null);
                    //log.error("----env"+env.hashCode());
                    if(null == env)
                        env = getEmptyParameter();
                    ((XMLDoObject)ch).doCheckThing(cache,env,input,output,config,null);

                    Object o = env.getResult();
                    if(null != o && o instanceof ResultCheck){
                        return ((ResultCheck)o).getRet();
                    }else{
                        return o;
                    }
                    //System.out.println("cache "+input.get("cache")+" "+Thread.currentThread().getName()+" "+new Date().getTime()+" "+(System.currentTimeMillis()-l));
                    //return o;
                }catch (Exception e){
                    throw new Exception("get cache["+cache+"]",e);
                }
            }
        }
        return null;
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid,XMLParameter env, Map input,Map output, Map config) throws Exception {
        if(null != input){

            String op= (String)input.get("op");
            String cache=(String)input.get("cache");

            if("get".equals(op)||"del".equals(op)||"exist".equals(op)||"clearAll".equals(op)||"size".equals(op)||"switch".equals(op)||"getall".equals(op)){
                if(log.isDebugEnabled()){
                    String key = ((null != input && StringUtils.isNotBlank(input.get("key")))?(String)input.get("key"):null);
                    System.out.println("   cache key:["+key+"]");
                }
                return true;
            }else{
                String key = (String)input.get("key");
                Object value =  input.get("value");
                if(StringUtils.isNotBlank(key)){
                    if(log.isDebugEnabled()){
                        System.out.println("   cache ["+input.get("cache")+"]"+op+" key:"+key +"\n   value:"+value);
                    }
//                    if(value instanceof String && XMLParameter.isHasRetainChars((String)value))
//                        return false;
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public ResultCheck checkReturn(String xmlid,XMLParameter env, Map input,Map output, Map config, Object ret) throws Exception {
        return new ResultCheck(true,ret);
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config,Object ret,Exception e) throws Exception {
        throw new Exception("now support rollback",e);
    }


}
