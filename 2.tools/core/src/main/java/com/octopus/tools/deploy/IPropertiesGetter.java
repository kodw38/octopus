package com.octopus.tools.deploy;

import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * User: Administrator
 * Date: 15-1-15
 * Time: 上午11:01
 */
public interface IPropertiesGetter {
    public Map<String, List<Properties>>  getCommandProperties(String configExcel);
}
