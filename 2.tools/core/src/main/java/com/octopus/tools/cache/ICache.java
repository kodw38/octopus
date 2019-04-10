package com.octopus.tools.cache;

import com.octopus.tools.cache.impl.QueryCondition;

import java.util.Collection;
import java.util.Map;

/**
 * 缓存是针对系统的，是全局的数据。
 * User: Administrator
 * Date: 14-8-25
 * Time: 下午1:01
 */
public interface ICache {
    static final String METHOD_NAME_ADD="add";
    static final String METHOD_NAME_ADDLIST="addList";
    static final String METHOD_NAME_UPDATE="update";
    static final String METHOD_NAME_DELETE="delete";

    public boolean init(String name,int maxCount,int memSizeMB);

    public boolean add(boolean isback,String key,Object data) throws InterruptedException;

    public boolean addList(boolean isback,String key,Object data,Object unique,Map group,int limitSize,boolean isreplace)throws Exception;

    public boolean initAdd(boolean isback,String key,Object data) throws InterruptedException;

    public Object  get(boolean isback,String key);

    public Collection getKeys(boolean isback);

    public boolean switchStore();

    public boolean update(boolean isback,String key,Object data) throws InterruptedException;

    public boolean delete(boolean isback,String key) throws InterruptedException;

    public Object[] query(boolean isback,QueryCondition cnd);

    public boolean clear(boolean isback);

    public void addListener(ICacheListener cacheListener);

    public boolean existListObjectByUnique(String key,Object unique,Object obj);


/*
    public void addEvent(ICacheEvent cacheEvent);
*/

    public int getSize(boolean isback);

    public String getId();

}
