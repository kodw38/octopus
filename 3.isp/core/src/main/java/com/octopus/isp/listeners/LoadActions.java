package com.octopus.isp.listeners;

import com.octopus.isp.actions.SystemAction;
import com.octopus.isp.bridge.impl.Bridge;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.XMLUtil;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import com.octopus.utils.xml.desc.Desc;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * User: wfgao_000
 * Date: 15-8-28
 * Time: 下午10:27
 */
public class LoadActions extends XMLDoObject {
    transient Log log = LogFactory.getLog(LoadActions.class);
    XMLDoObject remotestore;
    XMLDoObject stathandler;
    Map<String,IBodyCreator> ls= new HashMap();
    String remotepath;
    String statusoppath;
    String statuspath;
    String optype;
    List<String> localService;
    public LoadActions(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        XMLMakeup[] ms = xml.getChild("bodycreater");
        remotepath = xml.getProperties().getProperty("remotepath");
        statusoppath = xml.getProperties().getProperty("statusoppath");
        statuspath = xml.getProperties().getProperty("statuspath");
        optype = xml.getProperties().getProperty("optype");
        if(null != ms){
            for(XMLMakeup x:ms){
                if(null != x){
                    String k = x.getProperties().getProperty("key");
                    if(StringUtils.isNotBlank(k)){
                        String c = x.getText();
                        if(StringUtils.isNotBlank(c)){
                            ls.put(k,(IBodyCreator)Class.forName(c).newInstance());
                        }
                    }
                }
            }
        }


    }
    void addListeners(String c)throws Exception{
        HashMap map = new HashMap();
        //add listener
        if(StringUtils.isNotBlank(c)) {
            map.put("path", remotepath + "/" + c);
        }else{
            map.put("path", remotepath );
        }
        map.put("op", "addChildrenDataListener");
        remotestore.doSomeThing(null,null,map,null,null);
        map.put("op", "addChildrenPathListener");
        remotestore.doSomeThing(null,null,map,null,null);

        //add listener
        if(StringUtils.isNotBlank(c)) {
            map.put("path", statusoppath + "/" + c);
        }else{
            map.put("path", statusoppath);
        }
        map.put("op", "addChildrenDataListener");
        remotestore.doSomeThing(null,null,map,null,null);
        map.put("op", "addChildrenPathListener");
        remotestore.doSomeThing(null,null,map,null,null);

        //add status listener
        if(StringUtils.isNotBlank(c)) {
            map.put("path", statuspath + "/" + c);
        }else{
            map.put("path", statuspath);
        }
        map.put("op", "addChildrenDataListener");
        remotestore.doSomeThing(null,null,map,null,null);
        map.put("op", "addChildrenPathListener");
        remotestore.doSomeThing(null,null,map,null,null);
    }
    void addDataListener(String path)throws Exception{
        HashMap map = new HashMap();
        //add listener
        map.put("path", path);
        map.put("op","addPathDataListener");
        remotestore.doSomeThing(null,null,map,null,null);
    }
    List<String>  localfirst=null;
    public void reConZkInit(){
        try {
            addServiceFromZk(localfirst);
            registerLocalService();
        }catch (Exception e){
            log.error("reconnecte zk init load services error",e);
        }
    }
    /**
     * zk service first load, than load local , local service can not recover zk service .
     * @param srs
     * @throws Exception
     */
    public void init(Map<String,Map<String,XMLMakeup>> srs,Map descs,List<String> localfirstServiceList,boolean isinit,List<String> exclude)throws Exception{
        log.info("----------------begin load actions----------------");
        localfirst=localfirstServiceList;
        /*ISPDictionary dictionary = (ISPDictionary)getObjectById("Dictionary");
        Map<String,Map<String,Map<String,Object>>> ss = dictionary.getServices();
        if(null != ss){
            Iterator<String> its = ss.keySet().iterator();
            while(its.hasNext()){
                String catalog = its.next();
                Map<String,Map<String,Object>> s = ss.get(catalog);
                Iterator<String> it = s.keySet().iterator();
                while(it.hasNext()){
                    try{
                        String name = it.next();
                        Map<String,Object> ps = s.get(name);
                        createService(env,ps);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }*/
        Map<String,String> rss =addServiceFromZk(localfirstServiceList);//zk 中的服务优先级最高,localfirstServiceList中的服务zk不加载

        if(null != srs){
            Iterator<Map<String,XMLMakeup>> it = srs.values().iterator();
            while(it.hasNext()){
                try {
                    List<XMLObject> tmp = new LinkedList();
                    Iterator<XMLMakeup> ss = it.next().values().iterator();
                    while(ss.hasNext()) {
                        XMLMakeup x = ss.next();
                        if(null != exclude && null !=x.getId() && ArrayUtils.isInStringArray(exclude,x.getId())){
                            continue;
                        }
                        if(null == rss || !rss.containsKey(x.getId()) ||(null != localfirstServiceList && localfirstServiceList.contains(x.getId()))) {
                            if (null == localService) localService = new ArrayList<String>();
                            Map desc = null;
                            if (null != descs) {
                                desc = (Map) descs.get(x.getId());
                            }

                            XMLObject o = addService(x, desc);
                            if (null != o) {
                                localService.add(o.getXML().getId());
                                tmp.add(o);
                            } else {
                                log.error("load service is null or disable:" + x);
                            }

                        }
                        /*if(null != remotestore && null!=remotepath){
                            String optype = x.getProperties().getProperty("opType");
                            Map data= Desc.getDescStructure(x.getId());
                            if(null != data) {
                                HashMap in = new HashMap();
                                in.put("op","setData");
                                if(StringUtils.isNotBlank(optype)) {
                                    in.put("path", remotepath + "/" + x.getProperties().getProperty("opType") + "/" + x.getId());
                                }else{
                                    in.put("path", remotepath + "/" + "DEFAULT" + "/" + x.getId());
                                }
                                in.put("data", ObjectUtils.convertMap2String(data));
                                remotestore.doSomeThing(null, null, in, null, null);
                                log.info("syn service [" + x.getId() + "] to remote store.");
                            }
                        }*/

                    }
                    //todo maybe todo each xmlobject initial() method
                    if(isinit && tmp.size()>0){
                        for(XMLObject o:tmp){
                            o.initial();
                        }
                    }

                }catch (Exception e){
                    log.error("convert service String to XMLMakeup error", e);
                }

            }
            if(null !=stathandler){
                registerLocalService();
            }
        }

        log.info("----------------end load actions----------------");

    }
    synchronized void  registerLocalService()throws Exception{
        if(null != localService && localService.size()>0){
            for(String s:localService){
                try {
                    HashMap in = new HashMap();
                    in.put("op", "initStatInfo");
                    in.put("name", s);
                    if (null != stathandler) {
                        stathandler.doSomeThing(null, null, in, null, null);
                        XMLObject o = getObjectById(s);
                        setActive( s, o.isActive());
                        log.debug("stat local service:"+s);
                    }
                }catch (Exception e){

                }
            }
        }
    }
    String getSelfInstanceId(){
        Bridge b = (Bridge)getObjectById("bridge");
        return b.getInstanceId();
    }
    Map<String,String> addServiceFromZk(List<String> localfirst)throws Exception{
        Map<String,String> rss = new LinkedHashMap<String, String>();
        // get services from remote center
        if(null != remotestore && null != remotepath){
            HashMap map = new LinkedHashMap();
            //root services , busitype services, this instance name services
            String insid = getSelfInstanceId();
            map.put("path", remotepath+"/"+insid);
            map.put("op", "getChildrenData");
            Map<String,String> ts = (Map)remotestore.doSomeThing(null,null,map,null,null);
            if(null != ts) {
                rss.putAll(ts);
            }
            addListeners(insid);
            if(StringUtils.isNotBlank(optype)) {
                map.put("path", remotepath+"/"+optype);
                map.put("op", "getChildrenData");
                Map<String,String> tt = (Map)remotestore.doSomeThing(null,null,map,null,null);
                if(null != tt) {
                    Iterator<String> its = tt.keySet().iterator();
                    while(its.hasNext()){
                        String k = its.next();
                        if(!rss.containsKey(k)){
                            rss.put(k,tt.get(k));
                        }
                    }
                }
                addListeners(optype);
            }else{
                addListeners("");
                map.put("path", remotepath);
                map.put("op", "getChildren");
                List<String> s = (List)remotestore.doSomeThing(null,null,map,null,null);
                if(null != s){
                    for(String c:s) {
                        if(c.startsWith("INS-")) continue;//filter out other refer insids services
                        map.put("path", remotepath + "/" + c);
                        map.put("op", "getChildrenData");
                        Map te = (Map)remotestore.doSomeThing(null,null,map,null,null);
                        if(null != te && te.size()>0){
                            //addListeners(c);
                            rss.putAll(te);
                        }else{
                            map.put("path", remotepath + "/" + c);
                            map.put("op", "getData");
                            String ser = (String)remotestore.doSomeThing(null,null,map,null,null);
                            if(!rss.containsKey(c)) {
                                rss.put(c, ser);
                            }
                            addDataListener(remotepath + "/" + c);
                        }

                        addListeners(c);
                    }
                }
            }
            log.info("local first services "+localfirst);
            if(null != rss){
                Iterator<String> its = rss.keySet().iterator();
                while(its.hasNext()){
                    String k = its.next();
                    String b = rss.get(k);
                    if (null == getObjectById(k)) {
                        if(null != b) {
                            Map des = StringUtils.convert2MapJSONObject(b);
                            des = Desc.removeNotServiceProperty(des);
                            if(null != des && des.size()>0 ) {
                                if((null ==localfirst || !localfirst.contains(des.get("name")))) {
                                    HashMap cond = new HashMap();
                                    cond.put("op", "getData");
                                    String r = getname(statusoppath, (String) des.get("opType"), (String) des.get("package"), (String) des.get("name"));
                                    cond.put("path", r);
                                    Object st = remotestore.doSomeThing(null, null, cond, null, null);
                                    boolean active = true;
                                    if (null != st) {
                                        if (st instanceof String) {
                                            Map m = StringUtils.convert2MapJSONObject((String) st);
                                            if (null != m) {
                                                if ("publishSuspendService".equals(m.get("op"))) {
                                                    active = false;
                                                }
                                            }
                                        }
                                    }
                                    addDataListener(r);
                                    addServiceByDesc(des, active);
                                    log.info("create local service from zk ["+des.get("name")+"]" + k);
                                }
                            }else{
                                log.error("can not load service:\n"+b);
                            }
                        }
                    }
                }
            }

        }
        return rss;
    }

