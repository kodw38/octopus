package com.octopus.tools.pubsub;

import com.alibaba.otter.canal.common.zookeeper.StringSerializer;
import com.octopus.isp.bridge.impl.Bridge;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cls.ClassUtils;
import com.octopus.utils.net.NetUtils;
import com.octopus.utils.si.jvm.JVMUtil;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import com.octopus.utils.xml.auto.logic.XMLLogic;
import com.octopus.utils.xml.desc.Desc;
import org.I0Itec.zkclient.IZkConnection;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.serialize.ZkSerializer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.util.HSSFColor;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by admin on 2020/3/17.
 */
public class ZkClientObject extends XMLDoObject {
    private static transient Log log = LogFactory.getLog(ZkClientObject.class);
    ZKClient zkClient;

    List<String> srv_paths =null;//服务存放的地址路径，这个下面的服务都被该实例加载，监控这些目录，如果有新服务新增删除，就及时变更。，要是发布服务地址，或子路径

    String srv_root_path= null;//发布的服务存放的地址路径

    String srv_status_path= null;//存放每个服务实例启动时加载的服务信息

    String servers_path = null;//服务实例信息路径

    Map<String,List<Map>> srvIdRelIns = new ConcurrentHashMap<String, List<Map>>();//缓存服务和实例关系信息，key为服务名称，Map为服务instance信息

    Map<String,Map> instanceMap = new ConcurrentHashMap();//缓存服务实例的信息，key为实例名称，value为实例信息

    Map<String,Map> serviceStatusMap = new ConcurrentHashMap();//缓存服务信息，key为服务名称，value为服务信息

    ReentrantLock lock = new ReentrantLock();

    String thisInstanceId=null;

    static Map<String,List<InvokeTaskByObjName>> envHandles = new HashMap();
    static boolean isrun = false;

    public ZkClientObject(XMLMakeup xml, XMLObject parent, Object[] containers) throws Exception {
        super(xml, parent, containers);
        String c = getXML().getProperties().getProperty("config");
        if(null != c && c.startsWith("{")){
            Map m = StringUtils.convert2MapJSONObject(c);
            if(null != m){
                srv_paths = (List)getConfig("SERVICES_LOAD_PATHS");
                srv_root_path = (String)getConfig("SERVICES_INFO_ROOT");

                servers_path = (String) getConfig("SERVERS_ROOT");
                srv_status_path = (String) getConfig("SERVICE_STATUS_ROOT");
            }
        }
        log.info("connecting zk server......");
        String config = getXML().getProperties().getProperty("config");
        Map cof = StringUtils.convert2MapJSONObject(config);
        zkClient = new ZKClient((String) cof.get("hostPort"), (Integer)cof.get("sessionTimeout"), (Integer)cof.get("connectTimeout"), new StringSerializer());

        //当启动端口前，监听配置的目录
        addApplicationReadyAction(this,"startMonitor",null,null);
    }

    void startMonitor(){
        try {
            zkClient.waitUntilConnected();
            //注册实例信息到临时目录下,先上传实例信息，后面的服务需要关联实例信息
            registerInstance2Zk();

            //上传本地服务状态，到临时目录
            uploadServicesStatus();


            //监听发布的服务目录，服务状态目录，主机目录
            if (null != srv_paths) {
                for (String s : srv_paths)
                    zkClient.monitorAllPathFrom(s);
            }
            if(StringUtils.isNotBlank(srv_status_path))
                zkClient.monitorAllPathFrom(srv_status_path);
            if(StringUtils.isNotBlank(servers_path))
                zkClient.monitorAllPathFrom(servers_path);

            //加载其他实例的服务,和实例信息
            loadOtherInstancesServices();


            addTriggerWatch();


            if(!isrun){
            //防止zk的监听事件遗漏，每10分钟全量同步一次数据
                isrun=true;
                Timer t = new Timer(true);
                t.schedule(new TimerTask(){
                    @Override
                    public void run() {
                        //reload status services to cache
                        loadOtherInstancesServices();
                        //compare srv_paths
                        compareLoadServices();
                    }
                },600000,600000);
            }

        }catch (Exception e){
            log.error("start zk monitor",e);
        }
    }

