package com.octopus.tools.dataclient.dataquery.ws4;

import com.octopus.tools.dataclient.dataquery.AngleConfig;
import com.octopus.tools.dataclient.dataquery.AngleQuery;
import com.octopus.tools.dataclient.dataquery.FieldMapping;
import com.octopus.tools.dataclient.dataquery.FieldValue;
import com.octopus.tools.dataclient.v2.DataClient2;
import com.octopus.utils.alone.ObjectUtils;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-8-5
 * Time: 下午2:31
 */
public class WSAdapter {
    AngleQuery angleQuery = new AngleQuery();

    public void setConfig(AngleConfig config){
        AngleQuery.setConfig(config);
    }
    public void setDataSource(DataClient2 dc){
        AngleQuery.setDataClient(dc);
    }

    public String[] query(WSQueryBean queryBean) throws Exception {

        List<Map<String,String>> ret =  angleQuery.query(null,queryBean.getQueryFields(), getMap(queryBean.getFieldMapping()), getValue(queryBean.getFieldValue()), null);
        return getRet(ret);
    }
    public String[] queryByRedis(Jedis jedis,WSQueryBean queryBean) throws Exception {

        List<Map<String,String>> ret =  angleQuery.query(jedis,queryBean.getQueryFields(), getMap(queryBean.getFieldMapping()), getValue(queryBean.getFieldValue()), null);
        return getRet(ret);
    }
    Map<String,String> getMap(FieldMapping[] maps){
        if(null != maps){
            HashMap ret = new HashMap();
            for(FieldMapping f:maps){
               ret.put(f.getField1(),f.getField2());
            }
            return ret;
        }
        return null;
    }
    Map<String,List<String>> getValue(FieldValue[] vs){
        if(null != vs){
            HashMap<String,List<String>> ret = new HashMap();
            for(FieldValue f:vs){
                if(!ret.containsKey(f.getField())) ret.put(f.getField(),new ArrayList<String>());
                for(String v :f.getValues())
                ret.get(f.getField()).add(v);
            }
            return ret;
        }
        return null;
    }
    String[] getRet(List<Map<String,String>> ret){
        if(null != ret){
            String[] ms = new String[ret.size()];
            for(int i=0;i<ms.length;i++)
                ms[i]=ObjectUtils.convertMap2String(ret.get(i));
            return ms;
        }
        return null;
    }
}
