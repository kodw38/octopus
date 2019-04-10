package com.octopus.utils.xml.auto.logic;

import com.octopus.utils.transaction.TransactionConsole;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.XMLDoObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import redis.clients.jedis.Jedis;

import java.util.HashMap;

/**
 * User: wfgao_000
 * Date: 15-11-11
 * Time: 上午9:26
 */
public class TradeConsole extends TransactionConsole {
    static transient Log log = LogFactory.getLog(TradeConsole.class);
    XMLDoObject logger;
    String redis_key;
    XMLDoObject setStatusQueue;
    public TradeConsole(String instanceId){
        super(instanceId);
    }

    public void setRedisLoger(XMLObject logger,String key) {
        this.logger = (XMLDoObject)logger;
        this.redis_key = key;
    }

    Jedis getJedis(){
        try {
            HashMap in = new HashMap();
            in.put("key", redis_key);
            in.put("op","getRedis");
            Jedis jedis = (Jedis)logger.doSomeThing(null, null, in, null, null);
            return jedis;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
    public void saveLog(String transactionId,String key,String text){
        /*Jedis jedis=null;
        try{
            jedis=getJedis();
            jedis.hset(transactionId, key, text);
        }finally {
            if(null != jedis)jedis.close();
        }*/
        if(false) {
            try {
                HashMap in = new HashMap();

                in.put("data", text);
                logger.doCheckThing(null, null, in, null, null, null);
            } catch (Exception e) {

            }
        }
    }
    public synchronized void clearAsynLog(String transactionId){
        /*Jedis jedis=null;
        try{
            jedis=getJedis();
            jedis.del(transactionId);
        }finally {
            if(null != jedis)jedis.close();
        }*/
        try {
            HashMap in = new HashMap();
            in.put("op", "delete");
            in.put("key", transactionId);
            if(false) {
                logger.doCheckThing(null, null, in, null, null, null);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //MessageConsumer consumer=null;
    public String collectionFeedbackMsg(String queue) throws Exception {
        /*if(null == consumer)
            consumer=getReceiver(queue);
        String ret = ((TextMessage)consumer.receive()).getText();
        if(null == consumer){
            log.error("trade queue ["+queue+"] is not availiable,please confirm the queue app ");
        }
        return ret;*/
        try {
            if(null != setStatusQueue) {
                HashMap in = new HashMap();
                in.put("op", "get");
                return (String) setStatusQueue.doSomeThing(null, null, in, null, null);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
    public synchronized void sendFeekBack(String msg)throws Exception{
        /*if(null == session && null == producer){
            Connection connection = null;
            ConnectionFactory connectionFactory;
            Destination destination;
            connectionFactory = new ActiveMQConnectionFactory(
                    ActiveMQConnection.DEFAULT_USER,
                    ActiveMQConnection.DEFAULT_PASSWORD,
                    activeMQAddress);
            try {
                // 构造从工厂得到连接对象
                connection = connectionFactory.createConnection();
                connection.start();
                session = connection.createSession(Boolean.FALSE,Session.AUTO_ACKNOWLEDGE);
                destination = session.createQueue(instanceinfo);
                producer = session.createProducer(destination);
                producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

            }catch (Exception e){
                log.error(e);
            }
        }
        try{
            if(log.isDebugEnabled())
                log.debug("MQ S " + System.currentTimeMillis() + " " + msg);
            TextMessage message = session.createTextMessage(msg);

            producer.send(message);
            //session.commit();
        }catch (Exception e){
            throw e;
        }*/
        HashMap in = new HashMap();
        in.put("op", "add");
        in.put("data", msg);
        setStatusQueue.doSomeThing(null, null, in, null, null);
    }

    public void setStatusQueue(XMLDoObject queue) {
        this.setStatusQueue = queue;
    }
}
