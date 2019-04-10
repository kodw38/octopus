package com.octopus.tools.queue;

import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import kafka.Kafka;
import kafka.server.KafkaServerStartable;

import java.security.AccessControlException;
import java.security.Permission;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.Properties;

/**
 * Created by robai on 2017/10/27.
 */
public class KafkaQueue extends XMLDoObject {
    public KafkaQueue(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    public void doInitial(){
        XMLMakeup[] is = getXML().getChild("instance");
        if(null!= is && is.length>0) {
            for(XMLMakeup in:is) {
                Properties properties = in.getPropertiesByChildNameAndText("property");
                ExecutorUtils.work(this,"startKafka",new Class[]{Properties.class},new Object[]{properties});
                //startKafka(properties);
            }
        }
    }

    class KafkaRun implements Runnable{
        KafkaServerStartable kafkaServerStartable;
        KafkaRun(KafkaServerStartable kafkaServerStartable){
            this.kafkaServerStartable=kafkaServerStartable;
        }

        @Override
        public void run() {
            kafkaServerStartable.shutdown();
        }
    }
    public void startKafka(Properties serverProps)
    {
        try
        {
            Policy.setPolicy(new Policy(){
                @Override
                public boolean implies(ProtectionDomain domain, Permission permission) {
                    if (permission instanceof javax.management.MBeanTrustPermission) {
                        return true;
                    } else {
                        return true;//defaultPolicy.implies(domain, permission);
                    }
                }
            });

            KafkaServerStartable kafkaServerStartable = KafkaServerStartable.fromProps(serverProps);
            Runtime.getRuntime().addShutdownHook(new Thread(new KafkaRun(kafkaServerStartable)));
            kafkaServerStartable.startup();
            kafkaServerStartable.awaitShutdown();
            System.exit(0);
            //Kafka.main(new String[]{"C:\\Users\\robai\\Downloads\\kafka_2.11-0.11.0.1\\config\\server.properties"});
        }
        finally
        {

        }
    }


    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return false;
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return null;
    }

    @Override
    public ResultCheck checkReturn(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return null;
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret, Exception e) throws Exception {
        return false;
    }
}
