package com.octopus.tools.pubsub;

import com.alibaba.otter.canal.common.zookeeper.StringSerializer;
import com.octopus.tools.pubsub.ZkClientListen;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cls.ClassUtils;
import com.octopus.utils.net.NetUtils;
import com.octopus.utils.si.jvm.JVMUtil;
import com.octopus.utils.thread.ExecutorUtils;
import org.I0Itec.zkclient.*;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;
import org.I0Itec.zkclient.serialize.ZkSerializer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.DataInputBuffer;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;

import java.io.DataInput;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by admin on 2020/3/16.
 */
public abstract class MyZKClient extends ZkClient {
    static transient Log log = LogFactory.getLog(MyZKClient.class);
    String instancesPath=null;
    //add path watch path
    private List<String> watchPathList = new ArrayList();

    public MyZKClient(String serverstring) throws Exception{
        super(serverstring);
        init();
    }

    public MyZKClient(String zkServers, int connectionTimeout) throws Exception{
        super(zkServers, connectionTimeout);
        init();
    }

    public MyZKClient(String zkServers, int sessionTimeout, int connectionTimeout) throws Exception{
        super(zkServers, sessionTimeout, connectionTimeout);
        init();
    }

    public MyZKClient(String zkServers, int sessionTimeout, int connectionTimeout, ZkSerializer zkSerializer) throws Exception{
        super(zkServers, sessionTimeout, connectionTimeout, zkSerializer);
        init();
    }


    public MyZKClient(String zkServers, int sessionTimeout, int connectionTimeout, ZkSerializer zkSerializer, long operationRetryTimeout) throws Exception{
        super(zkServers, sessionTimeout, connectionTimeout, zkSerializer, operationRetryTimeout);
        init();
    }

    public MyZKClient(IZkConnection connection)throws Exception {
        super(connection);
        init();
    }

    public MyZKClient(IZkConnection connection, int connectionTimeout)throws Exception {
        super(connection, connectionTimeout);
        init();
    }

    public MyZKClient(IZkConnection zkConnection, int connectionTimeout, ZkSerializer zkSerializer) throws Exception{
        super(zkConnection, connectionTimeout, zkSerializer);
        init();
    }

    public MyZKClient(IZkConnection zkConnection, int connectionTimeout, ZkSerializer zkSerializer, long operationRetryTimeout) throws Exception{
        super(zkConnection, connectionTimeout, zkSerializer, operationRetryTimeout);
        init();
    }

    public void process(WatchedEvent event) {
        super.process(event);
    }

    void init()throws Exception{
        instancesPath = registerInstance();

        subscribeStateChanges(new IZkStateListener() {
            @Override
            public void handleStateChanged(Watcher.Event.KeeperState keeperState) throws Exception {
                if(keeperState.getIntValue()==0 && "Disconnected".equals(keeperState.name())){
                    //System.out.println("zkClient disconnectioned and wait 30 seconds will reconnect");
                    /*boolean b = waitUntilConnected();
                    if(!b) {
                        zkClient = null;
                        ExecutorUtils.work(new ZkClientListen.ReConn());
                        log.error("zkClient disconnectioned and wait 30 seconds will reconnect");
                    }*/
                }else if(keeperState.getIntValue()==-112){//session expire
                    log.error("zkClient session expired, reconnecting");
                    doSessionExpired();
                    log.error("reconnected successfully");

                }else if(keeperState.getIntValue()== 3) {//SyncConnected
                    log.error("zkClient SyncConnected,redo register instance");
                    instancesPath = registerInstance();
                }
                //System.out.println(keeperState.getIntValue());


            }

            @Override
            public void handleNewSession() throws Exception {
                //System.out.println("zk status new Session");
            }

            @Override
            public void handleSessionEstablishmentError(Throwable throwable) throws Exception {
                //System.out.println("handleSessionEstablishmentError");
            }
        });
    }

    public List<String> getAllInstances(){
        if(StringUtils.isNotBlank(instancesPath)){
            List<String> pl =  getChildren(instancesPath);
            if(null != pl){
                List<String> ret = new ArrayList<>();
                for(String p:pl){
                    ret.add((String)readData(instancesPath+"/"+p));
                }
                return ret;
            }
            return null;
        }else{
            return null;
        }
    }
    /**
     * monitor a path , any child path or data changed will occur do things
     * @param path
     * @throws Exception
     */
    public void monitorAllPathFrom(String path)throws Exception{
        createPersistentWatchPath(path,true);
        cycleAddPathWatch(path,true,false);
    }
    void cycleAddPathWatch(String path,boolean createDataWatch,boolean isDo){
        List<String> ls = getChildren(path);
        if(null != ls && ls.size()>0){
            for(String l:ls){
                String p = path+"/"+l;
                synchronized (watchPathList) {
                    if (!watchPathList.contains(p)) {
                        watchPathList.add(p);
                        subscribeChildChanges(p, new SynZkChildListener(true, createDataWatch));
                        if (isDo)
                            doAddPath(p);
                        if (createDataWatch) {
                            subscribeDataChanges(p, new SynZkDataListener());
                            if (isDo) {
                                Object o = readData(p);
                                if (null != o) {
                                    doDataChanged(p, o);
                                }
                            }
                        }
                    }
                }
                cycleAddPathWatch(p,createDataWatch,isDo);
            }
        }
    }

