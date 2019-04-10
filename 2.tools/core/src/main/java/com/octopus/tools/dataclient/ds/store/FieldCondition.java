package com.octopus.tools.dataclient.ds.store;


import com.octopus.tools.dataclient.ds.field.FieldContainer;
import com.octopus.tools.dataclient.ds.field.FieldDef;
import com.octopus.tools.dataclient.ds.field.FieldType;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.time.DateTimeUtils;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;


/**
 * User: wf
 * Date: 2008-8-26
 * Time: 11:14:34
 */
public class FieldCondition implements Serializable{
    public static final int EQUAL = 1;
    public static final String STR_EQUAL = "=";
    public static final int BETWEEN = 2;
    public static final int LIKE = 3;
    public static final int EQUAL_MORE = 4;
    public static final int MORE = 5;
    public static final int EQUAL_LESS = 6;
    public static final int LESS = 7;
    public static final String STR_LESS = "<";
    public static final int IN =8;

    public static final char splitchar = 1;
    int startIndex,endIndex;

    private FieldDef field;
    private ValueCond valueCond;
    private RowCond rowIndexCond;
    private RowCond rowCount;
    private FieldDef relField;
    private String tableName;
    public FieldCondition(){}

    public FieldCondition(String db,String cond,Object para) throws Exception {
        if(StringUtils.isNotBlank(db)){
            setTableName(db);
        }
        if(cond.contains(STR_EQUAL)){
            int n= cond.indexOf(STR_EQUAL);
            String fn = cond.substring(0,n).trim();
            FieldDef fd = FieldContainer.getField(fn);
            if(null == fd)throw new Exception("not find FieldDef ["+fn+"] in dictionary");
            field=fd;
            Object v = cond.substring(n+STR_EQUAL.length()).trim();
            if(((String)v).startsWith(":")){
                if(null != para){
                    if(para instanceof Map){
                        v = ((Map)para).get(((String)v).substring(1));
                    }
                }
            }
            valueCond = new ValueCond(v,EQUAL);
        }else if(cond.contains(STR_LESS)){
            int n= cond.indexOf(STR_LESS);
            String fn = cond.substring(0,n).trim();
            FieldDef fd = FieldContainer.getField(fn);
            if(null == fd)throw new Exception("not find FieldDef ["+fn+"] in dictionary");
            field=fd;
            Object v = cond.substring(n+STR_LESS.length()).trim();
            if(((String)v).startsWith(":")){
                if(null != para){
                    if(para instanceof Map){
                        v = ((Map)para).get(((String)v).substring(1));
                    }
                }
            }
            valueCond = new ValueCond(v,LESS);
        }else{
            throw new Exception("now not support fieldCondition String["+cond+"]");
        }
    }

