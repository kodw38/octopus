package com.octopus.utils.cls;

import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cls.jcl.MyURLClassLoader;
import com.octopus.utils.cls.proxy.*;
import com.octopus.utils.exception.ISPException;
import com.octopus.utils.exception.Logger;
import com.octopus.utils.file.FileUtils;
import com.octopus.utils.time.DateTimeUtils;
import com.octopus.utils.zip.ZipUtil;
import jj2000.j2k.util.ArrayUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xsocket.Execution;

import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.lang.reflect.*;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Timestamp;
import java.util.*;

/*
import org.objectweb.asm.*;
*/

/**
 * User: wangfeng2
 * Date: 14-8-15
 * Time: 上午11:25
 */
public class ClassUtils {
    static transient Log log = LogFactory.getLog(ClassUtils.class);

    static String[] SimpleType={"int","java.lang.String","boolean","float","double","short","long","byte","char"
            ,"java.lang.Integer","java.lang.Double","java.lang.Float","java.lang.Long","java.lang.Character","java.lang.Boolean"
            ,"java.lang.Byte","java.lang.Short","java.util.Date"
            ,"java.util.Calendar","java.sql.Date","java.sql.Time","java.sql.Timestamp","java.math.BigDecimal","java.math.BigInteger","java.lang.Object"
            ,"java.net.URI","java.util.ArrayList","java.util.Vector","java.util.List","java.util.HashMap","java.util.Hashtable","java.util.Map"};

    static String[] SimpleExcludeContainerType={"int","java.lang.String","boolean","float","double","short","long","byte","char"
            ,"java.lang.Integer","java.lang.Double","java.lang.Float","java.lang.Long","java.lang.Character","java.lang.Boolean"
            ,"java.lang.Byte","java.lang.Short","java.util.Date"
            ,"java.util.Calendar","java.sql.Date","java.sql.Time","java.sql.Timestamp","java.math.BigDecimal","java.math.BigInteger","java.lang.Object"
            ,"java.net.URI"};

    static Map<String,Class> primitiveArrayMap  = new HashMap();
    static List ObjectMehots = new ArrayList();
    static{
        ObjectMehots.add("wait");
        ObjectMehots.add("clone");
        ObjectMehots.add("equals");
        ObjectMehots.add("hashCode");
        ObjectMehots.add("toString");
        ObjectMehots.add("finalize");
        ObjectMehots.add("getClass");
        ObjectMehots.add("notify");
        ObjectMehots.add("notifyAll");
    }
    static {
        primitiveArrayMap.put("int",int[].class);
        primitiveArrayMap.put("boolean",boolean[].class);
        primitiveArrayMap.put("float",float[].class);
        primitiveArrayMap.put("double",double[].class);
        primitiveArrayMap.put("short",short[].class);
        primitiveArrayMap.put("long",long[].class);
        primitiveArrayMap.put("byte",byte[].class);
        primitiveArrayMap.put("char",char[].class);
    }

    static Map<String,Class> primitiveMap  = new HashMap();
    static {
        primitiveMap.put("int",int.class);
        primitiveMap.put("boolean",boolean.class);
        primitiveMap.put("float",float.class);
        primitiveMap.put("double",double.class);
        primitiveMap.put("short",short.class);
        primitiveMap.put("long",long.class);
        primitiveMap.put("byte",byte.class);
        primitiveMap.put("char",char.class);
    }
    static Map<Class,Class> primitiveObjectMap  = new HashMap();
    static {
        primitiveObjectMap.put(int.class,Integer.class);
        primitiveObjectMap.put(boolean.class,Boolean.class);
        primitiveObjectMap.put(float.class,Float.class);
        primitiveObjectMap.put(double.class,Double.class);
        primitiveObjectMap.put(short.class,Short.class);
        primitiveObjectMap.put(long.class,Long.class);
        primitiveObjectMap.put(byte.class,Byte.class);
        primitiveObjectMap.put(char.class,Character.class);
    }
    static Map<String,String> primitiveNameObjectMap  = new HashMap();
    static {
        primitiveNameObjectMap.put(int.class.getName(),Integer.class.getName());
        primitiveNameObjectMap.put(boolean.class.getName(),Boolean.class.getName());
        primitiveNameObjectMap.put(float.class.getName(),Float.class.getName());
        primitiveNameObjectMap.put(double.class.getName(),Double.class.getName());
        primitiveNameObjectMap.put(short.class.getName(),Short.class.getName());
        primitiveNameObjectMap.put(long.class.getName(),Long.class.getName());
        primitiveNameObjectMap.put(byte.class.getName(),Byte.class.getName());
        primitiveNameObjectMap.put(char.class.getName(),Character.class.getName());
    }
    static Map<String,String> primitiveDefaultValue  = new HashMap();
    static {
        primitiveDefaultValue.put("int","0");
        primitiveDefaultValue.put("boolean","false");
        primitiveDefaultValue.put("float","0.0");
        primitiveDefaultValue.put("double","0.0");
        primitiveDefaultValue.put("short","0");
        primitiveDefaultValue.put("long","0");
        primitiveDefaultValue.put("byte","0");
        primitiveDefaultValue.put("char","'0'");
    }

