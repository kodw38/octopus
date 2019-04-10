package com.octopus.utils.xml.auto.logic;

import com.octopus.utils.transaction.XMLDoTradeTask;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.Map;



/**
 * User: wfgao_000
 * Date: 15-11-11
 * Time: 上午9:18
 */
public class XMLLogicTrade extends XMLDoObject {
    TradeConsole tradeConsole;
    XMLDoObject statusQueue;
    String finish=null;
    public XMLLogicTrade(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        String instanceid = xml.getRoot().getFirstCurChildText("instanceid");
        /*String activeXAddr=xml.getFirstCurChildText("activexaddr");
        String redis=xml.getFirstCurChildText("redis");
        */
        finish = xml.getFirstCurChildText("finish");
        String logger = xml.getFirstCurChildText("logger");
        tradeConsole = new TradeConsole(instanceid);
        tradeConsole.setRedisLoger(getObjectById("RedisClient"),logger);
        tradeConsole.setStatusQueue(statusQueue);

    }

    public String newTradeId(){
        return tradeConsole.newTranslactionId();
    }

    public void addTradeTask(XMLParameter env,Object task,String tradeId,String taskCode,Object pars,String receiveAddress) throws Exception {
        if(null == tradeConsole.getFinished(tradeId)){
            TradeFinish f = (TradeFinish)Class.forName(finish).newInstance();
            f.setParameter(env);
            tradeConsole.setFinished(tradeId,f);
        }
        tradeConsole.addTransactionTask(task,tradeId,taskCode,pars,receiveAddress);
    }
    public Object getResult(String tradeId)throws Exception{
        Object o=null;
        try {
            o = tradeConsole.getResult(tradeId);
            return o;
        }catch (Exception e){
            throw e;
        }finally {
            tradeConsole.clear(tradeId);
        }
    }

    @Override
    public Object doSomeThing(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        Object[] ps = (Object[])env.getTradeTasks().get(xmlid);
        if(null != ps) {
            XMLDoTradeTask task = new XMLDoTradeTask((XMLDoObject) ps[0],statusQueue);
            addTradeTask(env, task, (String) ps[1], (String) ps[2], ps[3], (String) ps[4]);
        }
        return null;
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResultCheck checkReturn(String xmlid,XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config,Object ret,Exception e) throws Exception {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
