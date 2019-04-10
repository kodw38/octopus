package com.octopus.tools.deploy;

import com.octopus.tools.deploy.property.ExcelPropertiesGetter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * User: Administrator
 * Date: 15-1-15
 * Time: 上午10:58
 */
public class PropertiesMgr {
    Map<String,List<Properties>> commands = new HashMap();
    public PropertiesMgr(String configExcel) {
        ExcelPropertiesGetter getter = new ExcelPropertiesGetter();
        commands = getter.getCommandProperties(configExcel);
    }
    public PropertiesMgr(){

    }

    public Map<String, List<Properties>> getCommands() {
        return commands;
    }

    public List<Properties> getCommandPropertyList(String commandName){
        return commands.get(commandName);
    }
}
