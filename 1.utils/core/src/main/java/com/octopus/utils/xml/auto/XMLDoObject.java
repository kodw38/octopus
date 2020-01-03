package com.octopus.utils.xml.auto;

import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cls.ClassUtils;
import com.octopus.utils.exception.ISPException;
import com.octopus.utils.exception.Logger;
import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.thread.ds.InvokeTask;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.defpro.IExeProperty;
import com.octopus.utils.xml.auto.defpro.IObjectInitProperty;
import com.octopus.utils.xml.auto.defpro.IObjectInvokeProperty;
import com.octopus.utils.xml.auto.defpro.impl.*;
import com.octopus.utils.xml.auto.defpro.impl.utils.DoAction;
import com.octopus.utils.xml.desc.Desc;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import scala.collection.parallel.ParIterableLike;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: Administrator
 * Date: 14-9-3
 * Time: 上午10:32
 */
public abstract class XMLDoObject extends XMLObject implements IXMLDoObject {
    protected static transient Log log = LogFactory.getLog(XMLDoObject.class);
    public static String NOT_EXE_PROPERTY="^NOT_EXE_PROPERTY";
    static LinkedHashMap<String,IExeProperty> initPropertyMap = new LinkedHashMap<String, IExeProperty>();
    static LinkedHashMap<String,IExeProperty> invokePropertyMap = new LinkedHashMap<String, IExeProperty>();
    static List<String> asynList = new ArrayList<String>();
    IObjectInvokeProperty asynProperty = new SimpleAsynProperty(); //简单的异步执行
    private int defaultTimeout=5000;//默认5秒超时
    private int maxTimeoutInvokeCount=10;//连续Timeout超做这个次数,将开始预计Timeout异常，默认10次
    private int preJudgeTimeoutIntervalMinutes=5;//预计Timeout时，保持的时间
    private Map<String,AtomicInteger> timeoutCountMap= null;//当前对象处理时,前一次请求有没有发生timeout异常
    //获取上文的属性定义类
    static Map<String,IExeProperty> getDefProperty(XMLMakeup xml){
        Map<String,IExeProperty> ret = new LinkedHashMap<String, IExeProperty>();
        XMLMakeup parent = xml.getParent();
        while(null != parent){
            XMLMakeup ds = (XMLMakeup) ArrayUtils.getFirst(parent.getChild("defs"));
            if(null != ds){
                for(XMLMakeup d:ds.getChild("def")){
                    if(d.getProperties().containsKey("proid") && StringUtils.isNotBlank(d.getProperties().getProperty("proid")) && StringUtils.isNotBlank(d.getProperties().getProperty("clazz"))){
                        if(!ret.containsKey(d.getProperties().getProperty("proid"))){
                            try{
                                IExeProperty ep = (IExeProperty)Class.forName(d.getProperties().getProperty("clazz")).getConstructor(XMLMakeup.class).newInstance(d);
                                ret.put(d.getProperties().getProperty("proid"),ep);
                            }catch (Exception e){
                                log.error("defined property,the property clazz must implement IObjectInitProperty or IObjectInvokeProperty.");
                            }
                        }
                    }
                }
            }
            parent=parent.getParent();
        }
        return ret;
    }

    public XMLDoObject(XMLMakeup xml,XMLObject parent,Object[] containers) throws Exception {
        super(xml,parent,containers);
        //优先查找自定义的属性处理类
        if(null == xml)return;
        Map pros = getDefProperty(xml);
        if(null != pros){
            Iterator<String> ks = pros.keySet().iterator();
            while(ks.hasNext()){
                String k =ks.next();
                if(ClassUtils.isInterfaceImplMatching(IObjectInitProperty.class,pros.get(k).getClass())){
                    initPropertyMap.put(k,(IObjectInitProperty)pros.get(k));
                }
                if(ClassUtils.isInterfaceImplMatching(IObjectInvokeProperty.class,pros.get(k).getClass())){
                    invokePropertyMap.put(k,(IObjectInvokeProperty)pros.get(k));
                    if(((IObjectInvokeProperty) pros.get(k)).isAsyn())
                        asynList.add(k);
                }
            }
        }
        if(!initPropertyMap.containsKey("worktime"))
            initPropertyMap.put("worktime",new WorkTimeInitProperty());

        if(!initPropertyMap.containsKey("init")){
            initPropertyMap.put("init",new SystemInitialProperty());
        }
        if(!initPropertyMap.containsKey("trigger")){
            initPropertyMap.put("trigger",new TriggerEventProperty());
        }

        if(!invokePropertyMap.containsKey("retry")){
            invokePropertyMap.put("retry", new RetryInvokeProperty());
        }
        if(!invokePropertyMap.containsKey("globalsingle")){
            invokePropertyMap.put("globalsingle",new SingleThreadExeProperty());
        }

        if(!invokePropertyMap.containsKey("concurrence")){
            invokePropertyMap.put("concurrence",new ConcurrenceInvokeProperty());
            asynList.add("concurrence");
        }


        if(!invokePropertyMap.containsKey("worktime")){
            invokePropertyMap.put("worktime", new WorkTimeInitProperty());
            asynList.add("worktime");
        }
        /*如果设置了interruptnotification=true属性，执行到这里时，将中断，抛出等待信息。
          当前请求信息走requestsuspend，并发送消息到kafka，抛出异常信息给使用者。
          kafka处理任务接收到该信息时发送信息给使用者，表明在处理，处理完成后，激活之前的请求设置该节点返回值。再发送信息给使用者。
        */
        if(!invokePropertyMap.containsKey("interruptnotification")){
            invokePropertyMap.put("interruptnotification",new InterruptNotificationProperty());
        }


        /*if(!invokePropertyMap.containsKey("trade")){
            invokePropertyMap.put("trade", new TradeProperty());
        }*/

        //设置是否异步执行属性
        setAsynProperty(xml);
        //设置是否可执行
        setExeProperty(xml);
        //初始时时特殊属性
        //addSystemLoadInitAction(this,"exeProperty",new Class[]{Map.class,XMLMakeup.class,XMLParameter.class,Map.class,Map.class,Map.class,Boolean.class},new Object[]{initPropertyMap,xml,null,getInput(xml.getProperties()),getOutput(xml.getProperties()),getConifg(xml),Boolean.TRUE});
        //exeProperty();
        //addAutoInitChildren(getEmptyParameter(),xml,"init");

    }
    public void addTrigger(Object cond, InvokeTaskByObjName task) throws Exception {
        throw new Exception("This service["+getXML().getId()+"] does not support addTrigger method");
    }

    /**
     *do initial after all system object created
     * @throws Exception
     */
    public void initial()throws Exception {
        //initial Object extends XMLDoObject
        doInitial();
        //执行XMLDoObject配置的特殊属性,init="{}",worktime etc.
        exeProperty(initPropertyMap,getXML(),getSystempar(),getInput(getXML().getProperties()),getOutput(getXML().getProperties()),getConifg(getXML()),Boolean.TRUE);
        //执行孩子中带有auto="init"属性的任务
        autoInitChildren(getEmptyParameter(),getXML(),"init");

    }
    public void destroy() throws Exception {
        //shutdown worktime when destroy
        if(initPropertyMap.containsKey("worktime")){
            IObjectInitProperty init = (IObjectInitProperty)initPropertyMap.get("worktime");
            init.destroy(getXML());
        }
        /*if(invokePropertyMap.containsKey("worktime")){

        }*/
    }

    //自动执行带before属性的子对象
    public void autoInitChildren(XMLParameter parameter,XMLMakeup xml,String autotype) throws Exception {
        if(null != xml && null != xml.getChildren() && xml.getChildren().size()>0){
            List<XMLMakeup> children = xml.getChildren();
            if(null != children){
                String msg = null;
                for(XMLMakeup x:children){
                    if(x.getProperties().containsKey("auto")){
                        if(null != autotype && autotype.equals(x.getProperties().getProperty("auto"))){
                            msg = x.getProperties().getProperty("msg");
                            if(null != msg){
                                if(msg.startsWith("{")){
                                    Object o = parameter.getExpressValueFromMap(msg,this);
                                    Date d = new Date();
                                    log.debug("["+Thread.currentThread().getName()+"] ["+d +" "+d.getTime()+"] ["+xml.getId()+" "+"] "+o);
                                }else{
                                    Date d = new Date();
                                    log.debug("["+Thread.currentThread().getName()+"] ["+d +" "+d.getTime()+"] ["+xml.getId()+"] "+" "+msg);
                                }
                            }
                            String id=null;
                            if(StringUtils.isNotBlank(x.getProperties().getProperty("action"))){
                                id=x.getProperties().getProperty("action");
                            }else {
                                //Object o  = getPropertyObject(x);
                                id=x.getId();
                            }
                            Object o= getObjectById(id);
                            if(null != o && o instanceof XMLDoObject){
                                //addSystemLoadInitAction(o,"doThing",new Class[]{XMLParameter.class,XMLMakeup.class},new Object[]{parameter,x});
                                ((XMLDoObject) o).doThing(parameter,x);
                            }
                        }
                    }
                }
            }
        }
    }

    public void setEnv(XMLParameter data)throws Exception{
        Object ed = getEnvData();
        data.addParameter("${env}",ed);
        XMLMakeup x = getXML();
        if(null != x && null != x.getRoot()) {
            String inid = x.getRoot().getFirstCurChildText("instanceid");
            if(StringUtils.isNotBlank(inid)) {
                data.addGlobalParameter("${instanceid}", inid);
            }
        }
    }

    public Map getEnvData()throws Exception{
        XMLDoObject env = (XMLDoObject)getPropertyObject("env");
        if(null != env) {
            Object o = env.doSomeThing(null, null, null, null, null);
            return (Map) o;
        }else{
            return null;
        }
    }

