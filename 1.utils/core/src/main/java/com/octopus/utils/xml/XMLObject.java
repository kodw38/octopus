package com.octopus.utils.xml;

import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cachebatch.DateTimeUtil;
import com.octopus.utils.cls.ClassUtils;
import com.octopus.utils.cls.jcl.MyURLClassLoader;
import com.octopus.utils.cls.jcl.SingleClassLoader;
import com.octopus.utils.cls.proxy.IMethodAddition;
import com.octopus.utils.file.FileInfo;
import com.octopus.utils.file.FileUtils;
import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.thread.ds.InvokeTask;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import com.octopus.utils.xml.desc.Desc;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用于自动根据xml配置文件生成对象的类。
 * xml对象的定义：
 *  1.装入的xml配置文本，根标签的id和本对象xmlId标识一致。
 *  2.标签的clazz 属性指定的类继承了XMLObject类，指定类可以是本实现类的父类。实现类可以解析老的xml。
 *  3.如果标签只有名称没有text也没有children，没有其他属性，为类应用,应用夫对象中的同名变量。如：<instancesmgr/>
 * 功能：
 *  1.本实现类解析xml
 *  2.设置xml属性到本实现类
 *  3.设置xml属性到本set方法
 *
 * User: wangfeng2
 * Date: 14-8-18
 * Time: 下午6:00
 */
public abstract class XMLObject implements Serializable,Comparable{
    static transient Log log = LogFactory.getLog(XMLObject.class);
    //xml配置文本中与之匹配的id
    //protected String xmlId;

    //构造对象时忽略的标签
    private static String[] ignoreNames={"defs","def"};

    //xml可以是绝对路径也可以是classpath
    private static String[] FIX_PROPERTIES={"xmlid","xml","seq","key","clazz","isenable","classloader","xmlExtProperties","xmlExtFields"};

    //代理类的标签
    private static String FIX_TITLE_XMLOBJECT_INVOCATIONHANDLER = "handler";

    //绝对路径
    private static String FIX_TITLE_CHILDREN_XMLOBJECT_DIRECTORY = "childrendir";

    //多版本服务的容器Map<服务名称,<版本号,服务>>
    //protected static Map<String,Map<String,XMLObject>> mltVersionXmlObjectContainer = new ConcurrentHashMap<String, Map<String,XMLObject>>();

    //实例化类的ClassLoader，默认为该类的ClassLoader
    private ClassLoader classLoader;

    //xml配置文件
    private XMLMakeup xml;

    private XMLObject parent;

    private double seq;

    //加载的对象文件路径
    private String path;

    String id;
    boolean isactive=true; // 激活的可以被调用

    private Properties xmlExtProperties= new Properties();
    private Map xmlExtFields= new HashMap();

    static Map<String,XMLObject> XmlObjectContainer = new ConcurrentHashMap<String, XMLObject>(); //主的对象容器空间
    static List<String> objectIds = Collections.synchronizedList(new LinkedList());
    static HashMap<String,Map> descCache = new HashMap(); // only store invoke structure desc
    static HashMap<String,Map> descInvokeCache = new HashMap(); // only store invoke structure desc
    //系统配置完成后初始化的动作
    static List<Object[]> initActions = new LinkedList<Object[]>();
    static List<Object[]> initAfterActions = new LinkedList<Object[]>();
    //store refer objId and Field list
    static Map<String,List<Object[]>> referRelFieldMap = new HashMap();

    Map<String,XMLObject> singleXmlObjectContainer = null; //独立的容器空间,在有子系统,需要单独运行环境时使用
    //系统配置完成后初始化的动作
    List<Object[]> singleInitActions = null;
    List<Object[]> singleInitAfterActions = null;
    HashMap<String,Map> singleDescCache = null; // only store invoke structure desc
    HashMap<String,Map> singleDescInvokeCache =null; // only store invoke structure desc
    boolean isSystemReady=false;

    static List<Runnable> systemFinishedEnvets = new LinkedList<Runnable>();

    public static XMLObject loadApplication(String mainXmlFilePath,ClassLoader classLoader,boolean isUpdateSaveDesc,boolean isShareContainer) throws Exception, ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchFieldException {
        XMLMakeup xml = XMLUtil.getDataFromXml(mainXmlFilePath);
        return loadApplication(xml,classLoader,isUpdateSaveDesc,isShareContainer);
    }

