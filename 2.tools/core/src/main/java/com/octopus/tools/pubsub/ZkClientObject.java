package com.octopus.tools.pubsub;

import com.alibaba.otter.canal.common.zookeeper.StringSerializer;
import com.octopus.isp.bridge.impl.Bridge;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cls.ClassUtils;
import com.octopus.utils.net.NetUtils;
import com.octopus.utils.si.jvm.JVMUtil;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import com.octopus.utils.xml.auto.logic.XMLLogic;
import org.I0Itec.zkclient.IZkConnection;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.serialize.ZkSerializer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.util.*;

/**
 * Created by admin on 2020/3/17.
 */
public class ZkClientObject extends XMLLogic {
    private static transient Log log = LogFactory.getLog(ZkClientObject.class);
    ZKClient zkClient;

    static Map<String,List<InvokeTaskByObjName>> envHandles = new HashMap();

    public ZkClientObject(XMLMakeup xml, XMLObject parent, Object[] containers) throws Exception {
        super(xml, parent, containers);
    }
    public void doInitial(){
        try {

            log.info("connecting zk server......");
            String config = getXML().getProperties().getProperty("config");
            Map cof = StringUtils.convert2MapJSONObject(config);
            zkClient = new ZKClient((String) cof.get("hostPort"), (Integer)cof.get("sessionTimeout"), (Integer)cof.get("connectTimeout"), new StringSerializer());

            addSystemFinishedEvent(new Runnable() {
                @Override
                public void run() {
                    addTriggerWatch();
                }
            });
        }catch (Exception e){
            log.error("zookeeper connect service error , will try again",e);
        }
    }

