package com.octopus.utils.ha;

import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLUtil;
import org.apache.zookeeper.*;

import java.io.IOException;
import java.net.InetAddress;

/**
 * User: wfgao_000
 * Date: 15-5-28
 * Time: 下午1:15
 */
public class ZookeeperHA implements Watcher {

    ZooKeeper zk=null;
    String root="/AI_APP_CONSOLE_HA";
    protected static Integer mutex;
    int sessionTimeout = 10000;

    protected ZookeeperHA(){

    }

    void doThing(XMLMakeup xml){
        //reg used app instance to zookeeper
        while(true){
            try{
                XMLMakeup[] mx = xml.getByTagProperty("ha","type","single");
                doSingle(mx);
                Thread.sleep(1000);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    void doSingle(XMLMakeup[] ss)throws Exception{
        boolean t=false;
        for(XMLMakeup s:ss){
            String userName = s.getProperties().getProperty("sshuser");
            String userPwd = s.getProperties().getProperty("sshpwd");
            String processname = s.getProperties().getProperty("processname");
            String ip = s.getProperties().getProperty("ip");
            boolean b = InstanceControl.checkInstance(ip,userName,userPwd,processname);
            if(b && t) {
                InstanceControl.stopInstance(ip,userName,userPwd,processname);
                System.out.println("stoped "+ip+" "+userName+" "+processname);
            }
            if(b && !t)t=b;
        }
        if(!t){
            for(XMLMakeup s:ss){
                String userName = s.getProperties().getProperty("sshuser");
                String userPwd = s.getProperties().getProperty("sshpwd");
                String processname = s.getProperties().getProperty("processname");
                String restartscript = s.getProperties().getProperty("restartscript");
                String ip = s.getProperties().getProperty("ip");
                boolean b= InstanceControl.startInstance(ip,userName,userPwd,processname,restartscript);
                if(b){
                    System.out.println("started "+ip+" "+userName+" "+processname);
                    break;
                }
            }
        }
    }

    void doLeader(XMLMakeup xml) throws InterruptedException {
        byte[] leader = null;
        try {
            leader = zk.getData(root + "_leader", true, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (leader != null) {
            synchronized (mutex){
                mutex.wait();
            }
            doLeader(xml);
        } else {
            String newLeader = null;
            String newAddress=null;
            try {

                byte[] localhost = InetAddress.getLocalHost().getAddress();
                newLeader = zk.create(root + "_leader", localhost,ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
                if(null != newLeader){
                    System.out.println("created "+root+ "_leader");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            if (newLeader != null && null !=newAddress) {
                doThing(xml);
            } else {
                synchronized (mutex){
                    mutex.wait();
                }
                doLeader(xml);
            }
        }
    }

    public void init(String cnfPath)throws Exception{
        System.out.println("load config:"+cnfPath);
        XMLMakeup xml = XMLUtil.getDataFromXml(cnfPath);
        XMLMakeup[] xs = xml.getChild("zookeeper");
        regZookeeper(xs[0]);
        doLeader(xml);
    }
    void regZookeeper(XMLMakeup x) throws IOException {
        String connectString = x.getProperties().getProperty("connectString");
        System.out.println("zookeeper connect is:"+connectString);
        zk = new ZooKeeper(connectString, sessionTimeout, this);
        System.out.println(zk.getState().isConnected());
        System.out.println(zk.getState().isAlive());
        if(zk.getState().isAlive()){
            System.out.println("zookeeper connected");
        }
        mutex = new Integer(-1);
    }

    public static void main(String[] args){
        try{
            if(null == args|| args.length==0) {
                System.out.println("please input config file path");
                System.exit(3);
            }
            ZookeeperHA zp = new ZookeeperHA();
            zp.init(args[0]);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    synchronized public void process(WatchedEvent event) {
        if (null !=event && (root + "_leader").equals(event.getPath()) && event.getType() == Event.EventType.NodeCreated) {

            /*if( != address){
                boolean b=false;
                String ip= new String((byte[])address);
                System.out.println("pre ip:"+ip);
                //success close pre instance
                XMLMakeup[] t = xml.getChild("hac");
                b = InstanceControl.stopInstance(ip,t[0].getProperties().getProperty("sshuser"),t[0].getProperties().getProperty("sshpwd"),t[0].getProperties().getProperty("processname"));
                if(b){
                    System.out.println("stoped "+ip+" "+t[0].getProperties().getProperty("sshuser")+" "+t[0].getProperties().getProperty("processname"));

                }

            }*/
            synchronized (mutex) {

                mutex.notify();
            }
        }

    }
}