    /**
     *
     * @param xml  启动的主应用文件
     * @param classLoader 加载类classloader
     * @param isUpdateSaveDesc 这个服务容器是否可以变更服务， true可以变更（默认true），false不能变更（如果是copy容器则不能变更服务）
     * @param isShareContainer 是否共享服务容器，true(使用共享服务容器)，false(该应用将使用单独的服务容器)
     * @return
     * @throws Exception
     */
    public static XMLObject loadApplication(XMLMakeup xml,ClassLoader classLoader,boolean isUpdateSaveDesc,boolean isShareContainer)throws Exception{
        Object[] others=null;
        if(!isShareContainer){
            if(isUpdateSaveDesc) {
                HashMap<String,Map> singleDescCache = new HashMap(); // only store invoke structure desc
                HashMap<String,Map> singleDescInvokeCache = new HashMap(); // only store invoke structure desc
                others = new Object[5];
                others[3]=singleDescCache;
                others[4]=singleDescInvokeCache;
            }else{
                others = new Object[3];
            }
            Map<String,XMLObject> singleXmlObjectContainer = new ConcurrentHashMap<String, XMLObject>();
            //系统配置完成后初始化的动作
            List<Object[]> singleInitActions = new LinkedList<Object[]>();
            List<Object[]> singleInitAfterActions = new LinkedList<Object[]>();
            others[0]=singleXmlObjectContainer;
            others[1]=singleInitActions;
            others[2]=singleInitAfterActions;
        }
        log.info("*********************starting application**************************");
        XMLObject ret= createXMLObject(xml,classLoader,null,others);
        log.info("... finished create objects");
        if(null != ret) {
            ret.doObjectInitial();
            log.info("... finished initial each object");
            ret.doSystemLoadInit();
            log.info("... finished initial application, welcome to use");
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(systemFinishedEnvets.size()>0) {
                    for (Runnable run:systemFinishedEnvets){
                        run.run();
                    }
                }
            }
        }).start();
        ret.isSystemReady=true;
        return ret;
    }


    public boolean isSystemReady(){
        return isSystemReady;
    }

    public void addSystemFinishedEvent(Runnable run){
        systemFinishedEnvets.add(run);
    }

    void doObjectInitial(){
        if(null != objectIds){
            Map<String,XMLObject> m = getXMLObjectContainer();
                for (int i=0;i<objectIds.size();i++) {
                    try {
                        if(objectIds.size()>i && null != objectIds.get(i) && null != m.get(objectIds.get(i))) {
                            m.get(objectIds.get(i)).setRefer();
                            m.get(objectIds.get(i)).initial();
                        }
                    } catch (Exception e) {
                        log.error(id + " initial happen error:", e);
                    }
                }

        }
    }

    //最上层的描述在List 0位置
    static List<String> getDescription(XMLObject o)throws IOException{
        if(null !=o) {
            List<String> os = Desc.getExtendObjsDesc(o.getClass());
            LinkedList list = new LinkedList();
            String s = Desc.getThisDescription(o.getXML());
            if (StringUtils.isNotBlank(s)) {
                list.add(s);
                if(null != os) {
                    for (String u : os) {
                        if (StringUtils.isNotBlank(u))
                            list.add(u);
                    }
                }
            }
            if (list.size() > 0) return list;

        }
        return null;
    }

    /**
     * copy 某个服务的desc，并替换指定节点路径中的属性
     * @param
     * @param replaceProperties
     * @return
     */
    public Map copyDesc(String srvName,String newName,Map replaceProperties)throws Exception{
        if(StringUtils.isNotBlank(newName) && StringUtils.isNotBlank(srvName)) {
            /*String m = (String) getThisDescription(xml);
            Map n = StringUtils.convert2MapJSONObject(m);*/
            Map n = getDescStructure(srvName);
            n.put("name", newName);
            String c = (String)n.get("body");
            XMLMakeup x = XMLUtil.getDataFromString(c);
            x.setPropertiesByPathMap(replaceProperties);
            n.put("body",x.toString());
            return n;
        }
        return null;
    }

    /**
     * 修改或设置对象的某些属性，如果属性key存在则将替换为新的值
     * @param propathData
     * @throws Exception
     */
    public void setObjectProperties(Map propathData)throws Exception{
        Map n = getDescStructure(id);
        String c = (String)n.get("body");
        XMLMakeup x = XMLUtil.getDataFromString(c);
        x.setPropertiesByPathMap(propathData);
       n.put("body",x.toString());
        Desc.saveDesc(n);
        getXML().setPropertiesByPathMap(propathData);
        clearCache();
    }

    /**
     * 删除对象的某些属性
     * @param
     * @throws Exception
     */
    public boolean removeObjectProperties(Object pathsMap)throws Exception{
        Map n = getDescStructure(id);
        String c = (String)n.get("body");
        XMLMakeup x = XMLUtil.getDataFromString(c);
        x.removePropertiesByPath(pathsMap);
        n.put("body",x.toString());
        Desc.saveDesc(n);
        boolean b=  getXML().removePropertiesByPath(pathsMap);
        if(b){
            clearCache();
        }
        return b;
    }

    protected Object[] getSingleContainers(){
        if(null == singleXmlObjectContainer){
            return null;
        }else {
            Object[] others = new Object[5];
            others[0] = singleXmlObjectContainer;
            others[1] = singleInitActions;
            others[2] = singleInitAfterActions;
            others[3] = singleDescCache;
            others[4] = singleDescInvokeCache;
            return others;
        }
    }
    //根据当前对象的说明文档
    public List<String> getDescription() throws IOException {
        return getDescription(this);
    }

    //获取某个对象key的说明文档
    public List<String> getDescription(String key) throws IOException {
        XMLObject o = getObjectById(key);
        if(null != o)
        return getDescription(o);
        return null;
    }

    public void addInvokeDescStructure(Map invokeDescStructure){
        if(null != invokeDescStructure){
            if(null == singleDescInvokeCache) {
                descInvokeCache.put((String) invokeDescStructure.get("name"), invokeDescStructure);
            }else{
                singleDescInvokeCache.put((String)invokeDescStructure.get("name"),invokeDescStructure);
            }
        }
    }
    //获取调用的结构，去掉每个属性的说明
    public Map getInvokeDescStructure() throws Exception{
        if(null == singleDescInvokeCache) {
            if (descInvokeCache.containsKey(this.getXML().getId())) {
                return descInvokeCache.get(this.getXML().getId());
            } else {
                Map map = getDescStructure();
                if (null != map) {
                    Map m = Desc.getInvokeDescStructure(map);
                    descInvokeCache.put(this.getXML().getId(), m);
                    return m;
                }
                return null;
            }
        }else{
            if (singleDescInvokeCache.containsKey(this.getXML().getId())) {
                return singleDescInvokeCache.get(this.getXML().getId());
            } else {
                Map map = getDescStructure();
                if (null != map) {
                    Map m = Desc.getInvokeDescStructure(map);
                    singleDescInvokeCache.put(this.getXML().getId(), m);
                    return m;
                }
                return null;
            }
        }
    }
    protected void removeDescCache(String name){
        if(singleDescCache==null && singleDescInvokeCache==null) {
            descCache.remove(name);
            descInvokeCache.remove(name);
        }else{
            singleDescCache.remove(name);
            singleDescInvokeCache.remove(name);
        }
    }
    public void removeDescCache(){
        removeDescCache(id);
    }

    //获取描述的结构
    public Map getDescStructure() throws IOException {
        boolean isin;
        if(singleDescCache==null){
            isin = descCache.containsKey(this.getXML().getId());
        }else{
            isin = singleDescCache.containsKey(this.getXML().getId());
        }
        if(isin){
            if(singleDescCache==null) {
                return descCache.get(this.getXML().getId());
            }else{
                return singleDescCache.get(this.getXML().getId());
            }
        }else {
            LinkedHashMap map = new LinkedHashMap();
            Map t = Desc.getEnptyDescStructure();
            map.putAll(t);
            List<String> desc = getDescription();
            if (null != desc) {
                for (int i = desc.size() - 1; i >= 0; i--) {
                    if (null != desc.get(i)) {
                        Map m = StringUtils.convert2MapJSONObject(desc.get(i));

                        if (null != m) {
                            map.putAll(m);
                        }
                    }
                }
                map.put("path", this.getXML().getProperties().getProperty("path"));
                if(singleDescCache==null) {
                    descCache.put(this.getXML().getId(), map);
                }else{
                    singleDescCache.put(this.getXML().getId(), map);
                }
                if(StringUtils.isBlank((String)map.get("body"))){
                    String body = getXML().toString();
                    map.put("body",body);
                }
                return map;
            }else{
                if(singleDescCache==null) {
                    descCache.put(this.getXML().getId(), null);
                }else{
                    singleDescCache.put(this.getXML().getId(), null);
                }
            }

        }
        return null;
    }
    public Map getStoreDescStructure(String key)throws IOException{
        LinkedHashMap map = new LinkedHashMap();
        Map t = Desc.getEnptyDescStructure();
        map.putAll(t);
        List<String> desc = getDescription(key);
        if(null != desc) {
            for(int i=desc.size()-1;i>=0;i--) {
                //文本内容从下至上覆盖
                if(null != desc.get(i)) {
                    Map m = StringUtils.convert2MapJSONObject(desc.get(i));
                    if (null != m) {
                        map.putAll(m);
                    }
                }
            }
            map.put("path",getObjectById(key).getXML().getProperties().getProperty("path"));
            //if body is null and the service exist , used exist obj xml
            if(StringUtils.isBlank((String)map.get("body")) && null !=getObjectById(key)){
                String body = getObjectById(key).getXML().toString();
                map.put("body",body);
            }

            return  map;
        }
        return null;
    }
    public Map getDescStructure(String key)throws IOException{
        //获取功能描述文件文本内容，0为当前功能文本，1，2，3为继承类功能文本

        if(singleDescCache==null) {
            if (descCache.containsKey(key)) {
                //System.out.println("get desc:"+key);
                return descCache.get(key);
            } else {
                Map map = getStoreDescStructure(key);
                if (null != map) {
                    descCache.put(key, map);
                }else{
                    map = descInvokeCache.get(key);
                }
                if(null != map) {
                    //System.out.println("get desc:"+key);
                    return map;
                }
            }
        }else{
            if (singleDescCache.containsKey(key)) {
               // System.out.println("get desc:"+key);
                return singleDescCache.get(key);
            } else {
                Map map = getStoreDescStructure(key);
                if (null != map) {
                    singleDescCache.put(key, map);
                }else{
                    map = singleDescInvokeCache.get(key);
                }
                if(null != map){
                    //System.out.println("get desc:"+key);
                    return map;
                }
            }

        }
        log.info("not find object[" + key + "] description file");
        return null;
    }


    public XMLObject(XMLMakeup xml,XMLObject parent,Object[] containers) throws Exception {
        initObject(xml,parent,containers);
    }
    void initObject(XMLMakeup xml,XMLObject parent,Object[] containers)throws Exception{
        if(null != containers){
            if(null != containers[0]){
                singleXmlObjectContainer = (Map)containers[0];
            }
            if(containers.length>1 && null != containers[1]){
                singleInitActions = (List)containers[1];
            }
            if(containers.length>2 && null != containers[2]){
                singleInitAfterActions = (List)containers[2];
            }
            if(containers.length>3 && null != containers[3]){
                singleDescCache = (HashMap)containers[3];
            }
            if(containers.length>4 && null != containers[4]){
                singleDescInvokeCache = (HashMap)containers[4];
            }
        }
        if(null != parent)setParent(parent);
        if(null != xml){
            try{
                //if one time used , do not input it into object container, if face interrupt flow node, set property onetime and save the flow service.
                String onetime = xml.getProperties().getProperty("onetime");
                if(StringUtils.isBlank(onetime) || (StringUtils.isNotBlank(onetime) && !StringUtils.isTrue(onetime))) {
                    if(null == singleXmlObjectContainer){
                        XmlObjectContainer.put(xml.getId(), this);
                    }else {
                        singleXmlObjectContainer.put(xml.getId(), this);
                    }
                    objectIds.add(xml.getId());
                    log.info("create XMLObject ok "+xml.getId()+"\n"+xml);
                }

                /*String version = xml.getProperties().getProperty("version");
                String id = xml.getId();
                //当第一个该服务时放入当以服务容器中，不考虑版本
                if(StringUtils.isBlank(version)
                        || ( (!XmlObjectContainer.containsKey(id) || version.equals(XmlObjectContainer.get(id).getXML().getProperties().getProperty("version"))) && !mltVersionXmlObjectContainer.containsKey(id))){
                    XmlObjectContainer.put(id,this);
                }else{
                    //如果已经有该服务了，就把新创建的和以前的都移到版本容器中
                    if(XmlObjectContainer.containsKey(id)){
                        XMLObject xmlObject = XmlObjectContainer.get(id);
                        if(!mltVersionXmlObjectContainer.containsKey(id)){
                            mltVersionXmlObjectContainer.put(id,new LenSortMap(LenSortMap.SORT_DESC));
                        }
                        String v = xmlObject.getXML().getProperties().getProperty("version");
                        if(StringUtils.isBlank(v)){
                            v="0";
                        }
                        mltVersionXmlObjectContainer.get(id).put(v,xmlObject);
                    }
                    if(mltVersionXmlObjectContainer.containsKey(id)){
                        mltVersionXmlObjectContainer.get(id).put(version,this);
                    }
                }
                */
                loadXML(xml);

            }catch (Exception e){
                if(null == singleXmlObjectContainer){
                    XmlObjectContainer.remove(xml.getId());
                }else {
                    singleXmlObjectContainer.remove(xml.getId());
                }
                objectIds.remove(xml.getId());
                throw e;
            }
        }
    }

    public Map<String,XMLObject> getXMLObjectContainer(){
        if(null== singleXmlObjectContainer){
            return XmlObjectContainer;
        }else {
            return singleXmlObjectContainer;
        }
    }
    protected void addSystemLoadInitAction(Object obj,String fu,Class[] cs,Object[] pars){
        if(singleInitActions==null) {
            initActions.add(new Object[]{obj, fu, cs, pars});
        }else{
            singleInitActions.add(new Object[]{obj, fu, cs, pars});
        }
    }
    protected void addSystemLoadInitAfterAction(Object obj,String fu,Class[] cs,Object[] pars){
        if(null == singleInitAfterActions) {
            initAfterActions.add(new Object[]{obj, fu, cs, pars});
        }else{
            singleInitAfterActions.add(new Object[]{obj, fu, cs, pars});
        }
    }

    void doSystemLoadInit(){
        List list = null;
        if(singleInitActions==null){
            list=initActions;
        }else{
            list = singleInitActions;
        }
        for(int i=0;i<list.size();i++){
            Object[] o = (Object[])list.get(i) ;
            try {
                if(null != o[3] && ((Object[])o[3]).length>=3 && null == ((Object[])o[3])[2]){
                    ((Object[])o[3])[2]=getSystempar();
                }
                ExecutorUtils.synWork(o[0], (String) o[1], (Class[]) o[2], (Object[]) o[3]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        List alist = null;
        if(null == singleInitAfterActions) {
            alist=initAfterActions;
        }else{
            alist = singleInitAfterActions;
        }
        for(int i=0;i<alist.size();i++){
            Object[] o = (Object[])alist.get(i) ;
            try {
                if(null != o[3] && ((Object[])o[3]).length>=3 && null == ((Object[])o[3])[2]){
                    ((Object[])o[3])[2]=getSystempar();
                }
                ExecutorUtils.synWork(o[0], (String) o[1], (Class[]) o[2], (Object[]) o[3]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    protected XMLParameter getSystempar() throws Exception {
        XMLDoObject envobj = (XMLDoObject) getRoot();
        Object o = envobj.getPropertyObject("env");
        XMLParameter systempar = null;
        if (null != envobj && null !=o) {
            HashMap input = new HashMap();
            input.put("op", "getSystemParameters");
            return (XMLParameter)envobj.doSomeThing(null, null, input, null, null);
        }
        return null;
    }


    public void setClassLoader(ClassLoader classLoader){
        this.classLoader=classLoader;
    }

    static XMLMakeup getDef(String xmlId,XMLMakeup xml){
        XMLMakeup parent = xml.getParent();
        while(null != parent){
            XMLMakeup ds = (XMLMakeup)ArrayUtils.getFirst(parent.getChild("defs"));
            if(null != ds){
                for(XMLMakeup d:ds.getChild("def")){
                    if(d.getProperties().getProperty("xmlid").equals(xmlId) && StringUtils.isNotBlank(d.getProperties().getProperty("clazz"))){
                        return d;
                    }
                }
            }
            parent=parent.getParent();
        }
        return null;
    }

    static String[] findClazzAndLoader(XMLMakeup xml){
        if(StringUtils.isNotBlank(xml.getProperties().getProperty("xmlid")) || StringUtils.isNotBlank(xml.getProperties().getProperty("clazz"))){
            String[] ret = new String[2];
            if(StringUtils.isNotBlank(xml.getProperties().getProperty("clazz"))){
                ret[0]= xml.getProperties().getProperty("clazz");
            }
            if(StringUtils.isNotBlank(xml.getProperties().getProperty("classloader"))){
                ret[1]= xml.getProperties().getProperty("classloader");
            }
            List<XMLMakeup> defCfg=null;
            if(StringUtils.isBlank(ret[0])){
                XMLMakeup x = getDef(xml.getProperties().getProperty("xmlid"),xml);
                if(null == x) throw new RuntimeException("not def xmlid="+xml.getProperties().getProperty("xmlid"));
                ret[0]=x.getProperties().getProperty("clazz");
                if(StringUtils.isBlank(ret[1]) && StringUtils.isNotBlank(x.getProperties().getProperty("classloader"))){
                    ret[1]= x.getProperties().getProperty("classloader");
                }
                defCfg=x.getChildren();
            }
            if(!xml.getProperties().containsKey("clazz")) {
                xml.getProperties().setProperty("clazz", ret[0]);
                if(null !=defCfg && defCfg.size()>0) {
                    for(int i=0;i<defCfg.size();i++)
                        xml.getChildren().add(i,defCfg.get(i) );
                }
            }
            return ret;
        }
        return null;
    }
    public XMLParameter getEmptyParameter()throws Exception{
        XMLParameter data = new XMLParameter();
        data.addParameter("${env}",getEnvProperties());
        return data;
    }
    public Map getEnvProperties()throws Exception{
        XMLDoObject env = (XMLDoObject)getObjectById("env");
        if(null != env) {
            Map ed = (Map) env.doSomeThing(null, null, null, null, null);
            return ed;
        }
        return new HashMap();
    }

    static XMLObject getXMLInstance(XMLMakeup xml,ClassLoader loader,XMLObject parent,Object[] others) throws Exception, IllegalAccessException, InstantiationException {
        XMLObject object =null;

        if(!xml.isEnable()) return null;
        String[] cl = findClazzAndLoader(xml);
        if(StringUtils.isBlank(xml.getText()) && xml.getChildren().size()==0 && StringUtils.isNotBlank(xml.getProperties().getProperty("xml"))){
            XMLMakeup xp =xml.getParent();
            Properties p = xml.getProperties();
            String name = xml.getName();
            xml = XMLUtil.getDataFromXml(xml.getProperties().getProperty("xml"));
            xml.setParent(xp);
            if(null != cl && null != cl[0]){
                xml.getProperties().put("clazz",cl[0]);
            }else{
                cl = findClazzAndLoader(xml);
            }
            xml.getProperties().putAll(p);
            xml.setName(name);

        }
        if(null != cl && StringUtils.isNotBlank(cl[0])){
            String clazz = cl[0];
            String classLoader=cl[1];
            Class c=null;
            ClassLoader ld = null;
            if(null != loader){
                c = ClassUtils.getClass(loader,clazz);
                ld = loader;
            }else if(StringUtils.isNotBlank(classLoader)){
                ld = (ClassLoader)XMLObject.class.getClassLoader().loadClass(classLoader).newInstance();
                c = ClassUtils.getClass(ld,clazz);
            }else{
                c = ClassUtils.getClass(XMLObject.class.getClassLoader(),clazz);
            }

            if(!ClassUtils.isExtendFrom(XMLObject.class.getName(),c)){
                throw new Exception(c +" is not extends XMLObject or not in same ClassLoader");
            }
            log.debug("create srv:"+xml);
            object = (XMLObject)getProxyClass(xml,c,parent,others);
            object.id=xml.getId();
            object.setClassLoader(ld);

            return object;
        }
        return null;
    }

    static Object getProxyClass(XMLMakeup xml,Class clazz,XMLObject parent,Object[] others) throws Exception {
        if(IMethodAddition.class.isAssignableFrom(clazz)) return clazz.getConstructor(XMLMakeup.class,XMLObject.class,Object[].class).newInstance(xml,parent,others);

        IMethodAddition[] addition =null;
        List<String> proxyMethods = null;
        boolean isallmethod = false;
        Map<String,List<String>> methodMap = new HashMap();
        if(null != parent){
            List li = parent.getAllPropertyObject(FIX_TITLE_XMLOBJECT_INVOCATIONHANDLER);
            if(li.size()>0){

                List<IMethodAddition> ret = new ArrayList();
                for(int i=0;i<li.size();i++){
                    String id= xml.getProperties().getProperty("xmlid");
                    if(((XMLObject)li.get(i)).getXML().getProperties().containsKey("targetxmlids") && StringUtils.isNotBlank(id)){
                        String[] targetXmls = ((XMLObject)li.get(i)).getXML().getProperties().getProperty("targetxmlids").split(",");
                        List ms = new ArrayList();
                        for(String s:targetXmls){
                            s = s.trim();
                            String[] ss = s.split("\\#");
                            if(ss.length>1){
                                if(!methodMap.containsKey(ss[0]))methodMap.put(ss[0],new ArrayList<String>());
                                for(int n=1;n<ss.length;n++){
                                    if(!methodMap.get(ss[0]).contains(ss[n])){
                                        methodMap.get(ss[0]).add(ss[n]);
                                    }
                                    if(!ms.contains(ss[n])) {
                                        ms.add(ss[n]);
                                    }
                                }
                            }else{
                                methodMap.put(ss[0],null);
                            }
                        }
                        if(methodMap.containsKey(id)){
                            IMethodAddition addition1 = (IMethodAddition)li.get(i);
                            //addition1.setMethods(methodMap.get(xml.getProperties().getProperty("xmlid")));
                            if(ms.size()>0) {
                                addition1.setMethods(ms);
                            }
                            ret.add(addition1);
                            proxyMethods = methodMap.get(id);
                        }
                    }else if(((XMLObject)li.get(i)).getXML().getProperties().contains("targetids") && StringUtils.isNotBlank(xml.getId()) ){
                        String[] targetids = ((XMLObject)li.get(i)).getXML().getProperties().getProperty("targetids").split(",");
                        for(String s:targetids){
                            s = s.trim();
                            String[] ss = s.split("\\#");
                            if(ss.length>1){
                                if(!methodMap.containsKey(ss[0]))methodMap.put(ss[0],new ArrayList<String>());
                                for(int n=1;n<ss.length;n++){
                                    if(!methodMap.get(ss[0]).contains(ss[n])){
                                        methodMap.get(ss[0]).add(ss[n]);
                                    }
                                }
                            }else{
                                methodMap.put(ss[0],null);
                            }
                        }
                        if(methodMap.containsKey(xml.getId())){
                            IMethodAddition addition1 = (IMethodAddition)li.get(i);
                            addition1.setMethods(methodMap.get(xml.getId()));
                            ret.add(addition1);
                            proxyMethods = methodMap.get(xml.getId());
                        }
                    }else{
                        //for all
                        if(IMethodAddition.class.isAssignableFrom(li.get(i).getClass())) {
                            ret.add((IMethodAddition) li.get(i));
                            //isallmethod = true;
                        }
                    }
                }
                if(ret.size()>0)
                    addition=(IMethodAddition[])ret.toArray(new IMethodAddition[0]);

            }
        }
        if(null != addition){
            if(log.isDebugEnabled()){
                StringBuffer sb = new StringBuffer();
                for(Object o:addition){
                    if(sb.length()==0)
                    sb.append(o.getClass().getName());
                    else
                        sb.append(",").append(o.getClass().getName());
                }
                log.debug(clazz.getName()+" add proxy "+sb.toString());
            }
            return (XMLObject)ClassUtils.getProxyObject(clazz,proxyMethods,isallmethod,addition,new Class[]{XMLMakeup.class,XMLObject.class,Object[].class}, new Object[]{xml,parent,others},null);
        }else{
            return clazz.getConstructor(clazz.getClassLoader().loadClass(XMLMakeup.class.getName()),clazz.getClassLoader().loadClass(XMLObject.class.getName()),Object[].class).newInstance(xml,parent,others);
        }
    }

    protected boolean isEnable(XMLMakeup xml) throws Exception{
        if (StringUtils.isNotBlank(xml.getProperties().getProperty("isenable"))) {
            String s = xml.getProperties().getProperty("isenable");
            if("true".equalsIgnoreCase(s)){
                return true;
            }else if("false".equalsIgnoreCase(s)){
                return false;
            }
            Object o = getEmptyParameter().getExpressValueFromMap(xml.getProperties().getProperty("isenable"),this);
            if (!ObjectUtils.isTrue(o)) {
                return false;
            }
        }
        return true;
    }

    protected static XMLObject createXMLObject(XMLMakeup xml,ClassLoader classLoader,XMLObject parent,Object[] others) throws Exception, IllegalAccessException, InstantiationException, NoSuchFieldException {
        try{
            if(null != parent && null != parent.getXML() && null == xml.getParent()){
                xml.setParent(parent.getXML());
            }
            /*if(null == classLoader && null != xml.getProperties() && xml.getProperties().containsKey("classpath")){
                String s = xml.getProperties().getProperty("classpath");
                if(StringUtils.isNotBlank(s)){
                    String[] ss = s.split(",");
                    List<URL> li = new ArrayList();
                    for(String f:ss){
                        li.add(new File(f).toURL());
                    }
                    classLoader = new MyURLClassLoader(li.toArray(new URL[0]),XMLObject.class.getClassLoader());

                }
            }*/
            XMLObject object = getXMLInstance(xml, classLoader,parent,others);
            if(null != object){
                if( (null == others|| (others.length>3 && null != others[3] && null != others[4]))) {
                    //初始化,启动特殊属性的对象.
                    object.removeDescCache((String) object.getXML().getId());
                }
                updateRefer(xml,object);
                if(log.isDebugEnabled()) {
                    log.debug("created XMLObject " + xml.getId());
                }

            }

            return object;
        }catch (Exception e){
            throw new Exception(xml.getId(),e);
        }
    }
    static void updateRefer(XMLMakeup xml,XMLObject object) throws IllegalAccessException {
        //reset refer
        if(referRelFieldMap.containsKey(xml.getId())){
            List<Object[]> fs = referRelFieldMap.get(xml.getId());
            if(null != fs && fs.size()>0){
                for(Object[] f:fs){
                    if(!xml.getId().equals((String)f[0])) {
                        if(null != object){
                            Object o = object.getObjectById((String) f[0]);
                            if (null != o) {
                                synchronized (o) {
                                    ((Field) f[1]).set(o, object);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    static void removeRefer(XMLObject object) throws IllegalAccessException {
        if(null != object) {
            if (referRelFieldMap.containsKey(object.getXML().getId())) {
                List<Object[]> fs = referRelFieldMap.get(object.getXML().getId());
                if (null != fs && fs.size() > 0) {
                    for (Object[] f : fs) {
                        if (!object.getXML().getId().equals((String) f[0])) {
                            Object o = object.getObjectById((String) f[0]);
                            if (null != o) {
                                synchronized (o) {
                                    ((Field) f[1]).set(o, null);
                                }
                            }

                        }
                    }
                }
            }
        }
    }

    /**
     * create xmlObject by description map include input,output,config,body etc.
     * @param desc
     * @param loader
     * @param parent
     * @return
     */
    protected XMLObject createXMLObjectByDesc(Map desc,ClassLoader loader,XMLObject parent,boolean isactive,Object[] others) throws Exception {
        //create XMLObject
        if(null == desc)return null;

        if(StringUtils.isBlank((String)desc.get("date"))) {
            desc.put("date", DateTimeUtil.getCurrDateTime());
        }
        XMLMakeup x = Desc.getInvokeStructure(desc);
        if(null == x)return null;
        if(!desc.get("name").equals(x.getProperties().getProperty("key"))){
            desc.put("body",StringUtils.replace((String)desc.get("body"),"key=\""+x.getProperties().getProperty("key")+"\"","key=\""+(String)desc.get("name")+"\""));
            x.getProperties().put("key",desc.get("name"));

        }
        //save desc
        Desc.saveDesc(desc);
        XMLObject obj=null;
        if (null != x) {
            if(isEnable(x)) {
                obj = createXMLObject(x, loader, parent, others);
            }
        }
        if(null != obj) {
            if (isactive) {
                obj.activeObject();
            } else {
                obj.suspendObject();
            }
            notifyObjectByName("launcher",true, "addService", desc);


        }

        return obj;
    }

    public boolean removeObject(String id) throws Exception {
        XMLObject o = getObjectById(id);
        if(null !=o){
            synchronized (o){
                removeRefer(o);
                o.destroy();
                removeDescCache(id);
                notifyObjectByName("launcher",true, "deleteService", id);
                o.suspendObject();
                o.removeXMLObjectById(o.getXML().getId());
                Desc.removeDesc((String)o.getXML().getProperties().get("package"),id);
            }
            return true;
        }
        return false;
    }
    protected boolean updateObjectByDesc(Map desc)throws Exception{
        if(StringUtils.isBlank((String)desc.get("date"))) {
            desc.put("date", DateTimeUtil.getCurrDateTime());
        }
        String name = (String)desc.get("name");
        XMLObject o=null;
        if(null == singleXmlObjectContainer){
            o = XmlObjectContainer.get(name);
        }else{
            o = singleXmlObjectContainer.get(name);
        }
        if(null != o) {
            synchronized (o) {
                boolean b = o.isActive();
                o.destroy();
                //desc.put("name","temp_"+name);
                createXMLObjectByDesc(desc, o.getClass().getClassLoader(), o.getParent(), b, getSingleContainers());
            /*removeObject(name);
            newobj.rename(name);
            desc.put("name",name);
            Desc.saveDesc(desc);
            Desc.removeDesc("temp_"+name);*/
            log.info("upload XMLObject "+name+" successfully");
                return true;
            }
        }
        return false;
    }

    public synchronized void notifyObjectByName(String action,boolean isAsyn,String op,Object obj){
        XMLObject o=null;
        if(null == singleXmlObjectContainer){
            o = XmlObjectContainer.get(action);
        }else{
            o = singleXmlObjectContainer.get(action);
        }

        if(o == null) {
            Iterator its = null;
            if(null == singleXmlObjectContainer){
                its = XmlObjectContainer.keySet().iterator();
            }else {
                its = singleXmlObjectContainer.keySet().iterator();
            }
            while (its.hasNext()) {
                if(null == singleXmlObjectContainer){
                    o = XmlObjectContainer.get(its.next());
                }else {
                    o = singleXmlObjectContainer.get(its.next());
                }
                if (o.getXML().getName().equals(action)) {
                    break;
                }else{
                    o=null;
                }
            }
        }

        if(null == o) {
            log.error("not find object [" + action + "]");
        }
        log.debug("notifyObject :"+o.getClass().getName()+" "+o.getXML().getId()+" op:"+op);
        if(isAsyn){
            ExecutorUtils.work(o, "notifyObject", new Class[]{String.class, Object.class}, new Object[]{op, obj});
        }else {
            try {
                ExecutorUtils.synWork(o, "notifyObject", new Class[]{String.class, Object.class}, new Object[]{op, obj});
            } catch (Exception e) {
                log.error("notifyObject fail", e);
            }
        }
        //o.notifyObject(op,obj);

    }
    public abstract void notifyObject(String op,Object obj)throws Exception;
    public abstract void addTrigger(Object cond,InvokeTaskByObjName task)throws Exception;
    public abstract void destroy()throws Exception;
    //do initial after all system object created
    public abstract void initial()throws Exception;


    boolean loadXML(XMLMakeup xml) throws Exception, NoSuchFieldException, IllegalAccessException, InstantiationException {
        this.xml=xml;
        //xml property设置到当前实现类
        log.debug("begin init properties "+xml.getId());
        setProperties(xml.getProperties());
        log.debug("end init properties "+xml.getId());
        //设置xml子对象
        log.debug("begin init children "+xml.getId());
        setChildren(xml.getChildren());
        log.debug("end init children "+xml.getId());
        return true;
    }



    /**
     * 设置properties中的属性到该实现类的field中
     * @param properties
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    boolean setProperties(Properties properties) throws NoSuchFieldException, Exception {
        Enumeration es = properties.keys();
        Object obj = ClassUtils.getFieldValue(this,"xmlExtProperties",false);
        Properties pro = null;
        if(null != obj && obj instanceof Properties){
            pro = (Properties)obj;
        }
        boolean ret=false;
        while(es.hasMoreElements()){
            String k = (String)es.nextElement();
            Object v = properties.get(k);
            if(null !=v) {
                if(!(v instanceof String)){
                    v = String.valueOf(v);
                }
                if (!ArrayUtils.isInStringArray(FIX_PROPERTIES, k) && StringUtils.isNotBlank(v)) {
                    if (!ClassUtils.setFieldValue(this, k, v, false)) {
                        if (null != pro) {
                            pro.put(k, v);
                            ret = true;
                        }
                    } else {
                        ret = true;
                    }
                    clearCache();
                } else if (k.equals("seq") && StringUtils.isNotBlank(v)) {
                    seq = Double.parseDouble((String) v);
                }
            }

        }
        return ret;
    }

    void clearCache(String id){
        if(StringUtils.isNotBlank(id)) {
            if (null != descInvokeCache)
                descInvokeCache.remove(id);
            if (null != descCache)
                descCache.remove(id);
            if (null != singleDescInvokeCache)
                singleDescInvokeCache.remove(id);
            if (null != singleDescCache)
                singleDescCache.remove(id);
        }
    }
    void clearCache(){
        clearCache(id);
    }

    Object getNamesObject(Object o,String name) throws NoSuchFieldException, IllegalAccessException, InstantiationException {
        Field field = ClassUtils.getField(o,name+"s",false);
        Object ret=null;
        if(null != field){
            field.setAccessible(true);
            Object obj = field.get(o);
            if(null == obj){
                try {
                    if (null != field.getType().getConstructor()) {
                        obj = field.getType().newInstance();
                        field.set(o, obj);
                    }
                }catch (Exception e){}
            }
            ret= obj;
        }
        if(null == ret){
            ret= ((XMLObject)o).xmlExtFields.get(name+"s");
        }
        if(null == ret){
            ret= (XMLObject)((XMLObject)o).xmlExtFields.get(name);
        }
        return ret;
    }

    /**
     * 设置clildren中有clazz属性的项，设置到类的属性中
     * @param list
     */
    void setChildren(List<XMLMakeup> list) throws Exception, IllegalAccessException, InstantiationException, NoSuchFieldException {
        Object objMap = ClassUtils.getFieldValue(this,"xmlExtFields",false);
        Map extFields = null;
        if(null != objMap && objMap instanceof Map){
            extFields = (Map)objMap;
        }
        for(XMLMakeup xml:list){
            if(ArrayUtils.isInStringArray(ignoreNames,xml.getName())) continue;

            if(FIX_TITLE_CHILDREN_XMLOBJECT_DIRECTORY.equals(xml.getName()) && xml.getProperties().containsKey("path") && StringUtils.isNotBlank(xml.getProperties().getProperty("path"))){
                List<XMLMakeup> ls = XMLUtil.getXmlFromDir(xml.getProperties().getProperty("path"));
                if(null != ls){
                    setChildren(ls);
                }
                continue;
            }

            if((xml.getProperties().size()==0|| (xml.getProperties().size()==1 && xml.getProperties().containsKey("desc"))) && xml.getChildren().size()>0) {
                setChildren(xml.getChildren());
                continue;
            }
            log.debug("== begin init children "+xml.getId());
            Field field = ClassUtils.getField(this,xml.getName(),false);
            //for single instance in object. if field value exist ,it means the field is static type and init a value , than not set value again.
            if(null!=field && null != field.get(this)) {
                continue;
            }
            List array=null;
            Map map = null;
            if(null == field){
                Object obj = getNamesObject(this,xml.getName());
                if(null != obj){
                    if(obj instanceof List){
                        array=(List)obj;
                    }
                    if(obj instanceof Map){
                        map=(Map)obj;
                    }
                }
            }
            // text property
            if(xml.getChildren().size()==0 && StringUtils.isNotBlank(xml.getText()) && !xml.getProperties().containsKey("xmlid") && !xml.getProperties().containsKey("clazz")){
                if(xml.getProperties().size()==0 && null != field){
                    field.setAccessible(true);
                    field.set(this,xml.getText().trim());
                }
                if(xml.getProperties().containsKey("key")){
                    if(null != map){
                        map.put(xml.getProperties().getProperty("key"),xml.getText());
                    }else{
                        xmlExtProperties.put(xml.getProperties().getProperty("key"),xml.getText());
                    }
                }else{
                    if(null != map){
                        map.put(xml.getName(),xml.getText());
                    }else{
                        xmlExtProperties.put(xml.getName(),xml.getText());
                    }
                }
                continue;
            }

            //refer

            setRefer(xml,field,array,map,extFields);

            // clazz
            if(isEnable(xml)) {
                XMLObject object = createXMLObject(xml, null, this, getSingleContainers());
                if (null != object) {
                    if (null != field) {
                        field.setAccessible(true);
                        field.set(this, object);
                    } else if (null != array) {
                        if (StringUtils.isNotBlank(xml.getProperties().getProperty("seq"))) {
                            array.add(object);
                        }
                    } else if (null != map) {
                        if (StringUtils.isNotBlank(xml.getProperties().getProperty("key"))) {
                            map.put(xml.getProperties().getProperty("key"), object);
                        } else if (StringUtils.isNotBlank(xml.getProperties().getProperty("id"))) {
                            map.put(xml.getProperties().getProperty("id"), object);
                        }
                    } else if (null != extFields) {
                        String names = xml.getName() + "s";
                        if (StringUtils.isNotBlank(xml.getProperties().getProperty("seq"))) {
                            if (!extFields.containsKey(names)) {
                                extFields.put(names, new ArrayList());
                            }
                            ((List) extFields.get(names)).add(object);
                        } else if (StringUtils.isNotBlank(xml.getProperties().getProperty("key"))) {
                            if (!extFields.containsKey(names)) {
                                extFields.put(names, new HashMap());
                            }
                            ((Map) extFields.get(names)).put(xml.getProperties().getProperty("key"), object);
                        } else {
                            extFields.put(xml.getName(), object);
                        }
                    }

                }
            }
            log.debug("== end init children "+xml.getId());
        }
    }

    //objId refer Field List
    void setRefer(){
        try {
            List<XMLMakeup> list = xml.getChildren();
            if(null != list) {
                for (XMLMakeup xml : list) {
                    if(StringUtils.isBlank(xml.getText()) && xml.getChildren().size()==0
                            && !xml.getProperties().containsKey("clazz")
                            && !xml.getProperties().containsKey("xmlid")
                            ) {
                        String id= xml.getName();
                        String aid=id;
                        if(StringUtils.isNotBlank(xml.getProperties().getProperty("ref"))){
                            aid = xml.getProperties().getProperty("ref");
                        }
                        if(StringUtils.isNotBlank(id)){
                            try {
                                Field f = ClassUtils.getField(this, id, false);
                                if(null != f) {
                                    f.setAccessible(true);
                                    Object v = f.get(this);
                                    if(null == v) {
                                        Object o = getObjectById(aid);
                                        if (null != o) {
                                            f.set(this, o);

                                            setReferField(id,f);
                                        }
                                    }
                                }
                            }catch (Exception e){

                            }
                        }
                    }
                }
            }
        }catch (Exception e){

        }
    }
    void setReferField(String id,Field f){
        //set refer relation
        if(referRelFieldMap.containsKey(id)){
            referRelFieldMap.get(id).add(new Object[]{this.getXML().getId(),f});
        }else{
            referRelFieldMap.put(id,new ArrayList());
            referRelFieldMap.get(id).add(new Object[]{this.getXML().getId(),f});
        }
    }
    void setRefer(XMLMakeup xml,Field field,List array,Map map,Map extFields)throws Exception{
        if(StringUtils.isBlank(xml.getText()) && xml.getChildren().size()==0
                && !xml.getProperties().containsKey("clazz")
                && !xml.getProperties().containsKey("xmlid")){
            XMLObject object=null;
            try{
                String id= xml.getName();
                if(StringUtils.isNotBlank(xml.getProperties().getProperty("ref"))){
                    id = xml.getProperties().getProperty("ref");
                }
                object = (XMLObject)getObjectById(id);
            }catch (Exception e){
                log.error(xml.toString());
            }
            if(null != object){
                if(null != field){
                    field.setAccessible(true);
                    field.set(this,object);
                    setReferField(object.getXML().getId(),field);
                }else if(null != array){
                    if(StringUtils.isNotBlank(xml.getProperties().getProperty("seq"))){
                        array.add(object);
                    }
                }else if(null != map){
                    if(StringUtils.isNotBlank(xml.getProperties().getProperty("key"))){
                        map.put(xml.getProperties().getProperty("key"),object);
                    }
                }else if(null != extFields){
                    extFields.put(xml.getName(),object);
                }

            }
        }
    }

    protected void setParent(XMLObject parent){
        this.parent=parent;
    }

    public XMLObject getParent(){
        return parent;
    }

    public double getSeq(){
        return seq;
    }

    public Properties getXmlExtProperties(){
        return xmlExtProperties;
    }

    void getPropertyObjectFromOneObject(XMLObject obj,String fieldName,List ret) throws NoSuchFieldException, IllegalAccessException, InstantiationException {
        XMLObject o = (XMLObject)ClassUtils.getFieldValue(obj,fieldName,false);
        if(null != o){
            if(!ret.contains(o))
                ret.add(o);
        }else{
            Object os = getNamesObject(obj,fieldName);
            if(null != os){
                if(os instanceof List){
                    for(int i=0;i<((List) os).size();i++){
                        if(!ret.contains(((List) os).get(i)))
                            ret.add(((List) os).get(i));
                    }
                }
                if(os instanceof Map){
                    Iterator its = ((Map) os).keySet().iterator();
                    while(its.hasNext()){
                        Object k = its.next();
                        Object v = ((Map) os).get(k);
                        if(!ret.contains(v))
                            ret.add(v);
                    }
                }else{
                    if(!ret.contains(os)) {
                        ret.add(os);
                    }
                }
            }/*else{
                o = (XMLObject)obj.xmlExtFields.get(fieldName);
                if(null != o){
                    if(!ret.contains(o))
                        ret.add(o);
                }else{
                    os = obj.xmlExtFields.get(fieldName+"s");
                    if(null != os){
                        if(os instanceof List){
                            for(int i=0;i<((List) os).size();i++){
                                if(!ret.contains(((List) os).get(i)))
                                    ret.add(((List) os).get(i));
                            }
                        }
                        if(os instanceof Map){
                            Iterator its = ((Map) os).keySet().iterator();
                            while(its.hasNext()){
                                Object k = its.next();
                                Object v = ((Map) os).get(k);
                                if(!ret.contains(v))
                                    ret.add(v);
                            }
                        }
                    }
                }
            }*/
        }
    }

    void getObject(Object obj,List ret){
        try{
            Field[] fs = ClassUtils.getAllField(obj);
            if(null != fs){
                for(Field f:fs){
                    f.setAccessible(true);
                    if(f.getName().equals("XmlObjectContainer") || f.getName().equals("singleXmlObjectContainer")) continue;
                    Object o = f.get(obj);
                    if(o instanceof XMLObject)
                        ret.add(o);
                }
            }
            if(((XMLObject)obj).xmlExtFields.size()>0){
                Iterator its = ((XMLObject)obj).xmlExtFields.values().iterator();
                while(its.hasNext())
                    ret.add(its.next());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    void getAllObject(String id,Object obj,Map<String,XMLObject> ret){
        try{
            if(obj==null)return;
            if(ClassUtils.isInterfaceImplMatching(Collection.class,obj.getClass())){
                if(((Collection)obj).size()==0){
                    return;
                }
                Iterator its = ((Collection)obj).iterator();
                while(its.hasNext()){
                    getAllObject(null,its.next(),ret);
                }
            }else if(obj instanceof XMLObject){
                if(StringUtils.isBlank(id)){
                    if(!ret.containsKey(((XMLObject)obj).getXML().getId()))
                        ret.put(((XMLObject)obj).getXML().getId(),(XMLObject)obj);
                    else{
                        //log.error("the key "+((XMLObject)obj).getXML().getId()+" has been loaded,can not load again, please check xml config.");
                        return;
                    }
                }else{
                    if(!ret.containsKey(id)){
                        ret.put(id,(XMLObject)obj);
                    }else{
                        //log.error("the key "+id+" has load, please check xml config.");
                        return;
                    }
                }
                Field[] fs = ClassUtils.getAllField(obj);
                if(null != fs){
                    for(Field f:fs){
                        f.setAccessible(true);
                        if(f.getName().equals("XmlObjectContainer")|| f.getName().equals("singleXmlObjectContainer")) continue;
                        Object o = f.get(obj);
                        if(o instanceof Collection || o instanceof Map){
                            getAllObject(null,o,ret);
                        }
                        if(o instanceof XMLObject && !ArrayUtils.isInObjectArray(XMLObject.class.getDeclaredFields(),f)){
                            getAllObject(null,o,ret);
                        }
                    }
                }

            }else if(ClassUtils.isInterfaceImplMatching(Map.class,obj.getClass())){
                if(((Map)obj).size()==0){
                    return;
                }
                Iterator its = ((Map)obj).keySet().iterator();
                while(its.hasNext()){
                    String k = (String)its.next();
                    getAllObject(k,((Map)obj).get(k),ret);
                }
            }
        }catch (Exception e){
            log.error(xml,e);
        }
    }
    public XMLObject getRoot(){
        XMLObject top = this;
        while(null != top.getParent()){
            top = (XMLObject)top.getParent();
        }
        return top;
    }
    public Map<String,Object> getAllObjects(){
        XMLObject top = getRoot();
        Map result = new HashMap();
        getAllObject(null,top,result);
        /*for(Object o :ret){
            if(o instanceof XMLObject){
                if(((XMLObject)o).getXML().getProperties().containsKey("key")){
                    if(!result.containsKey(((XMLObject)o).getXML().getProperties().getProperty("key"))){
                        result.put(((XMLObject)o).getXML().getProperties().getProperty("key"),o);
                    }
                }
                if(!((XMLObject)o).getXML().getProperties().containsKey("key") && !((XMLObject)o).getXML().getProperties().containsKey("seq")){
                    if(!result.containsKey(((XMLObject)o).getXML().getName())){
                        result.put(((XMLObject)o).getXML().getName(),o);
                    }
                }
                if(!((XMLObject)o).getXML().getProperties().containsKey("key") && ((XMLObject)o).getXML().getProperties().containsKey("seq")){
                    if(!result.containsKey(((XMLObject) o).getXML().getParent().getName())){
                        result.put((((XMLObject)o).getXML().getParent().getName()),new ArrayList());
                    }
                    ((List)result.get((((XMLObject)o).getXML().getParent().getName()))).add(o);
                }
            }
        }*/
        return result;
    }

    public List<XMLObject> getAllUpPropertyObjects(){
        List ret = new ArrayList();
        try{
            getObject(this,ret);

            XMLObject p = (XMLObject)getParent();
            while(null != p){
                try{
                    getObject(p,ret);
                }catch (Exception ex){
                }finally {
                    p = (XMLObject)p.getParent();
                }
            }
        }catch (Exception e){
            XMLObject p = (XMLObject)getParent();
            while(null != p){
                try{
                    getObject(p,ret);
                }catch (Exception ex){
                }finally {
                    p = (XMLObject)p.getParent();
                }
            }
        }
        return ret;
    }

    public List getAllPropertyObject(String fieldName){
        List ret = new ArrayList();
        try{
            getPropertyObjectFromOneObject(this,fieldName,ret);

            XMLObject p = (XMLObject)getParent();
            while(null != p){
                try{
                    getPropertyObjectFromOneObject(p,fieldName,ret);
                }catch (Exception ex){
                }finally {
                    p = (XMLObject)p.getParent();
                }
            }
        }catch (Exception e){
            XMLObject p = (XMLObject)getParent();
            while(null != p){
                try{
                    getPropertyObjectFromOneObject(p,fieldName,ret);
                }catch (Exception ex){
                }finally {
                    p = (XMLObject)p.getParent();
                }
            }
        }
        return ret;
    }

    public Object getPropertyObject(String fieldName) {

        try{
            if(this.getXML().getName().equals(fieldName))
                return this;
            Object o = ClassUtils.getFieldValue(this,fieldName,false);
            if(null != o){
                return o;
            }else{
                o = xmlExtFields.get(fieldName);
                if(null != o)
                    return o;
                XMLObject p = (XMLObject)getParent();
                if(p.getXML().getName().equals(fieldName))
                    return p;
                while(null != p){
                    try{
                        o = ClassUtils.getFieldValue(p,fieldName,false);
                        if(null  != o)return o;
                        o = p.xmlExtFields.get(fieldName);
                        if(null != o)return o;
                    }catch (Exception ex){
                        ex.printStackTrace();
                    }finally {
                        p = (XMLObject)p.getParent();
                    }
                }
            }
        }catch (Exception e){
            XMLObject p = (XMLObject)getParent();
            while(null != p){
                try{
                    Object o = ClassUtils.getFieldValue(p, fieldName, false);
                    if(null  != o)return o;
                    o = p.xmlExtFields.get(fieldName);
                    if(null != o)return o;
                }catch (Exception ex){
                }finally {
                    p = (XMLObject)p.getParent();
                }
            }
        }

        return null;
    }

    public XMLObject getObjectById(String id){
        if(null != id) {
            if(null == singleXmlObjectContainer ) {
                return XmlObjectContainer.get(id);
            }else{
                XMLObject o= singleXmlObjectContainer.get(id);
                if(null==o){
                    o= XmlObjectContainer.get(id);
                }
                return o;
            }
        }else{
            return null;
        }
    }

    public Object getPropertyObject(XMLMakeup x) {
        try{
            if(null == x)return null;
            if(StringUtils.isBlank(x.getProperties().getProperty("key")) && StringUtils.isBlank(x.getProperties().getProperty("seq"))){
                return getPropertyObject(x.getName());
            }
            Object o = ClassUtils.getFieldValue(this,x.getName()+"s",false);
            if(null == o){
                o = xmlExtFields.get(x.getName()+"s");
                if(null ==o){
                    XMLObject p = (XMLObject)getParent();
                    while(null != p){
                        try{
                            o = ClassUtils.getFieldValue(p,x.getName()+"s",false);
                            if(null  != o)break;
                            o = p.xmlExtFields.get(x.getName()+"s");
                            if(null != o) break;
                        }catch (Exception ex){
                            ex.printStackTrace();
                        }finally {
                            p = (XMLObject)p.getParent();
                        }
                    }
                }

            }
            if(null != o){
                if(StringUtils.isNotBlank(x.getProperties().getProperty("key")) && o instanceof Map){
                    return (XMLObject)((Map)o).get(x.getProperties().getProperty("key"));
                }else if(StringUtils.isNotBlank(x.getProperties().getProperty("seq")) && o instanceof List){
                    return (XMLObject)((List)o).get(Integer.parseInt(x.getProperties().getProperty("seq")));
                }
            }
        }catch (Exception e){
            log.error(e.getMessage(),e);
        }

        return null;
    }

    //修改先修改xmlmakeup中在内容，在修改sourcefile中在内容
    public synchronized boolean updateXML(String oldXmlString,String newXmlString) throws Exception, IOException, NoSuchFieldException, InstantiationException, IllegalAccessException {
        //find xmlString
        XMLObject root = findXMLString(oldXmlString);
        if(null ==root)throw new Exception("not find oldXml in XMLObject:"+oldXmlString);
        XMLMakeup old = XMLUtil.getDataFromString(oldXmlString);
        Object[] os = findXMLObject(root, old);
        if(null != os && os.length==3){
            try{
                XMLMakeup newxml = XMLUtil.getDataFromString(newXmlString);
                if(null != (XMLMakeup)os[1]){
                    XMLMakeup parentxml = ((XMLMakeup)os[1]).getParent();
                    parentxml.getChildren().remove((XMLMakeup)os[1]);
                    parentxml.getChildren().add(newxml);
                    newxml.setParent(parentxml);
                }
                if(null != (XMLObject)os[0]){
                    XMLObject parent = ((XMLObject)os[0]).getParent();
                    XMLObject o=null;
                    if((Boolean)os[2]) {
                        if(isEnable(newxml)) {
                            o = createXMLObject(newxml, os[0].getClass().getClassLoader(), parent, getSingleContainers());
                        }
                    }else {
                        if(isEnable(((XMLObject) os[0]).getXML())) {
                            o = createXMLObject(((XMLObject) os[0]).getXML(), os[0].getClass().getClassLoader(), parent, getSingleContainers());
                        }
                    }
                    if(null != parent && null != o)
                        updateXMLObject(parent,(XMLObject)os[0],o);
                    else
                        throw new RuntimeException("not support root XMLObject update now.");
                }
                if(null != root){
                    updateXmlString(root.getXML().getSourcePath(),oldXmlString,newXmlString);
                }
                return true;
            }catch (Exception e){
                log.error(e);
                return false;
            }finally {

            }
        }
        return false;
    }
    public synchronized boolean addXML(String parentString,String newXmlString) throws Exception, IOException, NoSuchFieldException, InstantiationException, IllegalAccessException {
        XMLObject root = findXMLString(parentString.substring(0,parentString.length()-2));
        if(null ==root)throw new Exception("not find oldXml in XMLObject:"+parentString);
        XMLMakeup xml = XMLUtil.getDataFromString(parentString);
        Object[] os = findXMLObject(root, xml);
        if(null != os && os.length==3){
            try{
                XMLMakeup newxml = XMLUtil.getDataFromString(newXmlString);
                if(null != (XMLMakeup)os[1]){
                    XMLMakeup parentxml= (XMLMakeup)os[1];
                    parentxml.getChildren().add(newxml);
                    newxml.setParent(parentxml);
                }
                if(null != (XMLObject)os[0]){
                    XMLObject parent = ((XMLObject)os[0]);
                    XMLObject o=null;
                    if(isEnable(((XMLObject)os[0]).getXML())) {
                        o = createXMLObject(((XMLObject) os[0]).getXML(), parent.classLoader, parent.getParent(), getSingleContainers());
                    }
                    if(null != o && null != parent.getParent())
                        updateXMLObject(parent.getParent(),(XMLObject)os[0],o);
                }
                if(null != root){
                    addXmlString(root.getXML().getSourcePath(), parentString, newXmlString);
                }
                return true;
            }catch (Exception e){
                log.error(e);
                return false;
            }finally {
            }
        }
        return false;
    }

    public synchronized boolean removeXML(String xmlString) throws Exception, IOException, NoSuchFieldException, InstantiationException, IllegalAccessException {
        XMLObject root = findXMLString(xmlString);
        if(null ==root)throw new Exception("not find oldXml in XMLObject:"+xmlString);
        XMLMakeup xml = XMLUtil.getDataFromString(xmlString);
        Object[] os = findXMLObject(root, xml);
        if(null != os && os.length==3){
            try{
                if(null != (XMLMakeup)os[1]){
                    if(null == singleXmlObjectContainer) {
                        XmlObjectContainer.remove(((XMLMakeup) os[1]).getId());
                    }else{
                        singleXmlObjectContainer.remove(((XMLMakeup) os[1]).getId());
                    }
                    objectIds.remove(((XMLMakeup) os[1]).getId());
                    XMLMakeup parentxml= ((XMLMakeup)os[1]).getParent();
                    parentxml.getChildren().remove((XMLMakeup) os[1]);
                }
                if(null != (XMLObject)os[0]){
                    XMLObject parent = ((XMLObject)os[0]);
                    if(isEnable(((XMLObject)os[0]).getXML())) {
                        XMLObject o = createXMLObject(((XMLObject) os[0]).getXML(), parent.classLoader, parent.getParent(), getSingleContainers());
                        if (null != o && null != parent.getParent())
                            updateXMLObject(parent.getParent(), (XMLObject) os[0], o);
                    }
                }
                if(null != root){
                    removeXmlString(root.getXML().getSourcePath(), xmlString);
                }
                return true;
            }catch (Exception e){
                log.error(e);
                return false;
            }finally {
            }
        }
        return false;
    }
    //向下查找
    XMLObject findXMLString(String xmlString) throws IOException {
        XMLObject obj= this;
        StringBuffer sb = getSourceFile(obj.getXML().getSourcePath());
        if(null != sb && sb.indexOf(xmlString)>0){
            return obj;
        }else{
            Map<String,XMLObject> os = new HashMap();
            getAllObject(null,obj, os);
            if(os.size()>0){
                Iterator its = os.keySet().iterator();
                while(its.hasNext()){
                    XMLObject o = os.get(its.next());
                    sb = getSourceFile(o.getXML().getSourcePath());
                    if(null != sb && sb.indexOf(xmlString)>0){
                        return o;
                    }
                }
            }
        }
        return null;
    }
    //在一个xml对象中查找某个xml元素
    Object[] findXMLMakeup(XMLMakeup xmlMakeup,XMLMakeup find){
        //如果有key，根据key相同判断，如果没有key，判断所有属性
        if(sameXmlObject(xmlMakeup,find)){
            return new Object[]{xmlMakeup,true};
        }
        List<XMLMakeup> chl = xmlMakeup.getChildren();
        if(null != chl && chl.size()>0){
            for(XMLMakeup x:chl){
                if(x.getProperties().containsKey("xmlid") || x.getProperties().containsKey("xml") || x.getProperties().containsKey("clazz")) continue;
                if(sameXmlObject(x,find))
                    return new Object[]{x,false};
            }
        }
        return null;
    }
    boolean sameXmlObject(XMLMakeup xmlMakeup,XMLMakeup find){
        if(find.getProperties().containsKey("key") && xmlMakeup.getProperties().containsKey("key") && xmlMakeup.getProperties().getProperty("key").equals(find.getProperties().getProperty("key"))){
            return true;
        }else if(find.getProperties().containsKey("id") && xmlMakeup.getProperties().containsKey("id") && xmlMakeup.getProperties().getProperty("id").equals(find.getProperties().getProperty("id"))){
            return true;
        }else if(xmlMakeup.getName().equals(find.getName()) && sameProperty(find.getProperties(),xmlMakeup.getProperties())){
            return true;
        }
        return false;
    }
    //在一个xml对象中查找某个xml元素
    Object[] findXMLObject(XMLObject root,XMLMakeup find){
        Map<String,XMLObject> ls = new HashMap();
        getAllObject(null,root,ls);
        if(ls.size()>0){
            Iterator ist = ls.keySet().iterator();
            while(ist.hasNext()){
                XMLObject o  = ls.get(ist.next());
                Object[] ret = findXMLMakeup(o.getXML(),find);
                if(null != ret){
                    return new Object[]{o,ret[0],ret[1]};
                }
            }
        }
        return null;
    }

    //比较两个properties是否一样
    boolean sameProperty(Properties p1,Properties p2){
        if(p1.size()==p2.size()){
            Iterator its = p1.keySet().iterator();
            while(its.hasNext()){
                Object k = its.next();
                if(!(p2.containsKey(k) && p1.get(k).equals(p2.get(k)))){
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    //获取对象资源配置文本
    StringBuffer getSourceFile(String path) throws IOException {
        List<FileInfo> ls = FileUtils.getAllProtocolFiles(path,null,false);
        if(null != ls && ls.size()>0){
           return FileUtils.getFileContentStringBuffer(ls.get(0).getInputStream());
        }
        return null;
    }
    //向上查找
    public StringBuffer getSource() throws Exception {
        XMLMakeup x = this.getXML();
        while(null !=x && StringUtils.isBlank(x.getSourcePath() )){
            x = x.getParent();
        }
        return getSourceFile(x.getSourcePath());
    }
    public synchronized boolean removeXMLObjectById(String id)throws Exception{
        XMLObject old = null;
        if(singleXmlObjectContainer==null) {
            old=XmlObjectContainer.get(id);
            XmlObjectContainer.remove(id);
        }else{
            old=singleXmlObjectContainer.get(id);
            singleXmlObjectContainer.remove(id);
        }
        objectIds.remove(id);
        XMLObject parent = old.getParent();
        if(null != parent){
            synchronized (parent){
                Object o = ClassUtils.getFieldValue(parent,old.getXML().getName(),false);
                if(null == o){
                    o = ClassUtils.getFieldValue(parent,old.getXML().getName()+"s",false);
                    if(null != o){
                        if(o instanceof List){
                            ((List)o).remove(old);
                        }
                        if(o instanceof Map && StringUtils.isNotBlank(old.getXML().getProperties().getProperty("key"))){
                            if(((Map)o).containsKey(old.getXML().getProperties().getProperty("key")))
                                ((Map)o).remove(old.getXML().getProperties().getProperty("key"));
                        }
                        if(o instanceof Map && StringUtils.isNotBlank(old.getXML().getProperties().getProperty("id"))){
                            if(((Map)o).containsKey(old.getXML().getProperties().getProperty("id")))
                                ((Map)o).remove(old.getXML().getProperties().getProperty("id"));
                        }
                    }else{
                        o = parent.xmlExtFields.get(old.getXML().getName());
                        if(null ==o){
                            o = parent.xmlExtFields.get(old.getXML().getName()+"s");
                            if(null != o){
                                if(o instanceof List){
                                    ((List)o).remove(old);
                                }
                                if(o instanceof Map && StringUtils.isNotBlank(old.getXML().getProperties().getProperty("key"))){
                                    ((Map)o).remove(old.getXML().getProperties().getProperty("key"));
                                }
                            }
                        }else{
                            parent.xmlExtFields.remove(old.getXML().getName());
                        }
                    }
                }else{
                    ClassUtils.setFieldValue(parent,old.getXML().getName(),null,false);
                }
                return true;
            }
        }
        return false;
    }
    //替换对象中的xml元素，如果该元素是XMLObject中的元素，更新XMLObject对象
    public synchronized boolean  updateXMLObject(XMLObject parent,XMLObject old,XMLObject newo) throws Exception {
        synchronized (parent){
            Object o = ClassUtils.getFieldValue(parent,old.getXML().getName(),false);
            if(null == o){
                o = ClassUtils.getFieldValue(parent,old.getXML().getName()+"s",false);
                if(null != o){
                    if(o instanceof List){
                        ((List)o).remove(old);
                        ((List)o).add(newo);
                        Collections.sort((List)o);
                    }
                    if(o instanceof Map && StringUtils.isNotBlank(old.getXML().getProperties().getProperty("key"))){
                        ((Map)o).remove(old.getXML().getProperties().getProperty("key"));
                        ((Map)o).put(((Map)o),newo);
                    }
                }else{
                    o = parent.xmlExtFields.get(old.getXML().getName());
                    if(null ==o){
                        o = parent.xmlExtFields.get(old.getXML().getName()+"s");
                        if(null != o){
                            if(o instanceof List){
                                ((List)o).remove(old);
                                ((List)o).add(newo);
                                Collections.sort((List)o);
                            }
                            if(o instanceof Map && StringUtils.isNotBlank(old.getXML().getProperties().getProperty("key"))){
                                ((Map)o).remove(old.getXML().getProperties().getProperty("key"));
                                ((Map)o).put(((Map)o),newo);
                            }
                        }
                    }else{
                        parent.xmlExtFields.put(old.getXML().getName(),newo);
                    }
                }
            }else{
                ClassUtils.setFieldValue(parent,old.getXML().getName(),newo,false);
            }
            return true;
        }
    }
    void updateXmlString(String path,String oldxml,String newxml) throws Exception {
        StringBuffer sb = getSourceFile(path);
        if(null != sb){
            String content = sb.toString().replace(oldxml,newxml);
            FileUtils.saveAllProtocolFile(path,content);
        }
    }
    void addXmlString(String path,String parent,String newxml) throws Exception {
        StringBuffer sb = getSourceFile(path);
        if(null != sb){
            if(parent.contains("/>")){
                int s = parent.indexOf(" ");
                String title = parent.trim().substring(1, s);
                int n=sb.indexOf(parent);
                if(n>0){
                    String orig = parent.replace("/>",">");
                    String content = sb.replace(n,n+parent.length(),orig+newxml+"</"+title+">").toString();
                    FileUtils.saveAllProtocolFile(path, content);
                }else {
                    String orig = parent.replace("/>",">");
                    n = sb.indexOf(orig);
                    if(n>0){
                        String content = sb.replace(n,n+orig.length(),orig+newxml).toString();
                        FileUtils.saveAllProtocolFile(path, content);
                    }
                }
            }else{
                throw new Exception("not find parent xml") ;
            }
        }
    }
    void removeXmlString(String path,String oldxml) throws Exception {
        StringBuffer sb = getSourceFile(path);
        if(null != sb){
            String content = sb.toString().replace(oldxml,"");
            FileUtils.saveAllProtocolFile(path,content);
        }
    }


    public boolean setProperty(String key,String value) throws NoSuchFieldException, Exception {
        if(StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
            if(null != getXML() && getXML().getProperties().containsKey(key) && value.equals(getXML().getProperties().getProperty(key))){
                return false;
            }else {
                getXML().getProperties().put(key,value);
                Map desc = getDescStructure();
                if(null != desc){
                    desc.put(key,value);
                    Desc.saveDesc(desc);
                }
                Properties p = new Properties();
                p.setProperty(key, value);
                return setProperties(p);
            }
        }else{
            return false;
        }
    }

    public Map getRedoCfg()throws Exception{
        Map m = getDescStructure();
        if(null != m){
            if(null != m.get("redo") && m.get("redo") instanceof Map){
                return (Map)m.get("redo");
            }
        }
        return null;

    }
    public boolean isRedo()throws Exception{
        Map m = getDescStructure();
        if(null != m) {
            if(null != m.get("redo")) {
                if(m.get("redo") instanceof String) {
                    return StringUtils.isTrue((String) m.get("redo"));
                }else if(m.get("redo") instanceof Map && ((Map)m.get("redo")).size()>0){
                    return true;
                }else{
                    return false;
                }
            }else{
                return false;
            }
        }else{
            return false;
        }
    }
    public boolean isRedo(XMLMakeup x){

        Object o = x.getProperties().getProperty("redo");
        if(null != o) {
            if (o instanceof String && !((String) o).startsWith("{")) {
                return StringUtils.isTrue((String) o);
            } else if(o instanceof Map && ((Map)o).size()>0) {
                return true;
            }
        }
        return false;
    }
    public Object clone(){
        try {
            XMLObject object = this.getClass().newInstance();
            XMLMakeup x = XMLUtil.getDataFromString(xml.toString());
            object.loadXML(x);
            return object;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public XMLMakeup getXML(){
        return xml;
    }

    public String toString(){
        return xml.toString();
    }

    public String getPropertyByReferPath(String path) throws NoSuchFieldException, IllegalAccessException {
        String[] ps = path.split("\\.");
        Object o =null;
        for(int i=0;i<ps.length;i++){
            o = ClassUtils.getFieldValue(this,ps[i],false);
            if(o instanceof Map && i< ps.length-1){
                StringBuffer sb = new StringBuffer();
                int s = i+1;
                while(s<=ps.length-1){
                    if(sb.length()==0)
                        sb.append(ps[s]);
                    else
                        sb.append("trunk/core/lib/os").append(ps[s]);
                    s++;
                }
                o=((Map)o).get(sb.toString());
                break;
            }

        }
        if(null != o)
        return o.toString();
        return null;
    }

    @Override
    public int compareTo(Object o) {
        if(o instanceof XMLObject && StringUtils.isNotBlank(getXML().getProperties().getProperty("seq")) && StringUtils.isNotBlank(((XMLObject) o).getXML().getProperties().getProperty("seq")) ){
            return Double.parseDouble(getXML().getProperties().getProperty("seq"))>Double.parseDouble(((XMLObject)o).getXML().getProperties().getProperty("seq"))?1:0;
        }
        return 0;
    }
    public boolean isActive(){
        return isactive;
    }
    public boolean activeObject(){
        this.isactive = true;
        try {
            initial();
        }catch (Exception e){
            log.error(getXML().getId()+" initial happen error when do active:",e);
        }
        return true;
    }
    public boolean suspendObject(){
        this.isactive = false;
        try {
            destroy();
        }catch (Exception e){
            return false;
        }
        return true;
    }
    public void rename(String name){
        getXMLObjectContainer().remove(id);
        objectIds.remove(id);
        id=name;
        getXML().getProperties().put("key",name);
        getXMLObjectContainer().put(id, this);
        objectIds.add(id);
    }


}
