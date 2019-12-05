package com.octopus.tools.synchro.canal.impl;

import com.alibaba.otter.canal.common.AbstractCanalLifeCycle;
import com.alibaba.otter.canal.parse.exception.CanalParseException;
import com.alibaba.otter.canal.parse.index.CanalLogPositionManager;
import com.alibaba.otter.canal.protocol.position.EntryPosition;
import com.alibaba.otter.canal.protocol.position.LogIdentity;
import com.alibaba.otter.canal.protocol.position.LogPosition;
import com.octopus.utils.alone.NumberUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.exception.Logger;
import com.octopus.utils.net.NetUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import io.netty.util.NetUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: wfgao_000
 * Date: 16-6-5
 * Time: 下午4:52
 */
public class LogPositionManager extends AbstractCanalLifeCycle implements CanalLogPositionManager {
    transient static Log log = LogFactory.getLog(LogPositionManager.class);
    XMLMakeup xml =null;
    XMLDoObject obj=null;
    ConcurrentHashMap mk = new ConcurrentHashMap();
    Timer timer = new Timer(true);
    public void setConfig(XMLMakeup xml){
        this.xml=xml;
        // intervalsecond="30" limitsize="1000"
        String inter = xml.getProperties().getProperty("intervalsecond");
        long i = 600;
        if(StringUtils.isNotBlank(inter))
            i = Long.parseLong(inter);
        timer.schedule(new UpdateTask(), i * 1000L, i * 1000L);
    }
    public void setXMLObj(XMLDoObject obj){
        this.obj = obj;
    }
    public LogPositionManager() {
    }

    public void start()
    {
        super.start();

    }

    public void stop()
    {
        super.stop();

    }

    @Override
    public LogPosition getLatestIndexBy(String destination) {

/*
        LogPosition position = new LogPosition();
        EntryPosition p = new EntryPosition();
        p.setTimestamp(1465099963000L);
        LogIdentity iden = new LogIdentity();
        position.setIdentity(iden);
        position.setPostion(p);
        return position;  //To change body of implemented methods use File | Settings | File Templates.
*/

        //LogIdentity[sourceAddress=/127.0.0.1:3306,slaveId=-1]
        //LogIdentity[sourceAddress=/127.0.0.1:3306,slaveId=-1]
        if(null != xml && null != obj){

            XMLMakeup x = xml.getChild("reader")[0];
            String action = x.getProperties().getProperty("action");
            if(StringUtils.isNotBlank(action)){
                XMLDoObject o = (XMLDoObject)obj.getObjectById(action);
                if(null !=o ){
                    try{
                        String in = x.getProperties().getProperty("input");
                        in = in.replace("${name}",(String)x.getParent().getParent().getProperties().get("name"));
                        Map input = StringUtils.convert2MapJSONObject(in);
                        XMLParameter p = new XMLParameter();
                        o.doCheckThing(x.getId(),p,input,null,null,x);
                        ResultCheck ret = (ResultCheck)p.getResult();
                        if(null != ret){
                            List<Map> ls = (List)ret.getRet();
                            if(null != ls && ls.size()>0){
                                log.info("dest:" + destination + "  " + input + " | " + ls.get(0));
                                LogPosition position = new LogPosition();
                                EntryPosition pos = new EntryPosition();
                                //pos.setTimestamp(1465099963000L);
                                pos.setJournalName((String)ls.get(0).get("JOURNAL_NAME"));
                                if(ls.get(0).get("POSITION_NUM") instanceof String) {
                                    pos.setPosition(Long.parseLong((String) ls.get(0).get("POSITION_NUM")));
                                }else if(ls.get(0).get("POSITION_NUM") instanceof Integer){
                                    pos.setPosition(Long.valueOf((Integer) ls.get(0).get("POSITION_NUM")));
                                }else if(ls.get(0).get("POSITION_NUM") instanceof Long){
                                    pos.setPosition((Long) ls.get(0).get("POSITION_NUM"));
                                }
                                LogIdentity iden = new LogIdentity();
                                if(ls.get(0).get("SLAVE_ID") instanceof String) {
                                    iden.setSlaveId(StringUtils.toLong((String) ls.get(0).get("SLAVE_ID")));
                                }else if(ls.get(0).get("SLAVE_ID") instanceof Integer){
                                    iden.setSlaveId(Long.valueOf((Integer)ls.get(0).get("SLAVE_ID")));
                                }else if(ls.get(0).get("SLAVE_ID") instanceof Long){
                                    iden.setSlaveId((Long)ls.get(0).get("SLAVE_ID"));
                                }
                                //iden.setSlaveId((long)(((String)ls.get(0).get("SLAVE_ID")).hashCode()));
                                Object pp = ls.get(0).get("HOST_PORT");
                                int port=0;
                                if(pp instanceof Integer){
                                    port = (Integer) pp;
                                }else if(pp instanceof String){
                                    port = Integer.parseInt((String)pp);
                                }
                                iden.setSourceAddress(InetSocketAddress.createUnresolved((String) ls.get(0).get("HOST_IP"), port));
                                position.setIdentity(iden);
                                position.setPostion(pos);
                                return position;  //To change body of implemented methods use File | Settings | File Templates.

                            }
                        }
                    }catch (Exception e){
                        Logger.error(this.getClass(),null,null,"not find object ["+action+"]",e);
                    }
                }else{
                    Logger.error(this.getClass(),null,null,"not find object ["+action+"]",null);
                }
            }
        }
        return null;
    }

    @Override
    public void persistLogPosition(String s, LogPosition logPosition) throws CanalParseException {
        //每10分钟放入数据库   LogPosition[identity=LogIdentity[sourceAddress=/127.0.0.1:3306,slaveId=-1],postion=EntryPosition[included=false,journalName=mysql-bin.000508,position=2747,timestamp=1465130452000]]
        HashMap map = new HashMap();
        map.put("SLAVE_NAME",s);
        map.put("SLAVE_ID", NetUtils.getip()+"|"+System.getProperty("instance.name")+"|"+logPosition.getIdentity().getSlaveId());
        map.put("JOURNAL_NAME",logPosition.getPostion().getJournalName());
        map.put("POSITION_NUM",logPosition.getPostion().getPosition());
        map.put("HOST_IP",logPosition.getIdentity().getSourceAddress().getAddress().toString());
        map.put("HOST_PORT",logPosition.getIdentity().getSourceAddress().getPort());
        map.put("DONE_DATE",new Date());
        mk.put(s,map);
    }
    class  UpdateTask extends TimerTask{

        @Override
        public void run() {
            if(mk.size()>0) {
                XMLMakeup x = xml.getChild("writer")[0];
                String action = x.getProperties().getProperty("action");
                if (StringUtils.isNotBlank(action)) {
                    XMLDoObject o = (XMLDoObject) obj.getObjectById(action);
                    if (null != o) {
                        try {
                            Map input = StringUtils.convert2MapJSONObject(x.getProperties().getProperty("input"));
                            XMLParameter p = new XMLParameter();
                            List data = new ArrayList();
                            data.addAll(mk.values());
                            input.put("datas", data);
                            o.doCheckThing(x.getId(), p, input, null, null, x);
                        } catch (Exception e) {
                            Logger.error(this.getClass(), null, null, "not find object [" + action + "]", e);
                        }
                    } else {
                        Logger.error(this.getClass(), null, null, "not find object [" + action + "]", null);
                    }
                }
            }
        }
    }
}
