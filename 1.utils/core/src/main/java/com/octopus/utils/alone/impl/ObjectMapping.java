package com.octopus.utils.alone.impl;

import com.octopus.utils.cls.ClassUtils;
import net.sf.json.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.Field;
import java.util.*;

/**
 * User: Administrator
 * Date: 15-1-12
 * Time: 下午5:49
 */
public class ObjectMapping {
    static transient Log log = LogFactory.getLog(ObjectMapping.class);

    /*public static Object mapping(Object src,MappingInfo mi){
        try{
            Object target=mi.getTargetStruct().getType().newInstance();
            Iterator<String> ks = mi.getPathMapping().keySet().iterator();
            HashMap<String,Object> cache = new HashMap();
            Map tm= new HashMap();
            while(ks.hasNext()){
                String tp = ks.next();
                String sp = mi.getPathMapping().get(tp);
                Object srcObj = ObjectUtils.getValueByPath(src,sp);
                *//*Object srcObj = null;
                if(sp.contains(".")){
                    String parent = sp.substring(0, sp.lastIndexOf("."));
                    if(!cache.containsKey(parent)){
                        Object p = findObject(src, parent.split("\\."));
                        cache.put(parent,p);
                    }
                    srcObj=getDataFromObj(cache.get(parent), sp.substring(sp.lastIndexOf(".") + 1));
                }*//*
                tm.put(tp, srcObj);
            }

            if(tm.size()>0){
                setValue(target,mi.getTargetStruct().getChildren(), tm);
                return target;
            }else{
                return null;
            }
        }catch (Exception e){
            log.error(e);
        }
        return null;
    }*/
    static Object findObject(Object obj,String path){
        if(null == obj)return null;
        Object src = obj;
        Object find=null;
        if(src instanceof Map){

        }
        /*while(true){
            if(src instanceof JSONObject){
                find=((JSONObject)src).get(path);
            }else if(src instanceof Map){
                find=((Map)src).get(path);
            }else{
                try{
                    //find = ClassUtils.getFieldValue(src,path[0],false);
                }catch (Exception e){
                    log.error(e);
                }
            }
            src=find;
            //path=Arrays.copyOfRange(path, 1, path.length);
        }*/
        return find;
    }
    static Object getDataFromObj(Object src,String name){
        Object find=null;
        if(src instanceof JSONObject){
            find=((JSONObject)src).get(name);
        }else if(src instanceof Map){
            find=((Map)src).get(name);
        }else if(src.getClass().isArray()){
            List li = new LinkedList();
            for(int i=0;i<((Object[])src).length;i++){
                Object r = getDataFromObj(((Object[])src)[i],name);
                if(null != r)
                    li.add(r);
            }
            if(li.size()>0)
                find = li.toArray();
        }else if(Collection.class.isAssignableFrom(src.getClass())){
            List li = new LinkedList();
            Iterator its = ((Collection)src).iterator();
            while(its.hasNext()){
                Object r = getDataFromObj(its.next(),name);
                if(null != r)
                    li.add(r);
            }
            find=li;
        }else{
            try{
                find = ClassUtils.getFieldValue(src,name,false);
            }catch (Exception e){
                log.error(e);
            }
        }
        return find;
    }
    static void setValue(Object obj,String name,Object v){
        try{
            if(obj instanceof Map){
                ((Map)obj).put(name,v);
            }else if(obj instanceof JSONObject){
                 ((JSONObject)obj).put(name,v);
            }else{
                ClassUtils.setFieldValue(obj,name,v,false);
            }
        }catch (Exception e){
            e.printStackTrace();
            log.error(e);
        }
    }
    public Object generatorObjectByStructAndMapping(Object obj,List<StructInfo> childresstr,Map<String,Object> vs)throws Exception{
        fillData(obj,childresstr,vs);

        return obj;
    }
    public void fillData(Object o,List<StructInfo> childresstr,Map<String,Object> vs)throws Exception{
        /*while(its.hasNext()){
            String p = its.next();
            String[] ns = p.split("\\.");
            List<StructInfo> s = childresstr;
            Object tem = o;
            for(String n:ns){
                StructInfo info = getStruct(s,n);
                if(null != info && null == ObjectUtils.getValueByPath(tem,n)){
                    Object oo = info.getType().newInstance();
                    s = info.getChildren();
                    ObjectUtils.setValueByPath(tem,n,oo);
                    tem = oo;
                }
            }
        }*/

    }

