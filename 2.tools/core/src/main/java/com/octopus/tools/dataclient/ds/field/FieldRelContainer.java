package com.octopus.tools.dataclient.ds.field;


import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * User: wf
 * Date: 2008-8-25
 * Time: 23:12:43
 */
public class FieldRelContainer {
    private static final Logger log = Logger.getLogger(FieldRelContainer.class);
    
    private static final HashMap<FieldDef,List<FieldRel>> relFieldMap = new HashMap();
    private static final HashMap queryMap = new HashMap();

    public static List<FieldRel> getRelField(FieldDef field){
        return (List)relFieldMap.get(field);
    }

    public static List getRelField(FieldDef field,int relType){
        if(null == field || relType<=0){
            throw new IllegalArgumentException("parameter can't is null.");    
        }
        String key = field.toString() + "|RelType:"+relType;
        if(queryMap.containsKey(key)){
            return (List)queryMap.get(key);
        }
        List li = (List)relFieldMap.get(field);
        List temList = new ArrayList();
        if(li.size()>0){
            FieldRel rel;
            for(int i=0;i<li.size();i++){
                rel = (FieldRel)li.get(i);
                if(rel.getRelType()==relType){
                    temList.add(rel.getRelField());
                }
            }

        }
        queryMap.put(key,temList);
        return temList;
    }

    public static void addRelField(FieldDef field, FieldRel relField){
        if(null == relField){
            throw new IllegalArgumentException("relFieldDef is can't null.");
        }
        if(relFieldMap.containsKey(field)){
            ((List)relFieldMap.get(field)).add(relField);
        }else{
            List list = new ArrayList();
            list.add(relField);
            relFieldMap.put(field,list);
        }
    }

    public static void addRelField(FieldDef field,FieldDef relField,int relType){
        if(null == relField){
            throw new IllegalArgumentException("relFieldDef is can't null.");
        }
        if(relType<=0){
            throw new IllegalArgumentException("relType is can't null.");
        }
        if(relFieldMap.containsKey(field)){
           ((List)relFieldMap.get(field)).add(new FieldRel(relField,relType));
        }else{
            List list = new ArrayList();
            list.add(new FieldRel(relField,relType));
            relFieldMap.put(field,list);
        }
    }

    public static void removeRelField(FieldDef field){
        relFieldMap.remove(field);
    }

    public static void clearRelFields(){
        relFieldMap.clear();
    }
    
}
