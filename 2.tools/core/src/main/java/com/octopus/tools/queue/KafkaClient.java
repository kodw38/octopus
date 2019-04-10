package com.octopus.tools.queue;

import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.thread.ThreadPool;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.*;

/**
 * Created by robai on 2017/11/1.
 */
public class KafkaClient extends XMLDoObject{
    static transient Log log = LogFactory.getLog(KafkaClient.class);
    KafkaProducer<String, String> producer;
    KafkaConsumer<String ,String> consumer;
    Set topics;
    int threadnum;
    Map<String,String> topicMap;
    ThreadPool threadPool;
    public KafkaClient(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        Properties props = xml.getChild("properties")[0].getPropertiesByChildNameAndText("property");
        producer = new KafkaProducer<String, String>(props);
        consumer = new KafkaConsumer<String ,String>(props);
        topicMap = xml.getChild("consumerhandlers")[0].getChildrenKeyValue("topic");
        topics = topicMap.keySet();
        threadnum = Integer.parseInt(xml.getProperties().getProperty("workthreadcount"));
        threadPool = ExecutorUtils.getFixedThreadPool("kafkaClient_task_threadnum",threadnum);

    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;
    }


    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(null != input){
            if("send".equals(input.get("op"))) {
                if(input.get("value") instanceof Map) {
                    send((String) input.get("topic"), (String) input.get("key"), ObjectUtils.convertMap2String((Map) input.get("value")));
                }else if(input.get("value") instanceof String){
                    send((String) input.get("topic"), (String) input.get("key"), (String) input.get("value"));
                }
                return true;
            }

        }
        return null;
    }

    @Override
    public ResultCheck checkReturn(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return new ResultCheck(true,ret);
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret, Exception e) throws Exception {
        return false;
    }

    public void send(String topic,String key,String value){
            ProducerRecord<String, String> record = new ProducerRecord<String, String>(topic, key, value);
            producer.send(record, new Callback() {
                @Override
                public void onCompletion(RecordMetadata metadata, Exception e) {
                    // TODO Auto-generated method stub
                    if (e != null)
                        log.error("the producer has a error:" + e.getMessage());
                    else {
                        if(log.isDebugEnabled()) {
                            log.debug("The offset of the record we just sent is: " + metadata.offset());
                            log.debug("The partition of the record we just sent is: " + metadata.partition());
                        }
                    }
                }
            });
    }

    public void doInitial(){
        ExecutorUtils.work(new Runnable(){

            @Override
            public void run() {
                consumer.subscribe(topics);
                final int minBatchSize = 1;  //批量提交数量
                List<ConsumerRecord<String, String>> buffer = new ArrayList();
                while (true) {
                    ConsumerRecords<String, String> records = consumer.poll(100);
                    for (ConsumerRecord<String, String> record : records) {
                        String act = topicMap.get(record.topic());
                        if(StringUtils.isNotBlank(act)){
                            threadPool.getExecutor().execute(new QueueWork(act,record.key(),record.value()));
                        }
                        if(log.isDebugEnabled()) {
                            log.debug("consumer message values is " + record.value() + " and the offset is " + record.offset());
                        }
                        buffer.add(record);
                    }
                    if (buffer.size() >= minBatchSize) {
                        if(log.isDebugEnabled()) {
                            log.debug("now commit offset" + buffer.size());
                        }
                        consumer.commitSync();
                        buffer.clear();
                    }
                }
            }
        });

    }
    class QueueWork implements Runnable{
        String action,key,value;
        public  QueueWork(String action,String key,String value){
            this.action=action;
            this.key=key;
            this.value=value;
        }
        @Override
        public void run() {
            XMLDoObject o = (XMLDoObject)getObjectById(action);
            try {
                if(null!= o) {
                    XMLParameter x = o.getEmptyParameter();
                    HashMap map = new HashMap();
                    map.put("key", key);
                    map.put("value", value);
                    //log.info("kafka receive.....");
                    log.debug("kafka client receive data key[" + map.get("key") + "] value[" + map.get("value") + "]");
                    x.put("${input_data}", map);
                    o.doThing(x, null);
                }
            }catch (Throwable e){
                log.error("kafka client receive data deal with error",e);
            }
        }
    }
}