    StructInfo getStruct(List<StructInfo> ls,String name){
        if(null != ls){
            for(StructInfo f:ls){
                if(f.getName().equals(name))
                    return f;
            }
        }
        return null;
    }
    //向对象中批量设置值
    public static void setValue(Object obj,List<StructInfo> childresstr,Map<String,Object> vs){
        if(null == obj || null == vs)return;
        if(obj instanceof JSONObject){
            Iterator<String> its = vs.keySet().iterator();
            while(its.hasNext()){
                String k = its.next();
                String[] ks = k.split("\\.");
                Object to = obj;
                boolean isfindParent=true;
                StringBuffer sb = new StringBuffer();
                while(ks.length>1){
                    if(sb.length()==0)
                        sb.append(ks[0]);
                    else
                        sb.append(".").append(ks[0]);
                    try{
                        /*Object f = ((JSONObject)to).get(ks[0]);
                        if(null != f){
                            to  = f;
                            ks = Arrays.copyOfRange(ks,1,ks.length);
                            isfindParent=true;
                        }else{
                            Object o = si.getNewObject(sb.toString());
                            setValue(o,ks[0],o);
                            to=o;
                        }*/
                    }catch (Exception e){
                        log.error(e);
                        break;
                    }

                }
                try{
                    if(isfindParent && to != null){
                        setValue(to,ks[ks.length-1],vs.get(k));
                    }
                }catch (Exception e){
                    log.error(e);
                }
            }
        }else if(obj instanceof Map){
            Iterator<String> its = vs.keySet().iterator();
            while(its.hasNext()){
                String k = its.next();
                String[] ks = k.split("\\.");
                Object to = obj;
                boolean isfindParent=false;
                StringBuffer sb = new StringBuffer();
                int n=0;
                while(ks.length>0){
                    if(sb.length()==0)
                        sb.append(ks[n]);
                    else
                        sb.append(".").append(ks[n]);
                    try{
                        Object f = ((Map)to).get(sb.toString());
                        if(null != f){
                            to  = f;
                            ks = Arrays.copyOfRange(ks,1,ks.length);
                            isfindParent=true;
                        }else{
                            for(int j=0;j<childresstr.size();j++) {
                                Object o = childresstr.get(j).getNewObject(sb.toString());
                                setValue(to, sb.toString(), o);
                            }

                        }
                    }catch (Exception e){
                        log.error(e);
                        break;
                    }finally {
                        n++;
                    }

                    try{
                        if(isfindParent && to != null){
                            setValue(to,ks[ks.length-1],vs.get(k));
                        }
                    }catch (Exception e){
                        log.error(e);
                    }
                }

            }
        }else{
            Iterator<String> its = vs.keySet().iterator();
            while(its.hasNext()){
                String k = its.next();
                String[] ks = k.split("\\.");
                Object to = obj;
                boolean isfindParent=true;
                while(ks.length>1){
                    try{
                        Field f = ClassUtils.getField(to, ks[0], false);
                        if(null != f){
                            if(null != f.get(to)){
                                to = f.get(to);
                            }else{
                                Object o = generateObj(f.getType());
                                setValue(to,ks[0],o);
                                to=o;
                            }
                            ks = Arrays.copyOfRange(ks,1,ks.length);
                        }else{
                            isfindParent=false;
                            break;
                        }
                    }catch (Exception e){
                        log.error(e);
                        isfindParent=false;
                        break;
                    }
                }
                try{
                    if(isfindParent && to != null){
                        setValue(to,ks[ks.length-1],vs.get(k));
                    }
                }catch (Exception e){
                    log.error(e);
                }
            }
        }
    }
    //根据类型创建默认的对象
    static Object generateObj(Class t){
        //非数组,抽象，有无参数的构造方法
        if(!((Class) t).isSynthetic() && !((Class) t).isInterface() && !((Class) t).isArray() && !((Class) t).isPrimitive()){
            try{
            return ((Class) t).newInstance();
            }catch (Exception e){
                log.error(e);
            }
        }
        return null;
    }
    static void setPath(Collection<String> c,PathTreeNode node){
        Iterator<String> cs =c.iterator();
        while(cs.hasNext()){
            String[] ns = cs.next().split("\\.");
            StringBuffer sb = new StringBuffer();
            PathTreeNode tem = node;
            for(int i=0;i<ns.length;i++){
                if(sb.length()==0)
                    sb.append(ns[i]);
                else
                    sb.append(".").append(ns[i]);
                tem.name=ns[i];
                tem.fullname=sb.toString();
                if(i<ns.length-1){
                    if(null == tem.cls) tem.cls=new ArrayList<PathTreeNode>();
                    boolean is=false;
                    for(PathTreeNode p:tem.cls){
                        if(ns[i+1].equals(p.name)){
                            tem=p;
                            is=true;
                        }
                    }
                    if(!is){
                        PathTreeNode n =new PathTreeNode();
                        tem.cls.add(n);
                        tem=n;
                    }

                }
            }
        }
    }

}
class PathTreeNode{
    String fullname;
    String name;
    List<PathTreeNode> cls;
    boolean isarray;
    Object v;

    public PathTreeNode clone(){
        PathTreeNode p = new PathTreeNode();
        p.name=name;
        if(null != cls){
            p.cls=new ArrayList<PathTreeNode>();
            for(PathTreeNode c:cls){
                p.cls.add(c.clone());
            }
        }
        return p;
    }

}