    void addTriggerWatch() {
        if (null != zkClient) {
            if (envHandles.size() > 0) {
                List temp = new ArrayList();
                Iterator its = envHandles.keySet().iterator();
                while (its.hasNext()) {
                    String k = (String) its.next();
                    String type = k.substring(0,k.indexOf("#"));
                    String path = k.substring(k.indexOf("#") + 1);
                    if (zkClient.exists(path) && !temp.contains(k)) {
                        try {
                            zkClient.monitorAllPathFrom(path);
                            temp.add(k);
                        }catch (Exception e){log.error("",e);}
                    }
                }
                temp.clear();
            }
        }
    }
    @Override
    public void addTrigger(Object cond, InvokeTaskByObjName task) throws Exception {
        log.info("add trigger cond "+cond);
        if(null != cond){
            Map m = null;
            if(cond instanceof String){
                m = StringUtils.convert2MapJSONObject((String)cond);
            }
            if(cond instanceof Map){
                m = (Map)cond;
            }
            if(null != m){
                String path = (String)m.get("path");
                List<String> ls = (List)m.get("events");
                if(null != ls && ls.size()>0){
                    for(String s:ls){
                        if(!envHandles.containsKey(s+"#"+path)){
                            envHandles.put(s+"#"+path,new LinkedList<InvokeTaskByObjName>());
                        }
                        if(!envHandles.get(s + "#" + path).contains(task)) {
                            List<InvokeTaskByObjName> list = envHandles.get(s + "#" + path);
                            InvokeTaskByObjName pre=null;
                            synchronized (list){
                                for(InvokeTaskByObjName n:list){
                                    if(n.getName().equals(task.getName())){
                                        pre = n;
                                    }
                                }
                            }
                            if(null != pre) {
                                list.remove(pre);
                            }
                            list.add(task);
                            zkClient.monitorAllPathFrom(path);
                        }
                    }
                }
            }
        }
    }
    public void doEvent(String type,String path,Object data){
        Iterator its = envHandles.keySet().iterator();
        while(its.hasNext()){
            String k = (String)its.next();
            if((type+"#"+path).startsWith(k)){
                List<InvokeTaskByObjName> ts = envHandles.get(k);
                if(null != ts && ts.size()>0){
                    for(InvokeTaskByObjName t:ts) {
                        XMLObject obj = getObjectById(t.getName());
                        if (null != obj) {
                            t.setXMlObject(obj);
                            t.run();
                        }
                    }
                }
            }
        }
    }
    private String[] register(){

            log.debug("java.library.path\n"+System.getProperty("java.library.path"));
            //register this instance info to zk
                XMLObject rt = getRoot();
                Bridge root=null;
                if(null != rt && rt instanceof Bridge){
                    root = (Bridge) rt;
                }
                if(null == root) {
                    root = (Bridge) getObjectById("bridge");
                }
                if(null != root) {
                    String id = root.getInstanceId();
                    try {
                        //if did not exist in zk ,or name is CONSILE or reconnection
                        if (id.contains("CONSOLE")) {
                            try {
                                Thread.sleep(2000);
                            } catch (Exception e) {
                            }
                        }

                        Map properties = getEnvProperties();
                        String logpath = properties.get("logDir") + "cur";
                        if (logpath.startsWith("./") || logpath.startsWith("../")) {
                            File f = new File("");
                            logpath = f.getAbsolutePath() + "/" + logpath;
                        }
                        String webport = (String) properties.get("webport");
                        String wshost = (String) properties.get("ws_host");
                        //if web port from system.property
                        String twp = System.getProperty("tb-webport");
                        if (StringUtils.isNotBlank(twp)) {
                            webport = twp;
                        }
                        String twsp = System.getProperty("tb-wsaddress");
                        if (StringUtils.isNotBlank(twsp)) {
                            wshost = twsp;
                        }
                        String startTime = "";
                        if (null != root) {
                            startTime = root.getStartTime();
                        }
                        String data = "{\"insId\":\"" + id + "\",\"ip\":\"" + NetUtils.getip() + "\",\"port\":\"" + webport + "\",\"ws_host\":\""
                                + wshost + "\",\"loginuser\":\"" + properties.get("logUserName") + "\",\"loginpwd\":\"" + properties.get("logPassword")
                                + "\",\"jmxRemotePort\":\"" + System.getProperty("com.sun.management.jmxremote.port") + "\",\"pid\":\""
                                + JVMUtil.getPid() + "\",\"startTime\":\"" + startTime + "\",\"timestamp\":\"" + System.currentTimeMillis() + "\",\"logPath\":\"" + logpath + "\"}";
                        return new String[]{id,data};
                    }catch (Exception e) {
                        return null;
                    }

                    } else{
                        return null;
                    }

    }
    void createParentAddListener(String p)throws Exception{
        try {
            int n = p.indexOf("/", 1);
            while (n > 0) {
                String t = p.substring(0, n);
                if (!zkClient.exists(t)) {
                    zkClient.createPersistent(t);
                    zkClient.monitorAllPathFrom(t);
                    log.info("create path listener "+t);
                }
                n = p.indexOf("/", n + 1);
                try {
                    Thread.sleep(500);
                }catch (Exception e){}
            }
        }catch (Exception e){
            log.error("zk create path error ["+p+"]",e);
            throw e;
        }
    }
    void createParent(String p){
        int n = p.indexOf("/",1);
        while(n>0) {
            String t = p.substring(0, n);
            if(null != zkClient && !zkClient.exists(t)){
                zkClient.createPersistent(t);
            }
            n=p.indexOf("/",n+1);
        }
    }
    public Object doSomeThing(String xmlid,XMLParameter env, Map input, Map output,Map config) throws Exception {
        String op = null;
        if(null != input) {
            op = (String) input.get("op");
        }
        log.debug("zkClient op " + input);
        if("stop".equals(op)){
            if(null != zkClient){
                zkClient.close();
            }
        }
        try {
            if (null != input && null != zkClient && null != input && null != op && op.startsWith("publish")
                    && null != input.get("path") && null != input.get("data")) {
                String p = (String) input.get("path");
                createParentAddListener(p);
                if (!zkClient.exists(p)) {
                    zkClient.createPersistentWatchPath(p,true);
                }else {
                    Boolean b = (Boolean) ClassUtils.invokeMethod(zkClient, "hasListeners", new Class[]{String.class}, new Object[]{p});
                    if (!b) {
                        zkClient.addDataWatch(p);
                    }
                    if (((String) input.get("op")).contains("Delete")) {
                        zkClient.delete(p);
                    } else {
                        if(log.isInfoEnabled()) {
                            log.info("wirtedata " + ((String) input.get("data")));
                        }
                        zkClient.writeData(p, ((String) input.get("data")), -1);
                    }
                    return true;
                }
                return false;
            } else if ("delete".equals(op)) {
                String p = (String) input.get("path");
                if (StringUtils.isNotBlank(p) && zkClient.exists(p)) {
                    zkClient.delete(p);
                }
                return true;
            }else  if(null!= zkClient && "deleteChildren".equals(op)){
                String p = (String) input.get("path");
                List<String> ls = zkClient.getChildren(p);
                if(null != ls){
                    for(String s:ls){
                        zkClient.delete(p+"/"+s);
                    }
                }
            }else if (null != zkClient && null != input && "addChildrenDataListener".equals(op)) {
                String p = (String) input.get("path");
                if (!zkClient.exists(p)) {
                    zkClient.createPersistent(p,true);
                }
                zkClient.monitorAllPathFrom(p);
                return true;

            } else if (null != zkClient && null != input && "addChildrenPathListener".equals(op)) {
                String p = (String) input.get("path");

                if (!zkClient.exists(p)) {
                    zkClient.createPersistent(p,true);
                }
                zkClient.monitorAllPathFrom(p);
                return true;
            } else if (null != zkClient && null != input && "addPathDataListener".equals(op)) {
                String p = (String) input.get("path");

                if (!zkClient.exists(p)) {
                    zkClient.createPersistent(p,true);
                }

                zkClient.addDataWatch(p);
                log.info("append zk data listener " + p);
                return true;
            } else if (null != zkClient && null != input && "setData".equals(op)) {
                String p = (String) input.get("path");
                String data = ObjectUtils.toString(input.get("data"));
                createParentAddListener(p);
                if (!zkClient.exists(p)) {
                    zkClient.createPersistent(p);
                }
                zkClient.writeData(p, data, -1);
                return true;
            } else if (null != zkClient && null != input && "onlySetData".equals(op)) {
                String p = (String) input.get("path");
                String data = (String) input.get("data");
                String isLeaderDo = (String)input.get("isLeaderDo");
                if(StringUtils.isNotBlank(isLeaderDo) && StringUtils.isTrue(isLeaderDo)){
                    if(!zkClient.isLeader()) return false;
                }
                zkClient.createPersistent(p,true);

                zkClient.writeData(p, data, -1);
                log.debug("this is leader. onlySetData:" + p + "\n" + data);
                return true;
            }else if (null != zkClient && null != input && "onlyWriteData".equals(op)) {
                String p = (String) input.get("path");
                String data = (String) input.get("data");
                if(zkClient.exists(p)){
                    zkClient.writeData(p, data, -1);
                    log.debug("onlyWriteData:" + p + "\n" + data);
                    return true;
                }
            } else if (null != zkClient && null != input && "onlySetTempData".equals(op)) {
                String p = (String) input.get("path");
                String data = (String) input.get("data");
                zkClient.createEphemeral(p);

                zkClient.writeData(p, data, -1);
                log.debug("onlySetTempData:" + p + "\n" + data);
                return true;
            } else if (null != zkClient && null != input && "getData".equals(op)) {
                String p = (String) input.get("path");
                if (zkClient.exists(p)) {
                    return zkClient.readData(p);
                }
                return null;
            } else if (null != zkClient && null != input && "getChildren".equals(op)) {
                String p = (String) input.get("path");
                if (zkClient.exists(p)) {
                    return zkClient.getChildren(p);
                }
                return null;
            } else if (null != zkClient && null != input && "isExist".equals(op)) {
                try {
                    String p = (String) input.get("path");
                    Object o = (zkClient.exists(p));
                    if (null != o && o instanceof Boolean && (Boolean) o) {
                        return Boolean.TRUE;
                    } else {
                        return Boolean.FALSE;
                    }
                }catch (Exception e){
                    return Boolean.FALSE;
                }
            } else if (null != zkClient && null != input && "addPathListener".equals(op)) {
                String p = (String) input.get("path");
                String type = (String) input.get("type");
                createParent(p);
                if (!zkClient.exists(p)) {
                    if (StringUtils.isNotBlank(type) && "temp".equals(type)) {
                        zkClient.createEphemeral(p);
                    } else {
                        zkClient.createPersistent(p);
                    }
                }
                zkClient.addPathWatch(p);
                return null;
            } else if (null != zkClient && null != input && "addPath".equals(op)) {
                String p = (String) input.get("path");
                String type = (String) input.get("type");
                createParent(p);
                if (!zkClient.exists(p)) {
                    if (StringUtils.isNotBlank(type) && "temp".equals(type)) {
                        zkClient.createEphemeral(p);
                    } else {
                        zkClient.createPersistent(p);
                    }
                }
                return null;
            } else if (null != zkClient && null != input && "getChildrenData".equals(op)) {
                HashMap m = new HashMap();
                String p = (String) input.get("path");
                if (zkClient.exists(p)) {
                    List<String> cl = zkClient.getChildren(p);
                    if (null != cl) {
                        for (String c : cl) {
                            if(zkClient.exists(p + "/" + c)) {
                                String s = zkClient.readData(p + "/" + c);
                                if (null != s) {
                                    m.put(c, s);
                                }
                            }
                        }
                    }
                }

                return m;
            } else if (null != env) {
                return super.doSomeThing(xmlid, env, input, output, config);
            } else {
                return null;
            }
        }catch (Exception e){
            log.error("zk client op error",e);
        }
        return null;
    }

