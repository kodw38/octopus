package com.octopus.tools.deploy;

import com.octopus.tools.deploy.command.CopyCommand;
import com.octopus.tools.deploy.command.ExeRemoteCommand;
import com.octopus.tools.deploy.command.InitializeDBData;
import com.octopus.utils.alone.StringUtils;

import java.io.File;
import java.util.HashMap;

/**
 * User: Administrator
 * Date: 15-1-15
 * Time: 上午10:58
 */
public class CommandMgr {
    HashMap<String,ICommand> cmdMap = new HashMap();
    HashMap<String,String> ccMap = new HashMap();
    public  CommandMgr(String configExcel,String scriptPath){
        cmdMap.put("copy",new CopyCommand(configExcel));
        cmdMap.put("loadSqlFiles",new InitializeDBData());
        cmdMap.put("exeCmd",new ExeRemoteCommand());
        if(StringUtils.isNotBlank(scriptPath)){
            File f = new File(scriptPath);
            String[] ss = f.list();
            if(null != ss){
                for(String mi:ss)
                    ccMap.put(mi.substring(mi.lastIndexOf("/")+1,mi.lastIndexOf(".")),f.getPath().replaceAll("\\\\","/")+"/"+mi);
            }
        }
    }
    public ICommand getCommand(String commandName){
        return cmdMap.get(commandName);
    }

    public String getCommandContent(String comandName){
        return ccMap.get(comandName);
    }

}