    static Class[] DefineClassParameterTypes = new Class[]{ClassLoader.class, String.class,byte[].class, int.class, int.class};

    public static boolean isSameType(Class c1,Class c2){
        if(null ==c1 && null ==c2)return true;
        if(null!=c1 && null!=c2 && c1.equals(c2)){
            return true;
        }
        if(null!=c1 && c1.isPrimitive()){
            Object o = primitiveObjectMap.get(c1);
            if(o.equals(c2)){
                return true;
            }
        }
        if(null!=c2 && c2.isPrimitive()){
            Object o = primitiveObjectMap.get(c2);
            if(o.equals(c1)){
                return true;
            }
        }
        return false;
    }
    public static boolean isSameType(Class[] c1,Class[] c2){
        if((null==c1 && null!=c2)||(c1!=null && c2==null)||(null!=c1 && null!=c2 && c1.length!=c2.length)){
           return false;
        }
        if(null!=c1 && c2!=null){
            for(int i=0;i<c1.length;i++){
                if(!isSameType(c1[i],c2[i]))
                    return false;
            }
        }
        return true;
    }

    public static boolean isSimple(String clz){
        for (String type:SimpleType)
            if(type.equals(clz))
                return true;
        if ((clz.startsWith("java.lang")) || (clz.startsWith("javax."))) {
            return true;
        }
        return false;
    }
    public static boolean isSimpleExcludeContainer(String clz){
        for (String type:SimpleExcludeContainerType)
            if(type.equals(clz))
                return true;
        return false;
    }

    public static Object convertSimpleObject(String clz,Object o){
        if(clz.equals("java.lang.Boolean") && o instanceof String){
            return StringUtils.isTrue((String)o);
        }
        if(clz.equals("java.lang.Boolean") && o instanceof Integer){
            return (Integer)o>0;
        }
        return o;
    }

    /**
     *
     * @param className
     * @return
     */
    public static String getClassTypeDefaultValue(String className){
        String ret = primitiveDefaultValue.get(className);
        if(null == ret)return "null";
        return ret;
    }
    public static String getPrimitiveObjectNameByPrimitiveName(String name){
        return primitiveNameObjectMap.get(name);
    }

    /**
     * 简单类型转为对象
     * @param i
     * @return
     */
    public static Object convertPrimitive2Object(int i){
        return new Integer(i);
    }
    public static Object convertPrimitive2Object(long i){
        return new Long(i);
    }
    public static Object convertPrimitive2Object(float i){
        return new Float(i);
    }
    public static Object convertPrimitive2Object(double i){
        return new Double(i);
    }
    public static Object convertPrimitive2Object(char i){
        return new Character(i);
    }
    public static Object convertPrimitive2Object(byte i){
        return new Byte(i);
    }
    public static Object convertPrimitive2Object(short i){
        return new Short(i);
    }
    public static Object convertPrimitive2Object(boolean i){
        return new Boolean(i);
    }

    public static int convertObject2Primitive(Integer i){
        if(null != i)
        return  i.intValue();
        return 0;
    }
    public static long convertObject2Primitive(Long i){
        if(null != i)
        return i.longValue();
        return 0;
    }
    public static float convertObject2Primitive(Float i){
        if(null != i)
        return i.floatValue();
        return 0;
    }
    public static double convertObject2Primitive(Double i){
        if(null != i)
        return i.doubleValue();
        return 0;
    }
    public static char convertObject2Primitive(Character i){
        if(null != i)
        return i.charValue();
        return 0;
    }
    public static byte convertObject2Primitive(Byte i){
        if(null != i)
        return i.byteValue();
        return 0;
    }
    public static short convertObject2Primitive(Short i){
        if(null != i)
        return i.shortValue();
        return 0;
    }
    public static boolean convertObject2Primitive(Boolean i){
        if(null != i)
        return i.booleanValue();
        return false;
    }


