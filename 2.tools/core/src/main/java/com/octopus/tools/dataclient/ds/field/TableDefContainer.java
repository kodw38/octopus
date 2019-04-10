package com.octopus.tools.dataclient.ds.field;

import com.octopus.utils.alone.ArrayUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * User: Administrator
 * Date: 14-10-22
 * Time: 下午11:16
 */
public class TableDefContainer {
    static HashMap<String,TableDef> tableDefMap = new HashMap<String, TableDef>();
    public static TableDef getTableDef(String tableName){
         return tableDefMap.get(tableName);
    }
    public static void addTableDef(TableDef tableDef){
        tableDefMap.put(tableDef.getName(),tableDef);
    }
    public static TableDef[] getBelongTables(FieldDef[] fs){
        if(ArrayUtils.isNotEmpty(fs)){
            //找树杈
            Iterator its= tableDefMap.keySet().iterator();
            boolean ismatch=true;
            while(its.hasNext()){
                ismatch=true;
                TableDef td = tableDefMap.get(its.next());
                FieldDef[] fds = td.getFieldDefs();
                for(FieldDef f:fs){
                    if(!isIN(f,fds)){
                        ismatch=false;
                        break;
                    }
                }
                if(ismatch)
                    return new TableDef[]{td};
            }
        }
        return null;
    }
    public static String getBelongTable(Map data)throws Exception{
        Iterator its= tableDefMap.keySet().iterator();
        boolean ismatch=true;
        while(its.hasNext()){
            ismatch=true;
            TableDef td = tableDefMap.get(its.next());
            FieldDef[] fds = td.getFieldDefs();
            Iterator ks = data.keySet().iterator();
            while(ks.hasNext()){
                if(!isIN((String)ks.next(),fds)){
                    ismatch=false;
                    break;
                }
            }
            if(ismatch)
                return td.getName();
        }
        throw new Exception("not find table");
    }
    static boolean isIN(FieldDef f,FieldDef[] fs){
        for(FieldDef s:fs){
            if(s.getFieldCode().equals(f.getFieldCode()))
                return true;
        }
        return false;
    }
    static boolean isIN(String f,FieldDef[] fs){
        for(FieldDef s:fs){
            if(s.getFieldCode().equals(f))
                return true;
        }
        return false;
    }
}