    protected boolean checkInputParameterByDesc(XMLParameter env,Map inputData) throws ISPException, IOException {
        if (null != getDescStructure() && null != getDescStructure().get("input") && getDescStructure().get("input") instanceof Map) {
            String id = (String)inputData.get("srvId");
            Map dc=null;
            if(StringUtils.isNotBlank(id) && null != getDescStructure(id) && null !=getDescStructure(id).get("input") &&  getDescStructure(id).get("input") instanceof Map){
                dc = (Map)getDescStructure(id).get("input");
            }
            if(dc ==null && null != getDescStructure().get("input") && getDescStructure().get("input") instanceof Map) {
                dc = (Map) getDescStructure().get("input");
            }
            if(null != dc){
                if(log.isDebugEnabled()){
                    log.debug("check input parameters:"+inputData);
                }
                return checkByDesc(env,dc,inputData);
            }
        }

        return true;
    }

    private boolean checkByDesc(XMLParameter env,Object parameterDesc,Object inputData)throws ISPException{
        if(null!=inputData && null != parameterDesc ){
            if(inputData.getClass().isArray() && (parameterDesc.getClass().isArray() || parameterDesc instanceof Collection)){
                for(Object o:(Object[])inputData){
                    boolean b = checkByDesc(env,parameterDesc.getClass().isArray()?((Object[])parameterDesc)[0]:((Collection)parameterDesc).iterator().next(),o);
                    if(!b) return b;
                }
            }else if(inputData instanceof Collection){
                Iterator it = ((Collection)inputData).iterator();
                while(it.hasNext()){
                    Object v1 = it.next();
                    boolean b = checkByDesc(env,parameterDesc.getClass().isArray()?((Object[])parameterDesc)[0]:((Collection)parameterDesc).iterator().next(),v1);
                    if(!b) return b;
                }
            }else if(inputData instanceof Map && parameterDesc instanceof Map) {
                //check multiRelation in map data
                boolean bb = Desc.checkItemByDesc(null,env,this,inputData,(Map)parameterDesc);
                if(!bb) return bb;

                Iterator its = ((Map)parameterDesc).keySet().iterator();
                while (its.hasNext()) {
                    Object k = its.next();
                    Object v = ((Map)inputData).get(k);
                    Object dv = ((Map)parameterDesc).get(k);
                    if (null != dv ) {
                        if (dv instanceof Map && Desc.isDescriptionField((Map) dv)) {
                            //check
                            boolean b= Desc.checkItemByDesc(k.toString(),env,this,v,(Map)dv);
                            if(!b) return b;
                        }
                        if (null != v && v.getClass().isArray()) {
                            for(Object o:(Object[])v){
                                boolean b = checkByDesc(env,dv.getClass().isArray()?((Object[])dv)[0]:((Collection)dv).iterator().next(),o);
                                if(!b) return b;
                            }
                        } else if (null != v && v instanceof Collection && ((dv.getClass().isArray() && ((Object[])dv).length>0) || (dv instanceof Collection && ((Collection)dv).size()>0))) {
                            Iterator it = ((Collection)v).iterator();
                            while(it.hasNext()){
                                Object v1 = it.next();
                                boolean b = checkByDesc(env,dv.getClass().isArray()?((Object[])dv)[0]:((Collection)dv).iterator().next(),v1);
                                if(!b) return b;
                            }
                        } else if (null != v && v instanceof Map && dv instanceof Map) {
                            checkByDesc(env,(Map) dv, (Map) v);
                        }
                    }
                }
            }else if(parameterDesc instanceof Map && Desc.isDescriptionField((Map) parameterDesc)){
                boolean b= Desc.checkItemByDesc(null,env,this,inputData,(Map)parameterDesc);
                if(!b) return b;
            }
        }
        return true;
    }



    public void notifyObject(String op,Object obj)throws Exception{
        Map input=null;
        if(null != obj && obj instanceof Map){
            ((Map)obj).put("op",op);
            input = (Map)obj;
        }else {
            input = new HashMap();
            input.put("op", op);
            input.put("data", obj);
        }

        XMLParameter data = new XMLParameter();
        setEnv(data);
        data.put("^${input}",input);
        if(log.isDebugEnabled()) {
            log.debug("notifyObject :" + this.getClass().getName() + "  " + this.getXML().getId() + " in:" + input);
        }
        doThing(data, getXML());
    }
    void setAsynProperty(XMLMakeup xml){
        if(null != xml){
            for(int i=0;i<asynList.size();i++){
                if(xml.getProperties().containsKey(asynList.get(i))){
                    xml.getProperties().put("isAsyn",true);
                    break;
                }
            }
            if(xml.getChildren().size()>0){
                for(XMLMakeup x:xml.getChildren()){
                    setAsynProperty(x);
                }
            }
        }

    }

    void setExeProperty(XMLMakeup x){
        if(null != x){
            Iterator<String> its = invokePropertyMap.keySet().iterator();
            while(its.hasNext()){
                if(x.getProperties().containsKey(its.next())){
                    x.getProperties().put("isExe",true);
                    break;
                }
            }
            its =initPropertyMap.keySet().iterator();
            while(its.hasNext()){
                if(x.getProperties().containsKey(its.next())){
                    x.getProperties().put("isExe",true);
                    break;
                }
            }
            if(x.getChildren().size()>0){
                for(XMLMakeup xml:x.getChildren()){
                    setExeProperty(xml);
                }
            }
        }


    }

    //缓存属性值JSONObject表达式
    static Map<String,Map> propertyDefValueCache= new HashMap<String, Map>();
    //执行属性
    public Object[] exeProperty(Map propertyMap,XMLMakeup xml,XMLParameter parameter,Map input,Map output,Map config,Boolean isconvert) throws Exception {
        Iterator ks = propertyMap.keySet().iterator();

        if(null == xml)xml = getXML();
        boolean b=false;
        //类初始化进来的参数需要根据ENV转化
        if(isconvert && null != parameter){
            if(null != input)
                input=parameter.getMapValueFromParameter(input,this);
            if(null != output)
                output=parameter.getMapValueFromParameter(output,this);
            if(null != config)
                config=parameter.getMapValueFromParameter(config,this);
        }
        List ret = new LinkedList();
        while(ks.hasNext()){
            String k = (String)ks.next();
            if(null != xml && xml.getProperties().containsKey(k) && StringUtils.isNotBlank(xml.getProperties().getProperty(k))){
                try{
                    //执行前检查
                    if(!isActive()) {
                        log.error(getXML().getId()+" is not active , can not execute properties");
                        return new Object[]{true,null};
                    }
                    if(!checkParameters(xml.getId(),parameter,input,output,config)){
                        return new Object[]{true,null};
                    }
                    if(!propertyDefValueCache.containsKey(xml.getProperties().getProperty(k))){
                        propertyDefValueCache.put(k,StringUtils.convert2MapJSONObject(xml.getProperties().getProperty(k)));
                    }
                    Map proMap = propertyDefValueCache.get(k);
                    if(null != parameter)
                        proMap=parameter.getMapValueFromParameter(proMap,this);
                    Object o = ((IExeProperty)propertyMap.get(k)).exeProperty(proMap, this,xml, parameter, input, output,config);
                    ret.add(o);
                    if(!(null != o && o.equals(NOT_EXE_PROPERTY)) && !b){
                        b=true;
                    }
                }catch (Exception e){
                    //log.error("exeProperty error",e);
                    throw e;
                    //log.error("defined property["+k+"]'s value["+xml.getProperty(k)+"] must jsonObject String.",e);
                }
            }
        }
        if(!b){
            //异步执行 注释掉：不是初始化实例时就异步执行，异步应该是作为执行器的一个属性
           /*if(isAsyn(xml)){
               asynProperty.exeProperty(null, this,xml, parameter, input, output,config);
               b=true;
           }*/
        }
        return new Object[]{b,ret};
    }

    //自动执行带before属性的子对象
    public void doChildren(XMLParameter parameter,XMLMakeup xml,String autotype,Map config) throws Exception {
        if(null == parameter)throw new Exception("XMLParameter can not be null");
        if(null != config && StringUtils.isNotBlank(config.get("auto")) && config.get("auto") instanceof String && !StringUtils.isTrue((String)config.get("auto"))){
            return;
        }

        if(null != xml && null != xml.getChildren() && xml.getChildren().size()>0){
            List<XMLMakeup> children = xml.getChildren();
            if(null != children){
                String msg = null;
                for(XMLMakeup x:children){
                    if(x.getProperties().containsKey("auto")){
                        if(null != autotype && autotype.equals(x.getProperties().getProperty("auto"))){
                            msg = x.getProperties().getProperty("msg");
                            if(null != msg){
                                if(msg.startsWith("{")){
                                    Object o = parameter.getExpressValueFromMap(msg,this);
                                    Date d = new Date();
                                    log.debug("["+Thread.currentThread().getName()+"] ["+d +" "+d.getTime()+"] ["+xml.getId()+" "+"] "+o);
                                }else{
                                    Date d = new Date();
                                    log.debug("["+Thread.currentThread().getName()+"] ["+d +" "+d.getTime()+"] ["+xml.getId()+"] "+" "+msg);
                                }
                            }
                            //Object o  = getPropertyObject(x);
                            Object o = getObjectById(x.getId());
                            if(Logger.isDebugEnabled()){
                                Logger.debug(this.getClass(),parameter,xml.getId(),"do "+xml.getId()+" before children "+x.getId(),(Map)parameter.get("${this_input}"),null);
                            }
                            if(null != o && o instanceof XMLDoObject){
                                ((XMLDoObject)o).doThing(parameter,x);
                            }
                        }
                    }
                }
            }
        }
    }
    //优先调用配置
    String getPropserty(Properties xml,String propertyKey){
        if(null != xml && StringUtils.isNotBlank(xml.getProperty(propertyKey))){
            return xml.getProperty(propertyKey);
        }
        if((null == xml || StringUtils.isBlank(xml.getProperty(propertyKey))) && StringUtils.isNotBlank(getXML().getProperties().getProperty(propertyKey))){
            return  getXML().getProperties().getProperty(propertyKey);
        }
        return null;
    }
    //调用参数配置缓存
    Map getConifg(XMLMakeup xml){
        Map map =null;
        String config = getPropserty(xml.getProperties(),"config");
        if(StringUtils.isNotBlank(config)){
            map= StringUtils.convert2MapJSONObject(config);

        }
        if(null == map) map=new HashMap();
        appendConfig(map,xml);
        return map;
    }
    Map getOutput(Properties properties){
        String jsonpar = getPropserty(properties,"output");
        if(StringUtils.isNotBlank(jsonpar)){
            Map m = StringUtils.convert2MapJSONObject(jsonpar);
            return m;
        }
        return null;
    }
    Map getInput(Properties properties){
        String jsonpar = getPropserty(properties,"input");
        if(StringUtils.isNotBlank(jsonpar)){
            Map m = StringUtils.convert2MapJSONObject(jsonpar);
            return m;
        }
        return null;
    }

