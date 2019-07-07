package com.octopus.utils.cls;

import com.alibaba.fastjson.util.ParameterizedTypeImpl;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cls.javassist.*;
import com.octopus.utils.cls.javassist.Modifier;
import com.octopus.utils.cls.javassist.bytecode.CodeAttribute;
import com.octopus.utils.cls.javassist.bytecode.Descriptor;
import com.octopus.utils.cls.javassist.bytecode.LocalVariableAttribute;
import com.octopus.utils.cls.pojo.IUnKnowObjectParse;
import com.octopus.utils.cls.pojo.MethodParameterProperty;
import com.octopus.utils.cls.pojo.PropertyInfo;
import com.octopus.utils.cls.pojo.UnKnowObject;
import com.octopus.utils.cls.proxy.GeneratorClass;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.activation.DataHandler;
import javax.jws.WebParam;
import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;


/**
 * �Լ���Ĳ���
 * User: wangfeng2
 * Date: 14-4-15
 * Time: ����9:42
 */
public class POJOUtil {
    static transient Log log = LogFactory.getLog(POJOUtil.class);

    //���ඨ��
    static String[] SimpleType={"int","java.lang.String","boolean","float","double","short","long","byte","char"
            ,"java.lang.Integer","java.lang.Double","java.lang.Float","java.lang.Long","java.lang.Character","java.lang.Boolean","java.lang.Byte","java.lang.Short","java.util.Date"
            ,"java.util.Calendar","java.sql.Date","java.sql.Time","java.sql.Timestamp","java.math.BigDecimal","java.math.BigInteger","java.lang.Object"
            ,"java.net.URI","java.util.ArrayList","java.util.Vector","java.util.List","java.util.HashMap","java.util.Hashtable","java.util.Map"};

