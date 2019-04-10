package com.octopus.utils.alone.impl;

import java.util.*;

/**
 * User: Administrator
 * Date: 15-1-12
 * Time: 下午7:20
 */
public class MappingInfo {
    //映射关系 key为目标对象路径，vlaue为原对象路径
    public Map<String,String> pathMapping = new HashMap<String,String>();
    //目标结构对象
    List<StructInfo> targetStruct = new ArrayList();

    public List<StructInfo> getTargetStruct() {
        return targetStruct;
    }

    public Map<String, String> getPathMapping() {
        return pathMapping;
    }

    public void setPathMapping(Map<String, String> pathMapping) {
        this.pathMapping = sortMapping(pathMapping);
    }
    //按长度从长到短排序
    static Map<String,String> sortMapping(Map<String,String> map){
        List<Map.Entry<String,String>> mappingList = null;
        mappingList = new ArrayList<Map.Entry<String,String>>(map.entrySet());
        Collections.sort(mappingList, new Comparator<Map.Entry<String, String>>() {
            public int compare(Map.Entry<String, String> mapping1, Map.Entry<String, String> mapping2) {
                return mapping1.getValue().split("\\.").length > mapping2.getValue().split("\\.").length ? 0 : 1;
            }
        });
        LinkedHashMap ret = new LinkedHashMap();
        for(Map.Entry<String,String> entry:mappingList){
            ret.put(entry.getKey(),entry.getValue());
        }
        return ret;
    }

}