    Object getConfig(String p){
        String c = getXML().getProperties().getProperty("config");
        if(null != c && c.startsWith("{")){
            Map m = StringUtils.convert2MapJSONObject(c);
            if(null != m){
                return m.get(p);
            }
        }
        return null;
    }
    class ZKClient extends MyZKClient{


        public ZKClient(String serverstring) throws Exception{
            super(serverstring);
        }

        public ZKClient(String zkServers, int connectionTimeout)throws Exception {
            super(zkServers, connectionTimeout);
        }

        public ZKClient(String zkServers, int sessionTimeout, int connectionTimeout) throws Exception{
            super(zkServers, sessionTimeout, connectionTimeout);
        }

        public ZKClient(String zkServers, int sessionTimeout, int connectionTimeout, ZkSerializer zkSerializer)throws Exception {
            super(zkServers, sessionTimeout, connectionTimeout, zkSerializer);
        }

        public ZKClient(String zkServers, int sessionTimeout, int connectionTimeout, ZkSerializer zkSerializer, long operationRetryTimeout) throws Exception{
            super(zkServers, sessionTimeout, connectionTimeout, zkSerializer, operationRetryTimeout);
        }

        public ZKClient(IZkConnection connection)throws Exception {
            super(connection);
        }

        public ZKClient(IZkConnection connection, int connectionTimeout)throws Exception {
            super(connection, connectionTimeout);
        }

