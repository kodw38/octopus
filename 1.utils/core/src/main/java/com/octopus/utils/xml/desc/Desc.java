package com.octopus.utils.xml.desc;

import com.octopus.isp.bridge.impl.Bridge;
import com.octopus.isp.ds.Context;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.NumberUtils;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cls.ClassUtils;
import com.octopus.utils.cls.POJOUtil;
import com.octopus.utils.exception.ISPException;
import com.octopus.utils.file.FileInfo;
import com.octopus.utils.file.FileUtils;
import com.octopus.utils.net.ws.wsdl.WSDLParse;
import com.octopus.utils.rule.RuleUtil;
import com.octopus.utils.time.DateTimeUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.XMLUtil;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.util.Hash;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: wfgao_000
 * Date: 2016/9/19
 * Time: 7:41
 */
public class Desc extends XMLDoObject{
    static Map ps=null;//参数模板
    static Map ss=null;//服务模板
    static private String descFileStoreProtocol=null;
    static transient Log log = LogFactory.getLog(Desc.class);
    static List nameListByClass = new ArrayList();

    static{
        try {
            StringBuffer p = FileUtils.getFileContentStringBuffer(Desc.class.getClassLoader().getResourceAsStream("com/octopus/utils/xml/desc/parameter.tpl"));
            if (null != p) {
                ps = StringUtils.convert2MapJSONObject(p.toString());
            }
            StringBuffer s = FileUtils.getFileContentStringBuffer(Desc.class.getClassLoader().getResourceAsStream("com/octopus/utils/xml/desc/action.tpl"));
            if (null != s) {
                ss = StringUtils.convert2MapJSONObject(s.toString());
                ss.put("body","");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    XMLDoObject srvhandler;//service update handler
    static String serviceSavePath;
    public Desc(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        if(null == serviceSavePath) {
            try {
                XMLParameter data = new XMLParameter();
                setEnv(data);
                serviceSavePath = (String) XMLParameter.getExpressValueFromMap(getXML().getProperties().getProperty("savepath"), data,this);
            }catch (Exception e){

            }
        }
    }
    static String getStoreProtocol(){
        String s =  System.getProperty("xml-path");
        if(StringUtils.isBlank(s)){
            return "classpath:";
        }
        return s;
    }
    //获取文件描述文件内容，只获取name,path,package,input,output,config,original 信息，这些信息比较少，每个实例需要用到结构做参数解析转换用。详细描述报文见
    public static String getThisDescription(XMLMakeup xml) throws IOException {
        String ret=null;
        //获取文件中的path
        if(StringUtils.isBlank(ret) && StringUtils.isNotBlank(xml.getProperties().getProperty("name")) && StringUtils.isNotBlank(xml.getProperties().getProperty("path"))){
            ret = getFile(xml.getProperties().getProperty("path"),xml.getProperties().getProperty("package"),xml.getProperties().getProperty("name")+".desc");
            /*List<FileInfo> fs = FileUtils.getAllProtocolFiles(xml.getProperties().getProperty("path")+"/"+xml.getProperties().getProperty("name")+".desc", null, false);
            if(null !=fs && fs.size()>0){
                return getDescFromFileInfo(fs.get(0));
            }*/
        }
        //获取文件中package路径下
        if(StringUtils.isBlank(ret) && StringUtils.isNotBlank(xml.getProperties().getProperty("name")) && StringUtils.isNotBlank(serviceSavePath)) {
            ret= getFile("file:"+serviceSavePath,xml.getProperties().getProperty("package"),xml.getProperties().getProperty("name")+".desc");
            /*List<FileInfo> fs = FileUtils.getAllProtocolFiles("file:"+serviceSavePath + "/" + xml.getProperties().getProperty("name") + ".desc", null, false);
            if (null != fs && fs.size() > 0) {
                return getDescFromFileInfo(fs.get(0));
            }*/
        }
        //获取desc目录下
        if(StringUtils.isBlank(ret) && StringUtils.isNotBlank(xml.getProperties().getProperty("key"))) {
            ret= getFile(descFileStoreProtocol+"desc",xml.getProperties().getProperty("package"),xml.getProperties().getProperty("key")+".desc");
            /*List<FileInfo> fs = FileUtils.getAllProtocolFiles(descFileStoreProtocol + "desc/" + xml.getProperties().getProperty("key") + ".desc", null, false);
            if (null != fs && fs.size() > 0) {
                return getDescFromFileInfo(fs.get(0));
            }*/
        }
        if(StringUtils.isBlank(ret) && StringUtils.isNotBlank(xml.getProperties().getProperty("clazz"))){
            if(StringUtils.isBlank(descFileStoreProtocol)){
                descFileStoreProtocol=getStoreProtocol();
            }
            String path = xml.getProperties().getProperty("path");
            String name = xml.getId();
            //在类中获取path路径
            if(StringUtils.isNotBlank(path) && StringUtils.isNotBlank(name)){
                String p=path;
                if(p.contains(".")&& !p.contains("/")){
                    p = StringUtils.replace(p,".","/");
                    //p = path.substring(0, path.lastIndexOf("."));
                    //n = path.substring(path.lastIndexOf(".")+1);
                }
                ret = getFile(descFileStoreProtocol,p,name+".desc");
                /*try {
                    fis = FileUtils.getAllProtocolFiles(descFileStoreProtocol + path.replaceAll(".","/") + ".desc", null, false);
                }catch (Exception e){

                }*/
            }
            //获取类中的desc
            if(null == ret && StringUtils.isNotBlank(xml.getProperties().getProperty("clazz"))) {
                path = xml.getProperties().getProperty("clazz");
                String p=null,n=null;
                if(path.contains(".")){
                    p = path.substring(0,path.lastIndexOf("."));
                    n = path.substring(path.lastIndexOf(".")+1);
                }else{
                    n=path;
                }
                ret = getFile(descFileStoreProtocol,p,n+".desc");
                /*try {
                    fis = FileUtils.getAllProtocolFiles(descFileStoreProtocol + path + ".desc", null, false);
                } catch (Exception e) {
                }*/
            }
            //从jar中获取desc
            if(StringUtils.isBlank(ret)) {
                String ss = FileUtils.getProtocolFile("classpath:" + xml.getId() + ".desc");
                if (null != ss && ss.startsWith("{")) {
                    ret= ss;
                }
            }
            /*if(null != fis && fis.size()>0){
                String sb= getDescFromFileInfo(fis.get(0));
                if(null != sb) {
                    xml.getProperties().put("path",path.replace("/","."));
                }
                return sb;

            }*/

        }

        if(StringUtils.isNotBlank(ret)){
            return ret;
        }else {
            log.warn("not find desc file by:" + xml.getId());
            return null;
        }
    }

    static String getFile(String path,String pkg,String name){
        try {
        String p = path;
        if(StringUtils.isNotBlank(pkg)) {
            if(!(p.endsWith("/")||p.endsWith(":"))){
                p+="/";
            }
            p += StringUtils.replace(pkg, ".", "/");
        }
        if(!(p.endsWith("/")||p.endsWith(":"))){
            p+="/";
        }
        p+=name;
        List<FileInfo> fs = FileUtils.getAllProtocolFiles(p,null,false);
        if((null == fs||fs.size()==0) && StringUtils.isNotBlank(pkg)){
            p=path;
            if(!(p.endsWith("/")||p.endsWith(":"))){
                p+="/";
            }
            p+=name;
            fs = FileUtils.getAllProtocolFiles(p,null,false);
        }
        if (null != fs && fs.size() > 0) {
            return getDescFromFileInfo(fs.get(0));
        }
        }catch(Exception e){

        }
        if(log.isWarnEnabled()) {
            log.warn("not find desc file by:" + path + "/" + pkg + "/" + name);
        }
        return null;
    }

    static String getDescFromFileInfo(FileInfo fi)throws IOException{
        StringBuffer sb = FileUtils.getFileContentStringBuffer(fi.getInputStream());
        if (null != sb) {
            String txt = sb.toString();
            Map m = StringUtils.convert2MapJSONObject(txt);
            if(!m.containsKey("date") && null != fi.getUpdateDate()){
                txt=txt.substring(0,txt.length()-1)+",date:'"+ DateTimeUtils.date2String(fi.getUpdateDate())+"'"+"}";
            }
            return txt;
        }
        return null;
    }



    //获取继承类描述文件文本内容 只获取name,path,package,input,output,config,original 信息，这些信息比较少，每个实例需要用到结构做参数解析转换用。详细描述报文见
    public static List<String> getExtendObjsDesc(Class xmlObjectClass) {
        List<String> ls = ClassUtils.getExtendsClass(xmlObjectClass);
        if(null != ls) {
            if(StringUtils.isBlank(descFileStoreProtocol)){
                descFileStoreProtocol=getStoreProtocol();
            }
            LinkedList li = new LinkedList();
            for(int n=0;n<ls.size()-1;n++){
                String path = ls.get(n);
                String p=null,na=null;
                if(path.contains(".")){
                    p = path.substring(0,path.lastIndexOf("."));
                    na = path.substring(path.lastIndexOf(".")+1)+".desc";
                }else{
                    na=path+".desc";
                }
                String t = getFile(descFileStoreProtocol,p,na);
                if(StringUtils.isNotBlank(t)){
                    li.add(t);
                }
                /*String path = descFileStoreProtocol + ls.get(n).replaceAll("\\.", "/") + ".desc";
                try {
                    List<FileInfo> fis = FileUtils.getAllProtocolFiles(descFileStoreProtocol + path, null, false);
                    if (null != fis && fis.size() > 0) {
                        StringBuffer sb = FileUtils.getFileContentStringBuffer(fis.get(0).getInputStream());
                        if (null != sb)
                            li.add(sb.toString());
                    }
                }catch (Exception e){
                    log.error("not find description file "+path);
                }*/
            }
            if(li.size()>0)
                return li;
            return null;
        }
        return null;
    }


    /**
     * 格式化一个说明报文到一个调用报文
     *
     */
    static Map invokeDescStruMap=new ConcurrentHashMap();
    public static Map getInvokeDescStructure(Map map){
        if(null != map) {
            if(invokeDescStruMap.containsKey(map.hashCode())){
                return (Map)invokeDescStruMap.get(map.hashCode());
            }else {
                HashMap ret = new LinkedHashMap();
                removeParameterDesc(map, ret);
                invokeDescStruMap.put(map.hashCode(),ret);
                return ret;
            }
        }else{
            return null;
        }
    }
    public Map getDefaultValueDescStructure(String key)throws Exception{
        Map m = getDescStructure(key);
        return getInvokeDescStructure(m);
    }
    public static Map getParameterDesc(Map m){
        if(null != ps) {
            HashMap l = new LinkedHashMap();
            Iterator<String> ks = ps.keySet().iterator();
            while(ks.hasNext()){
                String k = ks.next();
                if(m.containsKey(k)){
                    l.put(k,m.get(k));
                }
            }
            return l;
        }
        return null;
    }
    static void removeParameterDesc(Map src,Map target){
        if(null == src) return;
        Iterator<String> p = src.keySet().iterator();
        while(p.hasNext()){
            String k = p.next();
            Object o = src.get(k);
            if(k.startsWith("@")){
                continue;
            }
            if(o ==null){
                target.put(k,o);
            }else if(o instanceof Map && ((Map)o).size()>0){
                if(isDescriptionField((Map)o) && (null == ((Map)o).get("@type") || ((Map)o).get("@type") instanceof String)) {
                    Object v = ((Map)o).get("@value");

                    String t = (String)((Map)o).get("@type");
                    if(StringUtils.isNotBlank(t)) {
                        Object m = null;
                        if (v == null && StringUtils.isNotBlank(t) && (t.equals("java.util.Date") || t.equals("java.sql.Date") || t.equals("java.sql.Timestamp"))) {
                            m = ObjectUtils.DEFAULT_DATE_VALUE;
                        } else {
                            m = ObjectUtils.getTypeDefaultValue(v, t);
                        }
                        if(null !=m &&m instanceof Map) {
                            HashMap tm = new LinkedHashMap();
                            removeParameterDesc((Map)m,tm);
                            target.put(k, tm);
                        }else  if(null != m && Collection.class.isAssignableFrom(m.getClass())){
                            List tl = new LinkedList();
                            removeParameterDesc((Collection)m,tl);
                            target.put(k,tl);
                        }else{
                            target.put(k,m);
                        }
                    }else{
                        target.put(k,v);
                    }

                }else{
                    HashMap t = new LinkedHashMap();
                    removeParameterDesc((Map) o, t);
                    target.put(k,t);
                }
            }else if(Collection.class.isAssignableFrom(o.getClass())){
                List li = new LinkedList();
                removeParameterDesc((Collection) o, li);
                target.put(k,li);
            }else{
                target.put(k,o);
            }
        }
    }

    static void removeParameterDesc(Collection src,List li){
        Iterator ks = ((Collection)src).iterator();
        while(ks.hasNext()){
            Object i = ks.next();
            if(null !=i) {
                if (i instanceof Map) {
                    HashMap t = new LinkedHashMap();
                    if (isDescriptionField((Map) i)) {
                        Object v = ((Map) i).get("@value");
                        String ty = (String) ((Map) i).get("@type");
                        Object m = ObjectUtils.getTypeDefaultValue(v, ty);
                        li.add(m);
                    } else {
                        removeParameterDesc((Map) i, t);
                        li.add(t);
                    }
                } else if (Collection.class.isAssignableFrom(i.getClass())) {
                    List t = new LinkedList();
                    removeParameterDesc((Collection) i, t);
                    li.add(t);
                } else {
                    li.add(i);
                }
            }
        }
    }
    static void removeParameterDesc(String key,Object desc,Object thisMap,Object parentObject){
        if(null == desc)return;
        if(null != thisMap && (null == desc || desc instanceof String || desc instanceof Integer || desc instanceof Double||desc instanceof Date)){
            if(thisMap instanceof Map) {
                if (((Map) thisMap).size() > 0) {
                    ((Map) thisMap).put(key, desc);
                }
            }else
                ((List)thisMap).add(desc);
        } else if(desc instanceof Map && ((Map)desc).size()>0){
            if(isDescriptionField((Map)desc) && StringUtils.isNotBlank(key)) {
                if(parentObject instanceof Map)
                    ((Map)parentObject).put(key, ((Map) desc).get("@value"));
                else
                    ((List)parentObject).add(((Map) desc).get("@value"));
            }else{
                LinkedHashMap sub = new LinkedHashMap();
                Iterator<String> p = ((Map) desc).keySet().iterator();
                while(p.hasNext()){
                    String s = p.next();
                    removeParameterDesc(s,((Map) desc).get(s),sub,thisMap);
                }
                if(null != thisMap) {
                    if (thisMap instanceof Map)
                        ((Map) thisMap).put(key, sub);
                    else
                        ((List) thisMap).add(sub);
                }else{
                    if (parentObject instanceof Map)
                        ((Map) parentObject).put(key, sub);
                    else
                        ((List) parentObject).add(sub);
                }
            }
        }else if(List.class.isAssignableFrom(desc.getClass())){

            List li = new ArrayList();
            for(Object o : (List)desc){
                removeParameterDesc(null,o,li,parentObject);
            }
            if(thisMap instanceof Map)
                ((Map)thisMap).put(key,li);
            else
                ((List)thisMap).add(li);
        }
    }

    /** 判断该节点是否是描述节点，即该节点中属性是否都是参数描述属性
     *  properties 描述报文
     */
    public static boolean isDescriptionField(Map properties){
        if(null == properties)return false;
            if(null !=ps) {
                if(ps.keySet().containsAll(properties.keySet()))
                    return true;
                /*if( (properties.size()==1
                        && (
                            (properties.containsKey("type")&&StringUtils.isNotBlank(properties.get("type")))
                            ||properties.containsKey("desc")))
                    || (properties.size()>1 && ps.keySet().containsAll(properties.keySet()) && ((properties.containsKey("type") && StringUtils.isNotBlank(properties.get("type")) ) || !properties.containsKey("type")))) {
                    return true;

                }*/
            }
        return false;
    }

    /**
     * 是否描述服务报文，区分调用服务；判断报文属性中是否都是服务描述属性
     * @param descsrv
     * @return
     */
     public static boolean isDescriptionService(Map descsrv){
        if(null == descsrv)return false;
        if(null !=ss) {
            if(descsrv.containsKey("name") && descsrv.containsKey("body") && ss.keySet().containsAll(descsrv.keySet()))
                return true;
        }
        return false;
    }

    public static boolean isDescriptionString(String desc){
        boolean isdesc=false;
        if(desc.startsWith("{") && desc.endsWith("}")){
            Map m = StringUtils.convert2MapJSONObject(desc);
            if(null != m){
                isdesc= isDescriptionService(m);
            }
        }
        if(log.isDebugEnabled()){
            log.debug("isDescriptionString \n"+desc+"\n"+isdesc);
        }
        return isdesc;
    }

    /**
     * 判断是否服务描述属性
     * @param pro
     * @return
     */
     static boolean isDescriptionServiceProperty(String pro) {
        if(null == pro)return false;
        if(null !=ss) {
            if(ss.containsKey(pro))
                return true;
        }
        return false;
    }

    /**
     * 根据描述报文转为调用报文,即获取描述报文中的body部分，转换为XMLMakeup类型
     * @param desc
     * @return
     * @throws Exception
     */
    static String[] skipPropertyInDesc=new String[]{"body","input","output"};
    public static XMLMakeup getInvokeStructure(Map desc)throws Exception{
        if(null == desc) log.error("service desc map is null");

        if(isDescriptionService(desc)){
            String b= (String)desc.get("body");
            if(StringUtils.isNotBlank(b)){

                String content = StringUtils.toXMLRetainChar(b);
                XMLMakeup x = XMLUtil.getDataFromString(content);
                if(null != x) {
                    Properties append = getProperties(desc, skipPropertyInDesc);
                    x.getProperties().putAll(append);
                    if(StringUtils.isBlank(x.getProperties().getProperty("path")) && StringUtils.isNotBlank(serviceSavePath)) {
                        x.getProperties().put("path", "file:"+serviceSavePath);
                    }
                    if(!x.getProperties().containsKey("key") && x.getProperties().containsKey("name")){
                        x.getProperties().put("key",x.getProperties().get("name"));
                    }
                    return x;
                }else{
                    log.error("service generator XMLMakeup error. the text:"+x.getId());
                }
            }else{
                log.error("not get service body from service description");
            }
        }else{
            log.error("not a service description "+StringUtils.join(desc.keySet().toArray(new String[0])));
        }
       return null;
    }

    /**
     * get String properties from service description.
     * 从服务描述报文中获取非body的字符串属性
     * @param desc
     * @param exclude
     * @return
     */
     static Properties getProperties(Map desc,String[] exclude){
        Properties ret = new Properties();
        Iterator<String> its = desc.keySet().iterator();
        while(its.hasNext()){
            String k = its.next();
            Object o = desc.get(k);
            if(!ArrayUtils.isInStringArray(exclude,k) && isDescriptionServiceProperty(k)){
                Object v = getPropertyValue(o);
                if(null != v && v instanceof String){
                    ret.setProperty(k,(String)v);
                }
            }
        }
        return ret;
    }

    /**
     * get field value from field description
     * 获取字段值
     * @param o
     * @return
     */
     static Object getPropertyValue(Object o){
        if(null != o){
           if(o instanceof Map){
               if(isDescriptionField((Map)o)){
                   return getFieldObject((Map)o);
               }else{
                   return null;
               }
           }else if(o instanceof String){
               return (String)o;
           }
        }
        return null;
    }

     static Object getFieldObject(Map o){
        if(null != o) {
            if (POJOUtil.isPrimitive((String) o.get("class"))) {
                return o.get("value");
            }
        }
        return null;
    }

    //从描述报文中获取数据字典元素,把报文字段结构拉平，提取出code，name,type，desc，enum
    public static List<Map> getElements(Map properties){
        Map input = (Map)properties.get("input");
        Map out = (Map)properties.get("output");
        return null;
    }

    public static String getGeneratorClassName(String srvname,String packg){
        if(srvname.contains(".")){
            srvname = srvname.replaceAll("\\.","_");
        }
        String name = "Proxy" + StringUtils.upperCaseFirstChar(srvname);
        if (packg.contains("."))
            packg = packg.substring(0, packg.lastIndexOf("."));
        return packg+"."+name;
    }
    /**
     * generator Service Wrap class by desc for example web service or other need Stylish shape
     * @param
     * @return
     */
    public static Class getServiceWrapClassByDesc(String xmlid,Map des,String compilepath,String staticExecute,String exeMethod,boolean isThrowException){
        //include package ,name , a method, parameters,return type
        HashMap desc= new HashMap();

        Desc.removeParameterDesc(des,desc);

        if(StringUtils.isNotBlank(desc.get("name")) && desc.get("name") instanceof String) {
            try {
                String srvname = (String) desc.get("name");
                boolean isDate=true;
                //special for umobile business
                if("queryInternalBlacklist".equals(srvname)||"ProvisioningDomainV1.0Op".equals(srvname)){
                    isDate=false;
                }
                String packg = (String) desc.get("package");
                if (StringUtils.isNotBlank(srvname) && StringUtils.isNotBlank(packg)) {
                    if(srvname.contains(".")){
                        srvname = srvname.replaceAll("\\.","_");
                    }
                    String name = "Proxy" + StringUtils.upperCaseFirstChar(srvname);

                    if (packg.contains("."))
                        packg = packg.substring(0, packg.lastIndexOf("."));
                    String method = "do" + StringUtils.upperCaseFirstChar(srvname);

                    Object ps =  desc.get("input");
                    Map pars=null;
                    if(ps instanceof Map){
                        pars = (Map)ps;
                    }else{
                        pars = StringUtils.convert2MapJSONObject((String)ps);
                    }
                    StringBuffer par = new StringBuffer();
                    String pv = "";
                    String pn = "";
                    Map nss = WSDLParse.getNameSpaces(des);
                    List<Map> clss = new LinkedList<Map>();
                    if (null != pars) {
                        //par = POJOUtil.createClassByJsonData(packg + "." + name + "_Input", pars, compilepath);
                        Iterator<String> ist = pars.keySet().iterator();
                        while (ist.hasNext()) {
                            String k = ist.next();
                            if (par.length() > 0) {
                                par.append(",");
                                pv += ",";
                                pn += ",";
                            }
                            pv += k;
                            pn += k;
                            Map nt = (Map)ObjectUtils.getValueByPath(des,"original.nspInputMapping."+k);
                            String type;
                            if(null!=nt) {
                                //type = POJOUtil.convertUSDLParameterDesc2Class(pars.get(k), packg + "." + name + "_Input_" + k, compilepath, isDate);
                                type = POJOUtil.convertUSDLParameterDesc2ClassWithAnnotation(pars.get(k), packg + "." + name + "_Input_" + k, compilepath, isDate,nt,nss,true,clss);
                            }else{
                                type = POJOUtil.convertUSDLParameterDesc2Class(pars.get(k), packg + "." + name + "_Input_" + k, compilepath, isDate,true,clss);
                            }
                            if (StringUtils.isNotBlank(type)) {
                                String wpn = WSDLParse.getAnnotationWebParam(type,k,des);
                                if(StringUtils.isNotBlank(wpn)){
                                    type=wpn +" "+ type;
                                }else {
                                    type = "@WebParam(name=\"" + k + "\") " + type;
                                }
                                par.append(type).append(" ").append(k);
                            } else {
                                log.info("getServiceWrapClassByDesc get a parameter type is null [" + name + "]:\n" + pars.get(k));
                                return null;
                            }
                        }
                    }
                    if (pv.equals("")) {
                        pv = "null";
                    }
                    if (pv.contains(",")) {
                        pv = "new Object[]{" + pv + "}";
                    }
                    Object out = desc.get("output");
                    String ret = null;
                    if (null != out) {
                        Map nt = (Map)ObjectUtils.getValueByPath(des,"original.nspOutputMapping");//tb_nsp
                        if(null!=nt) {
                            //ret = POJOUtil.convertUSDLParameterDesc2Class(out, packg + "." + name + "_Output", compilepath,isDate);
                            ret = POJOUtil.convertUSDLParameterDesc2ClassWithAnnotation(out, packg + "." + name + "_Output", compilepath, isDate,nt,nss,true,clss);
                        }else{
                            ret = POJOUtil.convertUSDLParameterDesc2Class(out, packg + "." + name + "_Output", compilepath,isDate,true,clss);
                        }

                    }
                    if(clss.size()>0) {
                        mkclass(clss);
                    }

                    StringBuffer sbi = new StringBuffer();
                    StringBuffer sb = new StringBuffer();
                    sb.append("package ").append(packg).append(";");
                    sb.append("import javax.jws.WebParam;");
                    sb.append("import javax.jws.WebService;");
                    sb.append("import javax.jws.WebMethod;");
                    sb.append("import javax.jws.WebResult;");
                    //sb.append("import javax.xml.ws.*;");
                    sb.append("import javax.jws.*;");
                    //sb.append("import javax.annotation.*;");

                    String wsp = WSDLParse.getAnnotationWebServiceParameter(des);
                    if(StringUtils.isNotBlank(wsp)){
                        sb.append(wsp);
                    }else {
                        sb.append("@WebService ");
                    }
                    sbi.append(sb.toString());
                    String iname = "I"+name;
                    sbi.append("public interface ").append(iname).append("{");
                    sb.append("public class ").append(name).append(" implements "+iname+"{");

                    //sb.append("@Resource ");
                    //sb.append("private WebServiceContext context;");
                    sbi.append(assembleInterface(method,xmlid,par.toString(),ret,isThrowException,des));
                    sb.append(assemble(method, xmlid, par.toString(), pn, pv, ret, staticExecute, exeMethod, isThrowException,des));
                    sbi.append("}");
                    sb.append("}");


                    Class ic = ClassUtils.generatorClass(packg + "." + iname, sbi.toString(), compilepath);
                    Class c = ClassUtils.generatorClass(packg + "." + name, sb.toString(), compilepath);
                    return c;
                } else {
                    log.error(desc + " do'nt have name or path property, and can'nt generator class by Desc.");
                    return null;
                }
            } catch (Exception e) {
                log.error("getServiceWrapClassByDesc error \n" + desc, e);
                return null;
            }
        }else{
            log.error("not service desc \n"+desc);
            return null;
        }
    }
    static void mkclass(List<Map> clss){

        if(clss.size()>0){
            /*LinkedHashMap tm = new LinkedHashMap();
            for(Map m:clss){
                appendClassDesc(tm,m,"Body");
                appendClassDesc(tm,m,"Annotation");
                appendClassDesc(tm,m,"NSMapping");
            }
            Iterator its = tm.keySet().iterator();
            List<Map> cl = new LinkedList();
            //sort
            while(its.hasNext()) {
                String key = (String)its.next();
                if(log.isDebugEnabled())
                    log.debug("sort class "+key);
                Map m = (Map)tm.get(key);
                int n = findClass(cl,key);
                if(n>0){
                    cl.add(n,m);
                }else{
                    cl.add(m);
                }
            }*/
            for(Map m:clss){
                if(log.isDebugEnabled())
                    log.debug("generator class:"+m.get("ClassName"));
                POJOUtil.createClassByJsonData((String)m.get("ClassName"),(Map)m.get("Body"),(String)m.get("CompilePath"),(Boolean)m.get("IsDate"),(Map)m.get("Annotation"),(Map)m.get("NSMapping"));
            }

        }
    }
    static int findClass(List<Map> lc,String cn){
        for(int i=0;i<lc.size();i++){
            Map m = lc.get(i);
            if(existKeyInDeepMap(m,cn)){
                return i;
            }
        }
        return -1;
    }
    static boolean existKeyInDeepMap(Map m,String key ){
        if(null != m && null != key) {
            Iterator its = m.keySet().iterator();
            while(its.hasNext()){
                Object k = its.next();
                if(null != k && k.equals(key)){
                    return true;
                }
                Object o = m.get(k);
                if(null != o && o instanceof Map){
                    boolean b = existKeyInDeepMap((Map)o,key);
                    if(b){
                        return b;
                    }
                }
            }
        }
        return false;
    }
    static void appendClassDesc(HashMap tm,Map append,String key){
        if(!tm.containsKey(append.get("ClassName"))){
            tm.put(append.get("ClassName"),append);
        }else{
            Map b = (Map)tm.get(append.get("ClassName"));
            if(null == b.get(key)){
                b.put(key,append.get(key));
            }
            if(null != (Map)append.get(key) && null != (Map)b.get(key))
                ObjectUtils.appendDeepMapNotReplaceKey((Map)append.get(key),(Map)b.get(key));
        }
    }

    //删除编译的文件和classLoader中的文件
    public static void deleteServiceWrapClass(String name,XMLObject o,String compilePath){
        if(null !=o){
            String pk = o.getXML().getProperties().getProperty("package");
            if(null != pk) {
                if (pk.contains("."))
                    pk = pk.substring(0, pk.lastIndexOf("."));
            }else{
                pk="";
            }
            String cn = "proxy_"+name;
            List<String> fs = FileUtils.findFiles(compilePath+pk.replaceAll("\\.","/"),cn,null,null);
            if(null != fs){
                for(String f:fs){
                    FileUtils.removeFile(f);
                }
            }
        }
    }

    static String assembleInterface(String metodName,String srvName,String pars,String returnTypeName,boolean isThrowException,Map des){
        if(null == returnTypeName)
            returnTypeName = "void";

        String mothodBody="";
        String wrn = WSDLParse.getAnnotationWebResult(metodName, des);
        if(StringUtils.isNotBlank(wrn)){
            mothodBody+=wrn+" ";
        }
        if("void".equals(returnTypeName)){
            if(isThrowException) {
                mothodBody = "public " + returnTypeName + " " + metodName
                        + "(" + pars + ")throws java.rmi.RemoteException,java.lang.Exception ;";
            }else{
                mothodBody = "public " + returnTypeName + " " + metodName + "(" + pars + ") ;";

            }
        }else{
            if(isThrowException) {
                mothodBody = "public " + returnTypeName + " " + metodName + "(" + pars + ")throws java.rmi.RemoteException,java.lang.Exception;";
            }else{
                mothodBody = "public " + returnTypeName + " " + metodName + "(" + pars + ");";

            }

        }

        return mothodBody;
    }
    static String assemble(String metodName,String srvName,String pars,String pn,String pv,String returnTypeName,String staticExecute,String exeMethod,boolean isThrowException,Map des){
        if(null == returnTypeName)
            returnTypeName = "void";
       // String p = "";
        //String pv = "null";
        /*if(null != pars){
            p = pars.getName()+" "+pars.getSimpleName();
            pv = pars.getSimpleName();
        }*/
        String mothodBody="";
        String wrn = WSDLParse.getAnnotationWebResult(metodName, des);
        if(StringUtils.isNotBlank(wrn)){
            mothodBody+=wrn+" ";
        }
        if("void".equals(returnTypeName)){
            if(isThrowException) {
                mothodBody = "public " + returnTypeName + " " + metodName
                        + "(" + pars + ")throws java.rmi.RemoteException,java.lang.Exception {  "
                        + staticExecute + "." + exeMethod + "(\"" + srvName + "\",null,\"" + pn + "\"," + pv + ");"
                        + "}";
            }else{
                mothodBody = "public " + returnTypeName + " " + metodName + "(" + pars + ") {  try{"
                        + staticExecute + "." + exeMethod + "(\"" + srvName + "\",null,\"" + pn + "\"," + pv + ");"
                        + "}catch(Exception te){te.printStackTrace();}}";

            }
        }else{
            if(isThrowException) {
                mothodBody = "public " + returnTypeName + " " + metodName + "(" + pars + ")throws java.rmi.RemoteException,java.lang.Exception { "
                        + returnTypeName + " ret = (" + returnTypeName + ")" + staticExecute + "." + exeMethod + "(\"" + srvName + "\",\"" + returnTypeName + "\",\"" + pn + "\"," + pv + ");"
                        + (returnTypeName.equals("void") ? "" : "return ret;")
                        + "}";
            }else{
                mothodBody = "public " + returnTypeName + " " + metodName + "(" + pars + "){ try{"
                        + returnTypeName + " ret = (" + returnTypeName + ")" + staticExecute + "." + exeMethod + "(\"" + srvName + "\",\"" + returnTypeName + "\",\"" + pn + "\"," + pv + ");"
                        + (returnTypeName.equals("void") ? "" : "return ret;")
                        + "}catch(Exception te){te.printStackTrace();return null;}}";

            }

        }

        return mothodBody;
    }



    public static Map getEnptyDescStructure(){
        return ss;
    }

    public static Map removeNotServiceProperty(Map desc){
        if(null!=desc) {
            Map map = new LinkedHashMap();
            Iterator its = desc.keySet().iterator();
            while(its.hasNext()) {
                String k = (String)its.next();
                if(ss.keySet().contains(k)){
                    map.put(k,desc.get(k));
                }
            }
            return map;
        }
        return null;
    }

    /**
     * convert flow (draw on page) Structure json to Desc structure json
     * @param flowStruct
     * "title":"newFlow_1",
       "nodes":{
            "1508901907834":{
            "name":"c.html",
            "left":137,
            "top":74,
            "type":"open",
            "width":104,
            "height":28,
            "alt":true
        },"lines":{
            "1508901911371":{
            "type":"sl",
            "from":"1508901907834",
            "to":"1508901908955",
            "name":"",
            "dash":false,
            "alt":true
        },
     * @return
     */
    public static Map convertFlowStructure2DescMap(Map flowStruct){
        if(null != flowStruct){
            Map desc = new HashMap();
            desc.put("name",flowStruct.get("title"));
            desc.put("package",flowStruct.get("package"));
            desc.put("opType",flowStruct.get("opType"));
            desc.put("createby","flow");
            StringBuffer body = new StringBuffer();
            String hd = "<action key=\"\" ";
            Map mm = (Map)flowStruct.get("nodes");
            LinkedHashMap tempNode = new LinkedHashMap();
            if(null!= mm){
                Iterator<String> its = mm.keySet().iterator();
                while(its.hasNext()){
                    String id = its.next();
                    Map m = (Map)mm.get(id);
                    String key = (String)m.get("name");
                    String name = (String)m.get("svname");
                    String isend = null;
                    if(null != m.get("isend")) {
                        isend = (String) m.get("isend").toString();
                    }
                    String left=null;
                    if(null != m.get("left")) {
                        left =  m.get("left").toString();
                    }
                    String top = null;
                    if(null != m.get("top")) {
                        top =  m.get("top").toString();
                    }
                    String type = (String)m.get("type");

                    String width = null;
                    if(null != m.get("width")) {
                        width = m.get("width").toString();
                    }

                    String height = null;
                    if(null != m.get("height")) {
                        height= m.get("height").toString();
                    }
                    String alt = null;
                    if(null != m.get("alt")) {
                        alt = m.get("alt").toString();
                    }
                    Map input = null;
                    if(StringUtils.isTrue(isend)){
                        hd+=" result=\"${"+key+"}\"";
                    }
                    Object o = m.get("input");
                    if(null != o) {
                        if (o instanceof Map) {
                            input = (Map) o;
                        } else if(o instanceof String){
                            if(StringUtils.isNotBlank(o)){
                                input = StringUtils.convert2MapJSONObject((String)o);
                            }
                        }
                    }
                    StringBuffer a = new StringBuffer("<do ");
                    if("task".equals(type)){
                        a.append("key=\"").append(key).append("\"").append(" action=\"suspendTheRequest\"");
                    }else if("start round".equals(type)){

                    }else if("end".equals(type)){

                    }else {
                        a.append("key=\"").append(key).append("\"").append(" action=\"").append(name).append("\"");
                    }
                    if(null != input && input.size()>0){
                        String in = ObjectUtils.convertMap2String(input);
                        in = in.replaceAll("\\\"","\\\\\"");
                        log.debug("flow input:"+in);
                        a.append(" input=\"").append(in).append("\"");
                    }
                    a.append(" nodeid=\"").append(id).append("\"");
                    a.append(" isend=\"").append(isend).append("\"");
                    a.append(" left=\"").append(left).append("\"");
                    a.append(" top=\"").append(top).append("\"");
                    a.append(" type=\"").append(type).append("\"");
                    a.append(" width=\"").append(width).append("\"");
                    a.append(" height=\"").append(height).append("\"");
                    a.append(" alt=\"").append(alt).append("\"");
                    a.append("/>");
                    tempNode.put(id,a);
                }
            }
            hd+=" xmlid=\"Logic\">";
            Map ms= (Map)flowStruct.get("lines");
            if(null!= ms){
                Iterator<String> ks = ms.keySet().iterator();
                LinkedList ls = new LinkedList();
                while(ks.hasNext()){
                    Map m = (Map)ms.get(ks.next());
                    String type = (String)m.get("type");
                    String from = (String)m.get("from");
                    String to = (String)m.get("to");
                    String name = (String)m.get("name");
                    String dash = null;
                    if(null != m.get("dash")) {
                        dash =  m.get("dash").toString();
                    }
                    String alt = null;
                    if(null != m.get("alt")) {
                        alt = m.get("alt").toString();
                    }
                    if(!ls.contains(from) && !ls.contains(to)){
                        ls.add(from);
                        ls.add(to);
                    }else if(ls.contains(from) && !ls.contains(to)){
                        ls.add(ls.indexOf(from)+1,to);
                    }else if(!ls.contains(from) && ls.contains(to)){
                        ls.add(ls.indexOf(to),from);
                    }

                }
                tempNode = ObjectUtils.setMapKeySortByKeySet(tempNode,ls);
            }
            Iterator<String> its = tempNode.keySet().iterator();
            while(its.hasNext()){
                String nid = (String)its.next();
                StringBuffer sb = (StringBuffer)tempNode.get(nid);
                body.append(sb.toString());
            }
            body.append("</action>");
            desc.put("input",getCollectionInput(tempNode));
            desc.put("output",getCollectionOutput(tempNode));
            desc.put("error",getCollectionError(tempNode));
            body.insert(0,hd);
            desc.put("body",body.toString());
            return desc;
        }
        return null;
    }
    // get collection all refer service input parameters exclude parameters used in inner.
    static Map getCollectionInput(LinkedHashMap tempNode){
        return null;
    }
    // get collection all refer service output parameters
    static Object getCollectionOutput(LinkedHashMap tempNode){
        return null;
    }
    //get collection all refer service error defined.
    static Object getCollectionError(LinkedHashMap tempNode){
        return null;
    }

    /**
     * if the desc is create by Flow, can back from Desc Map to Flow Structure used to show in page
     * @param desc
     * @return
     */
    public static Map convertDescMap2FlowStructure(Map desc)throws Exception{
        HashMap map = new HashMap();
        String body = (String)desc.get("body");
        XMLMakeup x = XMLUtil.getDataFromString(body);
        map.put("title",x.getId());
        map.put("package",x.getProperties().getProperty("package"));
        map.put("opType", x.getProperties().getProperty("opType"));
        if("flow".equals(desc.get("createby"))) {
            Map nodes = new HashMap();
            Map lines = new HashMap();
            String from = null;
            int n = 0;
            for (XMLMakeup c : x.getChildren()) {
                if (StringUtils.isNotBlank(c.getProperties().getProperty("nodeid"))) {
                    Map m = new HashMap();
                    n++;
                    m.put("name", c.getId());
                    m.put("svname", c.getProperties().getProperty("action"));
                    if(StringUtils.isNotBlank(c.getProperties().getProperty("isend"))) {
                        m.put("isend", c.getProperties().getProperty("isend"));
                    }
                    int left=0;
                    if(StringUtils.isNotBlank(c.getProperties().getProperty("left"))){
                        left = Integer.parseInt(c.getProperties().getProperty("left"));
                    }
                    m.put("left", left);
                    int top=0;
                    if(StringUtils.isNotBlank(c.getProperties().getProperty("top"))){
                        top = Integer.parseInt(c.getProperties().getProperty("top"));
                    }
                    m.put("top", top);
                    m.put("type", c.getProperties().getProperty("type"));
                    int width=0;
                    if(StringUtils.isNotBlank(c.getProperties().getProperty("width"))){
                        width=Integer.parseInt(c.getProperties().getProperty("width"));
                    }
                    m.put("width", width);
                    int height=0;
                    if(StringUtils.isNotBlank(c.getProperties().getProperty("height"))){
                        height=Integer.parseInt(c.getProperties().getProperty("height"));
                    }
                    m.put("height", height);
                    String alt = c.getProperties().getProperty("alt");
                    boolean b=false;
                    if(StringUtils.isNotBlank(alt)){
                        b =Boolean.valueOf(alt);
                    }
                    m.put("alt", b);

                    m.put("input", c.getProperties().getProperty("input"));

                    nodes.put(c.getProperties().getProperty("nodeid"), m);
                    if (StringUtils.isNotBlank(from)) {
                        HashMap t = new HashMap();
                        t.put("from", from);
                        t.put("to", c.getProperties().getProperty("nodeid"));
                        t.put("dash", false);
                        t.put("alt", true);
                        t.put("type", "sl");
                        lines.put("" + n, t);
                    }
                    from = c.getProperties().getProperty("nodeid");
                }
            }
            map.put("nodes", nodes);
            map.put("lines", lines);
            return map;
        }else{
            return null;
        }
    }
    /**
     * 存储描述文件
     * @param desc
     */
    public static void saveDesc(Map desc){
        if(null != serviceSavePath) {
            try {
                Map map = removeNotServiceProperty(desc);
                StringBuffer sb = new StringBuffer(ObjectUtils.convertMap2String(map));
                save(serviceSavePath,(String)desc.get("package"),desc.get("name")+".desc",sb);
                log.info("service ["+desc.get("name")+"] save successful");
            }catch (Exception e){
                log.info("service ["+desc.get("name")+"] save fault",e);
            }
        }
    }
    static String save(String path,String pkg,String name,StringBuffer text) throws IOException {
        String p = path;
        if(StringUtils.isNotBlank(pkg) && !"null".equalsIgnoreCase(pkg)){
            if(pkg.contains(".")){
                p+="/"+StringUtils.replace(pkg,".","/");
            }else {
                p += "/" + pkg;
            }
        }
        //remove exist desc file
        removeDesc(pkg,name.substring(0,name.lastIndexOf(".")));
        FileUtils.makeDirectoryPath(p);
        FileUtils.saveFile(text, p+"/"+name, true, false);
        return p+"/"+name;
    }
    public static void removeDesc(String pkg,String id){
        String f=serviceSavePath;
        if(StringUtils.isNotBlank(pkg)){
            f +="/"+StringUtils.replace(pkg,".","/");
        }
        f+="/"+id+".desc";
        boolean b = FileUtils.removeFile(f);
        if(!b) {
            FileUtils.removeFile(serviceSavePath + "/" + id + ".desc");
        }

    }

    /**
     * create desc save to savePath by class
     * @param c
     * @param savePath
     * @param body           desc中描述body
     * @throws Exception
     */
    public static void generatorDescByClass(Class c,String savePath,String body,String invoker)throws Exception{
        Method[] ms = ClassUtils.getThisPublicMethods(c);
        if(null != ms && ms.length>0) {
            for (Method m : ms) {
                generatorDescByClassMethod(c, m, savePath, body, invoker);
            }
        }
    }

    /**
     * get method name from a class
     * @param c
     * @return
     * @throws Exception
     */
    public static List<String> getXmlServiceNameByClass(Class c)throws Exception{
        List li = new ArrayList();
        Method[] ms = ClassUtils.getThisPublicMethods(c);
        if(null != ms && ms.length>0) {
            for (Method m : ms) {
                if (!li.contains(m.getName())) {
                    li.add(m.getName());
                }
            }
            if (li.size() > 0)
                return li;
        }
        return null;
    }
    /**
     * generator desc from normal method
     * @param c normal function class
     * @param m
     * @param savepath
     * @throws Exception
     */
    public static void generatorDescByClassMethod(Class c,Method m,String savepath,String body,String invoker)throws Exception{

        Class[] cs = m.getParameterTypes();
        Map p=new LinkedHashMap();
        String singleList=null;
        String[] pn = POJOUtil.getParameterName(c, m);
        if(null != cs) {
            if(cs.length==1){
                if(POJOUtil.isPrimitive(cs[0].getName())){
                    p.put(pn[0],POJOUtil.getUSDLTypeValue(cs[0],0));
                }else if(List.class.isAssignableFrom(cs[0])){
                    p.put(pn[0], POJOUtil.getUSDLTypeString(cs[0], m.getGenericParameterTypes()[0]));
                }else {
                    //p = POJOUtil.convertBeanClass2USDLMap(cs[0], 0);
                    p.put(pn[0],POJOUtil.convertBeanClass2USDLMap(cs[0], 0));
                }
            }else if(cs.length>1){
                for (int i=0;i<cs.length;i++) {
                    if(POJOUtil.isPrimitive(cs[i].getName())){
                        p.put(pn[i],POJOUtil.getUSDLTypeValue(cs[i],0));
                    }else if(List.class.isAssignableFrom(cs[0])) {
                        p.put(pn[i],POJOUtil.getUSDLTypeString(cs[i], m.getGenericParameterTypes()[i]));
                    } else {
                        Map u = POJOUtil.convertBeanClass2USDLMap(cs[i],0);
                        p.put(pn[i], u);
                    }
                }
            }
        }
        String pars="";
        if(null != p) {
            pars=ObjectUtils.convertMap2String(p);
        }
        Class rc = m.getReturnType();
        String rb="";
        if(null != rc && !"void".equals(rc.getName())){
            rb = POJOUtil.getUSDLTypeString(rc,m.getGenericReturnType());
        }
        String pa = saveDescFile("ByClass",savepath,m.getDeclaringClass().getName(),m.getName(),body,invoker,pars,rb,"",pn,"");
        if(null != nameListByClass){
            nameListByClass.add(m.getName());
        }
        log.info("generator desc :"+pa);
    }
    static String saveDescFile(String type,String savepath,String pack,String name,String body,String invoker,String pars,String ret,String address,String[] parNames,String src)throws Exception{
        HashMap olddesc = new HashMap();
        appExistDescInfo(savepath,pack,name+".desc",olddesc);
        if(log.isDebugEnabled()) {
            log.debug("old desc:" + olddesc);
        }
        StringBuffer sb = new StringBuffer("{");
        sb.append("name:'"+name+"'");
        if(StringUtils.isNotBlank(type)){
            sb.append(",opType:'"+type+"'");
        }
        sb.append(",package:'"+pack+"'");
        sb.append(",path:'file:"+savepath+"'");
        if(null != pars) {
            sb.append(",input:" + getAppendString(pars,olddesc.get("input")));
        }
        if(StringUtils.isNotBlank(body)){
            sb.append(",body:\""+"<action key=\\\""+name+"\\\" input=\\\"{outsvid:'"+name+"'}\\\" result=\\\"${end}\\\" xmlid=\\\"Logic\\\">"+body+"</action>"+"\"");
        }
        if(null != ret){
            sb.append(",output:" + getAppendString(ret,olddesc.get("output")));
        }
        sb.append(",original:{");
        boolean isin=false;
        if(StringUtils.isNotBlank(invoker)){
            if(isin){
                sb.append(",");
            }else {
                isin=true;
            }
            sb.append("invoker:\"" + invoker + "\"");
        }
        if(StringUtils.isNotBlank(address)){
            if(isin){
                sb.append(",");
            }else {
                isin=true;
            }
            sb.append("address:\"" + address + "\"");
        }
        if(null != parNames && parNames.length>0){
            if(isin){
                sb.append(",");
            }else {
                isin=true;
            }
            sb.append(" parNames:["+ArrayUtils.toJoinString(parNames)+"]");
        }
        if(null != src){
            if(isin){
                sb.append(",");
            }else {
                isin=true;
            }
            sb.append(" src:\""+src+"\"");
        }
        sb.append("}");

        if(olddesc.size()>0){
            Iterator its = olddesc.keySet().iterator();
            while(its.hasNext()){
                Object k = its.next();
                if("input".equals(k) || "output".equals(k)) continue;
                Object v = olddesc.get(k);
                sb.append(","+k.toString()+":"+convertDescValueString(v));
            }
        }

        save(savepath,pack,name+".desc",sb);
        //FileUtils.makeDirectoryPath(savepath);
        //FileUtils.saveFile(sb, savepath + "/" + name + ".desc", true, false);
        return savepath +"/"+pack+ "/" + name + ".desc";
    }

    static String getAppendString(String s,Object m){
        if(null != m && m instanceof Map){
            Map n = StringUtils.convert2MapJSONObject(s);
            if(null != n){
                appendDescProperties(n,(Map)m);
                return ObjectUtils.convertMap2String(n);
            }
        }
        return s;
    }

    public static void appendDescProperties(Map n,Map o){
        if(null != n && null != o){
            Iterator its = n.keySet().iterator();
            while(its.hasNext()){
                Object k = its.next();
                Object v = n.get(k);
                Object ov = o.get(k);
                if(null != k && k.toString().startsWith("@")){
                    if( null != ov && ov instanceof Map && isDescriptionField((Map)ov)){
                        if(v==null)
                        n.put(k,ov);
                        else if(v instanceof Map){
                            ObjectUtils.appendDeepMapNotReplaceKey((Map)ov,(Map)v);
                        }
                    }
                }else if( ( null == v || (v instanceof String && StringUtils.isBlank((String)v)) ||(v instanceof Map && ((Map)v).size()==0) )
                        && null != ov && ov instanceof Map && isDescriptionField((Map)ov)){
                    n.put(k, ov);
                }else if ( (null == v || null!=v && (v instanceof List && ((List)v).size()==0))
                        && null != ov && ov instanceof List && null != ((List) ov).get(0) && ((List) ov).get(0) instanceof Map && isDescriptionField((Map)((List) ov).get(0)) ){
                    n.put(k,ov);
                }else if(null != v && v instanceof Map && null != ov && ov instanceof Map){
                    appendDescProperties((Map)v,(Map)ov);
                }
            }
            if(null != o && isDescriptionField(o)){
                Iterator it = o.keySet().iterator();
                while(it.hasNext()){
                    Object k = it.next();
                    if(!n.containsKey(k)){
                        n.put(k,o.get(k));
                    }
                }
            }
        }
    }

    static String convertDescValueString(Object o){
        if(null == o) return "";
        if(o instanceof String) return "\""+((String) o).replaceAll("\"","\\\"")+"\"";
        if(o instanceof Map){
            return ObjectUtils.convertMap2String((Map)o);
        }
        if(o instanceof List){
            return ObjectUtils.convertList2String((List)o);
        }
        return "";
    }

    //if the desc exist , need to move desc,error,depend,scene,example,busiArchitecture,techArchitecture properties to new desc
    static void appExistDescInfo(String savepath,String pack,String name,Map newdesc){
        if(null != newdesc) {
            String p = "";
            if (null != savepath) {
                p = savepath;
            }
            if (StringUtils.isNotBlank(pack)) {
                if(pack.contains(".")) {
                    p += "/" + pack.replaceAll("\\.", "/");
                }else{
                    p+="/"+pack;
                }
            }
            p += "/" + name;
            File f = new File(p);
            if (f.exists()) {
                try {
                    String x = FileUtils.getFileContentByFile(f);
                    if (StringUtils.isNotBlank(x)) {
                        Map m = StringUtils.convert2MapJSONObject(x);
                        if (null != m) {
                            if (null != m.get("desc")) {
                                newdesc.put("desc",m.get("desc"));
                            }
                            if (null != m.get("error")) {
                                newdesc.put("error",m.get("error"));
                            }
                            if (null != m.get("depend")) {
                                newdesc.put("depend",m.get("depend"));
                            }
                            if (null != m.get("scene")) {
                                newdesc.put("scene",m.get("scene"));
                            }
                            if (null != m.get("example")) {
                                newdesc.put("example",m.get("example"));
                            }
                            if (null != m.get("busiArchitecture")) {
                                newdesc.put("busiArchitecture",m.get("busiArchitecture"));
                            }
                            if (null != m.get("techArchitecture")) {
                                newdesc.put("techArchitecture",m.get("techArchitecture"));
                            }
                            if (null != m.get("input")) {
                                newdesc.put("input",m.get("input"));
                            }
                            if (null != m.get("output")) {
                                newdesc.put("output",m.get("output"));
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("appExistDescInfo", e);
                }
            }
        }
    }

    /**
     * generator usdl desc file by wsdl file.
     * if muti method in wsdl , and generator muti desc files.
     * @param wsdlFileName wsdl file path
     * @param savepath  save desc file path
     * @param body   business logic
     * @param invoker invoker
     */
    public static List<String> generatorDescByWSDLFile(String wsdlFileName,String savepath,String body,String invoker)throws Exception{
        String src="";
        List<String> ret = new ArrayList();
        if(wsdlFileName.indexOf("\\")>0){
            wsdlFileName = wsdlFileName.replaceAll("\\\\","/");
        }
        if(wsdlFileName.indexOf(":")>0){
            src = wsdlFileName.substring(wsdlFileName.lastIndexOf(":")+1);
        }
        if(wsdlFileName.indexOf("/")>0) {
            src = wsdlFileName.substring(wsdlFileName.lastIndexOf("/") + 1);
        }

        Map<String,Map> usdl = WSDLParse.parseWSDL(wsdlFileName);
        if(null != usdl) {
            Iterator its = usdl.keySet().iterator();
            while (its.hasNext()) {
                String opname = (String) its.next();
                Map desc = usdl.get(opname);
                ((Map) desc.get("original")).put("src", src);
                ((Map) desc.get("original")).put("invoker", invoker);
                String savepth = savepath + "/" + opname + ".desc";
                desc.put("path", "file:" + savepath);

                if (StringUtils.isNotBlank(body)) {
                    desc.put("body", "<action key=\"" + opname + "\" result=\"${end}\" xmlid=\"Logic\">" + body + "</action>");
                }
                //if desc exist , append some exist properties to new desc
                appExistDescInfo(savepath,(String) desc.get("package"),opname + ".desc",desc);
                StringBuffer sb = new StringBuffer(ObjectUtils.convertMap2String(desc));
                String p = save(savepath, (String) desc.get("package"), opname + ".desc", sb);
                //FileUtils.makeDirectoryPath(savepath);
                //FileUtils.saveFile(sb, savepth, true, false);
                ret.add(p);
                //String pf = saveDescFile(savepath, path, opname, body, invoker, p, r, address, parNames, src);

            }
        }

        if(ret.size()>0){
            return ret;
        }else{
            return null;
        }
    }

    public static void main(String[] args){
        try {
            //generatorDescByClass(TestSV.class,"C://log/sv",null,"");
            //generatorDescByWSDLFile("file:c:/log/test.xml","C:\\log\\web","body","invokersss");

        }catch (Exception e){
            e.printStackTrace();
        }
    }


    //根据调用报文，返回实际参数对象
    public static Object getBeanInputParameters(Map env,Map invokeDesc){
        return null;
    }

    //根据结构描述文档，获取参数类型
    public static Class[] getBeanInputParametersType(Map desc){
       return null;
    }

    //根据返回结构描述，转换结果再返回
    public static Object convertBeanReturn(Map desc,Object ret){
        return null;
    }
    //把调用报文转换为Bean
    public static Class convert2Bean(String path,String name,Map descInput){
       return null;
    }

    Map trans(XMLParameter pars,Map m)throws ISPException{
        if(null == m)return null;
        Map ret = new HashMap();
        ObjectUtils.appendDeepMapNotReplaceKey(m,ret);
        if(ret.containsKey("original")){
            Object o = ret.get("original");
            if(null != o && o instanceof Map){
                if(log.isDebugEnabled()) {
                    log.debug("original 1:" + o + "    " + pars.getParameter("${input_data}"));
                }
                Map r = pars.getMapValueFromParameter((Map)o,this);
                if(log.isDebugEnabled()) {
                    log.debug("original 2:" + r);
                }
                if(null != r){
                    ret.put("original",r);
                }
            }
        }
        return ret;
    }
    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(null == serviceSavePath) {
            serviceSavePath = (String) XMLParameter.getExpressValueFromMap(getXML().getProperties().getProperty("savepath"), env,this);
        }
        if(null != input){
            if("getInvokeStructure".equalsIgnoreCase((String)input.get("op"))){
                String txt = (String)input.get("txt");
                if(StringUtils.isNotBlank(txt)){
                    return getInvokeStructure(StringUtils.convert2MapJSONObject(txt));
                }
            }else if("getDesc".equals(input.get("op"))){
                String[] os = (String[])input.get("${targetNames}");
                if(null == os){
                    os = (String[])env.getParameter("${targetNames}");
                }
                if(null != os && os.length>0) {
                    Map rtn= getDescStructure(os[0]);
                    return trans(env,rtn);
                }
            }else if("getDefaultValueDescStructure".equals(input.get("op"))){
                String name = (String)input.get("name");
                if(StringUtils.isNotBlank(name)) {
                    Map rtn = getDefaultValueDescStructure(name);
                    return trans(env,rtn);
                }else{
                    String[] os = (String[]) input.get("${targetNames}");
                    if (null == os) {
                        os = (String[]) env.getParameter("${targetNames}");
                    }
                    if (null != os && os.length > 0) {
                        Map rtn= getDefaultValueDescStructure(os[0]);
                        //log.error("desc orginal:\n"+rtn);
                        Map ret =  trans(env,rtn);
                        //log.error("desc trans:\n"+ret);
                        return ret;

                    }
                }

            }else if("generatorByClass".equals(input.get("op"))){
                nameListByClass.clear();
                Object srs = input.get("classpath");
                String[] li=null;
                if(srs instanceof List){
                    li = (String[])((List)srs).toArray(new String[0]);
                }else if(srs.getClass().isArray()){
                    li = (String[])srs;
                }
                String path = (String)input.get("savepath");
                if(null != li && StringUtils.isNotBlank(path)) {
                    for(String s:li) {
                        try {
                            if(s.contains("#")){
                                String[] cs = s.split("\\#");
                                if(cs.length==2 && StringUtils.isNotBlank(cs[1])) {
                                    String[] ms = cs[1].split(";");
                                    if(null != ms){
                                        try {
                                            Class c = Class.forName(cs[0]);
                                            for (String m : ms) {
                                                if (StringUtils.isNotBlank(m)) {
                                                    Method mc= ClassUtils.getMethodByName(c,m);
                                                    if(null!= mc) {
                                                        generatorDescByClassMethod(c, mc, path, (String) input.get("body"), (String) input.get("invoker"));
                                                    }
                                                }
                                            }

                                        }catch (Exception e){
                                            log.error(e);
                                        }
                                    }
                                }
                            }else {
                                try {
                                    generatorDescByClass(Class.forName(s), path, (String) input.get("body"), (String) input.get("invoker"));
                                }catch(Exception e){
                                    log.error("not load class:"+s,e);
                                }
                            }
                        }catch (Exception e){
                            log.error("generator desc error:"+s,e);
                        }
                    }
                }
            }else if("generatorByWSDL".equals(input.get("op"))){
                String file = (String)input.get("file");
                String savepath = (String)input.get("savepath");
                if(StringUtils.isNotBlank(file) && StringUtils.isNotBlank(savepath)) {
                    try {
                        return generatorDescByWSDLFile(file, savepath,(String)input.get("body"),(String)input.get("invoker"));
                    }catch (Exception e){
                        log.error("generator desc error:"+file,e);
                    }

                }
            }else if("getXMLServicesName".equals(input.get("op"))){
                Object srs = input.get("classes");
                String[] li=null;
                if(srs instanceof List){
                    li = (String[])((List)srs).toArray(new String[0]);
                }else{
                    li = (String[])srs;
                }
                if(null != li) {
                    List ret = new ArrayList();
                    for(String s:li) {
                        List t = getXmlServiceNameByClass(Class.forName(s));
                        if(null != t){
                            ret.addAll(t);
                        }
                    }
                    return ret;
                }
            }else if("getDesc".equals(input.get("op"))){
                String txt = (String)input.get("txt");
                if(StringUtils.isNotBlank(txt)){
                    Map d = StringUtils.convert2MapJSONObject(txt);
                    return d;
                }
            }else  if("convertJson2InvokeXMLString".equals(input.get("op"))){
                Object o = input.get("data");
                Map info = (Map)input.get("srvinfo");
                return WSDLParse.convertMap2WSDLString((Map)o,info);
            }else if("addFirstSV".equals(input.get("op"))){
                String firstsvname = (String)input.get("name");
                if(StringUtils.isNotBlank(firstsvname)){
                    nameListByClass.add(firstsvname);
                }
            }
        }
        return null;
    }

    public static boolean checkItemByDesc(String name,XMLParameter env,XMLObject obj,Object o,Map desc)throws ISPException{
        if(null != desc){
            if(null != desc.get("@type") && null != o && !"".equals(o)){
                String type = (String)desc.get("@type");
                if(POJOUtil.isNumberClass(type) && !NumberUtils.isNumber(o.toString())){
                    throw new ISPException("ISP02001","Invalid data [(data)] type in [(classType)] parameter name [(name)]",new String[]{(o==null?"":o.toString()),type,name});
                }
            }
            if(null != desc.get("@enum")){
                Object ev = desc.get("@enum");
                if(null!=ev && null != o){
                    if(ev instanceof Collection){
                        Iterator it= ((Collection)ev).iterator();
                        boolean b = false;
                        while(it.hasNext()){
                            Object v = it.next();
                            if(o.equals(v)){
                                b=true;
                                break;
                            }
                            if(null !=o && null != v && o.toString().equals(v.toString())){
                                b=true;
                                break;
                            }
                        }
                        if(!b){
                            throw new ISPException("ISP02001",o+" is not in enum [(enum)] parameter name [(name)]",new String[]{ev.toString(),name});
                        }
                    }
                }
            }
            if(null != desc.get("@check") && desc.get("@check") instanceof String && null != o){
                if(null != env) {
                    Object v = env.getParameter("${context}");
                    if(null != v && v instanceof Context) {
                        String rule = (String)desc.get("@check");

                        boolean b = true;
                        if (null != o && o instanceof Map && XMLParameter.isHasRetainChars(rule) || rule.startsWith("(")) {
                            try {
                                XMLParameter pars = obj.getEmptyParameter();
                                pars.putAll((Map) o);
                                Object rul = pars.getExpressValueFromMap(rule, obj);
                                if (rul instanceof String) {
                                    Object r = RuleUtil.doRule((String) rul, (Map) o);
                                    if (null != r && r instanceof Boolean && !(Boolean) r) {
                                        b = (Boolean) r;
                                    } else if (null != r) {
                                        b = StringUtils.isTrue(r.toString());
                                    }
                                }
                            } catch (ISPException e) {
                                throw e;
                            } catch (Exception e) {
                                log.error("", e);

                            }
                        } else {
                            b = ((Context) v).checkFormat(rule, o);
                        }
                        if (!b) {
                            throw new ISPException("ISP02001", "data [(value)] isinvalid type parameter name [(name)]", new String[]{o.toString(), name});
                        }


                    }
                }
            }
            if(null != desc.get("@depend")){

            }
            if(null != desc.get("@length")){
                Object l = desc.get("@length");
                int n = 0;
                if(l instanceof String) n = Integer.parseInt((String)l);
                else n = (int)l;
                if(null != o ){
                    if(o.getClass().isArray() && ((Object[])o).length>n){
                        throw new ISPException("ISP02001","data length is over [(length)]",new String[]{n+""});
                    }else if(o instanceof Collection && ((Collection)o).size()>n){
                        throw new ISPException("ISP02001","data length is over [(length)]",new String[]{n+""});
                    }else if(o instanceof String && ((String)o).length()>n){
                        throw new ISPException("ISP02001","data length is over [(length)]",new String[]{n+""});
                    }
                }
            }
            if(null != desc.get("@isMust")){
                if(StringUtils.isTrue(desc.get("@isMust").toString()) && (null == o || "".equals(o))){
                    throw new ISPException("ISP02001","data is must need in parameter name [(name)]",new String[]{name});
                }
            }
            if(null != desc.get("@checkExpress") && !"".equals(desc.get("@checkExpress"))){
                Object v = env.getValueFromExpress(desc.get("@checkExpress"),obj);
                if (null != v && !StringUtils.isTrue(v.toString())) {
                    throw new ISPException("ISP02001", "data [(value)] does not match express [(express)] type parameter name [(name)]", new String[]{o.toString(),(String)desc.get("@checkExpress"), name});
                }
            }
            if(null != desc.get("@dependFields")){

            }
        }
        return true;
    }


    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;
    }

    @Override
    public ResultCheck checkReturn(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return new ResultCheck(true,ret);
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config,Object ret,Exception e) throws Exception {
        return false;
    }


}
