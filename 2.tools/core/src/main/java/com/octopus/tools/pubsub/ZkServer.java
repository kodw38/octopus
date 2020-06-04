package com.octopus.tools.pubsub;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkNoNodeException;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;
import org.I0Itec.zkclient.serialize.BytesPushThroughSerializer;
import org.apache.zookeeper.jmx.ManagedUtil;
import org.apache.zookeeper.server.ServerCnxnFactory;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;

import javax.management.JMException;
import java.io.File;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * User: wfgao_000
 * Date: 2016/9/27
 * Time: 21:20
 */
public class ZkServer extends XMLDoObject{
    private static final String USAGE ="Usage: ZooKeeperServerMain configfile | port datadir [ticktime] [maxcnxns]";
    ZkClient zkClient;
    AtomicBoolean isRunning = new AtomicBoolean(false);
    private ServerCnxnFactory cnxnFactory;
    public ZkServer(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        Map config = StringUtils.convert2MapJSONObject(xml.getProperties().getProperty("config"));
        ExecutorUtils.work(this,"start",new Class[]{Map.class},new Object[]{config});
        synchronized (isRunning) {
            if (!isRunning.get()) {
                isRunning.wait();
            }
        }
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(null !=input && input.containsKey("op")){
            if("start".equals(input.get("op"))){
                ExecutorUtils.work(this,"start",new Class[]{Map.class},new Object[]{config});
            }else if("send".equals(input.get("op"))){
                send(config,(String)input.get("path"),(String)input.get("data"));
            }
        }
        return null;
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

    public void start(Map<String,String> properties){
        try {
            System.out.println("start zookeeper server...");
            try {
                ManagedUtil.registerLog4jMBeans();
            } catch (JMException e) {
                log.warn("Unable to register log4j JMX control", e);
            }

            ServerConfig config = new ServerConfig();
            LinkedList li = new LinkedList<String>();
            li.add(properties.get("port"));
            li.add(properties.get("dataDir"));
            if(StringUtils.isNotBlank(properties.get("tickTime")))
                li.add(properties.get("tickTime"));
            if(StringUtils.isNotBlank(properties.get("maxClientCnxns")))
                li.add(properties.get("maxClientCnxns"));

            config.parse((String[])li.toArray(new String[0]));

            FileTxnSnapLog txnLog = null;
            try {
                ZooKeeperServer zkServer = new ZooKeeperServer();
                txnLog = new FileTxnSnapLog(new File(config.getDataLogDir()), new File(config.getDataDir()));
                zkServer.setTxnLogFactory(txnLog);
                zkServer.setTickTime(config.getTickTime());
                zkServer.setMinSessionTimeout(config.getMinSessionTimeout());
                zkServer.setMaxSessionTimeout(config.getMaxSessionTimeout());
                this.cnxnFactory = ServerCnxnFactory.createFactory();
                this.cnxnFactory.configure(config.getClientPortAddress(), config.getMaxClientCnxns());
                this.cnxnFactory.startup(zkServer);

                isRunning.set(true);
                synchronized (isRunning) {
                    isRunning.notify();
                }

                this.cnxnFactory.join();
                if(zkServer.isRunning()) {
                    zkServer.shutdown();
                }
            } catch (InterruptedException var8) {
                log.warn("Server interrupted", var8);
            } finally {
                if(txnLog != null) {
                    txnLog.close();
                }

            }
/*
            ZooKeeperServer zkServer = new ZooKeeperServer();

            FileTxnSnapLog txnLog = new FileTxnSnapLog(new File(config.dataLogDir), new File(config.dataDir));

            zkServer.setTxnLogFactory(txnLog);
            zkServer.setTickTime(config.tickTime);
            zkServer.setMinSessionTimeout(config.minSessionTimeout);
            zkServer.setMaxSessionTimeout(config.maxSessionTimeout);

            this.cnxnFactory = ServerCnxnFactory.createFactory();
            this.cnxnFactory.configure(config.getClientPortAddress(), config.getMaxClientCnxns());

            this.cnxnFactory.startup(zkServer);
            this.cnxnFactory.join();
            if (zkServer.isRunning()) {
                zkServer.shutdown();
            }
*/
        } catch (IllegalArgumentException e) {
            log.error("Invalid arguments, exiting abnormally", e);
            log.info(USAGE);
            System.err.println(USAGE);
            System.exit(2);
        }  catch (Exception e) {
            log.error("Unexpected exception, exiting abnormally", e);
            System.exit(1);
        }
        log.info("Exiting normally");
        //System.exit(0);

    }

    public void send(Map config,String path,String data){

        String host  =(String)config.get("host");
        String port  =(String)config.get("port");
        if(StringUtils.isNotBlank(host) && StringUtils.isNotBlank(port)) {
            try {
                //回写到zookeeper中
                if (null == zkClient) {
                    zkClient = new ZkClient(host+":"+port, 50000, 50000, new BytesPushThroughSerializer());
                    //执行订阅command节点数据变化和servers节点的列表变化
                /*zkClient.subscribeDataChanges(config.get("dataPath"), dataListener);
                zkClient.subscribeChildChanges(serversPath, childListener);*/
                }
                zkClient.writeData(path, data.getBytes());
            } catch (ZkNoNodeException e) {
                try {
                    zkClient.createPersistent(path, data.getBytes());
                } catch (ZkNodeExistsException xe) {
                    //节点已经存在异常，直接写入数据
                    zkClient.writeData(path, data.getBytes());
                }
            }
        }
    }

}
