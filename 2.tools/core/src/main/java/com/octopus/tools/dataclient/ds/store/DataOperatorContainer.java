package com.octopus.tools.dataclient.ds.store;

import com.octopus.tools.dataclient.ds.DelCnd;
import com.octopus.tools.dataclient.ds.UpdateData;
import com.octopus.tools.dataclient.ds.field.FieldContainer;
import com.octopus.tools.dataclient.ds.field.FieldDef;
import com.octopus.tools.dataclient.ds.field.FieldType;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.*;

/**
 * User: Administrator
 * Date: 14-10-22
 * Time: 下午4:56
 */
public class DataOperatorContainer {
    HashMap<FieldDef,List> addDataMap = new HashMap<FieldDef, List>();
    List<TableValue> addTableList = new ArrayList<TableValue>();

    List<UpdateData> updateDataMap = new ArrayList<UpdateData>();

    List<DelCnd> deleteDataList = new ArrayList<DelCnd>();

    public void addTablesValue(TableValue[] tvs){
        if(ArrayUtils.isNotEmpty(tvs)){
            for(TableValue tv:tvs)
                addTableList.add(tv);
        }
    }

    public boolean isEmpty(){
        if(addDataMap.size()==0 && addTableList.size()==0 && updateDataMap.size()==0 && deleteDataList.size()==0)
            return true;
        return false;
    }
    public List<TableValue> getAddTableList() {
        return addTableList;
    }

    public void setAddTableList(List<TableValue> addTableList) {
        this.addTableList = addTableList;
    }

    public void addData(FieldDef field,Object value){
        if(!addDataMap.containsKey(field)){
            addDataMap.put(field,new ArrayList());
        }
        if(!(value instanceof String && XMLParameter.isHasRetainChars((String)value)))
            addDataMap.get(field).add(value);
    }

    public void addData(Map jsonObject)throws Exception{
        Iterator ks = jsonObject.keySet().iterator();
        while(ks.hasNext()){
            String n = (String)ks.next();
            FieldDef f = FieldContainer.getField(n);
            if(null != f){
                if(f.getFieldType()== FieldType.FIELD_TYPE_BOOLEAN)
                    addData(f,Boolean.valueOf((String)jsonObject.get(n)));
                else if(f.getFieldType()==FieldType.FIELD_TYPE_DOUBLE)
                    if(null != jsonObject.get(n) && jsonObject.get(n) instanceof Double)
                        addData(f,(Double)jsonObject.get(n));
                    else if(null != jsonObject.get(n) && jsonObject.get(n) instanceof String){
                        addData(f,Double.valueOf((String)jsonObject.get(n)));
                    }else
                        addData(f,null);
                else if(f.getFieldType()==FieldType.FIELD_TYPE_LONG)
                    addData(f,Long.valueOf((String)jsonObject.get(n)));
                else if(f.getFieldType()==FieldType.FIELD_TYPE_STRING)
                    addData(f,(String)jsonObject.get(n));
                else if(f.getFieldType()==FieldType.FIELD_TYPE_INT)
                    addData(f,Integer.valueOf((String)jsonObject.get(n)));
                else if(f.getFieldType()==FieldType.FIELD_TYPE_DATE){
                    if(jsonObject.get(n) instanceof Long){
                        addData(f,new Date((Long)jsonObject.get(n)));
                    }else if(jsonObject.get(n) instanceof Date){
                        addData(f,jsonObject.get(n));
                    }else if(jsonObject.get(n) instanceof String){
                        addData(f,jsonObject.get(n));
                    }
                }
                else
                    throw new Exception("not support the data type["+f.getFieldType()+"]");
            }
        }
    }

    public void updateData(FieldCondition[] condition,FieldValue[] fieldValues,Object value){
        UpdateData ud = new UpdateData();
        ud.setConditions(condition);
        ud.setFieldValues(fieldValues);
        updateDataMap.add(ud);
    }

    public void deleteData(DelCnd condition){
        deleteDataList.add(condition);
    }

    public HashMap<FieldDef, List> getAddDataMap() {
        return addDataMap;
    }

    public List<UpdateData> getUpdateDataMap() {
        return updateDataMap;
    }

    public List<DelCnd> getDeleteDataList() {
        return deleteDataList;
    }
}
