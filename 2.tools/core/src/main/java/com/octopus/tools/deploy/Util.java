package com.octopus.tools.deploy;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.net.NetUtils;
import com.octopus.utils.net.ssh.SSHClient;

import java.util.Properties;

/**
 * User: Administrator
 * Date: 15-1-26
 * Time: 下午5:07
 */
public class Util {
    public static SSHClient getSSHClient(Properties p)throws Exception{
        String port = p.getProperty("SshPort");
        int rp = 22;
        if(StringUtils.isNotBlank(port)){
            rp = Integer.parseInt(port);
        }
        if(p.containsKey("^UserType") && StringUtils.isNotBlank(p.getProperty("^UserType"))){

            if(p.getProperty("^UserType").equals("U")){
                return NetUtils.getSSHClient(p.getProperty("FixedIp"), rp, p.getProperty("UserName"), p.getProperty("UserPwd"));
            }
            if(p.getProperty("^UserType").equals("S")){
                return NetUtils.getSSHClient(p.getProperty("FixedIp"), rp, p.getProperty("SuperName"), p.getProperty("SuperPwd"));
            }
        }
        if((Integer)p.get("PositionLevel")>10){
            return NetUtils.getSSHClient(p.getProperty("FixedIp"), rp, p.getProperty("UserName"), p.getProperty("UserPwd"));

        }else{
            return NetUtils.getSSHClient(p.getProperty("FixedIp"), rp, p.getProperty("SuperName"), p.getProperty("SuperPwd"));
        }
    }
}