    /*Map getParameter(String parString,String orgString){
        if(StringUtils.isBlank(parString) && StringUtils.isBlank(orgString))
            return null;
        if(StringUtils.isBlank(parString) && StringUtils.isNotBlank(orgString))
            return StringUtils.convert2MapJSONObject(orgString);
        if(StringUtils.isNotBlank(parString) && StringUtils.isBlank(orgString))
            return StringUtils.convert2MapJSONObject(parString);
        Map parMap = StringUtils.convert2MapJSONObject(parString);
        Map orgMap = StringUtils.convert2MapJSONObject(orgString);
        ObjectUtils.appendAllMap(orgMap,parMap);//把orgMap追加到parMap,如果有重名的不覆盖
        return parMap;
    }*/

    /*//请求的如参和定义的如参合并
    public void doThing(XMLParameter parameter,XMLMakeup xml)throws Exception{
        if(null != xml && xml.getProperties().containsKey("isenable") && !StringUtils.isTrue(xml.getProperties().getProperty("isenable")))
            return;
        Map parInput,parOutput,parConfig;
        parInput = getParameter(xml==null?null:xml.getProperties().getProperty("input"),getXML().getProperties().getProperty("input"));
        parOutput = getParameter(xml==null?null:xml.getProperties().getProperty("output"),getXML().getProperties().getProperty("output"));
        parConfig = getParameter(xml==null?null:xml.getProperties().getProperty("config"),getXML().getProperties().getProperty("config"));

        String xmlid=(String)parameter.getParameter("xmlid");
        if(null != xml && StringUtils.isBlank(xmlid)){
            xmlid=xml.getId();
        }
        if(StringUtils.isBlank(xmlid)){
            xmlid = getXML().getId();
        }
        Logger.debug(parameter,xmlid,"thing begin",null);

        Map input=null,output=null,config=null;
        //从环境中获取可能的上级传过来的input
        Object io = parameter.getParameter("^${input}");
        if(null !=io && io instanceof Map) {
            input = parameter.getMapValueFromParameter((Map) io);
            Object o = parameter.getParameter("^${input#"+xml.getId()+"}");
            if(null!= o && o instanceof Map){
                if(input ==null){
                    input=(Map)o;
                }else{
                    input.putAll((Map)o);
                }
                //保持上层特有的属性，不转给孩子
                if(input.containsKey("alarm")){
                    input.remove("alarm");
                }
            }
        }
        if(null != parameter.getTargetNames() && xmlid.equals(parameter.getTargetNames()[0])){
            Object inpar = parameter.getInputParameter();
            if(null != inpar && inpar instanceof Map){
                if(input==null){
                    input = new HashMap();
                }
                input.putAll((Map)inpar);

            }
        }
        //debug point
        if(StringUtils.isNotBlank(xml.getProperties().getProperty("debug"))){
            Map map = StringUtils.convert2MapJSONObject(xml.getProperties().getProperty("debug"));
            if(null != map){
                Object o = parameter.getExpressValueFromMap((String)map.get("stoppoint"));
                if(null != o){
                    if(StringUtils.isTrue(o.toString())){
                        //System.out.println();
                    }
                }
            }
        }

        //如果input值是一个变量，变量值是个Map则不再进行转换
        if(null!=xml.getProperties().getProperty("input") && xml.getProperties().getProperty("input").startsWith("${")){
            Object o  = ObjectUtils.getValueByPath(parameter,xml.getProperties().getProperty("input"));
            if(o instanceof Map) {
                if(null == input)
                    input=(Map)o;
                else {
                    //configuration maybe more deal with data so , configuration data is hight than paramter data
                    input.putAll((Map)o);
                }
            }
        }

        //从属性中获取input参数
        if(null != parInput){
            String convertType = null;
            if(parInput.containsKey("convertType")) {
                convertType=(String)parInput.get("convertType");
            }
            //System.out.println("str2Map "+new Date().getTime()+" "+(System.currentTimeMillis()-l));
            //performance is very good . long l = System.currentTimeMillis();
            Map jsonObject =null;
            if(null == convertType) {//默认全部类型转换
                jsonObject = parameter.getMapValueFromParameter(parInput);
            }else if("var".equals(convertType)){//只做() range转换，主要用于获取变量值，其他不做转换
                jsonObject = parameter.getMapValueFromParameter(jsonObject,new String[]{"getvar("});
            }else{
                jsonObject = parameter.getMapValueFromParameter(jsonObject);
            }
            //System.out.println(Thread.currentThread().getName()+"MapValue "+new Date().getTime()+" "+(System.currentTimeMillis()-l));
            if(null == input)
                input=jsonObject;
            else {
                //configuration maybe more deal with data so , configuration data is hight than paramter data
                input.putAll(jsonObject);
            }
        }
        //如果input参数没有，尝试从说明文档中获取
        //if(null == input){
        //    Map m = getInvokeStruct();
        //    if(null !=m){
        //        input = (Map)m.get("input");
        //        input = parameter.getMapValueFromParameter(input);
        //    }
        //}

        //从参数中构造json配置参数
        if(!parameter.containsParameter("^${output}")){
            if(null != parOutput){
                //long l = System.currentTimeMillis();
                output = parOutput;//getOutput(properties);
                //System.out.println("output Map2Value "+new Date().getTime()+" "+(System.currentTimeMillis()-l));
            }
        }else{
            output=  (Map)parameter.getParameter("^${output}");
        }
        //如果output为空，尝试从说明文档中加载
        //if(null == output){
        //    Map m = getInvokeStruct();
        //    if(null !=m){
        //        output = (Map)m.get("output");
        //        output = parameter.getMapValueFromParameter(output);
        //    }
        //}
        if(null != xml && parameter.containsKey("^${config#"+xmlid+"}")){
            config = (Map)parameter.getParameter("^${config#"+xmlid+"}");
        }
        if(!parameter.containsParameter("^${config}")){
            //long l = System.currentTimeMillis();
            //优先取调用时的配置
            if(null != parConfig){
                config=parConfig;
            }
            if(null != config)
                config=parameter.getMapValueFromParameter(config);
            //System.out.println("config "+new Date().getTime()+" "+(System.currentTimeMillis()-l));
        }else{
            config = (Map)parameter.getParameter("^${config}");
        }
        //如果config为空，尝试从说明文档中加载
        //if(null == config){
        //    Map m = getInvokeStruct();
        //    if(null !=m){
        //        config = (Map)m.get("config");
        //        config = parameter.getMapValueFromParameter(config);
        //    }
        //}
        if(null == config) config=new HashMap();
        appendConfig(config,xml);

        //如果有执行属性，调用执行属性
        Object[] rs = exeProperty(invokePropertyMap,xml,parameter,input,output,config,false);
        //System.out.println("1 doO:"+new Date().getTime());
        if(!(Boolean)rs[0]){
            if(!isAsyn(xml)){
                doCheckThing(xmlid,parameter,input,output,config,getXML());
            }else{
                if(null != XMLParameter.XMLLOGIC_BACK_CALL_KEY) {
                    ExecutorUtils.work(new DoAction(this, parameter, input, output, config, (Object[]) parameter.getParameter(XMLParameter.XMLLOGIC_BACK_CALL_KEY), xml), "doAction", null, null);
                    parameter.removeParameter(XMLParameter.XMLLOGIC_BACK_CALL_KEY);
                }else {
                    ExecutorUtils.work(this, "doCheckThing", new Class[]{String.class, XMLParameter.class, Map.class, Map.class, Map.class, XMLMakeup.class}, new Object[]{xmlid, parameter, input, output, config, xml});
                }
            }

        }
        //if(log.isDebugEnabled()) {
        //    parameter.printTime("do thing end " + (null == xml ? getXML().getId() : xml.getId()));
        //}
        Logger.debug(parameter,xml.getId(),"Thing end",null);

    }*/

    Map getValueMap(Map referInputMap,XMLParameter parameter,Properties pro) throws Exception {
        Map ret=null;
        String convertType=null;
        if(pro.containsKey("convertType")) {
            convertType=pro.getProperty("convertType");
        }
        if(StringUtils.isBlank(convertType)){
            ret = parameter.getMapValueFromParameter(referInputMap,this);
        }else if("var".equals(convertType)){
            ret = parameter.getMapValueFromParameter(referInputMap,new String[]{"getvar("},this);
        }else{
            ret = parameter.getMapValueFromParameter(referInputMap,this);
        }
        return ret;
    }