    public void doInitial(){

    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;
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
            if (null != input && null != zkClient && null != input && null != op && op.startsWith("publishService")) {
                String srvName = (String)input.get("name");
                if(StringUtils.isNotBlank(srvName)){
                    Map desc = getDescStructure(srvName);
                    if(null != desc){
                        String pak = (String)desc.get("package");
                        zkClient.setPersistentData(srv_root_path+"/"+pak.replaceAll("\\.","/")+"/"+srvName,ObjectUtils.convertMap2String(desc));
                    }

                }

            } else if ("delete".equals(op)) {
                String p = (String) input.get("path");
                if (StringUtils.isNotBlank(p) && zkClient.exists(p)) {
                    zkClient.delete(p);
                }
                if(log.isDebugEnabled())
                    log.debug("delete "+p);
                return true;
            }else  if(null!= zkClient && "deleteChildren".equals(op)){
                String p = (String) input.get("path");
                List<String> ls = zkClient.getChildren(p);
                if(null != ls){
                    for(String s:ls){
                        zkClient.delete(p+"/"+s);
                    }
                }
                if(log.isDebugEnabled())
                    log.debug("deleteChildren "+p);
            }else if (null != zkClient && null != input && "addChildrenDataListener".equals(op)) {
                String p = (String) input.get("path");
                if (!zkClient.exists(p)) {
                    zkClient.createPersistent(p,true);
                }
                log.info("add all path and data monitor base on "+p);
                zkClient.monitorAllPathFrom(p);
                return true;

            } else if (null != zkClient && null != input && "addChildrenPathListener".equals(op)) {
                String p = (String) input.get("path");

                if (!zkClient.exists(p)) {
                    zkClient.createPersistent(p,true);
                }
                log.info("add all path and data monitor base on "+p);
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
                log.debug("setData "+p);
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
                log.debug("getChildren "+p);
                if (zkClient.exists(p)) {
                    return zkClient.getChildren(p);
                }
                return null;
            } else if (null != zkClient && null != input && "isExist".equals(op)) {
                try {
                    String p = (String) input.get("path");
                    if(log.isDebugEnabled())
                        log.debug("isExist "+p);
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
                if(log.isDebugEnabled())
                    log.debug("addPathListener "+p);
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
                if(log.isDebugEnabled())
                    log.debug("addPath "+p);
                return null;
            } else if (null != zkClient && null != input && "getChildrenData".equals(op)) {
                HashMap m = new HashMap();
                String p = (String) input.get("path");
                zkClient.getChildData(p,m);
                if(log.isDebugEnabled())
                    log.debug("getChildrenData "+p);
                return m;
            }else if(null != zkClient && null != input && "getServiceList".equals(op)){
                return getAllDescByPaths();
            } else if (null != env) {
                return null;//super.doSomeThing(xmlid, env, input, output, config);
            } else {
                return null;
            }
        }catch (Exception e){
            log.error("zk client op error",e);
        }
        return null;
    }

    @Override
    public ResultCheck checkReturn(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return new ResultCheck(true,ret);
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return true;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret, Exception e) throws Exception {
        return true;
    }

