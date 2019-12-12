/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.octopus.utils.alone;

import com.octopus.utils.alone.impl.MappingInfo;
import com.octopus.utils.alone.impl.StructInfo;
import com.octopus.utils.cls.ClassUtils;
import com.octopus.utils.cls.POJOUtil;
import com.octopus.utils.exception.ISPException;
import com.octopus.utils.si.AgentMain;
import com.octopus.utils.si.JavaSizeOf;
import com.octopus.utils.si.jvm.JVMUtil;
import com.octopus.utils.time.DateTimeUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import com.octopus.utils.xml.desc.Desc;
import com.octopus.utils.zip.ZipUtil;
import com.sun.tools.attach.VirtualMachine;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.*;
import java.util.jar.JarOutputStream;


/**
 * <p>Operations on <code>Object</code>.</p>
 * 
 * <p>This class tries to handle <code>null</code> input gracefully.
 * An exception will generally not be thrown for a <code>null</code> input.
 * Each method documents its behaviour in more detail.</p>
 *
 * <p>#ThreadSafe#</p>
 * @author Apache Software Foundation
 * @author <a href="mailto:nissim@nksystems.com">Nissim Karpenstein</a>
 * @author <a href="mailto:janekdb@yahoo.co.uk">Janek Bogucki</a>
 * @author Daniel L. Rall
 * @author Gary Gregory
 * @author Mario Winterer
 * @author <a href="mailto:david@davidkarlsen.com">David J. M. Karlsen</a>
 * @since 1.0
 * @version $Id: ObjectUtils.java 1057434 2011-01-11 01:27:37Z niallp $
 */
//@Immutable
public class ObjectUtils {
    static transient Log log = LogFactory.getLog(ObjectUtils.class);
    public static String DEFAULT_DATE_VALUE="1970-01-01";
    /**
     * <p>Singleton used as a <code>null</code> placeholder where
     * <code>null</code> has another meaning.</p>
     *
     * <p>For example, in a <code>HashMap</code> the
     * {@link java.util.HashMap#get(Object)} method returns
     * <code>null</code> if the <code>Map</code> contains
     * <code>null</code> or if there is no matching key. The
     * <code>Null</code> placeholder can be used to distinguish between
     * these two cases.</p>
     *
     * <p>Another example is <code>Hashtable</code>, where <code>null</code>
     * cannot be stored.</p>
     *
     * <p>This instance is Serializable.</p>
     */
    public static final Null NULL = new Null();
    
    /**
     * <p><code>ObjectUtils</code> instances should NOT be constructed in
     * standard programming. Instead, the class should be used as
     * <code>ObjectUtils.defaultIfNull("a","b");</code>.</p>
     *
     * <p>This constructor is public to permit tools that require a JavaBean instance
     * to operate.</p>
     */
    public ObjectUtils() {
        super();
    }

    // Defaulting
    //-----------------------------------------------------------------------
    /**
     * <p>Returns a default value if the object passed is
     * <code>null</code>.</p>
     * 
     * <pre>
     * ObjectUtils.defaultIfNull(null, null)      = null
     * ObjectUtils.defaultIfNull(null, "")        = ""
     * ObjectUtils.defaultIfNull(null, "zz")      = "zz"
     * ObjectUtils.defaultIfNull("abc", *)        = "abc"
     * ObjectUtils.defaultIfNull(Boolean.TRUE, *) = Boolean.TRUE
     * </pre>
     *
     * @param object  the <code>Object</code> to test, may be <code>null</code>
     * @param defaultValue  the default value to return, may be <code>null</code>
     * @return <code>object</code> if it is not <code>null</code>, defaultValue otherwise
     */
    public static Object defaultIfNull(Object object, Object defaultValue) {
        return object != null ? object : defaultValue;
    }

    /**
     * <p>Compares two objects for equality, where either one or both
     * objects may be <code>null</code>.</p>
     *
     * <pre>
     * ObjectUtils.equals(null, null)                  = true
     * ObjectUtils.equals(null, "")                    = false
     * ObjectUtils.equals("", null)                    = false
     * ObjectUtils.equals("", "")                      = true
     * ObjectUtils.equals(Boolean.TRUE, null)          = false
     * ObjectUtils.equals(Boolean.TRUE, "true")        = false
     * ObjectUtils.equals(Boolean.TRUE, Boolean.TRUE)  = true
     * ObjectUtils.equals(Boolean.TRUE, Boolean.FALSE) = false
     * </pre>
     *
     * @param object1  the first object, may be <code>null</code>
     * @param object2  the second object, may be <code>null</code>
     * @return <code>true</code> if the values of both objects are the same
     */
    public static boolean equals(Object object1, Object object2) {
        if (object1 == object2) {
            return true;
        }
        if ((object1 == null) || (object2 == null)) {
            return false;
        }
        return object1.equals(object2);
    }

    /**
     * <p>Compares two objects for inequality, where either one or both
     * objects may be <code>null</code>.</p>
     *
     * <pre>
     * ObjectUtils.notEqual(null, null)                  = false
     * ObjectUtils.notEqual(null, "")                    = true
     * ObjectUtils.notEqual("", null)                    = true
     * ObjectUtils.notEqual("", "")                      = false
     * ObjectUtils.notEqual(Boolean.TRUE, null)          = true
     * ObjectUtils.notEqual(Boolean.TRUE, "true")        = true
     * ObjectUtils.notEqual(Boolean.TRUE, Boolean.TRUE)  = false
     * ObjectUtils.notEqual(Boolean.TRUE, Boolean.FALSE) = true
     * </pre>
     *
     * @param object1  the first object, may be <code>null</code>
     * @param object2  the second object, may be <code>null</code>
     * @return <code>false</code> if the values of both objects are the same
     * @since 2.6
     */
    public static boolean notEqual(Object object1, Object object2) {
        return ObjectUtils.equals(object1, object2) == false;
    }

    /**
     * <p>Gets the hash code of an object returning zero when the
     * object is <code>null</code>.</p>
     *
     * <pre>
     * ObjectUtils.hashCode(null)   = 0
     * ObjectUtils.hashCode(obj)    = obj.hashCode()
     * </pre>
     *
     * @param obj  the object to obtain the hash code of, may be <code>null</code>
     * @return the hash code of the object, or zero if null
     * @since 2.1
     */
    public static int hashCode(Object obj) {
        return (obj == null) ? 0 : obj.hashCode();
    }

    // Identity ToString
    //-----------------------------------------------------------------------
    /**
     * <p>Gets the toString that would be produced by <code>Object</code>
     * if a class did not override toString itself. <code>null</code>
     * will return <code>null</code>.</p>
     *
     * <pre>
     * ObjectUtils.identityToString(null)         = null
     * ObjectUtils.identityToString("")           = "java.lang.String@1e23"
     * ObjectUtils.identityToString(Boolean.TRUE) = "java.lang.Boolean@7fa"
     * </pre>
     *
     * @param object  the object to create a toString for, may be
     *  <code>null</code>
     * @return the default toString text, or <code>null</code> if
     *  <code>null</code> passed in
     */
    public static String identityToString(Object object) {
        if (object == null) {
            return null;
        }
        StringBuffer buffer = new StringBuffer();
        identityToString(buffer, object);
        return buffer.toString();
    }

    /**
     * <p>Appends the toString that would be produced by <code>Object</code>
     * if a class did not override toString itself. <code>null</code>
     * will throw a NullPointerException for either of the two parameters. </p>
     *
     * <pre>
     * ObjectUtils.identityToString(buf, "")            = buf.append("java.lang.String@1e23"
     * ObjectUtils.identityToString(buf, Boolean.TRUE)  = buf.append("java.lang.Boolean@7fa"
     * ObjectUtils.identityToString(buf, Boolean.TRUE)  = buf.append("java.lang.Boolean@7fa")
     * </pre>
     *
     * @param buffer  the buffer to append to
     * @param object  the object to create a toString for
     * @since 2.4
     */
    public static void identityToString(StringBuffer buffer, Object object) {
        if (object == null) {
            throw new NullPointerException("Cannot get the toString of a null identity");
        }
        buffer.append(object.getClass().getName())
              .append('@')
              .append(Integer.toHexString(System.identityHashCode(object)));
    }

    /**
     * <p>Appends the toString that would be produced by <code>Object</code>
     * if a class did not override toString itself. <code>null</code>
     * will return <code>null</code>.</p>
     *
     * <pre>
     * ObjectUtils.appendIdentityToString(*, null)            = null
     * ObjectUtils.appendIdentityToString(null, "")           = "java.lang.String@1e23"
     * ObjectUtils.appendIdentityToString(null, Boolean.TRUE) = "java.lang.Boolean@7fa"
     * ObjectUtils.appendIdentityToString(buf, Boolean.TRUE)  = buf.append("java.lang.Boolean@7fa")
     * </pre>
     *
     * @param buffer  the buffer to append to, may be <code>null</code>
     * @param object  the object to create a toString for, may be <code>null</code>
     * @return the default toString text, or <code>null</code> if
     *  <code>null</code> passed in
     * @since 2.0
     * @deprecated The design of this method is bad - see LANG-360. Instead, use identityToString(StringBuffer, Object).
     */
    public static StringBuffer appendIdentityToString(StringBuffer buffer, Object object) {
        if (object == null) {
            return null;
        }
        if (buffer == null) {
            buffer = new StringBuffer();
        }
        return buffer
            .append(object.getClass().getName())
            .append('@')
            .append(Integer.toHexString(System.identityHashCode(object)));
    }

    // ToString
    //-----------------------------------------------------------------------
    /**
     * <p>Gets the <code>toString</code> of an <code>Object</code> returning
     * an empty string ("") if <code>null</code> input.</p>
     * 
     * <pre>
     * ObjectUtils.toString(null)         = ""
     * ObjectUtils.toString("")           = ""
     * ObjectUtils.toString("bat")        = "bat"
     * ObjectUtils.toString(Boolean.TRUE) = "true"
     * </pre>
     * 
     * @see StringUtils#defaultString(String)
     * @see String#valueOf(Object)
     * @param obj  the Object to <code>toString</code>, may be null
     * @return the passed in Object's toString, or nullStr if <code>null</code> input
     * @since 2.0
     */
    public static String toString(Object obj) {
        if(null != obj){
            if(obj instanceof Map){
                return convertMap2String((Map)obj);
            }
        }
        return obj == null ? "" : obj.toString();
    }

    /**
     * <p>Gets the <code>toString</code> of an <code>Object</code> returning
     * a specified text if <code>null</code> input.</p>
     * 
     * <pre>
     * ObjectUtils.toString(null, null)           = null
     * ObjectUtils.toString(null, "null")         = "null"
     * ObjectUtils.toString("", "null")           = ""
     * ObjectUtils.toString("bat", "null")        = "bat"
     * ObjectUtils.toString(Boolean.TRUE, "null") = "true"
     * </pre>
     * 
     * @see StringUtils#defaultString(String,String)
     * @see String#valueOf(Object)
     * @param obj  the Object to <code>toString</code>, may be null
     * @param nullStr  the String to return if <code>null</code> input, may be null
     * @return the passed in Object's toString, or nullStr if <code>null</code> input
     * @since 2.0
     */
    public static String toString(Object obj, String nullStr) {
        return obj == null ? nullStr : obj.toString();
    }

    // Min/Max
    //-----------------------------------------------------------------------
    /**
     * Null safe comparison of Comparables.
     * 
     * @param c1  the first comparable, may be null
     * @param c2  the second comparable, may be null
     * @return
     *  <ul>
     *   <li>If both objects are non-null and unequal, the lesser object.
     *   <li>If both objects are non-null and equal, c1.
     *   <li>If one of the comparables is null, the non-null object.
     *   <li>If both the comparables are null, null is returned.
     *  </ul>
     */
    public static Object min(Comparable c1, Comparable c2) {
        return (compare(c1, c2, true) <= 0 ? c1 : c2);
    }

