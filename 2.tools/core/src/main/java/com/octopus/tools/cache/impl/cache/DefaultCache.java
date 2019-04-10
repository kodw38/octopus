package com.octopus.tools.cache.impl.cache;

import com.octopus.tools.cache.ICache;
import com.octopus.tools.cache.ICacheListener;
import com.octopus.tools.cache.impl.QueryCondition;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.NumberUtils;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 通用的内存数据存储
 * User: Administrator
 * Date: 14-9-17
 * Time: 下午2:27
 */
public class DefaultCache extends XMLDoObject implements ICache {
    transient static Log log = LogFactory.getLog(DefaultCache.class);
    String key;     //缓存名称
    int maxcount;     //缓存最大数量，-1为无底线，配置为一个实例的大小。
    double maxmemsize;   //缓存最大内存，单位MB，-1为无底线，配置为一个实例的大小。
    Map store;
    Map back;
/*
    ArrayList<ICacheEvent> events;
*/
    ArrayList<ICacheListener> listeners;

    public DefaultCache(XMLMakeup xml,XMLObject parent,Object[] containers) throws Exception {
        super(xml,parent,containers);
        String conf = xml.getProperties().getProperty("config");
        if(StringUtils.isNotBlank(conf)){
            Map m = StringUtils.convert2MapJSONObject(conf);
            if(null != m){
                String storetype = (String)m.get("storetype");
                String sort = (String)m.get("sort");
                if(StringUtils.isNotBlank(storetype)){
                    if(null == store) {
                        if(StringUtils.isNotBlank(sort)){
                            store = (Map)Class.forName(storetype).getConstructor(String.class).newInstance(sort);
                        }else {
                            store = (Map) Class.forName(storetype).newInstance();
                        }
                    }
                    if(null == back){
                        if(StringUtils.isNotBlank(sort)){
                            back = (Map)Class.forName(storetype).getConstructor(String.class).newInstance(sort);
                        }else {
                            back = (Map) Class.forName(storetype).newInstance();
                        }
                    }
                }
            }
        }
        if(store==null){
            store=new ConcurrentHashMap();
        }
        if(back==null){
            back=new ConcurrentHashMap();
        }
        if(null != listeners && listeners.size()>0 ) {
            for(ICacheListener o:listeners)
               System.out.println("init cache listener "+o.toString());
            ExecutorUtils.multiWorkSamePar(listeners.toArray(new ICacheListener[0]), "doListener", new Class[]{Object.class}, new Object[]{store});

        }
    }