    String getname(String root,String optype,String pk,String name){
        String r=root;
        if(StringUtils.isNotBlank(optype)){
            r+="/"+optype;
        }
        boolean is=false;
        if(StringUtils.isNotBlank( pk)){
            r+="/"+pk;
            is=true;
        }
        if(is){
            r += "." + name;
        }else {
            r +=  name;
        }
        return r;
    }
    /*void createService(XMLParameter env,Map ps)throws Exception{
        String bodyType = getStr(ps,"BODY_TYPE");
        if(StringUtils.isNotBlank(bodyType)){
            //add service to isp
            String body = getStr(ps,"BODY");
            IBodyCreator bc = ls.get(bodyType);
            if(null != bc)
                body = bc.createBody(body);
            if(StringUtils.isNotBlank(body)){
                XMLObject o = XMLObject.createXMLObject(XMLUtil.getDataFromString(body),null,this);
                o.setProperty("key",getStr(ps,"NAME"));
                System.out.println("load action: "+getStr(ps,"NAME"));
            }

            //add service to ws
            if(null != env.getParameter("WSContext")){
                ServiceInfo si = new ServiceInfo((String)ps.get("busi_class"),(String)ps.get("catalog"),(String)ps.get("name"),new String[]{(String)ps.get("parameter_type")},(String)ps.get("return_type"));

                WSISPDeployer.addService((ConfigurationContext)env.getParameter("WSContext"),si);

            }
        }
    }*/
    boolean checkDesc(Map desc)throws Exception{
        if(desc.containsKey("opType") && null!= desc.get("opType") && ((String)desc.get("opType")).startsWith("INS-")){
            throw new Exception("the service desc opType is not must start with [INS-]");
        }
        return true;
    }
    void addServiceByDesc(Map desc,boolean isactive)throws Exception{
        try {
            checkDesc(desc);
            createXMLObjectByDesc(Desc.removeNotServiceProperty(desc), null, this, isactive, getSingleContainers());

            HashMap in = new HashMap();
            in.put("op", "initStatInfo");
            in.put("name", desc.get("name"));
            if (null != stathandler) {
                stathandler.doSomeThing(null, null, in, null, null);
                setActive((String) desc.get("name"), isactive);
                log.debug("stat local service:"+desc.get("name"));
            }
        }catch (Exception e){
            log.error("load service error "+(null == desc?"name is null":desc.get("name")),e);
        }
    }
    //add service to container
    XMLObject addService(XMLMakeup serviceBody,Map desc)throws Exception{
        XMLObject o  = createXMLObject(serviceBody,null,this,getSingleContainers());
        if(null != o) {
            if (null != desc) {
                addInvokeDescStructure(desc);
            }
            log.info("has loaded service " + serviceBody.getId());
            return o;
        }else {
            return null;
        }
    }
    void setActive(String name,boolean isa)throws Exception{
        if(null != remotestore){
            Bridge b = (Bridge)getObjectById("bridge");
            HashMap map = new HashMap();
            map.put("path", SystemAction.getPublishStateZkPath(statuspath, b.getInstanceId(), name));
            map.put("data",String.valueOf(isa));
            map.put("op","onlySetData");
            remotestore.doSomeThing(null, null, map, null, null);
            map.clear();
            map.put("path", SystemAction.getPublishStateZkPath(statusoppath, b.getInstanceId(), name));
            map.put("op","addPathDataListener");
            remotestore.doSomeThing(null,null,map,null,null);
        }
    }
    void remoteService(String srvName)throws Exception{
        if(StringUtils.isNotBlank(srvName)) {
            if(removeXMLObjectById(srvName))
                log.info("has remoted service: "+srvName);
        }
    }
    void updateService(XMLMakeup serviceBody)throws Exception{
        if(null !=serviceBody){
            remoteService(serviceBody.getId());
            addService(serviceBody,null);

            System.out.println("has updated service: "+serviceBody.getId());
        }

    }
    @Override
    public Object doSomeThing(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(null != input && input.size()>0){
            String op = (String)input.get("op");
            if(op.equals("add")){
                String m = (String)input.get("data");
                try {
                    addService(XMLUtil.getDataFromString(m),null);
                }catch (Exception e){
                    log.error("when add service , convert string to XMLMakeup error",e);
                }
            }else if(op.equals("delete")){
                String srv = (String)input.get("data");
                try {
                    remoteService(srv);
                }catch (Exception e){
                    log.error("remove service["+srv+"] error",e);
                }
            }else if(op.equals("update")){
                String m = (String)input.get("data");
                try {
                    updateService(XMLUtil.getDataFromString(m));
                }catch (Exception e){
                    log.error("when update service , convert string to XMLMakeup error",e);
                }
            }else if("init".equalsIgnoreCase(op) && null != input.get("services")){
                if(null !=input.get("services") && input.get("services") instanceof Map ) {
                    //log.info("add services"+input.get("services"));
                    List<String> first=null;
                    if(null != input.get("localfirst") && input.get("localfirst") instanceof List){
                        first=(List)input.get("localfirst");
                    }
                    boolean isinit=false;
                    if(null != input.get("isDoInit") && StringUtils.isTrue((String)input.get("isDoInit"))){
                        isinit=true;
                    }
                    init((Map) input.get("services"),(Map) input.get("descs"),first,isinit,(List)input.get("exclude"));
                }
            }else if("reConZkInit".equals(op)){
                log.info("reload Services form zk by reconnect zk ");
                reConZkInit();
            }
        }
        return null;
    }

    @Override
    public void doInitial() throws Exception {
        /*String s = getXML().getProperties().getProperty("input");
        if(StringUtils.isNotBlank(s)) {
            Map parjs = StringUtils.convert2MapJSONObject(s);
            Map input = XMLParameter.getMapValueFromParameter(null,parjs,this);
            if((null == input.get("services") || (null != input.get("services") && input.get("services") instanceof Map ))
                && (null == input.get("descs") ||(null != input.get("descs") && input.get("descs") instanceof Map))) {
                init((Map) input.get("services"), (Map) input.get("descs"),null);
            }
        }*/
    }

    @Override
    public boolean checkInput(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;
    }

    @Override
    public ResultCheck checkReturn(String xmlid,XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return new ResultCheck(true,null);
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config,Object ret,Exception e) throws Exception {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
