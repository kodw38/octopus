package com.octopus.tools.synchro.canal;

import com.alibaba.otter.canal.protocol.CanalEntry;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.List;

/**
 * User: wfgao_000
 * Date: 15-8-13
 * Time: 上午11:10
 */
public interface ICanalMessageHandler {
    public void handle(XMLParameter env,List<CanalEntry.Entry> entrys)throws Exception;
}
