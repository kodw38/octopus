package com.octopus.tools.dataserver;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by robai on 2017/11/6.
 */
public class HBaseServer extends XMLDoObject {
    List<String> excludeJars;
    public HBaseServer(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        if(null != xml) {
            String s = xml.getProperties().getProperty("excludeJars");
            if (StringUtils.isNotBlank(s)) {
                excludeJars = Arrays.asList(s.split(","));
            }
        }
    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return false;
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return null;
    }

    @Override
    public ResultCheck checkReturn(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return null;
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret, Exception e) throws Exception {
        return false;
    }

    public void doInitial(){
        try {
            //master
            /*System.setProperty("proc_master","");
            System.setProperty("hbase.root.logger","INFO,RFA");
            System.setProperty("org.apache.hadoop.hbase.shaded.io.netty.packagePrefix","org.apache.hadoop.hbase.shaded");
            System.setProperty("hbase.log.dir","c:/log/hbase/log");
            System.setProperty("hbase.log.file","hbase-tb-master-VM_105_128_centos.log");
            System.setProperty("hbase.home.dir","c:/Users/robai/Downloads/hbase-1.3.1");
            System.setProperty("hbase.id.str","tb");
            System.setProperty("hbase.security.logger","INFO,RFAS");*/
            //log.error("==============starting hbase=========");
            /*new Thread(new Runnable() {
                @Override
                public void run() {
                    org.apache.hadoop.hbase.master.HMaster.main(new String[]{"start"});
                }
            }).start();
*/
            /*String log=null;
            if(null!=getEnvData()) {
                log = (String) getEnvData().get("logDir");
                if (StringUtils.isNotBlank(log)) {
                    log += "/hbase.log";
                }
            }*/

            ExecutorUtils.work(new Runnable() {

                @Override
                public void run() {
                    try {
                        ExecutorUtils.runMain(null, null, null, "org.apache.hadoop.hbase.master.HMaster", "start", null, excludeJars, null);
                    }catch (Exception e){
                        log.error(e);
                    }
                }
            });


            //remote each host start regionServer
            //System.setProperty("proc_regionserver","");
            //log.error("==============started hbase=========");
            //System.setProperty("hbase.regionserver.port","16201");
            //System.setProperty("hbase.regionserver.info.port","16301");

            //org.apache.hadoop.hbase.regionserver.HRegionServer.main(new String[]{"start"});

            //remote each host start regionServer
            //org.apache.hadoop.hbase.backup.BackupDriver.main(new String[]{});
        }catch (Exception e){
            log.error("start hbase error",e);
        }

    }

    public static  void main(String[] args){
        try{
            HBaseServer b = new HBaseServer(null,null,null);
            b.doInitial();
            Thread.sleep(30000);
            //ExecutorUtils.exc("c:/log/hbase.bat");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
