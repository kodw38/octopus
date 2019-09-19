package com.octopus.isp.actions;

import com.octopus.isp.bridge.impl.Bridge;
import com.octopus.isp.ds.RequestParameters;
import com.octopus.tools.dataclient.v2.ds.TableContainer;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.bftask.BFParameters;
import com.octopus.utils.cachebatch.DateTimeUtil;
import com.octopus.utils.cls.proxy.GeneratorClass;
import com.octopus.utils.exception.ExceptionUtil;
import com.octopus.utils.exception.ISPException;
import com.octopus.utils.file.FileUtils;
import com.octopus.utils.net.NetUtils;
import com.octopus.utils.si.jvm.JVMUtil;
import com.octopus.utils.time.DateTimeUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import com.octopus.utils.xml.desc.Desc;
import com.octopus.utils.zip.ZipUtil;
import com.sun.tools.javac.code.Attribute;
import com.sun.xml.internal.stream.writers.XMLDOMWriterImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipOutputStream;

/**
 * User: wfgao_000
 * Date: 16-3-20
 * Time: 下午8:56
 */
public class SystemAction extends XMLDoObject {
    private static transient Log log = LogFactory.getLog(SystemAction.class);
    static String INSID_SRVNAME_SPLITCHAR=".";
    XMLDoObject srvhandler;
    XMLDoObject srvstat;
    XMLDoObject tracelog;
    XMLDoObject command;
    XMLDoObject resource;
    XMLDoObject auth;
    XMLDoObject remote;
    String servicepath; //{op_type.package.name:service desc map}
    String statpath;
    String statuspath;
    String statusstorepath;
    String traceFlagPath;
    String serverspath;
    String simreturndir;
    String sipath;
    List<String> servicesearch;
    Map<String,List<Map>> srvIdRelIns = new ConcurrentHashMap<String, List<Map>>(); //Key is SrvId, Map is Ins info
    Map<String,List<Map>> srvInfoInCenter = new ConcurrentHashMap<String, List<Map>>(); //Key is SrvId, Map is srv info in each ins
    List<String> initpublish;
    public SystemAction(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        servicepath = xml.getProperties().getProperty("servicepath");
        statpath = xml.getProperties().getProperty("statpath");
        statuspath = xml.getProperties().getProperty("statusoppath");
        statusstorepath = xml.getProperties().getProperty("statuspath");
        traceFlagPath = xml.getProperties().getProperty("traceflagpath");
        serverspath = xml.getProperties().getProperty("serverspath");
        sipath = xml.getProperties().getProperty("sipath");
        String s = xml.getProperties().getProperty("servicesearch");
        if(StringUtils.isNotBlank(s)){
            servicesearch=Arrays.asList(s.split("\\,"));
        }
        if(StringUtils.isNotBlank(xml.getProperties().getProperty("simreturndir"))) {
            simreturndir = (String) getEmptyParameter().getValueFromExpress(xml.getProperties().getProperty("simreturndir"),this);
        }

    }
    public void doInitial(){
        try {
            if (StringUtils.isNotBlank(traceFlagPath)) {
                HashMap map = new HashMap();
                map.put("op", "addPathDataListener");
                map.put("path", traceFlagPath);
                srvhandler.doSomeThing(null, null, map, null, null);
            }
            initRegInfo(false);
            if (StringUtils.isNotBlank(getXML().getProperties().getProperty("initpublish"))) {
                Object o = getEmptyParameter().getValueFromExpress(getXML().getProperties().getProperty("initpublish"), this);
                if (o instanceof List) {
                    initpublish = (List) o;
                }
            }
            if (null != initpublish) {
                for (String s : initpublish) {
                    try {
                        Map m = getDescStructure(s);
                        if (null != m) {
                            String p = getServicePath((String)m.get("opType"),(String)m.get("package"),(String)m.get("name"));
                            Map map = new HashMap();
                            map.put("op","isExist");
                            map.put("path",p);
                            Object o = srvhandler.doSomeThing(null, null, map, null, null);
                            if(null != o && o instanceof Boolean && (Boolean)o) {
                                map.put("op", "getData");
                                String txt = (String)srvhandler.doSomeThing(null, null, map, null, null);
                                Map body = StringUtils.convert2MapJSONObject((String) txt);
                                if(DateTimeUtil.getDate((String)body.get("date")).before(DateTimeUtil.getDate((String)m.get("date")))){
                                    publishAddUpdateService(null, m);
                                    log.info("init publish " + s);
                                }
                            }
                        }
                    } catch (Exception e) {

                    }
                }
            }

            Timer t = new Timer();
            t.schedule(new TimerTask() {
                @Override
                public void run() {

                    try{
                        initRegInfo(true);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    try {
                        //load all services to local
                        Map<String, List<Map>> srvs = new HashMap();
                        Map<String, List<Map>> li = getSrvInfoRelIns(srvs);// when ins remove or srv add delete will notification this store
                        if (null != li) {
                            if (null != srvIdRelIns) {
                                synchronized (srvIdRelIns) {
                                    srvIdRelIns = li;
                                }
                            } else {
                                srvIdRelIns = li;
                            }
                        }
                        if (null != srvInfoInCenter) {
                            synchronized (srvInfoInCenter) {
                                srvInfoInCenter = srvs;
                            }
                        } else {
                            srvInfoInCenter = srvs;
                        }
                    }catch (Exception e){
                        log.error("",e);
                    }
                }
            },0,300000);
        }catch (Exception x){
            log.error("",x);
        }
    }

    void initRegInfo(boolean isReCon){
        try {
            if(srvhandler==null)return ;
            log.debug("java.library.path\n"+System.getProperty("java.library.path"));


            //reg this instance info to zk
            if(StringUtils.isNotBlank(serverspath)){
                Bridge root = (Bridge)getObjectById("bridge");
                if(null != root){
                    HashMap map = new HashMap();
                    String id = root.getInstanceId();
                    map.put("op", "isExist");
                    map.put("path", serverspath+"/"+id);
                    Boolean b = (Boolean)srvhandler.doSomeThing(null, null, map, null, null);
                    XMLParameter env = getEmptyParameter();
                    if(null != b && !b) {
                        if (!b || id.contains("CONSOLE") || isReCon) {
                            if(id.contains("CONSOLE")){
                                try {
                                    Thread.sleep(2000);
                                }catch (Exception e){}
                            }
                            map.put("op", "addPathListener");
                            map.put("path", serverspath);
                            srvhandler.doSomeThing(null, env, map, null, null);
                            map.clear();
                            map.put("op", "addPath");
                            map.put("path", serverspath + "/" + id);
                            map.put("type", "temp");
                            srvhandler.doSomeThing(null, null, map, null, null);
                            map.put("op", "onlyWriteData");
                            String logpath=(String)((Map)env.get("${env}")).get("logDir")+"cur";
                            if(logpath.startsWith("./") || logpath.startsWith("../")){
                                File f = new File("");
                                logpath = f.getAbsolutePath()+"/"+logpath;
                                log.info("log path is "+logpath);
                            }
                            Map properties = getEnvProperties();
                            String webport= (String)properties.get("webport");
                            String wshost = (String)properties.get("ws_host");

                            String twp = System.getProperty("tb-webport");
                            if(StringUtils.isNotBlank(twp)){
                                webport =twp;
                            }
                            String twsp = System.getProperty("tb-wsaddress");
                            if(StringUtils.isNotBlank(twsp)){
                                wshost =twsp;
                            }
                            map.put("data", "{\"insId\":\""+id+"\",\"ip\":\"" + NetUtils.getip() +"\",\"port\":\""+webport+"\",\"ws_host\":\""+wshost+"\",\"loginuser\":\""+properties.get("logUserName")+"\",\"loginpwd\":\""+properties.get("logPassword")+"\",\"jmxRemotePort\":\""+System.getProperty("com.sun.management.jmxremote.port")+"\",\"pid\":\"" + JVMUtil.getPid() + "\",\"logPath\":\""+logpath+"\"}");
                            srvhandler.doSomeThing(null, null, map, null, null);
                            log.info("syn center info,register ins:" + map.get("data"));

                        } else {
                            throw new Exception("syn center info,the server has exist [" + id + "] in cluster");
                        }
                    }

                }

            }


        }catch (Exception e){
            log.error("systemAction init error",e);
        }
    }
    //init rel by srvlist, key is service name, map is ins info
    Map<String,List<Map>> getSrvInfoRelIns(Map<String,List<Map>> getSrvs) throws Exception{
        try {
            //service inovke info in zk, all service init will sync the info
            if(null != srvhandler) {
                HashMap in = new HashMap();
                in.put("op", "getChildrenData");
                in.put("path", statpath);
                //all service stat data
                Map<String, String> servicesStatus = (Map) srvhandler.doSomeThing(null, null, in, null, null);
                if (log.isDebugEnabled()) {
                    log.debug("all services of stat data\n" + servicesStatus);
                }
                if (null != servicesStatus) {
                    List<Map> activeIns = getInsList();//获取活动的实例名称
                    Map<String, Map> insNames = new HashMap();
                    if (null != activeIns) {
                        for (Map n : activeIns) {
                            if (log.isInfoEnabled()) {
                                log.info("get active instance :" + n);
                            }
                            if (StringUtils.isNotBlank(n.get("insId"))) {
                                insNames.put((String) n.get("insId"), n);
                                n.put("INS_ID", n.get("insId"));
                            }
                        }
                    }
                    HashMap tempLocalCache = new HashMap();
                    Map<String, List<Map>> ret = new ConcurrentHashMap();
                    Iterator<String> its = servicesStatus.keySet().iterator();
                    Bridge thisroot = (Bridge) getObjectById("bridge");
                    while (its.hasNext()) {
                        String k = its.next();//.INS-NJ_115.ProvisioningDomainV1.0Op.
                        String kk = k.substring(0, k.length() - 1);
                        //String na = kk.substring(kk.lastIndexOf(".")+1,kk.length());//0Op
                        String name = kk.substring(kk.indexOf(".", 2) + 1, kk.length());//0Op
                        String v = servicesStatus.get(k);
                        Map m = StringUtils.convert2MapJSONObject(v);
                        if (null != m) {
                            if (insNames.containsKey(m.get("INS_ID")) && StringUtils.isTrue((String) m.get("isable"))
                                    && StringUtils.isNotBlank(m.get("PID"))
                            ) {
                                if (!ret.containsKey(name)) ret.put(name, new ArrayList());
                                ret.get(name).add(insNames.get(m.get("INS_ID")));

                                if (!getSrvs.containsKey(name)) getSrvs.put(name, new ArrayList());
                                getSrvs.get(name).add(m);

                                updateLocalCacheOfRemoteDesc(tempLocalCache,name,(String)m.get("INS_ID"),"ADD");

                            }
                        }

                    }
                    synchronized (localCacheOfRemoteDesc){
                        localCacheOfRemoteDesc.clear();
                        localCacheOfRemoteDesc.putAll(tempLocalCache);
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("load service in instances\n" + ret);
                    }
                    return ret;
                } else {
                    log.info("load zero stat service from zk path:" + statpath);
                }
            }

        }catch (Exception e){
            throw e;
        }

        return null;

        /*List<Map> ls = findServicesByCenter(null);
        if (null != ls) {
            Map<String, List<Map>> ret = new ConcurrentHashMap();
            List<Map> activeIns = getInsList();//获取活动的实例名称
            List<String> insNames = new ArrayList();
            if (null != activeIns) {
                for (Map n : activeIns) {
                    if (StringUtils.isNotBlank(n.get("insId"))) {
                        insNames.add((String) n.get("insId"));
                    }
                }
                String srvName, insId;
                for (Map a : ls) {
                    srvName = (String) a.get("NAME");
                    //if(StringUtils.isTrue((String)a.get("IS_PUBLISH"))) {
                        List<Map> cs = (List) a.get("CHILDREN");
                        if (null != cs) {
                            for (Map c : cs) {
                                insId = (String) c.get("INS_ID");
                                if (insNames.contains(insId) && StringUtils.isNotBlank(c.get("PID"))) {
                                    if (!ret.containsKey(srvName)) ret.put(srvName, new LinkedList<Map>());
                                    HashMap t = new HashMap();
                                    t.put("INS_ID", insId);
                                    ret.get(srvName).add(t);
                                }
                            }
                        }
                    //}
                }
            }

            return ret;
        }else{
            return new ConcurrentHashMap();
        }*/

    }
    void notifyByAddSrv(String srvName,String[] insId){
        if(log.isDebugEnabled()) {
            log.debug("syn center info, notifyByAddSrv " + srvName + " " + ArrayUtils.toJoinString(insId));
        }
        addInsInBySrv(srvName, insId);
    }
    String[] getInsNameBySrvId(String srvName){
        /*List<Map> ret = findServicesByCenter(srvName);
        if(null != ret){
            List<String> tt = new ArrayList();
            for(Map m:ret){
                if(m.get("NAME").equals(srvName)){
                    List<Map> ls = (List)m.get("CHILDREN");
                    if(null != ls){
                        for(Map t:ls){
                            if(!tt.contains(t.get("INS_ID"))){
                                tt.add((String)t.get("INS_ID"));
                            }
                        }
                    }
                }
            }
            return (String[])tt.toArray(new String[0]);
        }
        return null;*/
        List<Map> list = srvIdRelIns.get(srvName);
        if(null != list){
            List ret = new ArrayList();
            for(Map m:list){
                ret.add(m.get("insId"));
            }
            return (String[])ret.toArray(new String[0]);
        }
        return null;
    }
    void notifyByActiveSrv(String srvName,String[] insId){
        if(null == insId){
            insId = getInsNameBySrvId(srvName);
        }

        addInsInBySrv(srvName,insId);
        if(log.isInfoEnabled()) {
            log.info("syn center info, notifyByActiveSrv " + srvName + " " + ArrayUtils.toJoinString(insId));
        }
    }
    void notifyByRemoveSrv(String srvName,String insId){
        if(log.isInfoEnabled()) {
            log.info("syn center info, notifyByRemoveSrv " + srvName + " " + insId);
        }
        synchronized (srvIdRelIns) {
            if (StringUtils.isBlank(insId)) {//if insId is null means remove srv in all instances
                srvIdRelIns.remove(srvName);
                srvInfoInCenter.remove(srvName);
            } else {
                removeInsInBySrv(srvName, insId);
                removeSrvInfoFromCache(srvName, insId);
            }
        }
        if(addressmap.size()>0){
            String k = getAddressMapKey(insId,srvName);
            if(addressmap.containsKey(k)){
                addressmap.remove(k);
            }
        }
    }
    void notifyBySuspendSrv(String srvName,String insId){
        if(log.isInfoEnabled()) {
            log.info("syn center info, removeSrv " +srvName+ " from " + insId);
        }
        synchronized (srvIdRelIns) {
            if (StringUtils.isBlank(insId)) {//if insId is null means remove srv in all instances
                srvIdRelIns.remove(srvName);
                srvInfoInCenter.remove(srvName);
            } else {
                removeInsInBySrv(srvName, insId);
                removeSrvInfoFromCache(srvName,insId);

            }
            if (log.isDebugEnabled()) {
                log.info("syn center info, notifyBySuspendSrv " + srvName + " " + insId);
            }
        }
        if(addressmap.size()>0){
            String k = getAddressMapKey(insId,srvName);
            if(addressmap.containsKey(k)){
                addressmap.remove(k);
            }
        }
    }
    void  notifyByRemoveIns(String insId){
        if(log.isInfoEnabled()) {
            log.info("syn center info, notifyByRemoveIns " + " from " + insId);
        }
        if(null != srvIdRelIns) {
            synchronized (srvIdRelIns) {
                Iterator<String> name = srvIdRelIns.keySet().iterator();
                while (name.hasNext()) {
                    String n = name.next();
                    removeInsInBySrv(n, insId);
                }
            }
        }
        if(null != srvInfoInCenter){
            synchronized (srvInfoInCenter){
                Iterator<String> name = srvInfoInCenter.keySet().iterator();
                while(name.hasNext()){
                    String n = name.next();
                    removeSrvInfoFromCache(n,insId);
                }
            }
        }
        if(addressmap.size()>0){
            Iterator its = addressmap.keySet().iterator();
            while(its.hasNext()){
                String k = (String)its.next();
                if(k.contains(insId)){
                    addressmap.remove(k);
                }
            }
        }
    }
    void removeInsInBySrv(String srvName,String insId){
        if(log.isDebugEnabled()) {
            log.info("syn center info, removeInsInBySrv " +srvName+ " from " + insId);
        }
        synchronized (srvIdRelIns) {
            List<Map> t = srvIdRelIns.get(srvName);
            if (null != t) {
                for (int i = t.size() - 1; i >= 0; i--) {
                    if (t.get(i).get("INS_ID").equals(insId)) {
                        t.remove(i);
                    }
                }
            }
        }

    }
    void removeSrvInfoFromCache(String srvName,String insId){
        synchronized (srvInfoInCenter){
            List<Map> t = srvInfoInCenter.get(srvName);
            if(null != t){
                for(int i=t.size()-1;i>=0;i--){
                    if(t.get(i).get("INS_ID").equals(insId)){
                        t.remove(i);
                    }
                }
            }
        }
    }
    void addSrvInfoToCacheByAddSrv(XMLParameter env,Map desc){
        if(null != desc) {
            if (!srvInfoInCenter.containsKey(desc.get("name"))) {
                srvInfoInCenter.put((String) desc.get("name"), new LinkedList<Map>());
            }
            Map t = getStatMap(env,desc);
            String selfid = getSelfInstanceId();
            List<Map> m = srvInfoInCenter.get(desc.get("name"));
            if(null != m && m.size()>0){
                for(int i=m.size()-1;i>=0;i--){
                    if(m.get(i).get("INS_ID").equals(selfid)){
                        m.remove(i);
                    }
                }
            }
            if(null != t) {
                srvInfoInCenter.get(desc.get("name")).add(t);
            }
        }
    }
    Map getStatMap(XMLParameter env,Map desc){
        if(null != desc) {
            HashMap ret = new HashMap();
            ret.put("INS_ID",getSelfInstanceId());
            ret.put("IP",((Map)env.get("${env}")).get("${ip}"));
            ret.put("PID",((Map)env.get("${env}")).get("${pid}"));
            ret.put("name",desc.get("name"));
            ret.put("opType",desc.get("opType"));
            ret.put("path",desc.get("path"));
            ret.put("alarm",desc.get("alarm"));
            ret.put("redo",getRedoFlag(desc.get("redo")));
            ret.put("share",desc.get("share"));
            ret.put("package",desc.get("package"));
            ret.put("author",desc.get("author"));
            return ret;
        }
        return null;
    }
    String getRedoFlag(Object o){
        if(null != o) {
            if (o instanceof String) {
                return (String) o;
            } else if (o instanceof Map) {
                return "true";
            }
        }
        return "false";
    }
    void addInsInBySrv(String srvName,String[] insId){
        if(log.isDebugEnabled()) {
            if(null != insId) {
                log.info("syn center info, addInsInBySrv " + srvName + " to ins " + ArrayUtils.toJoinString(insId));
            }
        }
        if(null != srvIdRelIns) {
            synchronized (srvIdRelIns) {
                List<Map> t = srvIdRelIns.get(srvName);
                if (null == t) {
                    srvIdRelIns.put(srvName, new LinkedList());
                    t = srvIdRelIns.get(srvName);
                }

                if (null != t) {
                    if (null != insId && insId.length > 0) {
                        for (String id : insId) {
                            putIns(id, t);
                        }
                    } else {
                        putIns(getSelfInstanceId(), t);
                    }
                }
            }
        }

    }

    void putIns(String id,List<Map> t){
        if(null != t && null != id) {
            boolean isin = false;
            for (Map m : t) {
                if (m.get("INS_ID").equals(id)) {
                    isin = true;
                    break;
                }
            }

            if (!isin) {
                HashMap itm = new HashMap();
                itm.put("INS_ID", id);
                t.add(itm);
            }
        }
    }

    /**
     * get all service name in xml container
     * @return
     */
    String[] getAllServices(){
        LinkedList<String> ls = new LinkedList<String>();
        Map<String,XMLObject> objs = getXMLObjectContainer();
        if(null!=objs){
            Iterator<String> aname = objs.keySet().iterator();
            while(aname.hasNext()) {
                String name = aname.next();
                XMLObject parent = objs.get(name).getParent();
                if (null != parent && (parent.getXML().getId().startsWith("Load") || parent.getXML().getId().startsWith("load"))) {
                    ls.add(name);
                } else if(null != objs.get(name).getXML().getParent() && objs.get(name).getXML().getParent().getName().startsWith("actions")){
                    ls.add(name);
                }
            }
        }
        return (String[])ls.toArray(new String[0]) ;
    }
    List<Map> getStatByCenter(String opTypeOrInsId,String name,String[] insids,Map<String,String> servicesStatus,List<String> usedList){
        try {
            if(null != servicesStatus){
                List<Map> ret = new LinkedList<Map>();
                Iterator<String> ist = servicesStatus.keySet().iterator();
                while(ist.hasNext()){
                    String k = ist.next();
                    String insid = k.substring(1,k.indexOf(".",2));
                    //.INS-NJ_115.ProvisioningDomainV1.0Op.  INS-NJ_115,INS-CONSOLE:INS-NJ_115 result:false
                    log.debug(opTypeOrInsId+" "+name+" query cond "+k+"  " + ArrayUtils.toJoinString(insids) + ":" + insid + " result:"
                         + ((StringUtils.isBlank(opTypeOrInsId) || !opTypeOrInsId.startsWith("INS-")
                            || (opTypeOrInsId.startsWith("INS-") && k.startsWith("." + opTypeOrInsId + "."))) && k.contains("." + name + ".")));
                    if(ArrayUtils.isInStringArray(insids,insid)) {
                        if ((StringUtils.isBlank(opTypeOrInsId) || !opTypeOrInsId.startsWith("INS-") || (opTypeOrInsId.startsWith("INS-") && k.startsWith("." + opTypeOrInsId + "."))) && k.contains("." + name + ".")) {
                            log.debug("add service to result "+k);
                            ret.add(StringUtils.convert2MapJSONObject((String) servicesStatus.get(k)));
                            if (null != usedList) {
                                usedList.add(k);
                            }
                        }
                    }
                }
                return ret;
            }
            return null;
        }catch (Exception e){
            log.error("getStatByCenter",e);
        }
        return null;
    }
    Map getServiceInfoAndStatus(String id,Map st,List<Map> cs,Map<String,Boolean> servicesStatus ){
        HashMap map = new HashMap();
        map.put("NAME", id);
        if(null != st) {
            map.put("PACKAGE", st.get("package"));
            map.put("REDO", getRedoFlag(st.get("redo")));
            map.put("SHARE", st.get("share"));
            map.put("IS_ALARM",isAlarm(st));
            map.put("IS_WORKTIME",isWorktime(st));
            map.put("PATH", st.get("path"));
            map.put("OP_TYPE", st.get("opType"));
            map.put("CREATE_BY", st.get("createby"));
            map.put("AUTHOR", st.get("author"));
        }else{
            if(null != cs && cs.size()>0) {
                Map m = cs.get(0);
                if(null != m){
                    map.put("PACKAGE", m.get("package"));
                    map.put("REDO", getRedoFlag(m.get("redo")));
                    map.put("SHARE", m.get("share"));
                    map.put("IS_ALARM",m.get("isalarm"));
                    map.put("PATH", m.get("path"));
                    map.put("OP_TYPE", m.get("opType"));
                    map.put("CREATE_BY", m.get("date"));
                    map.put("AUTHOR", m.get("author"));
                }
            }
        }
        map.put("IS_PUBLISH", "Y");

        map.put("IS_LOCAL",getObjectById(id)!=null);
        try {
            if(null != st) {
                if(null !=servicesStatus && servicesStatus.size()>0){
                    String k =  getZkPath("",(String)st.get("opType"),(String)st.get("package"),id);
                    if(null !=servicesStatus.get(k)){
                        map.put("STATUS", servicesStatus.get(k));
                    }
                }
                if(null == map.get("STATUS")) {
                    boolean isaa = isPublishActive((String) st.get("opType"), (String) st.get("package"), id);
                    map.put("STATUS", isaa);
                }
            }
        } catch (Exception e) {
           // map.put("STATUS", "NOT EXIST");
        }
        //map.put("IS_LOCAL", "No");
        if (null != cs) {
            //set instance include the service info
            map.put("CHILDREN", cs);
            long count = 0, error = 0, time = 0, size = 0;
            //sum the instances invoke count
            for (Map c : cs) {
                if (null != c.get("INVOKE_COUNT") && !"null".equals(c.get("INVOKE_COUNT"))) {
                    count += getINVOKE_COUNTData(c,"INVOKE_COUNT");
                }
                if (null != c.get("INVOKE_ERROR_COUNT") && !"null".equals(c.get("INVOKE_ERROR_COUNT"))) {
                    error += getINVOKE_COUNTData(c,"INVOKE_ERROR_COUNT");
                }
                if (null != c.get("INVOKE_COST_TIME") && !"null".equals(c.get("INVOKE_COST_TIME"))) {
                    time += getINVOKE_COUNTData(c,"INVOKE_COST_TIME");
                }
                if (null != c.get("INVOKE_SIZE") && !"null".equals(c.get("INVOKE_SIZE"))) {
                    size += getINVOKE_COUNTData(c,"INVOKE_SIZE");
                }
                if(null != st) {
                    c.put("IS_PUBLISH", "Y");
                }else{
                    c.put("IS_PUBLISH", "N");
                }
                try {
                    String r = getPublishStateZkPath("",(String) c.get("INS_ID"), id);
                    if(null != servicesStatus && servicesStatus.containsKey(r)){
                        c.put("STATUS", servicesStatus.get(r));
                    }else {
                        boolean isa = isPublishOneActive((String) c.get("INS_ID"), id);
                        c.put("STATUS", isa);
                    }
                } catch (Exception e) {
                   // map.put("STATUS","NOT EXIST");
                }

            }
            map.put("INVOKE_COUNT", count);
            map.put("INVOKE_ERROR_COUNT", error);
            map.put("INVOKE_COST_TIME", time);
            map.put("INVOKE_SIZE", size);
            if(null != st) {
                map.put("DATE", st.get("date"));
            }
        }
        return map;
    }
    long getINVOKE_COUNTData(Map d ,String key){
        if (null != d.get(key) && !"null".equals(d.get(key)) && !"".equals(d.get(key))) {
            if(d.get(key) instanceof String) {
                if("".equals(d.get(key))){
                    return 0;
                }else {
                    return Long.valueOf((String) d.get(key));
                }
            }else if(d.get(key) instanceof Integer){
                return (Integer) d.get(key);
            }else if(d.get(key) instanceof Long){
                return (Long) d.get(key);
            }
        }
        return 0;
    }
    void appendFindServiceByPublish(String opTypeOrInsId,String desc,List<Map> ret,List retName,String name,String[] insids,Map<String,String> servicesStat,Map<String,Boolean> servicesStatus,List<String> usedList)throws Exception{
        if(StringUtils.isNotBlank(desc)) {
            String s = desc;
            Map st = com.octopus.utils.alone.StringUtils.convert2MapJSONObject(s);
            if(null == st) st = new HashMap();
            String id = (String) st.get("name");

            if(StringUtils.isNotBlank(id)) {
                if ((StringUtils.isBlank(name) || ismatch(name,id,s))) {
                    List<Map> cs = getStatByCenter(opTypeOrInsId,id,insids,servicesStat,usedList);// find instance by this service name
                    if(log.isDebugEnabled()) {
                        log.debug("find publish srv list:" + cs);
                        log.debug("find like package:" + servicesearch);
                    }
                    Map map = getServiceInfoAndStatus(id,st,cs,servicesStatus);// assemble show data
                    if(!retName.contains(id)) {
                        if(null != servicesearch && servicesearch.size()>0){
                            String pk = (String)st.get("package");
                            if(null ==pk){
                                pk = (String)st.get("PACKAGE");
                            }
                            if(ArrayUtils.isLikeArrayInString(pk,servicesearch)){
                                ret.add(map);
                                retName.add(id);
                            }
                        }else {
                            ret.add(map);
                            retName.add(id);
                        }
                    }else{
                        Map tar = null;
                        for(Map t:ret){
                            if(t.get("NAME").equals(id)){
                                tar = t;
                                break;
                            }
                        }
                        if(null != tar) {
                            List<Map> ls = (List) map.get("CHILDREN");
                            if (null != ls && ls.size() > 0) {
                                for (Map m : ls) {
                                    appendCount(tar, m);
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    List<Map> getStatusServices(String name){
        try {
            List usedList = new ArrayList();
            List<Map> ret = new ArrayList();
            List retName = new ArrayList();
            //add other not in zk and exist status service.
            HashMap in = new HashMap();
            in.put("op", "getChildrenData");
            in.put("path", statpath);
            //all service stat data
            Map<String, String> servicesStat = (Map) srvhandler.doSomeThing(null, null, in, null, null);
            in.clear();
            in.put("op", "getChildrenData");
            in.put("path", statuspath);
            //all service stat data
            Map<String, String> s_servicesStatus = (Map) srvhandler.doSomeThing(null, null, in, null, null);
            Map<String,Boolean> serviceStatus = getSrvStatus(s_servicesStatus);
            if (null != servicesStat && servicesStat.size() > 0) {
                Map<String, Map<String, String>> tem = new HashMap();
                Iterator<String> its = servicesStat.keySet().iterator();
                Bridge thisroot = (Bridge) getObjectById("bridge");
                while (its.hasNext()) {
                    String k = its.next();//.INS-NJ_115.ProvisioningDomainV1.0Op.
                    if (!usedList.contains(k) && !k.contains("." + thisroot.getInstanceId() + ".")) {
                        String kk = k.substring(0, k.length() - 1);
                        //String na = kk.substring(kk.lastIndexOf(".")+1,kk.length());//0Op
                        String na = kk.substring(kk.indexOf(".", 2) + 1, kk.length());//0Op

                        if ((StringUtils.isBlank(name) || (StringUtils.isNotBlank(name) && na.contains(name)))) {
                            if (!tem.containsKey(na)) tem.put(na, new HashMap());
                            tem.get(na).put(k, servicesStat.get(k));
                            if (log.isDebugEnabled()) {
                                log.debug("search service in status info:" + k + " " + servicesStat.get(k));
                            }
                        }
                    }
                }
                String[] insids = getInsListIds();
                Iterator<String> is = tem.keySet().iterator();
                while (is.hasNext()) {
                    String id = is.next();
                    if (log.isDebugEnabled()) {
                        log.debug("query srv not zk prepare stat :" + id + "\n" + tem.get(id));
                    }
                    List<Map> tt = getStatByCenter(null, id, insids, tem.get(id), null);
                    log.debug("get stat by center " + id + " " + tt);
                    Map m = getServiceInfoAndStatus(id, null, tt,serviceStatus);
                    log.debug("get service info and status " + id + " " + m);
                    if (null != m) {
                        m.put("IS_PUBLISH", "N");
                        //m.put("IS_LOCAL", "Yes");
                        if (!retName.contains(id)) {
                            if (null != servicesearch && servicesearch.size() > 0) {
                                String pk = (String) m.get("package");
                                if (null == pk) {
                                    pk = (String) m.get("PACKAGE");
                                }
                                if (ArrayUtils.isLikeArrayInString(pk, servicesearch)) {
                                    ret.add(m);
                                    retName.add(id);
                                }
                            } else {
                                ret.add(m);
                                retName.add(id);
                            }
                        }
                        if (log.isDebugEnabled()) {
                            log.debug("query srv not in zk:" + m);
                        }
                    }
                }
            }
            return ret;
        }catch(Exception e){
            log.error(e);
        }
        return null;
    }

    Map<String ,Boolean> getSrvStatus(Map<String,String> data){
        if(null != data){
            HashMap<String,Boolean> ret = new HashMap<String, Boolean>();
            Iterator<String> its = data.keySet().iterator();
            while(its.hasNext()){
                String s = its.next();
                ret.put(s,StringUtils.isTrue(data.get(s)));
            }
            return ret;
        }
        return null;
    }
    /**
     *
     * @param name
     * @return
     */
    List<Map> findServicesByCenter(XMLParameter env,String name){
        try {
            if(null != servicepath) {
                //service inovke info in zk
                HashMap in = new HashMap();
                in.put("op", "getChildrenData");
                in.put("path", statpath);
                //all service stat data
                Map<String, String> servicesStat = (Map) srvhandler.doSomeThing(null, null, in, null, null);
                if(log.isDebugEnabled()){
                    if(null != servicesStat){
                        Iterator<String> lo = servicesStat.keySet().iterator();
                        while(lo.hasNext()){
                            String k = lo.next();
                            log.debug("query srv stat:"+k+"\n"+servicesStat.get(k));
                        }
                    }

                }
                String[] insids = getInsListIds();
                if(log.isDebugEnabled()){
                    log.debug("query srv insids"+ArrayUtils.toJoinString(insids));
                }
                //services info in zk
                in = new HashMap();
                in.put("op", "getChildren");
                in.put("path", servicepath);
                List<String> ll = (List) srvhandler.doSomeThing(null, null, in, null, null);
                if(log.isDebugEnabled()){
                    log.debug("query srv list :"+ArrayUtils.toJoinString(ll));
                }
                List<Map> ret = new ArrayList(); // list contains result
                List<String> retName = new ArrayList(); //list contains result service name
                List<String> usedList = new ArrayList(); //list contains instance name in each result map
                in.clear();
                in.put("op", "getChildrenData");
                in.put("path", statusstorepath);
                Map<String, String> s_srvstatus = (Map) srvhandler.doSomeThing(null, null, in, null, null);
                Map<String,Boolean> srvstatus = getSrvStatus(s_srvstatus);
                if(null != ll) {
                    for(String l:ll) {
                        in.put("op", "getChildrenData");
                        in.put("path", servicepath+"/"+l);
                        Map<String, String> ls = (Map) srvhandler.doSomeThing(null, null, in, null, null);
                        if (null != ls && ls.size()>0) {
                            Iterator<String> its = ls.keySet().iterator();
                            while (its.hasNext()) {
                                String k = its.next();
                                if(log.isDebugEnabled()){
                                    log.debug("query srv item:"+l);
                                }
                                appendFindServiceByPublish(l,ls.get(k), ret, retName, name,insids, servicesStat,srvstatus, usedList);
                            }

                        }else{
                            in.put("op", "getData");
                            in.put("path", servicepath+"/"+l);
                            String s =  (String)srvhandler.doSomeThing(null, null, in, null, null);
                            if(log.isDebugEnabled()){
                                log.debug("query srv item:"+l);
                            }
                            appendFindServiceByPublish(null,s,ret,retName,name,insids,servicesStat,srvstatus,usedList);
                        }
                    }
                }
                if(log.isDebugEnabled()){
                    log.debug("publish search services "+ret);
                }
                //add other not in zk and exist status service.
                if(null !=servicesStat && servicesStat.size()>0){
                    Map<String,Map<String,String>> tem = new HashMap();
                    Iterator<String> its = servicesStat.keySet().iterator();
                    Bridge thisroot = (Bridge)getObjectById("bridge");
                    while(its.hasNext()){
                        String k = its.next();//.INS-NJ_115.ProvisioningDomainV1.0Op.
                        if(!usedList.contains(k) && !k.contains("."+thisroot.getInstanceId()+".")){
                            String kk = k.substring(0,k.length()-1);
                            //String na = kk.substring(kk.lastIndexOf(".")+1,kk.length());//0Op
                            String na = kk.substring(kk.indexOf(".",2)+1,kk.length());//0Op
                            String insid = null;
                            if(k.length()>1 && k.indexOf(".",1)>0) {
                                insid = k.substring(1, k.indexOf(".", 1));
                            }
                            if((StringUtils.isBlank(name) || ismatch(name,na,(Map)getLocalCacheOfRemoteDesc(na,insid)))) {
                                if (!tem.containsKey(na)) tem.put(na, new HashMap());
                                tem.get(na).put(k, servicesStat.get(k));
                                if(log.isDebugEnabled()){
                                    log.debug("search service in status info:"+k+" "+servicesStat.get(k));
                                }
                            }
                        }
                    }

                    Iterator<String> is = tem.keySet().iterator();
                    while(is.hasNext()) {
                        String id = is.next();
                        if(log.isDebugEnabled()){
                            log.debug("query srv not zk prepare stat :"+id+"\n"+tem.get(id));
                        }
                        List<Map> tt = getStatByCenter(null,id,insids, tem.get(id), null);
                        if(log.isDebugEnabled())
                        log.debug("get stat by center "+id+" "+tt);
                        Map m = getServiceInfoAndStatus(id, null, tt,srvstatus);
                        if(log.isDebugEnabled())
                        log.debug("get service info and status "+id+" "+m);
                        if (null != m) {
                            m.put("IS_PUBLISH","N");
                            //m.put("IS_LOCAL", "Yes");
                            if(!retName.contains(id)) {
                                /*if(null != tt && tt.size()>0 && "RUNNING".equals((String)tt.get(0).get("INS_STATUS")) && StringUtils.isNotBlank(tt.get(0).get("PID"))) {
                                    Object o = getObjectById("restfulClient");
                                    if (null != o && o instanceof XMLDoObject && tt.size() > 0 && tt.get(0) instanceof Map && querytype==QUERY_SERVICE_FOR_PAGE) {
                                        try {
                                            BFParameters p = new BFParameters();
                                            Map map = new HashMap();
                                            map.put("insid", tt.get(0).get("INS_ID"));
                                            map.put("srvid", "getActionPackage");
                                            map.put("method", "POST");
                                            map.put("data", "{name:'" + id + "'}");
                                            p.put("^${input}", map);
                                            ((XMLDoObject) o).doThing(p, null);
                                            Object rm = p.getResult();
                                            if (null != rm && rm instanceof ResultCheck) {
                                                rm = ((ResultCheck) rm).getRet();
                                            }
                                            if (null != rm && rm instanceof String) {
                                                m.put("PACKAGE", rm);
                                            }
                                        }catch(Exception e){

                                        }
                                    }
                                }*/
                                if(null != servicesearch && servicesearch.size()>0){
                                    String pk = (String)m.get("package");
                                    if(null ==pk){
                                        pk = (String)m.get("PACKAGE");
                                    }
                                    if(ArrayUtils.isLikeArrayInString(pk,servicesearch)){
                                        ret.add(m);
                                        retName.add(id);
                                    }
                                }else {
                                    ret.add(m);
                                    retName.add(id);
                                }
                            }
                            if(log.isDebugEnabled()){
                                log.debug("query srv not in zk:"+m);
                            }
                        }
                    }
                }
                if(log.isDebugEnabled()){
                    log.debug("after stat search services "+ret);
                }
                //find local service
                List<Map> ls = findServices(env,name);
                if(null != ls){
                    for(Map m:ls){
                        if(!retName.contains(m.get("NAME"))){
                            m.put("IS_PUBLISH","N");
                            List<Map> lt = (List)m.get("CHILDREN");
                            if(null != lt){
                                for(Map s:lt){
                                    s.put("IS_PUBLISH","N");
                                }
                            }
                            if(null != servicesearch && servicesearch.size()>0){
                                String pk = (String)m.get("package");
                                if(null ==pk){
                                    pk = (String)m.get("PACKAGE");
                                }
                                if(ArrayUtils.isLikeArrayInString(pk,servicesearch)){
                                    ret.add(m);
                                }
                            }else {
                                ret.add(m);
                            }
                            /*String s = servicesStatus.get(m.get("NAME"));

                            if(StringUtils.isNotBlank(s)){
                                appendCount(m,ot);
                            }*/
                            if(log.isDebugEnabled()){
                                log.debug("query srv self:"+m);
                            }
                        }else{
                            for(Map t:ret){
                                if(t.get("NAME").equals(m.get("NAME"))){
                                    if(null != m.get("CHILDREN")) {
                                        Map self = (Map) ((List) m.get("CHILDREN")).get(0);
                                        if (null != self) {
                                            boolean isin = false;
                                            if (null != ((List) t.get("CHILDREN"))) {
                                                for (int j = 0; j < ((List) t.get("CHILDREN")).size(); j++) {
                                                    if (((Map) ((List) t.get("CHILDREN")).get(j)).get("INS_ID").equals(self.get("INS_ID"))) {
                                                        isin = true;
                                                        break;
                                                    }
                                                }
                                            }
                                            if (!isin) {
                                                self.put("IS_PUBLISH", "N");
                                                //self.put("IS_LOCAL", "Yes");
                                                appendCount(t, self);
                                                t.put("REDO", getRedoFlag(m.get("REDO")));
                                                t.put("SHARE", m.get("SHARE"));
                                                if (log.isDebugEnabled()) {
                                                    log.debug("query srv self:" + self);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if(log.isDebugEnabled()){
                    log.debug("after self search services "+ret);
                }
                List<Map> res = new LinkedList<Map>();
                for(Map m:ret){
                    if(null != m.get("CHILDREN") && ((List)m.get("CHILDREN")).size()>0){
                        res.add(m);
                    }
                }
                if(log.isDebugEnabled()){
                    log.debug("query srv result:"+res);
                }
                filterAuth(env,res);
                if(log.isDebugEnabled()){
                    log.debug("query srv result2:"+res);
                }
                return res;
            }
        }catch (Exception e){
            log.error("get service list error",e);
        }
        return null;
    }
    boolean ismatch(String searchKey,String name,Map desc){
        try {
            if(log.isDebugEnabled())
            log.debug("find service by "+searchKey+" name:"+name+" desc:"+desc);
            if (StringUtils.isNotBlank(name) && name.contains(searchKey)) return true;
            if(null != desc) {
                String n = ObjectUtils.convertMap2String((Map)desc);

                if(StringUtils.isNotBlank(n)) {
                    n = n.replaceAll("\"","");
                    return n.indexOf(searchKey) > 0;
                }
            }
            return false;
        }catch (Exception e){
            return false;
        }
    }
    boolean ismatch(String searchKey,String name,String desc){
        try {
            if (StringUtils.isNotBlank(name) && name.contains(searchKey)) return true;

            if(StringUtils.isNotBlank(desc)) {
                desc = desc.replaceAll("\"","");
                return desc.indexOf(searchKey) > 0;
            }
            return false;
        }catch (Exception e){
            return false;
        }
    }

    public List getTreeServices(List<Map> ss){
        if(null != ss){
            List ret = new LinkedList();
            for(Map s:ss){
                putTreeNode("",(String) s.get("PACKAGE"), (String) s.get("NAME"), ret);
            }
            return ret;
        }
        return null;
    }
    void putTreeNode(String pid,String pk,String name,List<Map> ret){
        if(StringUtils.isNotBlank(pk)){
            if(pk.contains(".")) {
                int n = pk.indexOf(".");
                String s = pk.substring(0,n);
                String ls = pk.substring(n+1);
                boolean is=false;
                for(Map m :ret){
                    if(m.get("id").equals(s)){
                        putTreeNode(s,ls,name,(List)m.get("children"));
                        is=true;
                        break;
                    }
                }
                if(!is){
                    Map c = new LinkedHashMap();
                    c.put("id",s);
                    c.put("name",s);
                    List ll = new LinkedList();
                    c.put("children",ll);
                    c.put("pId",pid);
                    ret.add(c);
                    putTreeNode(s,ls,name,ll);
                }

            }else{
                boolean is=false;
                for(Map m :ret){
                    if(m.get("id").equals(pk)){
                        List<Map> ll = (List)m.get("children");
                        Map c = new LinkedHashMap();
                        c.put("id",name);
                        c.put("name",name);
                        c.put("pId",pk);
                        ll.add(c);
                        is=true;
                        break;
                    }
                }
                if(!is){
                    Map c = new LinkedHashMap();
                    c.put("name",pk);
                    c.put("id",pk);
                    c.put("pId",pid);
                    List ll = new LinkedList();
                    c.put("children",ll);
                    Map m = new LinkedHashMap();
                    m.put("id",name);
                    m.put("name",name);
                    m.put("pId",pk);
                    ll.add(m);
                    ret.add(c);
                }

            }
        }else{
            Map c = new LinkedHashMap();
            c.put("pId","");
            c.put("id",name);
            c.put("name",name);
            ret.add(c);
        }
    }

    public Map getTreeServices2(List<Map> ss){
        if(null != ss){
            Map ret = new LinkedHashMap();
            for(Map s:ss){
                putTreeNode2((String) s.get("PACKAGE"), (String) s.get("NAME"), ret);
            }
            return ret;
        }
        return null;
    }
    void putTreeNode2(String pk,String name,Map ret){
        if(StringUtils.isNotBlank(pk)){
            if(pk.contains(".")) {
                int n = pk.indexOf(".");
                String s = pk.substring(0,n);
                String ls = pk.substring(n+1);

                if (ret.containsKey(s)) {
                    putTreeNode2(ls,name,(Map)ret.get(s));
                }else{
                    Map c = new LinkedHashMap();
                    ret.put(s, c);
                    putTreeNode2(ls,name,c);
                }

            }else{
                if(ret.containsKey(pk)){
                    ((Map)ret.get(pk)).put(name,"");
                }else {
                    Map m = new LinkedHashMap();
                    m.put(name, "");
                    ret.put(pk, m);
                }
            }
        }else{
            ret.put(name,"");
        }
    }
    /**
     * below sv can be show
     * 1. create by user
     * 2. hava auth config
     * 3. other share
     * @param res
     */
    void filterAuth(XMLParameter env,List<Map> res){
        if(null != auth && null != res){
            HashMap map = new HashMap();
            for(int i=res.size()-1;i>=0;i--){
                map.put("op","isCanSee");
                map.put("srvId",res.get(i).get("NAME"));
                try {
                    if (!((Boolean)auth.doSomeThing(null,env, map, null, null))) {
                        res.remove(i);
                    }
                }catch (Exception e){
                    res.remove(i);
                }
                map.clear();
            }

        }
    }
    void appendCount(Map t,Map s){
        List<Map> tt = (List)t.get("CHILDREN");
        if(null != tt) {
            for (Map m : tt) {
                if (m.get("INS_ID").equals(s.get("INS_ID"))) return;
            }
        }
        if(null != t.get("INVOKE_COUNT"))
            t.put("INVOKE_COUNT",(Long)t.get("INVOKE_COUNT")+ObjectUtils.getLong(s.get("INVOKE_COUNT")));
        if(null != t.get("INVOKE_ERROR_COUNT"))
            t.put("INVOKE_ERROR_COUNT",(Long)t.get("INVOKE_ERROR_COUNT")+ObjectUtils.getLong(s.get("INVOKE_ERROR_COUNT")));
        if(null != t.get("INVOKE_COST_TIME"))
            t.put("INVOKE_COST_TIME",(Long)t.get("INVOKE_COST_TIME")+ObjectUtils.getLong(s.get("INVOKE_COST_TIME")));
        if(null!= t.get("INVOKE_SIZE"))
            t.put("INVOKE_SIZE",(Long)t.get("INVOKE_SIZE")+ObjectUtils.getLong(s.get("INVOKE_SIZE")));
        if(null != tt)
            tt.add(s);
    }
    /* 本来想把服务信息缓存在本地，每次共页面查询方便的，但是想到每次服务的调用变更信息太多就不缓存了。以后如果服务太多，把服务归类，每种类型起一个Console Center各自管理。Center和Center之间沟通通过服务目录查询获取。服务目录需要时增加。
    List<Map> findCacheLocal(String name){
        List<Map> ret = new LinkedList();
        if(null !=srvInfoList){
            for(Map m:srvInfoList){
                if(((String)m.get("NAME")).contains(name)){
                    ret.add(m);
                }
            }
        }
        return ret;
    }*/
    /**
     * find service info, the service must has description file
     * 服务名称、包路径、操作类型、存储路径
     * @param linkename
     * @return
     */
    List<Map> findServices(XMLParameter env,String linkename) throws IOException {
        LinkedList<Map> ls = new LinkedList<Map>();
        Map<String,XMLObject> objs = getXMLObjectContainer();

        if(null!=objs && objs.size()>0){
                Iterator<String> aname = objs.keySet().iterator();
                while (aname.hasNext()) {
                    String name = aname.next();
                    XMLObject m = objs.get(name);
                    if (null == linkename || ismatch(linkename, name, m.getDescStructure())) {
                        XMLObject o = objs.get(name);
                        try {

                            Map st = o.getInvokeDescStructure();
                            HashMap d = new HashMap();
                            if (null != st && StringUtils.isNotBlank(st.get("name"))) {
                                if (null != st.get("name") && st.get("name") instanceof String) {
                                    d.put("NAME", name);
                                    d.put("PACKAGE", st.get("package"));
                                    d.put("REDO", getRedoFlag(st.get("redo")));
                                    d.put("SHARE", st.get("share"));
                                    d.put("IS_ALARM",isAlarm(st));
                                    d.put("IS_WORKTIME",isWorktime(st));
                                    d.put("PATH", st.get("path"));
                                    d.put("CREATE_BY", st.get("createby"));
                                    d.put("OP_TYPE", st.get("opType"));
                                    d.put("DATE", st.get("date"));
                                    d.put("AUTHOR",st.get("author"));
                                    d.put("STATUS", o.isActive());
                                    d.put("IS_LOCAL", true);
                                    log.debug("local service:"+name);
                                    if (null != srvstat) {
                                        HashMap map = new HashMap();
                                        map.put("srvId", name);
                                        map.put("op", "getStatInfo");
                                        Object rr = srvstat.doSomeThing(null, null, map, null, null);
                                        log.debug("stat local service:"+name);
                                        if (null != rr && rr instanceof Map) {
                                            ((Map) rr).put("STATUS", o.isActive());
                                           // ((Map) rr).put("IS_LOCAL", "Yes");

                                            List li = new ArrayList();

                                            li.add(rr);
                                            d.put("CHILDREN", li);
                                        }
                                        if (null != rr && rr instanceof Map) {
                                            d.put("INVOKE_COUNT", ((Map) rr).get("INVOKE_COUNT"));
                                            d.put("INVOKE_ERROR_COUNT", ((Map) rr).get("INVOKE_ERROR_COUNT"));
                                            d.put("INVOKE_COST_TIME", ((Map) rr).get("INVOKE_COST_TIME"));
                                            d.put("INVOKE_SIZE", ((Map) rr).get("INVOKE_SIZE"));

                                        }
                                    }

                                } else {
                                    log.error("find [" + name + "] desc incorrect please check:\n" + st);
                                }
                            }
                            if (d.size() > 0)
                                ls.add(d);
                        } catch (Exception e) {
                            log.error("findServices error:", e);
                        }
                    }
                }


        }

        log.debug("findServices ["+linkename+"] \n"+ls);
        return ls ;
    }
    //get service parameter ,include input ,output,config
    Map getServiceParameters(String name){
        XMLObject o = getXMLObjectContainer().get(name);
        if(null != o){
            try {
                Map st = o.getDescStructure();
                if (null != st) {
                    HashMap ret = new HashMap();
                    ret.put("INPUT",st.get("input"));
                    ret.put("OUTPUT", st.get("output"));
                    ret.put("CONFIG",st.get("config"));
                    if(ret.size()>0)
                    return ret;
                }
            }catch (Exception e){}
        }
        return null;
    }
    Map getServiceOutput(String name){
        XMLObject o = getXMLObjectContainer().get(name);
        if(null != o){
            try {
                Map st = o.getDescStructure();
                if (null != st) {
                    HashMap ret = new HashMap();
                    ret.put("OUTPUT", st.get("output"));
                    if(ret.size()>0)
                        return ret;
                }
            }catch (Exception e){}
        }
        return null;
    }
    //get a service description info , include description、body、error、depend
    Map getServiceDescription(String name){
        XMLObject o = getXMLObjectContainer().get(name);
        if(null != o){
            try {
                Map st = o.getDescStructure();
                if (null != st) {
                    HashMap ret = new HashMap();
                    ret.put("DESCRIPTION",st.get("desc"));
                    ret.put("BODY",st.get("body"));
                    ret.put("ERROR",st.get("error"));
                    ret.put("DEPEND",st.get("depend"));
                    if(ret.size()>0)
                        return ret;
                }
            }catch (Exception e){}
        }
        return null;
    }
    Map getServiceUsage(String name){
        XMLObject o = getXMLObjectContainer().get(name);
        if(null != o){
            try {
                Map st = o.getDescStructure();
                if (null != st) {
                    HashMap ret = new HashMap();
                    ret.put("SCENE",st.get("scene"));
                    ret.put("EXAMPLE",st.get("example"));
                    ret.put("ORIGINAL",st.get("original"));
                    if(ret.size()>0)
                        return ret;
                }
            }catch (Exception e){}
        }
        return null;
    }
    //get test invoke parameter
    Map getTestParameter(String name){
        XMLObject o = getXMLObjectContainer().get(name);
        if(null != o){
            try {
                Object r = o.getInvokeDescStructure().get("input");
                if(null != r && r instanceof Map) {
                    return (Map) r;
                }else{
                    return null;
                }
            }catch (Exception e){}
        }
        return null;
    }

    boolean checkDesc(Map desc)throws Exception{
        if(desc.containsKey("opType") && null!= desc.get("opType") && ((String)desc.get("opType")).startsWith("INS-")){
            throw new Exception("the service desc opType is not must start with [INS-]");
        }
        return true;
    }
    /**
     * 1. need to instance the new service and add to container
     * 2. need save the service description to file
     * 3. update out service , ext: webService class
     */
    boolean addService(Map allDesc,XMLParameter parameter)throws Exception{
        try {
            checkDesc(allDesc);
            if(StringUtils.isBlank((String)allDesc.get("author")) && null !=parameter && parameter instanceof RequestParameters){
                if(null != ((RequestParameters)parameter).getSession()) {
                    String author = ((RequestParameters) parameter).getSession().getUserName();
                    if(StringUtils.isNotBlank(author)) {
                        allDesc.put("author", author);
                    }
                }
            }
            if(null != allDesc.get("name") && allDesc.get("name") instanceof String) {
                if(null == getObjectById((String)allDesc.get("name") )) {
                    Map data = Desc.removeNotServiceProperty(allDesc);
                    boolean isa = isPublishActive((String)data.get("opType"),(String)data.get("package"),(String)data.get("name"));
                    createXMLObjectByDesc(data, this.getClass().getClassLoader(), this, isa,getSingleContainers());
                    if(null != srvstat){
                        HashMap map = new HashMap();
                        map.put("op","initStatInfo");
                        map.put("name",allDesc.get("name"));
                        srvstat.doSomeThing(null, null, map, null, null);
                        log.debug("stat service:"+allDesc.get("name"));
                        //set the service in instance status
                        setThisStatus((String) allDesc.get("name"), isa);
                    }
                    return true;
                }else{
                    throw new Exception("the service ["+allDesc.get("name") +"] has exist, can not new create.");
                }
            }
        }catch (Exception e){
            log.error("addService error:",e);
            throw e;
        }
return false;
    }
    boolean deleteService(String name)throws Exception{
        try {
            boolean is = removeObject(name);
            if(is) {
                if (null != srvstat) {
                    HashMap map = new HashMap();
                    map.put("op", "deleteStatInfo");
                    map.put("name", name);
                    srvstat.doSomeThing(null, null, map, null, null);
                }
            }
        return true;
        }catch (Exception e){
            log.error("remove Service error:",e);
            throw e;
        }
    }
    boolean updateService(Map allDesc)throws Exception{
        try {
            if(log.isInfoEnabled()){
                log.info("update service "+allDesc);
            }
            checkDesc(allDesc);
            Map data= Desc.removeNotServiceProperty(allDesc);
            if(null == getObjectById((String)data.get("name"))){
                createXMLObjectByDesc(data,this.getClass().getClassLoader(),this,false,getSingleContainers());
                return true;
            }else {
                return updateObjectByDesc(data);
            }
        }catch (Exception e){
            log.error("updateService error:",e);
            throw e;
        }
    }
    boolean addUpdateSerivce(Map desc,XMLParameter parameter)throws Exception{
        if(null == desc || !Desc.isDescriptionService(desc)){
            throw new Exception("not a service desc file , please check the file.");
        }
        if(StringUtils.isNotBlank((String) desc.get("name"))){
            XMLObject oo = getObjectById((String)desc.get("name"));
            if(null !=oo ){
                HashMap map = new HashMap();
                map.put("op","isStatExist");
                map.put("name",desc.get("name"));
                if (null != srvstat) {
                    updateService(desc);
                    Object o = srvstat.doSomeThing(null, null, map, null, null);
                    if(!(null != o && o instanceof Boolean && (Boolean)o)) {
                        map.put("op", "initStatInfo");
                        map.put("name", desc.get("name"));
                        srvstat.doSomeThing(null, null, map, null, null);
                        log.debug("stat local service:"+desc.get("name"));
                        setThisStatus((String) desc.get("name"), oo.isActive());
                    }

                }else{
                    updateService(desc);
                }
            }else{
                addService(desc,parameter);
            }
            return true;
        }
        return false;
    }
    boolean suspendService(String name){
        XMLObject o = getXMLObjectContainer().get(name);
        if(null != o){
            try {
                boolean is= o.suspendObject();
                setThisStatus(name,o.isActive());
                HashMap in = new HashMap();
                in.put("op","setSrvDisable");
                in.put("name",name);
                srvstat.doSomeThing(null,null,in,null,null);
                log.info("suspend service "+name);
                return is;
            }catch (Exception e){}
        }
        return false;
    }
    void setThisStatus(String name,boolean value)throws Exception{
        if(null != srvhandler){
            Bridge b = (Bridge)getPropertyObject("bridge");
            HashMap map = new HashMap();
            map.put("path",getPublishStateZkPath(statusstorepath, b.getInstanceId(), name));
            map.put("data",String.valueOf(value));
            map.put("op","onlySetData");
            srvhandler.doSomeThing(null,null,map,null,null);
        }
    }

    /**
     * 设置对象是否在异常经过时是否可以重做。
     * @param svName
     * @param isRedo
     * @return
     * @throws Exception
     */
    boolean setRedo(String svName,String isRedo)throws Exception{
        XMLObject o = getXMLObjectContainer().get(svName);
        if(StringUtils.isBlank(isRedo)){
            isRedo="true";
        }
        boolean ret = o.setProperty("redo",isRedo);
        return ret;
    }
    boolean publishSetRedo(String svName,String opType,String pack,String isRedo)throws Exception{
        Map m = new HashMap();
        if(StringUtils.isBlank(isRedo)){
            isRedo="true";
        }
        m.put("redo",isRedo);
        /*publishProperty(svName,"publishSetRedo",m);*/
        publishUpdateSv(svName,opType,pack,m);
        //return setRedo(svName,isRedo);
        return true;
    }

    /**
     * if the service open for other user , will set share =true
     * @param svName
     * @param isShare
     * @return
     * @throws Exception
     */
    boolean setShare(String svName,String isShare)throws Exception{
        XMLObject o = getXMLObjectContainer().get(svName);
        if(StringUtils.isBlank(isShare)){
            isShare="true";
        }
        boolean ret = o.setProperty("share",isShare);
        return ret;
    }
    boolean publishSetShare(String svName,String opType,String pack,String isShare)throws Exception{
        Map m = new HashMap();
        if(StringUtils.isBlank(isShare)){
            isShare="true";
        }
        m.put("share",isShare);
        publishUpdateSv(svName,opType,pack,m);
        return true;
    }
    boolean isWorktime(Map desc){
        String b= (String)desc.get("body");
        if(StringUtils.isNotBlank(b)){
            if(b.contains("worktime") && b.contains("crons")){
                return true;
            }
        }
        return false;
    }
    boolean isAlarm(Map desc){
        String b = (String)desc.get("body");
        if(StringUtils.isNotBlank(b)){
            List s = StringUtils.getTagsNoMark(b,"alarm","}");
            if(null != s && s.size()>0 && StringUtils.isNotBlank(s.get(0))){
                if(((String)s.get(0)).contains("check")) {
                    return true;
                }
            }
        }
        return false;
    }
    boolean publishUpdateSv(String name,String opType,String pack,Map pro)throws Exception{
        if(null != srvhandler) {

            String p = getServicePath(opType, pack, name);
            HashMap input = new HashMap();
            input.put("path", p);
            input.put("op", "getData");
            Object o = srvhandler.doSomeThing(null, null, input, null, null);
            if(null !=o && o instanceof String && null != pro){
                Map desc = StringUtils.convert2MapJSONObject((String)o);
                desc.putAll(pro);

                input.put("data", ObjectUtils.convertMap2String(desc));
                input.put("op", "onlyWriteData");
                srvhandler.doSomeThing(null, null, input, null, null);
                return true;
            }
        }
        return false;

    }
    boolean publishProperty(String name,String op,Map map)throws Exception{
        if(StringUtils.isNotBlank(name)){
            XMLObject o = getObjectById(name);
            String opType = o.getXML().getProperties().getProperty("opType");
            String pk = o.getXML().getProperties().getProperty("package");
            String r = getZkPath(statuspath,opType,pk,name);
            Map input = new HashMap();
            input.put("path",r);

            map.put("op",op);
            map.put("name",name);
            input.put("data",ObjectUtils.convertMap2String(map));
            input.put("op",op);
            srvhandler.doSomeThing(null, null, input, null, null);
            return true;
        }
        return false;
    }
    boolean activeService(String name){
        XMLObject o = getXMLObjectContainer().get(name);
        if(null != o){
                try {
                    boolean is = o.activeObject();
                    setThisStatus(name,o.isActive());
                    HashMap in = new HashMap();
                    in.put("op","setSrvAble");
                    in.put("name",name);
                    srvstat.doSomeThing(null,null,in,null,null);
                    log.info("active service " + name);
                    return is;

                } catch (Exception e) {
                }

        }
        return false;
    }
    String getServicePath(String opType,String pck,String name){
        String r = servicepath;
        if (StringUtils.isNotBlank(opType)) {
            r += "/" + opType;
        }

        boolean is = false;
        if (StringUtils.isNotBlank(pck)) {
            r += "/" + pck;
            is = true;
        }

        if (is) {
            r += "." + name;

        } else {
            r += name;
        }
        return r;
    }
    boolean publishAddUpdateService(String insids,Map desc){
        if(null != srvhandler && null != desc){
            try {
                boolean isa = false;
                if(null != getObjectById((String)desc.get("name"))){
                    isa =getObjectById((String)desc.get("name")).isActive();
                }
                if(StringUtils.isNotBlank(desc.get("name")) && null == desc.get("body")){
                    String name = (String)desc.get("name");
                    desc = getStoreDescStructure(name);
                    if(null == desc){
                        throw new Exception("not find service ["+name+"] description in location ");
                    }
                }
                desc.put("op","publishAddUpdateService");
                List<String> spl = new ArrayList();
                String r;
                if(StringUtils.isNotBlank(insids)){
                    String[] ids = insids.split(",");
                    String sid = getSelfInstanceId();
                    boolean isinself = false;
                    for(String id:ids) {
                        if(id.equals(sid)){
                            isinself=true;
                        }
                        r = getServicePath(id, (String) desc.get("package"), (String) desc.get("name"));
                        spl.add(r);
                    }
                    if(!isinself){
                        r = getServicePath(sid, (String) desc.get("package"), (String) desc.get("name"));
                        spl.add(r);
                    }
                    if(StringUtils.isNotBlank(desc.get("opType"))) {
                        r = getServicePath((String) desc.get("opType"), (String) desc.get("package"), (String) desc.get("name"));
                        spl.add(r);
                    }
                }else{
                    r = getServicePath((String) desc.get("opType"), (String) desc.get("package"), (String) desc.get("name"));
                    spl.add(r);
                }
                Map input = new HashMap();
                for(String p:spl) {
                    input.put("path", p);
                    input.put("data", ObjectUtils.convertMap2String(desc));
                    input.put("op", desc.get("op"));
                    log.info("publishupdateservice "+input);
                    srvhandler.doSomeThing(null, null, input, null, null);
                }
                setPublishStatus((String)desc.get("opType"),(String)desc.get("package"),(String)desc.get("name"),isa);
                log.info("publish service [" + desc.get("name") + "] successful");
                return true;

            }catch (Exception e){
                log.error("publish service ["+desc.get("name")+"] fault",e);
                return false;
            }
        }else{
            return false;
        }
    }
    boolean publishDeleteService(String name)throws Exception{
        if(StringUtils.isNotBlank(name)){
            XMLObject o = getObjectById(name);
            String opType = o.getXML().getProperties().getProperty("opType");
            String pk = o.getXML().getProperties().getProperty("package");
            String r = getZkPath(servicepath,opType,pk, name);
            Map input = new HashMap();
            input.put("path",r);

            HashMap map = new HashMap();
            map.put("op","publishDeleteService");
            map.put("name",name);
            input.put("data",ObjectUtils.convertMap2String(map));
            input.put("op","publishDeleteService");
            srvhandler.doSomeThing(null, null, input, null, null);
            //delete status
            r = getZkPath(statusstorepath,opType,pk,name);
            input.put("path",r);
            input.put("op","delete");
            srvhandler.doSomeThing(null,null,input,null,null);
            return true;
        }

        return false;
    }

    /**
     * get publish active statusd
     * @param name
     * @return
     * @throws Exception
     */
    boolean isPublishOneActive(String insid,String name)throws Exception{
        if(StringUtils.isNotBlank(name)){
            String r = getPublishStateZkPath(statusstorepath, insid, name);
            Map input = new HashMap();
            input.put("path",r);
            input.put("op","getData");
            Object s = srvhandler.doSomeThing(null, null, input, null, null);
            if(s instanceof String){
                return StringUtils.isTrue((String)s);
            }
        }
        return false;
    }

    boolean isPublishActive(String opType,String pk,String name)throws Exception{
        try {
            if (StringUtils.isNotBlank(name)) {
                String r = getZkPath(statusstorepath,opType,pk, name);
                if(StringUtils.isNotBlank(r)) {
                    Map input = new HashMap();
                    input.put("path", r);
                    input.put("op", "getData");
                    String s = (String) srvhandler.doSomeThing(null, null, input, null, null);

                    return StringUtils.isTrue(s);
                }else{
                    return false;
                }
            }
        }catch (Exception e){
            log.error("",e);
        }

        return false;
    }
    boolean activeThisService(String localip,String name,String ip,String insid){
        Bridge b = (Bridge)getObjectById("bridge");
        if(localip.equals(ip) && b.getInstanceId().equals(insid)){
            return activeService(name);
        }
        return false;
    }
    String getSelfInstanceId(){
        Bridge b = (Bridge)getObjectById("bridge");
        return b.getInstanceId();
    }
    boolean suspendThisService(String localip,String name,String ip,String insid){
        Bridge b = (Bridge)getObjectById("bridge");
        if(localip.equals(ip) && b.getInstanceId().equals(insid)){
            return suspendService(name);
        }
        return false;
    }
    //get service status active or disactive in zk
    public static String getPublishStateZkPath(String zktype,String insid,String name)throws Exception{
        String r=zktype;
        /*if(StringUtils.isNotBlank(optype)){
            r+="/"+optype;
        }*/
        r+="/"+insid+INSID_SRVNAME_SPLITCHAR+name;
        /*boolean is=false;
        if(StringUtils.isNotBlank(pkg)){
            r+="_"+pkg;
            is=true;
        }
        if(is){
            r += "." + name;
        }else {
            r +=  name;
        }*/

        return r;
    }
    //get service path in zk
    String getZkPath(String zktype,String opType,String pk,String name)throws Exception{
        String r=zktype;
        if(StringUtils.isNotBlank(opType)){
            r+="/"+opType;
        }
        boolean is=false;
        if(StringUtils.isNotBlank(pk)){
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
    //get service path in zk
    /*String getZkPath(String zktype,String name)throws Exception{
        XMLObject obj = getObjectById(name);
        if(null == obj){
            return null;
        }
        XMLMakeup x = obj.getXML();
        String r=zktype;
        if(StringUtils.isNotBlank(x.getProperties().getProperty("opType"))){
            r+="/"+x.getProperties().getProperty("opType");
        }
        boolean is=false;
        if(StringUtils.isNotBlank( x.getProperties().getProperty("package"))){
            r+="/"+x.getProperties().getProperty("package");
            is=true;
        }
        if(is){
            r += "." + x.getId();
        }else {
            r +=  x.getId();
        }
        return r;
    }*/
    void setPublishStatus(String opType,String pk,String name,boolean activeStatus)throws Exception{
        String r = getZkPath(statusstorepath,opType,pk,name);
        HashMap input = new HashMap();
        input.put("path",r);
        input.put("op","onlySetData");
        input.put("data",String.valueOf(activeStatus));
        srvhandler.doSomeThing(null,null,input,null,null);
    }
    boolean publishActiveService(String name)throws Exception{
        if(StringUtils.isNotBlank(name)){
            XMLObject o = getObjectById(name);
            String opType = o.getXML().getProperties().getProperty("opType");
            String pk = o.getXML().getProperties().getProperty("package");
            String r = getZkPath(statuspath,opType,pk, name);
            Map input = new HashMap();
            input.put("path",r);

            HashMap map = new HashMap();
            map.put("op","publishActiveService");
            map.put("name",name);
            input.put("data",ObjectUtils.convertMap2String(map));
            input.put("op","publishActiveService");
            srvhandler.doSomeThing(null, null, input, null, null);
            setPublishStatus(opType,pk,name,true);
            return true;
        }

        return false;
    }
    boolean publishActiveOneService(String ip,String insid,String opType,String pkg,String name)throws Exception{
        if(StringUtils.isNotBlank(name)){
            String r = getPublishStateZkPath(statuspath,insid, name);
            Map input = new HashMap();
            input.put("path",r);
            HashMap map = new HashMap();
            map.put("op","publishActiveOneService");
            map.put("name",name);
            map.put("ip",ip);
            map.put("insid",insid);
            input.put("data",ObjectUtils.convertMap2String(map));
            input.put("op","publishActiveOneService");
            srvhandler.doSomeThing(null, null, input, null, null);
            return true;
        }

        return false;
    }

    boolean publishSuspendService(String name)throws Exception{
        if(StringUtils.isNotBlank(name)){
            XMLObject o = getObjectById(name);
            String opType = o.getXML().getProperties().getProperty("opType");
            String pk = o.getXML().getProperties().getProperty("package");
            String r = getZkPath(statuspath,opType,pk,name);
            Map input = new HashMap();
            input.put("path",r);

            HashMap map = new HashMap();
            map.put("op","publishSuspendService");
            map.put("name",name);
            input.put("data",ObjectUtils.convertMap2String(map));
            input.put("op","publishSuspendService");
            srvhandler.doSomeThing(null, null, input, null, null);

            setPublishStatus(opType,pk,name,false);
            return true;
        }

        return false;
    }
    boolean publishSuspendOneService(String ip,String insid,String optype,String pkg,String name)throws Exception{
        if(StringUtils.isNotBlank(name)){
            String r = getPublishStateZkPath(statuspath,insid, name);
            Map input = new HashMap();
            input.put("path",r);

            HashMap map = new HashMap();
            map.put("op","publishSuspendOneService");
            map.put("name",name);
            map.put("ip",ip);
            map.put("insid",insid);
            input.put("data",ObjectUtils.convertMap2String(map));
            input.put("op","publishSuspendOneService");
            srvhandler.doSomeThing(null, null, input, null, null);


            return true;
        }

        return false;
    }
    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(null != input){
            String op = (String)input.get("op");
            String sleep=(String)input.get("sleep");
            if(StringUtils.isNotBlank(sleep)){
                Thread.sleep(Integer.parseInt(sleep)*1000);
            }
            if(StringUtils.isNotBlank(op)){
                if("exit".equals(op)){
                    System.exit(3);
                }else if("getActions".equals(op)){
                    //获取所有服务
                    return getAllServices();

                }else if("getServices".equals(op)){
                    //获取查询的服务列表，包括：服务名称、包路径、操作类型、存储路径
                    if(null != srvhandler){
                        return findServicesByCenter(env,(String) input.get("name"));
                    }else {
                        //查询当前服务实例的服务信息
                        List<Map> ret= findServices(env,(String) input.get("name"));
                        filterAuth(env,ret);
                        return ret;
                    }
                }else if("getTreeServices".equals(op)){
                    List<Map> ls = findServicesByCenter(env,null);
                    return getTreeServices(ls);
                }else if("getServiceParameters".equals(op)){
                    //获取一个服务的参数，包括：入参，配置参数，出参
                    return getServiceParameters((String)input.get("name"));
                }else if("getServiceDescription".equals(op)){
                    //获取服务简要描述，报文体，异常，依赖等信息
                    return getServiceDescription((String) input.get("name"));
                }else if("getServiceUsage".equals(op)){
                    //获取使用手册，使用场景，这个文件会很大单独存储
                    return getServiceUsage((String) input.get("name"));
                }else if("getServiceOutput".equals(op)){
                    return getServiceOutput((String)input.get("name"));
                }else if("getTestParameter".equals(op)){
                    //获取测试报文
                    return getTestParameter((String) input.get("name"));


                }else if("addService".equals(op)){//add service description
                    //增加服务
                    Map desc = (Map)input.get("data");
                    boolean b = addService(desc,env);
                    notifyByAddSrv((String)desc.get("name"),null);
                    addSrvInfoToCacheByAddSrv(env,desc);

                    return b;
                }else if("copyService".equals(op)){//add service description
                    //copy服务
                    String name = (String)input.get("name");
                    String nname = (String)input.get("newName");
                    XMLObject o = getObjectById(name);
                    if(null ==o)throw new Exception("not find object by id ["+name+"]");
                    Map pros = (Map)input.get("pros");
                    if(StringUtils.isNotBlank(name)){
                        Map nmap= copyDesc(name,nname,pros);
                        boolean b = addService(nmap,env);
                        notifyByAddSrv(name,null);
                        addSrvInfoToCacheByAddSrv(env,nmap);
                        return b;
                    }

                }else if("setObjectProperties".equals(op)){
                    String name = (String)input.get("name");
                    Map pros = (Map)input.get("pros");
                    if(StringUtils.isNotBlank(name)){
                        XMLObject o = getObjectById(name);
                        if(null !=o){
                            o.setObjectProperties(pros);
                        }else{
                            throw new Exception("not find action ["+name+"]");
                        }
                    }
                }else if("removeObjectProperties".equals(op)){
                    String name = (String)input.get("name");
                    Object ps = input.get("paths");
                    if(StringUtils.isNotBlank(name)){
                        XMLObject o = getObjectById(name);
                        if(null !=o){
                            return o.removeObjectProperties(ps);
                        }
                    }
                    return false;
                }else if("deleteService".equals(op)){
                    //删除服务
                    notifyByRemoveSrv((String) input.get("name"),null);
                    return deleteService((String) input.get("name"));
                }else if("updateService".equals(op)){
                    //修改服务
                    Map m = (Map)input.get("data");
                    m.put("date",DateTimeUtil.getCurrDateTime());
                    return addUpdateSerivce(m,env);
                }else if("addUpdateService".equals(op)){
                    addUpdateSerivce((Map)input.get("data"),env);
                    /*String ids = (String)input.get("insIds");
                    if(StringUtils.isNotBlank(ids)){
                        String[] insIds = ids.split(",");
                        if(ArrayUtils.isInStringArray(insIds,getSelfInstanceId())){
                            addUpdateSerivce(input);
                            notifyByAddSrv((String)input.get("name"),insIds);
                        }
                    }else {
                        if(!(null != input.get("srcInsId") && input.get("srcInsId").equals(getSelfInstanceId()))) {
                            addUpdateSerivce(input);
                            notifyByAddSrv((String) input.get("name"), getInsListIds());
                        }
                    }*/
                }else if("suspendService".equals(op)){
                    //暂停服务
                    notifyBySuspendSrv((String) input.get("name"),null);
                    return suspendService((String) input.get("name"));
                }else if("activeService".equals(op)) {
                    //激活服务
                    notifyByActiveSrv((String) input.get("name"),null);
                    return activeService((String) input.get("name"));
                }else if("activeThisService".equals(op)){
                    notifyByActiveSrv((String) input.get("name"),new String[]{(String) input.get("insid")});
                    return activeThisService(((String)((Map)env.get("${env}")).get("${ip}")),(String) input.get("name"),(String) input.get("ip"),(String) input.get("insid"));
                }else if("suspendThisService".equals(op)) {
                    notifyBySuspendSrv((String) input.get("name"),(String) input.get("insid"));
                    return suspendThisService(((String)((Map)env.get("${env}")).get("${ip}")),(String) input.get("name"), (String) input.get("ip"), (String) input.get("insid"));
                }else if("publishAddUpdateService".equals(op)){
                    //集群服务的话，只用使用这个方法发布发布就可以
                    return publishAddUpdateService((String)input.get("insIds"),(Map)input);
                }else if("publishService".equals(op)){
                    //集群服务的话，只用使用这个方法发布发布就可以
                    String ids = (String)input.get("insIds");
                    return publishAddUpdateService(ids,input);
                }else if("publishDeleteService".equals(op)){
                    //集群服务的话，只用使用这个方法发布发布就可以
                    return publishDeleteService((String) input.get("name"));
                }else if("publishSuspendService".equals(op)){
                    //集群服务的话，只用使用这个方法发布发布就可以
                    return publishSuspendService((String) input.get("name"));
                }else if("publishActiveService".equals(op)){
                    //集群服务的话，只用使用这个方法发布发布就可以
                    return publishActiveService((String) input.get("name"));
                }else if("publishSuspendOneService".equals(op)){
                    //集群服务的话，只用使用这个方法发布发布就可以
                    return publishSuspendOneService((String) input.get("ip"), (String) input.get("insid"),(String)input.get("opType"),(String)input.get("package"), (String) input.get("name"));
                }else if("publishActiveOneService".equals(op)){
                    //集群服务的话，只用使用这个方法发布发布就可以
                    return publishActiveOneService((String) input.get("ip"), (String) input.get("insid"), (String)input.get("opType"),(String)input.get("package"),(String) input.get("name"));
                }

                else if("getUpdateDesc".equals(op)){
                    return getDescStructure((String) input.get("name"));
                }else if("setRedo".equals(op)){
                    if(null != input.get("redo")){
                        if(input.get("redo") instanceof String){
                            return setRedo((String) input.get("name"), (String) input.get("redo"));
                        }
                        if(input.get("redo") instanceof Map){
                            return setRedo((String) input.get("name"), ObjectUtils.convertMap2String((Map) input.get("redo")));
                        }
                    }

                }else if("publishSetRedo".equals(op)){
                    if(null != input.get("redo")){
                        if(input.get("redo") instanceof String){
                            return publishSetRedo((String) input.get("name"),(String) input.get("opType"),(String) input.get("package"), (String) input.get("redo"));
                        }
                        if(input.get("redo") instanceof Map){
                            return publishSetRedo((String) input.get("name"),(String) input.get("opType"),(String) input.get("package"), ObjectUtils.convertMap2String((Map) input.get("redo")));
                        }
                    }
                }else if("setShare".equals(op)){
                    return setShare((String) input.get("name"), (String) input.get("share"));
                }else if("publishSetShare".equals(op)){
                    return publishSetRedo((String) input.get("name"), (String) input.get("opType"), (String) input.get("package"), (String) input.get("share"));
                }else if("getNewDesc".equals(op)){
                    return Desc.getEnptyDescStructure();
                }else if("publishStartTrace".equals(op)){
                    return startTrace();
                }else if("publishStopTrace".equals(op)){
                    return stopTrace();
                }else if("getTraceList".equals(op)){
                    HashMap map = new HashMap();
                    map.put("op","getTraceList");
                    map.put("data",input.get("data"));
                    return tracelog.doSomeThing(null,null,map,null,null);
                }else if("getTrace".equals(op)){
                    HashMap map = new HashMap();
                    map.put("op","getTrace");
                    map.put("data",input.get("data"));
                    return tracelog.doSomeThing(null,null,map,null,null);
                }else if("removePath".equals(op)){
                    //set INS_STATUS
                    HashMap map = new HashMap();
                    String p = (String)input.get("path");
                    log.info("will remove path:"+p);
                    String id = p.substring(p.lastIndexOf("/")+1);
                    map.put("op","setInsStatusStopped");
                    map.put("insId",id);
                    srvstat.doSomeThing(null,null,map,null,null);
                    //remove insid from invoke ins list
                    notifyByRemoveIns(id);
                }else if("saveUploadFile".equals(op)){
                    String s = (String)input.get("savepath");
                    String fs = null;
                    if(StringUtils.isNotBlank(s) && env instanceof RequestParameters){
                        Map m = (Map)((Map)((RequestParameters)env).getRequestData()).get("files");
                        if(null != m){
                            Iterator<String> its = m.keySet().iterator();
                            while(its.hasNext()){
                                String name = its.next();
                                InputStream in=null;
                                if(m.get(name) instanceof InputStream) {
                                    in = (InputStream) m.get(name);
                                }else if(m.get(name) instanceof String){
                                    in = ObjectUtils.convertBase64String2InputStream((String)m.get(name));
                                }
                                FileUtils.saveFile(s+"/"+name,in);
                                fs = s+"/"+name;
                            }
                        }
                    }
                    return fs;
                }else if("addServiceByDesc".equals(op)){
                    Map m = (Map)input.get("desc");
                    if(null != m){
                        addUpdateSerivce(m,env);
                    }
                }else if("isExistService".equals(op)){
                    String s = (String)input.get("name");
                    if(StringUtils.isNotBlank(s)){
                        return getObjectById(s)==null?false:true;
                    }
                }else if("addServiceByFlowDesc".equals(op)){
                    Map m = (Map)input.get("flowdesc");
                    Map desc = Desc.convertFlowStructure2DescMap(m);
                    if(null != m){
                        addUpdateSerivce(desc,env);
                    }
                    return true;
                }else if("isFlowSV".equals(op)){
                    String m = (String)input.get("name");
                    if("flow".equals(getObjectById(m).getDescStructure().get("createby"))){
                        return true;
                    }else{
                        return false;
                    }

                }else if("getFlowDescByDesc".equals(op)){
                    String name = (String)input.get("name");
                    XMLObject o = getObjectById(name);
                    if(null != o){
                        Map m = o.getDescStructure();
                        return Desc.convertDescMap2FlowStructure(m);
                    }

                }else if("addPath".equals(op)){
                    //set INS_STATUS
                    HashMap map = new HashMap();
                    String p = (String)input.get("path");
                    log.info("will update path srv to RUNNING:"+p);
                    String id = p.substring(p.lastIndexOf("/")+1);
                    map.put("op","setInsStatusRunning");
                    map.put("insId",id);
                    srvstat.doSomeThing(null,null,map,null,null);
                    //add insid from invoke ins list

                }else if("getServicesFileRel".equals(op)){
                    List<String> fs = (List)input.get("data");
                    if(null != fs){
                        Map<String,List<String>> map = new HashMap();
                        for(String s:fs){
                           Map<String,XMLObject> all = getXMLObjectContainer();
                               Iterator<String> its = all.keySet().iterator();
                               while(its.hasNext()){
                                   XMLObject o = all.get(its.next());
                                   Map desc = o.getDescStructure();
                                   if(null != desc){
                                       Map org = (Map)desc.get("original");
                                       if(null != org){
                                           if(s.equals(org.get("src"))){
                                               if(!map.containsKey(s)){
                                                   map.put(s,new ArrayList());
                                               }
                                               map.get(s).add(o.getXML().getId());
                                           }
                                       }
                                   }
                               }

                        }
                        return map;

                    }
                }else if("getAddressByInstanceName".equals(op)){
                    String name = (String)input.get("name");
                    String instanceid = (String)input.get("instanceid");
                    String port = ObjectUtils.getStringValueByPath(env,"${requestProperties}.ServerPort");
                    if(StringUtils.isBlank(port)){
                        Map m = (Map)env.get("${env}");
                        if(null != m && instanceid.equals(m.get("${local_instanceId}"))) {
                            port = (String) m.get("webport");
                        }

                    }
                    String ret= getAddress(instanceid, port, name);
                    if(log.isDebugEnabled()){
                        log.debug("get remote instance address "+ret+" by instance id "+instanceid+" input "+input);
                    }
                    return ret;
                }else if("getInsList".equals(op)){
                    return getInsList();
                }else if("getOtherInsList".equals(op)){
                    List<Map> ms = getInsList();
                    if(null!= ms){
                        for(Map m:ms){
                            if(m.get("insId").equals(getSelfInstanceId())){
                                ms.remove(m);
                                break;
                            }
                        }
                    }
                    return ms;
                }else if("getInsListBySrvId".equals(op)){
                    String srv = (String)input.get("srvId");
                    List limitIn = (List)input.get("limitIn");
                    if(StringUtils.isNotBlank(srv)){
                        List<Map> ret= srvIdRelIns.get(srv);
                        if(null != limitIn && limitIn.size()>0){
                            ret = ArrayUtils.innerList(ret,limitIn);
                        }
                        if(log.isInfoEnabled()){
                            log.info("find instance list by "+srv+"["+ret+"]");
                        }
                        return ret;
                    }
                }else if("getInstanceInfo".equals(op)){
                    String instanceid = (String)input.get("instanceid");
                    if(StringUtils.isNotBlank(instanceid)){
                        return getInstanceInfo(instanceid);
                    }
                }else if("getErrorMessage".equals(op)){
                    int lines = 200;
                    if(StringUtils.isNotBlank(input.get("lineNum"))) {
                        if(StringUtils.isNumeric((String)input.get("lineNum"))) {
                            lines = Integer.parseInt((String) input.get("lineNum"));
                        }
                    }
                    return getErrorMessage((String)input.get("srvid"),(String)((Map)env.get("${env}")).get("logUserName"),(String)((Map)env.get("${env}")).get("logPassword"),(String)input.get("date"),lines);
                }else if("init".equals(op)){
                    log.info("reInit system");
                    doInitial();
                }else if("getDesc".equals(op)){
                    String srvName = (String)input.get("name");
                    Map ret=null;
                    if(StringUtils.isNotBlank(srvName)){
                        ret= getDescStructure(srvName);
                    }
                    if(log.isDebugEnabled())
                    log.debug("get desc "+srvName+" desc:"+ret);
                    return ret;
                }else if("getActionPackage".equals(op)){
                    String srvName = (String)input.get("name");
                    if(StringUtils.isNotBlank(srvName)){
                        Map m= getDescStructure(srvName);
                        if(null != m){
                            return m.get("package");
                        }
                        return null;

                    }
                }else if("eventListener".equals(op)){
                    String path = (String)input.get("path");
                    String srvid = path.substring(path.lastIndexOf(".")+1);
                    String insid = null;

                    Object data = input.get("data");
                    if(StringUtils.isNotBlank(path) && null != data){
                        if(path.startsWith(statusstorepath)){
                            int n = path.indexOf("INS-");
                            if(n>0) {
                                int l = path.indexOf(INSID_SRVNAME_SPLITCHAR);
                                int nn = path.lastIndexOf("/");
                                if (l > nn) {
                                    insid = path.substring(nn+ 1, l);
                                }
                            }
                            if("true".equals(data) && StringUtils.isNotBlank(srvid)){
                                String[] insids=null;
                                if(StringUtils.isNotBlank(insid)){
                                    insids = new String[]{insid};
                                }
                                notifyByActiveSrv(srvid,insids);
                            }
                            if("false".equals(data) && StringUtils.isNotBlank(srvid)){
                                String insids=null;
                                if(StringUtils.isNotBlank(insid)){
                                    insids =insid;
                                }
                                notifyBySuspendSrv(srvid,insids);
                            }
                        }
                    }
                }else if("getEnv".equals(op)){
                    if(null != getPropertyObject("env")) {
                        Map ret = new LinkedHashMap();
                        XMLMakeup xml = ((XMLObject) getPropertyObject("env")).getXML();
                        List<XMLMakeup> ps = xml.getChildren();
                        if (null != ps) {
                            for (XMLMakeup p : ps) {
                                String k = (String) p.getProperties().get("key");
                                String v = (String) p.getText();
                                ret.put(k, v);
                            }
                        }
                        return ret;
                    }
                    return null;
                }else if("isredo".equals(op)){
                    String name = (String)input.get("name");
                    if(StringUtils.isNotBlank(name)){
                        XMLObject x = getObjectById(name);
                        if(null != x){
                            return x.isRedo();
                        }
                    }
                    return false;
                }else if("getHelpMenus".equalsIgnoreCase(op)){
                    String helppath = (String)((Map)env.get("${env}")).get("ServicesDir");
                    if(null != resource && StringUtils.isNotBlank(helppath)){
                        Map map = new HashMap();
                        map.put("url",helppath+"../menus.json");
                        map.put("op","getTextContent");
                        String content = (String)resource.doSomeThing(null,null,map,null,null);
                        if(StringUtils.isNotBlank(content)){
                            return content;
                        }
                    }
                }else if("getAlarmActions".equals(op)){
                    Map map = getXMLObjectContainer();
                    Iterator its = map.keySet().iterator();
                    List list = new ArrayList();
                    list.add("OtherErrorLogAlarm");// it is fix alarm action for ELK collected log files
                    while(its.hasNext()){
                        String name = (String)its.next();
                        Object o = map.get(name);
                        if(o instanceof XMLDoObject){
                            if(((XMLDoObject)o).isAlarm()){
                                list.add(name);
                            }
                        }

                    }
                    if(list.size()>0) {
                        return list;
                    }

                }else if("getSrvInfoBySrvIdFromCenter".equals(op)){
                    String id = (String)input.get("srvId");
                    if(StringUtils.isNotBlank(id)){
                        return srvInfoInCenter.get(id);
                    }
                }else if("getServicesDescJar".equals(op)){
                    List<String> ids = (List)input.get("srvIds");
                    String jarName = (String)input.get("jarName");
                    String savepath = (String)input.get("savePath");
                    Map<String,String> descs = new HashMap();
                    getAllChildrenDescStringList(env,ids,descs,new ArrayList<String>());
                    getServicesDescJar(env,descs,jarName,savepath);
                }else if("getLibJar".equals(op)){
                    String lib = (String)input.get("libpath");
                    List<String> descjars = (List)input.get("descjars");
                    String jarname = (String)input.get("jarname");
                    String temp = (String)input.get("temppath");
                    List<String> excludes= (List)input.get("excludes");
                    combineJars(null,lib,excludes,jarname,temp,descjars,null);
                    return jarname;
                }else if("generatorApplication".equals(op)){
                    List<String> srvNames = (List)input.get("services");
                    String newJarName = (String)input.get("newJarName");
                    String appName = (String)input.get("appName");
                    String webname = (String)input.get("webName");
                    List excludes = (List)input.get("excludes");
                    List descjars = (List)input.get("descjars");
                    String acctCode=(String)((Map)env.get("${session}")).get("UserName");

                    String buildroot =(String)((Map)env.get("${env}")).get("buildDir");
                    if(StringUtils.isNotBlank(buildroot)) {
                        if (!(buildroot.endsWith("/") || buildroot.endsWith("\\"))) {
                            buildroot = buildroot + "/";
                        }
                        Map<String, Map<String, String>> pars = (Map) input.get("parameters");
                        return generatorApplication(newJarName,env, srvNames,excludes, appName, acctCode,webname, buildroot, pars,descjars);
                    }else{
                        throw new ISPException("SYSTEM.NOT_CONFIG_BUILD_PATH","please config [buildDir] property in main file");
                    }
                }else if("getWebNames".equals(op)){
                    String buildroot =(String)((Map)env.get("${env}")).get("buildDir");
                    if(StringUtils.isNotBlank(buildroot)) {
                        if (!(buildroot.endsWith("/") || buildroot.endsWith("\\"))) {
                            buildroot = buildroot + "/";
                        }
                        return getWebNames(env, buildroot);
                    }else{
                        throw new ISPException("SYSTEM.NOT_CONFIG_BUILD_PATH","please config [buildDir] property in main file");
                    }
                }else if("getServicesIntroduction".equals(op)){ //only for the input services exclude children services
                    List<String> names = (List)input.get("names");
                    if(null != names){
                        return getServicesIntroduction(env,names);
                    }
                    return null;
                }else if("getServiceAllAuthors".equals(op)){
                    String name = (String)input.get("name");
                    if(StringUtils.isNotBlank(name)) {
                        return getServiceAuthors(env, name);
                    }
                }

            }

        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
    List getWebNames(XMLParameter env,String buildroot){
        if(StringUtils.isNotBlank(buildroot)) {
            String web = buildroot + "user/webs";
            File f = new File(web);
            if(f.exists()) {
                File[] fs = f.listFiles();
                if(null != fs){
                    List<String> ret = new ArrayList();
                    for(File fi:fs){
                        ret.add(fi.getName());
                    }
                    return ret;
                }
            }
        }
        return null;
    }
    List getServicesIntroduction(XMLParameter env,List<String> names) throws Exception {
        List ret =new ArrayList();
        for(String s:names){
            String desc = getServiceDescString(s,env);
            if(null != desc) {
                Map d = StringUtils.convert2MapJSONObject(desc);
                if(null != d){
                    HashMap i = new HashMap();
                    i.put("name",d.get("name"));
                    i.put("package",d.get("package"));
                    i.put("input",d.get("input"));
                    i.put("output",d.get("output"));
                    i.put("date",d.get("date"));
                    i.put("desc",d.get("desc"));
                    i.put("error",d.get("error"));
                    i.put("depend",d.get("depend"));
                    i.put("scene",d.get("scene"));
                    i.put("example",d.get("example"));
                    i.put("busiArchitecture",d.get("busiArchitecture"));
                    i.put("techArchitecture",d.get("techArchitecture"));
                    ret.add(i);
                }
            }
        }
        return ret;
    }
    Object generatorApplication(String newJarName,XMLParameter env,List<String> srvNames,List<String> excludejars,String appName,String acctCode
            ,String webname,String approot,Map<String,Map<String,String>> parameters,List<String> descjars)throws Exception{
        //service jar
        log.info("root path:"+approot);
        String web=null;
        if(StringUtils.isNotBlank(webname)) {
            web = approot + "user/webs/"+webname;
        }
        String path = approot+"user/"+acctCode+"/"+appName;
        log.info("user path:"+path);
        new File(path+"/lib/").mkdirs();
        //get desc list
        Map<String,String> descs = new HashMap();
        getAllChildrenDescStringList(env,srvNames,descs,new ArrayList());
        //generator desc jar
        getServicesDescJar(env, descs, appName + "_services.jar", path + "/lib/");
        log.info("finished services jar:" + path + "/lib/" + appName + "_services.jar");
        //copy
        new File(path+"/bin").mkdirs();
        new File(path+"/classes").mkdirs();
        new File(path+"/data").mkdirs();
        new File(path+"/logs").mkdirs();
        new File(path+"/web").mkdirs();
        new File(path+"/doc").mkdirs();
        FileUtils.copyDict(new File(approot+"user/define/bin"),new File(path+"/bin"),null,null,null,null);
        FileUtils.copyDict(new File(approot+"user/define/classes"),new File(path+"/classes"),null,null,null,null);
        FileUtils.copyDict(new File(approot+"user/define/data"),new File(path+"/data"),null,null,null,null);
        FileUtils.copyDict(new File(approot+"user/define/logs"),new File(path+"/logs"),null,null,null,null);
        FileUtils.copyDict(new File(approot+"user/define/doc"),new File(path+"/doc"),null,null,null,null);
        //generator tablecontainer.xml

        generatorTableContainer(env,descs, path + "/classes/funs/tablecontainer.xml");
        //get authors
        Map authors = getServiceCreateUsers(env,descs);
        if(null != authors){
            FileUtils.saveFile(new StringBuffer(ObjectUtils.convertMap2String(authors)),path+"/doc/Authors.txt",true,false);
        }
        //get services introduction
        List ls = getServicesIntroduction(env,srvNames);
        if(null != ls){
            FileUtils.saveFile(new StringBuffer(ObjectUtils.convertList2String(ls)),path+"/doc/ServicesIntroduction.txt",true,false);
        }
        //lib jar
        String lib = approot+"../lib";
        log.info("lib path:"+lib);
        String jarname = path+"/lib/"+newJarName+".jar";
        String temp = path+"/temp";
        FileUtils.deleteDir(temp);
        File f = new File(temp);
        f.mkdirs();
        File lf = new File(lib);
        if(lf.exists()) {
            File[] fs = lf.listFiles();
            if(null != fs ) {
                if(fs.length==1){
                    FileUtils.copyFile(fs[0],new File(jarname));
                }else {
                    combineJars(approot, lib, excludejars, jarname, temp, descjars, parameters);
                }
            }
        }
        //remove excludejars from zip/lib
        /*if(null != excludejars) {
            for(String e:excludejars) {
                FileUtils.copyFile(lib+"/"+e,path+"/lib/"+e);
            }
        }*/
        log.info("finished lib jar:"+jarname);
        FileUtils.deleteDir(temp);

        if(StringUtils.isNotBlank(web)){
            File webfile = new File(web);
            if(webfile.exists()){
                FileUtils.copyDict(webfile, new File(path + "/web"), null, null, null, null);
            }else {
                FileUtils.copyDict(new File(approot + "user/define/web"), new File(path + "/web"), null, null, null, null);
            }
        }else{
            FileUtils.copyDict(new File(approot + "user/define/web"), new File(path + "/web"), null, null, null, null);
        }
        log.info("finished copy config files.");
        if(new File(path+"/web/WEB-INF/web.xml").exists()){
            Map mt = new HashMap();
            mt.put("tb_web.app",appName+".app");
            parameters.put("web.xml",mt);
        }
        log.info("finished change web.xml start config files.");
        //parameters
        if(null != parameters) {
            Iterator its = parameters.keySet().iterator();
            while(its.hasNext()) {
                String k = (String)its.next();
                String fn = path+"/"+k;
                File ff = new File(fn);
                if(ff.exists()) {
                    FileInputStream fi = new FileInputStream(ff);
                    byte[] bs = FileUtils.replaceFile(fi, parameters.get(k), "UTF-8");
                    FileUtils.saveStringBufferFile(new StringBuffer(new String(bs)), fn, false);
                    fi.close();
                }
            }
        }
        log.info("finished parameters changed");
        new File(path+"/classes/tb_web.app").renameTo(new File(path+"/classes/"+appName+".app"));
        log.info("finished change start file name");
        //zip
        String zipfile=path+"/"+appName+".zip";
        log.info("ziping file name:"+zipfile);
        File wf = new File(path+"/web");
        String isweb = "false";
        if(wf.exists()){
            File[] l = wf.listFiles();
            if(null != l && l.length>0){
                isweb="true";
            }
        }
        ZipUtil.zipFiles(zipfile,new String[]{path+"/bin",path+"/doc",path+"/classes",path+"/data",path+"/logs",path+"/web",path+"/lib"});
        log.info("finished zip file:"+zipfile);
        FileUtils.deleteDir(path+"/bin");
        FileUtils.deleteDir(path+"/classes");
        FileUtils.deleteDir(path+"/data");
        FileUtils.deleteDir(path+"/logs");
        FileUtils.deleteDir(path+"/doc");
        FileUtils.deleteDir(path+"/web");
        FileUtils.deleteDir(path+"/lib");
        long size = new File(zipfile).length();
        HashMap m = new HashMap();
        m.put("path",zipfile);
        m.put("size",size);
        m.put("isweb",isweb);
        m.put("count",descs.size());
        File logof = new File(path+"/upload/logo.png");
        if(logof.exists()){
            m.put("logo_path",path+"/upload/logo.png");
        }
        String s= ArrayUtils.toDistinctString(authors.values());
        if(null !=s) {
            if(s.length()>1024) {
                m.put("authors", s.substring(0, 1024));
            }else{
                m.put("authors", s);
            }
        }
        return m;

    }

    void combineJars(String rootpath,String lib,List extjars,String jarnamepath,String temppath,List<String> descjars,Map<String,Map<String,String>> parameters)throws Exception{
        List<String> ls =FileUtils.getAllFileNames(lib,"jar");
        ArrayUtils.sortByLen(ls,ArrayUtils.ABS);
        List<String> es = new LinkedList<String>();
        if(null != ls && ls.size()>0) {
            for(int i=ls.size()-1;i>=0;i--) {
                if(null != descjars && ArrayUtils.isLikeArrayInString(ls.get(i), descjars)){
                    es.add(ls.get(i));
                    continue;
                }
                String n = ls.get(i).contains("/")?ls.get(i).substring(ls.get(i).lastIndexOf("/")+1):ls.get(i).substring(ls.get(i).lastIndexOf("\\")+1);
                if(null != extjars && ArrayUtils.isInStringArray(extjars,n))
                    continue;
                try {
                    ZipUtil.unZipFile(ls.get(i), temppath);
                }catch(Exception e){
                    log.error("unable to unzip file:"+ls.get(i),e);
                    throw e;
                }
                log.info("unzip jar:"+ls.get(i)+ " to temp:"+temppath);
            }

            if(null != descjars) {
                for(int j=descjars.size()-1;j>=0;j--) {
                    for (int i = es.size() - 1; i >= 0; i--) {
                        if(es.get(i).indexOf(descjars.get(j))>=0) {
                            String n = es.get(i).contains("/") ? es.get(i).substring(es.get(i).lastIndexOf("/") + 1) : es.get(i).substring(es.get(i).lastIndexOf("\\") + 1);
                            if (null != extjars && ArrayUtils.isInStringArray(extjars, n))
                                continue;
                            try {
                                ZipUtil.unZipFile(es.get(i), temppath);
                            } catch (Exception e) {
                                log.error("unable to unzip file:" + es.get(i), e);
                                throw e;
                            }
                            log.info("unzip jar:" + es.get(i) + " to temp:" + temppath);
                            es.remove(i);
                        }
                    }
                }
            }
        }
        if(null != parameters){
            Collection s = parameters.values();
            if(null != s){
                Iterator<Map<String,String>> its = s.iterator();
                while(its.hasNext()){
                    Map<String,String> ts = its.next();
                    Iterator<String> ti = ts.keySet().iterator();
                    String sv=null;
                    while(ti.hasNext()){
                        String k= ti.next();
                        String v= ts.get(k);
                        if(null != k && k.equals("<property key=\"ServerHost\"></property>") && StringUtils.isNotBlank(v)){
                            List l = StringUtils.getTagsNoMark(v,">","<");
                            if(null != l && l.size()>0) {
                                String host = (String)l.get(0);
                                if(StringUtils.isNotBlank(host)) {
                                    GeneratorClass.appendMethodInClassPath(temppath, false, "com.octopus.utils.xml.XMLObject", new String[]{"getXMLInstance"}, "if(!\"" + host + "\".equals(com.octopus.utils.net.NetUtils.getip())){System.out.println(\"the server not match the ip machine! please check the server ip[" + host + "]\");System.exit(1);}", null, null, temppath);
                                    sv = host;
                                }
                            }
                        }
                        if(null != sv && null != k && k.equals("<property key=\"ServerHost.ServerCount\"></property>") && StringUtils.isNotBlank(v)){
                            List l = StringUtils.getTagsNoMark(v,">","<");
                            if(null != l && l.size()>0) {
                                String count = (String)l.get(0);
                                if(StringUtils.isNotBlank(count)) {
                                    GeneratorClass.appendMethodInClassPath(temppath, false, "com.octopus.tools.pubsub.ZkClientListen", new String[]{"connzk"}, null, null, "if(getServerCount(\"" + sv + "\")>" + count + "){System.out.println(\"Over the max server count[" + count + "] in this Host [" + sv + "]\");System.exit(1);}", temppath);
                                }
                            }
                        }
                    }
                }
            }
        }
        //over META-INF
        if(StringUtils.isNotBlank(rootpath)) {
            FileUtils.copyDict(new File(rootpath + "user/define/META-INF"), new File(temppath+"/META-INF"), null, null, null, null);
        }
        ZipUtil.jarFolder(temppath,jarnamepath);
    }
    //get desc String content
    String getServiceDescString(String n,XMLParameter env)throws Exception{
        String d=null;
        if (null != getObjectById(n)) {
            Map m = getDescStructure(n);
            d = ObjectUtils.convertMap2String(m);
        } else {
            String[] insids = getInsNameBySrvId(n);
            if (null != insids && insids.length > 0) {
                String ins = insids[0];
                if (StringUtils.isNotBlank(ins)) {
                    XMLDoObject re = (XMLDoObject) getObjectById("restfulClient");
                    HashMap in = new HashMap();
                    in.put("insid", ins);
                    in.put("srvid", "getDesc");
                    in.put("data", "{name:'" + n + "'}");
                    re.doCheckThing(null, env, in, null, null, null);
                    Object o = env.getResult();
                    if (null != o) {
                        if(((ResultCheck) o).getRet() instanceof String) {
                            d = (String) ((ResultCheck) o).getRet();
                        }
                        if(((ResultCheck) o).getRet() instanceof Map){
                            d = ObjectUtils.convertMap2String((Map)((ResultCheck) o).getRet());
                        }
                    }
                }
            }
        }
        return  d;
    }



    /**
     * get desc file zip to jar outStream
     * @return
     * @throws Exception
     */
    ByteArrayOutputStream getServicesJarOutputStream(Map<String,String> descs)throws Exception{
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JarOutputStream jout = new JarOutputStream(out);
        if(null != descs) {
            Iterator<String> ist = descs.keySet().iterator();
            while (ist.hasNext()) {
                String n = ist.next();
                String d = descs.get(n);
                if (null != d) {
                    ByteArrayInputStream in = new ByteArrayInputStream(d.getBytes());
                    ZipUtil.addFile(jout, n + ".desc", in);
                    in.close();
                }
            }

            jout.close();
            return out;
        }
        return null;
    }
    void getServicesDescJar(XMLParameter env,Map<String,String> descs,String jarName,String savepath)throws Exception{
        ByteArrayOutputStream out = getServicesJarOutputStream(descs);
        if(null != out){
            if (StringUtils.isBlank(savepath)) {
                HttpServletResponse response = (HttpServletResponse) env.get("${response}");
                if (null != response) {
                    response.setContentType("application/octet-stream;charset=UTF-8");
                    response.setHeader("Content-disposition", "attachment; filename=" + jarName.toString());
                    //写入
                    response.getOutputStream().write(out.toByteArray());
                    //关闭
                    response.flushBuffer();
                    env.put("^isstop", "true");
                }
            } else {
                FileOutputStream o = new FileOutputStream(savepath + "/" + jarName);
                o.write(out.toByteArray());
                o.close();
            }
        }
    }

    void generatorTableContainer(XMLParameter env,Map<String,String> descs,String fileName) throws Exception {
        if(null != descs){
            Iterator<String> its = descs.keySet().iterator();
            List<String>ts = new ArrayList();
            while(its.hasNext()){
                String c= descs.get(its.next());
                getTablesFromDesc(c, ts);
            }


            XMLDoObject tc = (XMLDoObject)getObjectById("tablecontainer");
            if(null != tc) {
                HashMap input = new HashMap();
                input.put("op", "generatorTableContainer");
                input.put("tables", ts);
                StringBuffer sb = (StringBuffer) tc.doSomeThing(null, env, input, null, null);
                if (null != sb) {
                    FileUtils.makeFilePath(fileName);
                    FileUtils.saveFile(sb, fileName, true, false);
                }
            }

        }
    }
    Map<String,String> getServiceAuthors(XMLParameter env,String name) throws Exception {
        String s = getServiceDescString(name,env);
        if(StringUtils.isNotBlank(s)) {
                HashMap tm = new HashMap();
                tm.put(name,s);
                return getServiceCreateUsers(env,tm);
        }
        return null;
    }
    //get user in desc return {servicename:'',user:''}
    Map<String,String> getServiceCreateUsers(XMLParameter env,Map<String,String> servicesDesc){
        if(null != servicesDesc) {
            Map ret = new HashMap();
            Iterator<String> its = servicesDesc.keySet().iterator();
            while(its.hasNext()){
                String k = its.next();
                String s =servicesDesc.get(k);
                if(StringUtils.isNotBlank(s)){
                    try {
                        Map t = StringUtils.convert2MapJSONObject(s);
                        if(null != t) {
                            if(StringUtils.isNotBlank(t.get("author"))){
                                ret.put(k,t.get("author"));
                            }
                        }
                    }catch (Exception e){

                    }
                }
            }
            return ret;
        }
        return null;
    }

    //get desc content by name
    void getAllChildrenDescStringList(XMLParameter env,List<String> ids,Map<String,String> rtn,List<String> temp)throws Exception{
        if(null != ids) {
            for (String n : ids) {
                if(!temp.contains(n)) {
                    String d = getServiceDescString(n, env);
                    if (StringUtils.isNotBlank(d)) {
                        rtn.put(n, d);
                        temp.add(n);
                        List<String> srs = StringUtils.getTagsNoMark(d, " action=\\\"", "\\\"");
                        getAllChildrenDescStringList(env,srs,rtn,temp);
                    }
                }
            }
        }
    }

    //get table names from desc
    void getTablesFromDesc(String desContent,List targetNames){
        XMLDoObject tc = (XMLDoObject)getObjectById("tablecontainer");
        if(tc instanceof TableContainer){
            Set tables = ((TableContainer)tc).getAllTables().keySet();
            List<String> ls = StringUtils.getTagsNoMark(desContent,"table:'","'");
            if(null != ls){
                for(String s:ls){
                    String us = s.toUpperCase();
                    if(tables.contains(us) && !targetNames.contains(us)){
                        targetNames.add(us);
                    }
                }
            }
            List<String> qs = StringUtils.getTagsNoMark(desContent,"sqls:[","]");
            if(null != ls){
                for(String s:qs){
                    s = s.toUpperCase();
                    Iterator<String> ta = tables.iterator();
                    while(ta.hasNext()){
                        String t =  ta.next();
                        if(s.indexOf(t)>0){
                            if (!targetNames.contains(s)) {
                                targetNames.add(s);
                            }
                        }
                    }

                }
            }

        }
    }

    String getErrorMessage(String srvid,String username,String pwd,String yyyymmddhh,int linenum){
        if(linenum==0)linenum=200;
        if(StringUtils.isBlank(yyyymmddhh)) yyyymmddhh=DateTimeUtils.getStringDateByPattern("yyyyMMddhh",null);
        String f = "SRV_ERROR_LOG_"+srvid.trim()+"."+ yyyymmddhh;
        List<Map> ls = getInsList();
        if(log.isDebugEnabled()) {
            log.debug("getErrorMessage:" + ls);
        }
        if(null!=ls){
            StringBuffer sb = new StringBuffer();
            for(Map m:ls){

                String s = getErrorOneMessage((String)m.get("ip"),"22",username,pwd,(String)m.get("logPath")+"/"+f,linenum);
                if(StringUtils.isNotBlank(s)) {
                    sb.append((String)m.get("ip")).append("-").append(m.get("logPath")+"/"+f).append("\n").append(s).append("\n");
                }
            }
            return sb.toString();
        }else{
            try {
                return getErrorOneMessage("127.0.0.1", "22", "", "", ((Map) getEmptyParameter().get("${env}")).get("logPath") + "/" + f,linenum);
            }catch (Exception e){}
        }
        return null;
    }
    String getErrorOneMessage(String host,String port,String userName,String userPasswd,String logFilePath,int lineNum){
        if(null != command){
            try {
                HashMap map = new HashMap();
                map.put("type", "shell");
                map.put("host",host);
                map.put("port",port);
                map.put("username",userName);
                map.put("password",userPasswd);
                map.put("data", "tail -n  "+lineNum+" " + logFilePath);
                return (String) command.doSomeThing(null, null, map, null, null);
            }catch (Exception e){

            }

        }
        return null;
    }
    Map getInstanceInfo(String insid){
        try {
            HashMap map = new HashMap();
            map.put("op", "getData");
            map.put("path", serverspath + "/" + insid);
            String d = (String) srvhandler.doSomeThing(null, null, map, null, null);
            return StringUtils.convert2MapJSONObject(d);
        }catch (Exception e){

        }
        return null;
    }

    List<Map> getInsList(){
        try {
            /*HashMap map = new HashMap();
            map.put("op", "getChildren");
            map.put("path", serverspath);
            List<String> d = (List<String>) srvhandler.doSomeThing(null, null, map, null, null);
            if(null !=d && d.size()>0){
                List<Map> ret = new ArrayList();
                map.clear();
                for(String m:d){
                    map.put("op", "getData");
                    map.put("path", serverspath + "/" + m);
                    String t = (String) srvhandler.doSomeThing(null, null, map, null, null);
                    if(log.isDebugEnabled()){
                        log.debug("get instance from zk path ["+map.get("path")+"] data ["+t+"]");
                    }
                    if(StringUtils.isNotBlank(t)){
                        ret.add(StringUtils.convert2MapJSONObject(t));
                    }
                }
                //return ret;
                System.out.println();
            }*/
            HashMap in = new HashMap();
            in.put("op", "getChildrenData");
            in.put("path", serverspath);
            Map<String, String> map2 = (Map)srvhandler.doSomeThing(null, null, in, null, null);
            if(null != map2 && map2.size()>0){
                List<Map> ret = new ArrayList();
                Iterator<String> its = map2.keySet().iterator();
                while(its.hasNext()){
                    String k = its.next();
                    ret.add(StringUtils.convert2MapJSONObject(map2.get(k)));
                }
                return ret;
            }
        }catch (Exception e){

        }
        return null;
    }

    String[] getInsListIds(){
        List<Map> ls = getInsList();
        if(null != ls){
            List<String> ret = new ArrayList();
            for(Map m:ls){
                if(!ret.contains(m.get("insId"))){
                    ret.add((String)m.get("insId"));
                }
            }
            return (String[])ret.toArray(new String[0]);
        }
        return null;
    }

    ConcurrentHashMap addressmap = new ConcurrentHashMap();
    String getAddressMapKey(String insid,String name){
        return insid+"_"+"_"+name;
    }
    String getCacheInsAddress(Map insinfo,String name){
        if(null != insinfo) {
            String insid = (String) insinfo.get("insId");
            if (log.isDebugEnabled()) {
                log.debug("get instance info by " + insid + " " + insinfo);
            }
            if (null != insinfo) {
                String ip = (String) insinfo.get("ip");
                String webport = (String) insinfo.get("port");

                if (StringUtils.isNotBlank(ip)) {
                    String ad = "http://" + ip + ":" + webport + "/" + "service?actions=" + name;
                    addressmap.put(getAddressMapKey(insid, name), ad);
                    if (log.isDebugEnabled()) {
                        log.debug(insid + " address " + ad);
                    }
                    return ad;
                }
            }
        }
        return null;
    }
    String getAddress(String insid,String port,String name){
        if(addressmap.containsKey(getAddressMapKey(insid,name))){
            String ad = (String)addressmap.get(getAddressMapKey(insid,name));
            if(log.isDebugEnabled()) {
                log.debug("instance "+insid + " address " + ad);
            }
            return ad;
        }
        try {
            Map insinfo = getInstanceInfo(insid);
            String ad =getCacheInsAddress(insinfo,name);
            if(StringUtils.isNotBlank(ad)){
                return ad;
            }

        }catch (Exception e){
            log.error("",e);
        }
        Bridge root = (Bridge)getObjectById("bridge");
        if(root.getInstanceId().equals(insid)) {
            String ad = "http://127.0.0.1:" + port + "/" + "service?actions=" + name;
            addressmap.put(getAddressMapKey(insid,name), ad);
            return ad;
        }else{
            return null;
        }
    }
    boolean startTrace()throws Exception{
        HashMap map = new HashMap();
        map.put("path",traceFlagPath);
        map.put("op","publishStartTrace");
        map.put("data","{op:'publishStartTrace'}");
        srvhandler.doSomeThing(null, null, map, null, null);
        return true;
    }
    boolean stopTrace()throws Exception{
        HashMap map = new HashMap();
        map.put("op","publishStopTrace");
        map.put("data","{op:'publishStopTrace'}");
        map.put("path",traceFlagPath);
        srvhandler.doSomeThing(null, null, map, null, null);
        return true;
    }
    static HashMap localCacheOfRemoteDesc = new HashMap();
    Map getLocalCacheOfRemoteDesc(String serviceName,String instanceId){
        if(StringUtils.isNotBlank(serviceName) && StringUtils.isNotBlank(instanceId)) {
            return (Map)localCacheOfRemoteDesc.get(instanceId+"."+serviceName);
        }
        return null;
    }
    void updateLocalCacheOfRemoteDesc(HashMap cache,String name,String instanceId,String op){
        if(getSelfInstanceId().contains("CONSOLE") && StringUtils.isNotBlank(instanceId) && StringUtils.isNotBlank(name)) {
            if ("DEL".equals(op)) {
                cache.remove(instanceId + "." + name);
            } else if ("ADD".equals(op)) {
                boolean is = false;
                Map desc = getRemoteDesc(name, instanceId);
                if(null != desc) {
                    cache.put(instanceId + "." + name, desc);
                    is = true;
                }
                if(log.isDebugEnabled())
                log.debug("add LocalCache :"+name+" ins:"+instanceId+" status:"+is);
            } else if ("UPD".equals(op)) {
                Map desc = getRemoteDesc(name, instanceId);
                if(null != desc)
                cache.put(instanceId + "." + name, desc);
            }
        }
    }
    Map getRemoteDesc(String name,String insId){
        if(null != remote){
            HashMap in = new HashMap();
            in.put("name",name);
            Object o = doRemoteAction(insId,"getDesc",in);
            if(null != o){
                if(o instanceof Map)
                return (Map)o;
                if(o instanceof String){
                    Map t = StringUtils.convert2MapJSONObject((String)o);
                    if(null != t){
                        return t;
                    }
                }
            }
        }
        return null;
    }

    Object doRemoteAction(String insId,String srvName,Map input){
        try {
            BFParameters p = new BFParameters(false);
            p.addParameter("${targetNames}", new String[]{srvName});
            p.addParameter("${input_data}", input);
            p.addParameter("${insid}", insId);
            Hashtable hb = new Hashtable();
            hb.put("targetinsid",insId);
            p.addParameter("${requestHeaders}",hb);
            remote.doThing(p, null);
            Object o = p.getResult();
            if (null != o) {
                if (o instanceof ResultCheck) {
                    o = ((ResultCheck) o).getRet();
                }
                return o;
            }
        }catch (Exception e){
            log.error("invoke remote error", ExceptionUtil.getRootCase(e));
        }
        return null;
    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getLog(URL url){
        try{
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

        }catch (Exception e){

        }
        return null;
    }



    @Override
    public ResultCheck checkReturn(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return new ResultCheck(true,ret);  //To change body of implemented methods use File | Settings | File Templates.
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
