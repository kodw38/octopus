package com.octopus.utils.xml.auto.logic;

import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.bftask.BFParameters;
import com.octopus.utils.exception.ISPException;
import com.octopus.utils.exception.Logger;
import com.octopus.utils.flow.FlowParameters;
import com.octopus.utils.thread.ThreadPool;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: Administrator
 * Date: 15-3-3
 * Time: 上午10:55
 * 支持标签
 *  <do  seq="" action="" jsonpar="" parmap=""
 *  <for collection=""
 *  <if
 */
public class XMLLogic extends XMLDoObject{
    transient static Log log = LogFactory.getLog(XMLLogic.class);
    //static XMLLogicTrade trade;
    XMLDoObject remote;
    public XMLLogic(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    static Map ObjectMap=null;
    Map getObjects(){
        if(null == ObjectMap){
            ObjectMap = getAllObjects();
        }
        return ObjectMap;
    }


    void doActionThing(XMLParameter env,XMLMakeup x)throws Exception{
        try {
            String a = x.getProperties().getProperty("action");
            if (StringUtils.isBlank(a))
                throw new Exception("action is not config in " + x.toString());
            //System.out.println("---"+new Date().getTime());
            XMLDoObject doAction = (XMLDoObject) getObjectById(a); //(XMLDoObject)ObjectUtils.getValueByPath(env.getParameter("${actions}"),a);
            //System.out.println("---"+new Date().getTime());
            Map input = (Map) env.getParameter("^${input#" + this.hashCode() + "}");

            Map config = (Map) env.getParameter("^${config#" + this.hashCode() + "}");

            if (null != doAction) {
                if(log.isDebugEnabled()){
                    log.debug("invoke local service "+a);
                }
                //如果前面没有设置redoe，该服务又是redo service设置标志
                if(!env.isRedoService() && doAction.isRedo()){
                    env.setRedoService();
                }
                if (null != input) {
                    String k = "^${input#" + x.getId() + "}";
                    if(env.containsKey(k) && null != env.get(k)) {
                        //input为子环境下当前的变量，k为xmllogic对象变量
                        if(!(null != env && null !=env.getInputParameter() && System.identityHashCode(env.get(k))==System.identityHashCode(env.getInputParameter()))) {

                            if(System.identityHashCode(env.get(k))!=System.identityHashCode(input)) {
                                if(log.isDebugEnabled()){
                                    log.debug("input "+input +" this input "+env.get(k));
                                }
                                ObjectUtils.appendDeepMapReplaceKey(input, (Map) env.get(k));
                                if(log.isDebugEnabled()){
                                    log.debug("this input "+env.get(k));
                                }
                            }
                        }
                    }else {
                        env.put(k, input);
                    }
                }
                if (null != config) {
                    String k = "^${config#" + x.getId() + "}";
                    if(env.containsKey(k) && null != env.get(k)){
                        if(!(null != env && null !=env.getInputParameter() && System.identityHashCode(env.get(k))==System.identityHashCode(env.getInputParameter()))) {
                            ObjectUtils.appendDeepMapReplaceKey(config, (Map) env.get(k));
                        }
                    }else {
                        env.put(k, config);
                    }
                }
                doAction.doThing(env, x);
            } else {
                if(log.isDebugEnabled()){
                    log.debug("invoke remote service "+a);
                }
                //本地没有该服务，查找，调用远程服务
                if(null != remote){
                    //input_data,targetNames
                    doRemote(remote,env,x,input);

                }else {
                    throw new Exception("not find the action[" + a + "]");
                }
                doPrint(env,x);
            }

        }catch (Exception e){
            doPrint(env,x);
            throw e;
        }
    }
    void doRemote(XMLDoObject obj,XMLParameter par,XMLMakeup x,Map input) throws Exception {
        BFParameters p = new BFParameters(false);
        String in = x.getProperties().getProperty("input");
        Map inmap=null;
        if(StringUtils.isNotBlank(in)){
            inmap = StringUtils.convert2MapJSONObject(in);
            inmap = par.getMapValueFromParameter(inmap,this);
        }
        if(null !=input){
            ObjectUtils.appendDeepMapNotReplaceKey(input,inmap);
        }
        p.addParameter("${targetNames}",new String[]{x.getProperties().getProperty("action")});
        p.addParameter("${input_data}",inmap);
        p.addParameter("${session}",par.getParameter("${session}"));
        p.addParameter("${AuthInfo}",par.getParameter("${AuthInfo}"));
        p.addParameter("${requestHeaders}",par.getParameter("${requestHeaders}"));
        p.addParameter("${requestProperties}",par.getParameter("${requestProperties}"));
        p.addParameter("${insid}",par.getParameter("${insid}"));
        if(null !=inmap) {
            inmap.remove("exe_id");
            inmap.remove("exe_xml");
            inmap.remove("exe_error");
        }
        if(log.isDebugEnabled()) {
            log.debug("remote XMLParameter\n" + p);
        }
        obj.doThing(p, x);
        par.setResult(p.getResult());
    }
    boolean addResult2Parameter(XMLParameter env,XMLMakeup x)throws ISPException{
        try{
            if(null !=env && null != env.getResult() && env.getResult() instanceof ResultCheck){
                ResultCheck os = (ResultCheck)env.getResult();
                if(null != os){
                    if(os.isSuccess()){
                        if(StringUtils.isNotBlank(x.getProperties().getProperty("key"))) {
                            //String rangekey ="${"+x.getProperties().getProperty("key")+"}";
                            //env.addParameter(rangekey,os.getRet());
                            String rangekey = x.getProperties().getProperty("key");
                            addTempParameter(env, getTempKeysMap(env), rangekey, os.getRet());
                            if (log.isDebugEnabled()) {
                                log.debug(" XMLParameter add  key:" + "${" + x.getProperties().getProperty("key") + "}\n" + "            value:" + os.getRet());
                            }
                            if (isResultKey(rangekey)) {
                                doResult(env, getXML());//如果当前结果需要作为xmllogic的返回结果,需要设置
                            }else{
                                env.setResult(null);
                            }
                        }else{
                            env.setResult(null);
                        }
                    }
                    ///if(os.isSuccess())
                       // env.setResult(null);
                    //System.out.println(Thread.currentThread().getName()+" do children:"+os.isSuccess());
                    return os.isSuccess();
                }
            }
            return false;
        }finally {
            //env.setResult(null);
        }
    }
    boolean isResultKey(String rangekey){
        return ("${"+rangekey+"}").equals(getXML().getProperties().getProperty("result"));
    }
    void addTempParameter(XMLParameter env,Map tempinputkeys,String k,Object o){
        if(null != tempinputkeys) {
            if (!env.containsParameter("${" + k + "}")) {
                env.addParameter("${" + k + "}", o);
                tempinputkeys.put("${" + k + "}", "~PRIVATE_NEW~");
            } else {
                tempinputkeys.put("${" + k + "}", env.getParameter("${" + k + "}"));
                env.addParameter("${" + k + "}", o);
            }
        }else{
            env.addParameter("${" + k + "}", o);
        }
    }
    Map getTempKeysMap(XMLParameter env){
        String id = getId(env);
        return (Map)env.getParameter("this_range_child_id_keys."+id);
    }
    void clearTransforKeyParameter(String id,XMLParameter env,String xmlid){
        //remote the env parameter inputed by parent return
        Map parentInput = (Map)env.getParameter("this_range_child_id_keys."+id);
        if(null != parentInput && null != env ){
            Iterator its = parentInput.keySet().iterator();
            while(its.hasNext()){
                String k = (String)its.next();
                if(StringUtils.isNotBlank(xmlid) && xmlid.equals("${"+id+"}")){
                    continue;
                }
                if("~PRIVATE_NEW~".equals(parentInput.get(k))){
                    env.removeParameter(k);
                }else{
                    env.addParameter(k, parentInput.get(k));
                }
            }
            parentInput.clear();
        }

    }
    void transforInputParameter(String id,XMLParameter env){
        //put parent result to env
        Map parentInput = (Map)env.getParameter("${this_input}");
        Map tempinputkeys = new HashMap();
        if(null != parentInput){
            if(null != env && null != parentInput){
                Iterator its = parentInput.keySet().iterator();
                while(its.hasNext()){
                    String k = (String)its.next();
                    addTempParameter(env,tempinputkeys,k,parentInput.get(k));
                }
                env.addParameter("this_range_input_keys."+id,tempinputkeys);
            }
        }
        //clear temp data
        this.clearTempData(env);
    }
    void clearTransforInputParameter(String id,XMLParameter env){
        //remote the env parameter inputed by parent return
        Map parentInput = (Map)env.getParameter("this_range_input_keys."+id);
        if(null != parentInput && null != env ){
            Iterator its = parentInput.keySet().iterator();
            while(its.hasNext()){
                String k = (String)its.next();

                if("~PRIVATE_NEW~".equals(parentInput.get(k))){
                    env.removeParameter(k);
                }else{
                    env.addParameter(k, parentInput.get(k));
                }
            }
            parentInput.clear();
        }

    }


    public void doChildren(XMLParameter env,XMLMakeup x,String autotype)throws Exception{

        if(StringUtils.isBlank(autotype) || !"logic".equals(autotype))return;
        //print property message
        String id = x.getId();
        /*String msg = x.getProperties().getProperty("msg");
         String id = x.getId();
         if(StringUtils.isNotBlank(msg)){
            if(StringUtils.isNotBlank(msg)){

                print(env,msg,null,id);
            }
        }*/
        String reqid = (String)env.getParameter("${requestId}");
        if(StringUtils.isNotBlank(reqid)){
            id =reqid+"-"+id;
        }
        try{

            //just result and do children
             if(addResult2Parameter(env,x)){
                     if (x.getChildren().size() > 0) {
                         try {
                             transforInputParameter(id, env);//为执行
                             for (XMLMakeup c : x.getChildren()) {
                                 doElement(env, c);
                                 //break
                                 String go = env.getBreakPoint();
                                 if (StringUtils.isNotBlank(go) && getXML().existKey(go)) {
                                     if (go.equals(x.getId()))
                                         env.removeBreakPoint();
                                     break;
                                 }
                             }
                         }finally {
                             clearTransforInputParameter(id,env);
                         }
                     }

             }else{
                 //do check error
                 //XMLMakeup cxe = (XMLMakeup)ArrayUtils.getFirst(x.getChild("checkerror"));
                 XMLMakeup cxe = x.getFirstChildrenEndWithName("checkerror");
                 if(null != cxe)
                     doDo(env,cxe);

             }

         }catch (Exception e){
             throw e;
         }finally {
            //remote children key from env
            String[] kes = x.getChildrenPropertiesValue("key");
            if(null != kes){
                for(String k:kes) {
                    if(!isResultKey(k)) {
                        env.removeParameter("${" + k + "}");
                    }
                }
            }
         }

    }
    void doDo(XMLParameter env,XMLMakeup x)throws Exception{
        boolean istrade=false;
        try{
            //log logic servicetrace
            if(isNotCycle(getXML()) && isNotCycle(x)){
                //env.addDetailServiceTrace(x.getId());
                String targetName="";
                if(null != env.getTargetNames()){
                    targetName=env.getTargetNames()[0];
                }
                if(targetName.equals(getXML().getId()) && getXML().existKey(x.getId())){
                    env.addServiceTrace(x.getId());
                }
            }
            env.setAutoProcess(true);

            istrade = startTrade(x.getId(),x,env,null,null,null);
            if(isAsyn(x)){
                //如果当前do为异步执行的,如果其有孩子,则异步执行完后再执行,不在当前线程执行
                //if(x.getChildren().size()>0){
                    //异步处理，需要传到子线程的变量用$${}表示
                    env.addParameter(XMLParameter.XMLLOGIC_BACK_CALL_KEY,new Object[]{this,"doChildren",new Class[]{XMLParameter.class,XMLMakeup.class,String.class},new Object[]{env,x,"logic"}});
                    //如果有执行属性，则子项的执行也要放入执行属性中执行
                //}
                doActionThing(env,x);
            }else{
                /*if(x.getProperties().containsKey("msg")){
                    if(StringUtils.isNotBlank(x.getProperties().getProperty("msg"))){
                        Object o = env.getExpressValueFromMap(x.getProperties().getProperty("msg"));
                        print(env,o,null);
                    }
                }*/
                if(hasExeProperty(x)
                        || (env.isSuspend() && null != env.getSuspendXMlId() && (x.getId()+","+x.getProperties().getProperty("action")).equals(env.getSuspendXMlId()))){
                    //如果有执行属性，则子项的执行也要放入执行属性中执行
                    env.addParameter(XMLParameter.XMLLOGIC_BACK_CALL_KEY,new Object[]{this,"doChildren",new Class[]{XMLParameter.class,XMLMakeup.class,String.class},new Object[]{env,x,"logic"}});
                    doActionThing(env, x);
                }else{
                    doActionThing(env,x);
                    doChildren(env, x, "logic");
                }
            }
            if(istrade){
                commits(env,null);
            }


        }catch (Exception e){
            //log.error(x.getId(),e);
            if(istrade){
                rollbacks(env,null,e);
            }
            throw e;
        }finally {
            /*if(isTrade(x)){
                try {
                    String tradeId = env.getTradeId();
                    Object ret = trade.getResult(tradeId);
                    env.setResult(ret);
                    env.removeTrade();
                }catch (Exception e){
                    throw e;
                }
            }*/
            env.setAutoProcess(false);
            doGoTo(x,env);
        }

    }

    /**
     * goto another node, need to interrupt currently logic and find the goto node to redo with currently env data
     * @param xml
     * @param env
     */
    void doGoTo(XMLMakeup xml,XMLParameter env)throws Exception{
        if(null != xml) {
            String output = (String)xml.getProperties().get("output");
            if(StringUtils.isNotBlank(output)){
                Map out = StringUtils.convert2MapJSONObject(output);
                if(null!=out){
                    List<Map> gotoList = (List)out.get("goto");
                    if(null != gotoList){
                        for(Map m:gotoList){
                            if(null != m.get("cond") && m.get("cond") instanceof String && StringUtils.isNotBlank(m.get("cond"))){
                                if(XMLParameter.isHasRetainChars((String)m.get("cond"))){
                                    if (StringUtils.isTrue(env.getExpressValueFromMap((String) m.get("cond"), this).toString())) {
                                        if(null!=m.get("to") && m.get("to") instanceof String && StringUtils.isNotBlank(m.get("to"))){
                                            env.setGoto((String)m.get("to"));
                                            throw new ISPException("FlowGoto","xmlLogic will goto node "+m.get("to"));
                                        }
                                    }

                                }
                            }
                        }
                    }
                }
            }
        }
    }

    void doFor(XMLParameter env,XMLMakeup x)throws Exception{
        //long l = System.currentTimeMillis();
        /*if(x.getProperties().containsKey("debug")){
            System.out.println();
        }*/
        String path = x.getProperties().getProperty("collection");
        Object o = ObjectUtils.getValueByPath(env.getReadOnlyParameter(),path);//获取for循环处理的数据对象，可以为Array,List,Map对象，也可以是int
        if(null ==o && StringUtils.isNumeric(path)){
            o = Integer.valueOf(path);
        }
        Object json = x.getProperties().get("json");//for中有100个循环处理数据,json设置批量处理的数值,如(len:'10',threadnum:'5') for每达到10时交给一个线程去批量处理，总共有5个线程
        //System.out.println("--"+Thread.currentThread().getName()+" "+new Date()+" lost:"+(System.currentTimeMillis()-l));
        int len=0;int threadNum=0;
        AsyncFor f=null;
        if(StringUtils.isNotBlank(json)){
            json=StringUtils.convert2MapJSONObject((String)json);
            if(StringUtils.isNotBlank(((Map)json).get("len"))) {
                len = ObjectUtils.getInt(((Map) json).get("len"));
            }if(StringUtils.isNotBlank(((Map)json).get("threadnum"))) {
                threadNum = ObjectUtils.getInt(((Map) json).get("threadnum"));
            }
            if(threadNum>0){
                //throw new Exception("now not support asyn for");
                ThreadPool tp=ThreadPool.getInstance().getThreadPool(x.getId(),threadNum);
                f=new AsyncFor();
                f.setThreadPool(tp);
                f.setSendLen(len);
                f.setXml(x);
                f.setXMLParameter(env);
            }
        }
        int ist=0;
        if(null != o && o.getClass().isArray()){
            Object[] os = (Object[])o;

            for(Object obj:os){
                if(null != obj){
                    if(threadNum>0){
                        f.addData(path+".item",obj);
                    }else{
                        env.addParameter(path+".item",obj);
                        if(x.getChildren().size()>0){
                            for(XMLMakeup c:x.getChildren()){
                                doElement(env,c);
                                //break
                                String go = env.getBreakPoint();
                                if(StringUtils.isNotBlank(go) && getXML().existKey(go)){
                                    if(go.equals(x.getId())){
                                        env.removeBreakPoint();
                                        ist=2;
                                    }else{
                                        ist=1;
                                    }
                                    break;
                                }

                            }
                            if(ist==1){
                                break;
                            }
                        }
                    }
                }
            }
        }else if(o instanceof Collection){
            Collection c = (Collection)o;
            Iterator its = c.iterator();
            while(its.hasNext()){
                Object obj = its.next();
                if(null != obj){
                    if(threadNum>0){
                        f.addData(path+".item",obj);
                    }else{
                        env.addParameter(path.concat(".item"),obj);
                        if(x.getChildren().size()>0){
                            for(XMLMakeup ch:x.getChildren()){
                                //System.out.println("for before "+Thread.currentThread().getName()+" "+new Date().getTime());
                                doElement(env,ch);
                                //System.out.println("for after "+Thread.currentThread().getName()+" "+new Date().getTime());
                                //break
                                String go = env.getBreakPoint();
                                if(StringUtils.isNotBlank(go) && getXML().existKey(go)){
                                    if(go.equals(x.getId())){
                                        env.removeBreakPoint();
                                        ist=2;
                                    }else{
                                        ist=1;
                                    }
                                    break;
                                }
                            }
                            if(ist==1){
                                break;
                            }
                        }
                    }
                }
            }
        }else if(o instanceof Integer){
            int c = (Integer)o;
            for(int i=0;i<c;i++){
                if(threadNum>0){
                    f.addData(path+".item","");
                }else{
                    if(x.getChildren().size()>0){
                        for(XMLMakeup ch:x.getChildren()){
                            doElement(env,ch);
                            String go = env.getBreakPoint();
                            if(StringUtils.isNotBlank(go) && getXML().existKey(go)){
                                if(go.equals(x.getId())){
                                    env.removeBreakPoint();
                                    ist=2;
                                }else{
                                    ist=1;
                                }
                                break;
                            }
                        }
                        if(ist==1){
                            break;
                        }
                    }
                }
            }
        }else if(o instanceof Map){
            Map m = (Map)o;
            Iterator its = m.keySet().iterator();
            while(its.hasNext()){
                Object k = its.next();
                Object v = m.get(k);
                if(threadNum>0){
                    throw new Exception("now for not support map item used threadnum parameter");
                }else{
                    env.addParameter(path+".key",k);
                    env.addParameter(path+".value",v);
                    if(x.getChildren().size()>0){
                        for(XMLMakeup ch:x.getChildren()){
                            doElement(env,ch);
                            //break
                            String go = env.getBreakPoint();
                            if(StringUtils.isNotBlank(go) && getXML().existKey(go)){
                                if(go.equals(x.getId())){
                                    env.removeBreakPoint();
                                    ist=2;
                                }else{
                                    ist=1;
                                }
                                break;
                            }
                        }
                        if(ist==1){
                            break;
                        }
                    }
                }
            }
        }
        if(null!=f){
            f.finished();
            ThreadPool.getInstance().returnThreadPool(f.tp);
        }
    }
    void doIf(XMLParameter env,XMLMakeup xml)throws Exception{
        String exp = xml.getProperties().getProperty("cond");
        /*if(log.isDebugEnabled()) {
            env.printTime("do if begin " + exp);
        }*/
        if(Logger.isDebugEnabled())
            Logger.debug(this.getClass(),env,(null==xml?"":xml.getId()),"do if begin "+exp,null,null);
        boolean o = checkStr(env, exp);
        /*if(log.isDebugEnabled()) {
            env.printTime("do if end " + o);
        }*/
        if(Logger.isDebugEnabled())
            Logger.debug(this.getClass(),env, (null==xml?"":xml.getId()),"do if end "+o,null,null);
        boolean istrue=true;
        for(XMLMakeup x:xml.getChildren()){
            if(x.getName().equals("else")) {
                istrue=false;
                continue;
            }
            if(o && istrue){
                doElement(env,x);
            }
            if(!o && !istrue){
                doElement(env,x);
            }
            //break
            String go = env.getBreakPoint();
            if(StringUtils.isNotBlank(go) && getXML().existKey(go)){
                if(go.equals(xml.getId()))
                    env.removeBreakPoint();
                break;
            }
        }
    }
    boolean checkWhileStr(XMLParameter env,String exp)throws Exception{
        if(exp.startsWith("#{")){
            String s = exp.substring(2,exp.length()-1);
            Object o = env.getExpressValueFromMap(s,this);
            AtomicInteger count = (AtomicInteger)env.getParameter("${while_same_cond_exe_count}");
            if(null!=count){
                if(count.get()>100){
                    throw new Exception("do while always cycle "+o);
                }
                Object t = (Object)env.getParameter("${while_same_cond_exe_obj}");
                if(t==null)
                    env.addParameter("${while_same_cond_exe_obj}",o);
                else{
                    if(t.equals(o))
                        count.addAndGet(1);
                    else
                        env.addParameter("${while_same_cond_exe_obj}",o);
                }
            }
            String rs = "#{"+o.toString()+"}";
            return checkStr(env,rs);
        }else{
            return checkStr(env,exp);
        }
    }
    void doWhile(XMLParameter env,XMLMakeup xml)throws Exception{
        String exp = xml.getProperties().getProperty("cond");

        env.addParameter("${while_same_cond_exe_count}",new AtomicInteger(1));
        while(checkWhileStr(env,exp)){
            int ist=0;
            for(XMLMakeup x:xml.getChildren()){
                doElement(env,x);
                //break
                String go = env.getBreakPoint();
                if(StringUtils.isNotBlank(go) && getXML().existKey(go)){
                    if(go.equals(x.getId())){
                        env.removeBreakPoint();
                        ist=2;
                    }else{
                        ist=1;
                    }
                    break;
                }

            }
            if(ist==1){
                break;
            }
            //System.out.println(new Date());
        }
        env.removeParameter("${while_same_cond_exe_count}");
        env.removeParameter("${while_same_cond_exe_obj}");
    }
    void doResult(XMLParameter env,XMLMakeup xml)throws ISPException{
        String value = xml.getProperties().getProperty("value");
        if(StringUtils.isNotBlank(value)) {
            if (value.startsWith("{")) {
                value = "{value:" + value + "}";
                Map m = (Map) env.getMapValueFromParameter(StringUtils.convert2MapJSONObject(value),this);
                Object o = m.get("value");
                env.setResult(o);
            } else {
                Object o = ObjectUtils.getValueByPath(env.getReadOnlyParameter(), xml.getProperties().getProperty("value"));
                env.setResult(o);
            }
        }else if(StringUtils.isNotBlank(xml.getProperties().getProperty("result"))){
            value = xml.getProperties().getProperty("result");
            if (value.startsWith("{")) {
                value = "{result:" + value + "}";
                Map m = (Map) env.getMapValueFromParameter(StringUtils.convert2MapJSONObject(value),this);
                Object o = m.get("result");
                env.setResult(o);
            } else {
                Object o = ObjectUtils.getValueByPath(env.getReadOnlyParameter(), xml.getProperties().getProperty("result"));
                env.setResult(o);
            }
        }
    }
    void doPrint(XMLParameter env,XMLMakeup xml){
        String msg = xml.getProperties().getProperty("msg");
        if(StringUtils.isNotBlank(msg)){
            Object logicid = env.getParameter("^${thisKey#"+this.hashCode()+"}");
            String id  = xml.getId();
            if(null != logicid){
                id +=" " +logicid;
            }
            print(env,msg,null,id);
        }else if(xml.getProperties().containsKey("trace")){
            XMLObject o =this;
            while(null !=o && o instanceof XMLObject ){
                log.info(o.getXML().getId());
                o = o.getParent();
            }
        }else if(xml.getProperties().containsKey("all")){
            if(null != env) {
                log.info(env.toString());
            }
        }

    }
    void doError(XMLParameter env,XMLMakeup xml)throws Exception{
        String msg = xml.getProperties().getProperty("msg");
        String code = xml.getProperties().getProperty("code");
        if(StringUtils.isNotBlank(msg)){
            if(msg instanceof String && ((String)msg).startsWith("{")){
                Map o = StringUtils.convert2MapJSONObject((String)msg);
                Map v = env.getMapValueFromParameter(o,this);
                throw new ISPException(code,v.toString());

            }else if(msg instanceof String && ((String)msg).startsWith("${")){
                Object o = ObjectUtils.getValueByPath(env.getReadOnlyParameter(),(String)msg);
                if(o instanceof Collection){
                    o = ArrayUtils.toJoinString((Collection)o);
                }else if(o.getClass().isArray()){
                    o = ArrayUtils.toJoinString((Object[])o);
                }
                throw new ISPException(code,o.toString());
            }else{
                throw new ISPException(code,msg);
            }
        }
    }
/*
    void print(XMLParameter env,Object msg,String append,String xmlkey){
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
*/
    public void  doElement(XMLParameter env,XMLMakeup xml)throws Exception{
        if (isEnable(xml)) {
            if (xml.getName().equals("if")) {
                doIf(env, xml);
            } else if (xml.getName().equals("for")) {
                doFor(env, xml);
            } else if (xml.getName().equals("do")) {
                doDo(env, xml);
            } else if (xml.getName().equals("while")) {
                doWhile(env, xml);
            } else if (xml.getName().equals("result")) {
                doResult(env, xml);
            } else if (xml.getName().equals("print")) {
                doPrint(env, xml);
            } else if (xml.getName().equals("error")) {
                doError(env, xml);
            } else if (xml.getName().equals("timestart")) {
                String msg = (String) xml.getProperties().get("msg");
                String key = (String) xml.getProperties().get("key");
                print(env, msg, " start ...", xml.getId());
                env.addParameter(key, System.currentTimeMillis());

            } else if (xml.getName().equals("timeprint")) {
                Object msg = (String) xml.getProperties().get("msg");
                String key = (String) xml.getProperties().get("key");
                if (null != env.getParameter(key)) {
                    Long pre = (Long) env.getParameter(key);
                    print(env, msg, " lost:(" + System.currentTimeMillis() + " - " + pre + ") = " + (System.currentTimeMillis() - pre) + " ms", xml.getId());
                } else
                    log.error("not find timeprint key [" + key + "]. please check var range");
            }
        }

    }

    String getId(XMLParameter env){
        String id=getXML().getId();
        if(env.containsParameter("${requestId}")){
            id =env.getParameter("${requestId}")+"-"+id;
        }
        return id;
    }
    @Override
    public Object doSomeThing(String xmlid,XMLParameter env, Map input, Map output,Map config) throws Exception {
        //异步回调
        if(null !=input && null != input.get("$${XMLLogic_DoXML}")){
            //backDo(env,parameter,jsonPar);
            return null;
        }else {
            if(null == env) env = new XMLParameter();
            String temk = ""+this.hashCode();
            boolean istrade=false;
            String id=getId(env);
            boolean isChild=false;
            try{
                env.addParameter("^${thisKey#"+temk+"}",xmlid);
                istrade = startTrade(getXML().getId(),getXML(),env,null,null,null);

                if(null != input && null == env.getParameter("^${input#"+temk+"}"))
                    env.addParameter("^${input#"+temk+"}",input);
                if(null != output && null == env.getParameter("^${output#"+temk+"}"))
                    env.addParameter("^${output#"+temk+"}",output);
                if(null != config && null == env.getParameter("^${config#"+temk+"}"))
                    env.addParameter("^${config#"+temk+"}",config);
                /*if(null != env.getResult() && null == env.getParameter("${result#"+temk+"}"))
                    env.addParameter("${result#"+temk+"}",env.getResult());*/
                //设置是否是redo service
                if(!env.isRedoService() && isRedo()){
                    env.setRedoService();
                }
                env.setAutoProcess(true);
                if(Logger.isDebugEnabled()){
                    Logger.debug(this.getClass(),env,id,"",null,null);
                }
                if(getXML().getChildren().size()>0){
                    isChild=true;
                    transforInputParameter(id,env);
                    env.addParameter("this_range_child_id_keys."+id,new HashMap());
                    if(null != input){
                        env.addParameter("${this_input}",input);
                    }
                    if(!env.isSuspend()||(env.isSuspend() && env.idSuspendDo())) {
                        try {
                            for (XMLMakeup x : getXML().getChildren()) {
                                doElement(env, x);
                                //break
                                String go = env.getBreakPoint();
                                if (StringUtils.isNotBlank(go) && getXML().existKey(go)) {
                                    if (go.equals(getXML().getId()))
                                        env.removeBreakPoint();
                                    break;
                                }
                            }
                        }catch(Exception e){
                            //当发生异常是，处理<catch><do....</catch>中的逻辑后，再抛出异常
                            try {
                                XMLMakeup[] cat = getXML().getChild("catch");
                                if (null != cat && cat.length > 0) {
                                    if (null != cat[0]) {
                                        List<XMLMakeup> cl = cat[0].getChildren();
                                        if (null != cl) {
                                            for (XMLMakeup x : cl) {
                                                doElement(env, x);
                                                //break
                                                String go = env.getBreakPoint();
                                                if (StringUtils.isNotBlank(go) && getXML().existKey(go)) {
                                                    if (go.equals(getXML().getId()))
                                                        env.removeBreakPoint();
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }catch(Exception se){

                            }
                            throw e;
                        }
                    }else{//重做
                        String ids = env.getSuspend();
                        if(StringUtils.isNotBlank(ids)) {
                            String[] idarray = ids.split("\\|");//reqSv+"|"+xmlid+"|"+nodeid+"|"+reqid;
                            if(idarray.length>3) {
                                if (StringUtils.isNotBlank(idarray[1])) {
                                    String[] keys = idarray[1].split(",");
                                    XMLMakeup sx = findSuspendNode(getXML(), keys[0], keys[1], idarray[2]);
                                    if (null != sx) {
                                        env.setSuspendDo(true);
                                        doActiveAction(env, sx);
                                    } else {
                                        if(keys[0].equals(keys[1]) && getXML().getId().equals(keys[0])){
                                            //redo it from first node
                                            env.setSuspendDo(true);
                                            log.debug("start to do action"+getXML().getId()+" from first Node.");
                                            doActiveAction(env, getXML());
                                        }else {
                                            throw new Exception("not find the redo node [" + ids + "]\n"+"key:"+keys[0]+" action:"+ keys[1]+" nodeid:"+ idarray[2]+"\n"+getXML());
                                        }
                                    }
                                }
                            }else if(idarray.length==1){
                                //start from fist node
                                env.setSuspendDo(true);
                                log.debug("start to do action"+getXML().getId()+" from first Node.");
                                doActiveAction(env, getXML());
                            }
                        }
                    }


                }
                doResult(env,getXML());
                if(istrade){
                    commits(env,env.getResult());
                }
                /*if(null == env.getResult() && null != env.getParameter("${result#"+temk+"}")){
                    env.setResult(env.getParameter("${result#"+temk+"}"));
                }*/
                return env.getResult();
            }catch(Exception e){
                if(istrade){
                    rollbacks(env,env.getResult(),e);
                }
                //do goto interrupt
                if(StringUtils.isNotBlank(env.getGoto())){
                    XMLMakeup x = findSuspendNode(getXML(),env.getGoto(),null,null);
                    if(null != x){
                        doGotoAction(env,x);
                    }
                    return null;
                }else {
                    throw e;
                }
            }finally {
                if(isChild) {
                    clearTransforInputParameter(id, env);
                    clearTransforKeyParameter(id, env, xmlid);
                }
                env.setAutoProcess(false);
                String[] kes = getXML().getChildrenPropertiesValue("key");
                if(null != kes){
                    for(String k:kes)
                        env.removeParameter("${"+k+"}");
                }
                env.removeParameter("^${input#"+temk+"}");
                env.removeParameter("^${output#"+temk+"}");
                env.removeParameter("^${config#"+temk+"}");
                env.removeParameter("^${thisKey#"+temk+"}");
                /*env.removeParameter("${result#"+temk+"}");*/
            }
        }
    }

    boolean isThisNode(XMLMakeup root,String key,String srvkey,String nodeid){
        if(StringUtils.isBlank(nodeid)|| nodeid.equals("null")) nodeid=null;
        if(
                (StringUtils.isBlank(nodeid)
                        ||(StringUtils.isNotBlank(nodeid) && nodeid.equals(root.getProperties().getProperty("nodeid"))))

                &&(StringUtils.isNotBlank(key) && root.getId().equals(key))
                && (null ==srvkey || (StringUtils.isNotBlank(root.getProperties().getProperty("action"))
                      && null !=getObjectById(root.getProperties().getProperty("action"))
                        && srvkey.equals(getObjectById(root.getProperties().getProperty("action")).getXML().getId()))
                   )
                ){
            return true;
        }
        return false;

    }
    XMLMakeup findSuspendNode(XMLMakeup root,String key,String srvname,String nodeid){
        if(isThisNode(root,key,srvname,nodeid)){
            return root;
        }else{
            String n = root.getProperties().getProperty("action");
            if(StringUtils.isNotBlank(n)){
                XMLObject obj = getObjectById(n);
                if(null != obj) {
                    XMLMakeup p = obj.getXML();
                    if (null != p) {
                        XMLMakeup o = findSuspendNode(p, key, srvname, nodeid);
                        if (null != o) {
                            return o;
                        }
                    }
                }else{
                    log.error("can not find redo service by name ["+n+"]");
                }
            }
            List<XMLMakeup> ls =  root.getChildren();
            if(null != ls){
                for(XMLMakeup x:ls){
                    XMLMakeup r = findSuspendNode(x,key,srvname,nodeid);
                    if(null != r){
                        return r;
                    }
                }
            }
        }
        return null;
    }
    void doGotoAction(XMLParameter p,XMLMakeup x) throws Exception {
        if(null != x) {
            doElement(p,x);
            //removeTimeoutRedo(p,x);
            //上次断点执行成功，移除断点标志
            //p.removeSuspendXMlId();
            //p.removeSuspend();
            //todo 移除Hbase中的上个断点日志

            //继续执行孩子逻辑
            /*for (XMLMakeup cx : x.getChildren()) {
                doElement(p, cx);
                //break
                String go = p.getBreakPoint();
                if (StringUtils.isNotBlank(go) && getXML().existKey(go)) {
                    if (go.equals(getXML().getId()))
                        p.removeBreakPoint();
                    break;
                }
            }*/

            //断点孩子逻辑执行完，开始向上遍历执行逻辑
            boolean is=false;
            XMLMakeup r=x;
            XMLMakeup pm=x;
            XMLMakeup topredox=null;
            while(null != pm.getParent()){
                if(isRedo(pm))
                    topredox=pm;
                pm = pm.getParent();
            }
            String curid = x.getId();
            String target = p.getTargetNames()[0];
            while (null != r.getParent()) {
                List<XMLMakeup> cs = r.getParent().getChildren();
                for (XMLMakeup c : cs) {
                    if (c.getId().equals(curid) && (
                            (!c.getProperties().containsKey("nodeid") && !x.getProperties().containsKey("nodeid"))
                                    || (c.getProperties().getProperty("nodeid").equals(x.getProperties().getProperty("nodeid"))))){
                        is=true;
                        continue;
                    }else if(is){
                        doElement(p, c);

                    }
                }
                if(null !=topredox && topredox.equals(r))
                    p.removeRedoService();
                is=false;
                curid = r.getParent().getId();
                r = r.getParent();
                if(r.getId().equals(target)){
                    break;
                }
            }

        }
    }

    void doActiveAction(XMLParameter p,XMLMakeup x) throws Exception {
        if(null != x) {
            doElement(p,x);
            removeTimeoutRedo(p);
            //上次断点执行成功，移除断点标志
            p.removeSuspendXMlId();
            p.removeSuspend();
            //todo 移除Hbase中的上个断点日志

            //继续执行孩子逻辑
            for (XMLMakeup cx : x.getChildren()) {
                doElement(p, cx);
                //break
                String go = p.getBreakPoint();
                if (StringUtils.isNotBlank(go) && getXML().existKey(go)) {
                    if (go.equals(getXML().getId()))
                        p.removeBreakPoint();
                    break;
                }
            }

            //断点孩子逻辑执行完，开始向上遍历执行逻辑
            boolean is=false;
            XMLMakeup r=x;
            XMLMakeup pm=x;
            XMLMakeup topredox=null;
            while(null != pm.getParent()){
                if(isRedo(pm))
                    topredox=pm;
                pm = pm.getParent();
            }
            String curid = x.getId();
            String target = p.getTargetNames()[0];
            while (null != r.getParent()) {
                List<XMLMakeup> cs = r.getParent().getChildren();
                for (XMLMakeup c : cs) {
                    if (c.getId().equals(curid) && (
                            (!c.getProperties().containsKey("nodeid") && !x.getProperties().containsKey("nodeid"))
                    || (c.getProperties().getProperty("nodeid").equals(x.getProperties().getProperty("nodeid"))))){
                        is=true;
                        continue;
                    }else if(is){
                        doElement(p, c);

                    }
                }
                if(null !=topredox && topredox.equals(r))
                    p.removeRedoService();
                is=false;
                curid = r.getParent().getId();
                r = r.getParent();
                if(r.getId().equals(target)){
                    break;
                }
            }

        }
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid,XMLParameter env, Map input,Map output, Map config) throws Exception {
        if(null != getEnvData() && null != input && null != getEnvData().get("isSrvCheckInput") && StringUtils.isTrue(getEnvData().get("isSrvCheckInput").toString())){
             checkInputParameterByDesc(env,input);
        }
        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResultCheck checkReturn(String xmlid,XMLParameter env, Map input, Map output,Map config, Object ret) throws Exception {
        ResultCheck ck=null;
        if(null != ret && ret instanceof ResultCheck){
            ck= (ResultCheck)ret;
        }else{
            ck= new ResultCheck(true,ret);  //To change body of implemented methods use File | Settings | File Templates.
        }
        if(ck.getRet() instanceof Exception){
            throw (Exception)ck.getRet();
        }else{
            return ck;
        }
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return true;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config,Object ret,Exception e) throws Exception {
        return true;
    }


    class AsyncFor{
        ThreadPool tp ;
        int sendLen;
        Object[] list=null;
        int add=0;
        String name =null;
        XMLParameter env;
        XMLMakeup xml;
        List<Thread> ts = new ArrayList();

        public void setSendLen(int sendLen) {
            this.sendLen = sendLen;
            list=new Object[sendLen];
        }
        public void setXml(XMLMakeup xml){
            this.xml=xml;
        }

        public void setXMLParameter(XMLParameter env){
            this.env=env;
        }
        public void setThreadPool(ThreadPool tp) {
            this.tp = tp;
        }
        public void addData(String name,Object obj){
            list[add++]=obj;
            this.name=name;
            if(add==sendLen){
                doSend(name,list,add);
                add=0;
            }
        }
        public void finished(){
            if(add>0){
                doSend(name,list,add);
                add=0;
            }

            tp.waitfinished();

        }
        void doSend(String name,Object[] list,int end){
            Object[] as = Arrays.copyOfRange(list,0,end);
            tp.getExecutor().execute(new AyncSend(env, xml, name, as));
        }
    }

    class AyncSend implements Runnable{
        XMLParameter env;
        XMLMakeup xml;
        String name;
        Object[] list;
        public AyncSend(XMLParameter env,XMLMakeup xml,String name,Object[] list){
            this.env=env;
            this.xml=xml;
            this.name=name;
            this.list=list;
        }

        @Override
        public void run() {
            try{
                FlowParameters f = new FlowParameters(false);

                f.addParameter(name,list);
                if(xml.getChildren().size()>0){
                    for(XMLMakeup c:xml.getChildren()){
                        doElement(f,c);
                    }
                }
            }catch (Exception e){
                log.error(e.getMessage(),e);
            }
        }
    }
}
