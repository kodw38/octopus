package com.octopus.tools.client;/**
 * Created by admin on 2020/6/26.
 */

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Map;

/**
 * @ClassName MQTTClient
 * @Description ToDo
 * @Author Kod Wong
 * @Date 2020/6/26 9:36
 * @Version 1.0
 * <action key="mqttClient" clazz="com.octopus.tools.client.MQTTClient">
 *     <property key="id">main</property>
 *     <property key="connectionTimeout">30</property>
 *     <property key="isCleanSession">true</property>
 *     <property key="address">tcp://47.111.102.176:1883</property>
 * </action>
 **/
public class MQTTClient extends XMLDoObject{
    transient static Log log = LogFactory.getLog(MQTTClient.class);
    public static org.eclipse.paho.client.mqttv3.MqttClient mqttClient = null;
    private static MemoryPersistence memoryPersistence = null;
    private static MqttConnectOptions mqttConnectOptions = null;

    String clientID;
    boolean cleanSession;
    int connectionTimeout;
    String mqttAddress;//tcp://47.111.102.176:1883

    public MQTTClient(XMLMakeup xml, XMLObject parent, Object[] containers) throws Exception {
        super(xml, parent, containers);
        clientID = xml.getFirstCurChildText("property","key","id");
        connectionTimeout = Integer.parseInt(xml.getFirstCurChildText("property","key","connectionTimeout"));
        cleanSession = StringUtils.isTrue(xml.getFirstCurChildText("property", "key", "isCleanSession"));
        mqttAddress = xml.getFirstCurChildText("property", "key", "address");
    }

    public void init(String clientId) {
        //初始化连接设置对象
        mqttConnectOptions = new MqttConnectOptions();
        //初始化MqttClient
        if(null != mqttConnectOptions) {
//			true可以安全地使用内存持久性作为客户端断开连接时清除的所有状态
            mqttConnectOptions.setCleanSession(cleanSession);
//			设置连接超时
            mqttConnectOptions.setConnectionTimeout(connectionTimeout);
//			设置持久化方式
            memoryPersistence = new MemoryPersistence();
            if(null != memoryPersistence && null != clientId) {
                try {
                    mqttClient = new org.eclipse.paho.client.mqttv3.MqttClient(mqttAddress, clientId,memoryPersistence);
                } catch (MqttException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }else {

            }
        }else {
            log.error("mqttConnectOptions对象为空");
        }
        //设置连接和回调
        if(null != mqttClient) {
            if(!mqttClient.isConnected()) {
                try {
                    log.info("创建连接:" + mqttClient.isConnected());
                    mqttClient.connect(mqttConnectOptions);
                } catch (MqttException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        }else {
            log.error("mqttClient为空");
        }
    }

    //	关闭连接
    public void closeConnect() {
        //关闭存储方式
        if(null != memoryPersistence) {
            try {
                memoryPersistence.close();
            } catch (MqttPersistenceException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }else {
            log.error("memoryPersistence is null");
        }

//		关闭连接
        if(null != mqttClient) {
            if(mqttClient.isConnected()) {
                try {
                    mqttClient.disconnect();
                    mqttClient.close();
                } catch (MqttException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }else {
                log.error("mqttClient is not connect");
            }
        }else {
            log.error("mqttClient is null");
        }
    }

    //	发布消息
    public void publishMessage(String pubTopic,String message,int qos) {
        if(null != mqttClient&& mqttClient.isConnected()) {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setQos(qos);
            mqttMessage.setPayload(message.getBytes());
            MqttTopic topic = mqttClient.getTopic(pubTopic);
            if(null != topic) {
                try {
                    MqttDeliveryToken publish = topic.publish(mqttMessage);
                    if(!publish.isComplete()) {
                        //log.info("消息发布成功");
                    }
                } catch (MqttException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        }else {
            reConnect();
        }

    }
    //	重新连接
    public  void reConnect() {
        if(null != mqttClient) {
            if(!mqttClient.isConnected()) {
                if(null != mqttConnectOptions) {
                    try {
                        mqttClient.connect(mqttConnectOptions);
                    } catch (MqttException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }else {
                    log.error("mqttConnectOptions is null");
                }
            }else {
                log.error("mqttClient is null or connect");
            }
        }else {
            init("admin");
        }

    }
    //	订阅主题
    public void subTopic(String topic) {
        if(null != mqttClient&& mqttClient.isConnected()) {
            try {
                mqttClient.subscribe(topic, 1);
            } catch (MqttException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }else {
            log.error("mqttClient is error");
        }
    }


    //	清空主题
    public void cleanTopic(String topic) {
        if (null != mqttClient && !mqttClient.isConnected()) {
            try {
                mqttClient.unsubscribe(topic);
            } catch (MqttException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            log.error("mqttClient is error");
        }
    }

    @Override
    public void doInitial() throws Exception {
        init(clientID);
    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
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
}