    Map definedInputMap = null;//该服务定义时配置的入参,其随着服务的变化而变化,正常运行中不发生变化,所以第一次使用时初始化.
    Map definedOutMap = null;//该服务定义时配置的入参,其随着服务的变化而变化,正常运行中不发生变化,所以第一次使用时初始化.
    Map definedConfigMap = null;//该服务定义时配置的入参,其随着服务的变化而变化,正常运行中不发生变化,所以第一次使用时初始化.
    public void doThing(XMLParameter parameter,XMLMakeup xml) throws Exception {
        //if before interrupt point for redo flow sr , do nothing
        if(null != xml && !xml.isEnable())
            return;
        if(log.isDebugEnabled()){
            log.debug(xml.toString());
        }
        //从参数中构造mapping参数
        Map input=null,output=null,config=null;
        String xmlid=null;

        xmlid = (String)parameter.getParameter("xmlid");
        String debug=null;
        boolean isAsyn=false;
        if(null !=xml) {
            if(StringUtils.isBlank(xmlid))
                xmlid = xml.getId();
            debug=xml.getProperties().getProperty("debug");
            isAsyn = isAsyn(xml);
        }else{
            if(StringUtils.isBlank(xmlid))
                xmlid=getXML().getId();
            debug=getXML().getProperties().getProperty("debug");
            isAsyn = isAsyn(getXML());
        }


        //debug模式时，可以端点
        if (StringUtils.isNotBlank(debug)) {
            Map map = StringUtils.convert2MapJSONObject(debug);
            if (null != map) {
                Object o = parameter.getExpressValueFromMap((String) map.get("stoppoint"),this);
                if (null != o) {
                    if (StringUtils.isTrue(o.toString())) {
                        System.out.println("it is debug point");
                    }
                }
            }
        }


        /*boolean ispar=true;
        if(xml==null) {
            xml = getXML();
            ispar=false;
        }*/

        /*Properties properties = null;
        if(null != xml) properties=xml.getProperties();
*/
        //if(xmlid.equals("stressTest")){
        //    System.out.println();
        //}
        Object ret=null;
        Map envinput=null;
        //从环境中获取可能的上级传过来的input
        //cell传入的参数
        Object io = parameter.getParameter("^${input}");
        if (null != io && io instanceof Map) {
            envinput = parameter.getMapValueFromParameter((Map) io, this);
        }
        //是xmllogic传递的参数
        Object logicPar=null;
        if(null != xml) {
            logicPar = parameter.getParameter("^${input#" + xml.getId() + "}");
        }
        if (null != logicPar && logicPar instanceof Map) {
            if (envinput == null) {
                envinput = (Map) logicPar;
            } else {
                //o中参数优先级高，需要覆盖input中相同的参数
                if(!(null != parameter && null !=parameter.getInputParameter() && System.identityHashCode(envinput)==System.identityHashCode(parameter.getInputParameter()))) {
                    ObjectUtils.appendDeepMapReplaceKey((Map) logicPar, envinput);
                }
                //input.putAll((Map)o);
            }
            //保持上层特有的属性，不转给孩子
            if (envinput.containsKey("alarm")) {
                envinput.remove("alarm");
            }
        }

        Object pi = parameter.get("${this_input}");
        //log.debug("3${data}"+parameter.get("${data}")+" this input"+pi);
        if (null != pi && pi instanceof Map) {
            if(null == envinput) {
                envinput = (Map) pi;
            }else {
                ObjectUtils.appendDeepMapNotReplaceKey((Map) pi, envinput);
            }
        }
        //log.debug("4${data}"+parameter.get("${data}"));
    /*if(null != parameter.getTargetNames() && xmlid.equals(parameter.getTargetNames()[0])){
        Object inpar = parameter.getInputParameter();
        if(null != inpar && inpar instanceof Map){
            if(input==null){
                input = new HashMap();
            }
            input.putAll((Map)inpar);

        }
    }*/

        if(null == definedInputMap){
            String definedInput = getXML().getProperties().getProperty("input");
            if (StringUtils.isNotBlank(definedInput)) {
                definedInputMap = StringUtils.convert2MapJSONObject((String) definedInput);
            }
        }
        Map definedInput=null;
        if(null != definedInputMap){
            definedInput = getValueMap(definedInputMap, parameter, getXML().getProperties());
        }


        Map referInputMap = null;//引用参数
        if(null != xml){
            //获取引用调用的参数
            String parmap = xml.getProperties().getProperty("input");
            if(log.isDebugEnabled()){
                log.debug("referXml input:"+parmap);
            }
            if (StringUtils.isNotBlank(parmap)) {
                if (parmap.startsWith("${")) {
                    Object o = ObjectUtils.getValueByPath(parameter, parmap);
                    if (o instanceof Map) {
                        referInputMap = (Map) o;
                    }
                } else {
                    referInputMap = StringUtils.convert2MapJSONObject(parmap);
                    if(Logger.isDebugEnabled()){
                        Logger.debug(this.getClass(), parameter, getXML().getId()+"-"+xmlid, "input map",referInputMap, null);
                        log.debug("${data}"+parameter.get("${data}"));
                    }
                    referInputMap = getValueMap(referInputMap, parameter, xml.getProperties());
                    if(Logger.isDebugEnabled()){
                        Logger.debug(this.getClass(), parameter, getXML().getId()+"-"+xmlid, "input value map",referInputMap, null);
                    }
                }
            }
        }
        //合并入参，引用入参高于原定义入参，原定义入参高于环境入参
        /*当引用入参为定义,使用环境入参; 对于xmllogic do 有时还是希望通过父节点转入相同的参数的。
        if(null != referInputMap){
            input=referInputMap;
            if(null != definedInput){
                ObjectUtils.appendDeepMapNotReplaceKey(definedInput, input);
            }
        }else if(null != definedInput){
            input = definedInput;
        }
        */
        /*if (null != envinput) {
                if (null != definedInput)
                    ObjectUtils.appendDeepMapReplaceKey(definedInput, input);
                if (null != referInputMap)
                    ObjectUtils.appendDeepMapReplaceKey(referInputMap, input);
            } else {
                input = referInputMap;
                if (null != input && null != definedInput) {
                    ObjectUtils.appendDeepMapNotReplaceKey(definedInput, input);
                } else if (null == input && null != definedInput) {
                    input = definedInput;
                }
            }*/


        //环境传进来的参数有可能是${input_data}对象，不能对其作修改。原始的参数只实例化一次，也不能修改。应用的参数是每次生成的，可以修改。refer 也可能是input_data 通过input=${input_data}指定
        if(null != envinput && (null == parameter
                || null == parameter.getInputParameter()
                ||(null != parameter && null !=parameter.getInputParameter() && System.identityHashCode(envinput)!=System.identityHashCode(parameter.getInputParameter())))){
            if(null != referInputMap &&
                    (null != parameter && null !=parameter.getInputParameter() && System.identityHashCode(referInputMap)==System.identityHashCode(parameter.getInputParameter()))){
                if(null != definedInput){//有些服务暴露给外面的是所有参数的一部分
                    input = new LinkedHashMap();
                    ObjectUtils.appendDeepMapNotReplaceKey(referInputMap, input);
                    ObjectUtils.appendDeepMapNotReplaceKey(definedInput, input);
                }else {
                    input = referInputMap;
                }
            }else if(null != envinput && null != referInputMap && null != definedInput){
                ObjectUtils.appendDeepMapNotReplaceKey(definedInput, referInputMap);
                ObjectUtils.appendDeepMapNotReplaceKey(referInputMap, envinput);
                input = envinput;
            }else if(null != referInputMap && null != definedInput){
                ObjectUtils.appendDeepMapNotReplaceKey(definedInput, referInputMap);
                ObjectUtils.appendDeepMapNotReplaceKey(envinput, referInputMap);
                input = referInputMap;
            }else if(null == referInputMap && null != definedInput){
                ObjectUtils.appendDeepMapNotReplaceKey(definedInput, envinput);
                input = envinput;
            }else if(null != referInputMap && null == definedInput){
                ObjectUtils.appendDeepMapNotReplaceKey(envinput,referInputMap );
                input = referInputMap;
            }else{
                input = envinput;
            }
        }else {
            if(null != referInputMap &&
                    (null != parameter && null !=parameter.getInputParameter() && System.identityHashCode(referInputMap)==System.identityHashCode(parameter.getInputParameter()))){
                if(null != definedInput){//有些服务暴露给外面的是所有参数的一部分
                    input = new LinkedHashMap();
                    ObjectUtils.appendDeepMapNotReplaceKey(referInputMap, input);
                    ObjectUtils.appendDeepMapNotReplaceKey(definedInput, input);
                }else {
                    input = referInputMap;
                }
            }else if(null != referInputMap && null != definedInput){
                ObjectUtils.appendDeepMapNotReplaceKey(definedInput, referInputMap);
                input = referInputMap;
            }else if(null == referInputMap && null != definedInput){
                input = definedInput;
            }else if(null != referInputMap && null == definedInput){
                input = referInputMap;
            }else{
                input =envinput;
            }

        }
        if(log.isDebugEnabled()){
            log.debug(xmlid+" define input:"+definedInput);
            log.debug(xmlid+" final input:"+input);
        }

        //从属性中获取input参数
        /*if (properties.containsKey("input")) {
            //获取服务定义时的参数

            if (ispar) {
                String definedInput = getXML().getProperties().getProperty("input");
                if (StringUtils.isNotBlank(definedInput)) {
                    definedInputMap = StringUtils.convert2MapJSONObject((String) definedInput);
                    definedInputMap = getValueMap(definedInputMap, parameter, getXML().getProperties());
                }
            }
            //获取引用调用的参数
            String parmap = properties.getProperty("input");
            Map referInputMap = null;
            if (StringUtils.isNotBlank(parmap)) {
                if (parmap.startsWith("${")) {
                    Object o = ObjectUtils.getValueByPath(parameter, parmap);
                    if (o instanceof Map) {
                        referInputMap = (Map) o;
                    }
                } else {
                    referInputMap = StringUtils.convert2MapJSONObject(parmap);
                    referInputMap = getValueMap(referInputMap, parameter, properties);
                }
            }
            if (null != input) {
                ObjectUtils.appendDeepMapReplaceKey(definedInputMap, input);
                ObjectUtils.appendDeepMapReplaceKey(referInputMap, input);
            }
            if (null == input) {
                input = referInputMap;
                ObjectUtils.appendDeepMapReplaceKey(definedInputMap, input);
            }
            if (null == input) {
                input = definedInputMap;
            }
        }*/
        //如果input参数没有，尝试从说明文档中获取
        //if(null == input){
        //    Map m = getInvokeStruct();
        //    if(null !=m){
        //        input = (Map)m.get("input");
        //        input = parameter.getMapValueFromParameter(input);
        //    }
        //}
        if(Logger.isInfoEnabled()) {
            Logger.info(this.getClass(), parameter, (null != xml?xml.getId():getXML().getId()), "begin doThing",input, null);
        }
        //从参数中构造json配置参数
        if (!parameter.containsParameter("^${output}")) {
            if (null != xml && xml.getProperties().containsKey("output")) {
                //long l = System.currentTimeMillis();
                output = getOutput(xml.getProperties());
                //System.out.println("output Map2Value "+new Date().getTime()+" "+(System.currentTimeMillis()-l));
            }
            if(null == output){
                if(null == definedOutMap){
                    definedOutMap= getOutput(getXML().getProperties());
                }
                output=definedOutMap;
            }
        } else {
            output = (Map) parameter.getParameter("^${output}");
        }
        //如果output为空，尝试从说明文档中加载
        //if(null == output){
        //    Map m = getInvokeStruct();
        //    if(null !=m){
        //        output = (Map)m.get("output");
        //        output = parameter.getMapValueFromParameter(output);
        //    }
        //}
        Map envCOnfig=null,referConfig=null,orgConfig=null;
        if (null != xml && parameter.containsKey("^${config#" + xml.getId() + "}")) {
            envCOnfig = (Map) parameter.getParameter("^${config#" + xml.getId() + "}");
        }
        if (!parameter.containsParameter("^${config}")){
            //long l = System.currentTimeMillis();
            //优先取调用时的配置
            if (null!=xml && xml.getProperties().containsKey("config")) {
                referConfig = StringUtils.convert2MapJSONObject(xml.getProperties().getProperty("config"));
                referConfig = parameter.getMapValueFromParameter(referConfig, this);
            }
            if(getXML().getProperties().containsKey("config")){
                if(null==definedConfigMap){
                    definedConfigMap=StringUtils.convert2MapJSONObject(getXML().getProperties().getProperty("config"));
                    definedConfigMap=parameter.getMapValueFromParameter(definedConfigMap, this);
                }
                //如果没有取定义时的配置
                //config = definedConfigMap;
            }
            //System.out.println("config "+new Date().getTime()+" "+(System.currentTimeMillis()-l));
        } else {
            orgConfig = (Map) parameter.getParameter("^${config}");
        }
        config= new HashMap();
        if(null != envCOnfig){
            ObjectUtils.appendDeepMapNotReplaceKey(envCOnfig,config);
        }
        if(null != referConfig){
            ObjectUtils.appendDeepMapNotReplaceKey(referConfig,config);
        }
        if(null != orgConfig){
            ObjectUtils.appendDeepMapNotReplaceKey(orgConfig,config);
        }
        if(null != definedConfigMap){
            ObjectUtils.appendDeepMapNotReplaceKey(definedConfigMap,config);
        }
        //如果config为空，尝试从说明文档中加载
        //if(null == config){
        //    Map m = getInvokeStruct();
        //    if(null !=m){
        //        config = (Map)m.get("config");
        //        config = parameter.getMapValueFromParameter(config);
        //    }
        //}
        if (null == config) config = new HashMap();
        appendConfig(config, xml==null?getXML():xml);


        if (Logger.isDebugEnabled()) {
            Logger.debug(this.getClass(), parameter, getXML().getId()+"-"+xmlid, "end parse parameters",input, null);
        }

        //只是做入参检查不做业务
        if(parameter.isOnlyInputCheck()){
            boolean b = checkInput(xmlid, parameter, input, output, config);
            parameter.setResult(b);
            return ;
        }

       //如果有执行属性，调用执行属性
        Object[] rs = exeProperty(invokePropertyMap,xml,parameter,input,output,config,false);
        //System.out.println("1 doO:"+new Date().getTime());
        if(!(Boolean)rs[0]){
            if(!isAsyn){
                //timeout 只针对同步执行的情况,如果是重做请求，不设置超时限制
                //如果前面逻辑没有设置重做标志,该服务配置为重做(避开xmlLoic中的重做逻辑，即不是从xmllogic来的重做配置,xmllogic中的重做逻辑会包含一组do的操作在一个时间限制内),如果有超时时间,且重做的服务,保存的数据库客户端对象都存在时,超时调用
                if(!parameter.isRedoService() && isRedo()
                        //&& StringUtils.isNumeric(getXML().getProperties().getProperty("redo"))
                        //&& Integer.parseInt(getXML().getProperties().getProperty("redo"))>0
                        && null != parameter.getParameter("${env}") && StringUtils.isNotBlank(((Map)parameter.getParameter("${env}")).get("saveRedoService"))
                        && null != getObjectById((String)((Map)parameter.getParameter("${env}")).get("saveRedoService"))
                        && null != getObjectById("redo")
                        ){
                    int cc = maxTimeoutInvokeCount;//持续超时的上限次数
                    String msg = null;
                    int timeout = 0;
                    int duringTime=preJudgeTimeoutIntervalMinutes;
                    Map redocfg = getRedoCfg();
                    if(null != redocfg){
                        if(StringUtils.isNotBlank(redocfg.get("count"))){//连续超时次数
                            cc = Integer.valueOf((String)redocfg.get("count"));
                        }
                        if(StringUtils.isNotBlank(redocfg.get("message"))){//超时，或等待时的提示信息
                            msg = (String)redocfg.get("message");
                        }
                        if(StringUtils.isNotBlank(redocfg.get("overTime"))){ //超时时间
                            timeout = Integer.parseInt((String)redocfg.get("overTime"));
                        }
                        if(StringUtils.isNotBlank(redocfg.get("duringTime"))){ //等待重试时间
                            duringTime = Integer.parseInt((String)redocfg.get("duringTime"));
                        }
                    }
                    if(timeout==0){
                        if(StringUtils.isNumeric(getXML().getProperties().getProperty("redo")))
                            timeout = Integer.parseInt(getXML().getProperties().getProperty("redo"));
                        else if("true".equals(getXML().getProperties().getProperty("redo"))){
                            timeout = defaultTimeout;
                        }
                    }

                    try {
                        //如果连续超过100次请求都超时,以后5内的请求直接超时,记录redo.类型为,
                        String srv = parameter.getTargetNames()[0];
                        if(!preJudgeTimeout(parameter,srv,cc,xmlid,getXML(),msg,timeout)) {
                            com.octopus.utils.thread.ThreadPool.MyExecutor exe = ExecutorUtils.getMyExecutor();
                            parameter.addGlobalParameter("^{Timeout_BG_Thread_Name}", exe.getThreadName());
                            InvokeTask task = new InvokeTask(this, "doCheckThing", new Class[]{String.class, XMLParameter.class, Map.class, Map.class, Map.class, XMLMakeup.class}, new Object[]{xmlid, parameter, input, output, config, xml});
                            exe.execute(task, timeout);
                       /* ExecutorUtils.synWork(this, "doCheckThing",
                                new Class[]{String.class, XMLParameter.class, Map.class, Map.class, Map.class, XMLMakeup.class}, new Object[]{xmlid, parameter, input, output, config, xml}
                                , Integer.parseInt(getXML().getProperties().getProperty("redo")));*/
                            parameter.setResult(parameter.getThreadResult(exe.getThreadName()));

                            if(null != timeoutCountMap && null != timeoutCountMap.get(srv)) {
                                timeoutCountMap.remove(srv);
                            }
                            if(!task.isSuccess() && null != task.getException()){
                                parameter.setError(true);
                                parameter.setException(task.getException());
                                throw (Exception) task.getException();
                            }
                        }
                    }catch(Exception e){
                        //发生超时异常时,后端业务处理还没有结束,只能在该服务抛出超时异常,不知里面的业务处理情况,设置SuspendXmlId后，里面业务即使发生异常也不会记录redolog
                        //里面业务如果成功处理，去redolog删除该log，如果发生异常，更新node信息
                        if(e instanceof TimeoutException && !parameter.isSuspend()){
                            parameter.setSuspendXMlId(xmlid+","+getXML().getId());////这里使用的是别名和实际服务名称，方便主服务查找，重做
                            //parameter.setTimeoutXMlId(xmlid + "," + getXML().getId());////这里使用的是别名和实际服务名称，方便主服务查找，重做
                            parameter.setStatus(XMLParameter.HAPPEN_TIMEOUT);
                            XMLDoObject save = (XMLDoObject)getObjectById((String)((Map)parameter.getParameter("${env}")).get("saveRedoService"));
                            parameter.setException(e);
                            parameter.setError(true);
                            save.doThing(parameter, xml);
                            //目标服务和当前服务组成出现问题的路径
                            synchronized (parameter) {
                                String srv = parameter.getTargetNames()[0];
                                if(StringUtils.isNotBlank(srv)) {
                                    if (timeoutCountMap == null)
                                        timeoutCountMap = new ConcurrentHashMap<String, AtomicInteger>();
                                    if(null == timeoutCountMap.get(srv)){
                                        timeoutCountMap.put(srv,new AtomicInteger(0));
                                    }

                                    if(timeoutCountMap.get(srv).intValue()<=maxTimeoutInvokeCount) {
                                        if(timeoutCountMap.get(srv).intValue()==maxTimeoutInvokeCount) {
                                            ExecutorUtils.work(new ClearTimeoutCountRunnable(timeoutCountMap,srv),duringTime*60*1000);
                                        }
                                        timeoutCountMap.get(srv).addAndGet(1);
                                    }

                                }
                            }
                            Object o = getDescStructure().get("original");
                            if(StringUtils.isNotBlank(msg)){
                                throw new ISPException("408", (String)parameter.getExpressValueFromMap(msg,this));

                            }else {
                                throw new ISPException("408", "service " + parameter.getTargetNames()[0] + " execute timeout " + timeout + " s, System will automatic and reply result later. input data is:" + parameter.getInputParameter() + ". the original is:" + o, e);
                            }
                        }
                        throw e;
                    }
                }else {
                    doCheckThing(xmlid, parameter, input, output, config, xml);
                }
            }else{
                if(parameter.containsKey(XMLParameter.XMLLOGIC_BACK_CALL_KEY)) {
                    ExecutorUtils.work(new DoAction(this, parameter, input, output, config, (Object[]) parameter.getParameter(XMLParameter.XMLLOGIC_BACK_CALL_KEY), xml), "doAction", null, null);
                    parameter.removeParameter(XMLParameter.XMLLOGIC_BACK_CALL_KEY);
                }else {
                    ExecutorUtils.work(this, "doCheckThing", new Class[]{String.class, XMLParameter.class, Map.class, Map.class, Map.class, XMLMakeup.class}, new Object[]{xmlid, parameter, input, output, config, xml});
                }
            }

        }
        if(Logger.isInfoEnabled()) {
            Logger.info(this.getClass(), parameter, getXML().getId()+"-"+xmlid, "end doThing",input, null);
        }
    }