    public static boolean isInterfaceImplMatching(Class inter,Class impl){
        return inter.isAssignableFrom(impl);
    }
    public static boolean isExtendFrom(String name,Class c){
        Class r = getRootExtend(c);

        if(null != r && r.getName().equals(name)){
            return true;
        }else{
            return false;
        }
    }
    public static Class getRootExtend(Class c){
        Class r = c;
        while(null !=r && null !=r.getSuperclass() && !r.getSuperclass().getName().equals(Object.class.getName())){
            r = r.getSuperclass();
        }
        return r;
    }

    /**
     * 获取对象中的属性
     * @param obj
     * @param fieldName
     * @param isThrowException
     * @return
     * @throws NoSuchFieldException
     */
    static Map<String,Field> fieldMap = new HashMap();
    public static Field getField(Object obj,String fieldName,boolean isThrowException) throws NoSuchFieldException {
        String k = obj.getClass().getName()+fieldName;
        Field ff = fieldMap.get(k);
        if(null != ff){
            return ff;
        }
        Class c = obj.getClass();
        while (null != c) {
            try {
                Field[] fs = c.getDeclaredFields();
                for (Field f : fs) {
                    if (f.getName().equals(fieldName)) {
                        f.setAccessible(true);
                        fieldMap.put(k, f);
                        return f;
                    }
                }
                c = c.getSuperclass();
            }catch (Throwable e){
                System.out.println();
            }
        }
        if (isThrowException)
            throw new NoSuchFieldException(c.getName() + "not find field:" + fieldName);
        return null;

    }

    public static Field[] getAllField(Object obj){
        Class c = obj.getClass();
        List ret = new ArrayList();
        while(null != c){
            Field[] fs = c.getDeclaredFields();
            for(Field f:fs){
                ret.add(f);
            }
            c = c.getSuperclass();
        }
        return (Field[])ret.toArray(new Field[0]);
    }
    static String[] jdkpaths = new String[]{"java.lang"};
    public static Field[] getAllFieldExcludeJDK(Class c){
        List ret = new ArrayList();
        if(null != c) {
            Class temp = c;
            while (null != temp) {
                Field[] fs = temp.getDeclaredFields();
                for (Field f : fs) {
                    ret.add(f);
                }
                temp = temp.getSuperclass();
                if(null != temp && ArrayUtils.isLikeStringArray(jdkpaths,temp.getName())) {
                    break;
                }
            }
            return (Field[]) ret.toArray(new Field[0]);
        }else{
            return null;
        }
    }
    public static List<String> getExtendsClass(Class c){
        LinkedList li = new LinkedList();
        getSuperClass(c,li);
        return li;
    }
    public static void getSuperClass(Class c,List li){
        Class n = c.getSuperclass();
        if(null !=n){
            li.add(n.getName());
            getSuperClass(n,li);
        }
    }

    static String getFieldGetSetName(String name){
        StringBuffer sb = new StringBuffer();
        sb.append(name);
        if (Character.isLowerCase(sb.charAt(0))) {
            if (sb.length() == 1 || !Character.isUpperCase(sb.charAt(1))) {
                sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
            }
        }
        return sb.toString();
    }

    public static Field[] getGetFields(Object obj){
        Field[] fs = getAllFieldExcludeJDK(obj.getClass());//obj.getClass().getDeclaredFields();
        Method[] ms = obj.getClass().getMethods();
        List<Field> ret = new ArrayList<Field>();
        for(Field f:fs){
            String mn = "get"+getFieldGetSetName(f.getName());
            String mi = "is"+getFieldGetSetName(f.getName());

            try{
                for(Method m:ms){
                    if((m.getName().equals(mn) || m.getName().equals(mi)) && (null == m.getParameterTypes() || m.getParameterTypes().length==0)){
                        ret.add(f);
                    }
                }
            }catch (Exception e){

            }
        }
        if(ret.size()>0)
        return ret.toArray(new Field[0]);
        return null;
    }

    /**
     * 给一个属性设置值
     * @param obj
     * @param fieldName
     * @param value
     */