    /**
     * Null safe comparison of Comparables.
     * 
     * @param c1  the first comparable, may be null
     * @param c2  the second comparable, may be null
     * @return
     *  <ul>
     *   <li>If both objects are non-null and unequal, the greater object.
     *   <li>If both objects are non-null and equal, c1.
     *   <li>If one of the comparables is null, the non-null object.
     *   <li>If both the comparables are null, null is returned.
     *  </ul>
     */
    public static Object max(Comparable c1, Comparable c2) {
        return (compare(c1, c2, false) >= 0 ? c1 : c2);
    }

    /**
     * Null safe comparison of Comparables.
     * {@code null} is assumed to be less than a non-{@code null} value.
     * 
     * @param c1  the first comparable, may be null
     * @param c2  the second comparable, may be null
     * @return a negative value if c1 < c2, zero if c1 = c2
     * and a positive value if c1 > c2
     * @since 2.6
     */
    public static int compare(Comparable c1, Comparable c2) {
        return compare(c1, c2, false);
    }

    /**
     * Null safe comparison of Comparables.
     * 
     * @param c1  the first comparable, may be null
     * @param c2  the second comparable, may be null
     * @param nullGreater if true <code>null</code> is considered greater
     * than a Non-<code>null</code> value or if false <code>null</code> is
     * considered less than a Non-<code>null</code> value
     * @return a negative value if c1 < c2, zero if c1 = c2
     * and a positive value if c1 > c2
     * @see java.util.Comparator#compare(Object, Object)
     * @since 2.6
     */
    public static int compare(Comparable c1, Comparable c2, boolean nullGreater) {
        if (c1 == c2) {
            return 0;
        } else if (c1 == null) {
            return (nullGreater ? 1 : -1);
        } else if (c2 == null) {
            return (nullGreater ? -1 : 1);
        }
        return c1.compareTo(c2);
    }
    
    /**
     * Clone an object.
     * 
     * @param o the object to clone
     * @return the clone if the object implements {@link Cloneable} otherwise <code>null</code>
     * @since 2.6
     */
    public static Object clone(final Object o) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        if (o instanceof Cloneable) {
            final Object result;
            if (o.getClass().isArray()) {
                final Class componentType = o.getClass().getComponentType();
                if (!componentType.isPrimitive()) {
                    result = ((Object[])o).clone();
                } else {
                    int length = Array.getLength(o);
                    result = Array.newInstance(componentType, length);
                    while (length-- > 0) {
                        Array.set(result, length, Array.get(o, length));
                    }
                }
            } else {
                    result = ClassUtils.invokeMethod(o, "clone",null,null);
            }
            return result;
        }

