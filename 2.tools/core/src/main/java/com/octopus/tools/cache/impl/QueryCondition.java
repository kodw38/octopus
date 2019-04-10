package com.octopus.tools.cache.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * User: Administrator
 * Date: 14-9-17
 * Time: 下午5:54
 */
public class QueryCondition {
    static final int EQUAL = 1;
    static final int LIKE = 2;
    static final int START_WITH = 3;
    static final int END_WITH = 4;

    String key;
    int type;
    QueryCondition(int type,String key){
        this.key=key;
        this.type=type;
    }

    public QueryCondition getLinkCondition(String key){
        return new QueryCondition(LIKE,key);
    }
    public QueryCondition getEqualCondition(String key){
        return new QueryCondition(EQUAL,key);
    }
    public QueryCondition getStartWithCondition(String key){
        return new QueryCondition(START_WITH,key);
    }
    public QueryCondition getEndWithCondition(String key){
        return new QueryCondition(END_WITH,key);
    }

    public Object[] query(Map map){
        Iterator<String> its = map.keySet().iterator();
        List li = new ArrayList();
        while(its.hasNext()){
            String k = its.next();
            if(type==EQUAL){
                if(k.equals(key)){
                    li.add(map.get(k));
                }
            }
            if(type==LIKE){
                if(k.contains(key)){
                    li.add(map.get(k));
                }
            }
            if(type==START_WITH){
                if(k.startsWith(key)){
                    li.add(map.get(k));
                }
            }
            if(type==END_WITH){
                if(k.endsWith(key)){
                    li.add(map.get(k));
                }
            }
        }
        if(li.size()>0)
            return li.toArray();
        return null;
    }

}
