package com.octopus.tools.pubsub;

import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cls.ClassUtils;
import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.thread.ds.InvokeTask;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.XMLParameter;
import com.octopus.utils.xml.auto.logic.XMLLogic;
import org.I0Itec.zkclient.IZkChildListener;
import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.IZkStateListener;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.serialize.BytesPushThroughSerializer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.zookeeper.Op;
import org.apache.zookeeper.Watcher;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * User: wfgao_000
 * Date: 2016/9/28
 * Time: 16:51
 */
public class ZkClientListen extends XMLLogic {
    static Log log = LogFactory.getLog(ZkClientListen.class);
    public static String[] EVENT_NAMES=new String[]{"path","data"};
    ZkClient zkClient;
    List<String> currentList = new ArrayList();
    static Map<String,List<InvokeTaskByObjName>> envHandles = new HashMap();
    public ZkClientListen(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    void appendChildEvent(String type,String path){
        if(!zkClient.exists(path)){
            zkClient.createPersistent(path);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if("path".equals(type) && !existChildListener(path)) {
            addPathListener(path);
            log.info("add path listener " + path);
        }
        if("data".equals(type) && !existDataListener(path)) {
            zkClient.subscribeDataChanges(path,new DataChangeListener());
            log.info("add data change listener " + path);
        }


        List<String> cls = zkClient.getChildren(path);
        if(null != cls && cls.size()>0){
            for(String c:cls){
                appendChildEvent(type,path+"/"+c);
            }
        }
    }
    boolean existDataListener(String path){
        try {
            Map m = (Map)ClassUtils.getFieldValue(zkClient, "_dataListener", false);
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
                        appendChildEvent(type,path);
                        temp.add(k);
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
                            appendChildEvent(s,path);
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
    class DataChangeListener implements IZkDataListener{
        public void handleDataDeleted(String dataPath) throws Exception {
            if (null != dataPath) {
                if(envHandles.size()>0){
                    doEvent("data",dataPath,null);
                }
                String name = dataPath.substring(dataPath.lastIndexOf(".") + 1);
                XMLParameter par = new XMLParameter();
                par.addParameter("${event_op}", "delete");
                par.addParameter("${name}", name);
                doThing(par, getXML());
            }
            log.info("happen delete event :"+dataPath);
        }

        public void handleDataChange(String dataPath, Object data) throws Exception {
            if (null != data) {
                String cmd = new String((byte[]) data);
                Object obj = null;
                if (cmd.startsWith("{")) {
                    obj = StringUtils.convert2MapJSONObject(cmd);
                }else{
                    obj = cmd;
                }
                if(envHandles.size()>0){
                    doEvent("data",dataPath,obj);
                }
                XMLParameter par = new XMLParameter();
                par.addParameter("${event_path}",dataPath);
                par.addParameter("${event_op}", "update");
                par.addParameter("${input_data}", obj);
                //log.error("publish do "+obj);
                doThing(par, getXML());

                log.debug("happen data event :"+dataPath+" "+obj);

            }
        }
    }

    private void addChildrenDataListener(String dataPath){
        if(StringUtils.isNotBlank(dataPath)) {
            //add exist path event
            List<String> hasExistList = zkClient.getChildren(dataPath);
            if (null != hasExistList) {
                for (String s : hasExistList) {
                    zkClient.subscribeDataChanges(dataPath + "/" + s, new DataChangeListener());
                    log.info("append zk data listener:"+dataPath + "/" + s);
                }
            }
        }
    }

    private void addPathListener(String dataPath){
        log.info("append zk path listener:"+dataPath );
        //create delete node happened, add , reduce event
        zkClient.subscribeChildChanges(dataPath, new IZkChildListener() {
            //用于监听zookeeper中servers节点的子节点列表变化
            public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
                //更新服务器列表
                log.info("current zk listener path Childs:\n"+currentChilds);
                if(envHandles.size()>0){
                    doEvent("path",parentPath,currentChilds);
                }
                if(null != currentChilds){
                    synchronized (currentList) {
                        for (String c : currentChilds) {
                            if (!currentList.contains(c)) {
                                //add
                                log.info(" zk add path [" + parentPath + "/" + c + "]");
                                currentList.add(c);
                                XMLParameter par = new XMLParameter();
                                par.put("${event_op}", "addPathEvent");
                                par.put("${path}", parentPath + "/" + c);
                                doThing(par, getXML());
                            }
                        }
                    }
                    synchronized (currentList) {
                        for (int i = currentList.size() - 1; i >= 0; i--) {
                            if (!currentChilds.contains(currentList.get(i))) {
                                //delete
                                log.info(" zk delete path ["+parentPath+"/" + currentList.get(i) + "]");
                                XMLParameter par = new XMLParameter();
                                par.put("${event_op}", "removePathEvent");
                                par.put("${path}", parentPath + "/" + currentList.get(i));
                                doThing(par, getXML());
                                currentList.remove(i);
                            }
                        }
                    }

                }

            }
        });
    }
    class PathListener implements IZkChildListener{
        @Override
        public void handleChildChange(String parentPath, List<String> currentChilds) throws Exception {
            //更新服务器列表
            if(envHandles.size()>0){
                doEvent("path",parentPath,currentChilds);
            }
            for(String s:currentChilds) {
                String pa = parentPath+"/"+s;
                addChildrenPathListener(pa);
                log.info("event append zk path listener:" + pa);
                zkClient.subscribeDataChanges(pa, new DataChangeListener());
                log.info("event append zk data listener:"+pa);
            }
        }
    }
    private void addChildrenPathListener(String dataPath){
        log.info("append zk path children listener:"+dataPath );
        //create delete node happened, add , reduce event
        zkClient.subscribeChildChanges(dataPath, new PathListener());
    }

    void addStatusListener(){
        zkClient.subscribeStateChanges(new IZkStateListener() {
            @Override
            public void handleStateChanged(Watcher.Event.KeeperState keeperState) throws Exception {
                log.warn("zk status changed"+keeperState.getIntValue()+"|"+keeperState.name());
                if(keeperState.getIntValue()==0 && "Disconnected".equals(keeperState.name())){
                    boolean b = zkClient.waitUntilConnected(5000, TimeUnit.MILLISECONDS);
                    if(!b) {
                        zkClient = null;
                        ExecutorUtils.work(new ReConn());
                        log.error("zkClient disconnectioned and wait 30 seconds will reconnect");
                    }
                }
                if(keeperState.getIntValue()==-112){//session expire
                    log.error("zkClient session expired");
                }
            }

            @Override
            public void handleNewSession() throws Exception {
                System.out.println("zk status new Session");
            }

            @Override
            public void handleSessionEstablishmentError(Throwable throwable) throws Exception {
                 log.error("handleSessionEstablishmentError",throwable);
            }
        });
    }
    private void connzk()throws Exception{
        log.info("connecting zk server......");
        String config = getXML().getProperties().getProperty("config");
        Map cof = StringUtils.convert2MapJSONObject(config);
        try {
            zkClient = new ZkClient((String) cof.get("hostPort"), (Integer)cof.get("sessionTimeout"), (Integer)cof.get("connectTimeout"), new BytesPushThroughSerializer());
            //zkClient = new ZkClient((String) cof.get("hostPort"), 30000, 30000, new BytesPushThroughSerializer());
            addStatusListener();
        }catch (Exception e){
            log.error(cof.get("hostPort"),e);
            if(null != zkClient) {
                zkClient.close();
            }
            throw e;
        }
    }

    public void doInitial(){
        try {
            connzk();
            //执行订阅command节点数据变化和servers节点的列表变化

            addSystemFinishedEvent(new Runnable() {
                @Override
                public void run() {
                    addTriggerWatch();
                }
            });


        }catch (Exception e){
            log.error("zookeeper connect service error , will try again",e);
            ExecutorUtils.work(new ReConn());
        }
    }
    int getServerCount(String ip){
        int rt = 0;
        if(null != zkClient) {
            List<String> ls = zkClient.getChildren("/SERVERS");
            if (null != ls) {
                for (String s : ls) {
                    if (StringUtils.isNotBlank(s)) {
                        byte[] b = zkClient.readData("/SERVERS/"+s);
                        if(null != b) {
                            String c = new String(b);
                            if (StringUtils.isNotBlank(c)) {
                                Map m = StringUtils.convert2MapJSONObject(c);
                                if (null != m && null != m.get("ip") && m.get("ip").equals(ip)) {
                                    rt++;
                                }
                            }
                        }
                    }
                }
            }
        }
        return rt;
    }
    class ReConn implements Runnable{

        @Override
        public void run() {
            while(null == zkClient ) {
                try {
                    connzk();
                    notifyObjectByName("StatHandler",true, "init", null);
                    notifyObjectByName("LoadDefineActions",true,"reConZkInit",null);
                    notifyObjectByName("system",true,"init",null);
                } catch (Exception e) {

                }finally {
                    try {
                        Thread.sleep(30000);
                    }catch (Exception e){}
                }
            }
            log.info("runing zk reconn ...");
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
    void createPath(String p){
        createParent(p);
        if(!zkClient.exists(p)){
            zkClient.createPersistent(p);
        }
    }
    void createParentAddListener(String p){
        try {
            int n = p.indexOf("/", 1);
            while (n > 0) {
                String t = p.substring(0, n);
                if (!zkClient.exists(t)) {
                    zkClient.createPersistent(t);
                    addChildrenPathListener(t);
                    log.info("create path listener "+t);
                }
                n = p.indexOf("/", n + 1);
                try {
                    Thread.sleep(500);
                }catch (Exception e){}
            }
        }catch (RuntimeException e){
            log.error("zk create path error ["+p+"]",e);
            throw e;
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
                    zkClient.createPersistent(p);
                    addChildrenPathListener(p);
                    zkClient.subscribeDataChanges(p, new DataChangeListener());
                    Thread.sleep(500);
                }
                long l = System.currentTimeMillis();
                while (!zkClient.exists(p)) {
                    Thread.sleep(100);
                    if (System.currentTimeMillis() - l > 500) {
                        break;
                    }
                }
                if (zkClient.exists(p)) {
                    Boolean b = (Boolean) ClassUtils.invokeMethod(zkClient, "hasListeners", new Class[]{String.class}, new Object[]{p});
                    if (!b) {
                        zkClient.subscribeDataChanges(p, new DataChangeListener());
                        Thread.sleep(500);
                    }
                    if (((String) input.get("op")).contains("Delete")) {
                        zkClient.delete(p);
                    } else {
                        if(log.isInfoEnabled()) {
                            log.info("wirtedata " + ((String) input.get("data")));
                        }
                        zkClient.writeData(p, ((String) input.get("data")).getBytes(), -1);
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
                    createPath(p);
                }
                addChildrenDataListener(p);
                return true;

            } else if (null != zkClient && null != input && "addChildrenPathListener".equals(op)) {
                String p = (String) input.get("path");

                if (!zkClient.exists(p)) {
                    createPath(p);
                }
                addChildrenPathListener(p);
                return true;
            } else if (null != zkClient && null != input && "addPathDataListener".equals(op)) {
                String p = (String) input.get("path");

                if (!zkClient.exists(p)) {
                    createParent(p);
                    zkClient.createPersistent(p);

                }
                long l = System.currentTimeMillis();
                while (!zkClient.exists(p)) {
                    Thread.sleep(10);
                    if (System.currentTimeMillis() - l > 200) {
                        break;
                    }
                }
                zkClient.subscribeDataChanges(p, new DataChangeListener());
                log.info("append zk data listener " + p);
                return true;
            } else if (null != zkClient && null != input && "setData".equals(op)) {
                String p = (String) input.get("path");
                String data = ObjectUtils.toString(input.get("data"));
                createParentAddListener(p);
                if (!zkClient.exists(p)) {
                    zkClient.createPersistent(p);
                }
                zkClient.writeData(p, data.getBytes(), -1);
                return true;
            } else if (null != zkClient && null != input && "onlySetData".equals(op)) {
                String p = (String) input.get("path");
                String data = (String) input.get("data");
                createParent(p);
                if (!zkClient.exists(p)) {
                    zkClient.createPersistent(p);
                }
                zkClient.writeData(p, data.getBytes(), -1);
                log.debug("onlySetData:" + p + "\n" + data);
                return true;
            }else if (null != zkClient && null != input && "onlyWriteData".equals(op)) {
                String p = (String) input.get("path");
                String data = (String) input.get("data");
                if(zkClient.exists(p)){
                    zkClient.writeData(p, data.getBytes(), -1);
                    log.debug("onlyWriteData:" + p + "\n" + data);
                    return true;
                }
            } else if (null != zkClient && null != input && "onlySetTempData".equals(op)) {
                String p = (String) input.get("path");
                String data = (String) input.get("data");
                createParent(p);
                if (!zkClient.exists(p)) {
                    zkClient.createEphemeral(p);
                }
                zkClient.writeData(p, data.getBytes(), -1);
                log.debug("onlySetTempData:" + p + "\n" + data);
                return true;
            } else if (null != zkClient && null != input && "getData".equals(op)) {
                String p = (String) input.get("path");
                if (zkClient.exists(p)) {
                    byte[] b = zkClient.readData(p);
                    if (null != b) {
                        return new String(b);
                    }
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
                addPathListener(p);
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
                                byte[] s = zkClient.readData(p + "/" + c);
                                if (null != s) {
                                    m.put(c, new String(s));
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
}