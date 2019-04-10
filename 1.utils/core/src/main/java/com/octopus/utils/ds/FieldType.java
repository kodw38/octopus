package com.octopus.utils.ds;

import com.octopus.utils.alone.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * User: wf
 * Date: 2008-8-25
 * Time: 23:03:26
 */
public class FieldType {

    public static final String FIELD_TYPE_LONG = "L";
    public static final String FIELD_TYPE_INT = "I";
    public static final String FIELD_TYPE_DOUBLE = "D";
    public static final String FIELD_TYPE_STRING = "S";
    public static final String FIELD_TYPE_DATE = "T";
    public static final String FIELD_TYPE_BOOLEAN = "B";
    public static final String FIELD_TYPE_BIT = "b";
    public static final String FIELD_TYPE_STREAM = "E";

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd 24hh:MM:ss");

    public static String convertFrom(String type){
        if(StringUtils.isBlank(type) || "undefined".equals(type))return null;
        if(FIELD_TYPE_LONG.equals(type)){
            return Long.class.getName();
        }
        if(FIELD_TYPE_INT.equals(type)){
            return Integer.class.getName();
        }
        if(FIELD_TYPE_DOUBLE.equals(type)){
            return Double.class.getName();
        }
        if(FIELD_TYPE_STRING.equals(type)){
            return String.class.getName();
        }
        if(FIELD_TYPE_DATE.equals(type)){
            return Date.class.getName();
        }
        if(FIELD_TYPE_BOOLEAN.equals(type)){
            return Boolean.class.getName();
        }
        if(FIELD_TYPE_STREAM.equals(type)){
            return byte[].class.getName();
        }
        if(FIELD_TYPE_BIT.equals(type)){
            return String.class.getName();
        }
        throw new UnsupportedOperationException("unsupport db type "+type);

    }
    public static String convertDBFrom(String type,int len){
        if(StringUtils.isBlank(type) || "undefined".equals(type))return null;
        if(FIELD_TYPE_LONG.equals(type)){
            return "bigint";
        }
        if(FIELD_TYPE_INT.equals(type)){
            return "int";
        }
        if(FIELD_TYPE_DOUBLE.equals(type)){
            return "double";
        }
        if(FIELD_TYPE_STRING.equals(type)){
            if(len>65535){
                return "longtext";
            }else if(len>21845){
                return "text";
            }else {
                return "varchar";
            }
        }
        if(FIELD_TYPE_DATE.equals(type)){
            return "DateTime";
        }
        if(FIELD_TYPE_BOOLEAN.equals(type)){
            return "int";
        }
        if(FIELD_TYPE_BIT.equals(type)){
            return "bit";
        }
        if(FIELD_TYPE_STREAM.equals(type)){
            return "blob";
        }
        throw new UnsupportedOperationException("unsupport db type "+type);
    }
    public static Class getTypeClass(String type){
        if(type.equals(FIELD_TYPE_LONG)){
            return Long.class;
        }
        if(type.equals(FIELD_TYPE_DOUBLE)){
            return Double.class;
        }
        if(type.equals(FIELD_TYPE_STRING)){
            return String.class;
        }
        if(type.equals(FIELD_TYPE_DATE)){
            return Date.class;
        }
        if(type.equals(FIELD_TYPE_INT)){
            return Integer.class;
        }
        if(type.equals(FIELD_TYPE_STREAM)){
            return Byte[].class;
        }
        if(type.equals(FIELD_TYPE_BIT)){
            return String.class;
        }
        return null;
    }

    public static Object convertTypeObject(String o,String type)throws Exception{
        o = o.replaceAll("\\'","");
        if(FIELD_TYPE_LONG.equals(type)){
            return new Long(o);
        }
        if(FIELD_TYPE_DOUBLE.equals(type)){
            return new Double(o);
        }
        if(FIELD_TYPE_STRING.equals(type)){
            return o.trim();
        }
        if(FIELD_TYPE_DATE.equals(type)){
            return dateFormat.parse(o);
        }
        if(FIELD_TYPE_INT.equals(type)){
            return Integer.parseInt(o);
        }
        if(FIELD_TYPE_STREAM.equals(type)){
            return o.getBytes();
        }
        if(FIELD_TYPE_BIT.equals(type)){
            return String.valueOf(o);
        }
        throw new UnsupportedOperationException("no suppert data type:"+type+"[LONG = L,DOUBLE = D,STRING = S,INT = I,DATE = T]");
    }

}
