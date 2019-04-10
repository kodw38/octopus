package com.octopus.tools.synchro.canal;

import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-11-23
 * Time: 下午2:10
 */
public interface ITableSplit {
    public String split(String tables);
    public String split(String tables,Map map);
}