    public static boolean setFieldValue(Object obj,String fieldName,Object value,boolean isThrowException) throws IllegalAccessException,Exception {
        long l = System.currentTimeMillis();
        try{

        Field f = getField(obj, fieldName,isThrowException);
        if(null != f){

            if(null == value) return false;
                //f.set(obj,null);
            else if( f.getType().equals(value.getClass()) && f.getType().isAssignableFrom(value.getClass())){
                f.set(obj,value);
            }else{
                f.set(obj,chgValue(fieldName,f.getType(),value));
            }
            return true;
        }
        return false;
        }catch (Exception e){
            throw new Exception(obj.getClass().getName(),e);
        }
    }
    public static Object chgValue(String name,Class c,Object o)throws Exception{
        try {
            if (null == o) return o;
            if(null == c)return o;
            if (isSameType(c, o.getClass())) {
                return o;
            }
            if (c.getName().equals("java.lang.String") && !o.getClass().isArray() && !Collection.class.isAssignableFrom(o.getClass())) {
                return o.toString();
            }
            if (c.getName().equals("double")) {
                return Double.valueOf(o.toString());
            }
            if (c.getName().equals("java.lang.Boolean") || c.getName().equals("boolean")) {
                if (o instanceof Integer) {
                    return (Integer) o > 0;
                } else if (o instanceof String) {
                    return StringUtils.isTrue((String) o);
                } else {
                    return null != o;
                }
            }
            if ((c.getName().equals("java.lang.Float")) || ((c.getName().equals("float")))) {
                //try {
                    if (null == o || "".equals(o)) {
                        return 0;
                    }
                    if ((o instanceof String)) {
                        return Float.valueOf((String) o);
                    }
                    if (o instanceof Integer) {
                        return new Float((Integer) o);
                    }
                /*} catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }*/
            }
            if (c.getName().equals("java.util.Date") && o instanceof String) {
                //try {
                    return DateTimeUtils.getDate((String) o);
                /*} catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }*/
            }
            if (c.getName().equals("java.sql.Timestamp") && o instanceof String) {
                //try {
                    Date d = DateTimeUtils.getDate((String) o);
                    return new Timestamp(d.getTime());
                /*} catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }*/
            }
            if (c.getName().equals("int")) {
                //try {
                    if (null == o || "".equals(o)) {
                        return 0;
                    }
                    if (o instanceof String) {
                        return Integer.parseInt((String) o);
                    }
                    if(o instanceof Long){
                        return ((Long)o).intValue();
                    }
                    if(o instanceof Integer){
                        return ((Integer)o).intValue();
                    }
                /*} catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }*/
            }
            if (c.getName().equals("java.lang.Integer")) {
                //try {
                    if (null == o || "".equals(o)) {
                        return null;
                    }
                    if (o instanceof String) {
                        return Integer.parseInt((String) o);
                    }
                    if(o instanceof Long){
                        return ((Long)o).intValue();
                    }

                /*} catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }*/
            }
            if (c.getName().equals("long")) {
                //try {
                    if (null == o || "".equals(o)) {
                        return 0;
                    }
                    if (o instanceof String) {
                        return Long.parseLong((String) o);
                    }
                    if(o instanceof Long){
                        return ((Long)o).longValue();
                    }
                    if(o instanceof Integer){
                        return ((Integer)o).longValue();
                    }
                /*} catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }*/
            }
            if (c.getName().equals("java.lang.Long")) {
                //try {
                    if (null == o || "".equals(o)) {
                        return null;
                    }
                    if (o instanceof String) {
                        return Long.parseLong((String) o);
                    }
                    if(o instanceof Long){
                        return ((Long)o);
                    }
                    if(o instanceof Integer){
                        return ((Integer)o).longValue();
                    }
                /*} catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }*/
            }
            if (c.getName().equals("java.lang.Long") || c.getName().equals("long") && o instanceof Integer) {
                //try {
                    return new Long((Integer) o);
                /*} catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }*/
            }
            if (List.class.isAssignableFrom(c) && o instanceof List) {
                return o;
            }
            if (Map.class.isAssignableFrom(c) && o instanceof Map) {
                return o;
            }
            if (Map.class.isAssignableFrom(c) && o instanceof String) {
                if (StringUtils.isNotBlank((String) o)) {
                    return StringUtils.convert2MapJSONObject((String) o);
                } else {
                    return null;
                }
            }
            if (o.getClass().isArray()) {
                List list = new ArrayList();
                for (Object n : (Object[]) o)
                    list.add(n);
                return list;
            }
            if (o instanceof String && c.isArray()) {
                List list = new ArrayList();
                list.add(o);
                return list;
            }
            if (o instanceof String && List.class.isAssignableFrom(c)) {
                List list = new ArrayList();
                list.add(o);
                return list;
            }
            if (c.getName().equals("java.lang.Object")) {
                return o;
            }

            if (InputStream.class.isAssignableFrom(c)) {
                return o;
            }
            if (c.isAssignableFrom(o.getClass())) {
                return o;
            }
        }catch (Exception e){
            log.error(e);
            throw new ISPException("ISP00017","Invalid data [(data)] type in [(classType)] parameter name [(name)]",new String[]{(o==null?"":o.toString()),c.getName(),name});
        }
        throw new RuntimeException(ClassUtils.class.getName()+". now not support the type convert value:"+o.getClass().getName() +" to "+c.getName()+" value "+o);
    }

