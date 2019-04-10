package com.octopus.tools.dataclient.dataquery;

import com.octopus.tools.dataclient.dataquery.redis.RedisClient;
import com.octopus.tools.synchro.canal.AbstractMessageHandler;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLParameter;
import redis.clients.jedis.Jedis;

import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-8-13
 * Time: 上午11:17
 */
public class CanalRealTimHandler extends AbstractMessageHandler {
    AngleConfig config=null;
    String rediskey;
    RedisClient rc;
    public CanalRealTimHandler(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);

        rediskey = (String)StringUtils.convert2MapJSONObject(xml.getProperties().getProperty("config")).get("redis");
        rc = (RedisClient)getObjectById("RedisClient");

        config=(AngleConfig)getPropertyObject("config");


    }
    Jedis getRedis() throws Exception {
        return rc.getRedis(rediskey);
    }

    @Override
    public void doRecord(XMLParameter env,String type, String tableCode, Map oldData, Map newData) throws Exception {
        tableCode=tableCode.substring(tableCode.lastIndexOf(".")+1);
        if(type.equals("INSERT")){
                Jedis jedis = getRedis();
                try {
                    AngleLoader.appendData(config,jedis,tableCode,newData,null,null);
                }finally {
                    jedis.close();
                }
        }else if(type.equals("DELETE")){

                Jedis jedis = getRedis();
                try{
                    AngleLoader.deleteData(config,jedis,tableCode, oldData);
                }finally {
                    jedis.close();
                }
        }else if(type.equals("UPDATE")){

                Jedis jedis = getRedis();
                try{
                    AngleLoader.updateData(config,jedis,tableCode, oldData, newData,null,null);
                }finally {
                    jedis.close();
                }
        }

    }

    @Override
    public Object doSomeThing(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
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