    /**
     * 预判断请求是否会超时，如果预判断超时，直接保存redo记录，返回异常。
     * @param parameter
     * @param xml
     */
    boolean preJudgeTimeout(XMLParameter parameter,String srv,int maxTimeoutInvokeCount,String xmlid,XMLMakeup xml,String message,int timeout)throws Exception{
        if(null != parameter){
            Map map = getEnvData();
            if (StringUtils.isTrue((String) map.get("isPreJudgeTimeout"))) {
                if(null != timeoutCountMap && null != timeoutCountMap.get(srv) && timeoutCountMap.get(srv).intValue()>maxTimeoutInvokeCount){//之前发生过Timeout异常
                    //String cachekey =  map.get("${ip}")+"_"+map.get("${local_instanceId}")+"_"+xml.getId();
                    //XMLObject o  = getObjectById("RedisClient");
                    parameter.setSuspendXMlId(xmlid+","+getXML().getId());////这里使用的是别名和实际服务名称，方便主服务查找，重做
                    parameter.setStatus(XMLParameter.HAPPEN_PREJUDGETIMEOUT);
                    XMLDoObject save = (XMLDoObject)getObjectById((String)((Map)parameter.getParameter("${env}")).get("saveRedoService"));
                    save.doThing(parameter, xml);
                    Object o = getDescStructure().get("original");
                    if(null != message){
                        throw new ISPException("407", (String)parameter.getExpressValueFromMap(message,this));
                    }else {
                        throw new ISPException("407", "service " + parameter.getTargetNames()[0] + " prejudage timeout " + timeout + " s, System will automatic and reply result later. input data is:" + parameter.getInputParameter() + ". the orginal is :" + o);
                    }
                }
            }
        }
        return false;
    }

