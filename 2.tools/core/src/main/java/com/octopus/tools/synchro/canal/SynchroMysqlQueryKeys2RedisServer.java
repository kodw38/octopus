package com.octopus.tools.synchro.canal;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.Message;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-8-12
 * Time: 下午4:05
 */
public class SynchroMysqlQueryKeys2RedisServer extends XMLDoObject {
    transient static Log log = LogFactory.getLog(SynchroMysqlQueryKeys2RedisServer.class);
    ICanalMessageHandler handler;
    ITableSplit split;
    public SynchroMysqlQueryKeys2RedisServer(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    @Override
    public Object doSomeThing(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        CanalConnector connector=null;
        String ip=null,port=null,clientId=null,destination=null;
        try{
            if(null != input && input.containsKey("op")){
                String op = (String)input.get("op");
                if(op.equals("subscribe") && input.containsKey("tables") && StringUtils.isNotBlank(input.get("tables"))){
                    XMLMakeup m = getXML().getByTagProperty("property","name","canal.instance.filter.regex")[0];
                    String nts = m.getText()+","+ input.get("tables");
                    m.setText(nts);
                    connector.subscribe(nts);
                }

            }
            if(null == input){
                String zks = getXML().getFirstCurChildText("property","name","canal.zkServers");
                destination = getXML().getFirstCurChildText("property","name","canal.name");
                ip = getXML().getFirstCurChildText("property","name","canal.ip");
                port = getXML().getFirstCurChildText("property","name","canal.port");
                clientId = getXML().getFirstCurChildText("property","name","client.id");
                int cid=1001;
                if(StringUtils.isNotBlank(clientId)){
                    cid=Integer.parseInt(clientId);
                }
                //System.out.println(cid);
                if(null == zks || "".equals(zks.trim())){
                    connector = CanalConnectors.newSingleConnector(new InetSocketAddress(ip, Integer.parseInt(port)), destination, "", "",cid);
                }else{
                    connector = CanalConnectors.newClusterConnector(zks,destination,"","");
                }
                /*ClientIdentity clientIdentity = new ClientIdentity(destination, (short)Short.parseShort(clientId));
                if(connector instanceof SimpleCanalConnector){
                    Field f = ClassUtils.getField(connector,"clientIdentity",false);
                    f.setAccessible(true);
                    f.set(connector,clientIdentity);
                }*/
                connector.connect();

                String tables = getXML().getFirstCurChildText("property","name","canal.instance.filter.regex");
                if(null != split){
                    Map tabs = new HashMap();
                    tables= split.split(tables,tabs);
                    env.put("${canal_split_tables}",tabs);
                    System.out.println("syn monitor tables:\n"+tables);
                }
                connector.subscribe(tables);
                //connector.subscribe(".*\\\\..*");
                System.out.println("mysql receive "+tables);
                connector.rollback();
                int batchSize = 1000;
                while (true) {//emptyCount < totalEmtryCount
                    try{
                        Message message = connector.getWithoutAck(batchSize); // 获取指定数量的数据
                        long batchId = message.getId();
                        int size = message.getEntries().size();
                        if (batchId == -1 || size == 0) {
                            Thread.sleep(1000);
                        } else {
                            try{
                                if(null != handler){
                                    handler.handle(env,message.getEntries());
                                }
                                connector.ack(batchId); // 提交确认
                            }catch (Exception e){
                                log.error("handle data error ",e);
                                connector.rollback(batchId); // 处理失败, 回滚数据
                                Thread.sleep(60000);
                            }
                        }
                    }finally {


                    }
                }
            }
        }catch (Exception e){
           log.error(ip+":"+port+" "+destination,e);
        } finally {
            if(null != connector)connector.disconnect();
        }
        return null;
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;
    }

    @Override
    public ResultCheck checkReturn(String xmlid,XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return new ResultCheck(true,null);
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config,Object ret,Exception e) throws Exception {
        return false;
    }
}
