package com.octopus.tools.dataclient.ds.store;


import com.octopus.tools.dataclient.ds.field.FieldDef;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * User: wf
 * Date: 2008-8-25
 * Time: 23:42:30
 */
public class StoreContainer {
    
    private static Logger log = Logger.getLogger(StoreContainer.class);
    
    private HashMap recs = new HashMap();

    public  void addRecord(FieldDef field, FieldValue value){
        String fieldString = field.toString();
        synchronized(fieldString){
            if(null == value){
                throw new IllegalArgumentException(" parameter  can't is null.");
            }
            if(recs.containsKey(fieldString)){
                ((RowValueMap)recs.get(fieldString)).put(value);
            }else{
                RowValueMap rowValue = new RowValueMap();
                rowValue.put(value);
                recs.put(fieldString,rowValue);
            }
        }
    }

    public void updateRecord(FieldDef field, FieldValue old, FieldValue nnew){
         String fieldString = field.toString();
         synchronized(fieldString){
            ((RowValueMap)recs.get(fieldString)).update(old,nnew);
         }
    }

    public void removeRecord(FieldDef field, FieldValue old){
         String fieldString = field.toString();
         synchronized(fieldString){
            ((RowValueMap)recs.get(fieldString)).remove(old);
         }
    }

    public List getFieldValuesByCondition(FieldCondition cond){
        String fieldString = cond.getField().toString();
        synchronized(fieldString){
            RowValueMap rowValue = (RowValueMap)recs.get(fieldString);
            if(null == rowValue){
                throw new IllegalArgumentException("cache data has not the field info:"+fieldString);    
            }
            if(null != cond.getRowIndexCond() && cond.getRowIndexCond().getCondType() == FieldCondition.EQUAL){
                FieldValue rec = rowValue.getRecordValueByRowID(cond.getRowIndexCond().getPar1());
                if(null != rec){
                    List ret = new ArrayList();
                    ret.add(rec);
                    return CheckCount(ret,cond.getRowCount());
                }
            }
            if(null != cond.getRowIndexCond() && cond.getRowIndexCond().getCondType() == FieldCondition.BETWEEN){
                List recList = rowValue.getRecordValueByBetweenRow(cond.getRowIndexCond().getPar1(),cond.getRowIndexCond().getPar2());
                if(null != recList){
                    return CheckCount(recList,cond.getRowCount());
                }
            }
            if(null != cond.getRowIndexCond() && cond.getRowIndexCond().getCondType() == FieldCondition.IN){
                List recList = rowValue.getRecordValueByInRow(cond.getRowIndexCond().getArray());
                if(null != recList){
                    return CheckCount(recList,cond.getRowCount());
                }
            }
            if(null != cond.getRowIndexCond() && cond.getRowIndexCond().getCondType() == FieldCondition.EQUAL_LESS){
                List recList = rowValue.getRecordValueByRowEqualLess(cond.getRowIndexCond().getPar1());
                if(null != recList){
                    return CheckCount(recList,cond.getRowCount());
                }
            }
            if(null != cond.getRowIndexCond() && cond.getRowIndexCond().getCondType() == FieldCondition.EQUAL_MORE){
                List recList = rowValue.getRecordValueByRowEqualMore(cond.getRowIndexCond().getPar1());
                if(null != recList){
                    return CheckCount(recList,cond.getRowCount());
                }
            }

            if(null != cond.getRowIndexCond() && cond.getRowIndexCond().getCondType() == FieldCondition.LESS){
                List recList = rowValue.getRecordValueByRowLess(cond.getRowIndexCond().getPar1());
                if(null != recList){
                    return CheckCount(recList,cond.getRowCount());
                }
            }
            if(null != cond.getRowIndexCond() && cond.getRowIndexCond().getCondType() == FieldCondition.MORE){
                List recList = rowValue.getRecordValueByRowMore(cond.getRowIndexCond().getPar1());
                if(null != recList){
                    return CheckCount(recList,cond.getRowCount());
                }
            }

            if(null != cond.getValueCond() && cond.getValueCond().getCondType() == FieldCondition.EQUAL){
                List recList = rowValue.getRecordValueListByValue(cond.getValueCond().getPar1());
                if(null != recList){
                    return  CheckCount(recList,cond.getRowCount());
                }
            }
            if(null != cond.getValueCond() && cond.getValueCond().getCondType() == FieldCondition.LIKE){
                List recList = rowValue.getRecordValueListByLikeValue(cond.getValueCond().getPar1());
                if(null != recList){
                    return CheckCount(recList,cond.getRowCount());
                }
            }
            if(null != cond.getValueCond() && cond.getValueCond().getCondType() == FieldCondition.BETWEEN){
                List recList = rowValue.getRecordValueByBetweenValue(cond.getValueCond().getPar1(),cond.getValueCond().getPar2());
                if(null != recList){
                    return CheckCount(recList,cond.getRowCount());
                }
            }
            if(null != cond.getValueCond() && cond.getValueCond().getCondType() == FieldCondition.IN){
                List recList = rowValue.getRecordValueListByInValue(cond.getValueCond().getArray());
                if(null != recList){
                    return CheckCount(recList,cond.getRowCount());
                }
            }
            if(null != cond.getValueCond() && cond.getValueCond().getCondType() == FieldCondition.EQUAL_LESS){
                List recList = rowValue.getRecordValueByValueEqualLess(cond.getValueCond().getPar1());
                if(null != recList){
                    return CheckCount(recList,cond.getRowCount());
                }
            }
            if(null != cond.getValueCond() && cond.getValueCond().getCondType() == FieldCondition.EQUAL_MORE){
                List recList = rowValue.getRecordValueByValueEqualMore(cond.getValueCond().getPar1());
                if(null != recList){
                    return CheckCount(recList,cond.getRowCount());
                }
            }
            if(null != cond.getValueCond() && cond.getValueCond().getCondType() == FieldCondition.LESS){
                List recList = rowValue.getRecordValueByValueLess(cond.getValueCond().getPar1());
                if(null != recList){
                    return CheckCount(recList,cond.getRowCount());
                }
            }
            if(null != cond.getValueCond() && cond.getValueCond().getCondType() == FieldCondition.MORE){
                List recList = rowValue.getRecordValueByValueMore(cond.getValueCond().getPar1());
                if(null != recList){
                    return CheckCount(recList,cond.getRowCount());
                }
            }

            if(null != cond.getRelField()){
                RowValueMap relRowValue = (RowValueMap)recs.get(cond.getRelField().toString());
                List li = getSameValue(rowValue.getValueMap(),relRowValue.getValueMap());
                if(null != li){
                    return CheckCount(li,cond.getRowCount());
                }
            }
            
            return null;
        }
    }

