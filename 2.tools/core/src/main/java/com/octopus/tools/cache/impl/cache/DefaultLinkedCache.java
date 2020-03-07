package com.octopus.tools.cache.impl.cache;

import com.octopus.tools.cache.ICache;
import com.octopus.tools.cache.ICacheListener;
import com.octopus.tools.cache.impl.QueryCondition;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.lang.ArrayUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 通用的内存数据存储
 * User: Administrator
 * Date: 14-9-17
 * Time: 下午2:27
 */
public class DefaultLinkedCache extends XMLDoObject implements ICache {
    String key;     //缓存名称
    int maxcount;     //缓存最大数量，-1为无底线，配置为一个实例的大小。
    double maxmemsize;   //缓存最大内存，单位MB，-1为无底线，配置为一个实例的大小。
    Map store = new ConcurrentHashMap();
    Map back = new ConcurrentHashMap();
    ConcurrentLinkedQueue queue;
/*
    ArrayList<ICacheEvent> events;
*/
    ArrayList<ICacheListener> listeners;

    public DefaultLinkedCache(XMLMakeup xml,XMLObject parent,Object[] containers) throws Exception {
        super(xml,parent,containers);
        if(null != listeners && listeners.size()>0 )
            ExecutorUtils.multiWorkSamePar(listeners.toArray(new ICacheListener[0]), "doListener", new Class[]{Object.class}, new Object[]{store});
    }

    @Override
    public boolean init(String name, int maxCount, int memSizeMB) {
        key=name;
        maxcount=maxCount;
        maxmemsize=memSizeMB;
        return true;
    }
    public boolean isExist(boolean isback,String key){
        if(!isback)
            return store.containsKey(key);
        else
            return back.containsKey(key);
    }
    /**
     * 并发调用，都返回true则返回true，否则返回false。并打印异常信息。
     * @return
     */
    boolean doEvents(String methodName,String key,Object data) throws InterruptedException {
/*
        if(null != events && events.size()>0){
            ICacheEvent[] es = events.toArray(new ICacheEvent[0]);
            return ExecutorUtils.teamWork2AllTrue(es, "doCacheEvent", new Class[]{String.class, String.class, Object.class, Object.class}, new Object[]{methodName, key, data, store});
        }
*/
        return true;
    }


    /**
     * Event都返回true，才存储
     * @param key
     * @param data
     * @return
     */
    @Override
    public boolean add(boolean isback,String key,Object data) throws InterruptedException{
        if(!isback)
            return add(store,key,data);
        else
            return add(back,key,data);
    }
    public boolean add(Map store,String key,Object data) throws InterruptedException {
        if(doEvents(ICache.METHOD_NAME_ADD,key,data)){

            //if(store.containsKey(key)) return false;
            //synchronized (store){
                store.put(key,data);
            //}
            return true;
        }
        return false;
    }

    @Override
    public boolean initAdd(boolean isback,String key, Object data) throws InterruptedException{
        if(!isback)
            return initAdd(store,key,data);
        else
            return initAdd(back,key,data);
    }
    boolean initAdd(Map store,String key, Object data) throws InterruptedException {
        if(store.containsKey(key)) return false;
        synchronized (store){
            store.put(key,data);
        }
        return true;
    }
    Map getObjects(Object unique,Object obj){
        HashMap m = new HashMap();
        if(unique instanceof String) {
            Object o2 = ObjectUtils.getValueByPath(obj, (String) unique);
            m.put(unique,o2);
        }else if(unique instanceof List){
            for(Object s:(List)unique){
                Object o2 = ObjectUtils.getValueByPath(obj, (String) s);
                m.put(s,o2);
            }
        }
        return m;
    }

    public boolean addList(boolean isback,String key, Object data, Object unique,Map group,int limitSize,boolean isreplace) throws Exception{
        if(!isback)
            return addList(store,key,data,unique,group,limitSize,isreplace);
        else
            return addList(back,key,data,unique,group,limitSize,isreplace);
    }
    public boolean addList(Map store,String key, Object data, Object unique,Map group,int limitSize,boolean isreplace) throws Exception {
        //System.out.println(Thread.currentThread().getName()+" key:"+key);
        if(null != unique){
            Object o2 = getObjects(unique, data);
            if(null != o2){
                if(!store.containsKey(key)) store.put(key,new LinkedList());
                List li = (List)store.get(key);
                if(null != li){
                    synchronized (li) {
                        for (int i = 0; i < li.size(); i++) {
                            Object o = li.get(i);
                            Object o1 = getObjects(unique, o);
                            if (null != o1 && ArrayUtils.isEquals(o1, o2)) {
                                return false;
                            }
                        }
                    }
                }
            }else{
                return false;
            }
        }else{
            if(!store.containsKey(key)) store.put(key,new CopyOnWriteArrayList());
            if(((List)store.get(key)).contains(data)) return false;
            else {
                ((List) store.get(key)).add(data);
                if(limitSize>0 && ((List)store.get(key)).size()>limitSize){
                    int n = ((List)store.get(key)).size()-limitSize;
                    while(n>0){
                        ((List)store.get(key)).remove(0);
                        n--;
                    }
                }
            }
        }
        if(doEvents(ICache.METHOD_NAME_ADDLIST,key,data)){
            ((List)store.get(key)).add(data);
            return true;
        }
        return false;
    }

