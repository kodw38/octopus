package com.octopus.utils.ha;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.net.NetUtils;
import com.octopus.utils.net.ssh.SSHClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * User: wfgao_000
 * Date: 15-5-28
 * Time: 下午1:24
 */
public class InstanceControl {
    static transient Log log = LogFactory.getLog(InstanceControl.class);

    public static boolean stopInstance(String ip,String user,String pwd,String processName)throws Exception{
        SSHClient client = NetUtils.getSSHClient(ip,22,user,pwd);
        StringBuffer ps = client.exec("ps -ef|grep "+processName+" | grep -v grep | awk '{print $2}'");
        if(StringUtils.isBlank(ps.toString())){
            return true;
        }else{
            StringBuffer tt = client.exec("top -b -n 5|grep "+ps.toString()+" | grep -v grep | awk '{print $9}'");
            String t = tt.toString();
            if(StringUtils.isNotBlank(t)){
                String[] ms = t.split("\n");
                boolean isZero=true;
                for(String m:ms){
                   if(Float.parseFloat(m.trim())>0){
                       isZero=false;
                       break;
                   }
                }
                if(isZero){
                    client.exec("kill -9 "+ps.toString()+" 2>&1 >/dev/null");
                    ps = client.exec("ps -ef|grep "+processName+" | grep -v grep | awk '{print $2}'");
                    if(StringUtils.isBlank(ps.toString())){
                        return true;
                    }
                }
            }
        }
        return false;
    }
    public static boolean startInstance(String ip,String user,String pwd,String processName,String startScript) throws Exception {
        if(stopInstance(ip,user,pwd,processName)){
            SSHClient client = NetUtils.getSSHClient(ip,22,user,pwd);
            client.shell(startScript);
            int n=10;
            while(n-->=0){
                Thread.sleep(1000);
                boolean b= checkInstance(ip,user,pwd,processName);
                if(b){
                    return true;
                }
            }
            stopInstance(ip,user,pwd,processName);
            return false;
        }
        return false;
    }
    public static boolean checkInstance(String ip,String user,String pwd,String processName) throws Exception {
        SSHClient client = NetUtils.getSSHClient(ip,22,user,pwd);
        StringBuffer ps = client.exec("ps -ef|grep "+processName+" | grep -v grep | awk '{print $2}'");
        log.info("check "+ip+" "+user+" "+processName+" "+ps);
        if(StringUtils.isNotBlank(ps.toString())){
            return true;
        }
        return false;
    }

}
