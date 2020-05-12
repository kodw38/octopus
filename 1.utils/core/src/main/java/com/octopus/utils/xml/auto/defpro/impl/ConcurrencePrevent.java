package com.octopus.utils.xml.auto.defpro.impl;

import com.octopus.utils.alone.SNUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.exception.ISPException;
import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import com.octopus.utils.xml.auto.defpro.IObjectInvokeProperty;
import org.apache.htrace.commons.logging.Log;
import org.apache.htrace.commons.logging.LogFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisDataException;

import java.util.HashMap;
import java.util.Map;

import static com.octopus.utils.xml.auto.XMLDoObject.NOT_EXE_PROPERTY;

/**
 * Created by admin on 2020/2/24.
 */
public class ConcurrencePrevent implements IObjectInvokeProperty {
    private static Log log = LogFactory.getLog(ConcurrencePrevent.class);
    @Override
    public Object exeProperty(Map proValue, XMLDoObject obj, XMLMakeup xml, XMLParameter parameter, Map input, Map output, Map config) throws Exception {
        XMLDoObject r = (XMLDoObject) obj.getObjectById("RedisClient");
        if(null != r) {
            String key=null;
            int wait=0;
            if (null != proValue && proValue.containsKey("key") && null != proValue.get("key") && proValue.get("key") instanceof String) {
                key = (String) proValue.get("key");
            }
            if (null != proValue && proValue.containsKey("wait") && null != proValue.get("wait")) {
                if(proValue.get("wait") instanceof String)
                wait = Integer.parseInt((String) proValue.get("wait"));
                if(proValue.get("wait") instanceof Integer)
                    wait = (Integer)proValue.get("wait");
            }
            log.info("ConcurrencePrevent key:"+key);
            if(StringUtils.isNotBlank(key) && !XMLParameter.isHasRetainChars(key)) {
                if(wait==0) {
                    rejectOther(r, key, obj, xml, parameter, input, output, config);
                }else{
                    waitOther(r, key, obj, xml, parameter, input, output, config,wait);
                }
            }else{
                return NOT_EXE_PROPERTY;
            }
        }else{
            log.error("need RedisClient component, can not exe the property SingleThreadExeProperty");
            return NOT_EXE_PROPERTY;
        }
        return null;
    }
    void rejectOther(XMLDoObject r,String key,XMLDoObject obj, XMLMakeup xml, XMLParameter parameter, Map input, Map output, Map config) throws Exception {
        Jedis j = null;
        try {
            HashMap in = new HashMap();
            in.put("op", "borrow");
            in.put("db", "system");
            j = (Jedis) r.doSomeThing(null, null, in, null, null);
            long n = j.incr(key);
            System.out.println(n);
            if(n<=1) {
                doAction(obj, xml, parameter, input, output, config);
                log.debug(xml.getId()+" ConcurrencePrevent executed");
            }else{
                throw new ISPException("REPEAT_REQUEST","sorry . There is a similar request being processed, please wait a minute again");
            }
        } finally {
            if (null != j) {
                long c =j.decr(key);
                if(c==0)j.del(key);
                HashMap in = new HashMap();
                in.put("op", "return");
                in.put("db", "system");
                in.put("obj", j);
                r.doSomeThing(null, null, in, null, null);
            }
        }
    }
    void waitOther(XMLDoObject r,String key,XMLDoObject obj, XMLMakeup xml, XMLParameter parameter, Map input, Map output, Map config,int wait) throws Exception {
        Jedis j = null;
        String id = SNUtils.getUUID();
        try {
            HashMap in = new HashMap();
            in.put("op", "borrow");
            in.put("db", "system");
            j = (Jedis) r.doSomeThing(null, null, in, null, null);
            try {
                long n = j.rpush(key, id);
            }catch (JedisDataException ex){
                j.del(key);
                j.rpush(key, id);
            }
            j.expire(key, wait);
            long t = System.currentTimeMillis();
            long w = wait*1000;
            while (true) {
                if (System.currentTimeMillis() - t > w) {
                    //timeout
                    throw new Exception("sorry. There is a similar request being processed, please wait a minute again");
                }
                String curid = j.lindex(key, 0);
                if(curid==null){
                    //timeout
                    throw new Exception("sorry. There is a similar request being processed, please wait a minute again");
                }
                if(curid.equals(id)) {
                    doAction(obj, xml, parameter, input, output, config);
                    break;
                }
                Thread.sleep(10);
            }
        } finally {
            if (null != j) {
                j.lrem(key, 0, id);
                HashMap in = new HashMap();
                in.put("op", "return");
                in.put("db", "system");
                in.put("obj", j);
                r.doSomeThing(null, null, in, null, null);
            }
        }
    }

        void doAction(XMLDoObject obj,XMLMakeup xml,XMLParameter parameter,Map input,Map output,Map config)throws Exception{
        obj.doCheckThing(xml.getId(), parameter, input, output, config, xml);
        Object[] callback = (Object[])parameter.getParameter(XMLParameter.XMLLOGIC_BACK_CALL_KEY);
        if(null != callback){
            ExecutorUtils.synWork(callback[0], (String) callback[1], (Class[]) callback[2], new Object[]{parameter,((Object[]) callback[3])[1],((Object[]) callback[3])[2]});
        }
    }
    @Override
    public boolean isAsyn() {
        return false;
    }
}