        public ZKClient(IZkConnection zkConnection, int connectionTimeout, ZkSerializer zkSerializer) throws Exception{
            super(zkConnection, connectionTimeout, zkSerializer);
        }

        public ZKClient(IZkConnection zkConnection, int connectionTimeout, ZkSerializer zkSerializer, long operationRetryTimeout) throws Exception{
            super(zkConnection, connectionTimeout, zkSerializer, operationRetryTimeout);
        }


        @Override
        public void doDataDeleted(String dataPath) {
            if (null != dataPath) {
                if(envHandles.size()>0){
                    doEvent("data",dataPath,null);
                }

                //delete published service
                String srvroot = getXML().getProperties().getProperty("SERVICES_INFO_ROOT");
                if(null == srvroot){
                    srvroot = (String)getConfig("SERVICES_INFO_ROOT");
                }
                if(dataPath.contains(".") && dataPath.startsWith(srvroot)) {
                    try {
                        String name = dataPath.substring(dataPath.lastIndexOf(".") + 1);
                        XMLParameter par = new XMLParameter();
                        par.addParameter("${event_op}", "delete");
                        par.addParameter("${name}", name);
                        doThing(par, getXML());
                    }catch (Exception e){
                        log.error("",e);
                    }
                }
            }
            log.info("happen delete event :"+dataPath);
        }