    /*// only query one table data
    public TableValue getValueListByConditions(FieldCondition[] conds,FieldDef[] queryFields){
        TableValue ret = new TableValue();

        if(null != conds){
            FieldDef field;
            FieldCondition tempCond;
            String dbid=null;
            String tableName=null;
            for(int i=0;i<queryFields.length;i++){
                field = queryFields[i];
                if(dbid == null) dbid = field.getDatasource();
                if(tableName==null) tableName = field.getName();
                if(!dbid.equals(field.getDatasource()) || !tableName.equals(field.getName())){
                    throw new IllegalArgumentException(" only query one table data.");
                }
            }
            for(int i=0;i<conds.length;i++){
                tempCond = conds[i];
                if(!dbid.equals(tempCond.getField().getDatasource()) || !tableName.equals(tempCond.getField().getName())){
                    throw new IllegalArgumentException(" only query one table data.");
                }
            }
            TreeSet pkset = new TreeSet();
            Iterator tempIter;

            List minlist =null;
            List temp =null;
            int mincount  =0;
            int minindex = 0;
            for(int i=0;i<conds.length;i++){
                temp = getFieldValuesByCondition(conds[i]);
                if(null != temp){
                    if(mincount==0){
                        minlist = temp;
                        mincount = minlist.size();
                    }
                    if(temp.size()<mincount){
                        minlist = temp;
                        mincount = minlist.size();
                        minindex = i;
                    }
                }
            }
            if(mincount>0){

                boolean isin = false;
                for(int m=0;m<minlist.size();m++){
                    FieldValue fieldvalue = (FieldValue)minlist.get(m);
                    Long pkid = fieldvalue.getRowId();
                    isin = false;
                    RowValueMap value;
                    FieldValue relvalue;
                    for(int k=0;k<conds.length;k++){
                        if(minindex != k){
                            value =  (RowValueMap)recs.get(conds[k].getField().toString());
                            relvalue = value.getRecordValueByRowID(pkid);
                            boolean is =conds[k].matchValueCond(relvalue);
                            if(is){
                                isin = true;
                            }else{
                                isin = false;
                                break;
                            }
                        }
                    }
                    if(isin){
                        pkset.add(pkid);
                    }
                    
                }
            }

            tempIter = pkset.iterator();
            RowValueMap rowValue;
            List li =new ArrayList();
            Object[] value;
            Long pk;
            while(tempIter.hasNext()){
                value = new Object[queryFields.length];
                pk =  (Long)tempIter.next();
//                StringBuffer tem = new StringBuffer();
//                tem.append("pk:"+pk);
                for(int i=0;i<queryFields.length;i++){
                    rowValue = (RowValueMap)recs.get(queryFields[i].toString());
//                    tem.append(" "+queryFields[i].getFieldCode()+":"+rowValue.getRecordValueByRowID(pk));
                    if(null != rowValue && null != rowValue.getRecordValueByRowID(pk)){
                        value[i]=rowValue.getRecordValueByRowID(pk).getValue();
                    }else{
                        value[i]=null;
                    }
                }
//                System.out.println(tem.toString());
                li.add(value);
            }

            TableDef t = new TableDef();
            t.setDataSource(dbid);
            t.setName(tableName);
            ret.setTableDef(t);
            ret.setCacheMaxPk(getOneTableMaxRow(dbid,tableName));
            ret.setCacheMinPk(getOneTableMinRow(dbid,tableName));
            ret.setQueryCondition(conds);
            if(pkset.size()>0){
                ret.setResultMaxPk((Long)pkset.last());
                ret.setResultMinPk((Long)pkset.first());
            }else{
                ret.setResultMaxPk(new Long(0));
                ret.setResultMinPk(new Long(0));                
            }
            ret.setRecordValues(li);
            return ret;
        }

        return null;
    }*/