    //获取本实例可以加载的服务
    List<Map> getAllDescByPaths(){
        if(null !=srv_paths && srv_paths.size()>0) {
            List<Map> ret = new ArrayList();
            HashMap tem = new HashMap();
            for(String p:srv_paths) {
                HashMap m = new HashMap();
                zkClient.getAllChildrenData(p,m);
                if(null != m && m.size()>0){
                    tem.putAll(m);
                }
            }
            if(tem.size()>0){
                List li = new ArrayList();
                Iterator<String> its = tem.keySet().iterator();
                while(its.hasNext()){
                    String k = its.next();
                    String s = (String)tem.get(k);
                    Map desc = (Map)StringUtils.convert2MapJSONObject(s);
                    if(null != desc) {
                        desc.put("path","zk:"+k);
                        li.add(desc);
                    }
                }
                return li;
            }
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
            if(!dataPath.contains("/"+thisInstanceId+"/")) {
                log.info("happen delete event :" + dataPath);
                if (null != dataPath) {
                    if (envHandles.size() > 0) {
                        doEvent("data", dataPath, null);
                    }

                    //delete published service
                    try {
                        reactRemoveData(dataPath);
                    } catch (Exception e) {
                        log.error("", e);
                    }

                }
            }
        }

        @Override
        public void doDataChanged(String dataPath, Object data) {
            log.debug("happen data changed :" + dataPath );
            if(!dataPath.contains("/"+thisInstanceId+"/")) {
                if (null != data) {
                    String cmd = (String) data;
                    Object obj = null;
                    if (cmd.startsWith("{")) {
                        obj = StringUtils.convert2MapJSONObject(cmd);
                    } else {
                        obj = cmd;
                    }
                    if (envHandles.size() > 0) {
                        doEvent("data", dataPath, obj);
                    }
                    try {

                        reactUpdateData(dataPath, obj);

                    } catch (Exception e) {
                        log.error("", e);
                    }

                }
            }

        }

        @Override
        public void doAddPath(String s) {
            /*if (!s.contains("/" + thisInstanceId + "/")) {
                log.info("add path :\n" + s);
                if (envHandles.size() > 0) {
                    doEvent("path", s, null);
                }
                try {
                    //如果增加了服务实例，加载该服务实例的服务
                    reactAddPath(s);
                }catch(Exception e){
                    log.error("", e);
                }
            }*/
        }

        //当有路径被删除或掉线时
        @Override
        public void doRemovePath(String s) {
            /*if(!s.contains("/"+thisInstanceId+"/")) {
                log.info("remove path :\n" + s);
                if (envHandles.size() > 0) {
                    doEvent("path", s, null);
                }
                try {
                    //更新本地服务，包括本实例和其他实例的服务
                    reactRemovePath(s);
                } catch (Exception e) {
                    log.error("", e);
                }
            }*/
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

        //session超时失去连接后的操作
        @Override
        public void doSessionExpired() {
            if(null !=zkClient){
                try {
                    zkClient.clone();
                }catch (Exception e){}
            }
            startMonitor();
        }

        @Override
        public void doSyncConnected() {
            //startMonitor();
        }


        /**
         * get instances root path in Zookeeper
         *@Description
         *@auther Kod Wong
         *@Date 2020/5/25 14:47
         *@Param
         *@return
         *@Version 1.0
         */
        @Override
        public String getServersPath() {
            return servers_path;
        }

        /**
         * find the  earliest register zk instance info , if the instance's ip and pid match this instance . this instance is leader.
         * @Description
         *@auther Kod Wong
         *@Date 2020/5/25 14:48
         *@Param
         *@return
         *@Version 1.0
         */
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

    Bridge getRootBridge(){
        XMLObject rt = getRoot();
        Bridge root=null;
        if(null != rt && rt instanceof Bridge){
            root = (Bridge) rt;
        }
        if(null == root) {
            root = (Bridge) getObjectById("bridge");
        }
        return root;
    }

    //主机本实例信息到zk
    void registerInstance2Zk(){
        Bridge root = getRootBridge();
        if(null != root) {
            try {
                thisInstanceId = root.getInstanceId();
                Map properties = getEnvProperties();
                String ip = NetUtils.getip();
                String webport = (String) properties.get("webport");
                String wshost = (String) properties.get("ws_host");
                String ishttps = (String) properties.get("ishttps");
                String ssl_port = (String) properties.get("ssl_port");
                String httphost = "http://"+ip+":"+webport;
                String httpshost="";
                if(StringUtils.isTrue(ishttps)) {
                    httpshost ="https://"+ip+":"+ssl_port;
                }
                String twp = System.getProperty("tb-webport");
                if(StringUtils.isNotBlank(twp)){
                    webport =twp;
                }
                String twsp = System.getProperty("tb-wsaddress");
                if(StringUtils.isNotBlank(twsp)){
                    wshost =twsp;
                }
                String startTime = "";
                if(null !=root){
                    startTime = root.getStartTime();
                }
                String logpath=(String)properties.get("logDir");
                if(logpath.startsWith("./") || logpath.startsWith("../")){
                    File f = new File("");
                    logpath = f.getAbsolutePath()+"/"+logpath;
                    log.info("log path is "+logpath);
                }

                String txt = "{\"insId\":\"" + thisInstanceId
                        + "\",\"ip\":\"" + ip
                        + "\",\"INS_ID\":\"" + thisInstanceId
                        + "\",\"port\":\"" + webport
                        + "\",\"zk_servers_path\":\"" + servers_path
                        + "\",\"zk_services_status_path\":\"" + srv_status_path
                        + "\",\"zk_load_services_path\":\"" + ArrayUtils.toJoinString(srv_paths)
                        + "\",\"zk_services_store_path\":\"" + srv_root_path
                        + "\",\"ws_host\":\"" + wshost
                        + "\",\"http_host\":\"" + httphost
                        + "\",\"https_host\":\"" + httpshost
                        + "\",\"loginuser\":\"" + properties.get("logUserName")
                        + "\",\"loginpwd\":\"" + properties.get("logPassword")
                        + "\",\"jmxRemotePort\":\"" + System.getProperty("com.sun.management.jmxremote.port")
                        + "\",\"pid\":\"" + JVMUtil.getPid()
                        + "\",\"startTime\":\"" + startTime
                        + "\",\"status\":\"" + "running"
                        + "\",\"timestamp\":\"" + System.currentTimeMillis()
                        + "\",\"logPath\":\"" + logpath.replaceAll("\\\\","/") + "\"}";
                zkClient.setEphemeralData(servers_path+"/"+thisInstanceId,txt);
            }catch (Exception e){
                log.error("",e);
            }
        }

    }

    //上传所有加载的服务到zk
    void uploadServicesStatus(){
        try {
            Bridge root = getRootBridge();
            String insId = root.getInstanceId();
            String path = srv_status_path + "/" + insId;
            String s = getXML().getProperties().getProperty("config");
            Map m = StringUtils.convert2MapJSONObject(s);

            Map config = getEmptyParameter().getMapValueFromParameter(m, this);
            if (null != config) {
                Object list = (List) config.get("servicesList");
                if (null != list && list instanceof List) {
                    List<String> srvs = (List)list;
                    String pid = JVMUtil.getPid();
                    for(String name:srvs){
                        //check exist
                        XMLObject o = getObjectById(name);
                        if(null != o){
                            Map desc = getDescStructure(name);
                            String datetime = (null != root ?root.getDefaultFormatDate(new Date()):"");
                            String data= getStatusString(desc,o,insId,pid,datetime);
                            zkClient.setEphemeralData(path + "/" + name, data);
                        }
                    }
                }
            }
        }catch (Exception e){
            log.error("",e);
        }
    }

    String getStatusString(Map desc,XMLObject o,String insId,String pid,String datetime){
        boolean isobj = false;
        String redo = "";
        if (null != desc && null != desc.get("redo") && desc.get("redo") instanceof String) {
            redo = (String) desc.get("redo");
        }
        if (null != desc && null != desc.get("redo") && desc.get("redo") instanceof Map) {
            redo = ObjectUtils.convertMap2String((Map) desc.get("redo"));
            isobj = true;
        }
        String isalarm="";
        if(o instanceof XMLDoObject) {
            isalarm = String.valueOf(((XMLDoObject)o).isAlarm());
        }
        String p = (String)(desc !=null?desc.get("path"):"");
        if(StringUtils.isNotBlank(p))p = p.replaceAll("\\\\","/");
        String status="running";
        if(!o.isActive()){
            status="suspend";
        }
        String data= "{\"ip\":\"" + NetUtils.getip()
                + "\",\"package\":\""+(desc !=null?desc.get("package"):"")
                + "\",\"isable\":\""+String.valueOf(o.isActive())
                +"\",\"redo\":"+(isobj?redo:("\""+redo+ "\""))
                +",\"share\":\""+(desc !=null?desc.get("share"):"")
                +"\",\"author\":\""+(desc !=null?desc.get("author"):"")
                + "\",\"isalarm\":\""+isalarm
                + "\",\"path\":\""+p
                + "\",\"opType\":\""+(desc !=null?desc.get("opType"):"")
                + "\",\"date\":\""+(desc !=null?desc.get("date"):"")
                +"\",\"insId\":\"" + insId
                + "\" ,\"status\":\""+status+"\",\"pid\":" + pid
                +",\"loadDate\":\""+datetime+"\""
                +",\"name\":\""+desc.get("name")+"\""
                + "}";
        return data;
    }

    //加载其他服务实例 load others instance services
    void loadOtherInstancesServices(){
        lock.lock();
        List<String> ins = zkClient.getAllInstances();
        if(null != ins) {
            Map<String, List<Map>> temp = new ConcurrentHashMap<String, List<Map>>();
            for(String insbody:ins){
                loadServices(insbody,temp);
            }
            synchronized (srvIdRelIns) {
                srvIdRelIns.clear();
                srvIdRelIns.putAll(temp);//srvIdRelIns的指针不能换，因为已经被其他对象初始化引用了。
            }
        }
        lock.unlock();
    }

    //比较现有服务和zk中的服务差异
    void compareLoadServices(){
        List<Map> all = getAllDescByPaths();
        if(null != all){
            for(Map m:all){
                String n = (String)m.get("name");
                if(null != getObjectById(n)){
                    //目前只做，zk上有的，本地没有的，新增服务。zk上删除的，本地咱不处理
                    try {
                        createXMLObjectByDesc(Desc.removeNotServiceProperty(m), null, this, true, getSingleContainers());
                        log.info("receive zk service :" + n);
                    }catch (Exception e){
                        log.error("create service from zk error:",e);
                    }

                }
            }
        }

    }

    void loadServices(String insbody,Map<String, List<Map>> temp){
        lock.lock();
        Map m = new HashMap();
        Map ins = StringUtils.convert2MapJSONObject(insbody);
        String insId = (String)ins.get("insId");
        //cache instance info
        if(null != insId && null!=ins)
            instanceMap.put(insId,ins);
        if(StringUtils.isNotBlank(insId) && !insId.equalsIgnoreCase(thisInstanceId)) {
            String status = (String) ins.get("status");
            //判断实例是否激活
            if ("running".equalsIgnoreCase(status)) {
                zkClient.getAllChildrenData(srv_status_path + "/" + insId, m);
                if (m.size() > 0) {
                    Iterator<String> its = m.keySet().iterator();
                    while (its.hasNext()) {
                        String srvpath = its.next();
                        String srv = (String) m.get(srvpath);
                        String srvId = srvpath.substring(srvpath.lastIndexOf("/")+1);
                        Map s = StringUtils.convert2MapJSONObject(srv);
                        //cache service info
                        serviceStatusMap.put(srvId,s);
                        //判断服务是否激活
                        if (null != s && "running".equalsIgnoreCase((String) s.get("status"))) {
                            if (!temp.containsKey(srvId)) temp.put(srvId, new ArrayList());
                            temp.get(srvId).add(ins);
                        }

                    }
                    log.info("load instance "+insId+" services count "+m.size());
                }
            }
        }
        lock.unlock();
    }

    void reactRemoveData(String path){
        synchronized (srvIdRelIns) {
            //删除某个服务
            //排除本实例自身的事件响应

            if (!path.contains("/" + thisInstanceId + "/")) {
                if (path.startsWith(srv_status_path)) {
                    int p = path.lastIndexOf("/");
                    String srvName = path.substring(p + 1);
                    String insId = path.substring(path.substring(0, p).lastIndexOf("/") + 1, p);
                    List<Map> its = srvIdRelIns.get(srvName);

                    if (null != its) {
                            for (int i = its.size()-1;i>=0;i--) {
                                String insName = (String) its.get(i).get("insId");
                                if (insName.equalsIgnoreCase(insId)) {
                                    //删除一个服务 /xxxx../xx/instanceid/srvname
                                    reactRemoveCacheService(srvName, insId);
                                } /*else if (path.endsWith("/" + insName)) {
                            //删除一个实例
                            reactRemoveCacheIns(insName);
                        }*/
                            }

                    }
                }
                //删除实例
                if (path.startsWith(servers_path)) {
                    //删除一个实例
                    reactRemoveCacheIns(path.substring(path.lastIndexOf("/") + 1));
                }
            }
        }
    }

    void reactUpdateData(String path,Object data){
        synchronized (srvIdRelIns) {
            log.debug("update data:" + path);
            //更新服务实例信息
            if (path.startsWith(servers_path)) {
                if (data instanceof Map) {
                    System.out.println("update path:" + path);
                    if ("suspend".equalsIgnoreCase((String) ((Map) data).get("status")) || "stoped".equalsIgnoreCase((String) ((Map) data).get("status"))) {
                        //暂停服务实例
                        reactRemoveCacheIns(path.substring(path.lastIndexOf("/") + 1));
                    } else if ("running".equalsIgnoreCase((String) ((Map) data).get("status"))) {
                        //加载服务实例
                        loadServices(path.substring(path.lastIndexOf("/") + 1), srvIdRelIns);
                    }
                }
            }else if (path.startsWith(srv_status_path)) {
                //更新服务状态信息
                if (data instanceof Map) {
                    if ("suspend".equalsIgnoreCase((String) ((Map) data).get("status")) || "stoped".equalsIgnoreCase((String) ((Map) data).get("status"))) {
                        //暂停服务
                        int p = path.lastIndexOf("/");
                        String srvName = path.substring(p + 1);
                        String tem = path.substring(0, p);
                        String insId = tem.substring(tem.lastIndexOf("/") + 1);
                        reactRemoveCacheService(srvName, insId);
                    } else if ("running".equalsIgnoreCase((String) ((Map) data).get("status"))) {
                        //加载服务
                        String id = path.substring(path.lastIndexOf("/") + 1);
                        serviceStatusMap.put(id,(Map)data);

                        List<Map> li = srvIdRelIns.get(id);
                        if (null == li) {
                            srvIdRelIns.put(id, new ArrayList<Map>());
                            li = srvIdRelIns.get(id);
                        }
                        if (null != li) {
                            boolean is = false;
                            for (Map m : li) {
                                if (null != data && data instanceof Map && null != ((Map) data).get("insId") && ((Map) data).get("insId").equals(m.get("insId"))) {
                                    is = true;
                                    break;
                                }
                            }
                            if (!is)
                                li.add((Map) data);
                        }
                    }
                }
            }else{
                if(null !=srv_paths){
                    //check new service
                    for(String s:srv_paths){
                        if(path.startsWith(s)){
                            if(data instanceof Map){
                                //desc
                                try {
                                    createXMLObjectByDesc(Desc.removeNotServiceProperty((Map) data), null, this, true, getSingleContainers());
                                    log.info("receive zk service :"+s);
                                }catch (Exception e){
                                    log.error("add service form zk error:",e);
                                }
                            }
                        }
                    }
                }

            }
        }
    }

    void reactAddPath(String path){
        lock.lock();
        if(path.startsWith(servers_path)){
            //增加服务实例,服务也会一个一个的增加，所以事例就不处理了
            //loadServices(path.substring(path.lastIndexOf("/")+1),srvIdRelIns);
        }
        if(path.startsWith(srv_status_path)){
            //增加服务
            int p = path.lastIndexOf("/");
            String srvName = path.substring(p+1);
            String tem = path.substring(0,p);
            String insId = tem.substring(tem.lastIndexOf("/")+1);
            if(!exist(srvName,insId)){

                String ins = zkClient.readData(servers_path + "/" + insId);
                Map m = StringUtils.convert2MapJSONObject(ins);

                //cache service
                serviceStatusMap.put(srvName, new HashMap());

                if (!srvIdRelIns.containsKey(srvName)) srvIdRelIns.put(srvName, new ArrayList<Map>());
                srvIdRelIns.get(srvName).add(m);
                log.info("srvIdRelIns add:"+srvName+":"+m.get("insId"));

            }
        }
        lock.unlock();
    }

    void reactRemovePath(String path){
        lock.lock();
        if(path.startsWith(servers_path)){
            //删除实例,服务也会一个一个的增加，所以事例就不处理了
            //reactRemoveCacheIns(path.substring(path.lastIndexOf("/")+1));
        }
        if(path.startsWith(srv_status_path)){
            //删除服务
            int p = path.lastIndexOf("/");
            String srvName = path.substring(p+1);
            String tem = path.substring(0,p);
            String insId = tem.substring(tem.lastIndexOf("/")+1);
            reactRemoveCacheService(srvName,insId);
        }
        lock.unlock();
    }

    boolean exist(String srvName,String insId){
        List<Map> list = srvIdRelIns.get(srvName);
        if(null != list){
            for(Map m:list){
                if(insId.equalsIgnoreCase((String)m.get("insId"))){
                    return true;
                }
            }
        }
        return false;
    }
    void reactRemoveCacheService(String srvName,String insId){
        serviceStatusMap.remove(srvName);

        List<Map> ls = srvIdRelIns.get(srvName);
        if(null != ls) {
            for (int i = ls.size() - 1; i >= 0; i--) {
                if (insId.equalsIgnoreCase((String) ls.get(i).get("insId"))) {
                    log.info("srvIdRelIns remove :"+srvName+":"+insId);
                    ls.remove(i);
                }
            }
        }
    }
    void reactRemoveCacheIns(String insId){
        //remove cache instance
        instanceMap.remove(insId);
        //remove cache services of this insId避免重复处理
        /*Iterator<String> list = srvIdRelIns.keySet().iterator();
        while(list.hasNext()){
            String srvName = list.next();
            List<Map> ls = srvIdRelIns.get(srvName);
            for(int i = ls.size()-1;i>=0;i--){
                if(insId.equalsIgnoreCase((String)ls.get(i).get("insId"))){
                    ls.remove(i);
                    log.info("srvIdRelIns remove :"+srvName+":"+insId);
                }
            }
            //if no instance contains this srv
            if(ls.size()==0){
                serviceStatusMap.remove(srvName);
            }
        }*/
    }

    //本实例增加服务同步到zk,会得到XMLObject的createObjectByDesc事件
    void addThisServiceEvent(String xmlid,Object[] pars,Exception e,Object ret){
        if(null == e) {
            Map desc = (Map)pars[0];
            String insId = getRootBridge().getInstanceId();
            String p = srv_status_path + "/" + insId + "/" + desc.get("name");
            XMLObject o = getObjectById((String) desc.get("name"));
            Bridge root = getRootBridge();
            String pid = JVMUtil.getPid();
            String datetime = (null != root ? root.getDefaultFormatDate(new Date()) : "");
            zkClient.setEphemeralData(p, getStatusString(desc, o, insId, pid, datetime));
        }
    }

    //本实例删除服务同步到zk,会得到XMLObject的removeObjectByDesc事件
    void removeThisServiceEvent(String xmlid,Object[] pars,Exception e,Object ret){
        if(null == e) {
            String id = (String)pars[0];
            String insId = getRootBridge().getInstanceId();
            String p = srv_status_path + "/" + insId + "/" + id;
            zkClient.delete(p);
        }
    }

    //本实例修改服务同步到zk,会得到XMLObject的updateObjectByDesc事件
    void updateThisServiceEvent(String xmlid,Object[] pars,Exception e,Object ret){
        if(null == e) {
            try {
                Map desc = null;
                if (null != pars && null != pars[0] && pars[0] instanceof Map)
                    desc = (Map) pars[0];
                else
                    desc = getDescStructure(xmlid);
                String insId = getRootBridge().getInstanceId();
                String p = srv_status_path + "/" + insId + "/" + desc.get("name");
                XMLObject o = getObjectById((String) desc.get("name"));
                Bridge root = getRootBridge();
                String pid = JVMUtil.getPid();
                String datetime = (null != root ? root.getDefaultFormatDate(new Date()) : "");
                zkClient.writeData(p, getStatusString(desc, o, insId, pid, datetime));
            }catch (Exception ex){

            }
        }
    }



    //根据服务名称获取其他实例名称
    String[] getInsNameBySrvId(String srvName){
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


}