    /**
     * is the object of the class mulit
     * @param c
     * @return
     */
    public static boolean isArray(Class c){
        if(c.isArray()){
            return true;
        }else if(Collection.class.isAssignableFrom(c)){
            return true;
        }
        return false;
    }

    /**
     * 获取对象中的属性值
     * @param obj
     * @param fieldName
     * @param isThrowException
     * @return
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public static Object getFieldValue(Object obj,String fieldName,boolean isThrowException) throws NoSuchFieldException, IllegalAccessException {
        Field f = getField(obj, fieldName,isThrowException);
        if(null != f){
            f.setAccessible(true);
            return f.get(obj);
        }
        return null;
    }
    public static Method getMethodByName(Class clazz , String methodName){
        Method[] ms =  clazz.getDeclaredMethods();
        for(Method m:ms){
            if(m.getName().equals(methodName)){
                return m;
            }
        }
        return null;
    }
    public static Method getMethod(Class clazz, String methodName,Class[] classes)  {
        Method method = null;
        try {
            method = clazz.getDeclaredMethod(methodName, classes);
        } catch (NoSuchMethodException e) {
            try {
                method = clazz.getMethod(methodName, classes);
            } catch (NoSuchMethodException ex) {
                if (clazz.getSuperclass() == null) {
                    return method;
                } else {
                    method = getMethod(clazz.getSuperclass(), methodName,
                            classes);
                }
            }
        }
        return method;
    }
    public static Object invokeMethod(Object o,String method,Class[] parsClass,Object[] pars) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        if(null == o){
            return null;
        }
        Method tm = getMethod(o.getClass(), method, parsClass);
        if(null != tm) {
            tm.setAccessible(true);
            return tm.invoke(o, pars);
        }else{
            throw new NoSuchMethodException(method);
        }

    }

    public static Object invokeMethod(Object o,String method,Class[] parsClass,Object[] pars,boolean isthrowException) throws Throwable {
        try {
            if (null == o) {
                return null;
            }
            Method tm = getMethod(o.getClass(), method, parsClass);
            tm.setAccessible(true);
            return tm.invoke(o, pars);

        }catch (Throwable e){
            if(isthrowException){
                throw e;
            }else{
                return null;
            }
        }
    }

    public static Class<?>[] getClasses(final Object... array) {
        if (array == null) {
            return null;
        } else if (array.length == 0) {
            return ArrayUtils.EMPTY_CLASS_ARRAY;
        }
        final Class<?>[] classes = new Class[array.length];
        for (int i = 0; i < array.length; i++) {
            classes[i] = array[i] == null ? null : array[i].getClass();
        }
        return classes;
    }
    public static Map<String,InputStream> getFilesFromLoader(String dir,final String endwith) throws Exception {
        URL templateUri = ClassUtils.class.getClassLoader().getResource(dir);
        if(templateUri == null) {
            System.out.println(dir + " not exists");
            return null;
        }
        URI path = templateUri.toURI();
        log.info("load file from "+path.toString());
        if("file".equals(path.getScheme())){
            File fileTemplateDir = new File(path);
            if (StringUtils.isNotBlank(endwith)) {
                File[] templateFiles = fileTemplateDir.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.endsWith(endwith);
                    }
                });
                return FileUtils.convertFileListToMap(templateFiles);
            } else {
                return FileUtils.convertFileListToMap(fileTemplateDir.listFiles());
            }
        }else{
            return ZipUtil.getZipFiles(path,endwith);
        }
    }


    public static void main(String[] args){
        try{

            //getFilesFromLoader("com/alibaba/fastjson/annotation/", null);

        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 调用类中的方法
     * @param c
     * @param method
     * @param cs
     * @param args
     * @return
     * @throws Exception
     */
    public static Object invokeMethod(Class c,String method,Class[] cs ,Object[] args)throws Exception{
        try {
            Method m = c.getMethod(method, cs);
            if (null == m)
                throw new ClassNotFoundException(" not find  " + c.getName() + " " + method + "(" + class2String(cs) + ")");
            m.setAccessible(true);

            return m.invoke(c.newInstance(), args);
        }catch (Exception e){
            log.error("invokeMethod error:"+c+"|"+method+"|"+cs+"|"+args);
            throw e;
        }
    }