    @Override
    public boolean init(String name, int maxCount, int memSizeMB) {
        key=name;
        maxcount=maxCount;
        maxmemsize=memSizeMB;
        return true;
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
    public boolean add(boolean isback,String key,Object data) throws InterruptedException {
        if(!isback)
           return add(store,key,data);
        else
            return add(back,key,data);
    }
    boolean add(Map store,String key,Object data) throws InterruptedException {
        if(doEvents(ICache.METHOD_NAME_ADD,key,data)){

            //if(store.containsKey(key)) return false;
            synchronized (store){
                store.put(key,data);
            }
            log.debug("add cache key: "+key+" data: "+data);
            return true;
        }
        return false;
    }
    @Override
    public boolean addList(boolean isback,String key, Object data, Object unique,Map group,int limitSize,boolean isreplace) throws Exception{
        if(!isback)
            return addList(store,key,data,unique,group,limitSize,isreplace);
        else
            return addList(back,key,data,unique,group,limitSize,isreplace);
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

    boolean addList(Map store,String key, Object data, Object unique,Map group,int limitsize,boolean isreplace) throws Exception {
        if(!store.containsKey(key)) store.put(key,new CopyOnWriteArrayList());
        if(null != unique){
            List li = (List)store.get(key);
            Map o2 =getObjects(unique, data);
            if(null != o2){
                for(int i=0;i<li.size();i++){
                    Object o = li.get(i);
                    Map o1 = getObjects(unique, o);
                    if(null != o1 && ArrayUtils.isEquals(o1, o2)){
                        if(isreplace){
                            li.remove(o);
                            break;
                        }else {
                            return false;
                        }
                    }
                }
            }else{
                return false;
            }
        }else{
            if(((List)store.get(key)).contains(data)) return false;
        }
        if(doEvents(ICache.METHOD_NAME_ADDLIST,key,data)){
            synchronized (store){
                if(null != group){
                    String by = (String)group.get("by");
                    String sum = (String)group.get("sum");
                    List<String> set = (List)group.get("set");
                    Object bv = ObjectUtils.getValueByPath(data,by);
                    Object add = ObjectUtils.getValueByPath(data,sum);
                    if(null != add){
                        List li = (List)store.get(key);
                        for(int i=0;i<li.size();i++){
                            Object o = li.get(i);
                            Object obv = ObjectUtils.getValueByPath(o,by);
                            bv = ObjectUtils.convertType(bv,obv.getClass().getName());
                            if(null != obv && obv.equals(bv)){
                                Object ov = ObjectUtils.getValueByPath(o,sum);
                                add = ObjectUtils.convertType(add,ov.getClass().getName());

                                ObjectUtils.setValueByPath(o,sum, NumberUtils.add(ov, add));
                                if(null != set && set.size()>0){
                                    for(String f:set){
                                        ObjectUtils.setValueByPath(o,f, ObjectUtils.getValueByPath(data,f));
                                    }
                                }
                                return true;
                            }
                        }
                        ((List)store.get(key)).add(data);
                        //System.out.println("=="+((List)store.get(key)).hashCode()+" "+key+" "+data);
                    }else{
                        return false;
                    }
                }else{
                    ((List)store.get(key)).add(data);
                    if(limitsize>0 && ((List)store.get(key)).size()>limitsize){
                        int n = ((List)store.get(key)).size()-limitsize;
                        while(n>0){
                            ((List)store.get(key)).remove(0);
                            n--;
                        }
                    }
                }
            }
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
    public boolean initAdd(Map store,String key, Object data) throws InterruptedException {
        if(store.containsKey(key)) return false;
        synchronized (store){
            store.put(key,data);
        }
        return true;
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
    public boolean update(Map store,String key,Object data) throws InterruptedException {
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
    public boolean delete(Map store,String key) throws InterruptedException {
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
    public boolean clear(Map store) {
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
    public boolean isExist(boolean isback,String key){
        if(!isback)
            return store.containsKey(key);
        else
            return back.containsKey(key);
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
    public List getvalues(boolean isback,List<String> fs){
        if(!isback)
            return getValues(store, fs);
        else
            return getValues(back, fs);
    }
    public List search(boolean isback,List<String> fields,String search)throws Exception{
        if(!isback)
            return searchValues(store, fields, search);
        else
            return searchValues(back, fields, search);
    }
    public List searchValues(Map data,List<String> fields,String search)throws Exception{
        Collection cs = data.values();
        Iterator its = cs.iterator();
        List ret = new ArrayList();
        while(its.hasNext()){
            Object o = its.next();
            if(o instanceof Map && null != fields && StringUtils.isNotBlank(search)){
                for(String k:fields) {
                    if(null != ((Map) o).get(k) && (((Map) o).get(k)).toString().indexOf(search)>0){
                        ret.add(o);
                    }
                }
            }else{
                throw new Exception("now not support.");
            }
        }
        if(ret.size()>0)
            return ret;
        return null;

    }
    public List getValues(Map data,List<String> fs){
        if(null != data && null != fs){
            List list = new ArrayList();
            Iterator its = data.keySet().iterator();
            while(its.hasNext()){
                String s = (String)its.next();
                if(data.get(s) instanceof Map){
                    HashMap m = new HashMap();
                    for(String f:fs){
                        Object v = ((Map) data.get(s)).get(f);
                        if(null != v){
                            m.put(f,v);
                        }
                    }
                    if(m.size()>0)
                        list.add(m);
                }
            }
            if(list.size()>0)
            return list;
        }
        return null;
    }
    public List getKeys(Map store){
        synchronized (store){
            List list = new LinkedList();
            Set ret = store.keySet();
            if(null != ret)
                list.addAll(ret);
            return list;
        }
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
    public boolean existListObjectByUnique(String key, Object unique, Object obj) {
        List li = (List)store.get(key);
        if(null != li){
            for(int i=0;i<li.size();i++){
                Object o = li.get(i);
                Object o1 = ObjectUtils.getValueByPath(o,(String)unique);
                if(null != o1 && o1.equals(obj)){
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
    public int getSize(Map store) {
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
        long l = System.currentTimeMillis();
        //try{
        String op = (String)input.get("op");
        String iback = (String)input.get("isback");
        boolean isback=false;
        if(StringUtils.isNotBlank(iback))
            isback=StringUtils.isTrue(iback);
        Object key = input.get("key");
        String replace = (String)input.get("replace");
        if(null != key && key instanceof String) key = ((String)key).trim();
        Object value = input.get("value");
        Map conds = (Map)input.get("conds");


        if(StringUtils.isNotBlank(op) ){
            if(op.equals("add")){
                if(StringUtils.isNotBlank(key) && null != value){
                    add(isback,(String)key,value);
                    return value;
                }else{
                    return null;
                }
            }else if(op.equals("addList")){
                Map group = (Map)input.get("group");
                Object unique = input.get("unique");
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
                boolean b = addList(isback,(String)key,value,unique,group,limit,StringUtils.isTrue(replace));
                if(b)
                    return value;
                else
                    return null;
            }else if("getList".equals(op)){
                String[] ks = StringUtils.split((String)key,",");
                if(null != ks) {
                    Map ret = new LinkedHashMap();
                    for(String k:ks) {
                        Object o=get(isback, k);
                        if(null != o && o instanceof List){
                            if(null != conds){
                                for(Object i:(List)o){
                                    if(i instanceof Map){
                                        if(ArrayUtils.contains((Map)i,conds)){
                                            if(null ==ret.get(k))ret.put(k,new LinkedList());
                                            ((List)ret.get(k)).add(i);
                                        }
                                    }
                                }
                            }else{
                                ret.put(k,o);
                            }
                        }
                    }
                    return ret;
                }
                return null;
            }else if("removeList".equals(op)){
                Object unique = input.get("unique");
                if(null == unique){
                    if(StringUtils.isNotBlank(key)){
                        return delete(isback,(String)key);
                    }
                }else{
                    List li = (List)store.get(key);
                    if(null !=li) {
                        Map o2 = getObjects(unique, value);
                        if (null != o2) {
                            for (int i = 0; i < li.size(); i++) {
                                Object o = li.get(i);
                                Map o1 = getObjects(unique, o);
                                if (null != o1 && ArrayUtils.isEquals(o1, o2)) {
                                    li.remove(o);
                                    return true;
                                }
                            }
                        } else {
                            return false;
                        }
                    }else{
                        return false;
                    }
                }
            }else if(op.equals("get")){

                Object obj = null;
                if(StringUtils.isNotBlank(key)){
                    if(key instanceof String) {
                        obj = get(isback, (String)key);
                    }else if (key instanceof List){
                        obj = new LinkedList();
                        for(String s:(List<String>)key){
                            Object n = get(isback,s);
                            if(null != n){
                                ((List)obj).add(n);
                            }
                        }
                    }
                    /*if(cache.equals("user_rule")){
                        log.error(cache);
                    }*/
                }else if(!input.containsKey("key") && null != input.get("fields")){
                    obj = getvalues(isback,(List)input.get("fields"));
                }else if(!input.containsKey("key"))
                    obj= getKeys(isback);
                else
                    throw new Exception(xmlid+" cache ["+input.get("cache")+"] get key ["+key+"].");


                return obj;
            }else if("search".equalsIgnoreCase(op)){
                List<String> fields = (List)input.get("fields");
                String like = (String)input.get("key");
                return search(isback,fields,like);
            }else if(op.equals("getall")){
                if(!isback)
                    return store;
                else
                    return back;
            }else if(op.equals("exist")){
                return isExist(isback, (String)key);
            }else if(op.equals("del")){
                if(StringUtils.isNotBlank(key)){
                    return delete(isback,(String)key);
                }else{
                    return clear(isback);
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
        /*}finally {
            System.out.println(Thread.currentThread().getName()+" cache:"+(System.currentTimeMillis()-l));
        }*/
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
        if(null == ret)
            return new ResultCheck(false,ret);
        else
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