    void appendConfig(Map config,XMLMakeup xml){
        if(null != xml && null!=config){
            Iterator its = xml.getProperties().keySet().iterator();
            while(its.hasNext()) {
                String k = (String)its.next();
                if(!config.containsKey(k)) {
                    config.put(k, xml.getProperties().getProperty(k));
                }
            }
            if(!config.containsKey("id")){
                config.put("id",xml.getId());
            }
        }
    }

    protected boolean checkStr(XMLParameter env,String exp)throws ISPException{
        Object o = env.getExpressValueFromMap(exp,this);
        return ObjectUtils.isTrue(o);
    }

    boolean check(XMLParameter env,Object exp,Map srcMap)throws Exception{
        if(null !=exp){
            try{
/*
                env.addParameter("${input}",input);
                env.addParameter("${config}",config);
*/
                if(exp instanceof String && StringUtils.isNotBlank(exp)){
                    if(XMLParameter.isHasRetainChars((String)exp)){
                         return StringUtils.isTrue(env.getExpressValueFromMap((String)exp,this).toString());
                    }
                    if(/*!checkStr(env,(String)exp)*/ !ObjectUtils.isTrue(srcMap.get("check"))){
                        if(log.isDebugEnabled())
                            log.debug(this.getXML().getId()+"input check["+exp+"] is false and not execute.");
                        srcMap.put("check",false);
                        return false;
                    }else{
                        srcMap.put("check",true);
                        return true;
                    }

                }else if(exp instanceof Map){
                    //String ex = (String)((Map)exp).get("check");
                    if(/*!checkStr(env,ex)*/!ObjectUtils.isTrue(((Map)srcMap.get("check")).get("check"))){
                        srcMap.put("check",false);
                        exp = env.getMapValueFromParameter((Map)exp,this);
                        Object error = ((Map)exp).get("error");
                        if(null != error){
                            if(error instanceof String){
                                if(log.isDebugEnabled())
                                    log.debug(error);
                                if(StringUtils.isNotBlank(error)){
                                    throw new Exception((String)error);
                                }else{
                                    return false;
                                }
                            }else if(error instanceof Map){
                                Map em= (Map)error;
                                String msg = (String)em.get("msg");
                                String go = (String)em.get("break");
                                String code = (String)em.get("code");
                                if(StringUtils.isNotBlank(msg)){
                                    if(log.isDebugEnabled())
                                        log.debug(msg);
                                    if(StringUtils.isBlank(go)){
                                        throw new ISPException(code,msg);
                                    }
                                }
                                if(StringUtils.isNotBlank(go)){
                                    env.setBreakPoint(go);
                                }
                                return false;
                            }else{
                                log.error(error);
                                return false;
                            }
                        }
                        return false;
                    }else{
                        srcMap.put("check",true);
                        return true;
                    }

                }else if(exp instanceof Boolean){
                    return (Boolean)exp;
                }else{
                    return true;
                }

            }finally {
/*
                env.removeParameter("${input}");
                env.removeParameter("${config}");
*/
            }
        }else{
            return true;
        }
    }

    public boolean checkParameters(String xmlid,XMLParameter env,Map input,Map output,Map config) throws Exception {
        if(!isActive()) throw new Exception("current service ["+getXML().getId()+"] is not active. please check the service status.");
        if(null !=input && input.containsKey("check")){
            Object exp=input.get("check");
            if(null == exp || (exp instanceof String && StringUtils.isBlank((String)exp)))
                return true;
            //input = env.getMapValueFromParameter(input);
            boolean checkresult = check(env,exp,input);
            if(!checkresult)
                return checkresult;
        }
        return checkInput(xmlid,env,input,output,config);
    }



    public ResultCheck checkResult(String xmlid,XMLParameter env,Map input,Map output,Map config,Object ret)throws Exception{
        if(null != output){
            /*//1 format
            Map format = (Map)output.get("format");
            if(null != format){
                format = env.getMapValueFromParameter(format);
                output.put("format",format);
                ret = env.format(ret,format);
            }
            //2 check
            Object exp = output.get("check");
            if(null != exp){
                boolean checkresult = check(env,exp,input,output,config,output);
                if(!checkresult){
                     return new ResultCheck(false,ret,ResultCheck.RESULT_CHECK_FAULT);
                }
            }
            //3 filter
            Object filter = output.get("filter");
            if(null != filter){
                if(filter instanceof String)
                    env.getExpressValueFromMap((String)filter);
            }*/
            Object ck = output.get("check");
            //截取结果的某部分作为结果
            if(StringUtils.isNotBlank((String)output.get("return")) && null != env.get("${return}")){
                Object rr = ObjectUtils.getValueByPath(env,(String)output.get("return"));
                env.put("${return}",rr);
            }
            output = env.getMapValueFromParameter(output,this);
            if(null != ck){
                if(!check(env,ck,output)){
                    //System.out.println(Thread.currentThread().getName()+" false");
                    return new ResultCheck(false,ret,ResultCheck.RESULT_CHECK_FAULT);
                }
            }

        }
        ResultCheck rc= checkReturn(xmlid,env,input,output,config,env.getParameter("${return}"));
        rc.setStatus(ResultCheck.RESULT_CHECK_SUCCESS);
        //System.out.println(Thread.currentThread().getName()+" "+ rc.isSuccess());
        return rc;
    }

    public void clearTempData(XMLParameter parameter){
        parameter.removeParameter("${this_input}");
        parameter.removeParameter("${this_output}");
        parameter.removeParameter("${this_config}");
        parameter.removeParameter("${this_return}");

    }

    /**
     * notification:[
        {action:'ZkClientListener',op:'onlySetTempData',data:{}},
     ]
     * @param c
     * @param parameter
     */
    void doOneNotification(Map c,XMLParameter parameter)throws ISPException{
        if(StringUtils.isNotBlank(c.get("action")) && StringUtils.isNotBlank(c.get("op"))) {
            boolean isAsyn = true;
            if(null != c.get("isAsyn")) {
                if (c.get("isAsyn") instanceof String && StringUtils.isNotBlank(c.get("isAsyn"))) {
                    isAsyn = StringUtils.isTrue((String) c.get("isAsyn"));
                }else if(c.get("isAsyn") instanceof Boolean){
                    isAsyn = (Boolean)c.get("isAsyn");
                }
            }
            if(null != parameter){
                notifyObjectByName((String)parameter.getExpressValueFromMap((String) c.get("action"),this)
                        , isAsyn
                        , (String)parameter.getExpressValueFromMap((String) c.get("op"),this)
                        , parameter.getMapValueFromParameter((Map) c.get("data"),this));
            }else {
                notifyObjectByName((String) c.get("action"),isAsyn, (String) c.get("op"), (Map) c.get("data"));
            }
        }
    }
    void doNotification(Map config,XMLParameter parameter)throws ISPException{
        //任务执行成功后，执行通知任务do notification
        if(null != config && config.containsKey("notification")){
            Object notification = config.get("notification");
            if(notification instanceof Map){
                doOneNotification((Map) notification, parameter);
            }else if(notification instanceof List){
                for(Map m:(List<Map>)notification) {
                    doOneNotification(m, parameter);
                }
            }
        }
    }

