package com.octopus.isp.bridge.launchers.impl.pageframe.channel;/**
 * Created by admin on 2020/7/17.
 */

import com.octopus.isp.ds.RequestParameters;
import com.octopus.tools.dataclient.dataquery.redis.RedisClient;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.exception.ISPException;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.Map;

/**
 * @ClassName AsyncServletTrans
 * @Description ToDo
 * @Author Kod Wong
 * @Date 2020/7/17 12:58
 * @Version 1.0
 **/
public class AsyncServletTrans extends XMLDoObject {
    transient static Log log = LogFactory.getLog(AsyncServletTrans.class);
    String redisDB,responseRedisKey,requestRedisKeyPrefix;

    public AsyncServletTrans(XMLMakeup xml, XMLObject parent, Object[] containers) throws Exception {
        super(xml, parent, containers);
        Map config = StringUtils.convert2MapJSONObject(xml.getProperties().getProperty("config"));
        redisDB = (String)config.get("redisDB");
        responseRedisKey = (String)config.get("responseRedisKey");
        requestRedisKeyPrefix = (String)config.get("requestRedisKeyPrefix");
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {

        return true;
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        String op = (String)input.get("op");
        if("receiveAsync".equals(op)) {
            Jedis jedis = null;
            try {
                RedisClient redis = (RedisClient) getObjectById("RedisClient");
                //放入待处理队列，交由家庭服务处理
                jedis = redis.getRedis(redisDB);
                String key = requestRedisKeyPrefix+((RequestParameters)env).getSession().getUserName();
                List<String> data = jedis.lrange(key,0,0);
                if(null != data && data.size()==1){
                    jedis.lrem(key,1,data.get(0));
                    if(log.isDebugEnabled()){
                        log.debug("receiveAsync data:"+data.get(0));
                    }
                    return data.get(0);
                }
            } catch (Exception e) {
                log.error("",e);
            } finally {
                if (jedis != null) jedis.close();
            }
        }else if("replyAsync".equals(op)){
            Jedis jedis = null;
            try{
                Map m = (Map)input.get("data");
                RedisClient redis = (RedisClient) getObjectById("RedisClient");
                //放入待处理队列，交由家庭服务处理
                jedis = redis.getRedis(redisDB);
                if(m.containsKey("requestId")){
                    if(log.isDebugEnabled()){
                        log.debug("replyAsync data:"+m);
                    }
                    jedis.lpush(responseRedisKey, ObjectUtils.convertMap2String(m));
                }else{
                    throw new ISPException("ASYNC_REPLY_CHECK_ERR",m+"\n is not a correct reply");
                }
            }catch (Exception e){
                log.error("",e);
            }finally {
                if (jedis != null) jedis.close();
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
}