    /**
     * 调用类中的静态方法
     * @param c
     * @param method
     * @param cs
     * @param args
     * @return
     * @throws NoSuchMethodException
     * @throws ClassNotFoundException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public static Object invokeStaticMethod(Class c,String method,Class[] cs ,Object[] args) throws NoSuchMethodException, ClassNotFoundException, InvocationTargetException, IllegalAccessException {
        Method m = c.getMethod(method,cs);
        if(null == m)throw new ClassNotFoundException(" not find  "+c.getName()+ " "+method+"("+class2String(cs)+")");
        m.setAccessible(true);
        return m.invoke(null,args);
    }

    /**
     * 获取类描述
     * @param cs
     * @return
     */
    private static String class2String(Class[] cs){
        if(null !=cs){
            StringBuffer sb = new StringBuffer();
            for(Class c:cs){
                if(sb.length()==0){
                    toDescriptor(sb,c);
                }else{
                    sb.append(",");
                    toDescriptor(sb,c);
                }
            }
            return sb.toString();
        }
        return "";
    }

    private static void toDescriptor(StringBuffer desc, Class type) {
        if (type.isArray()) {
            desc.append((char)'[');
            try {
                toDescriptor(desc, type.getComponentType());
            }
            catch (Exception e) {

            }
        }
        else if (type.isPrimitive()) {
            desc.append((char)'L').append(type.getName()).append((char)';');
        }else {
            desc.append((char)'L').append(type.getName()).append((char)';');//.replace((char)'.', (char)'/')
        }
    }

    /**
     * 根据方法参数描述获取参数类
     * @param parameterDeclare
     * @return
     * @throws Exception
     */
    public static Class[] getParametersClass(String parameterDeclare)throws Exception{
        String p = parameterDeclare.trim();
        if(p.startsWith("(")){
            p = p.substring(1,p.length()-1);
        }
        if(!"".equals(p)){
            String[] cs=p.split(",");
            return getClasses(cs);
        }
        return null;
    }

    /**
     * 根据类型名称获取类
     * @param pars
     * @return
     * @throws Exception
     */
    public static Class[] getClasses(String[] pars)throws Exception{
        if(null != pars){
                if(ArrayUtils.isNotEmpty(pars)){
                    Class[] ret = new Class[pars.length];
                    for(int i=0;i<pars.length;i++){
                        if(pars[i].startsWith("[")){
                            Class c = getPrimitiveArrayClass(pars[i].substring(1));
                            if(null != c){
                                ret[i]=c;
                            }else{
                                ret[i]=ret[i] = Array.newInstance(Class.forName(pars[i].substring(1)), 0).getClass();
                            }
                        }else{
                            Class c = getPrimitiveClass(pars[i]);
                            if(null != c){
                                ret[i]= c;
                            }else{
                                ret[i]=Class.forName(pars[i]);
                            }
                        }
                    }
                    return ret;
                }
        }
        return null;

    }

    /**
     * 根据名称获取primitive数组类
     * @param primitiveClass
     * @return
     */
    public static Class getPrimitiveArrayClass(String primitiveClass){
        return primitiveArrayMap.get(primitiveClass);
    }

    /**
     * 根据名称获取primitive类
     * @param primitiveClass
     * @return
     */
    public static Class getPrimitiveClass(String primitiveClass){
        return primitiveMap.get(primitiveClass);
    }

    /**
     * 根据名称获取primitive类
     * @param primitiveClass
     * @return
     */
    public static Class getPrimitiveObjectClass(Class primitiveClass){
        return primitiveObjectMap.get(primitiveClass);
    }

    /**
     * 给一个类所实现的所有接口类方法增加IMethodAddition方法
     * @param implClass
     * @param action
     * @return
     * @throws Exception
     */
    public static Object getProxyObject(Class implClass,IMethodAddition[] action,Class[] constructorClass,Object[] pars,IProxyHandler handler)throws Exception{
        //根据接口类的方法设置代理方法
        Class[] intrs= ClassUtils.getAllInterfaces(implClass);
        if(null == intrs){
            return implClass.newInstance();
        }else{
            Method[] ms= getPublicMethods(intrs);
            if(null != ms){
                return getProxyObject(implClass,ms,action,constructorClass,pars,handler);
            }else{
                if(null == constructorClass){
                    return implClass.newInstance();
                }else{
                    return implClass.getConstructor(constructorClass).newInstance(pars);
                }
            }
        }
    }
    public static Object getProxyObject(Class implClass,List<String> methodsName,boolean isAllMethod,IMethodAddition[] action,Class[] constructorClass,Object[] pars,IProxyHandler handler)throws Exception{
        //根据接口类的方法设置代理方法
        Class[] intrs= ClassUtils.getAllInterfaces(implClass);
        if(null == intrs){
            return implClass.newInstance();
        }else{
            Method[] ms= getPublicMethods(intrs);
            if(!isAllMethod && null != ms && null != methodsName && methodsName.size()>0){
                List<Method> li = new LinkedList();
                for(Method m:ms){
                    if(ArrayUtils.isInStringArray(methodsName,m.getName())){
                        li.add(m);
                    }
                }
                ms = (Method[])li.toArray(new Method[0]);
                return getProxyObject(implClass,ms,action,constructorClass,pars,handler);
            }else if(isAllMethod && null != ms){
                return getProxyObject(implClass,ms,action,constructorClass,pars,handler);
            }else{
                if(null == constructorClass){
                    return implClass.newInstance();
                }else{
                    return implClass.getConstructor(constructorClass).newInstance(pars);
                }
            }
        }
    }
    public static boolean exist(String cla){

        try {
            ClassUtils.class.getClassLoader().loadClass(cla);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }

    }

