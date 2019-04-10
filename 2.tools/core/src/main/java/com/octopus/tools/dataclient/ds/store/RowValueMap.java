package com.octopus.tools.dataclient.ds.store;

import java.util.*;

/**
 * User: wf
 * Date: 2008-8-26
 * Time: 14:14:54
 */
public class RowValueMap {
    private TreeMap valueMap = new TreeMap();
    private TreeMap rowIdMap = new TreeMap();

    public RowValueMap(){}

    public void put(FieldValue value){
        if(null == value.getRowId()){
            throw new IllegalArgumentException(" parameter RecordValue'RowId can't is null.");
        }
        if(null == value.getValue()){
            throw new IllegalArgumentException(" parameter RecordValue'Value can't is null.");
        }
        if(valueMap.containsKey(value.getValue())){
            ((List)valueMap.get(value.getValue())).add(value);
        }else{
            List li = new ArrayList();
            li.add(value);
            valueMap.put(value.getValue(),li);
        }
        if(rowIdMap.containsKey(value.getRowId())){
            throw new IllegalArgumentException(" row index:"+value.getRowId()+" data has exist.");
        }else{
            rowIdMap.put(value.getRowId(),value);
        }
    }

    void update(FieldValue old, FieldValue nnew){
        if(null == old.getRowId()){
            throw new IllegalArgumentException("updateing old RecordValue'RowId can't is null.");
        }
        if(null == old.getValue()){
            throw new IllegalArgumentException("updateing old RecordValue'Value can't is null.");
        }
        if(null == nnew.getRowId()){
            throw new IllegalArgumentException("updateing new RecordValue'RowId can't is null.");
        }
        if(null == nnew.getValue()){
            throw new IllegalArgumentException("updateing new RecordValue'Value can't is null.");
        }

        if(old.getRowId() == nnew.getRowId()){
            rowIdMap.put(old.getRowId(),nnew);
            ((List)valueMap.get(old.getValue())).remove(old);
            ((List)valueMap.get(old.getValue())).add(nnew);
        }else{
            remove(old);
            put(nnew);
        }
    }

    void remove(FieldValue value){
        if(null != value.getValue()){
            ((List)valueMap.get(value.getValue())).remove(value);
        }
        if(null == value.getRowId()){
            rowIdMap.remove(value.getRowId());      
        }
    }

    public FieldValue getRecordValueByRowID(Long rowId){
        return (FieldValue)rowIdMap.get(rowId);
    }

    public List getRecordValueByInRow(Long[] rowids){
        List li = new ArrayList();
        if(null != rowids){
            FieldValue value;
            for(int i=0;i<rowids.length;i++){
                value = (FieldValue)rowIdMap.get(rowids[i]);
                if(null != value){  li.add(value); }
            }
        }
        return li;
    }
    
    public Long getMaxRow(){
        return (Long)rowIdMap.lastKey();
    }

    public Long getMinRow(){
        return (Long)rowIdMap.firstKey();
    }

    public Object getMaxValue(){
        return valueMap.lastKey();
    }

    public Object getMinValue(){
        return valueMap.firstKey();    
    }

    public int getSize(){
        return rowIdMap.size();
    }

    public List getRecordValueListByValue(Object value){
        return (List)valueMap.get(value);
    }

    public List getRecordValueListByInValue(Object[] values){
        List ret = new ArrayList();
        List tem;
        if(null != values){
            for(int i=0;i<values.length;i++){
                tem = (List)valueMap.get(values[i]);
                if(null != tem){
                    ret.addAll(tem);
                }
            }
        }
        return ret;
    }

    public List getRecordValueByBetweenValue(Object after,Object before){
        SortedMap subMap = valueMap.subMap(after,before);
        List list = new ArrayList();
        Iterator cs = subMap.values().iterator();
        List tem;
        while(cs.hasNext()){
            tem = (List)cs.next();
            list.addAll(tem);
        }
        return list;            
    }    

    public List getRecordValueByBetweenRow(Long after,Long before){
        SortedMap subMap = rowIdMap.subMap(after,before);
        List list = new ArrayList();
        list.addAll(subMap.values());
        return list;
    }

    public List getRecordValueByRowEqualMore(Long rowid){
        Long max = this.getMaxRow();
        SortedMap subMap = rowIdMap.subMap(rowid,max);
        subMap.put(max,this.getRecordValueByRowID(max));
        List list = new ArrayList();
        list.addAll(subMap.values());
        return list;
    }
    public List getRecordValueByRowMore(Long rowid){
        Long max = this.getMaxRow();
        SortedMap subMap = rowIdMap.subMap(rowid,max);
        subMap.remove(rowid);
        subMap.put(max,this.getRecordValueByRowID(max));
        List list = new ArrayList();
        list.addAll(subMap.values());
        return list;
    }

    public List getRecordValueByRowEqualLess(Long rowid){
        Long min = this.getMinRow();
        SortedMap subMap = rowIdMap.subMap(min,rowid);
        subMap.put(rowid,this.getRecordValueByRowID(rowid));
        List list = new ArrayList();
        list.addAll(subMap.values());
        return list;
    }

    public List getRecordValueByRowLess(Long rowid){
        Long min = this.getMinRow();
        SortedMap subMap = rowIdMap.subMap(min,rowid);
        List list = new ArrayList();
        list.addAll(subMap.values());
        return list;
    }

    public List getRecordValueByValueEqualMore(Object value){
        Object max = this.getMaxValue();
        SortedMap subMap = rowIdMap.subMap(value,max);
        subMap.put(max,this.getRecordValueListByValue(max));
        List list = new ArrayList();
        Iterator cs = subMap.values().iterator();
        List tem;
        while(cs.hasNext()){
            tem = (List)cs.next();
            list.addAll(tem);
        }
        return list;
    }
    public List getRecordValueByValueMore(Object value){
        Object max = this.getMaxValue();
        SortedMap subMap = rowIdMap.subMap(value,max);
        subMap.remove(value);
        subMap.put(max,this.getRecordValueListByValue(max));
        List list = new ArrayList();
        Iterator cs = subMap.values().iterator();
        List tem;
        while(cs.hasNext()){
            tem = (List)cs.next();
            list.addAll(tem);
        }
        return list;
    }

    public List getRecordValueByValueEqualLess(Object value){
        Object min = this.getMinValue();
        SortedMap subMap = valueMap.subMap(min,value);
        subMap.put(value,this.getRecordValueListByValue(value));
        List list = new ArrayList();
        Iterator cs = subMap.values().iterator();
        List tem;
        while(cs.hasNext()){
            tem = (List)cs.next();
            list.addAll(tem);
        }
        return list;
    }

    public List getRecordValueByValueLess(Object value){
        List list = new ArrayList();
        Object min = this.getMinValue();
        SortedMap subMap = valueMap.subMap(min,value);
        Iterator cs = subMap.values().iterator();
        List tem;
        while(cs.hasNext()){
            tem = (List)cs.next();
            list.addAll(tem);
        }
        return list;
    }

    public List getRecordValueListByLikeValue(Object value){
        List list = new ArrayList();
        Iterator it = valueMap.keySet().iterator();
        while(it.hasNext()){
            Object o = it.next();
            if(o.toString().indexOf(value.toString())>=0){
                list.addAll((List)valueMap.get(o));
            }
        }
        return list;
    }



    TreeMap getValueMap() {
        return valueMap;
    }

    TreeMap getRowIdMap() {
        return rowIdMap;
    }
}
