package com.octopus.tools.dataclient.ds.field;

import java.io.InputStream;
import java.sql.Types;
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
    public static final String FIELD_TYPE_DOUBLE = "F";
    public static final String FIELD_TYPE_STRING = "S";
    public static final String FIELD_TYPE_INPUTSTREAM = "M";
    public static final String FIELD_TYPE_DATE = "D";
    public static final String FIELD_TYPE_BOOLEAN = "B";
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd 24hh:MM:ss");


    public static String convertFrom(int type){
        if(Types.BLOB == type){
            return FIELD_TYPE_INPUTSTREAM;
        }
        if(Types.CHAR == type){
            return FIELD_TYPE_STRING;
        }
        if(Types.DATE == type){
            return FIELD_TYPE_DATE;
        }
        if(Types.DOUBLE == type){
            return FIELD_TYPE_DOUBLE;
        }
        if(Types.FLOAT == type){
            return FIELD_TYPE_DOUBLE;
        }
        if(Types.INTEGER == type){
            return FIELD_TYPE_LONG;
        }
        if(Types.LONGVARCHAR == type){
            return FIELD_TYPE_STRING;
        }
        if(Types.REAL == type){
            return FIELD_TYPE_DOUBLE;
        }
        if(Types.TIME == type){
            return FIELD_TYPE_DATE;
        }
        if(Types.TIMESTAMP == type){
            return FIELD_TYPE_DATE;
        }
        if(Types.VARCHAR == type){
            return FIELD_TYPE_STRING;
        }
        if(Types.NUMERIC == type){
            return FIELD_TYPE_LONG;
        }
        if(Types.INTEGER == type){
            return FIELD_TYPE_INT;
        }
        throw new UnsupportedOperationException("unsupport db type "+type);

    }

    public static Class getTypeClass(String type){
        if(type == FIELD_TYPE_LONG){
            return Long.class;
        }
        if(type == FIELD_TYPE_DOUBLE){
            return Double.class;
        }
        if(type == FIELD_TYPE_STRING){
            return String.class;
        }
        if(type == FIELD_TYPE_INPUTSTREAM){
            return InputStream.class;
        }
        if(type == FIELD_TYPE_DATE){
            return Date.class;
        }
        if(type == FIELD_TYPE_INT){
            return Integer.class;
        }
        return null;
    }

    public static Object convertTypeObject(String o,String type)throws Exception{
        o = o.replaceAll("\\'","");
        if(FIELD_TYPE_LONG==type){
            return new Long(o);
        }
        if(FIELD_TYPE_DOUBLE==type){
            return new Double(o);
        }
        if(FIELD_TYPE_STRING==type){
            return o.trim();
        }
        if(FIELD_TYPE_INPUTSTREAM==type){
            return o.trim();
        }
        if(FIELD_TYPE_DATE==type){
            return dateFormat.parse(o);
        }
        if(FIELD_TYPE_INT==type){
            return Integer.parseInt(o);
        }
        throw new UnsupportedOperationException("no suppert data type:"+type+"[LONG = 1,DOUBLE = 2,STRING = 3,INPUTSTREAM = 4,DATE = 5]");
    }

}
