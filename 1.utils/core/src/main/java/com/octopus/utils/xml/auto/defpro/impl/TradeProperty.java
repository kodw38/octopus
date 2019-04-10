package com.octopus.utils.xml.auto.defpro.impl;

import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.transaction.TransactionConsole;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import com.octopus.utils.xml.auto.defpro.IObjectInvokeProperty;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import redis.clients.jedis.JedisPool;

import java.util.List;
import java.util.Map;

/**
 * trad='true'
 * User: wfgao_000
 * Date: 15-7-21
 * Time: 下午3:45
 */
public class TradeProperty implements IObjectInvokeProperty {
    transient static Log log = LogFactory.getLog(TradeProperty.class);
    TransactionConsole tradeConsole =null;
    JedisPool jedisPool=null;

    public TradeProperty(XMLMakeup xml){
        String instanceId=(String)xml.getProperties().getProperty("instanceid");
        String activeXAddr=(String)xml.getProperties().getProperty("activexaddr");
        String redishost  = (String)xml.getProperties().getProperty("redishost");
        String redisport  = (String)xml.getProperties().getProperty("redisport");
        String sdb  = (String)xml.getProperties().getProperty("redisdb");
        /*try{
            if(StringUtils.isNotBlank(redisport) && StringUtils.isNotBlank(redishost)) {
                jedisPool = new JedisPool(redishost, Integer.parseInt(redisport));
                int db = Integer.parseInt(sdb);
                tradeConsole = new TransactionConsole(instanceId);
            }
        }catch (Exception e){
            e.printStackTrace();
        }*/
    }
    @Override
    public Object exeProperty(Map proValue, XMLDoObject obj, XMLMakeup xml, XMLParameter parameter, Map input, Map output, Map config) {

        if(null == parameter) parameter= new XMLParameter();
        try{
        String tradeid = (String)parameter.getParameter("translactionid");
        if(StringUtils.isBlank(tradeid)){
            tradeid = tradeConsole.newTranslactionId();
            parameter.addParameter("translactionid",tradeid);
        }
        String taskcode = (String)parameter.getParameter("taskcode");
        if(StringUtils.isBlank(taskcode)){
            taskcode = xml.getId();
            parameter.addParameter("taskcode",taskcode);
        }
        String address = (String)parameter.getParameter("notifyaddress");
        /*if(StringUtils.isBlank(address))
            address="";
        parameter.addParameter("xml",xml);*/
        tradeConsole.addTransactionTask(obj, tradeid, taskcode, parameter, address);
        addChileren(obj,tradeid,parameter,address);

        Object[] cds = (Object[])parameter.getParameter(XMLParameter.XMLLOGIC_BACK_CALL_KEY);
        if(null != cds){
            Object iml = cds[0];

            Object[] xs = (Object[])cds[3];
            XMLParameter par = (XMLParameter)xs[0];
            XMLMakeup x = (XMLMakeup)xs[1];

            if(x.getChildren().size()>0){
                for(XMLMakeup cx:x.getChildren()){
                    String a = cx.getProperties().getProperty("action");
                    par.addParameter("xml",cx);
                    //XMLDoObject doAction = (XMLDoObject) ObjectUtils.getValueByPath(par.getParameter("${actions}"), a);
                    XMLDoObject doAction = (XMLDoObject) obj.getObjectById(a);
                    tradeConsole.addTransactionTask(doAction, tradeid, cx.getId(), par, address);
                }
            }
        }

        ResultCheck rc =  (ResultCheck)tradeConsole.getResult(tradeid);
        if(null != rc && log.isDebugEnabled()){
            System.out.println(rc.getRet());
        }
        return rc;

        }catch (Exception e){
            e.printStackTrace();
            log.error(e);
        }
        return null;
    }

    void addChileren(XMLDoObject obj,String tradeid,XMLParameter par,String address) throws Exception {
        if(null != obj){
            List<XMLMakeup> ls = obj.getXML().getChildren();
            if(null != ls && ls.size()>0){
                for(XMLMakeup x:ls){
                    //XMLDoObject doAction = (XMLDoObject) ObjectUtils.getValueByPath(par.getParameter("${actions}"), x.getId());
                    XMLDoObject doAction = (XMLDoObject) obj.getObjectById(x.getId());
                    tradeConsole.addTransactionTask(doAction, tradeid, x.getId(), par, address);
                    addChileren(doAction,tradeid,par,address);
                }
            }
        }
    }

    @Override
    public boolean isAsyn() {
        return false;
    }

}