    /**
     * alarm:{check:'',notification:{action:'ZkClientListener',op:'onlySetTempData',data:{}}}
     * @param c
     * @param parameter
     */
    void doAlarm(String c,XMLParameter parameter,Throwable e){
        boolean isalarm=false;
        Map a = null;
        if("input".equals(c)){
            isalarm=isInputAlarm();
            if(isalarm){
                String s = getXML().getProperties().getProperty("input");
                a = (Map)StringUtils.convert2MapJSONObject(s).get("alarm");
            }
        }else if("output".equals(c)){
            isalarm=isOutputAlarm();
            if(isalarm){
                String s = getXML().getProperties().getProperty("output");
                a = (Map)StringUtils.convert2MapJSONObject(s).get("alarm");
            }
        }
        if(isalarm && null != a){
            Object o = a.get("check");
            try {
                if (null != o) {
                    if (null != e) {
                        parameter.setError(true);
                        parameter.setException(e);
                    }
                    boolean b = check(parameter, o, a);
                    if(log.isInfoEnabled()) {
                        log.info("alarm check " + o + " result " + b + " result: " + parameter.getResult());
                    }
                    if (b) {
                        //Map d = parameter.getManualMapValueFromParameter(a);
                        doNotification(a, parameter);
                    }
                }

            } catch (Exception ex) {

            }
        }

    }
    /*void createTradeId(XMLParameter env,XMLDoObject td){
        if(null != td) {
            String tradeId = env.getTradeId();
            if(StringUtils.isBlank(tradeId)){
                tradeId = ((XMLLogicTrade)td).newTradeId();
                env.setTradeId(tradeId);
            }
        }
    }*/

    protected boolean startTrade(String xmlid,XMLMakeup xml,XMLParameter parameter,Map input,Map output,Map config){
        String tradeid = parameter.getTradeId();
        boolean isTradeStart=false;
        if(StringUtils.isBlank(tradeid)){
            if(isTrade(xml)){
                tradeid=parameter.newTradeId();
                parameter.setTradeId(tradeid);
                isTradeStart=true;
            }
        }
        if(StringUtils.isNotBlank(tradeid)){
            parameter.addTradeTask(new Object[]{this,xmlid,parameter,input,output,config,xml});
        }
        return isTradeStart;
    }
    protected void commits(XMLParameter parameter,Object ret)throws Exception{
        List<Object[]> trades = parameter.getTradeTaskList();
        if (null != trades) {
            for (Object[] ot : trades) {
                ((XMLDoObject) ot[0]).commit((String) ot[1], (XMLParameter) ot[2], (Map) ot[3], (Map) ot[4], (Map) ot[5], ret);
            }
            parameter.removeTrade();
        }
    }
    protected void rollbacks(XMLParameter parameter,Object ret,Exception e)throws Exception{
        List<Object[]> trades = parameter.getTradeTaskList();
        if (null != trades) {
            for (Object[] ot : trades) {
                ((XMLDoObject) ot[0]).rollback((String) ot[1], (XMLParameter) ot[2], (Map) ot[3], (Map) ot[4], (Map) ot[5], ret, e);
            }
            parameter.removeTrade();
        }

    }
    public void doCheckThing(String xmlid,XMLParameter parameter,Map input,Map output,Map config,XMLMakeup parxml)throws Exception{
        /*if(log.isDebugEnabled()) {
            if(null != parameter) {
                parameter.printTime("do checkThing begin " + (null == xml ? getXML().getId() : xml.getId()));
            }
        }*/
        //服务逻辑使用服务定义的xml，外面传进来的xml只是描述调用时参数
        String id = null ==parxml?getXML().getId():parxml.getId();
        XMLMakeup xml= null ==parxml?getXML():parxml;

        /*if(!xml.toString().equals(getXML().toString())){
            System.out.println();
        }*/
        if(Logger.isDebugEnabled()) {
            Logger.debug(this.getClass(), parameter, getXML().getId()+"-"+id, "begin doCheckThing",input, null);
        }
        //System.out.println("["+Thread.currentThread().getName()+"] ["+new Date().getTime()+"] "+(xml.getId())+" begin... ");
        //boolean isSingleTrade=false;
        Object tempInput= parameter.get("${this_input}");
        Object tempOutput= parameter.get("${this_output}");
        Object tempConfig= parameter.get("${this_config}");
        try{
            if(null == parameter) parameter = new XMLParameter();
            Object ret=null;
            //把解析后处理前的数据放入环境中
            parameter.addParameter("${this_input}",input);
            parameter.addParameter("${this_output}",output);
            parameter.addParameter("${this_config}",config);

            /*String tradeId = parameter.getTradeId();
            //xmldoObject对象属性配置了isTrade
            if(StringUtils.isBlank(tradeId) && isTrade(xml)){
                XMLDoObject td = (XMLDoObject)getObjectById("trade");
                createTradeId(parameter,td);
                tradeId = parameter.getTradeId();
                isSingleTrade=true;
            }
            if(StringUtils.isNotBlank(tradeId)){
                XMLDoObject td = (XMLDoObject)getObjectById("trade");
                parameter.addTradeTask(this,xmlid,new Object[]{parameter,input,output,config});
                td.doSomeThing(xmlid,parameter,null,null,null);
            }else{*/
                if(checkParameters(xmlid,parameter,input,output,config)){
                    //do input alarm
                    doAlarm("input",parameter,null);
                    //========trade judge set begin===========//
                    // only in xmllogic trade
                    boolean isTradeStart = startTrade(xmlid,xml,parameter,input,output,config);
                    //=========trade set end=================//
                    doChildren(parameter,getXML(),"before",config);
                    try {
                        if(Logger.isDebugEnabled()) {
                            Logger.debug(this.getClass(), parameter, getXML().getId()+"-"+id, "begin doSomeThing",input, null);
                        }
                        ret = doSomeThing(xmlid, parameter, input, output, config);
                        /*if(log.isDebugEnabled()) {
                            parameter.printTime("do someThing end " + (null == xml ? getXML().getId() : xml.getId()));
                        }*/
                        if(Logger.isDebugEnabled()) {
                            Logger.debug(this.getClass(), parameter, getXML().getId()+"-"+id, "end doSomeThing",input, null);
                        }
                        if (null == ret) {
                            if (null != parameter)
                                parameter.removeParameter("${return}");
                        } else {
                            if (ret instanceof ResultCheck)
                                ret = ((ResultCheck) ret).getRet();
                            parameter.addParameter("${this_return}", ret);
                            if (null != parameter)
                                parameter.addParameter("${return}", ret);

                            //System.out.println(Thread.currentThread().getName()+" "+new Date().getTime());
                        }
                        ret = checkResult(xmlid, parameter, input, output, config, ret);

                        if(StringUtils.isBlank(parameter.getTradeId())) {
                            commit(xmlid, parameter, input, output, config, ret);
                        }else if(isTradeStart){
                            //======commit other task if trade======//
                            commits(parameter,ret);

                        }
                        doAlarm("output",parameter,null);
                    }catch(Exception e){
                        //Logger.error(parameter,xml,"something error happened ",e);
                        //Logger.error(this.getClass(),parameter,getXML().getId(),"rollback",e);
                        try {
                            if (StringUtils.isBlank(parameter.getTradeId())) {
                                rollback(xmlid, parameter, input, output, config, ret, e);
                            } else if (isTradeStart) {
                                //======rollback other task if trade======//
                                rollbacks(parameter, ret, e);

                            }
                            doAlarm("output",parameter,e);
                            throw e;
                        }catch (Exception ex){
                            doAlarm("output",parameter,e);
                            throw ex;
                        }


                    }finally {
                        if(isTradeStart){
                            parameter.removeTrade();
                        }
                    }
                }else{
                    ret=new ResultCheck(false,null,ResultCheck.INPUT_CHECK_FAULT);
                }
            //}

            //do children
            if(null == ret || (null != ret && ret instanceof  ResultCheck && ((ResultCheck)ret).isSuccess()) || !(ret instanceof  ResultCheck)){
                //任务执行后，再执行孩子的任务
                doChildren(parameter,getXML(),"after",config);

                doNotification(config,parameter);
            }
            //System.out.println(this.getClass().getName()+" "+Thread.currentThread().getName()+ " 1111");
            //如果有指定执行结果存放位置则放入参数的对应位置
            if(null != output && null != parxml && StringUtils.isNotBlank(parxml.getProperties().getProperty("to") )){
                parameter.setResult(parxml.getProperties().getProperty("to"),ret);
            }else{
                //System.out.println(this.getClass().getName()+" "+Thread.currentThread().getName()+ " 222");

                if(null != parameter){
                    //System.out.println(this.getClass().getName()+" "+Thread.currentThread().getName()+ " 3333");
                    if(null != ret){
                        if(ret instanceof ResultCheck){
                            //System.out.println(this.getClass().getName()+" "+Thread.currentThread().getName()+ " 4444");
                            if(((ResultCheck)ret).isSuccess()){
                                parameter.setResult(ret);
                                //System.out.println(Thread.currentThread().getName() +" ret success "+ret);
                            }else{
                                //System.out.println(this.getClass().getName()+" "+Thread.currentThread().getName()+ " 5555");
                                Object rt = parameter.getResult();
                                if(null !=rt){
                                    if(rt instanceof ResultCheck){
                                        ((ResultCheck)rt).setSuccess(false);
                                    }else{
                                        parameter.setResult(new ResultCheck(false,parameter.getResult()));
                                    }
                                    //System.out.println(Thread.currentThread().getName() +" ret fault "+parameter.getResult());
                                }else{
                                    parameter.setResult(new ResultCheck(false,null));
                                }
                            }
                            //System.out.println(Thread.currentThread().getName()+"---");
                        }else{
                            //System.out.println(this.getClass().getName()+" "+Thread.currentThread().getName()+ " 6666");
                            parameter.setResult(new ResultCheck(true,ret));
                        }
                    }else{
                        //System.out.println(this.getClass().getName()+" "+Thread.currentThread().getName()+ " 7777");
                        parameter.setResult(new ResultCheck(true,null));
                    }
                }
            }
            //如果前面已经调用超时，这里后端业务处理结束，删除redo日志
            if(isTimeoutActionBackground(parameter,xml)){
                removeTimeoutRedo(parameter, xml);
            }
        }catch(Exception ex){

            //log.error(xml.getId(),ex);
            if(null != parameter){
                parameter.setException(ex);
                parameter.setError(true);
                //clearTempData(parameter);
                parameter.setResult(new ResultCheck(false,ex,ResultCheck.INPUT_CHECK_ERROR));
            }
            //log.error(xml.getId()+"\n"+getString(input,output,config),ex);
            //Logger.error(parameter,xml,"checkthing error happened",ex);
            //如果之前逻辑(xmllogic中上面的逻辑)设置了重做标志，或者该服务自身配置了重做标志，发生异常时记录重做日志，如果改服务设置了redo，且出现异常，需要重做，我们先保存，后由重做机制重做
            if(null != parameter && (parameter.isRedoService() || isRedo())
                    && (null == parameter.getSuspendXMlId()|| isTimeoutBackground(parameter,xml))
                    && null != parameter.getParameter("${env}") && StringUtils.isNotBlank(((Map)parameter.getParameter("${env}")).get("saveRedoService"))
                    && null != getObjectById((String)((Map)parameter.getParameter("${env}")).get("saveRedoService"))
                    && null != getObjectById("redo")
                    && StringUtils.isNotBlank(parameter.getParameter("${requestId}"))
                    && !parameter.isSuspend() ){
                if(isTimeoutBackground(parameter,xml)){
                    removeTimeoutRedo(parameter,xml);
                    parameter.setStatus(XMLParameter.HAPPEN_TIMEOUT_EXCEPTION);//timeout_exception
                }else{
                    parameter.setStatus(XMLParameter.HAPPEN_EXCEPTION);//exception
                }
                XMLDoObject suspendTheRequest = (XMLDoObject)getObjectById((String)((Map)parameter.getParameter("${env}")).get("saveRedoService"));
                //parameter.setSuspendXMlId(xml.getId());
                parameter.setSuspendXMlId(xmlid+","+getXML().getId());//这里设置的是实用的别名，方便主服务查找
                log.error("happend error node:"+xml);
                suspendTheRequest.doThing(parameter, xml);
            }
            throw ex;
        }finally {
            /*if(isSingleTrade){
                try {
                    XMLDoObject trade = (XMLDoObject)getObjectById("trade");
                    String tradeId = parameter.getTradeId();
                    Object ret = ((XMLLogicTrade)trade).getResult(tradeId);
                    parameter.setResult(ret);
                    parameter.removeTrade();
                }catch (Exception e){
                    throw e;
                }

            }*/
            XMLMakeup m = xml;
            if(null != parxml){m = parxml;}
            if(null != m && m.getProperties().containsKey("msg")) {
                print(parameter, m.getProperties().getProperty("msg"), null, xmlid);
            }
            if(null != parameter ){
                clearTempData(parameter);
                if(!parameter.isAutoProcess()){
                    parameter.removeParameter("${return}");
                }
            }
            if(tempConfig!=null){
                parameter.put("${this_config}",tempConfig);
            }
            if(tempInput!=null){
                parameter.put("${this_input}",tempInput);
            }
            if(tempOutput!=null){
                parameter.put("${this_output}",tempOutput);
            }
            if(Logger.isDebugEnabled()) {
                Logger.debug(this.getClass(), parameter, getXML().getId()+"-"+id, "end doCheckTing",input, null);
            }
            /*if(log.isDebugEnabled()) {
                parameter.printTime("do checkThing end " + (null == xml ? getXML().getId() : xml.getId()));
            }*/
            //System.out.println("["+Thread.currentThread().getName()+"] ["+new Date().getTime()+"] "+(xml.getId())+" lost:"+(System.currentTimeMillis()-l)+" input:"+input+"  output:"+output+" config:"+config);
            //System.out.println("["+Thread.currentThread().getName()+"] ["+new Date().getTime()+"] "+(xml.getId())+" lost:"+(System.currentTimeMillis()-l));
        }
    }