    /**
     * Event都返回true，才更新
     * @param key
     * @param data
     * @return
     */
    @Override
    public boolean update(boolean isback,String key,Object data) throws InterruptedException{
         if(!isback)
             return update(store,key,data);
        else
             return update(back,key,data);
    }
     boolean update(Map store,String key,Object data) throws InterruptedException {
        if(doEvents(ICache.METHOD_NAME_UPDATE,key,data)){

            if(!store.containsKey(key)) return false;
            synchronized (store){
                store.put(key,data);
            }
        }
        return false;
    }

    /**
     * Event都返回true，才删除
     * @param key
     * @return
     */
    @Override
    public boolean delete(boolean isback,String key) throws InterruptedException{
       if(!isback)
           return delete(store,key);
        else
           return delete(back,key);
    }
    boolean delete(Map store,String key) throws InterruptedException {
        if(doEvents(ICache.METHOD_NAME_DELETE,key,null)){

            synchronized (store){
                return store.remove(key)==null?false:true;
            }
        }
        return false;
    }

    @Override
    public boolean clear(boolean isback){
        if(!isback)
            return clear(store);
        else
            return clear(back);
    }
    boolean clear(Map store) {
        synchronized (store){
            store.clear();
        }
        return true;
    }

    public Object get(boolean isback,String key){
        if(!isback)
            return get(store,key);
        else
            return get(back,key);
    }

    public Object get(Map store,String key){
        return store.get(key);
    }
    public List getKeys(boolean isback){
        if(!isback)
            return getKeys(store);
        else
            return getKeys(back);
    }

    @Override
    public boolean switchStore() {
        synchronized (this){
            Map tem = store;
            store=back;
            back=tem;
            return true;
        }//To change body of implemented methods use File | Settings | File Templates.
    }

    public List getKeys(Map store){
        synchronized (store){
            List list = new LinkedList();
            list.addAll(store.keySet());
            return list;
        }
    }

    @Override
    public Object[] query(boolean isback,QueryCondition cnd){
        if(!isback)
            return query(store,cnd);
        else
            return query(back,cnd);
    }
    public Object[] query(Map store,QueryCondition cnd) {
        if(null != cnd){
            return cnd.query(store);
        }
        return null;
    }


    @Override
    public void addListener(ICacheListener cacheListener) {
        listeners.add(cacheListener);
    }

    @Override
    public synchronized boolean existListObjectByUnique(String key,Object unique, Object obj) {
        List li = (List)store.get(key);
        if(null != li){
                for (int i = 0; i < li.size(); i++) {
                    Object o = li.get(i);
                    Object o1 = ObjectUtils.getValueByPath(o, (String)unique);
                    if (null != o1 && o1.equals(obj)) {
                        return true;
                    }
                }
        }
        return false;
    }

/*
    @Override
    public void addEvent(ICacheEvent cacheEvent) {
        events.add(cacheEvent);
    }
*/

    @Override
    public int getSize(boolean isback){
        if(!isback)
            return getSize(store);
        else
            return getSize(back);
    }
    int getSize(Map store) {
        synchronized (store){
            return store.size();
        }
    }

    @Override
    public String getId() {
        return key;
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        String op = (String)input.get("op");
        String back = (String)input.get("isback");
        boolean isback=false;
        if(StringUtils.isNotBlank(back))
            isback=StringUtils.isTrue(back);
        String key = (String)input.get("key");
        String replace = (String)input.get("replace");
        Object value = input.get("value");
        if(StringUtils.isNotBlank(op) ){
            if(op.equals("add")){

                if(StringUtils.isNotBlank(key) && null != value){
                    add(isback,key,value);
                    return value;
                }else{
                    return null;
                }
            }else if(op.equals("addList")){

                Map  group = (Map)input.get("group");
                String  unique = (String)input.get("unique");
                Object lim = input.get("limit");
                int limit=0;
                if(null != lim && StringUtils.isNotBlank(lim)){
                    if(lim instanceof String) {
                        limit = Integer.valueOf((String)lim);
                    }
                    if(lim instanceof Integer){
                        limit = (Integer)lim;
                    }
                }
                boolean b = addList(isback,key,value,unique,group,limit,StringUtils.isTrue(replace));
                if(b)
                    return value;
                else
                    return null;
            }else if(op.equals("get")){

                Object obj = null;
                if(StringUtils.isNotBlank(key)){
                    obj= get(isback,key);
                    /*if(cache.equals("user_rule")){
                        log.error(cache);
                    }*/
                }
                else if(!input.containsKey("key"))
                    obj= getKeys(isback);
                else
                    throw new Exception("cache ["+input.get("cache")+"] get key ["+key+"].");


                return obj;
            }else if(op.equals("exist")){
                return isExist(isback,key);
            }else if(op.equals("del")){
                if(StringUtils.isNotBlank(key)){
                    delete(isback,key);
                }else{
                    clear(isback);
                }

            }else if(op.equals("size")){
                return getSize(isback);
            }else if(op.equals("switch")){
                return switchStore();
            }else{
                throw new RuntimeException("now not support the cache operation["+op+"]");
            }
        }
        return null;
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResultCheck checkReturn(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return new ResultCheck(true,ret);  //To change body of implemented methods use File | Settings | File Templates.
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
