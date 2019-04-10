package com.octopus.tools.resource;

import java.util.Map;

/**
 * 获取任何资源，db、file，ftp。并以配置的formate类返回。调用者无需关心数据存放在哪，是否用缓存。
 * User: Administrator
 * Date: 14-8-25
 * Time: 下午2:26
 */
public interface IResource {

    public Object add(String opId,Map[] data,Object env)throws Exception;
    public Object delete(String opId,Map cnd,Object env)throws Exception;
    public Object update(String opId,Map cnd,Map data,Object env)throws Exception;

    public Object rollbackAdd(String opId,Map[] data,Object env)throws Exception;
    public Object rollbackDelete(String opId,Map cnd,Object env)throws Exception;
    public Object rollbackUpdate(String opId,Map cnd,Map data,Object env)throws Exception;

    public Object query(String opId,Map cnd,int startIndex,int endIndex,Object env)throws Exception;

    public Object getMetaData(String opId,Map cnd,Object env)throws Exception;

}