    protected void print(XMLParameter env,Object msg,String append,String xmlkey){
        if(msg !=null){
            try{
                int n = 0;
                if(null != env && null != env.getResult() && env.getResult() instanceof ResultCheck){
                    n = ((ResultCheck)env.getResult()).getStatus();
                }
                if(null == append)append="";
                if (msg instanceof Map){
                    Object ch = ((Map)msg).get("check");
                    if(!checkStr(env,(String)ch))
                        return;
                    msg = ((Map)msg).get("msg");
                }
                if(msg instanceof String && ((String)msg).startsWith("{")){
                    Map o = StringUtils.convert2MapJSONObject((String)msg);

                    if(null !=o.get("status") && !ArrayUtils.isInStringArray(((String)o.get("status")).split(","),String.valueOf(n))){
                        return;
                    }
                    Map v = env.getMapValueFromParameter(o,this);
                    if(log.isInfoEnabled())
                    log.info(("["+System.currentTimeMillis()+"] ["+xmlkey+"] "+v.toString()+append+"\n"));
                }else if(msg instanceof String && ((String)msg).startsWith("${")){
                    Object o = ObjectUtils.getValueByPath(env.getReadOnlyParameter(),(String)msg);
                    if(o instanceof Collection){
                        o = ArrayUtils.toJoinString((Collection)o);
                    }else if(o.getClass().isArray()){
                        o = ArrayUtils.toJoinString((Object[])o);
                    }
                    log.info(("["+System.currentTimeMillis()+"] ["+xmlkey+"] "+o+append+"\n"));
                }else{
                    log.info(("["+System.currentTimeMillis()+"] ["+xmlkey+"] "+msg+append+"\n"));
                }
            }catch (Exception e){
                try{
                    log.error(("["+System.currentTimeMillis()+"] ["+xmlkey+"] "+e.getMessage()+append+"\n"));
                }catch (Exception ex){}
            }
        }
    }

    //判断是否是发生timeout的后续处理过程
    boolean isTimeoutActionBackground(XMLParameter par,XMLMakeup xml){
        if(par.getStatus()==XMLParameter.HAPPEN_TIMEOUT
                && Thread.currentThread().getName().equals(par.getGlobalParameter("^{Timeout_BG_Thread_Name}"))
                && (xml.getId()+","+getXML().getId()).equals(par.getSuspendXMlId())
                ){
            return true;
        }
        return false;
    }
    boolean isTimeoutBackground(XMLParameter par,XMLMakeup xml){
        if(par.getStatus()==XMLParameter.HAPPEN_TIMEOUT
                && Thread.currentThread().getName().equals(par.getGlobalParameter("^{Timeout_BG_Thread_Name}"))
                ){
            return true;
        }
        return false;
    }
    //timeout 后台程序处理完毕，更新redo
    protected void removeTimeoutRedo(XMLParameter parameter,XMLMakeup xml)throws Exception{
        XMLDoObject suspendTheRequest = (XMLDoObject)getObjectById((String)((Map)parameter.getParameter("${env}")).get("saveRedoService"));
        parameter.setStatus(XMLParameter.TIMEOUT_DELETE);
        suspendTheRequest.doThing(parameter,xml);

    }
    String getString(Map input,Map output,Map config){
        StringBuffer sb = new StringBuffer();
        sb.append("input:"+input+"\n");
        sb.append("output:"+output+"\n");
        sb.append("config:"+config+"\n");
        return sb.toString();
    }

    boolean isInputAlarm(){
        if(getXML().getProperties().containsKey("input")){
            String input = getXML().getProperties().getProperty("input");
            int n =input.indexOf("alarm");
            if(n>=0 && input.indexOf("check")>n && input.indexOf("notification")>n){
                return true;
            }

        }
        return false;
    }
    boolean isOutputAlarm(){
        if(getXML().getProperties().containsKey("output")){
            String output = getXML().getProperties().getProperty("output");
            int n =output.indexOf("alarm");
            if(n>=0 && output.indexOf("check")>n && output.indexOf("notification")>n){
                return true;
            }
        }
        return false;
    }

    public boolean isAlarm(){
        boolean isAlarm = isInputAlarm();
        if(isAlarm)return isAlarm;
        isAlarm=isOutputAlarm();
        return isAlarm;
    }
    public boolean isAsyn(XMLMakeup x){
        Object r = x.getProperties().get("isAsyn");
        if(r==null)
            r = x.getProperties().get("isasyn");
        if(r instanceof String){
            r = Boolean.parseBoolean((String)r);
        }
        if(null != r) {
            return (Boolean) r;
        }
        return false;
    }

    public boolean isTrade(XMLMakeup x){
        Object r = x.getProperties().get("isTrade");
        if(r==null)
            r = x.getProperties().get("istrade");
        if(r instanceof String){
            r = Boolean.parseBoolean((String)r);
        }
        if(null != r)
            return (Boolean)r;
        else
            return false;
    }
    public boolean hasExeProperty(XMLMakeup x){
        Object r = x.getProperties().get("isExe");
        if(null !=r)
            return (Boolean)r;
        else
            return false;
    }

}