        return null;
    }

    /**
     * Clone an object if possible. This method is similar to {@link #clone(Object)}, but will
     * return the provided instance as the return value instead of <code>null</code> if the instance
     * is not cloneable. This is more convenient if the caller uses different
     * implementations (e.g. of a service) and some of the implementations do not allow concurrent
     * processing or have state. In such cases the implementation can simply provide a proper
     * clone implementation and the caller's code does not have to change.
     * 
     * @param o the object to clone
     * @return the clone if the object implements {@link Cloneable} otherwise the object itself
     * @since 2.6
     */
    public static Object cloneIfPossible(final Object o) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        final Object clone = clone(o);
        return clone == null ? o : clone;
    }

    // Null
    //-----------------------------------------------------------------------
    /**
     * <p>Class used as a null placeholder where <code>null</code>
     * has another meaning.</p>
     *
     * <p>For example, in a <code>HashMap</code> the
     * {@link java.util.HashMap#get(Object)} method returns
     * <code>null</code> if the <code>Map</code> contains
     * <code>null</code> or if there is no matching key. The
     * <code>Null</code> placeholder can be used to distinguish between
     * these two cases.</p>
     *
     * <p>Another example is <code>Hashtable</code>, where <code>null</code>
     * cannot be stored.</p>
     */
    public static class Null implements Serializable {
        /**
         * Required for serialization support. Declare serialization compatibility with Commons Lang 1.0
         * 
         * @see java.io.Serializable
         */
        private static final long serialVersionUID = 7092611880189329093L;
        
        /**
         * Restricted constructor - singleton.
         */
        Null() {
            super();
        }
        
        /**
         * <p>Ensure singleton.</p>
         * 
         * @return the singleton value
         */
        private Object readResolve() {
            return ObjectUtils.NULL;
        }
    }

    public static boolean isNotNull(Object o){
        if(null == o) return false;
        else return true;
    }

    public static boolean isNotNull(Collection c){
        if(null == c || c.size()==0){
            return false;
        }else{
            return true;
        }
    }

    public static boolean isNotNull(Object[] os){
        if(null == os || os.length==0 || os[0]==null){
            return false;
        }else{
            return true;
        }
    }

    public static boolean isNotNull(String s){
        if(null == s || "".equals(s.trim())){
            return false;
        }else{
            return true;
        }
    }

    public static List getValueListFromMapDataByJsonMapExp(Map data,List list,char[][] begintag,char [][] endtag,IPointParse[] parses,char[][] stringBeginTag,Map rep,XMLObject obj)throws ISPException{
        List ret = new LinkedList();
        Object o = null;
        for(int i=0;i<list.size();i++){
            o=list.get(i);
            if(o instanceof String){
                ret.add(getValueFromExpress((String)o,data,begintag,endtag,parses,stringBeginTag,obj));
            }else if(o instanceof Map){
                ret.add(getValueMapFromMapDataByJsonMapExp(data, (Map) o, begintag, endtag,parses,stringBeginTag,rep,obj));
            }else if(o instanceof List){
                ret.add(getValueListFromMapDataByJsonMapExp(data, (List) o, begintag, endtag,parses,stringBeginTag,rep,obj));
            }else{
                ret.add(o);
            }
        }
        return ret;
    }

    public static Object getValueFromExpress(Object v,Map data,char[][] begintag,char [][] endtag,IPointParse[] parses,char[][] stringBeginTag,XMLObject obj)throws ISPException{
        /*if(((String) v).startsWith("$${")){
            //光变量,获取真实值
            //performance is very good long l= System.currentTimeMillis();
            v = ObjectUtils.getValueByPath(data,((String)v).substring(1));
            //System.out.println("getvalue: "+Thread.currentThread().getName()+" "+new Date().getTime()+" "+(System.currentTimeMillis()-l));
            //if(null != t)v = t;
        }else*/
        if(((String) v).startsWith("@{")){//从对象中获取属性@{xxobjname}.propertyName
            List li = StringUtils.getTagsNoMark((String)v,"@{","}");
            if(null !=li && li.size()>0){
                Object o = obj.getObjectById((String)li.get(0));
                if(null != o){
                    String p = ((String)v).substring(((String)v).lastIndexOf("}")+2);
                    v = ObjectUtils.getValueByPath(o, p);
                }
            }
        }else if(((String) v).startsWith("${") || ((String) v).startsWith("^${")){
            //光变量 ${rp}[#{len(${rp})-2}],如果运算方法在path内部由getValueByPath处理,这里只处理路径后面取数组的情况
            //判断可能是数组,把[]中的含有运算方法的进一步计算
            if(((String)v).charAt(((String)v).length()-1)==']'){
                int n = StringUtils.lastIndexOfStartPointOfMarchMark((String)v,"[","]");
                if(n>0) {
                    String e = ((String) v).substring(n);
                    if (isContainsExpressBeginFlag(e, begintag)) {
                        v = ((String) v).substring(0, n) + getExpressValueFromMap(e, begintag, endtag, data, parses,obj);
                    }
                }
            }
            //performance is very good long l= System.currentTimeMillis();
            Object t = ObjectUtils.getValueByPath(data,(String)v);
            //System.out.println("getvalue: "+Thread.currentThread().getName()+" "+new Date().getTime()+" "+(System.currentTimeMillis()-l));
            //if(null != t)v = t;
            if(null != t) {
                v = t;
            }else{
                if(ObjectUtils.existPath(data, (String) v)){
                    v=t;
                }
            }
        }else if(isContainsExpressBeginFlag((String) v, begintag)){
            //字符表达式
            String exv = (String)v;
            v = getExpressValueFromMap((String)v,begintag,endtag,data,parses,obj);
            if(null != v && v instanceof String){
                /*if(isContainsExpressBeginFlag((String)v,XMLParameter.FilterTagsBegin))
                   v=null;
               else */
                if(null == stringBeginTag || !isContainsExpressBeginFlag(exv,stringBeginTag)) {
                    if (((String) v).startsWith("[")) {
                        v = StringUtils.convert2ListJSONObject((String) v);
                    } else if (((String) v).startsWith("{")) {
                        v = StringUtils.convert2MapJSONObject((String) v);
                    }
                }
            }
        }
        return v;
    }

    static void getKeysValuesList(Map map,List ks,List vs){
        if(null != map){
            Iterator its = map.keySet().iterator();
            while(its.hasNext()){
                Object k = its.next();
                ks.add(k);
                vs.add(map.get(k));
            }
        }
    }
    /**
     * 从环境变量中解析参数map中的value的值，并返回
     * @param data
     * @param map
     * @return
     */
    static String[] excludeGet= new String[]{"exe_xml","exe_id","exe_error"};
    public static Map getValueMapFromMapDataByJsonMapExp(Map data,Map map,char[][] begintag,char [][] endtag,IPointParse[] parses,char[][] stringBeginTag,Map rep,XMLObject obj)throws ISPException{
        if(null == map)return null;
        if(Desc.isDescriptionService(map)){ // if service desc not convert variable
            return map;
        }
        Iterator its = map.keySet().iterator();
        HashMap ret = new LinkedHashMap();
        while(its.hasNext()){
            String k = (String)its.next();
            if(ArrayUtils.isInStringArray(excludeGet,k)) continue;
            Object v = map.get(k);
            if(v instanceof String){
                if(null != rep){
                    LinkedList os = new LinkedList();
                    LinkedList ns = new LinkedList();
                    getKeysValuesList(rep,os,ns);
                    v  = StringUtils.replace((String)v,os,ns);
                }
                v = getValueFromExpress((String)v,data,begintag,endtag,parses,stringBeginTag,obj);
            }else if(v instanceof Map){
                v = getValueMapFromMapDataByJsonMapExp(data, (Map) v, begintag, endtag,parses,stringBeginTag,rep,obj);
                Map t=((Map)v);
                //json 解析后的后序对象处理，json解析只能处理string，int，double类型的数据转换。对象类型的转换在此处理
                if(t.containsKey("declare") && null != t.get("declare") && StringUtils.isNotBlank(((Map)t.get("declare")).get("clazz")) ){
                    if(t.containsKey("data")) {
                        //通过设置的value map构造新对象
                        try {
                            v = setValue(Class.forName((String) ((Map) t.get("declare")).get("clazz")), t.get("data"), begintag);
                        } catch (Exception e) {
                            log.error("set " + ((Map) t.get("declare")).get("clazz") + " values: " + t.get("data") + " error:", e);
                        }
                    }else if(t.containsKey("mapping") && t.containsKey("src") && null != t.get("src") && null!=((Map) t.get("declare")).get("structure")){
                        //通过原对象的属性路径映射来构造新对象
                        /*
                          {
                            declare:{class:'',structure:[{name:'',type:'',isarray:'',children:[...]},...]},
                            mapping:{targetpath:srcpath,...}
                            src:,
                            target:,
                          }
                         */
                        try {
                            Object target = null;
                            Object src = t.get("src");
                            Map<String,String> mapping = (Map)t.get("mapping");
                            if (null != t.get("target")) {
                                target = t.get("target");
                            } else {
                                target = Class.forName((String) ((Map) t.get("declare")).get("clazz")).newInstance();
                            }
                            List<Map> list = (List)((Map) t.get("declare")).get("structure");
                            MappingInfo mi = convertMapToMappingInfo(list,mapping);
                            mapping(src, mi, target);
                            v = target;
                        }catch (Exception e){
                            log.error("mapping " + ((Map) t.get("declare")).get("clazz") + " error:", e);
                        }
                    }
                }
            }else if(v instanceof List){
                List l = (List)v;
                v = getValueListFromMapDataByJsonMapExp(data, l, begintag, endtag,parses,stringBeginTag,rep,obj);
            }
            //处理转移符号
            if(v instanceof String){
                if(((String)v).contains("\\n")){
                    v = StringUtils.replace((String)v,"\\n","\n");
                }
            }
            ret.put(k,v);
        }
        return ret;
    }

    /**
     * 根据mapping把一个对象的属性映射为一个map返回
     * @param obj
     * @param mapping
     * @return
     */
    public static Map getObjectMapping2Map(Object obj,Map mapping){
        Map t = new HashMap();
        MappingInfo mi = ObjectUtils.convertMapToMappingInfo(null, mapping);
        ObjectUtils.mapping(obj,mi,t);
        if(null != t && t.size()>0) {
            return t;
        }
        return null;
    }
    public static MappingInfo convertMapToMappingInfo(List<Map> structList,Map pathMapping){
        MappingInfo mi = new MappingInfo();
        if(null != pathMapping)
            mi.setPathMapping(pathMapping);
        if(null != structList) {
            for (Map ms : structList) {
                mi.getTargetStruct().add(convertMapToStructInfo(ms));
            }
        }
        return mi;
    }

    /**
     *
     * @param structJson
     * [
     *  {name:'',type:'java.util.HashMap',children:[]}
     * ]
     * @param mappingJson
     * {targetpath:srcpath}
     * @return
     */
    public static MappingInfo getMappingInfoFromJson(String structJson,String mappingJson){
        List strulist =null;
        if(structJson.charAt(0)=='[')
            strulist=StringUtils.convert2ListJSONObject(structJson);
        else {
            strulist = new ArrayList();
            Map m = StringUtils.convert2MapJSONObject(structJson);
            if(null != m)
                strulist.add(m);
        }
        Map pathMapping = StringUtils.convert2MapJSONObject(mappingJson);
        return convertMapToMappingInfo(strulist,pathMapping);
    }

    /**
     * map 中描述结构转换为structInfo对象
     * {
     *     name:'',type:'',isarray:'',children:[]
     * }
     * @param map
     * @return
     */
    static StructInfo convertMapToStructInfo(Map map){
        try {
            StructInfo root = new StructInfo();
            root.setName((String) map.get("name"));
            root.setType(Class.forName((String) map.get("type")));
            Object o =map.get("isarray");
            if(null != o){
                if(o instanceof Boolean){
                    root.setArray((Boolean)o);
                }else{
                    root.setArray(StringUtils.isTrue( map.get("isarray").toString()));
                }
            }else{
                root.setArray(false);
            }
            List<Map> cl = (List) map.get("children");
            if(null != cl && cl.size()>0){
                for(Map m:cl){
                    StructInfo c = convertMapToStructInfo(m);
                    if(null != c) {
                        root.getChildren().add(c);
                        c.setParent(root);
                    }
                }
            }
            return root;
        }catch (Exception e){
            log.error("parse declare structure error",e);
        }
        return null;
    }

    public static boolean isContainsExpressBeginFlag(String exp,char[][] begintag){
        if(null==exp || "".equals(exp) || begintag==null || begintag.length==0 )return false;
        try{
            char[] tmp= new char[1024];
            int tmpCur=0;
            int tmpCurRow=-1;
            for(int i=0;i<exp.length();i++){
                char c = exp.charAt(i);
                if(tmpCurRow==-1){
                    for(int j=0;j<begintag.length;j++){
                        if(begintag[j].length>tmpCur && begintag[j][tmpCur]==c){
                            tmp[tmpCur++]=c;
                            tmpCurRow=j;
                            if(begintag[tmpCurRow].length==tmpCur && isSamePre(tmp,tmpCur,begintag[j]) ){
                                return true;
                            }
                            break;
                        }

                    }
                }else{
                    if(begintag[tmpCurRow].length>tmpCur && begintag[tmpCurRow][tmpCur]==c){
                        tmp[tmpCur++]=c;
                        if(begintag[tmpCurRow].length==tmpCur && isSamePre(tmp,tmpCur,begintag[tmpCurRow])){
                            return true;
                        }
                    }else{
                        int j=0;
                        for(j=0;j<begintag.length;j++){
                            if(begintag[j].length>tmpCur && begintag[j][tmpCur]==c && isSamePre(tmp,tmpCur,begintag[j])){
                                tmp[tmpCur++]=c;
                                tmpCurRow=j;
                                if(begintag[tmpCurRow].length==tmpCur){
                                    return true;
                                }
                                break;
                            }

                        }
                        if(j>=begintag.length){
                            tmpCur=0;
                            tmpCurRow=-1;
                        }

                    }
                }
            }
            return false;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
    /**
     * 把exp中的表达式从map中获取值，逻辑运算并替换
     * @param exp
     * @param data
     * @return
     */
    public static Object getExpressValueFromMap(String exp,char[][] begintag,char [][] endtag,Map data,IPointParse[] parses,XMLObject obj)throws ISPException{
        /*long l=0;
        if(log.isInfoEnabled())
            l =System.currentTimeMillis();*/
        if(null==exp || "".equals(exp) || begintag==null || begintag.length==0|| endtag==null||endtag.length==0 || data==null)return exp;
        char [] target = new char[exp.length()];
        char[] tmp= new char[1024];
        //boolean isin=false;
        char[] expchars= null;
        Stack expStack = new Stack();
        Stack expcurStack = new Stack();
        Stack tagindex = new Stack();
        try{
            int targetCur=0;
            int curRow=-1;
            int tmpCur=0;
            int tmpCurRow=-1;
            int expcur= 0;
            for(int i=0;i<exp.length();i++){
                char c = exp.charAt(i);

                if(expchars==null && expcurStack.isEmpty()){

                    if(target.length<=targetCur){
                        target=StringUtils.expandCapacity(target,targetCur+1024);
                    }
                    target[targetCur++]=c;
                }
                if(expchars !=null){
                    if(tmpCurRow==-1){
                        for(int j=0;j<begintag.length;j++){
                            if(begintag[j].length>tmpCur && begintag[j][tmpCur]==c){
                                tmp[tmpCur++]=c;
                                tmpCurRow=j;
                                break;
                            }

                        }
                    }else{
                        if(begintag[tmpCurRow].length>tmpCur && begintag[tmpCurRow][tmpCur]==c){
                            tmp[tmpCur++]=c;
                        }else{
                            boolean ischg=false;
                            if(tmpCurRow<begintag.length){
                                for(int k=tmpCurRow+1;k<begintag.length;k++){
                                    if(begintag[k].length<tmpCur) break;
                                    int m=0;
                                    boolean ism=true;
                                    for(;m<tmpCur;m++){
                                        if(begintag[k][m]!=begintag[tmpCurRow][m]){
                                            ism=false;
                                            break;
                                        }
                                    }
                                    if(ism && begintag[k].length>tmpCur && begintag[k][tmpCur]==c){
                                        ischg=true;
                                        tmp[tmpCur++]=c;
                                        tmpCurRow=k;
                                        break;
                                    }
                                }
                            }
                            if(!ischg){
                                tmpCur=0;
                                tmpCurRow=-1;
                            }
                        }
                    }
                    if(expchars.length<=expcur+1){//fixed for expchars outof array 1024
                        expchars=StringUtils.expandCapacity(expchars,1024);
                    }
                    expchars[expcur++]=c;
                    if(tmpCurRow>=0 && tmpCur>0 && begintag[tmpCurRow].length==tmpCur){
                        expStack.push(expchars);
                        tagindex.push(curRow);
                        expcur-= begintag[tmpCurRow].length;
                        expcurStack.push(expcur);
                        curRow= tmpCurRow;
                        expchars = new char[1024];
                        expcur=0;
                        expchars = StringUtils.appendChar(expchars,expcur,begintag[curRow],begintag[curRow].length);
                        expcur+= begintag[curRow].length;
                        tmpCur=0;
                        tmpCurRow=-1;

                    }else if(curRow>=0 && endtag[curRow].length>tmpCur && endtag[curRow][tmpCur]==c){
                        tmp[tmpCur++]=c;
                        if(endtag[curRow].length==tmpCur){
                            String oneexp =new String(Arrays.copyOf(expchars,expcur));
                            String s= null;
                            //deal one expression
                            boolean ishastag=false;

                            for(int k=0;k<begintag.length;k++){
                                if(oneexp.substring(begintag[curRow].length,oneexp.length()-endtag[curRow].length).indexOf(new String(begintag[k]))>0){
                                    ishastag=true;
                                    break;
                                }
                            }

                            /*if(ishastag){
                                s = oneexp;
                            }else{*/
                                if(null != parses){
                                    s =parses[curRow].parse(oneexp,data,obj);
                                }else{
                                    s =String.valueOf(getValueByPath(data, oneexp));
                                }
                           // }
                            if(s==null)s="";
                            char[] cs =s.toCharArray();
                            if(!expStack.isEmpty()) {
                                expchars = (char[])expStack.pop();
                                expcur=(Integer)expcurStack.pop();
                                curRow = (Integer)tagindex.pop();
                                expchars=StringUtils.appendChar(expchars,expcur,cs,cs.length);
                                expcur+=cs.length;
                            }else{
                                target=StringUtils.appendChar(target,targetCur,cs,cs.length);
                                targetCur+=cs.length;
                                expchars=null;
                                expcur=0;
                                curRow=-1;

                            }
                            //isin=false;
                            tmpCur=0;
                        }
                    }
                }
                if(expchars==null && expcurStack.isEmpty()){
                    if(tmpCurRow==-1){
                        for(int j=0;j<begintag.length;j++){
                            if(begintag[j].length>tmpCur && begintag[j][tmpCur]==c){
                                tmp[tmpCur++]=c;
                                tmpCurRow=j;
                                if(begintag[tmpCurRow].length==tmpCur){
                                    curRow=tmpCurRow;
                                    tmpCurRow=-1;
                                    tmpCur=0;
                                }
                                break;
                            }

                        }
                    }else{
                        if(begintag[tmpCurRow].length>tmpCur && begintag[tmpCurRow][tmpCur]==c){
                            tmp[tmpCur++]=c;
                            if(begintag[tmpCurRow].length==tmpCur){
                                curRow=tmpCurRow;
                                tmpCurRow=-1;
                                tmpCur=0;
                            }
                        }else{
                            int j=0;
                            for(j=0;j<begintag.length;j++){
                                if(begintag[j].length>tmpCur && begintag[j][tmpCur]==c && isSamePre(tmp,tmpCur,begintag[j])){
                                    tmp[tmpCur++]=c;
                                    tmpCurRow=j;
                                    if(begintag[tmpCurRow].length==tmpCur){
                                        curRow=tmpCurRow;
                                        tmpCurRow=-1;
                                        tmpCur=0;
                                    }
                                    break;
                                }

                            }
                            if(j>=begintag.length){
                                tmpCur=0;
                                tmpCurRow=-1;
                            }

                        }
                    }
                    if(curRow>=0){
                        if(expchars==null){
                            expchars = new char[1024];
                            targetCur-=begintag[curRow].length;
                        }else  if(expStack.isEmpty()){
                            expStack.push(expchars);
                            expcur-= begintag[curRow].length;
                            expcurStack.push(expcur);
                            expchars = new char[1024];
                            tagindex.push(curRow);
                            expcur=0;
                        }

                        expchars = StringUtils.appendChar(expchars,expcur,begintag[curRow],begintag[curRow].length);
                        expcur+= begintag[curRow].length;

                        //isin=true;

                    }
                }

            }

            StringBuffer sb = new StringBuffer(new String(Arrays.copyOf(target, targetCur)).trim());
            if(!expStack.isEmpty()){
                for(int i=0;i<expStack.size();i++){
                    char[] t = (char[])expStack.get(i);
                    int  n= (int)expcurStack.get(i);
                    sb.append(new String(Arrays.copyOf(t,n)).trim());
                }
            }
            if(null !=expchars){
                sb.append(new String(expchars).trim());
            }
            return sb.toString();
            /*if(!expStack.isEmpty()){
                if(null !=expchars){
                    sb.insert(0,new String(expchars));
                }
                while(!expStack.isEmpty()){
                    sb.insert(0,new String((char[])expStack.pop()));
                }
                sb.insert(0,new String(Arrays.copyOf(target, targetCur)));
                return sb.toString();
            }else if(null != expchars && expchars.length>0 && expcurStack.isEmpty()) {
                return new String(Arrays.copyOf(target, targetCur))+ new String(expchars);
            }else {
                return new String(Arrays.copyOf(target, targetCur));
            }*/
        }catch (ISPException ex){
            throw ex;
        }catch (Exception e){
            log.error("get value from map error:"+expchars.length+"\n"+exp,e);
            return exp;
        }finally {
            target = null;
            tmp= null;
            expchars= null;
            expStack = null;
            expcurStack = null;
            tagindex = null;
        }
    }

    static boolean isSamePre(char [] cs,int pos,char[] begintag){
        for(int i=0;i<pos;i++){
            if(cs[i]!=begintag[i])
                return false;
        }
        return true;
    }

    static Object findObj(Object findO,String point)throws Throwable{
        boolean isarray=false;
        List<String> arrays=null;
        //function
        boolean isToStr=false;
        //[] array
        if(point.contains("[")){
            if(point.endsWith("]")){
                arrays =StringUtils.getTagsNoMark(point,"[","]");
                point = point.substring(0,point.indexOf("["));
                isarray=true;
            }
        }

        if(point.startsWith("to_str(")){
            List<String> t =StringUtils.getTagsNoMark(point,"to_str(",")");
            point = t.get(0);
            isToStr=true;
        }else if(findO instanceof Map && !point.startsWith("${") && XMLParameter.startWithRetainChars(point)){
            //数据值转换，如果是最后一个节点，获取到的值可以根据表达式转换
            Object ret= XMLParameter.getExpressValueFromMap(point,(Map)findO,null);
            if(null != ret && StringUtils.isNotBlank(ret) && !point.equals(ret)){
                if(ret instanceof String){
                    //System.out.println("tt:"+ret);
                    Object o = getValueByPath(findO,(String)ret);
                    //System.out.println("tt:"+o);
                    if(null != o){
                        return o;
                    }else{
                        return ret;
                    }
                }else {
                    return ret;
                }
            }
        }else if(findO instanceof XMLParameter && !isarray && point.startsWith("${")){
            Object o = ((XMLParameter)findO).getParameterWithoutThreadName(point);
            if(log.isDebugEnabled()){
                log.debug("getValue ["+o+"] by path ["+point+"]");
            }
            if(null != o){
                return o;
            }
        }else if(findO instanceof XMLMakeup && !point.startsWith("${") && (XMLParameter.startWithRetainChars(point) || point.startsWith("("))){
            //如果是xmlmakeup对象，根据结尾表达式判断如果表达式为真，返回表达式对象。如：RootInfo.RowSet.#{(Name)=SETDealerInfo}
            Object ret=XMLParameter.getExpressValueFromMap(point,((XMLMakeup)findO).getProperties(),null);
            if(XMLParameter.isBooleanPoint(point)){
                if(!(null != ret && ret instanceof String && StringUtils.isTrue((String)ret)))
                    findO=null;
            }else{
                findO= ret;
            }
        }


        //get value
        if(null==findO)
            return null;
        if(findO.getClass().isArray()){
            if(StringUtils.isNotBlank(point))
                return null;
            //Object[] os = (Object[])findO;
            //throw new Exception("now not support get value form array["+findO+"] object."+point);
        }else{
            if(findO instanceof Map){
                findO=((Map)findO).get(point);
            }else if(findO instanceof JSONObject){
                findO=((JSONObject)findO).get(point);
            }else if(findO instanceof JSONArray){

            }else if(findO instanceof XMLMakeup){
                if(!XMLParameter.isHasRetainChars(point)) {
                    List li = new ArrayList();
                    if (point.equalsIgnoreCase("properties")) {
                        findO = ((XMLMakeup) findO).getProperties();
                    } else {
                        for (XMLMakeup x : ((XMLMakeup) findO).getChildren()) {
                            if (x.getName().equals(point)) {
                                li.add(x);
                            }
                        }
                        if (li.size() == 1) {
                            findO = li.get(0);
                        } else if (li.size() > 1) {
                            findO = li;
                        } else
                            findO = null;
                    }
                }
            }else{
                Object o = ClassUtils.getFieldValue(findO,point,false);
                if(null ==o && point.startsWith("#")){
                    findO=ClassUtils.invokeMethod(findO,point.substring(1),null,null,false);
                }else{
                    findO=o;
                }
            }

        }
        //change result
        if(isarray && null != findO){
            findO=getArrayValue(findO,arrays);
        }
        arrays=null;
        //reconver function
        if(isToStr && null != findO){
            findO=String.valueOf(findO);
        }
        return findO;
    }

    static Object getArrayValue(Object findO,List<String> arrays){
        Object ret=null;
        for(String in:arrays){
            if(findO.getClass().isArray()){
                int i=0;
                if(in.equals("last")){
                    i= ((Object[])findO).length-1;
                }else{
                    i = Integer.parseInt(in);
                }
                if(((Object[])findO).length<=i)
                    return null;
                if(i<0)
                    return null;
                findO= ((Object[])findO)[i];
            }else if(Collection.class.isAssignableFrom(findO.getClass())){
                int i=0;
                if(in.equals("last")){
                    i=  ((Collection)findO).size()-1;
                }else{
                    i = Integer.parseInt(in);
                }
                Iterator its = ((Collection)findO).iterator();
                int n=-1;
                while(its.hasNext()){
                    Object oo = its.next();
                    n++;
                    if(n==i){
                        findO=oo;
                        break;
                    }

                }
                if(n<0 || n<i){
                    return null;
                }
            }
        }
        return findO;
    }

    public static boolean existPath(Object o,String path){
        int n = path.indexOf(".");
        if(n>0){
            String k = path.substring(0,n);
            Object so = getValueByPath(o,k);
            if(null != so) {
                String s = path.substring(n + 1);
                return existPath(so,s);
            }else{
                return false;
            }

        }else{
            if(null != o){
                if(o instanceof Map){
                    if(((Map)o).containsKey(path)){
                        return true;
                    }else{
                        return false;
                    }
                }else{
                    try {
                        Field f = ClassUtils.getField(o, path, false);
                        if(null != f){
                            return true;
                        }
                        return false;
                    }catch (Exception e){
                        return false;
                    }
                }
            }else{
                return false;
            }

        }
    }

    public static Object getValueWithArrayNullByPath(Object o,String path){
        return getValue(o,path,true);
    }
    public static Object getValueByPath(Object o,String path){
        return getValue(o,path,false);
    }
    /**
     * 根据路径获取值,能获取数组,map,list,具体属性中的值
     * @param o
     * @param path
     * @return
     */
    public static Object getValue(Object o,String path,boolean isAppendArrayNull){
        try{
            if(log.isDebugEnabled()){
                log.debug("getValue ["+o+"] by path ["+path+"]");
            }
            if(null == o) return null;
            if(null == path)return o;
            if(o.getClass().isArray()|| Collection.class.isAssignableFrom(o.getClass())){
                return handleArray(o,path,isAppendArrayNull);
            }
            Object findO = findObj(o,path);
            if(null != findO)return findO;
            String p=path;
            String sub=null;
            while(true){
                int n = StringUtils.lastOutOfIndex(p,'.','(',')');
                if(n<=0)break;
                p = p.substring(0,n);
                findO=getValue(o,p,isAppendArrayNull);
                if(null != findO){
                    sub = path.substring(p.length()+1);
                    Object array = handleArray(findO,sub,isAppendArrayNull);
                    if(null != array){
                        findO=array;
                        return findO;
                    }
                    break;
                }
            }
            p=null;
            if(null == findO)return null;
            return getValue(findO,sub,isAppendArrayNull);
        }catch (Throwable e){
            log.error(path,e);
            return null;
        }
    }


    public static boolean setValueByPath(Object o,String path,Object v){
        try{
            Object obj=null;
            String key = null;
            if(path.contains(".")){
                String rp = path.substring(0,path.lastIndexOf("."));
                key = path.substring(path.lastIndexOf(".")+1);
                obj = findObj(o,rp);
            }else{
                obj = o;
                key = path;
            }
            if(obj instanceof Map){
                Class t = null;
                if(null != ((Map)obj).get(key))
                    t = ((Map)obj).get(key).getClass();
                if(null != t)
                    ((Map)obj).put(key,ClassUtils.chgValue(key,t,v));
                else
                    ((Map)obj).put(key,v);
            }else if(obj instanceof List){
                for(Object n:(List)obj){
                    setValueByPath(n,path,v);
                }
            }else{
                ClassUtils.setFieldValue(o,key,v,false);
            }
            return true;
        }catch (Throwable e){
            log.error("set value to obj fault",e);
            return false;
        }
    }
    public static boolean removeValueByPath(Object o,String path){
        try{
            Object obj=null;
            String key = null;
            if(path.contains(".")){
                String rp = path.substring(0,path.lastIndexOf("."));
                key = path.substring(path.lastIndexOf(".")+1);
                obj = findObj(o,rp);
            }else{
                obj = o;
                key = path;
            }
            if(obj instanceof Map){
                /*Class t = null;
                if(null != ((Map)obj).get(key))
                    t = ((Map)obj).get(key).getClass();
                if(null != t)
                    ((Map)obj).remove(key,ClassUtils.chgValue(t,v));
                else*/
                    ((Map)obj).remove(key);
            }else{
                ClassUtils.setFieldValue(o,key,null,false);
            }
            return true;
        }catch (Throwable e){
            log.error("remove value to obj fault",e);
            return false;
        }
    }
    static Object handleArray(Object findO,String path,boolean isAppendNull){
        if(null != findO){
            if(findO.getClass().isArray()){
                Object[] os = (Object[])findO;
                List list = new ArrayList();
                for(Object o1:os){
                    Object ret = getValue(o1,path,isAppendNull);
                    if(null != ret)
                        list.add(ret);

                    if(isAppendNull && null == ret){
                        list.add(ret);
                    }
                }
                return list;
            }else if(findO instanceof Collection){
                Iterator its = ((Collection)findO).iterator();
                List list = new ArrayList();
                while(its.hasNext()){
                    Object ret = getValue(its.next(),path,isAppendNull);
                    if( (null != ret && ret instanceof Map && ((Map)ret).size()>0) || (null != ret && ret instanceof Collection && ((Collection)ret).size()>0) || (null != ret && !(ret instanceof Map) && !(ret instanceof Collection)))
                        list.add(ret);
                    if(isAppendNull && null == ret){
                        list.add(ret);
                    }
                }
                return list;
            }
        }
        return null;
    }

    /**
     * 获取字符串的路径值
     * @param obj
     * @param path
     * @return
     */
    public static String getStringValueByPath(Object obj,String path)throws Exception{
        Object ret = getValueByPath(obj,path);
        if(null !=ret){
            if(((ret instanceof Collection && ((Collection) ret).size()==0) ||(ret.getClass().isArray() && ((Object[])ret).length==0)))
               return null;
            if(ret instanceof Map)
                return ObjectUtils.convertMap2String((Map)ret);
            return ret.toString();
        }else{
            return null;
        }
    }
    public static String encodeURL(Object o){
        if(null != o){
            if (o instanceof Map){
                try {
                    return convertMap2String((Map)o);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return o.toString();
    }
    public static int getInt(Object o){
        if(o instanceof String)
            return Integer.parseInt((String)o);
        else if(o instanceof Long)
            return ((Long)o).intValue();
        else
            return (Integer)o;
    }
    public static Integer getInteger(Object o){
        if(null ==o || "".equals(o)) return null;
        if(o instanceof String)
            return Integer.valueOf((String)o);
        else  {
            return (Integer) o;
        }
    }

    public static Object getTypeDefaultValue(Object value,String typeClass){
        if(StringUtils.isBlank(typeClass))return value;
        if(null == value){
            if(typeClass.equals("int")||typeClass.equals("float")||typeClass.equals("double")||typeClass.equals("long")
                    || typeClass.equals("java.lang.Integer") || typeClass.equals("java.lang.Double") || typeClass.equals("java.lang.Float")
                    || typeClass.equals("java.lang.Long")||typeClass.equals("java.math.BigDecimal")||typeClass.equals("java.math.BigInteger")
                    ){
                value = "0";
            }
            if(typeClass.equals("java.lang.String")){
                value = "";
            }
            if(typeClass.equals("boolean")||typeClass.equals("java.lang.Boolean")){
                value = "false";
            }
            if(typeClass.equals("java.util.Date")||typeClass.equals("java.sql.Date")||typeClass.equals("java.sql.Timestamp")){
                value = DEFAULT_DATE_VALUE;
            }
            return convertType(value,typeClass);
        }
        return null;
    }
    public static Object convertType(Object obj,String type){
        if(null == obj)return null;
        if(StringUtils.isBlank(type)) return obj;
        if(obj.getClass().getName().equals(type))
            return obj;
        if(type.equals("String")||type.equals("java.lang.String")){
            return obj.toString();
        }else if("java.lang.Double".equals(type)||"double".equals(type)){
            if(obj instanceof String){
                return Double.parseDouble((String)obj);
            }
        }else if("java.lang.Integer".equals(type)||"int".equals(type)){
            if(obj instanceof String){
                return Integer.parseInt((((String)obj).contains(".")?((String)obj).substring(0,((String)obj).indexOf(".")):((String)obj)));
            }
        }else if("java.lang.Long".equals(type)||"long".equals(type)){
            if(obj instanceof String){
                return Long.parseLong((((String)obj).contains(".")?((String)obj).substring(0,((String)obj).indexOf(".")):((String)obj)));
            }
        }else if("java.lang.Boolean".equals(type)||"boolean".equals(type)) {
            if (obj instanceof String) {
                return Boolean.parseBoolean((String) obj);
            }
        }else if("java.util.Date".equals(type)){
            if(obj instanceof String){
                try {
                    return DateTimeUtils.getDate((String) obj);
                }catch (Exception e){
                    return new Date(0);
                }
            }
        }else if("java.sql.Timestamp".equals(type)){
            if(obj instanceof String){
                try {
                    Date d = DateTimeUtils.getDate((String) obj);
                    return new Timestamp(d.getTime());
                }catch (Exception e){
                    return new Date(0);
                }
            }
        }else if("java.lang.Float".equals(type)||"float".equals(type)){
            if(obj instanceof String){
                try {
                    return Float.parseFloat((String)obj);
                }catch (Exception e){
                    return new Date(0);
                }
            }
        }
        throw new RuntimeException("now not support "+obj+" convertType to "+type);
    }

    public static ByteArrayInputStream convert(ByteArrayOutputStream out){
        return new ByteArrayInputStream(out.toByteArray());
    }
    public static ByteArrayInputStream convert2InputStream(byte[] out){
        return new ByteArrayInputStream(out);
    }
    public static ByteArrayOutputStream convert(byte[] bs) throws IOException {
        if(null != bs){
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            o.write(bs);
            return o;
        }
        return null;

    }


    public static Object setValue(Class c,Object data,char[][] specChars)throws Exception{

        if(Map.class.isAssignableFrom(c) && data instanceof Map ){
            Map m = (Map)c.newInstance();
            m.putAll((Map)data);
            return m;
        }else if(!ClassUtils.isSimple(c.getName()) && data instanceof Map){
            Object ret = c.newInstance();
            Iterator its = ((Map)data).keySet().iterator();
            boolean isset=false;
            while(its.hasNext()){
                String name =(String)its.next();
                Object v =  ((Map)data).get(name);
                if(!isContainsExpressBeginFlag((String)v, XMLParameter.FilterTagsBegin)){
                    if(v instanceof String){ //
                        ClassUtils.setFieldValue(ret,name,v,false);
                        if(!isset)
                            isset=true;
                    }else{
                        ClassUtils.setFieldValue(ret,name,v,false);
                        if(!isset)
                            isset=true;
                    }
                }
            }
            if(isset)
                return ret;
        }


        return null;
    }

    /**
     *
     * @param map
     * @return
     */
    public static Map filterMap(Map map,Map<String,String> path) throws Throwable {
        if( null != map && null!= path && path.size()>0){
            Iterator<String> its = path.keySet().iterator();
            while(its.hasNext()) {
                String k = its.next();
                filterMapNull(map, k, StringUtils.isTrue(path.get(k)), null, null);
            }
        }
        return map;
    }
    static boolean containKey(Object o,String path){
        try {
            if (null == o) return false;
            if (o instanceof Map) return ((Map) o).containsKey(path);
            if (!o.getClass().isArray() && !(o instanceof Collection))
                return null == ClassUtils.getField(o.getClass(), path, false) ? false : true;
            return false;
        }catch (Exception e){
            return false;
        }
    }

    /**
     *
     * @param o  对象
     * @param pro 属性名称
     * @param v  属性值
     */
    static void removePro(Object o ,String pro,Object v){
        if(o instanceof Map){
            ((Map)o).remove(pro);
        }else if(o instanceof Collection){
            ((Collection)o).remove(v);
        }
        //其他类型时不需要处理

    }

    /**
     *
     * @param parent 上级对象
     * @param ok    对象在上级对象中的属性名称
     * @param o    对象
     * @param pro  对象中的属性
     * @param isWithParent  对象中的属性值为null时，是否把改对象在上级对象中删除
     * @throws Exception
     */
    static void removeObj(Object parent,String ok,Object o,String pro,boolean isWithParent) throws Throwable {
        Object v = findObj(o,pro);
        if(null == v ||(v instanceof String && StringUtils.isBlank((String)v))||(v instanceof Integer && (Integer)v==0)||(v instanceof Collection && (((Collection) v).size()==0 || !ArrayUtils.isNotEmpty((Collection)v)))||(v instanceof Map && ((Map) v).size()==0)){
           if(isWithParent){
               removePro(parent,ok,o);
           }else{
               removePro(o,pro,null);
           }
        }
    }

    /**
     * 从对象中删除为空值的子对象
     * @param m    对象
     * @param path 要删除的对象路径
     * @param isWithParent 是否连值为空的对象在上级对象中删除
     * @param subParent   临时上级对象
     * @throws Exception
     */
    static void filterMapNull(Object m,String path,boolean isWithParent,Object subParent,String mpath) throws Throwable {
        Boolean o = containKey(m, path);
        if(o){
            //find object will handle
            removeObj(subParent,mpath,m,path,isWithParent);
        }else{
            int n = path.indexOf(".");
            String sk,sb;
            Boolean t;
            while(n>0){
                sk = path.substring(0,n);
                t = containKey(m, sk);
                if(t){
                    sb = path.substring(n+1);
                    Object tt = findObj(m,sk);
                    if(tt.getClass().isArray()){
                       for(Object p:(Object[])tt){
                           if(sb.length()>0){
                               filterMapNull(p,sb,isWithParent,tt,null);
                           }
                       }
                    }else if(tt instanceof List){
                        for (int i=((List) tt).size()-1;i>=0;i--){
                            Object p = ((List) tt).get(i);
                            if(sb.length()>0){
                                filterMapNull(p,sb,isWithParent,tt,null);
                            }
                        }
                    }else{
                        filterMapNull(tt,sb,isWithParent,m,sk);
                    }
                    break;
                }else{
                    n = path.indexOf(".", n);
                }
            }
        }
    }
    public static String convertMap2String(Map map)  {
/*
        if(log.isDebugEnabled()) {
            new Exception().printStackTrace();
            log.debug("convertMap2String parameters:" + map);
        }
*/
        if(null !=map) {
            synchronized (map) {
                StringBuffer sb = new StringBuffer("{");
                boolean isfirst = true;
                List ids = new ArrayList();
                if (null != map && map.size() > 0) {
                    Iterator its = map.keySet().iterator();
                    while (its.hasNext()) {
                        if (!isfirst)
                            sb.append(",");
                        Object o = its.next();
                        Object v = map.get(o);
                        appendObject2StringBuffer(sb, o, v);
                        if (isfirst)
                            isfirst = false;
                    }
                }
                sb.append("}");
                return sb.toString();
            }
        }
        return "";
    }
    public static String convertList2String(List li) {
        StringBuffer sb = new StringBuffer();
        appendObject2StringBuffer(sb,null,li);
        return sb.toString();
    }
    public static String convertKeyWithoutThreadNameMap2String(Map map,String[] keys)  {
        StringBuffer sb = new StringBuffer("{");
        boolean isfirst=true;
        if(null != map && map.size()>0){
            Iterator its = map.keySet().iterator();
            while(its.hasNext()){
                Object o = its.next();
                if(o instanceof String) {
                    int n = ((String)o).indexOf("[");
                    if (n > 0) {
                        o = o.toString().substring(0, n);
                    }
                }
                if(!ArrayUtils.isInStringArray(keys,o.toString())){
                    continue;
                }
                if(!isfirst)
                    sb.append(",");
                Object v = map.get(o);

                appendObject2StringBuffer(sb,o,v);
                if(isfirst)
                    isfirst=false;
            }
        }
        sb.append("}");
        return sb.toString();
    }

    public static String convertKeyWithoutThreadNameMap2String(Map map)  {
        StringBuffer sb = new StringBuffer("{");
        boolean isfirst=true;
        if(null != map && map.size()>0){
            Iterator its = map.keySet().iterator();
            while(its.hasNext()){
                if(!isfirst)
                    sb.append(",");
                Object o = its.next();
                Object v = map.get(o);
                if(o instanceof String) {
                    int n = ((String)o).indexOf("[");
                    if (n > 0) {
                        o = o.toString().substring(0, n);
                    }
                }
                appendObject2StringBuffer(sb,o,v);
                if(isfirst)
                    isfirst=false;
            }
        }
        sb.append("}");
        return sb.toString();
    }
    public static void appendObject2StringBuffer(StringBuffer sb,Object o,Object v)  {
        if(v==null && o==null){
            return;
        }
        if(null != o && v == null){
            sb.append(keyv(o)).append(":").append(valuev(null));
            return;
        }

        if(v.getClass().isArray()){
            if(null!= o && StringUtils.isNotBlank(o)){
                sb.append(keyv(o)).append(":");
            }
            sb.append("[");

            boolean isf=true;
            for(int i=0;i<Array.getLength(v);i++){
                if(!isf)
                    sb.append(",");
                appendObject2StringBuffer(sb, null, Array.get(v,i));
                if(isf)
                    isf=false;
            }
            sb.append("]");
        }else if(v instanceof Map){
            if (null != o)
                sb.append(keyv(o)).append(":").append(convertMap2String((Map) v));
            else{
                if(v instanceof JSONObject){
                    sb.append(((JSONObject)v).toString());
                }else if(v instanceof com.alibaba.fastjson.JSONObject){
                    sb.append(((com.alibaba.fastjson.JSONObject)v).toJSONString());
                }else {
                    sb.append(convertMap2String((Map) v));
                }
            }
        }else if(v instanceof Collection){
            Iterator it = ((Collection)v).iterator();
            if(null != o) {
                sb.append(keyv(o)).append(":").append("[");
            }else{
                sb.append("[");
            }
            boolean isf=true;
            while(it.hasNext()){
                Object m = it.next();
                if(!isf)
                    sb.append(",");
                appendObject2StringBuffer(sb, null, m);
                if(isf)
                    isf=false;
            }
            sb.append("]");
        }else if(null !=o){
            sb.append(keyv(o)).append(":").append(valuev(v));
        }else{
            sb.append(valuev(v));
        }
    }
    static BASE64Encoder base64Encoder = new BASE64Encoder();
    static String inputStream2StringHeader="data;base64;";
    public static String convertInputStream2Base64String(InputStream in) throws IOException {
        byte[] b = new byte[in.available()];
        in.read(b);
        return inputStream2StringHeader+base64Encoder.encode(b);
    }
    static BASE64Decoder base64Decoder = new BASE64Decoder();
    public static InputStream convertBase64String2InputStream(String in) throws IOException {
        if(in.startsWith(inputStream2StringHeader)) {
            in  = in.substring(inputStream2StringHeader.length());
            byte[] b = base64Decoder.decodeBuffer(in);
            ByteArrayInputStream ret = new ByteArrayInputStream(b);
            return ret;
        }else{
            return null;
        }
    }
    static String valuev(Object v)  {
        if(null == v)return "\"\"";
        if(v instanceof String){
            if((
                    ((String)v).startsWith("[") && ((String)v).endsWith("]") && null != StringUtils.convert2ListJSONObject((String)v) && StringUtils.convert2ListJSONObject((String)v).size()>0
            )
                    || ( ((String)v).startsWith("{") && ((String)v).endsWith("}") && null != StringUtils.convert2MapJSONObject((String)v))) {
                return jsonReaminDeal((String) v);
            }else {
                String s= StringUtils.subQuotStringWithCheckJavaTransChar(v.toString());
                /* remove by wangfeng 2019/4/1 replace with appendCharBefore
                String s = StringUtils.replace(v.toString(),"\"","\\\"");
                s = StringUtils.replace(s,"\\\\\"","\\\\\\\"");*/
                return "\"" + s + "\"";
            }
        }else if(v instanceof Date){
            return "\""+DateTimeUtils.date2String((Date)v,"yyyy-MM-dd HH:mm:ss")+"\"";
        }else if(v instanceof InputStream){
            try {
                return convertInputStream2Base64String((InputStream) v);
            }catch (Exception e){
                log.error("convert inputStream to String error",e);
            }
            return null;
        }else{
            return jsonReaminDeal(v.toString());
        }
    }
    static String jsonReaminDeal(String s){
        if(s.contains("\\")){
            s = s.replaceAll("\\\\","/");
        }
        return s;
    }
    static String keyv(Object k){
        if(k!=null) {

            return "\"".concat(jsonReaminDeal(k.toString()).concat("\""));
        }
        return null;
    }

    /**
     * 根据MappingInfo结构，复制src对象属性，成为新对象
     * @param src 原对象
     * @param mi mapping关系和新对象结构
     * @param target 目标对象
     */
    public static void mapping(Object src,MappingInfo mi,Object target){
        try{

            Iterator<String> ks = mi.getPathMapping().keySet().iterator();
            Map tm= new HashMap();
            //long t = 0;
            while(ks.hasNext()){
                String tp = ks.next();
                String sp = mi.getPathMapping().get(tp);
                //long l = System.currentTimeMillis();
                Object srcObj = getValueByPath(src,sp);
                //long m = System.currentTimeMillis()-l;
                //System.out.println(sp+":"+m);
                //t+=m;
                tm.put(tp, srcObj);
            }
            //System.out.println("total:"+t);
            if(tm.size()>0){
                setValue(target,mi.getTargetStruct(), tm);
            }

        }catch (Exception e){
            log.error("mapping object error:",e);
        }
    }
    //向对象中批量设置值

    /**
     *
     * @param obj  一个对象不是数组
     * @param childresstr
     * @param vs  vs key is target path , and path order from short to long
     * @throws Exception
     */
    public static void setValue(Object obj,List<StructInfo> childresstr,Map<String,Object> vs)throws Exception{
        if(null == obj || null == vs)return;
        Iterator<String> its = vs.keySet().iterator();
        while(its.hasNext()){
            //枚举每一个mapping路径的结果，向新对象映射
            String tartpath = its.next();
            Object srcObj = vs.get(tartpath);
            String[] paths = tartpath.split("\\.");
            Object tempObj = obj;
            List<StructInfo> tempstrs =  childresstr;
            StructInfo curStru = null; //current property struct
            for(StructInfo info:tempstrs){
                if(null == info.getName() && null!=info.getType() && (ClassUtils.isArray(info.getType()) && !ClassUtils.isArray(srcObj.getClass()))){
                    curStru=info;
                    break;
                }else if(null != info.getName() && info.getName().equals(paths[0])){
                    curStru = info;
                    break;
                }
            }
            //按照结构描述路径设置对象数据(mapping中的路径,在数组，list时没有属性key)
            String curPropertyName = paths[0];
                if(null != curStru && curStru.isArray() && srcObj.getClass().isArray()){
                    Object[] os = (Object[])Array.newInstance(curStru.getType(), ((Object[]) srcObj).length);
                    for(int i=0;i<((Object[]) srcObj).length;i++){
                        Object no = curStru.getType().newInstance();
                        HashMap<String,Object> nm = new HashMap();
                        nm.put(tartpath,((Object[])srcObj)[i]);
                        setValue(no,curStru.getChildren(),nm);
                        os[i]=no;
                    }
                    setValueByPath(tempObj,curPropertyName,os);
                }else if(null != curStru && Collection.class.isAssignableFrom(curStru.getType()) && srcObj instanceof Collection) {
                    List os = (List) getValueByPath(tempObj, curPropertyName);
                    if (null == os) {
                        os = (List) curStru.getType().newInstance();
                    }
                    Iterator it = ((Collection) srcObj).iterator();

                    if(!(ClassUtils.isArray(curStru.getChildren().get(0).getType())) && (ClassUtils.isArray((((List) srcObj).get(0)).getClass())) ){
                        //如果当前目标类型不是数组或List,数据源对象下一个还是数组或List，进行数据层级缩小一级
                        LinkedList temp = new LinkedList();
                        Iterator tt = ((Collection) srcObj).iterator();
                        while(tt.hasNext()) {
                            Collection sub = (Collection)tt.next();
                            Iterator st = sub.iterator();
                            while(st.hasNext())
                                temp.add(st.next());
                        }
                        it = temp.iterator();
                    }

                    int n = 0;
                    while (it.hasNext()) {
                        //list 中数据结构相同
                        StructInfo subinfo = curStru.getChildren().get(0);
                        HashMap<String, Object> nm = new HashMap();
                        String sup = tartpath;
                        if (null != curStru.getName())//当前数组在一个对象中时有name值
                            sup = tartpath.substring(curStru.getName().length() + 1);
                        nm.put(sup, it.next());
                        if (n > os.size() - 1) {
                            os.add(subinfo.getType().newInstance());
                        }
                        Object no = os.get(n);
                        setValue(no, subinfo.getChildren(), nm);

                        n++;
                    }
                    setValueByPath(tempObj, curPropertyName, os);
                } else if(null ==curStru && tempObj instanceof Collection && srcObj instanceof Collection){
                    //上级是list，srcObj是list，当前stru不是List，扩展上级
                    List os = (List)tempObj;
                    StructInfo subinfo = tempstrs.get(0);

                    Iterator it = ((Collection) srcObj).iterator();

                    if(((Collection) srcObj).size()>0 && !(ClassUtils.isArray(subinfo.getType())) && (ClassUtils.isArray((((List) srcObj).get(0)).getClass()))){
                      //如果当前目标类型不是数组或List,数据源对象下一个还是数组或List，进行数据层级缩小一级
                        LinkedList temp = new LinkedList();
                        Iterator tt = ((Collection) srcObj).iterator();
                        while(tt.hasNext()) {
                            Collection sub = (Collection)tt.next();
                            Iterator st = sub.iterator();
                            while(st.hasNext())
                                temp.add(st.next());
                        }
                        it = temp.iterator();
                    }

                    int n = 0;
                    while (it.hasNext()) {
                        //list 中数据结构相同
                        if (n > os.size() - 1) {
                            os.add(subinfo.getType().newInstance());
                        }
                        Object no = os.get(n);
                        HashMap<String, Object> nm = new HashMap();
                        nm.put(tartpath, it.next());
                        setValue(no, subinfo.getChildren(), nm);
                        n++;
                    }


                }else{
                    //find cur node classs defined
                    if(paths.length>1) {
                        Object propertyObj = getValueByPath(tempObj, curPropertyName);
                        Class canInstanceClass = curStru.getType();
                        String p = tartpath.substring(tartpath.indexOf(".") + 1);
                        HashMap<String, Object> v1 = new HashMap<String, Object>();
                        v1.put(p, srcObj);
                        if (null == propertyObj) {
                            //if in middle of the path and not value then set init class instance
                            Object newobj = canInstanceClass.newInstance();
                            setValueByPath(tempObj, curPropertyName, newobj);
                            tempObj = newobj;
                        } else if (!(null != propertyObj && propertyObj.getClass().isAssignableFrom(canInstanceClass))) {
                            throw new Exception("StructInfo descript property " + canInstanceClass.getName() + " " + curPropertyName + " not match in " + obj);
                        } else {
                            tempObj = propertyObj;
                        }
                        setValue(tempObj, curStru.getChildren(), v1);
                    }else if(null != tempObj && null != srcObj){
                        if(List.class.isAssignableFrom(tempObj.getClass())
                                && ((null == curStru && Map.class.isAssignableFrom(tempstrs.get(0).getType())) || (null != curStru && Map.class.isAssignableFrom(curStru.getType())))
                                && !List.class.isAssignableFrom(srcObj.getClass())){
                            //目标是List，源数据不是List,源数据放入一个Map中，map放入List返回

                            Map m = null;
                            if(((List)tempObj).size()==0) {
                                if (null != curStru)
                                    m = (Map) curStru.getType().newInstance();
                                else
                                    m = (Map) tempstrs.get(0).getType().newInstance();
                                ((List) tempObj).add(m);
                            }
                            m = (Map)((List)tempObj).get(0);
                            setValue(m,childresstr,vs);
                        }else if(null == curStru && Map.class.isAssignableFrom(tempObj.getClass()) && List.class.isAssignableFrom(srcObj.getClass()) ){
                            //目标是Map,没有当前结构，源数据是数组，把数组降级
                            HashMap<String, Object> v1 = new HashMap<String, Object>();
                            v1.put(tartpath,((List)srcObj).get(0));

                            setValue(tempObj,tempstrs,v1);

                        }else if(List.class.isAssignableFrom(tempObj.getClass()) && List.class.isAssignableFrom(srcObj.getClass())){
                            //路径末尾
                            ((List)tempObj).addAll((List)srcObj);
                        }else if(tempObj.getClass().isArray() && srcObj.getClass().isArray()){
                             tempObj=srcObj;
                        }else {
                            setValueByPath(tempObj, curPropertyName, srcObj);
                        }
                    }
                }

        }
    }


    public static void setValue(Object o,String classDesc,Map<String,Object> map)throws Exception{
        if(o instanceof Map && !classDesc.contains("<")){
            ((Map)o).put(map.get("key"),map.get("value"));
        }else if(o instanceof Map && classDesc.contains("<")){
            if(!((Map)o).containsKey(map.get("key"))) ((Map)o).put(map.get("key"),new ArrayList());
            ((List)((Map)o).get(map.get("key"))).add(map.get("value"));
        }else if(o instanceof List){
            if(classDesc.contains("<")){
                Object ao = ClassUtils.getClass(null,(String)StringUtils.getTagsNoMark(classDesc,"<",">").get(0)).newInstance();
                if(!ao.getClass().isArray() && !List.class.isAssignableFrom(ao.getClass()) && !Map.class.isAssignableFrom(ao.getClass())){
                    Iterator its = map.keySet().iterator();
                    while(its.hasNext()){
                        String k = (String)its.next();
                        Field f = ClassUtils.getField(ao,k,false);
                        String c = f.getType().getName();
                        if(ClassUtils.isSimple(c)){
                           ClassUtils.setFieldValue(ao,k,ClassUtils.convertSimpleObject(c,map.get(k)),false);
                        }else
                        ClassUtils.setFieldValue(ao,k,map.get(k),false);
                    }
                    ((List)o).add(ao);
                }
            }
        }else{
            throw new Exception("now not support ["+o.getClass().getName()+"] setValue");
        }
    }

    /**
     * 把Map中的数据放入POJO对象中
     * @param c
     * @param l
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    public static List generatorObjectByMapData(Class c,List<Map<String,?>> l) throws InstantiationException, IllegalAccessException, SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException{
        List ret = new ArrayList();
        for(Map map : l){
            Iterator its = map.keySet().iterator();
            Object o = c.newInstance();
            boolean isv = false;
            while(its.hasNext()){

                String s = (String)its.next();
                if(null != map.get(s)){
                    try{
                        String ts = "set"+String.valueOf((s.charAt(0))).toUpperCase()+s.substring(1);
                        Method m = c.getMethod(ts, new Class[]{map.get(s).getClass()});
                        m.invoke(o, new Object[]{map.get(s)});
                        isv=true;
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }
            if(isv)
                ret.add(o);
        }
        return ret;
    }


    static boolean isIn(int a,int b,int[][] ds){
        for(int[] dd:ds){
            String s = String.valueOf(a);
            boolean is=false;
            if(s.length()>1){
                is = true;
                for(int i=0;i<s.length();i++){
                    if(!ArrayUtils.isInIntArray(dd,Integer.valueOf(s.charAt(i)+""))){
                        is = false;
                        break;
                    }
                }
            }else{
                is = ArrayUtils.isInIntArray(dd,a);
            }
            if(is &&  ArrayUtils.isInIntArray(dd,b))
                return true;
        }
        return false;
    }
    public static Map<String,List<Properties>> pickLinesByRelation(List<List<Map<String,String>>> datas ,int[][] dataIndexRelateArray,String[] extRelateFields,String key){
        Map<Integer,List<Map<String,String>>> ds = composeRelation(datas,dataIndexRelateArray,extRelateFields);
        Integer[] ks = ds.keySet().toArray(new Integer[0]);
        HashMap<String,List<Properties>> ret = new HashMap<String, List<Properties>>();
        HashMap<String,Integer> temp = new HashMap();
        for(int i=ks.length-1;i>=0;i--){
            for(Map<String,String> m:ds.get(ks[i])){
                if(StringUtils.isNotBlank(m.get(key))){
                    if(ret.containsKey(m.get(key)) ){
                        if(temp.containsKey(m.get(key)) && i==temp.get(m.get(key))){
                            Properties p = new Properties();
                            p.put("PositionLevel",ks[i]);
                            p.putAll(m);
                            ret.get(m.get(key)).add(p);
                        }

                    }else{
                        ret.put(m.get(key),new ArrayList<Properties>());
                        Properties p = new Properties();
                        p.put("PositionLevel",ks[i]);
                        p.putAll(m);
                        ret.get(m.get(key)).add(p);
                        temp.put(m.get(key),i);
                    }
                }
            }
        }

        return ret;
    }
    /**
     * 根据excel中多sheet的数据datas,查找根据字段名称关联的关系,在根据某个字段的值提取关联的一行一行的记录。
     * @param datas
     * @param
     * @return
     */
    public static Map<Integer,List<Map<String,String>>> composeRelation(List<List<Map<String,String>>> datas ,int[][] dataIndexRelateArray,String[] extRelateFields){
        if(null != datas){
            Map<Integer,List<Map<String,String>>> ret = new TreeMap<Integer, List<Map<String, String>>>();
            int n=0;
            for(List<Map<String,String>> o:datas){
                if(!ret.containsKey(n)) ret.put(n,new ArrayList());
                for(Map<String,String> r:o){
                   if(r.size()==0) continue;
                   Integer[] its = ret.keySet().toArray(new Integer[0]);
                   for(int p=its.length-1;p>=0;p--){
                       Integer k = its[p];
                       if(Integer.parseInt((String)(String.valueOf(k).substring(0,1)))!=n && isIn(k,n,dataIndexRelateArray)){
                           List<Map<String,String>> list = ret.get(k);
                           for(int i=list.size()-1;i>=0;i--){
                               Map<String,String> m = list.get(i);
                               boolean isext = false;
                               Iterator<String> itss = m.keySet().iterator();
                               while(itss.hasNext()){
                                   String kk = itss.next();
                                   if(r.keySet().contains(kk) && (extRelateFields==null || (null != extRelateFields && !ArrayUtils.isInStringArray(extRelateFields,kk))) ){
                                       if(r.get(kk).equals(m.get(kk))){
                                           isext=true;
                                       }else{
                                           isext=false;
                                           break;

                                       }
                                   }
                               }
                               if(isext){
                                   HashMap t= new HashMap();
                                   t.putAll(m);
                                   t.putAll(r);
                                   int nn = Integer.parseInt(n+""+k);
                                   if(!ret.containsKey(nn)){
                                       ret.put(nn,new ArrayList<Map<String,String>>());
                                   }
                                   ret.get(nn).add(t);
                               }
                           }
                       }
                   }

                   ret.get(n).add(r);

               }
               n++;
            }
            return ret;
        }
        return null;
    }
    public static List convertBeanList2MapList(List datas){
        if(null != datas){
            List ret = new ArrayList();
            for(Object m:datas){
                try{
                    ret.add(POJOUtil.convertPojo2Map(m,null));
                }catch (Exception e){
                    log.error(e.getMessage(),e);
                }
            }
            if(ret.size()>0)
                return ret;
            return null;
        }
        return null;
    }

    /**
     * convert list of map to list of pojobean
     * @param datas
     * @param a
     * @return
     */
    public static List convertMapList2BeanList(List<Map> datas,Class a){
        if(null != datas){
            List ret = new ArrayList();
            for(Map m:datas){
                try{
                    ret.add(POJOUtil.convertDBMap2POJO(m,a));
                }catch (Exception e){
                    log.error(e.getMessage(),e);
                }
            }
            if(ret.size()>0)
                return ret;
            return null;
        }
        return null;
    }
    public static boolean isTrue(Object o){
        if(null == o)return false;

        if(o instanceof Boolean) return (Boolean)o;
        if(o instanceof Integer) if(((Integer)o)>0)return true;else return false;
        if(o instanceof String) return StringUtils.isTrue((String)o);

        return true;

    }

    public static HashMap sortMapByCompareTo(HashMap map){
        if(null != map) {
            List<Map.Entry<String, Object>> infoIds = new ArrayList<Map.Entry<String, Object>>(map.entrySet());
            Collections.sort(infoIds, new Comparator<Map.Entry<String, Object>>() {
                @Override
                public int compare(Map.Entry<String, Object> o1, Map.Entry<String, Object> o2) {
                    if (o1.getKey().equals(o2.getKey())) return 0;
                    else if (o1.getKey().compareTo(o2.getKey()) > 0) return 1;
                    else return -1;
                }
            });
            HashMap temp = new HashMap();
            for(Map.Entry e:infoIds){
                temp.put(e.getKey(),e.getValue());
            }
            return temp;
        }
        return null;
    }
    public static HashMap sortMapByKeyLength(HashMap map){
        if(null != map) {
            List<Map.Entry<String, Object>> infoIds = new ArrayList<Map.Entry<String, Object>>(map.entrySet());
            Collections.sort(infoIds, new Comparator<Map.Entry<String, Object>>() {
                @Override
                public int compare(Map.Entry<String, Object> o1, Map.Entry<String, Object> o2) {
                    if (o1.getKey().length()>o2.getKey().length()) return -1;
                    else  return 1;
                }
            });
            LinkedHashMap temp = new LinkedHashMap();
            for(Map.Entry e:infoIds){
                temp.put(e.getKey(),e.getValue());
            }
            return temp;
        }
        return null;
    }

    /**
     * 获取内存中对象的大小
     * @param obj
     * @return
     * @throws Exception
     */
    public static long getSizeOfJavaObject(Object obj)throws Exception{
        Instrumentation in = getInstrumentation();
        if(null != in){
            JavaSizeOf.setInstrumentation(in);
            return JavaSizeOf.sizeof(obj);
        }
        return -1;
    }

    static Instrumentation getInstrumentation(){
        Instrumentation instrumentation=null;
        if(instrumentation==null){
            try{
                VirtualMachine vm = JVMUtil.getVM();
                if(null != vm){
                    String userdir = System.getProperty("user.dir");
                    if(StringUtils.isBlank(userdir)) throw new RuntimeException("the user.dir is not exist!");
                    String agentpath=userdir+"/ispagentmain.jar";
                    File f = new File(agentpath);
                    if(!f.exists()){
                        String meta = "META-INF/MANIFEST.MF";
                        ZipUtil.addFile(new JarOutputStream(new FileOutputStream(f)), meta, new ByteArrayInputStream(
                                ("Manifest-Version: 1.0\n" +
                                        "Created-By: 1.6.0_23 (Sun Microsystems Inc.)\n" +
                                        "Agent-Class: com.octopus.utils.si.AgentMain").getBytes()));
                    }
                    vm.loadAgent(agentpath);
                    instrumentation = AgentMain.getInstrumentation();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        return instrumentation;
    }
    public static Long getLong(Object o){
        if(null != o && !"null".equals(o) && o instanceof String && StringUtils.isNotBlank((String)o)){
            return Long.parseLong((String)o);
        }else if(o instanceof Long){
            return (Long)o;
        }else if(o instanceof Integer){
            return ((Integer)o).longValue();
        }
        return new Long(0);
    }

    /**
     * set Object of afterKey after Object of key in LinkedHashMap
     * @param map
     * @param keys
     */
    public static LinkedHashMap setMapKeySortByKeySet(LinkedHashMap map,LinkedList keys){
        LinkedHashMap ret = new LinkedHashMap();
        Iterator its= keys.iterator();
        while(its.hasNext()){
            Object o = its.next();
            ret.put(o,map.get(o));
        }
        if(ret.size()!=map.size()){
            its = map.keySet().iterator();
            while(its.hasNext()){
                Object o = its.next();
                if(!ret.containsKey(o)){
                    ret.put(o,map.get(o));
                }
            }
        }
        return ret;
    }

    /**
     * 把append中的map根据避免重复字段，增加到src列表中，如果列表中已经存在则不增加
     * @param src
     * @param append
     * @param avoidRepeatFields
     */
    public static void addAllMap(List<Map> src,List<Map> append,List<String> avoidRepeatFields){
        if(null != append) {
            if (null == avoidRepeatFields) {
                src.addAll(append);
            }else{
                for(Map m:append){
                    if(!ArrayUtils.isInMapArrayByFields(src,m,avoidRepeatFields)){
                        src.add(m);
                    }
                }
            }
        }
    }

    /**
     * 深度追加Map中的值,不覆盖已有的值
     * @param append
     * @param target
     */
    public static void appendDeepMapNotReplaceKey(Map append,Map target){
        if(null != append && null != target) {
            for (Object e : append.entrySet()) {
                if(!target.containsKey(((Map.Entry) e).getKey()) || null == target.get(((Map.Entry) e).getKey())) {
                    if(System.identityHashCode(((Map.Entry) e).getValue())!=System.identityHashCode(target)) {
                        target.put(((Map.Entry) e).getKey(), ((Map.Entry) e).getValue());
                    }
                }else{
                    if(target.get(((Map.Entry) e).getKey()) instanceof Map && ((Map.Entry) e).getValue() instanceof Map){
                        appendDeepMapNotReplaceKey((Map)((Map.Entry) e).getValue(),(Map)target.get(((Map.Entry) e).getKey()));
                    }
                }
            }
        }
    }

    /**
     * 深度追加Map中的值,覆盖已有的值
     * @param append
     * @param target
     */
    public static void appendDeepMapReplaceKey(Map append,Map target){
        if(null != append && null != target) {
            for (Object e : append.entrySet()) {

                if(target.get(((Map.Entry) e).getKey()) instanceof Map && ((Map.Entry) e).getValue() instanceof Map){
                    appendDeepMapReplaceKey((Map)((Map.Entry) e).getValue(),(Map)target.get(((Map.Entry) e).getKey()));
                }else{
                    target.put(((Map.Entry) e).getKey(),((Map.Entry) e).getValue());
                }

            }
        }
    }

    /**
     * 把src 复制到target,结构相同
     * @param src
     * @param target
     * exampls
     * ["Host Operations",{"Log Operations":["Realtime Catch"]}]
     * ["Service Manage","Data Dictionary",{"Log Operations":["Log Search"]},"Help"]
     */
    public static void mergeObject(Object src,Object target){
        if(src instanceof Map && target instanceof Map){
            Iterator its = ((Map) src).keySet().iterator();
            while(its.hasNext()){
                Object k = its.next();
                Object v = ((Map) src).get(k);
                if(null != v && null!= ((Map) target).get(k) && v.getClass().isAssignableFrom(((Map) target).get(k).getClass())){
                    mergeObject(v,((Map) target).get(k));
                }
                if(null != v && !((Map) target).containsKey(k) || (((Map) target).containsKey(k) && null== ((Map) target).get(k))){
                    ((Map) target).put(k,v);
                }
            }
        }
        if(src instanceof List && target instanceof List){
            for(Object o:(List)src){
                if(o instanceof Map){
                    boolean isin=false;
                    for(Object t:(List)target) {
                        if(t instanceof Map && ((Map)t).keySet().containsAll(((Map) o).keySet())){
                            isin=true;
                            mergeObject(o,t);
                            break;
                        }
                    }
                    if(!isin){
                        ((List)target).add(o);
                    }

                }else if(o instanceof List){
                    boolean isin=false;
                    for(Object t:(List)target) {
                        if(t instanceof List){
                            isin=true;
                            mergeObject(o,t);
                            break;
                        }
                    }
                    if(!isin){
                        ((List)target).add(o);
                    }
                }else{
                    boolean isin=false;
                    for(Object t:(List)target){
                        if(null != t && o !=null && t.equals(o)){
                            isin=true;
                            break;
                        }
                    }
                    if(!isin && null != o){
                        ((List)target).add(o);
                    }
                }
            }
        }
    }

    public static List<Map> convertListMap2TreeStructure(List<Map> ls,String rootId,String parentId,String id,String defChildrenName){
        List<Map > nodeList = new ArrayList();
        for(Map node1 : ls){//taskDTOList 是数据库获取的List列表数据或者来自其他数据源的List
            boolean mark = false;
            for(Map node2 : ls){
                if(node1.get(parentId)!=null && node1.get(parentId).equals(node2.get(id))){
                    mark = true;
                    if(node2.get(defChildrenName) == null)
                        node2.put(defChildrenName,new ArrayList<Map>());
                    ((List)node2.get(defChildrenName)).add(node1);
                    break;
                }
            }
            if(!mark){
                nodeList.add(node1);
            }
        }
        return nodeList;
    }
    static void convert2MapList(List src,String pid,List target,String pidName,String idName,String nameName){
        if(null != src) {
            for (Object o : src) {
                Map m = new HashMap();
                if(o instanceof String){
                    m.put(idName,o.hashCode()+"");
                    m.put(nameName,o);
                    m.put(pidName,pid);
                    target.add(m);
                }else if(o instanceof Map){
                    Iterator its  = ((Map)o).keySet().iterator();
                    while(its.hasNext()){
                        String k = (String)its.next();
                        Object v = ((Map)o).get(k);
                        m.put(idName,k.hashCode()+"");
                        m.put(nameName,k);
                        m.put(pidName,pid);
                        target.add(m);
                        if(null != v && v instanceof List){
                            convert2MapList((List)v,k.hashCode()+"",target,pidName,idName,nameName);
                        }
                    }
                }
            }
        }
    }
    public static List<Map> convertJsons2TreeStructure(List ls,String defIdName,String defParentIdName,String defChildrenName,String nameName){
        if(null != ls){
            List ll =new LinkedList();
            convert2MapList(ls,"",ll,defParentIdName,defIdName,nameName);
            return convertListMap2TreeStructure(ll,null,defParentIdName,defIdName,defChildrenName);
        }
        return null;
    }


    /**
     * 合并list或map的字符串
     * @param list 内容为list或map的字符串
     * @return
     */
    public static String mergeListString(List<String> list) throws IOException {
        if(null != list){
            if(list.get(0).startsWith("[")){
                List ret = new LinkedList();
                for(String s:list){
                    List li = StringUtils.convert2ListJSONObject(s);
                    mergeObject(li,ret);
                }
                return convertList2String(ret);
            }else if(list.get(0).startsWith("{")){
                Map ret = new LinkedHashMap();
                for(String s:list){
                    Map m = StringUtils.convert2MapJSONObject(s);
                    mergeObject(m, ret);
                }
                return ObjectUtils.convertMap2String(ret);
            }

        }
        return null;
    }
    public static void main(String[] args){
        //System.out.println("TEST.F2".compareTo("TEST.G1"));
        /*System.out.println(DateTimeUtils.date2String(new Date(),"yyyy-MM-dd HH:mm:ss"));
        HashMap map1 = new HashMap();
        map1.put("src",StringUtils.convert2ListJSONObject("[[{'www1':'w1'},{'www2':'w2'},{'www3':'w3'}],[{'www1':'j1'},{'www2':'j2'},{'www3':'j3'}]]"));
        HashMap map3 = new HashMap();
        map3.put("src",StringUtils.convert2ListJSONObject("[[{'www1':'m1'},{'www2':'m2'},{'www3':'m3'}],[{'www1':'k1'},{'www2':'k2'},{'www3':'k3'}]]"));

        MappingInfo strs = ObjectUtils.getMappingInfoFromJson("[{name:'root',type:'java.util.HashMap',children:[{name:'wf',type:'java.util.HashMap',children:[{name:'tt',type:'java.util.LinkedList',children:[{type:'java.util.HashMap',children:[{name:'f1',type:'java.lang.String'},{name:'f2',type:'java.lang.String'}]}]}]}]}]"
                ,"{'root.wf.tt.f1':'src.www1','root.wf.tt.f2':'src.www2'}");

        MappingInfo strs2 = ObjectUtils.getMappingInfoFromJson("[{name:'root',type:'java.util.HashMap',children:[{name:'ww',type:'java.util.HashMap',children:[{name:'tt',type:'java.util.LinkedList',children:[{type:'java.util.HashMap',children:[{name:'f1',type:'java.lang.String'},{name:'f2',type:'java.lang.String'}]}]}]}]}]"
                ,"{'root.ww.tt.f1':'src.www1','root.ww.tt.f2':'src.www2'}");

        HashMap map2 = new HashMap();
        ObjectUtils.mapping(map1,strs,map2);
        HashMap map4 = new HashMap();
        long l = System.currentTimeMillis();
        ObjectUtils.mapping(map3,strs2,map4);
        System.out.println("cost time:"+(System.currentTimeMillis()-l));
        System.out.println(map4);

        String s1 ="[\"Host Operations\",{\"Log Operations\":[\"Realtime Catch\"]}]";
        String s2 = "[\"Service Manage\",\"Data Dictionary\",{\"Log Operations\":[\"Log Search\"]},\"Help\"]";
        List li = new ArrayList();
        li.add(s1);
        li.add(s2);
        String ret = mergeListString(li);
        System.out.println(ret);
*/
        /*String s= StringUtils.replaceEach("'Log Search': {\n" +
                "                url: 'logSearch.html',\n" +
                "                target: '_blank',\n" +
                "                icon: 'icon-search',\n" +
                "                active: 2\n" +
                "            },",jsonchar,jsonEncodeChars);
        System.out.print(s);*/
        /*List ls = StringUtils.convert2ListJSONObject("[\"Home\",\"Service Manage\",\"Data Dictionary\",{\"Log Operations\":[\"Log Search\",\"Realtime Catch\"]},\"Server Operations\",\"Host Operations\",\"Alarm\",{\"Reports\":[\"Trade Log Analyze\"]},{\"I18N\":[\"Page I18N\",\"Data I18n\",\"Service I18N\",\"Exception I18n\"]},{\"Contract\":[\"roleManage\",\"account\"]},{\"Tools\":[\"Release Note Compare\",\"SVN Operate Log\",\"Shell Run Excel Config\",\"ZkQuery\",\"ES Tool\",\"Kibana\"]},\"Applications Manage\",\"Help\"]");
        List<Map> rt = convertJsons2TreeStructure(ls,"id","pId","children","name");
        System.out.println(rt);*/
        try {
            String s = convertInputStream2Base64String(new FileInputStream(new File("C:\\Users\\Administrator\\Pictures\\7.png")));
            System.out.println(s);
            FileOutputStream out  = new FileOutputStream(new File("c:/logs/a.png"));
            InputStream i = convertBase64String2InputStream(s);
            byte[] b= new byte[i.available()];
            i.read(b);
            out.write(b);
        }catch (Exception e){
            e.printStackTrace();
        }
     }

    static String[] jsonchar = new String[]{"{","}","[","]","\"","'",":",",","\n"};
    static String[] jsonEncodeChars = new String[]{"%7B","%7D","%5B","%5D","%22","%27","%3A","%2C","%0A"};
    public static Map encodeJsonCharForMap(Map<String,String> data){
        if(null != data){
            Iterator<String> its = data.keySet().iterator();
            while(its.hasNext()){
                String k = its.next();
                String v = String.valueOf(data.get(k));
                String ek = StringUtils.replaceEach(k,jsonchar,jsonEncodeChars);
                String ev = StringUtils.replaceEach(v,jsonchar,jsonEncodeChars);
                if(null != k && k.equals(ek)){
                    data.put(ek,ev);
                }else{
                    data.put(ek,ev);
                    data.remove(k);
                }
            }
            return data;
        }
        return data;
    }

    public static Map decodeJsonCharForMap(Map<String,String> data){
        if(null != data){
            Iterator<String> its = data.keySet().iterator();
            while(its.hasNext()){
                String k = its.next();
                String v = data.get(k);
                String ek = StringUtils.replaceEach(k,jsonEncodeChars,jsonchar);
                String ev = StringUtils.replaceEach(v,jsonEncodeChars,jsonchar);
                if(k.equals(ek)){
                    data.put(ek,ev);
                }else{
                    data.put(ek,ev);
                    data.remove(k);
                }
            }
            return data;
        }
        return data;
    }
    public static List encodeJsonCharForMapList(List<Map<String,String>> data){
        if(null != data){
            for(Map m:data){
                encodeJsonCharForMap(m);
            }
            return data;
        }
        return data;
    }
    public static List decodeJsonCharForMapList(List<Map<String,String>> data){
        if(null != data){
            for(Map m:data){
                decodeJsonCharForMap(m);
            }
            return data;
        }
        return data;
    }
}
