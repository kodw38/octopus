package com.octopus.utils.alone.impl;

import com.octopus.utils.cls.POJOUtil;
import com.octopus.utils.cls.pojo.PropertyInfo;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.*;

/**
 * User: wfgao_000
 * Date: 2016/11/7
 * Time: 17:46
 */
public class MappingValue {
    /**
     *
     * @param srcschame   原对象结构描述
     * @param srcObj     原对象
     * @param mapping   映射关系
     * @param tarschame  目标对象结构
     * @param t         目标对象
     * @throws Exception
     */
    public static void generateMapping(List<PropertyInfo> srcschame,JSONArray srcObj,Map<String,String> mapping,JSONObject tarschame,JSONObject t) throws Exception {
        long l =System.currentTimeMillis();
        //原数据补偿属性type,isgeneric,isarray
        for(int i=0;i<srcObj.size();i++){
            compensatePro((JSONObject) srcObj.get(i), srcschame.get(i));
        }
        //System.out.println("1:"+(System.currentTimeMillis() - l));
        //复制tarschame结构
        compensatePro(t,tarschame);
        //System.out.println("2:"+(System.currentTimeMillis() - l));
        PathTreeNode kt = new PathTreeNode();
        setPath(mapping.keySet(),kt);
        //补充数组属性并修改fullname
        compensateArrayPath(kt, t);
        //看看是否有数组，如果有扩展tarschema为目标数组结构
        //System.out.println("3:"+(System.currentTimeMillis() - l));
        expArray(t,srcObj,mapping,kt);

        //System.out.println("4:"+(System.currentTimeMillis() - l));

    }
    private static void compensatePro(JSONObject t,JSONObject o){
        if(null !=o && null != t){
            t.put("name", o.get("name"));
            t.put("type", o.get("type"));
            t.put("isgeneric", o.get("isgeneric"));
            t.put("isarray", o.get("isarray"));
            t.put("modifier",o.get("modifier"));
            t.put("primitive",o.get("primitive"));
            JSONArray cls = (JSONArray)o.get("@content");
            if(null != cls){
                if(null ==t.get("@content")) t.put("@content",new JSONArray());
                for(int k=0;k<cls.size();k++){
                    if(cls.get(k) instanceof JSONObject){
                        JSONObject to = new JSONObject();
                        compensatePro(to,(JSONObject)cls.get(k));
                        ((JSONArray)t.get("@content")).add(to);

                    }
                }
            }
        }
    }
    //type,isgeneric,isarray
    private static  void compensatePro(JSONObject obj,PropertyInfo pro){
        if(null != obj && null != pro){
            obj.put("type",pro.getType());
            obj.put("isgeneric",pro.isGeneric());
            obj.put("isarray",pro.isArray());
            obj.put("modifier",pro.getModifier());
            obj.put("primitive",pro.isPrimitive());
            JSONArray cls = (JSONArray)obj.get("@content");
            if(null != cls){
                for(int k=0;k<cls.size();k++){
                    if(cls.get(k) instanceof JSONObject){
                        compensatePro((JSONObject)cls.get(k),getPropertyInfoByName(pro.getChildren(),((JSONObject)cls.get(k)).getString("name")));
                    }else if(cls.get(k) instanceof JSONArray){
                        for(int j=0;j<((JSONArray) cls.get(k)).size();j++){
                            compensatePro((JSONObject)((JSONArray) cls.get(k)).get(j),getPropertyInfoByName(pro.getChildren(),((JSONObject)((JSONArray) cls.get(k)).get(j)).getString("name")));
                        }
                    }
                }
            }
        }
    }
    private static PropertyInfo getPropertyInfoByName(List<PropertyInfo> list,String name){
        for(PropertyInfo p :list){
            if(p.getName().equals(name))
                return p;
        }
        return null;
    }
    private static void setPath(Collection<String> c,PathTreeNode node){
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
    static private JSONArray copyArray(JSONArray array){
        JSONArray ret = new JSONArray();
        for(int i=0;i<array.size();i++){
            JSONObject t = new JSONObject();
            compensatePro(t,(JSONObject)array.get(i));
            ret.add(t);
        }
        return ret;
    }
    static boolean isArray(JSONObject t) throws ClassNotFoundException {
        if(null != t.get("isarray") && (Boolean)t.get("isarray")){
            return true;
        }
        if(null == t.get("type"))return false;
        String className = t.getString("type");
        if(null != t.get("modifier") && t.getInt("modifier")==1){//是接口类需要寻找实现类
            className = getImplClassFromInter(className);
        }

        Class c =null;
        if(null != t.get("primitive") || null != POJOUtil.getPrimitiveClass(className) ){
            c = POJOUtil.getPrimitiveClass(className);
        }else{
            c = Class.forName(className);
        }
        if(c==null)return false;
        if(List.class.isAssignableFrom(c)){
            return true;
        }
        return false;
    }
    static void compensateArrayPath(PathTreeNode node,JSONObject obj) throws ClassNotFoundException {
        if(node.name.equals(obj.get("name"))){
            /*if(sb.length()==0)
                sb.append(node.name);
            else
                sb.append(".").append(node.name);*/
            if(isArray(obj)){
                node.isarray=true;
                if(null !=node.cls){
                    for(PathTreeNode n:node.cls){
                        n.fullname=n.fullname.substring(n.fullname.indexOf(node.fullname)+node.fullname.length()+1);
                    }
                }
            }
            if(null != node.cls){
                for(PathTreeNode n:node.cls){
                    compensateArrayPath(n,(JSONObject)getData(n.name,(JSONArray)obj.get("@content")));
                }
            }
        }
    }

    /**
     *
     * @param targetObj  目标对象
     * @param srcObj   原对象
     * @param mapping 映射关系 key为目录属性路径，value为原对象属性路径
     * @param targetPath  目标结构描述
     * @throws ClassNotFoundException
     */
    static void expArray(JSONObject targetObj,JSONArray srcObj,Map<String,String> mapping,PathTreeNode targetPath) throws ClassNotFoundException {
        if(null != targetPath && targetPath.name.equals(targetObj.get("name"))){
            if(isArray(targetObj)){
                Iterator<String> k = mapping.keySet().iterator();
                while(k.hasNext()){
                    String ks = k.next();
                    if(ks.indexOf(targetPath.fullname)==0){
                        String sp = mapping.get(ks);

                        /*if(null != OSDIConfigManager.getConfig().getDefaultValue() && null != OSDIConfigManager.getConfig().getDefaultValue().getContentName()
                                && sp.startsWith(OSDIConfigManager.getConfig().getDefaultValue().getContentName()))
                            continue;
                        */
                        StringBuffer sb = new StringBuffer();
                        JSONObject arrayObj = getArray(sp, srcObj,sb);
                        if(null != arrayObj){
                            int count = ((JSONArray)arrayObj.get("@content")).size();

                            if(null !=targetObj.get("type") && POJOUtil.isSimpleType(targetObj.getString("type"))){

                            }else{
                                //扩展数组
                                if(null == targetObj.get("size")){
                                    JSONArray a1 = (JSONArray)targetObj.get("@content");
                                    JSONArray a2 = copyArray(a1);
                                    ((JSONArray) targetObj.get("@content")).clear();
                                    for(int i=0;i<count;i++){
                                        JSONArray a3=copyArray(a2);
                                        ((JSONArray) targetObj.get("@content")).add(a3);
                                    }
                                    targetObj.accumulate("size",count);
                                }

                                HashMap map = new HashMap();
                                String nk = ks.substring(targetPath.fullname.length()+1);
                                String nv = sp.substring(sb.toString().length()+1);
                                map.put(nk,nv);
                                String nh = getHead(nk);

                                for(int i=0;i<count;i++){
                                    JSONArray array = ((JSONArray)((JSONArray)targetObj.get("@content")).get(i));
                                    for(int m=0;m< array.size();m++){
                                        String n =  ((JSONObject) array.get(m)).getString("name");
                                        JSONArray subArray=null;
                                        Object o = ((JSONArray)arrayObj.get("@content")).get(i);
                                        if(o instanceof JSONArray){
                                            subArray=(JSONArray)o;
                                        }
                                        if(nh.equals(n)){
                                            //expArray((JSONObject)array.get(m),subArray,map,getPathTreeNode( n,getSubPath(tp.cls,tp.fullname)));
                                            expArray((JSONObject)array.get(m),subArray,map,getPathTreeNode( n,targetPath.cls));
                                        }
                                    }

                                }
                            }

                        }else{
                            //枚举数组设置值
                            JSONArray datas =(JSONArray)targetObj.get("@content");
                            String h = getHead(ks.substring(targetPath.fullname.length()+1));
                            for(int m=0;m<datas.size();m++){

                                JSONObject o = (JSONObject)getData(h,(JSONArray)datas.get(m));

                                    JSONObject ov = (JSONObject)getData(getHead(sp),srcObj);
                                    if(null != ov){
                                        Object v = getValue(ov,sp);
                                        o.put("value",v);
                                    }


                            }

                        }
                    }
                }


            }else{
                //简单类型设置值
                if(null != targetObj.get("type") && POJOUtil.isSimpleType(targetObj.getString("type"))){
                    String tn = mapping.get(targetPath.fullname);
                    if(null != tn){

                            JSONObject ov = (JSONObject)getData(getHead(tn),srcObj);
                            if(null != ov){
                                Object v = getValue(ov,tn);
                                targetObj.put("value",v);
                            }

                    }
                }

                List<PathTreeNode> list = targetPath.cls;
                if(null != list){
                    JSONArray as = (JSONArray)targetObj.get("@content");
                    for(PathTreeNode p:list){
                        expArray((JSONObject)getData(p.name,as),srcObj,mapping,p);
                    }
                }
            }
        }
    }
    static String getImplClassFromInter(String name){
        return null;
    }
    static PathTreeNode getPathTreeNode(String name,List<PathTreeNode> list){
        if(null != list)
            for(PathTreeNode p:list)
                if(p.name.equals(name))
                    return p;
        return null;
    }
    static String getHead(String p){
        int n = p.indexOf(".");
        String h=p;
        if(n>0)
            h=p.substring(0,p.indexOf("."));
        return h;
    }
    static JSONObject getArray(String p,JSONArray obj,StringBuffer sb) throws ClassNotFoundException {
        if(null != obj){
            String h = getHead(p);
            if(sb.length()==0){
                sb.append(h);
            }else{
                sb.append(".").append(h);
            }
            for(int i=0;i<obj.size();i++){
                if(((JSONObject)obj.get(i)).get("name").equals(h)){
                    if(isArray((JSONObject)obj.get(i))){
                        return (JSONObject)obj.get(i);
                    }else{
                        if(null == ((JSONObject)obj.get(i)).get("type") || !POJOUtil.isSimpleType(((JSONObject) obj.get(i)).getString("type")) ){
                            return getArray(p.substring(h.length()+1),(JSONArray)((JSONObject)obj.get(i)).get("@content"),sb);
                        }
                    }
                }
            }
        }
        return null;
    }
    static Object getValue(JSONObject obj,String n){
        String[] ns = n.split("\\.");
        JSONObject o = obj;
        for(int i=0;i<ns.length;i++){
            if(null ==o)return null;
            if(o.get("name").equals(ns[i])){
                if(i==ns.length-1)
                    return o.get("value");
                JSONArray array =(JSONArray)obj.get("@content");
                o=(JSONObject)getData(ns[i+1],array);
            }
        }
        return null;
    }
    private static Object getData(String name,JSONArray array){
        for(int i=0;i<array.size();i++){
            if (array.get(i) instanceof JSONObject){
                if(((JSONObject)array.get(i)).getString("name").equals(name))
                    return array.get(i);
            }
        }
        return null;
    }

    public static void main(String[] args){
        try {
            List<PropertyInfo> ps = new ArrayList();
            JSONArray srcobj = new JSONArray();
            Map<String, String> map = new HashMap();
            map.put("mapA.list1.mapB.key","mapC.key");
            JSONObject tarps = new JSONObject();
            JSONObject mapC = new JSONObject();

            JSONObject key = new JSONObject();

            JSONObject target = new JSONObject();

            generateMapping(ps, srcobj, map, tarps, target);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
