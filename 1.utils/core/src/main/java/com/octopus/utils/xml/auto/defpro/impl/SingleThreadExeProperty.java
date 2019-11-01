package com.octopus.utils.xml.auto.defpro.impl;

import com.octopus.utils.alone.SNUtils;
import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import com.octopus.utils.xml.auto.defpro.IObjectInvokeProperty;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import redis.clients.jedis.Jedis;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * globalsingle=true
 * 在集群部署情况下,执行到同一个do key时,排队单任务一个一个执行.
 * Created by Administrator on 2018/5/10.
 */
public class SingleThreadExeProperty implements IObjectInvokeProperty {
    static transient Log log = LogFactory.getLog(SingleThreadExeProperty.class);
    static long setFlag=900000000;
    static long MAX_NUMBER=20;
    static int timeout=30000;

    @Override
    public Object exeProperty(Map proValue, XMLDoObject obj, XMLMakeup xml, XMLParameter parameter, Map input, Map output, Map config) {
        try {
            XMLDoObject r = (XMLDoObject) obj.getObjectById("RedisClient");
            HashMap in = new HashMap();
            in.put("op", "borrow");
            in.put("db", "system");
            Jedis j = (Jedis) r.doSomeThing(null, null, in, null, null);
            if(null != j) {
                String lockKey = xml.getId();
                if(null != proValue && proValue.containsKey("lockKey") && null != proValue.get("lockKey") && proValue.get("lockKey") instanceof String){
                    lockKey=(String) proValue.get("lockKey")+"."+xml.getId();
                }
                boolean isOneTimeInSameLockKey=false;
                if(null != proValue && proValue.containsKey("isOneTimeInSameLockKey") && null != proValue.get("isOneTimeInSameLockKey") && proValue.get("isOneTimeInSameLockKey") instanceof Boolean){
                    isOneTimeInSameLockKey=(Boolean) proValue.get("isOneTimeInSameLockKey");
                }
                String id = SNUtils.getUUID();
                boolean isOwner=false;
                int waitTimeout = timeout;
                if(null != proValue && proValue.containsKey("waitTimeout") && null != proValue.get("waitTimeout") && proValue.get("waitTimeout") instanceof Integer){
                    waitTimeout=(Integer) proValue.get("waitTimeout");
                }
                log.info("globalsingle begin doing action lockKey:"+lockKey+",isOneTimeInSameLockKey:"+isOneTimeInSameLockKey+",waitTimeout:"+waitTimeout);
                try {
                    if(j.llen(lockKey)==0){
                        j.rpush(lockKey, id);
                        isOwner=true;
                        j.expire(lockKey,waitTimeout/1000);
                    }else {
                        j.rpush(lockKey, id);
                    }
                    long t = System.currentTimeMillis();
                    while(true){
                        if (System.currentTimeMillis() - t > waitTimeout) {
                            if(!isOneTimeInSameLockKey) {
                                log.info("globalsingle start doing action "+xml.getId());
                                doAction(obj, xml, parameter, input, output, config);
                            }
                            throw new Exception("wait timeout");
                        }
                        Thread.sleep(1);
                        String curid = j.lindex(lockKey,0);
                        if(curid.equals(id)){
                            if(isOwner||!isOneTimeInSameLockKey) {
                                log.info("globalsingle start doing action "+xml.getId());
                                doAction(obj, xml, parameter, input, output, config);
                            }
                            break;
                        }
                    }

                    //judge other wf instance working the custId,if exit and not create then waiting
                    /*String v = j.get(tempKey);
                    if (null != v) {
                        long l = Long.parseLong(v);
                        long t = System.currentTimeMillis();
                        try {
                            while (l < setFlag) {
                                Thread.sleep(5);
                                l = j.incrBy(tempKey, 0);
                                if (System.currentTimeMillis() - t > timeout) {
                                    throw new Exception("wait timeout");
                                }
                            }
                        } catch (Exception e) {
                            log.error("lock single thread execute error", e);
                        }
                        return true;
                    } else {
                        boolean isExist = false;
                        long oldCount = j.incrBy(tempKey, 1);
                        j.expire(tempKey, timeout);
//                j.decr(custId);
                        if (oldCount == 1) {
                            //do thing
                            try {
                                System.out.println("0000000000000000000000"+obj+"\n"+input);
                                obj.doCheckThing(xml.getId(), parameter, input, output, config, xml);
                                System.out.println("1111111111111111111111");
                            } catch (Exception e) {
                                log.error("single thread execute doCheckThing error", e);
                            }
                        } else {
                            long l = j.incrBy(tempKey, 0);
                            long t = System.currentTimeMillis();
                            try {
                                while (l < setFlag) {
                                    Thread.sleep(5);
                                    l = j.incrBy(tempKey, 0);
                                    if (System.currentTimeMillis() - t > timeout) {
                                        throw new Exception("wait timeout");
                                    }
                                }
                            } catch (Exception e) {
                                log.error("lock single thread execute error", e);
                            }
                            isExist = true;
                        }
                        return isExist;
                    }*/
                } finally {
                    log.info("globalsingle end doing action "+xml.getId());
                    j.lrem(lockKey,0,id);
                    in.put("op","return");
                    in.put("obj",j);
                    r.doSomeThing(null,null,in,null,null);
                }
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return null;
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
