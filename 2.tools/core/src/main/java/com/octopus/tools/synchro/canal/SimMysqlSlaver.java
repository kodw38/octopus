package com.octopus.tools.synchro.canal;

import com.alibaba.otter.canal.instance.core.CanalInstance;
import com.alibaba.otter.canal.instance.core.CanalInstanceGenerator;
import com.alibaba.otter.canal.instance.manager.CanalInstanceWithManager;
import com.alibaba.otter.canal.instance.manager.model.Canal;
import com.alibaba.otter.canal.instance.manager.model.CanalParameter;
import com.alibaba.otter.canal.server.embeded.CanalServerWithEmbeded;
import com.alibaba.otter.canal.server.netty.CanalServerWithNetty;
import com.octopus.tools.synchro.canal.impl.MyCanalParameter;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.safety.RC2;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-8-12
 * Time: 下午5:01
 */
public class SimMysqlSlaver extends XMLDoObject{
    ITableSplit split;
    public SimMysqlSlaver(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    @Override
    public Object doSomeThing(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(null!= input && null != input.get("instances") && ((List)input.get("instances")).size()>0 && null != getXML().getChild("template")){
            XMLMakeup tem = getXML().getChild("template")[0];
            for(Map m:((List<Map>)input.get("instances"))){
                String insName = (String)m.get("insName");
                String insPort = (String)m.get("insPort");
                String slaveId = (String)m.get("slaveId");
                String address = (String)m.get("address");
                String dbUsername = (String)m.get("dbUsername");
                String dbPassword = (String)m.get("dbPassword");
                String tables = (String)m.get("tables");
                XMLMakeup x = tem.clone();
                x.getProperties().put("name",insName);
                x.getProperties().put("prot",insPort);
                List<XMLMakeup> ls = x.getChildren();
                for(XMLMakeup l:ls){
                    if("canal.instance.mysql.slaveId".equals(l.getProperties().get("name"))){
                        l.setText(slaveId);
                    }
                    if("canal.instance.master.address".equals(l.getProperties().get("name"))){
                        l.setText(address);
                    }
                    if("canal.instance.dbUsername".equals(l.getProperties().get("name"))){
                        l.setText(dbUsername);
                    }
                    if("canal.instance.dbPassword".equals(l.getProperties().get("name"))){
                        l.setText(dbPassword);
                    }
                    if("canal.instance.filter.regex".equals(l.getProperties().get("name"))){
                        l.setText(tables);
                    }
                }
                createOneInstance(x);
            }
        }
        XMLMakeup[] xs = getXML().getChild("instance");
        for(XMLMakeup x:xs){
            createOneInstance(x);
        }
        return null;
    }
    void createOneInstance(XMLMakeup x){
        CanalServerWithEmbeded server = new CanalServerWithEmbeded();
        server.setCanalInstanceGenerator(new InstanceGenerator(x));

        CanalServerWithNetty nettyServer = new CanalServerWithNetty(server);
        nettyServer.setPort(Integer.parseInt(x.getProperties().getProperty("prot")));
        nettyServer.start();
        server.start(x.getProperties().getProperty("name"));
    }
    class InstanceGenerator  implements CanalInstanceGenerator{
        String filter;
        XMLMakeup x;
        public InstanceGenerator(XMLMakeup x){
            this.filter=x.getFirstCurChildText("property", "name", "canal.instance.filter.regex");
            if(null !=split)
                this.filter=split.split(this.filter);
            System.out.println("slaver monitor table:\n"+this.filter);
            this.x=x;
        }

        @Override
        public CanalInstance generate(String s) {
            Canal canal = buildCanal(x);
            return new CanalInstanceWithManager(canal, filter);
        }
    }
    Canal buildCanal(XMLMakeup x){

            Canal canal = new Canal();
            canal.setId(Long.getLong(x.getId()));
            canal.setName(x.getProperties().getProperty("name"));
            canal.setDesc(x.getProperties().getProperty("desc"));

            MyCanalParameter parameter = new MyCanalParameter();
            parameter.setZkClusters(Arrays.asList(x.getFirstCurChildText("property","name","canal.zkServers")));

            if(x.getFirstCurChildText("property","name","canal.MetaMode").equals("MEMORY"))
                parameter.setMetaMode(CanalParameter.MetaMode.MEMORY);

            if(x.getFirstCurChildText("property","name","canal.HaMode").equals("HEARTBEAT"))
                parameter.setHaMode(CanalParameter.HAMode.HEARTBEAT);

            if(x.getFirstCurChildText("property","name","canal.IndexMode").equals("MEMORY"))
                parameter.setIndexMode(CanalParameter.IndexMode.MEMORY);

            if(x.getFirstCurChildText("property","name","canal.StorageMode").equals("MEMORY"))
                parameter.setStorageMode(CanalParameter.StorageMode.MEMORY);

            parameter.setMemoryStorageBufferSize(Integer.parseInt(x.getFirstCurChildText("property","name","canal.MemoryStorageBufferSize")));

            if(x.getFirstCurChildText("property","name","canal.SourcingType").equals("MYSQL"))
                parameter.setSourcingType(CanalParameter.SourcingType.MYSQL);
            if(x.getFirstCurChildText("property","name","canal.SourcingType").equals("ORACLE"))
                parameter.setSourcingType(CanalParameter.SourcingType.ORACLE);

            String[] ipport = x.getFirstCurChildText("property","name","canal.instance.master.address").split(":");
            parameter.setDbAddresses(Arrays.asList(new InetSocketAddress(ipport[0], Integer.parseInt(ipport[1])),
                    new InetSocketAddress(ipport[0], Integer.parseInt(ipport[1]))));
            parameter.setDbUsername(x.getFirstCurChildText("property","name","canal.instance.dbUsername"));
            String passwd = x.getFirstCurChildText("property", "name", "canal.instance.dbPassword");
        if(StringUtils.isNotBlank(passwd)){
            if(passwd.startsWith("{RC2}")){
                RC2 rc = new RC2();
                try {
                    passwd = rc.decrypt(passwd.substring(5));
                }catch (Exception e){}
            }
        }
            parameter.setDbPassword(passwd);

            String journalName=x.getFirstCurChildText("property","name","canal.instance.master.journal.name");
            String position=x.getFirstCurChildText("property","name","canal.instance.master.position");
            String timestamp=x.getFirstCurChildText("property","name","canal.instance.master.timestamp");
            XMLMakeup[] logpositionimpl=x.getByTagProperty("property","name","canal.logpositionmanager");
            if(null != logpositionimpl && logpositionimpl.length>0){
                parameter.put("logpositionmanager",logpositionimpl);
                parameter.put("SlaverObj",this);
            }

/*
            parameter.setPositions(Arrays.asList("{\"journalName\":\"mysql-bin.000001\",\"position\":6163L,\"timestamp\":1322803601000L}",
                "{\"journalName\":\"mysql-bin.000001\",\"position\":6163L,\"timestamp\":1322803601000L}"));
*/
        if(StringUtils.isNotBlank(timestamp)){
            parameter.setPositions(Arrays.asList("{\"timestamp\":"+timestamp+"L}"));
        }else if(StringUtils.isNotBlank(journalName))
            parameter.setPositions(Arrays.asList("{\"journalName\":\""+journalName+"\",\"position\":"+position+"L}"));

        parameter.setSlaveId(Long.parseLong(x.getFirstCurChildText("property","name","canal.instance.mysql.slaveId")));
            parameter.setLocalBinlogDirectory((String)x.getFirstCurChildText("property","name","canal.instance.localbinlogdirectory"));

            parameter.setDefaultConnectionTimeoutInSeconds(30);
            parameter.setConnectionCharset(x.getFirstCurChildText("property", "name", "canal.instance.connectionCharset"));
            parameter.setConnectionCharsetNumber((byte) 33);
            parameter.setReceiveBufferSize(8 * 1024);
            parameter.setSendBufferSize(8 * 1024);

            parameter.setDetectingEnable(false);
            parameter.setDetectingIntervalInSeconds(10);
            parameter.setDetectingRetryTimes(3);
            parameter.setDetectingSQL("select 1");

            canal.setCanalParameter(parameter);
            return canal;

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