        @Override
        public void doDataChanged(String dataPath, Object data) {
            if (null != data) {
                String cmd = (String)data;
                Object obj = null;
                if (cmd.startsWith("{")) {
                    obj = StringUtils.convert2MapJSONObject(cmd);
                }else{
                    obj = cmd;
                }
                if(envHandles.size()>0){
                    doEvent("data",dataPath,obj);
                }
                try {
                    XMLParameter par = new XMLParameter();
                    par.addParameter("${event_path}", dataPath);
                    par.addParameter("${event_op}", "update");
                    par.addParameter("${input_data}", obj);
                    //log.error("publish do "+obj);
                    doThing(par, getXML());
                }catch (Exception e){
                    log.error("",e);
                }
                log.debug("happen data event :"+dataPath+" "+obj);

            }

        }

        @Override
        public void doAddPath(String s) {
            log.info("current zk listener path Childs:\n"+s);
            if(envHandles.size()>0){
                doEvent("path",s,null);
            }
            try {
                XMLParameter par = new XMLParameter();
                par.put("${event_op}", "addPathEvent");
                par.put("${path}", s);
                doThing(par, getXML());
            }catch (Exception e){
                log.error("",e);
            }
        }

        @Override
        public void doRemovePath(String s) {
            log.info("current zk listener path Childs:\n"+s);
            if(envHandles.size()>0){
                doEvent("path",s,null);
            }
            try{
                log.info(" zk delete path ["+s + "]");
                XMLParameter par = new XMLParameter();
                par.put("${event_op}", "removePathEvent");
                par.put("${path}", s);
                doThing(par, getXML());
            }catch (Exception e){
                log.error("",e);
            }
        }
        boolean existDataListener(String path){
            try {
                Map m = (Map) ClassUtils.getFieldValue(zkClient, "_dataListener", false);
                if(null != m){
                    Set s = (Set)m.get(path);
                    if(null != s){
                        if(s.size()>0){
                            return true;
                        }
                    }
                }
                return false;
            }catch (Exception e){
                return false;
            }
        }
        boolean existChildListener(String path){
            try {
                Map m = (Map)ClassUtils.getFieldValue(zkClient, "_childListener", false);
                if(null != m){
                    Set s = (Set)m.get(path);
                    if(null != s){
                        if(s.size()>0){
                            return true;
                        }
                    }
                }
                return false;
            }catch (Exception e){
                return false;
            }
        }
        @Override
        public void doSessionExpired() {
            if(null !=zkClient){
                try {
                    zkClient.clone();
                }catch (Exception e){}
            }
            doInitial();
        }

        @Override
        public String[] getInstanceInfo() {
            return register();
        }

        @Override
        public String getGroupPath() {
            String root = getXML().getProperties().getProperty("SERVERS_ROOT");
            if(null == root){
                root = (String)getConfig("SERVERS_ROOT");
            }
            return root;
        }

        public boolean isLeader(){
            List<String> ls = getAllInstances();
            if(null != ls){
                String ip="",pid="";
                long minstarttime=0;
                for(String s:ls){
                    Map m = StringUtils.convert2MapJSONObject(s);
                    String startTime= (String)m.get("timestamp");
                    if(StringUtils.isNotBlank(startTime)){
                        long l = Long.parseLong(startTime);
                        if(minstarttime==0 || minstarttime>l){
                            minstarttime=l;
                            if(StringUtils.isNotBlank(m.get("ip")) && StringUtils.isNotBlank(m.get("pid"))) {
                                ip = (String) m.get("ip");
                                pid = (String) m.get("pid");
                            }
                        }
                    }else{
                        return true;
                    }
                }
                String lip = NetUtils.getip();
                String lpid = JVMUtil.getPid();
                if(ip.equals(lip) && pid.equals(lpid)){
                    return true;
                }else{
                    return false;
                }
            }else {
                return true;
            }
        }

    }
}
