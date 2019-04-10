package com.octopus.utils.pool;

import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * User: wf
 * Date: 2008-8-23
 * Time: 15:22:57
 */
public class ObjectPool {
    private static final Logger log = Logger.getLogger(ObjectPool.class);

    private int count = 10;

    private Set idleList = new HashSet();

    private Set activeList = new HashSet();

    private final Object lock = new Object();

    ObjectPool(int count,Object[] os){
        if(count ==0 || null == os || count != os.length){
            throw new IllegalArgumentException("Invalid constract parameters."); 
        }
        this.count = count;
        for(int i=0;i<os.length;i++){
            idleList.add(os[i]);
        }
    }

    public Object borrowObject() throws Exception{
        synchronized(lock){
            if(idleList.size()==0 && activeList.size()>0){
                log.info(" the pool objects are all borrowed. please wait...");
                while(idleList.size()==0 && activeList.size()>0){
                    lock.wait(500);
                }
            }
            if(idleList.size()>0){
                Object o = idleList.iterator().next();
                idleList.remove(o);
                
                activeList.add(o);
                lock.notifyAll();
                return o;
            }
        }
        return null;
    }    
    
    public void returnObject(Object obj) throws Exception{
        synchronized(lock){
           if(!activeList.contains(obj)){
               throw new IllegalArgumentException("return Object is not borrow Object.");
           }
           activeList.remove(obj);
           idleList.add(obj);
            lock.notifyAll();
        }
    }

    public void invalidateObject(Object obj) throws Exception{
        synchronized(lock){
            if(activeList.contains(obj)){
                log.info(obj.getClass().getName()+" is using ,please waiting return Object....");
                while(activeList.contains(obj)){
                    lock.wait(500);
                }
                log.info(obj.getClass().getName()+" has invalidate.");
            }
            if(idleList.contains(obj)){
                idleList.remove(obj);
                lock.notifyAll();
            }
        }
    }

    public void addObject(Object obj) throws Exception{
        synchronized(lock){
            if((activeList.size()+idleList.size())<count){
                idleList.add(obj);
                lock.notifyAll();
            }else{
                throw new IllegalArgumentException("do not add obj to the pool ,because the pool is expire.");
            }
        }
    }

    public int getIdleCount(){
        synchronized(lock){
            return idleList.size();
        }
    }

    public int getActiveCount(){
        synchronized(lock){
            return activeList.size();
        }
    }

    public int getCount(){
        return count;
    }

    public void clear() throws Exception{
        synchronized(lock){
            while(activeList.size()>0){
                log.info(activeList.size()+" object is using ,please waiting return all Object....");
                lock.wait(500);
            }
            idleList.clear();
            lock.notifyAll();
        }
    }
    
}
