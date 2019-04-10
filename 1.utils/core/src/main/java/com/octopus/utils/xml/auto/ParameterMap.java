package com.octopus.utils.xml.auto;

import com.octopus.utils.thread.ExecutorUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: Administrator
 * Date: 15-4-2
 * Time: 上午9:58
 */
public class ParameterMap extends ConcurrentHashMap{
    static transient Log log = LogFactory.getLog(ParameterMap.class);
    //static List<String> threadList = Collections.synchronizedList(new LinkedList());;

    public synchronized Object put(Object k,Object v){
        if(null != k && null != v) {
        /*if(Thread.activeCount()<threadList.size()){
            gc();
        }*/

            String threadName = Thread.currentThread().getName();
            super.put(((String) k).concat("[").concat(threadName).concat("]"), v);
            /*if (!threadList.contains(threadName))
                threadList.add(threadName);*/
            return v;
        }else{
            return null;
        }
    }
    public synchronized Object putGlobal(String key,Object v){
        super.put(key,v);
        return v;
    }
    public synchronized Object getGlobal(String key){
        return super.get(key);
    }
    public synchronized void gc(){
        /*Thread[] ts = ExecutorUtils.findAllThreads();
        if(null != ts){
            String k=null;
            boolean b=false;
            List<String> del = new ArrayList<String>();
            synchronized (threadList){
                for(int i=threadList.size()-1;i>=0;i--){
                    k = threadList.get(i);
                    b=false;
                    for(Thread t:ts){
                        if(k.equals(t.getName())){
                            b=true;
                           break;
                        }
                    }
                    if(!b){
                        del.add(k);
                    }
                }
                if(del.size()>0){
                    for(String s:del){
                        threadList.remove(s);
                    }
                    Iterator es = this.keySet().iterator();
                    while(es.hasNext()){
                        String key = (String)es.next();
                        for(String old:del){
                            if(key.contains("[".concat(old).concat("]"))){
                                remove(key);
                                break;
                            }
                        }
                    }
                    //Runtime.getRuntime().gc();
                }
            }

        }*/
    }
    public synchronized Object getSub(Object k){
        String threadName = ((String)k).concat("[").concat(Thread.currentThread().getName());
        Iterator its = super.keySet().iterator();
        while(its.hasNext()){
            String key = ((String)its.next());
            if(key.indexOf(threadName)>=0){
                return super.get(key);
            }
        }
        return null;
    }
    @Override
    public synchronized Object get(Object k){
        String threadName = Thread.currentThread().getName();
        Object o = super.get(((String)k).concat("[").concat(threadName).concat("]"));
        if(null !=o )return o;
        //get self parent value
        if(ExecutorUtils.isSubThread(threadName)){
            Object parent = getParent((String)k);
            if(null != parent){
                return parent;
            }
        }
        //get main thread value
        o = super.get(k);
        if(null == o){

        }
        return o;
    }

    public synchronized Object removeSub(Object k){
        String threadName = ((String)k).concat("[").concat(Thread.currentThread().getName());
        Iterator its = super.keySet().iterator();
        while(its.hasNext()){
            String key = ((String)its.next());
            if(key.indexOf(threadName)>=0){
                Object v= super.get(key);
                super.remove(key);
                return v;
            }
        }
        return null;
    }
    @Override
    public synchronized Object remove(Object k){
        Object o = super.remove(((String)k).concat("[").concat(Thread.currentThread().getName()).concat("]"));
        if(null != o)return true;
        if(null == o){
            o = super.remove(k);
            if(null != o)
                return true;
        }
        return false;
    }
    public synchronized Object remove(String threadName,Object k){
        Object o = super.remove(((String)k).concat("[").concat(threadName).concat("]"));
        if(null != o)return true;
        if(null == o){
            o = super.remove(k);
            if(null != o)
                return true;
        }
        return false;
    }
    @Override
    public boolean containsKey(Object key){
        String threadName = Thread.currentThread().getName();
        boolean  b = super.containsKey(((String)key).concat("[").concat(threadName).concat("]"));
        if(b)return true;
        if(ExecutorUtils.isSubThread(threadName)){
            Object parent = getParent((String)key);
            if(null != parent)
                return true;
            //return super.containsKey(key);
        }
        return super.containsKey(key);

    }
    Object getParent(String key){
        String s = Thread.currentThread().getName();
        if(s.contains(".")){
            Object obj = null;
            String t = s;
            while(t.contains(".")){
                t = t.substring(0,t.lastIndexOf("."));
                obj = super.get(key.concat("[").concat(t).concat("]"));
                if(null != obj)
                    return obj;
            }
        }
        return null;
    }
}