    public static Object instance(String c,Object o) throws Exception {
        Class ct = getClass(null,c);
        if(null == o){
            return ct.newInstance();
        }else{
            Constructor[] cs = ct.getConstructors();
            if(null == cs || cs.length==0) throw new ClassNotFoundException("not find parameter "+o +" contructor");
            for(Constructor cm:cs){
                Class[] cc = cm.getParameterTypes();
                if(!o.getClass().isArray() && cc.length==1){
                    return cm.newInstance(chgValue(cm.getName(),cc[0],o));
                }
            }
        }
        throw new InstantiationException("now not support class instance "+c);
    }

    /**
     * 获取publiec方法
     * @param cs
     * @return
     */
    public static Method[] getPublicMethods(Class[] cs){
        List<Method> li = new ArrayList();
        for(Class c:cs){
            Method[] ms = c.getMethods();
            if(null != ms){
                for(Method m:ms){
                    if(Modifier.isPublic(m.getModifiers()) && !li.contains(m) ){
                        li.add(m);
                    }
                }
            }
        }
        if(li.size()>0){
           return li.toArray(new Method[0]);
        }
        return null;
    }

    public static Method[] getThisPublicMethods(Class c){
        List<Method> li = new ArrayList();
        Method[] sm =  c.getMethods();
        for(Method m:sm){
           if(Modifier.isPublic(m.getModifiers())&& !ObjectMehots.contains(m.getName())){
               li.add(m);
           }
        }
        if(li.size()>0)
        return li.toArray(new Method[0]);
        return null;
    }


    /**
     * 给一个实现类的指定方法增加IMethodAddition方法
     * @param implClass
     * @param methods
     * @param addition
     * @return
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public static Object getProxyObject(Class implClass,Method[] methods,IMethodAddition[] addition ,Class[] constructorClass,Object[] pars,IProxyHandler handler) throws IllegalAccessException, InstantiationException {
        if(null == methods)return implClass.newInstance();
        if(handler==null)
            handler=new DefaultHandler();
        return ObjectProxy.newProxyInstance(implClass, methods, addition,constructorClass,pars,handler);
    }

    /**
     * 获取一个类的所有接口类
     * @param impl
     * @return
     */
    public static Class[] getAllInterfaces(Class impl){
        Class[] ins = impl.getInterfaces();
        List ret= new ArrayList();
        if(null != ins){
            for(Class in:ins){
                ret.add(in);
            }
        }
        Class sup = impl.getSuperclass();
        if(null != sup){
            Class[] rs = getAllInterfaces(sup);
            if(null != rs){
                for(Class r:rs)
                    ret.add(r);
            }

        }
        return (Class[])ret.toArray(new Class[0]);
    }

    /**
     * 从一个类中获取其实现的最多方法的接口类
     * @param c
     * @return
     */
    public static Class getMaxMethodsInterface(Class c){
        Class[] ins = c.getInterfaces();
        Class ret=null;
        if(null != ins){
            for(Class in:ins){
                if(ret==null) ret = in;
                else{
                    if(ret.getMethods().length<in.getMethods().length)
                        ret = in;
                }
            }
        }
        Class sup = c.getSuperclass();
        if(null != sup){
            Class r = getMaxMethodsInterface(sup);
            if(ret==null){
                ret=r;
            }else{
                if(null !=r && r.getMethods().length>ret.getMethods().length){
                    ret = r;
                }
            }

        }
        return ret;
    }

    /**
     * 根据.class的byte[]内容和指定的classLoader生成class
     * @param loader
     * @param bytes
     * @param className
     * @return
     * @throws Exception
     */
    public static Class defineClass(ClassLoader loader,byte[] bytes,String className) throws Exception {
//        return (Class)ClassUtils.invokeStaticMethod(Proxy.class,"defineClass0",DefineClassParameterTypes,new Object[]{loader, className, bytes, (int)0, (int)bytes.length});
        return (Class)ClassUtils.invokeMethod(loader,"defineClass",new Class[]{String.class,byte[].class,int.class,int.class},new Object[]{className, bytes, (int)0, (int)bytes.length});
    }