    /**
     * create a path and add path listener and data listener
     * @param path
     * @param createDataWatch
     * @throws Exception
     */
    public void createPersistentWatchPath(String path,boolean createDataWatch)throws Exception{
        try {
            createPersistent(path);
            if(!watchPathList.contains(path)) {
                watchPathList.add(path);
                subscribeChildChanges(path, new SynZkChildListener(true, createDataWatch));
                if (createDataWatch)
                    subscribeDataChanges(path, new SynZkDataListener());
            }
            //Thread.sleep(50);
        }catch (ZkNodeExistsException exception){
            if(!watchPathList.contains(path)) {
                subscribeChildChanges(path, new SynZkChildListener(true, createDataWatch));
                if (createDataWatch)
                    subscribeDataChanges(path, new SynZkDataListener());
            }
        } catch (Exception e){
            throw e;
        }
    }

    public void addDataWatch(String path){
        subscribeDataChanges(path,new SynZkDataListener());
    }
    public void addPathWatch(String path){
        subscribeChildChanges(path,new SynZkChildListener(true,false));
    }

    protected class SynZkChildListener implements IZkChildListener {
        boolean isPathWatch,isDataWatch;
        public SynZkChildListener(boolean createPathWatch,boolean createDataWatch){
            this.isPathWatch=createPathWatch;
            this.isDataWatch=createDataWatch;
        }
        @Override
        public void handleChildChange(String parentPath, List<String> curlist) throws Exception {
            //System.out.println("PathChange---------"+parentPath+" :"+ArrayUtils.toJoinString(curlist));
            //System.out.println(parentPath+":"+ ArrayUtils.toJoinString(curlist));
            if(null != curlist) {
                synchronized (watchPathList) {
                    for (String c : curlist) {
                        String p = parentPath + "/" + c;
                        if (!watchPathList.contains(p)) {
                            //add
                            watchPathList.add(p);
                            if (isPathWatch) {
                                subscribeChildChanges(p, new SynZkChildListener(isPathWatch, isDataWatch));
                            }
                            if (isDataWatch)
                                subscribeDataChanges(p, new SynZkDataListener());
                            doAddPath(p);
                            if(isPathWatch)
                                cycleAddPathWatch(p,isDataWatch,true);
                        }
                    }
                    for (int i = curlist.size() - 1; i >= 0; i--) {
                        String p = parentPath + "/" + curlist.get(i);
                        if (!watchPathList.contains(p)) {
                            //delete
                            watchPathList.remove(p);
                            unsubscribeChildChanges(p);
                            doRemovePath(p);

                        }
                    }
                }
            }else if(StringUtils.isNotBlank(parentPath)){
                for(int i=watchPathList.size()-1;i>=0;i--){
                    if(watchPathList.get(i).startsWith(parentPath)){
                        unsubscribeChildChanges(watchPathList.get(i));
                        watchPathList.remove(i);
                        doRemovePath(parentPath);
                    }
                }
            }

        }
    }
    protected class SynZkDataListener implements IZkDataListener {
        @Override
        public void handleDataChange(String s, Object o) throws Exception {
            //System.out.println("DataChange---------"+s);
            doDataChanged(s,o);
        }

        @Override
        public void handleDataDeleted(String s) throws Exception {
            //System.out.println("DataDeleted---------"+s);
            watchPathList.remove(s);
            unsubscribeDataChanges(s);
            doDataDeleted(s);
        }
    }
    public void unsubscribeChildChanges(String path) {
        try {
            Map var3 = (Map) ClassUtils.getFieldValue(this, "_childListener", false);
            synchronized (var3) {
                Set<IZkChildListener> listeners = (Set) var3.get(path);
                listeners.clear();

            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void unsubscribeDataChanges(String path) {
        try {
            ConcurrentHashMap var3 = (ConcurrentHashMap) ClassUtils.getFieldValue(this, "_dataListener", false);
            synchronized (var3) {
                Set<IZkDataListener> listeners = (Set) var3.get(path);
                listeners.clear();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public String registerInstance()throws Exception{
            String[] data = getInstanceInfo();
            String path = getGroupPath();
            if (null == path) path = "/ids";
            if(!exists(path)){
                createPersistent(path,true);
            }
            String srp = path + "/" + data[0];
            createEphemeral(srp, data[1]);
            monitorAllPathFrom(path);
            return path;
    }
    public abstract void doDataDeleted(String s);
    public abstract void doDataChanged(String s,Object o);
    public abstract void doAddPath(String s);
    public abstract void doRemovePath(String s);
    public abstract void doSessionExpired();
    public abstract String[] getInstanceInfo();
    public abstract String getGroupPath();

}
