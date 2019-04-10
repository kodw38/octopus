package com.octopus.utils.exception;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;


/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2008</p>
 * <p>Company: AI(NanJing)</p>
 *
 * @author WangFeng
 * @version 3.0
 */
public final class ExceptionUtil {

	private static Logger log = Logger.getLogger(ExceptionUtil.class);

    public static Throwable getRootCase(Throwable a){
        Throwable r = null;
        for(r = a;r.getCause()!=null;r = r.getCause());
        return r;
    }
    public static String getRootString(Throwable a){
        return getString(getRootCase(a));
    }


    public static String getString(Throwable e){
        if(null != e) {
            StringBuffer sb = new StringBuffer("-" + e.toString() + "\n");
            StackTraceElement[] es = e.getStackTrace();
            if (null != es) {
                for (StackTraceElement s : es) {

                    sb.append(" ").append(s.toString()).append("\n");
                }
                Throwable a = e.getCause();
                if (null != a) {
                    sb.append(getString(a));
                }
            }
            return sb.toString();
        }else {
            return null;
        }
    }

    public static String getStackTraceString(StackTraceElement[] tes){
        StringWriter sw = new StringWriter();
        if (null != tes) {
            for (StackTraceElement s : tes) {
                sw.append(" ").append(s.toString()).append("\n");
            }
        }
        return sw.getBuffer().toString();
    }



    public static ErrMsg getMsg(Throwable e){
        String code="500";
        Field[] fs = e.getClass().getFields();
        String msg = ExceptionUtils.getRootCauseMessage(e);
        for(Field f:fs){
            String m = f.getName().toLowerCase();
            if(m.contains("code")){
                try {
                    Object o = f.get(e);
                    if(null != o){
                        code = o.toString();
                    }
                } catch (IllegalAccessException e1) {
                    e1.printStackTrace();
                }
            }
            if(m.contains("msg")||m.contains("message")){
                try {
                    Object o = f.get(e);
                    if(null !=o){
                        msg = o.toString();
                    }
                } catch (IllegalAccessException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return new ErrMsg(code,msg);
    }


}