    public static Class getClass(ClassLoader loader,String className) throws ClassNotFoundException {
        if(className.contains("<")){
            className = className.substring(0,className.indexOf("<"));
        }
        if(null == loader)loader=ClassUtils.class.getClassLoader();
        /*if(loader instanceof MyURLClassLoader){
            return ((MyURLClassLoader)loader).findClass(className);
        }else {*/
            return loader.loadClass(className);
        //}
    }

    private static Map generatorClasses = Collections.synchronizedMap(new WeakHashMap());

    /**
     *
      * @param className 要生成的类名称
     * @param fields《String,String>  属性名称，属性类型
     * @param methodBodys<String,String> 方法名称，方法代码体
     * @return
     */
    public static Class generatorClass(String className,Map<String,String> fields,Map<String,String> methodBodys){
        try{
            return GeneratorClass.generatorClass(className,fields,methodBodys);
        }catch (Exception e){
            log.error("dynamical compile class exception:",e);
        }
        return null;
    }

    /**
     * generator class by java text, and compil bin file to compilePath, and load compil file to classLoader
     * @param className
     * @param text
     * @param compilePath
     * @return
     */
    public static Class generatorClass(String className,String text,String compilePath){

        return GeneratorClass.generatorClass(className,text,compilePath);

    }

    public static List<String> getLibJarPath(List<String> likename){
        try {
            String s = GeneratorClass.getClassPath(ClassUtils.class.getClassLoader());
            log.info(ClassUtils.class.getClassLoader().getClass().getName()+" load class :\n"+s);
            String[] jars=null;
            if(s.indexOf(";")>=0) {
                jars = s.split("\\;");
            }else{
                jars = s.split("\\:");
            }
            List ret = new ArrayList();
            for (String js : jars) {
                if(null !=likename) {
                    if (ArrayUtils.isLikeArrayInString(js, likename)) {
                        ret.add(js);
                    }
                }else{
                    ret.add(js);
                }
            }
            if (ret.size() > 0) {
                return ret;
            }
        }catch (Exception e){}
        return null;
    }

    public static Class generatorPOJOClass(String  className,Map<String,String> fields){
        try{
            return GeneratorClass.generatorPOJOClass(className,fields);
        }catch (Exception e){
            log.error("dynamical compile class exception:",e);
        }
        return null;
    }

    public static boolean contains(Class c,String[] properties){
        if(null != properties) {
            Method[] fs = c.getMethods();
            boolean[] bs = new boolean[properties.length];
            Arrays.fill(bs,false);
            for (Method f : fs) {
                for(int i=0;i<properties.length;i++) {
                    if(null !=properties[i] && f.getName().equals(properties[i])) {
                        bs[i]=true;
                        break;
                    }
                }
            }
            for(boolean b:bs){
                if(!b){
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /*public static String[] getMethodParameterNames(Class clazz, final Method method) {
        Class[] parameterTypes = method.getParameterTypes();
        if (parameterTypes == null || parameterTypes.length == 0) {
            return null;
        }
        final org.objectweb.asm.Type[] types = new org.objectweb.asm.Type[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            types[i] = org.objectweb.asm.Type.getType(parameterTypes[i]);
        }
        final String[] parameterNames = new String[parameterTypes.length];

        String className = clazz.getName();
        int lastDotIndex = className.lastIndexOf(".");
        className = className.substring(lastDotIndex + 1) + ".class";
        InputStream is = clazz.getResourceAsStream(className);
        try {
            ClassReader classReader = new ClassReader(is);
            classReader.accept(new ClassVisitor(Opcodes.ASM4) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                    // 只处理指定的方法
                    org.objectweb.asm.Type[] argumentTypes = org.objectweb.asm.Type.getArgumentTypes(desc);
                    if (!method.getName().equals(name) || !Arrays.equals(argumentTypes, types)) {
                        return null;
                    }
                    return new MethodVisitor(Opcodes.ASM4) {
                        @Override
                        public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
                            // 静态方法第一个参数就是方法的参数，如果是实例方法，第一个参数是this
                            if (Modifier.isStatic(method.getModifiers())) {
                                parameterNames[index] = name;
                            }
                            else if (index > 0) {
                                parameterNames[index - 1] = name;
                            }
                        }
                    };

                }
            }, 0);
        } catch (IOException e) {
            log.error("get name error",e);
        }
        return parameterNames;
    }*/

}