    /*public Object[] getOneTableRecordByRow(String dbsid,String table,Long rowid,String[] fieldcodes){
        HashMap fields = (HashMap)((HashMap) FieldContainer.getDbsFields().get(dbsid)).get(table);
        if(null != fields){
            Object[] ret = new Object[fieldcodes.length];
            FieldDef field;
            RowValueMap rowmap;
            for(int i=0;i<fieldcodes.length;i++){
                field = (FieldDef)fields.get(fieldcodes[i]);
                rowmap = (RowValueMap)recs.get(field.toString());
                ret[i]=rowmap.getRecordValueByRowID(rowid).getValue();
            }
            return ret;
        }
        return null;
    }

    public Long getOneTableMaxRow(String dbsid,String table){
        FieldDef pkField = FieldContainer.getPkField(dbsid,table);
        if(null == pkField ){
            throw new IllegalArgumentException("primery key column not find");
        }
        RowValueMap valuemap = (RowValueMap)recs.get(pkField.toString());
        return valuemap.getMaxRow();
    }
    public Long getOneTableMinRow(String dbsid,String table){
        FieldDef pkField = FieldContainer.getPkField(dbsid,table);
        RowValueMap valuemap = (RowValueMap)recs.get(pkField.toString());
        return valuemap.getMinRow();
    }
    public long getOneTableSize(String dbsid,String table){
        FieldDef pkField = FieldContainer.getPkField(dbsid,table);
        RowValueMap valuemap = (RowValueMap)recs.get(pkField.toString());
        return valuemap.getSize();
    }*/


    private List CheckCount(List set, FieldCondition.RowCond count){
        if(null == count || (count.getPar1().longValue()<=0 && count.getPar2().longValue()<=0) ){
            return set;    
        }
        if(count.getCondType() == FieldCondition.BETWEEN){
            if(count.getPar1().longValue()<=0){
                count.setPar1(new Long(0));
            }
            if(count.getPar2().longValue()<=0){
                count.setPar2(new Long(Long.MAX_VALUE));
            }
            if(set.size()>= count.getPar1().longValue() && set.size()<=count.getPar2().longValue()){
                return set;
            }
        }
        if(count.getCondType() == FieldCondition.EQUAL){
            if(set.size() == count.getPar1().longValue()){
                return set;
            }
        }
        return null;
        
    }

    private List getSameValue(TreeMap value,TreeMap relValue){
        Iterator its = relValue.keySet().iterator();
        List ret = new ArrayList();
        while(its.hasNext()){
            Object o = its.next();
            if(value.containsKey(o)){
                ret.addAll((Set)value.get(o));
            }
        }
        return ret;
    }

}
