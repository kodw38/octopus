package com.octopus.tools.deploy;

import java.util.Properties;

/**
 * User: Administrator
 * Date: 15-1-15
 * Time: 上午10:59
 */
public interface ICommand {

    public String exeCommand(CommandMgr commandMgr,Properties properties)throws Exception;
}