    //����Ϊ�յ�����
    static String[] NotNillableType={
            "int","double","long","boolean","short","float"
    };
    public static String[] NumberType = new String[]{"int","long","double","float","java.lang.Integer","java.lang.Long","java.lang.Double","java.lang.Float"};
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
        primitiveMap.put("java.lang.String",String.class);
        primitiveMap.put(Integer.class.getName(),Integer.class);
        primitiveMap.put(Double.class.getName(),Double.class);
        primitiveMap.put(Boolean.class.getName(),Boolean.class);
        primitiveMap.put(Float.class.getName(),Float.class);
        primitiveMap.put(Long.class.getName(),Long.class);
        primitiveMap.put(Date.class.getName(),Date.class);
        primitiveMap.put(Timestamp.class.getName(),Date.class);

    }
    static Map<String,String> primitiveObjectName  = new HashMap();
    static {
        primitiveObjectName.put("int",Integer.class.getName());
        primitiveObjectName.put("boolean",Boolean.class.getName());
        primitiveObjectName.put("float",Float.class.getName());
        primitiveObjectName.put("double",Double.class.getName());
        primitiveObjectName.put("short",Short.class.getName());
        primitiveObjectName.put("long",Long.class.getName());
        primitiveObjectName.put("byte",Byte.class.getName());
        primitiveObjectName.put("char",Character.class.getName());

    }
    public static String getPrimitiveObjectName(String name){
        String vv= primitiveObjectName.get(name);
        if(StringUtils.isNotBlank(vv)){
            return vv;
        }else{
            return name;
        }
    }
    public static boolean isNumberClass(String type){
        return ArrayUtils.isInStringArray(NumberType,type);
    }

    public static boolean isPrimitive(String className){
        return primitiveMap.containsKey(className);
    }
    static Map<String,Class> primitiveArrayMap  = new HashMap();
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

    public static boolean isBoolean(String type){
        if(null != type && (type.equals("boolean") || type.equals("java.lang.Boolean")) ){
            return true;
        }
        return false;
    }
    public static boolean isSimilarType(String t,String t2){
        if(t.equals(t2))return true;
        if(t.equals("int") && t2.equals("java.lang.Integer")) return true;
        if(t.equals("double") && t2.equals("java.lang.Double")) return true;
        if(t.equals("long") && t2.equals("java.lang.Long")) return true;
        return false;
    }
    public static Class getPrimitiveClass(String primitiveClass){
        return primitiveMap.get(primitiveClass);
    }

    public static Class getPrimitiveArrayClass(String primitiveClass){
        return primitiveArrayMap.get(primitiveClass);
    }

    public static String firstCharUpcase(String s){
        StringBuffer sb = new StringBuffer(s);
        sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
        return sb.toString();
    }

    public static Object getDefaultValue(Object o,String propertyName){
        try{

            String[] ps = propertyName.split(".");
            if(ps.length>1){
                Object r = o;
                for(String p :ps){
                    r = getDefaultValue(r,p);
                }
                return r;
            }else{
                return o.getClass().getMethod("get"+firstCharUpcase(propertyName)).invoke(o);
            }
        }catch(Exception e){
        }
        return null;
    }

    /**
     * �ҵ�ǰ׺���ʾ����Ϊ�յ�str
     * @param o
     * @param mapping
     * @return
     */
    public static String[] isFirstNull(final Object o,Map<String,String> mapping){
        List<String> list=new ArrayList<String>();
        Iterator ite=mapping.values().iterator();
        Object a=new Object();

        try{
            while(ite.hasNext()){
                boolean falg=false;
                a=o;
                String propertyName=(String)ite.next();
                String[] ps = propertyName.split("\\.");
                for(int i=0;i<ps.length-1;i++){
                    Object r=a;
                    a=r.getClass().getMethod("get"+firstCharUpcase(ps[i])).invoke(r);
                    if(null==a){
                        if(!list.contains(ps[i])){
                            list.add(ps[i]);
                        }
                        break;
                    }
                }
            }
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
        return list.toArray(new String[]{});
    }

    public static Object[] getValue(final String key,Object o[],String propertyName,boolean isDelete){
        try{
            String[] ps = propertyName.split("\\.");
            if(ps.length>1){
                Object[] r = o;
                for(String p :ps){
                    r = getValue(key,r,p,isDelete);
                }
                return r;
            }else{
                Object o1=o[0].getClass().getMethod("get"+firstCharUpcase(propertyName)).invoke(o[0]);
                Object o2=o[0].getClass().getMethod("get"+firstCharUpcase(propertyName)).getReturnType();
                if(null != key && key.startsWith("#") && isDelete){
                    o[0].getClass().getMethod("set"+firstCharUpcase(propertyName),(Class)o2).invoke(o[0],new Object[]{null});
                }
                return new Object[]{o1,o2};
            }
        }catch(Exception e){
            return null;
        }
    }
    static String getField2PropertyName(String k){
        String[] st = k.split("_");
        StringBuffer sb = new StringBuffer();
        for(int i=0;i<st.length;i++){
            if(sb.length()!=0)
                sb.append(Character.toUpperCase(st[i].charAt(0))+st[i].toLowerCase().substring(1));
            else
                sb.append(st[i].toLowerCase());
        }
            return sb.toString();
    }
    public static void setValues(Map map ,Object obj){
        Iterator its = map.keySet().iterator();
        while(its.hasNext()){
            String k = (String)its.next();
            String kn = getField2PropertyName(k);
            try{
                ClassUtils.setFieldValue(obj,kn,map.get(k),false);
            }catch (Exception e){
                log.error("set object["+obj+"] value error",e);
            }
        }
    }
    public static void copy(Object src,Object target){
        Field[] fs = ClassUtils.getAllField(src);
        if(null != fs){
            for(Field f:fs){
                try{
                ClassUtils.setFieldValue(target,f.getName(),ClassUtils.getFieldValue(src,f.getName(),false),false);
                }catch (Exception e){
                    log.error("set value error",e);
                }
            }
        }
    }
    public static void setValue(Object obj,String path,Object[] value) throws Exception {
        if(path.startsWith("#")){
            if(obj.getClass().getMethod("set",new Class[]{String.class,Object.class})!=null && null!=value[0]){
                obj.getClass().getMethod("set", new Class[]{String.class,Object.class}).invoke(obj, StringUtils.substringAfter(path, "#"), value[0]);
            }else{
                throw new Exception("框架对象不支持动态构建属性！"+obj);
            }
            return;
        }
        String[] ps = path.split("\\.");
        Object tem = obj;
        try{
            for(int i=0;i<ps.length;i++){
                if(i==ps.length-1){
                    tem.getClass().getMethod("set"+firstCharUpcase(ps[i]),(Class)value[1]).invoke(tem,value[0]);
                }else{
                    tem = tem.getClass().getMethod("get"+firstCharUpcase(ps[i])).invoke(tem);
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }


    /**
     * ��src�е�����ֵ����mapping�е�����ӳ���ϵ,���õ�target���С�
     * @param src
     * @param mapping<String,String> pageframe�е�����,����ֵ������ Ϊ��С������
     * @param returnObj
     */
    public static void copyData(Object src,Map<String,String> mapping,Object returnObj,boolean isDelete)throws Exception{
        Iterator its = mapping.keySet().iterator();
        String key, value;
        String[] strs=isFirstNull(returnObj,mapping);
        while(its.hasNext()){
            boolean flag=false;
            key = (String)its.next();
            value = mapping.get(key);
            for(String str:strs){
                if(value.indexOf(str)>-1){
                    flag=true;break;
                }
            }
            if(flag) continue;
            Object[] obj=new Object[]{returnObj,null};
            obj = getValue(key,obj,value,isDelete);
            setValue(src,key,obj);
        }
    }

    public static boolean isExist(String clazz,String method,String paramterclazzs){
        try{
            Method m=null;
            if(null != paramterclazzs)
                m = Class.forName(clazz).getMethod(method,getClasses(paramterclazzs));
            else
                m = Class.forName(clazz).getMethod(method);
            if(null != m)
                return true;

        }catch (Exception e){
        }
        return false;
    }

    /**
     * ��ȡ��������(java.lang.String,xxxx)
     * @param pars
     * @return
     */
    public static Class[] getClasses(String pars)throws Exception{
        if(null != pars){
            String p = pars.trim();
            if(p.startsWith("(")){
                p = p.substring(1,p.length()-1);
            }
            if(!"".equals(p)){
                String[] cs=p.split(",");

                if(null != cs && !"".equals(cs)){
                    Class[] ret = new Class[cs.length];
                    for(int i=0;i<cs.length;i++){
                        if(cs[i].startsWith("[")){
                            Class c = POJOUtil.getPrimitiveArrayClass(cs[i].substring(1));
                            if(null != c){
                                ret[i]=c;
                            }else{
                                ret[i]=ret[i] = Array.newInstance(Class.forName(cs[i].substring(1)),0).getClass();
                            }
                        }else{
                            Class c = POJOUtil.getPrimitiveClass(cs[i]);
                            if(null != c){
                                ret[i]= c;
                            }else{
                                ret[i]=Class.forName(cs[i]);
                            }
                        }
                    }
                    return ret;
                }
            }
        }
        return null;

    }

    /**
     * �ж�һ�����Ƿ��Ǽ���
     * @param typeName
     * @return
     */
    public static boolean isSimpleType(String typeName)
    {
        for (String type:SimpleType)
            if(type.equals(typeName))
                return true;
        if ((typeName.startsWith("java.lang")) || (typeName.startsWith("javax."))) {
            return true;
        }
        return false;
    }

    static String isBinary(boolean isArrayType,Class type){
        if ((isArrayType) && ("byte".equals(type.getComponentType().getName()))) {
            return "base64Binary";
        }
        if (isDataHandler(type)) {
            return  "base64Binary";
        }
        return type.getName();
    }

    static IUnKnowObjectParse getUnKnowObjectParse(Map<UnKnowObject,IUnKnowObjectParse> map,String className,String propertyType,String propertyName){
        return null;
    }

    /**
     * ������
     * @param genericFieldType
     * @param propertyName
     * @param child
     */
    static void parseGeneric(Type genericFieldType,String propertyName,PropertyInfo child,Map<UnKnowObject,IUnKnowObjectParse> unknowObjectparse){
        if(null != genericFieldType){
            if ((genericFieldType instanceof ParameterizedType)) {
                child.setGeneric(true);
                ParameterizedType aType = (ParameterizedType)genericFieldType;
                Type[] fieldArgTypes = aType.getActualTypeArguments();
                try {
                    for(Type genericType:fieldArgTypes){
                        PropertyInfo subchild = child.addChild();
                        Class parameterType=null;
                        Type type1=null;
                        if ((genericType instanceof GenericArrayType)){
                            subchild.setArray(true);
                            Type t = ((GenericArrayType)genericType).getGenericComponentType();

                            if(t instanceof ParameterizedType){
                                subchild.setGeneric(true);
                                subchild.setName(propertyName);
                                parseGeneric((ParameterizedType)t,propertyName,subchild,unknowObjectparse);
                            }
                            if(t instanceof Class){
                                parameterType = (Class)t;
                            }
                        }else if(genericType instanceof ParameterizedType){
                            subchild.setName(propertyName);
                            Class c = (Class)((ParameterizedType) genericType).getRawType();
                            subchild.setType(c.getName());
                            subchild.setModifier(c.isInterface()?1:0);
                            parseGeneric((ParameterizedType)genericType,propertyName,subchild,unknowObjectparse);
                        }else{
                            parameterType = (Class)genericType;
                        }

                        parseProperties(parameterType,type1, propertyName, subchild,unknowObjectparse);

                    }
                }
                catch (Exception e)
                {

                }
            }
        }
    }

    /**
     * ��ȡһ�������������,������PropertyInfo�ṹ��
     * 1.�����Ա�����getter,setter������
     * 2.��(����extends���������
     * 3.��������пյĹ��캯��
     * @param c
     * @param propertyInfo
     * @return
     * @throws Exception
     */
    public static void parseProperties(Class c,Type generic,String name,PropertyInfo propertyInfo, Map<UnKnowObject,IUnKnowObjectParse> unknowObjectparse)throws Exception{

        //��¼��ǰ������
        propertyInfo.set(c.isArray()?c.getComponentType().getName():c.getName(),name,isBinary(c.isArray(),c).equals("base64Binary"),c.isArray(),c.isPrimitive(),c.isInterface()?1:0);
        //���ʹ���
        parseGeneric(generic,name,propertyInfo,unknowObjectparse);
        if(c.isArray()) c = c.getComponentType();
        //����Ǹ��ӵ����ͣ�����ֽ�
        if (!isSimpleType(c.getName())){
            //��ȡ��(extends�������
            BeanInfo beanInfo = Introspector.getBeanInfo(c);
            PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
            if(null != pds){
                for(PropertyDescriptor pd:pds){
                    String propertyName = pd.getName();
                    //��ȡ��getter��setter����������
                    if ( (!pd.getName().equals("class")) && (pd.getPropertyType() != null)
                            && (null != pd.getReadMethod() && null != pd.getWriteMethod())
                        //&& ( (beanExcludeInfo == null) || (!beanExcludeInfo.isExcludedProperty(propertyName))
                            ){
                        //��¼��������
                        PropertyInfo child = propertyInfo.addChild();
                        parseProperties(pd.getPropertyType(),null, propertyName, child,unknowObjectparse);
                        Type genericFieldType = pd.getReadMethod().getGenericReturnType();
                        parseGeneric(genericFieldType,propertyName,child,unknowObjectparse);
                    }
                }
            }
        }

        //���ö�������unknow���󣬲�������û�н�������ݣ���ʹ��IUnKnowObjectParse�ٴν���
        if( null == propertyInfo.getChildren() || propertyInfo.getChildren().size()==0 ){
            IUnKnowObjectParse objectParse = getUnKnowObjectParse(unknowObjectparse,c.getName(),c.getName(),name);
            if(null != objectParse){
                objectParse.parse(c,propertyInfo);
            }
        }
    }

    static boolean isInArray(Method[] array,Method key){
        for(Method a:array){
            if(a.equals(key))
                return true;
        }
        return false;
    }



    /**
     * get parameters name of method
     * @param c
     * @param m
     * @return
     * @throws Exception
     */
    public static String[] getParameterName(Class c,Method m)throws Exception{
        ClassPool pool = ClassPool.getDefault();
        pool.insertClassPath(new ClassClassPath(c));
        CtClass cc = pool.get(c.getName());
        Class[] ps = m.getParameterTypes();
        //CtMethod[] ctMethods = cc.getMethods();
        //Type[] types = m.getGenericParameterTypes();
        Class ret = m.getReturnType();

        if(null !=ps && ps.length>0){
            String[] paramNames=null;//parameters name
            CtClass ctret = null;
            if(null != ret)
                ctret = pool.get(ret.getName());
            CtClass[] ctpars = new CtClass[ps.length];
            for(int i=0;i<ps.length;i++){
                ctpars[i] = pool.get(ps[i].getName());

            }
            CtMethod ctMethod = cc.getMethod(m.getName(), Descriptor.ofMethod(ctret, ctpars));
            cc.defrost();
            CodeAttribute codeAttribute = ctMethod.getMethodInfo().getCodeAttribute();
            cc.freeze();
            if(null != codeAttribute){
                LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);
                if(null != attr){
                    paramNames = new String[ps.length];
                    int pos = Modifier.isStatic(ctMethod.getModifiers()) ? 0 : 1;
                    for (int i = 0; i < paramNames.length; i++)
                        paramNames[i] = attr.variableName(i + pos);
                }else {
                    for (int i = 0; i < paramNames.length; i++) {
                        paramNames[i] = "arg" + i;
                    }
                }
            }
            if(null == paramNames && ps.length>0){
                paramNames = new String[ps.length];
                Annotation[][] ans= m.getParameterAnnotations();
                for (int i = 0; i < paramNames.length; i++) {
                    if(null != ans && ans.length>i && null != ans[i] && ans[i].length>0 && WebParam.class.isAssignableFrom(ans[i][0].getClass())){
                        paramNames[i] = ((WebParam)ans[i][0]).name();
                    }else {
                        paramNames[i] = "arg" + i;
                    }
                }
            }
            return paramNames;
        }
        return null;
    }

    /**
     * ��ȡһ����������ز����
     * @param c
     * @param methodes
     */
    public static List<MethodParameterProperty> parseMethodParameters(Class c,Method[] methodes,Class methodParameterPropertyClass, Class propertyInfoClass,Map<UnKnowObject,IUnKnowObjectParse> unknowObjectparse) throws Exception {
        ClassPool pool = ClassPool.getDefault();
        pool.insertClassPath(new ClassClassPath(c)); //��servlet��������
        CtClass cc = pool.get(c.getName());

        List<MethodParameterProperty> list = new ArrayList<MethodParameterProperty>();
        if(null ==methodParameterPropertyClass)
            methodParameterPropertyClass =MethodParameterProperty.class;
        if(null == propertyInfoClass)
            propertyInfoClass = PropertyInfo.class;
        Method[] rms = methodes;
        if(null == rms)
            rms = c.getMethods();
        for(int j=0;j<rms.length;j++){
            Class[] ps = rms[j].getParameterTypes();

            //CtMethod[] ctMethods = cc.getMethods();
            Type[] types = rms[j].getGenericParameterTypes();
            Class ret = rms[j].getReturnType();
            if((null !=ps && ps.length>0) || null != ret){

                MethodParameterProperty mp = (MethodParameterProperty)methodParameterPropertyClass.newInstance();
                mp.setMethodName(rms[j].getName());
                mp.setClassName(c.getName());
                if(null !=ps && ps.length>0){
                    //��ȡ�������
                    String[] paramNames=null;
                    CtClass ctret = null;
                    if(null != ret)
                        ctret = pool.get(ret.getName());
                    CtClass[] ctpars = new CtClass[ps.length];
                    for(int i=0;i<ps.length;i++){
                        ctpars[i] = pool.get(ps[i].getName());

                    }
                    CtMethod ctMethod = cc.getMethod(rms[j].getName(), Descriptor.ofMethod(ctret, ctpars));
                    CodeAttribute codeAttribute = ctMethod.getMethodInfo().getCodeAttribute();
                    if(null != codeAttribute){
                        LocalVariableAttribute attr = (LocalVariableAttribute) codeAttribute.getAttribute(LocalVariableAttribute.tag);
                        if(null != attr){
                            paramNames = new String[ps.length];
                            int pos = Modifier.isStatic(ctMethod.getModifiers()) ? 0 : 1;
                            for (int i = 0; i < paramNames.length; i++)
                                paramNames[i] = attr.variableName(i + pos);
                        }
                    }
                    //��¼����
                    for(int i=0;i<ps.length;i++){

                        PropertyInfo propertyInfo = (PropertyInfo)propertyInfoClass.newInstance();
                        parseProperties(ps[i],types[i],paramNames==null?("parameter"+i):paramNames[i], propertyInfo,unknowObjectparse);
                        mp.getInputParameter().add(propertyInfo);
                    }
                }
                //��¼���ض���
                if(null != ret){
                    PropertyInfo propertyInfo = (PropertyInfo)propertyInfoClass.newInstance();
                    parseProperties(ret,rms[j].getGenericReturnType(),"returnObj", propertyInfo,unknowObjectparse);
                    mp.setReturnParameter(propertyInfo);
                }
                list.add(mp);
            }

        }
        if(list.size()>0)
            return list;
        return null;
    }

    static boolean isDataHandler(Class<?> clazz){
        return (clazz != null) && (DataHandler.class.isAssignableFrom(clazz));
    }

    public static void print(PropertyInfo info,int level){
        String space="";
        for(int i=0;i<level*2;i++)
            space+=" ";
        //System.out.println(space+info.toString());
        if(null != info.getChildren())
            for(PropertyInfo c:info.getChildren()){
                print(c,level+1);
            }
    }

    public static void print(MethodParameterProperty pro){
        if(null != pro){
            System.out.println("ClassName:"+pro.getClassName()+"#"+pro.getMethodName());
            System.out.println("--------------input parameters");
            for(Object pp:pro.getInputParameter()){
                PropertyInfo p = (PropertyInfo)pp;
                print(p,1);
            }
            System.out.println("--------------return parameters");
            print(pro.getReturnParameter(),1);
        }
    }


    public static String getUSDLTypeValue(Class c,int typevalue){
        if(typevalue==0){
            return "{@type:'"+c.getName()+"'}";
        }
        return null;
    }

    /**
     *
     * @param c
     * @param typevalue 1:defaultvalue, 0:{type:''}
     * @return
     */
    public static Map convertBeanClass2USDLMap(Class c,int typevalue){
        if(null !=c ){
            Map map = new HashMap();
            Field[] fs = ClassUtils.getAllFieldExcludeJDK(c);
            for(Field f:fs) {
                //if(f.getType().isSynthetic()) continue;
                if(Modifier.isStatic(f.getModifiers())) continue;
                if(Modifier.isFinal(f.getModifiers())) continue;

                Class fc = null;
                if (f.getGenericType() instanceof Class){
                    fc = ((Class) f.getGenericType());
                }else if(f.getGenericType() instanceof ParameterizedType){
                    fc = (Class)((ParameterizedType)f.getGenericType()).getRawType();
                }
                if(null != fc) {
                    if (fc.isArray()) {
                        List li = new LinkedList();
                        if (isSimpleType(fc.getName())) {
                            li.add(getUSDLTypeValue(fc.getComponentType(), typevalue));
                        } else {
                            li.add(convertBeanClass2USDLMap(fc.getComponentType(), typevalue));
                        }
                        map.put(f.getName(), li);
                    } else if (Collection.class.isAssignableFrom(fc)) {
                        if (((ParameterizedType) f.getGenericType()).getActualTypeArguments().length > 0) {
                            List li = new LinkedList();
                            map.put(f.getName(), li);
                            Type suf = ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0];
                            List tem = li;
                            while (suf instanceof ParameterizedType) {
                                List tl = new LinkedList();
                                tem.add(tl);
                                suf = ((ParameterizedType) suf).getActualTypeArguments()[0];
                                tem = tl;
                            }
                            if (suf instanceof Class) {
                                if (isSimpleType(((Class) suf).getName())) {
                                    tem.add(getUSDLTypeValue((Class) suf, typevalue));
                                } else {
                                    tem.add(convertBeanClass2USDLMap((Class) suf, typevalue));
                                }
                            }

                        }
                    } else if (isPrimitive(fc.getName())) {
                        map.put(f.getName(), getUSDLTypeValue(fc, typevalue));
                    } else {
                        Map m = convertBeanClass2USDLMap(fc, typevalue);
                        map.put(f.getName(), m);
                    }
                }
            }
            return map;
        }
        return null;
    }

    /**
     * generator type by class name and generic info
     * @param c
     * @return
     */
    public static Type getTypeByString(String c)throws Exception{
        if(null != c) {
            if (c.contains("<")) {
                c = c.trim();
                String sb = c.substring(c.indexOf("<")+1,c.length()-1);
                ParameterizedType type = new ParameterizedTypeImpl(new Type[]{getTypeByString(sb)},null,Class.forName(c.substring(0,c.indexOf("<"))));
                return type;
            } else {
                return Class.forName(c);
            }
        }
        return null;
    }
    /**
     * convert usdl: list,map,primitive to bean
     * @param o
     * @param type
     * @return
     */
    public static Object convertUSDL2POJO(Object o,Type type,AtomicLong dataSize)throws Exception{
        if(null != o){
            if(type instanceof Class) {
                Class ret = (Class) type;
                if (POJOUtil.isPrimitive(o.getClass().getName())) {
                    calPrimateSize(o,dataSize);
                    return o;
                } else if (o instanceof List) {

                    if (List.class.isAssignableFrom(ret)) {
                        List rt = (List) ret.newInstance();
                        for (Object oo : (List) o) {
                            rt.add(convertUSDL2POJO(oo, ret.getComponentType(),dataSize));
                        }
                        return rt;
                    } else if (ret.isArray()) {
                        int c = ((List) o).size();
                        Object[] os =  (Object[])Array.newInstance(ret.getComponentType(), c);
                        for (int i = 0; i < ((List) o).size(); i++) {
                            os[i] = convertUSDL2POJO(((List) o).get(i), ret.getComponentType(),dataSize);
                        }
                        return os;
                    }
                } else if (o instanceof Map) {
                    return convertMap2POJO((Map) o, ret,dataSize);
                }
            }else if(type instanceof ParameterizedType){
                if(o instanceof List && List.class.isAssignableFrom((Class) ((ParameterizedType) type).getRawType())){
                    List ret=null;
                    if(((Class) ((ParameterizedType) type).getRawType()).isInterface()){
                        ret = (List)o.getClass().newInstance();
                    }else{
                        ret = (List)((Class) ((ParameterizedType) type).getRawType()).newInstance();
                    }
                    for(Object oo:(List)o) {
                        Object r = convertUSDL2POJO(oo,((ParameterizedType) type).getActualTypeArguments()[0],dataSize);
                        ret.add(r);
                    }
                    return ret;
                }
            }
        }
        return null;
    }
    //convert Class with parameterType to USDL structure string ext: [[{m:{type:''},t:{type:''}}]]
    public static String getUSDLTypeString(Class rc,Type type){
        if(null != rc) {
            if (POJOUtil.isPrimitive(rc.getName())) {
                return POJOUtil.getUSDLTypeValue(rc, 0);
            } else {
                if (List.class.isAssignableFrom(rc)) {
                    StringBuffer sb = new StringBuffer();
                    sb.append("[");
                    if(type instanceof ParameterizedType){
                        Type t = ((ParameterizedType)type).getActualTypeArguments()[0];
                        if(null != t && t instanceof Class) {
                            sb.append(getUSDLTypeString((Class)t, null));
                        }else{
                            sb.append(getUSDLTypeString((Class)((ParameterizedType)((ParameterizedType)type).getActualTypeArguments()[0]).getRawType(), t));
                        }
                    }
                    sb.append("]");
                    return sb.toString();
                } else if (rc.isArray()) {
                    StringBuffer sb = new StringBuffer();
                    sb.append("[");
                    sb.append(getUSDLTypeString(rc.getComponentType(),null));
                    sb.append("]");
                    return sb.toString();
                } else {
                    Map t = POJOUtil.convertBeanClass2USDLMap(rc, 0);
                    if (null != t) {
                        return ObjectUtils.convertMap2String(t);
                    }
                }
            }
            return "";
        }
        return null;
    }
    /**
     * convert bean,List,primitive type to List,Map,primitive, there is must the same type in list or array.
     * @param ret
     * @return
     */
    public static Object convertPOJO2USDL(Object ret,AtomicLong calDataSize)throws Exception{

        if (null != ret) {
            if (!POJOUtil.isPrimitive(ret.getClass().getName())) {
                if(ret instanceof List){
                    List rt = (List)ret.getClass().newInstance();
                    for(Object o :(List)ret) {
                        rt.add(convertPOJO2USDL(o,calDataSize));
                    }
                    return rt;
                }else if(ret.getClass().isArray()){
                    List rt = new LinkedList();
                    if(null != ret){
                        if(isPrimitive(ret.getClass().getComponentType().getName())){
                           int c = Array.getLength(ret);
                           for(int i=0;i<c;i++) {
                               calPrimateSize(Array.get(ret,i),calDataSize);
                               rt.add(Array.get(ret,i));
                           }
                        }else {
                            for (Object o : (Object[]) ret) {
                                rt.add(convertPOJO2USDL(o,calDataSize));
                            }
                        }
                    }
                    return rt;
                }else {//map
                    return convertPojo2Map(ret,calDataSize);
                }
            }else{
                calPrimateSize(ret,calDataSize);
                return ret;
            }
        }
        return null;
    }
    static void calPrimateSize(Object o , AtomicLong dataSize){
        if(null != dataSize){
            if(o instanceof String) {
                dataSize.addAndGet(((String) o).length());
            }else {
                dataSize.addAndGet((o.toString().length()));
            }
        }
    }
    public static List convertPojoArray2Map(Object o,AtomicLong calDataSize) throws Exception{
        if(null!=o) {
            List li = new LinkedList();
            int n = Array.getLength(o);
            for (int i = 0; i < n; i++) {
                Object io = Array.get(o, i);
                if(null != io) {
                    if (isSimpleType(io.getClass().getName())) {
                        calPrimateSize(o, calDataSize);
                        li.add(io);
                    } else {
                        li.add(convertPojo2Map(io, calDataSize));
                    }
                }else{
                    li.add(null);
                }
            }
            return li;
        }else{
            return null;
        }
    }
    public static List convertPojoList2Map(Object o,AtomicLong calDataSize) throws Exception{
        Iterator its = ((Collection) o).iterator();
        List li = new LinkedList();
        while (its.hasNext()) {
            Object io = its.next();
            if (isSimpleType(io.getClass().getName())) {
                calPrimateSize(o,calDataSize);
                li.add(io);
            } else {
                li.add(convertPojo2Map(io,calDataSize));
            }
        }
        return li;
    }
    /**
     * 把pojo对象转换为简单map对象,List,Map,String
     * @param pojoBean
     * @return
     */
    public static Map convertPojo2Map(Object pojoBean,AtomicLong calDataSize) throws Exception {
        if(null != pojoBean){
            Map map = new HashMap();
            Field[] fs = ClassUtils.getGetFields(pojoBean);
            if(null != fs) {
                for (Field f : fs) {
                    f.setAccessible(true);
                    Object o = f.get(pojoBean);
                    if (null != o && o != pojoBean) {
                        if (o.getClass().isArray()) {
                            map.put(f.getName(), convertPojoArray2Map(o,calDataSize));
                        } else if (o instanceof Collection) {
                            map.put(f.getName(), convertPojoList2Map(o,calDataSize));
                        } else if (isPrimitive(o.getClass().getName())) {
                            if(null!=o && o instanceof Double && ((Double)o).isNaN()){
                                o="";
                            }
                            calPrimateSize(o,calDataSize);
                            map.put(f.getName(), o);
                        } else {
                            Map m = convertPojo2Map(o,calDataSize);
                            map.put(f.getName(), m);
                        }
                    }
                }
                return map;
            }else if(pojoBean instanceof Map){
                return (Map)pojoBean;
            }
        }
        return null;
    }

    /**
     *
     * @param map   usdl data
     * @param cs    parameter class
     * @param typs  parameter generics
     * @param parNames  parameter names
     * @return
     * @throws Exception
     */
    public static Object[] convertMapData2POJOs(Map map,Class[] cs,Type[] typs,String[] parNames) throws Exception {
        if(null == cs)return null;
        /*if(cs.length==1 && null != map){
            if(null != parNames && parNames.length==1 && map.containsKey(parNames[0]) && map.size()==1){
                return new Object[]{convertUSDL2POJO(map.get(parNames[0]),typs[0])};
            }else {
                Object o = convertMap2POJO(map, cs[0]);
                if (null != o) {
                    return new Object[]{o};
                }
            }
        }*/
        if(cs.length>0 && null != map){
            Object[] ret = new Object[parNames.length];
            for(int i=0;i<ret.length;i++) {
                Object o = map.get(parNames[i]);
                if(null != o) {
                    if (isPrimitive(o.getClass().getName())) {
                        ret[i] = ClassUtils.chgValue(parNames[i],(Class) typs[i], o);
                    } else if (o instanceof List) {
                        ret[i] = convertUSDL2POJO(map.get(parNames[i]), typs[i], null);
                    }
                    if (o instanceof Map) {
                        ret[i] = convertMap2POJO((Map) o, cs[i], null);
                    }
                }
            }
            return ret;
        }
        return null;
    }
    /**
     * convert map to pojo bean
     * @param map
     * @param c
     * @return
     * @throws Exception
     */
    public static Object convertDBMap2POJO(Map<String,Object> map,Class c)throws Exception {
        if(null != c){
            Object o = c.newInstance();
            Iterator<String> its = map.keySet().iterator();
            while(its.hasNext()){
                String s = its.next();
                ClassUtils.setFieldValue(o, getField2PropertyName(s),map.get(s),false);
            }
            return o;
        }
        return null;
    }

    /**
     * convert map to class
     * @param map
     * @param c
     * @return
     * @throws Exception
     */
    public static Object convertMap2POJO(Map<String,Object> map,Class c,AtomicLong calDataSize) throws Exception {
        long l = System.currentTimeMillis();
        if(null != c){
            if(isPrimitive(c.getName()) && null != map && map.size()==1){
                Map.Entry e = map.entrySet().iterator().next();
                Object r = e.getValue();
                calPrimateSize(r,calDataSize);
                return ClassUtils.chgValue((String)e.getKey(),c,r);
            }else {
                Object o = c.newInstance();
                if(Map.class.isAssignableFrom(o.getClass())){
                    ((Map)o).putAll(map);
                }else {
                    Iterator<String> its = map.keySet().iterator();
                    while (its.hasNext()) {
                        String s = its.next();
                        Object obj = map.get(s);
                        Field f = ClassUtils.getField(o, s, false);
                        if (null != obj && null != f) {
                            Class fc = f.getType();
                            if (List.class.isAssignableFrom(obj.getClass())
                                    && (
                                    ((List.class.isAssignableFrom(fc)) && f.getGenericType() instanceof ParameterizedType && null != ((ParameterizedType) f.getGenericType()).getActualTypeArguments())
                                            || (fc.isArray() && null != fc.getComponentType())
                            )) {
                                Object nc = convertUSDL2POJO(obj, f.getGenericType(), calDataSize);
                                ClassUtils.setFieldValue(o, s, nc, false);
                            } else if (isSimilarType(fc.getName(), obj.getClass().getName())) {
                                calPrimateSize(obj, calDataSize);
                                ClassUtils.setFieldValue(o, s, obj, false);
                            } else if (isPrimitive(fc.getName()) && isPrimitive(obj.getClass().getName())) {
                                calPrimateSize(obj, calDataSize);
                                ClassUtils.setFieldValue(o, s, obj, false);
                            } else if (obj instanceof Map) {
                                Object sub = convertMap2POJO((Map) obj, fc, calDataSize);
                                ClassUtils.setFieldValue(o, s, sub, false);
                            }
                        }

                    }
                }
                return o;
            }
        }
        return null;
    }

    /**
     *
     * @param o
     * @param c
     * @param genericType
     * @return
     */
    /*public static List convertList2POJO(List o,Class c, Class genericType) throws Exception {
        List ret=null;
        if(c.isInterface()) {
            ret = (List) o.getClass().newInstance();
        }else{
            ret = (List)c.newInstance();
        }
        for(Object s:o){
            if(List.class.isAssignableFrom(s.getClass()) && List.class.isAssignableFrom(genericType) ){
                throw new Exception("now not support");
            }else if(s instanceof Map){
                Object r = convertMap2POJO((Map)s,genericType);
                ret.add(r);
            }else{
                ret.add(s);
            }
        }
        if(ret.size()>0)
        return ret;
        return null;
    }*/

    /**
     * create bean class by fields and methods in jsonMap, but can not with generic
     * @param className
     * @param jsonmap
     * @return
     */
    public static Class createClassByJsonData(String className,Map jsonmap){
        LinkedHashMap<String,Map> classText = getClassMapFromJson(className,jsonmap);
        if(null != classText){
            Iterator<String> its = classText.keySet().iterator();
            Class root = null;
            while(its.hasNext()){
                String clazz = its.next();
                Map m = classText.get(clazz);
                Class c = ClassUtils.generatorClass(clazz,(Map)m.get("fields"),(Map)m.get("methods"));
                if(clazz.equals(className))
                    root = c;
            }
            return root;
        }
        return null;
    }

    /**
     * create bean class by json structure, and generator class,return root class
     * @param className
     * @param jsonmap
     * @return
     */
    public static Class createClassByJsonData(String className,Map jsonmap,String classSavePath,boolean isDate,Map annon,Map nsMapping){
        //get class name and text from jsonMap

        LinkedHashMap<String,String> classText = getClassTextFromJson(className,jsonmap,isDate,annon,nsMapping);
        if(null != classText){
            Iterator<String> its = classText.keySet().iterator();
            Class root = null;
            while(its.hasNext()){
                String clazz = its.next();
                if(log.isDebugEnabled())
                    log.debug("createClassByJsonData className :"+className+" subclass: "+clazz);
                String text = classText.get(clazz);
                Class c = ClassUtils.generatorClass(clazz,text,classSavePath);
                if(clazz.equals(className))
                    root = c;
            }
            return root;
        }
        return null;
    }

    /**
     * 根据json生产pojo bean 内容，最底层的在最前面，所以是LinkedMap
     * @param className
     * @param jsonmap
     * @return
     */
    public static LinkedHashMap<String,Map> getClassMapFromJson(String className,Map jsonmap){
        LinkedHashMap<String,Map> list = new LinkedHashMap();
        String pak = className.substring(0,className.lastIndexOf("."));
        String rootclazz = className.substring(className.lastIndexOf(".") + 1);
        loopClazz(pak,rootclazz,jsonmap,list);
        return list;
    }
    static String loopClazz(String pak,String className,Object obj,LinkedHashMap<String,Map> list){
        Map c = new HashMap();
        Map fs = new HashMap();
        Map ms = new HashMap();
        if (isPrimitive(obj.getClass().getName())) {
            return getPrimitiveObjectName(obj.getClass().getName());
        } else if (Collection.class.isAssignableFrom(obj.getClass())) {
            String sub = loopClazz(pak,"_"+className, ((Collection) obj).iterator().next(),list);
            return  "java.util.ArrayList<" + sub + ">";
        }else if(obj instanceof Map) {
            Map jsonmap = (Map)obj;
            Iterator<String> its = jsonmap.keySet().iterator();
            while (its.hasNext()) {
                String k = its.next();
                Object o = jsonmap.get(k);
                String type = "";
                if(null == o){
                    type="java.lang.String";
                }else {
                    if (isPrimitive(o.getClass().getName())) {
                        type = getPrimitiveObjectName(o.getClass().getName());

                    } else if (o.getClass().isArray()) {

                    } else if (Collection.class.isAssignableFrom(o.getClass())) {
                        String sub = loopClazz(pak, "_" + k, ((Collection) o).iterator().next(), list);
                        type = "java.util.ArrayList<" + sub + ">";
                    } else if (Map.class.isAssignableFrom(o.getClass())) {
                        loopClazz(pak, "_" + k, (Map) o, list);
                        type = pak + "." + "_" + k;
                    }
                }
                fs.put(k,type);
                StringBuffer bb = new StringBuffer();
                bb.append("public " + type + " get").append(StringUtils.getNameFilterSpace(k)).append("()").append("{return ").append(k).append(";}");
                ms.put("get"+StringUtils.getNameFilterSpace(k),bb.toString());
                bb.delete(0,bb.length());
                bb.append("public void set").append(StringUtils.getNameFilterSpace(k)).append("(" + type + " ").append(k).append(")").append("{this.").append(k).append("=").append(k).append(";}");
                ms.put("set"+StringUtils.getNameFilterSpace(k),bb.toString());
            }
            c.put("fields",fs);
            c.put("methods",ms);
            list.put(pak + "." + className, c);

        }
        return pak+"."+className;
    }
    /**
     * 根据json生产pojo bean 内容，最底层的在最前面，所以是LinkedMap
     * @param className
     * @param jsonmap
     * @return
     */
    public static LinkedHashMap<String,String> getClassTextFromJson(String className,Map jsonmap,boolean isDate,Map annon,Map nsMapping){
        LinkedHashMap<String,String> list = new LinkedHashMap();
        LinkedHashMap<String,Map> tmpObjMap = new LinkedHashMap();
        String pak = className.substring(0,className.lastIndexOf("."));
        String rootclazz = className.substring(className.lastIndexOf(".") + 1);
        loopClazzText(pak,rootclazz,jsonmap,list,isDate,annon,nsMapping,tmpObjMap);
        return list;
    }
    static String loopClazzText(String pak,String className,Object obj,LinkedHashMap<String,String> list,boolean isDate,Object annon,Map nsMapping,Map tmpObjMap){
        if (isPrimitive(obj.getClass().getName())) {
            return  getPrimitiveObjectName(obj.getClass().getName());

        } else if (Collection.class.isAssignableFrom(obj.getClass())) {
            if(((Collection) obj).size()>0) {
                String sub = loopClazzText(pak, "_" + className, ((Collection) obj).iterator().next(), list, isDate, annon, nsMapping, tmpObjMap);
                return "java.util.ArrayList<" + sub + ">";

            }
        }else if(obj instanceof Map) {
            StringBuffer sb = new StringBuffer();
            StringBuffer pb = new StringBuffer();
            StringBuffer ib = new StringBuffer();
            StringBuffer bb = new StringBuffer();
            List<String> pros = new LinkedList<String>();
            Map jsonmap = (Map)obj;
            Iterator<String> its = jsonmap.keySet().iterator();
            while (its.hasNext()) {
                String k = its.next();
                Object o = jsonmap.get(k);

                String type = "";
                if(null == o){
                    type="String";
                }else {
                    if (isDate && ObjectUtils.DEFAULT_DATE_VALUE.equals(o)){
                        type = "java.util.Date";
                    }else if (isPrimitive(o.getClass().getName())) {
                        type= getPrimitiveObjectName(o.getClass().getName());

                    } else if (o.getClass().isArray()) {

                    } else if (Collection.class.isAssignableFrom(o.getClass())) {
                        if(((Collection) o).iterator().hasNext()) {
                            String sub = loopClazzText(pak, "_" + k, ((Collection) o).iterator().next(), list, isDate, ((null != annon && annon instanceof Map) ? ((Map) annon).get(k) : null), nsMapping, tmpObjMap);
                            type = "java.util.ArrayList<" + sub + ">";

                        }
                    } else if (Map.class.isAssignableFrom(o.getClass())) {
                        loopClazzText(pak, "_" + k, (Map) o, list, isDate, ((null != annon && annon instanceof Map) ? ((Map) annon).get(k) : null), nsMapping, tmpObjMap);
                        type = pak + "." + "_" + k;
                    }
                }
                Object suban = ((null != annon && annon instanceof Map)?((Map)annon).get(k):null);
                if(null != suban && suban instanceof Map && StringUtils.isNotBlank(((Map)suban).get("tb_nsp")) ){
                    String n = (String)((Map)suban).get("tb_nsp");
                    if(null != nsMapping && nsMapping.containsKey(n)) {
                        String na="";
                        if(type.contains("<")){
                            na = type.substring(type.indexOf("<")+1,type.indexOf(">"));
                            na = na.substring(na.lastIndexOf(".") + 1);
                        }else {
                            na = type.substring(type.lastIndexOf(".") + 1);
                        }
                        pros.add(k);
                        pb.append(" @XmlElement(name = \""+StringUtils.upperCaseFirstChar(k)+"\", namespace = \""+nsMapping.get(n)+"\") ");
                    }
                }
                pb.append(type + " ").append(k).append(";");
                ib.append(type + " ").append(k).append(";");
                bb.append("public " + type + " get").append(StringUtils.getNameFilterSpace(k)).append("()").append("{return ").append(k).append(";}");
                bb.append("public void set").append(StringUtils.getNameFilterSpace(k)).append("(" + type + " ").append(k).append(")").append("{this.").append(k).append("=").append(k).append(";}");
            }
            sb.append("package ").append(pak).append(";");
            boolean isannon=false;
            if(null != annon) {
                sb.append("import javax.xml.bind.annotation.XmlAccessType;" +
                        "import javax.xml.bind.annotation.XmlAccessorType;" +
                        "import javax.xml.bind.annotation.XmlElement;" +
                        "import javax.xml.bind.annotation.XmlRootElement;" +
                        "import javax.xml.bind.annotation.XmlType;");
                if (null != annon && annon instanceof Map && StringUtils.isNotBlank(((Map) annon).get("tb_nsp"))) {
                    String n = (String) ((Map) annon).get("tb_nsp");
                    if (StringUtils.isNotBlank(n) && nsMapping.containsKey(n)) {
                        sb.append(" @XmlAccessorType(XmlAccessType.FIELD) ");
                        if(pros.size()>0){
                            sb.append(" @XmlType(name = \"\", propOrder = {");
                            for(int i=0;i<pros.size();i++){
                                if(i!=0)sb.append(",");
                                sb.append("\""+pros.get(i)+"\"");
                            }
                            sb.append("}) ");
                            isannon=true;
                        }
                        sb.append(" @XmlRootElement(name = \"" + className + "\", namespace = \"" + nsMapping.get(n) + "\") ");
                    }
                }
            }
            sb.append("public class ").append(className).append(" implements java.io.Serializable{");
            if(isannon) {
                sb.append(pb.toString());
            }else{
                sb.append(ib.toString());
            }
            sb.append(bb.toString());
            sb.append("}");
            String k = pak + "." + className;
            if(!list.containsKey(k)){
                list.put(k, sb.toString());
                tmpObjMap.put(k,(Map)obj);
            }else{
                String s = list.get(k);
                //duplicate define class and content is diffient
                //merge and move refer file to up
                //list.put(k,sb.toString());
                mergeAndUp(pak,className,isDate,annon,nsMapping,k,(Map)tmpObjMap.get(k),s,(Map)obj,sb.toString(),list);
                log.error("there is duplicate class define:"+k);
            }


        }
        return pak+"."+className;
    }

    /**
     * merge duplicate define pojo class and move up refer sub class
     * @param preObjMap
     * @param preClassBody
     * @param curObjMap
     * @param curClassBody
     * @param listClassBody
     */
    static void mergeAndUp(String pkg,String className,boolean isdata,Object annon,Map nsMapping,String classfullName,Map preObjMap,String preClassBody,Map curObjMap,String curClassBody,LinkedHashMap<String,String> listClassBody){
        //merge
        if(null != preObjMap && null != curObjMap){
            ObjectUtils.appendDeepMapNotReplaceKey(curObjMap,preObjMap);
        }
        LinkedHashMap<String,String> tlist = new LinkedHashMap<String, String>();
        LinkedHashMap<String,Map> temObjMap = new LinkedHashMap<String, Map>();
        loopClazzText(pkg,className,preObjMap,tlist,isdata,annon,nsMapping,temObjMap);
        if(tlist.size()>0) {
            printLog("build", listClassBody);
            printLog("build-----------------",null);
            printLog("rebuild", tlist);
            printLog("rebuild-----------------",null);
            Iterator its = listClassBody.keySet().iterator();
            while(its.hasNext()){
                Object t = its.next();
                if(!tlist.containsKey(t)){
                    tlist.put((String)t,listClassBody.get(t));
                }
            }
            listClassBody.clear();
            listClassBody.putAll(tlist);
            printLog("endbuild",listClassBody);
            printLog("endbuild-----------------",null);
        }
    }
    static void printLog(String title,LinkedHashMap<String,String> list){
        if(log.isErrorEnabled()) {
            if(null != title) {
                if (null == list) {
                    log.debug(title);
                }else{
                    Iterator its = list.keySet().iterator();
                    while(its.hasNext()){
                        String k = (String)its.next();
                        String v = (String)list.get(k);
                        log.debug(title+"|"+k+"|"+v);
                    }
                }
            }

        }
    }

    public static String convertUSDLParameterDesc2Class(Object out,String className,String compilepath,boolean isDate,boolean isOnlyGetClass,List<Map> classes){
        return convertUSDLParameterDesc2ClassWithAnnotation(out,className,compilepath,isDate,null,null,isOnlyGetClass,classes);
    }
    /**
     * convert usdl desc input, out to class
     * @param out
     * @param className
     * @param compilepath
     * @return  className
     */
    public static String convertUSDLParameterDesc2ClassWithAnnotation(Object out,String className,String compilepath,boolean isDate,Map annon,Map nsMapping,boolean isOnlyGetClass,List<Map> classes){
        String ret=null;
        if(null != out) {
            if (out instanceof Map) {
                if(!isOnlyGetClass||classes==null) {
                    Class pc = POJOUtil.createClassByJsonData(className, (Map) out, compilepath, isDate, annon, nsMapping);
                    if (null != pc) {
                        ret = pc.getName();
                    }
                }else{
                    HashMap map = new HashMap();
                    map.put("ClassName",className);
                    map.put("Body",out);
                    map.put("CompilePath",compilepath);
                    map.put("IsDate",isDate);
                    map.put("Annotation",annon);
                    map.put("NSMapping",nsMapping);

                    classes.add(map);
                    ret = className;
                }
            } else if (out.getClass().isArray()) {

            } else if (out instanceof List) {
                StringBuffer sb = new StringBuffer("java.util.List");
                if(((List) out).size()>0) {
                    Object it = ((List) out).get(0);
                    int i = 0;
                    while (null != it && it instanceof List) {
                        sb.append("<java.util.List");
                        it = ((List) it).get(0);
                        i++;
                    }
                    if (POJOUtil.isPrimitive(it.getClass().getName())) {
                        sb.append("<").append(it.getClass().getName()).append(">");
                    } else if (it instanceof Map) {
                        if(!isOnlyGetClass||classes==null) {
                            Class pc = POJOUtil.createClassByJsonData(className, (Map) it, compilepath, isDate, null, nsMapping);
                            if (null != pc) {
                                sb.append("<").append(pc.getName()).append(">");
                            }
                        }else{
                            HashMap map = new HashMap();
                            map.put("ClassName",className);
                            map.put("Body",it);
                            map.put("CompilePath",compilepath);
                            map.put("IsDate",isDate);
                            map.put("Annotation",null);
                            map.put("NSMapping",nsMapping);

                            classes.add(map);
                            sb.append("<").append(className).append(">");
                        }

                    }
                    for (int j = 0; j < i; j++) {
                        sb.append(">");
                    }
                }
                ret = sb.toString();
            } else {
                ret = out.getClass().getName();
            }
        }
        return ret;
    }

    /**
     * 按POJO从简单到组合的顺序，批量创建POJOclass到classLoader中
     * @param
     * @return
     */
    public static void generatorClassByDeclared(List<PojoDeclared> ls){
        if(null != ls){
            for(PojoDeclared pd :ls) {
                try{
                    GeneratorClass.generatorPOJOClass(pd.getName(),pd.getProperties());
                }catch (Exception e){
                    log.error(e);
                }
            }
        }
    }

    public class PojoDeclared{
        String name;
        Map<String,String> properties = new HashMap<String, String>();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Map<String, String> getProperties() {
            return properties;
        }

        public void setProperties(Map<String, String> properties) {
            this.properties = properties;
        }
    }

    public static void main(String[] args){
        try {
            Map map = StringUtils.convert2MapJSONObject("{wf:'ee',li:[[{m:'1',ef:'f2'}]]}");
            LinkedHashMap<String,Map> s = getClassMapFromJson("com.test.TestWF", map);
            Iterator<String> its = s.keySet().iterator();
            while(its.hasNext()){
                String k = its.next();
                System.out.println("---------------------------\n"+k+"\n"+s.get(k));
            }
        }catch (Exception e){
            e.printStackTrace();

        }
    }
}
