package com.octopus.tools.deploy.command;

import com.octopus.tools.deploy.CommandMgr;
import com.octopus.tools.deploy.ICommand;
import com.octopus.tools.deploy.Util;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.net.ssh.SSHClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Properties;

/**
 * User: Administrator
 * Date: 15-1-26
 * Time: 下午4:39
 */
public class ExeRemoteCommand implements ICommand {
    static transient Log log = LogFactory.getLog(ExeRemoteCommand.class);
    @Override
    public String exeCommand(CommandMgr commandMgr,Properties properties) {
        String remoteCommand = null;
        if(properties.containsKey("RemoteCommand") && StringUtils.isNotBlank(properties.getProperty("RemoteCommand"))){
            remoteCommand=properties.getProperty("RemoteCommand");
        }else if(properties.containsKey("inputParameter") && StringUtils.isNotBlank(properties.getProperty("inputParameter"))){
            remoteCommand= properties.getProperty("inputParameter");
        }
        if(StringUtils.isNotBlank(remoteCommand)){
            SSHClient client=null;
            try{
                client = Util.getSSHClient(properties);
                log.error("execute command in "+properties.getProperty("FixedIp")+" ["+properties.getProperty("UserName")+"] "+":"+remoteCommand);
                StringBuilder ret = client.shell(remoteCommand);
                return ret.toString();
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                if(null != client)client.close();
            }
        }
        return null;
    }
}