    public String toString(){
        if(valueCond.condType==EQUAL){
            return field.getFieldCode()+STR_EQUAL+"'"+valueCond.getPar1()+"'";
        }
        return "";
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public void setEndIndex(int endIndex) {
        this.endIndex = endIndex;
    }

    public FieldCondition(FieldDef field) {
        this.field =  field;
    }
    
    public FieldDef getField(){
        return field;
    }

    public void setField(FieldDef field){
        this.field = field;
    }

    public void setValueCond(Object value,int condType){
        if(!FieldType.getTypeClass(field.getFieldType()).getName().equals(value.getClass().getName())){
            throw new IllegalArgumentException(value.getClass() +" and Field's"+field.getClass()+" is not matching ");
        }
        valueCond=new ValueCond(value,condType);
    }
    public void setValueCond(Object afterValue,Object beforeValue,int condType){
        if(!FieldType.getTypeClass(field.getFieldType()).getName().equals(afterValue.getClass().getName())){
            throw new IllegalArgumentException(afterValue.getClass() +" and Field's"+field.getClass()+" is not matching ");
        }
        if(!FieldType.getTypeClass(field.getFieldType()).getName().equals(beforeValue.getClass().getName())){
            throw new IllegalArgumentException(beforeValue.getClass() +" and Field's"+field.getClass()+" is not matching ");
        }
        valueCond=new ValueCond(afterValue,beforeValue,condType);
    }

    public void setRowIndexCond(Long value,int condType){
        if(condType == LIKE){
            throw new UnsupportedOperationException("row query unsupport like flag.");
        }
        rowIndexCond = new RowCond(value,condType);
    }    

    public void setRowIndexCond(Long afterValue,Long beforeValue,int condType){
        if(condType == LIKE){
            throw new UnsupportedOperationException("row query unsupport like flag.");
        }
        rowIndexCond = new RowCond(afterValue,beforeValue,condType);
    }

    public void setRowCountCond(Long count,int condType){
        if(condType == LIKE){
            throw new UnsupportedOperationException("row query unsupport like flag.");
        }
        rowCount = new RowCond(count,condType);
    }
    
    public void setRowCountCound(Long start,Long end,int condType){
        if(condType == LIKE){
            throw new UnsupportedOperationException("row query unsupport like flag.");
        }
        rowCount = new RowCond(start,end,condType);
    }

    public void setRelField(FieldDef relField){
        this.relField = relField;
    }
    public FieldDef getRelField(){
        return this.relField;
    }
    public RowCond getRowCount(){
        return rowCount;
    }

    public ValueCond getValueCond() {
        return valueCond;
    }

    public RowCond getRowIndexCond() {
        return rowIndexCond;
    }

    public void clear(){
        valueCond = null;
        rowIndexCond = null;
        rowCount = null;
        this.relField = null;
    }

    public String toString(List pars){
        StringBuffer ret = new StringBuffer();
        if(null != valueCond ){
            if(FieldCondition.BETWEEN == valueCond.getCondType()){
                if(!"".equals(ret)) ret.append(" and ");
                ret.append(" "+ field.getFieldCode() + " between ? and ?");
                if(FieldType.FIELD_TYPE_DATE==field.getFieldType()){
                    pars.add(new Timestamp(((Date)valueCond.getPar1()).getTime()));
                    pars.add(new Timestamp(((Date)valueCond.getPar2()).getTime()));
                }else{
                    pars.add(valueCond.getPar1());
                    pars.add(valueCond.getPar2());
                }
            }
            if(FieldCondition.EQUAL == valueCond.getCondType()){
                if(!"".equals(ret)) ret.append(" and ");
                ret.append(" "+ field.getFieldCode() + " = ? ");
                if(FieldType.FIELD_TYPE_DATE==field.getFieldType()){
                    pars.add(new Timestamp(((Date)valueCond.getPar1()).getTime()));
                }else{
                    pars.add(valueCond.getPar1());
                }
            }
            if(FieldCondition.EQUAL_LESS == valueCond.getCondType()){
                if(!"".equals(ret)) ret.append(" and ");
                ret.append(" "+ field.getFieldCode() + " <= ? ");
                if(FieldType.FIELD_TYPE_DATE==field.getFieldType()){
                    pars.add(new Timestamp(((Date)valueCond.getPar1()).getTime()));
                }else{
                    pars.add(valueCond.getPar1());
                }
            }
            if(FieldCondition.EQUAL_MORE == valueCond.getCondType()){
                if(!"".equals(ret)) ret.append(" and ");
                ret.append(" "+ field.getFieldCode() + " >= ? ");
                if(FieldType.FIELD_TYPE_DATE==field.getFieldType()){
                    pars.add(new Timestamp(((Date)valueCond.getPar1()).getTime()));
                }else{
                    pars.add(valueCond.getPar1());
                }
            }
            if(FieldCondition.IN == valueCond.getCondType()){
                Object[] os = valueCond.getArray();
                String in="";
                for(int i=0;i<os.length;i++){
                    if(i==0){
                        in = os[i].toString();
                    }else{
                        in += "," + os[i].toString();
                    }
                }
                if(!"".equals(ret)) ret.append(" and ");
                ret.append(" "+ field.getFieldCode() + " in( "+in+" ) ");
            }
            if(FieldCondition.LESS == valueCond.getCondType()){
                if(!"".equals(ret)) ret.append(" and ");
                ret.append(" "+ field.getFieldCode() + " < ? ");
                if(FieldType.FIELD_TYPE_DATE==field.getFieldType()){
                    pars.add(new Timestamp(((Date)valueCond.getPar1()).getTime()));
                }else{
                    pars.add(valueCond.getPar1());
                }
            }
            if(FieldCondition.LIKE == valueCond.getCondType()){
                if(!"".equals(ret)) ret.append(" and ");
                ret.append(" "+ field.getFieldCode() + " like ? ");
                if(FieldType.FIELD_TYPE_DATE==field.getFieldType()){
                    pars.add(new Timestamp(((Date)valueCond.getPar1()).getTime()));
                }else{
                    pars.add("'%"+valueCond.getPar1().toString()+"%'");
                }
            }
            if(FieldCondition.MORE == valueCond.getCondType()){
                if(!"".equals(ret)) ret.append(" and ");
                ret.append(" "+ field.getFieldCode() + " > ? ");
                if(FieldType.FIELD_TYPE_DATE==field.getFieldType()){
                    //pars.add(new Timestamp(((Date)valueCond.getPar1()).getTime()));
                }else{
                    pars.add(valueCond.getPar1());
                }
            }
        }

        if(null !=rowIndexCond){
            if(FieldCondition.BETWEEN == rowIndexCond.getCondType()){
                if(!"".equals(ret)) ret.append(" and ");
                ret.append(" "+ field.getFieldCode() + " between ? and ?");
                pars.add(rowIndexCond.getPar1());
                pars.add(rowIndexCond.getPar2());
            }
            if(FieldCondition.EQUAL == rowIndexCond.getCondType()){
                if(!"".equals(ret)) ret.append(" and ");
                ret.append(" "+ field.getFieldCode() + " = ? ");
                pars.add(rowIndexCond.getPar1());
            }
            if(FieldCondition.EQUAL_LESS == rowIndexCond.getCondType()){
                if(!"".equals(ret)) ret.append(" and ");
                ret.append(" "+ field.getFieldCode() + " <= ? ");
                pars.add(rowIndexCond.getPar1());
            }
            if(FieldCondition.EQUAL_MORE == rowIndexCond.getCondType()){
                if(!"".equals(ret)) ret.append(" and ");
                ret.append(" "+ field.getFieldCode() + " >= ? ");
                pars.add(rowIndexCond.getPar1());
            }
            if(FieldCondition.IN == rowIndexCond.getCondType()){
                Object[] os = rowIndexCond.getArray();
                String in="";
                for(int i=0;i<os.length;i++){
                    if(i==0){
                        in = os[i].toString();
                    }else{
                        in += "," + os[i].toString();
                    }
                }
                if(!"".equals(ret)) ret.append(" and ");
                ret.append(" "+ field.getFieldCode() + " in( "+in+" ) ");
            }
            if(FieldCondition.LESS == rowIndexCond.getCondType()){
                if(!"".equals(ret)) ret.append(" and ");
                ret.append(" "+ field.getFieldCode() + " < ? ");
                pars.add(rowIndexCond.getPar1());
            }

            if(FieldCondition.MORE == rowIndexCond.getCondType()){
                if(!"".equals(ret)) ret.append(" and ");
                ret.append(" "+ field.getFieldCode() + " > ? ");
                pars.add(rowIndexCond.getPar1());

            }
        }
        if(null != relField){
            if(!"".equals(ret)) ret.append(" and ");
            ret.append(" " + field.getFieldCode() +" = "+relField.getFieldCode());
        }
        return ret.toString();
    }

    public String toTranString(){
        StringBuffer sb = new StringBuffer();
        sb.append(splitchar);
        sb.append(field.getFieldCode());
        sb.append(splitchar);
        if(null != valueCond ){
            if(FieldCondition.BETWEEN == valueCond.getCondType()){
                sb.append(splitchar);
                sb.append(BETWEEN);
                sb.append(splitchar);

                sb.append(splitchar);
                sb.append(valueCond.getPar1().toString());
                sb.append(splitchar);

                sb.append(splitchar);
                sb.append(valueCond.getPar2().toString());
                sb.append(splitchar);

            }
            if(FieldCondition.EQUAL == valueCond.getCondType()){
                sb.append(splitchar);
                sb.append(EQUAL);
                sb.append(splitchar);

                sb.append(splitchar);
                sb.append(valueCond.getPar1().toString());
                sb.append(splitchar);
            }
            if(FieldCondition.EQUAL_LESS == valueCond.getCondType()){
                sb.append(splitchar);
                sb.append(EQUAL_LESS);
                sb.append(splitchar);

                sb.append(splitchar);
                sb.append(valueCond.getPar1().toString());
                sb.append(splitchar);
            }
            if(FieldCondition.EQUAL_MORE == valueCond.getCondType()){
                sb.append(splitchar);
                sb.append(EQUAL_MORE);
                sb.append(splitchar);

                sb.append(splitchar);
                sb.append(valueCond.getPar1().toString());
                sb.append(splitchar);
            }
            if(FieldCondition.IN == valueCond.getCondType()){
                sb.append(splitchar);
                sb.append(IN);
                sb.append(splitchar);

                sb.append(splitchar);
                Object[] os = valueCond.getArray();
                String in="";
                for(int i=0;i<os.length;i++){
                    if(i==0){
                        in = os[i].toString();
                    }else{
                        in += "," + os[i].toString();
                    }
                }
                sb.append(in);
                sb.append(splitchar);
            }
            if(FieldCondition.LESS == valueCond.getCondType()){
                sb.append(splitchar);
                sb.append(LESS);
                sb.append(splitchar);

                sb.append(splitchar);
                sb.append(valueCond.getPar1().toString());
                sb.append(splitchar);
            }
            if(FieldCondition.LIKE == valueCond.getCondType()){
                sb.append(splitchar);
                sb.append(LIKE);
                sb.append(splitchar);

                sb.append(splitchar);
                sb.append(valueCond.getPar1().toString());
                sb.append(splitchar);
            }
            if(FieldCondition.MORE == valueCond.getCondType()){
                sb.append(splitchar);
                sb.append(MORE);
                sb.append(splitchar);

                sb.append(splitchar);
                sb.append(valueCond.getPar1().toString());
                sb.append(splitchar);
            }
        }
        
        return sb.toString();
    }

    public class RowCond implements Serializable{
        private Long par1=null;
        private Long par2=null;
        private int condType =0;
        private Long[] array=null;
        public RowCond(){

        }
        public RowCond(Long value,int type){
            this.par1 = value;
            this.condType = type;
        }
        public RowCond(Long[] value,int type){
            this.array = value;
            this.condType = type;
        }
        public RowCond(Long afterValue,Long beforeValue,int type){
            this.par1=afterValue;
            this.par2 = beforeValue;
            this.condType = type;
        }

        public Long[] getArray() {
            return array;
        }

        public void setArray(Long[] array) {
            this.array = array;
        }

        public int getCondType() {
            return condType;
        }

        public void setCondType(int condType) {
            this.condType = condType;
        }

        public Long getPar1() {
            return par1;
        }

        public void setPar1(Long par1) {
            this.par1 = par1;
        }

        public Long getPar2() {
            return par2;
        }

        public void setPar2(Long par2) {
            this.par2 = par2;
        }
    }
    
    public class ValueCond implements Serializable{
        private Object par1=null;
        private Object par2=null;
        private Object[] array = null;
        private int condType =0;

        public ValueCond(){

        }
        public String getCondTypeStr(){
            if(condType==FieldCondition.BETWEEN){
                if(field.getFieldType()==FieldType.FIELD_TYPE_STRING)
                    return " between '"+par1+"' and '"+par2+"'";
                else
                    return " between "+par1+" and "+par2;
            }
            if(condType==FieldCondition.EQUAL)
                if(field.getFieldType()==FieldType.FIELD_TYPE_STRING)
                    return " ='"+par1+"'";
                else
                    return " ="+par1;
            if(condType==FieldCondition.EQUAL_LESS){
                if(par1 instanceof Date)
                    return "<='"+ DateTimeUtils.DATA_FORMAT_YYYY_MM_DD_HH_MM_SS.format((Date)par1)+"'";
                else if(field.getFieldType()==FieldType.FIELD_TYPE_STRING)
                    return "<='"+ par1+"'";
                else
                    return "<="+ par1;
            }
            if(condType==FieldCondition.EQUAL_MORE)
                if(field.getFieldType()==FieldType.FIELD_TYPE_STRING)
                    return ">='"+par1+"'";
                else
                    return ">="+par1;
            if(condType==FieldCondition.LESS)
                if(field.getFieldType()==FieldType.FIELD_TYPE_STRING)
                return "<'"+par1+"'";
            else
                return "<"+par1;
            if(condType==FieldCondition.MORE)
                if(field.getFieldType()==FieldType.FIELD_TYPE_STRING)
                return ">'"+par1+"'";
            else
                return ">"+par1;
            return "";
        }
        public ValueCond(Object value,int type){
            this.par1 = value;
            this.condType = type;
        }
        public ValueCond(Object[] values,int type){
            this.array = values;
            this.condType = type;
        }
        public ValueCond(Object afterValue,Object beforeValue,int type){
            this.par1=afterValue;
            this.par2 = beforeValue;
            this.condType = type;
        }

        public Object[] getArray() {
            return array;
        }

        public void setArray(Object[] array) {
            this.array = array;
        }

        public int getCondType() {
            return condType;
        }

        public void setCondType(int condType) {
            this.condType = condType;
        }

        public Object getPar1() {
            return par1;
        }

        public void setPar1(Object par1) {
            this.par1 = par1;
        }

        public Object getPar2() {
            return par2;
        }

        public void setPar2(Object par2) {
            this.par2 = par2;
        }
    }

    /**
     * the value is maching the condition
     * @param value
     * @return
     */
    public boolean matchValueCond(FieldValue value){
        if(EQUAL == valueCond.condType){
            if(valueCond.getPar1().equals(value.getValue())) return true; else return false;
        }
        if(BETWEEN == valueCond.condType){
            if((compare(value.getValue(), valueCond.getPar1()) >= 0) && (compare(value.getValue(), valueCond.getPar2())   <  0)) return true;else return false;
        }
        if(LIKE == valueCond.condType){
            if(value.getValue().toString().indexOf(valueCond.getPar1().toString())>=0) return true;else return false;
        }
        if(EQUAL_MORE == valueCond.condType){
            if((compare(value.getValue(), valueCond.getPar1()) >= 0)) return true;else return false;
        }
        if(MORE == valueCond.condType){
           if((compare(value.getValue(), valueCond.getPar1()) > 0)) return true;else return false;
        }
        if(EQUAL_LESS == valueCond.condType){
           if((compare(value.getValue(), valueCond.getPar1()) <= 0)) return true;else return false;
        }
        if(LESS == valueCond.condType){
           if((compare(value.getValue(), valueCond.getPar1()) < 0)) return true;else return false;
        }
        if(IN ==valueCond.condType){
            Object[] os = valueCond.getArray();
            for(int i=0;i<os.length;i++){
                if(os[i].equals(value.getValue())) return true;
            }
            return false;
        }
        return false;
    }
    private int compare(Object value ,Object value2) {
        return ((Comparable)value).compareTo(value2);
    }


    public static FieldCondition newEqualCondition(FieldDef field,Object value){
        //return new FieldCondition(field,FieldCondition.EQUAL,value);
        return null;
    }
}
