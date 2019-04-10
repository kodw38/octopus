package com.octopus.tools.cache.impl;

import com.octopus.tools.cache.ICache;
import com.octopus.tools.cache.ICacheEvent;
import com.octopus.tools.dataclient.utils.IDataMapping;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.cachebatch.AsynContainer;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-11-4
 * Time: 下午10:38
 */
public class AmortizeStoreEvent  extends XMLDoObject implements ICacheEvent {
    static transient Log log = LogFactory.getLog(AmortizeStoreEvent.class);
    IDataMapping mapping;
    AsynContainer container =null;
    public AmortizeStoreEvent(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        long intervalsecond = Long.parseLong(xml.getProperties().getProperty("intervalsecond"));
        long limitsize= Long.parseLong(xml.getProperties().getProperty("limitsize"));
        container = new AsynContainer(intervalsecond,limitsize,AmortizeTask.class);
        Object dataClient = getObjectById(xml.getProperties().getProperty("batchhandle"));
        container.setOther(dataClient);
    }

    @Override
    public boolean doCacheEvent(String method,String key,Object value) {
        try{
            //数据库保存成功后更新内存
                    if(method.equals(ICache.METHOD_NAME_ADD)){
                        if(null != mapping){
                            List<Map> tvs = mapping.mapping(value);
                            if(ArrayUtils.isNotEmpty(tvs)){
                                container.insert(tvs);
                                return true;
                            }
                        }else{
                            container.insert(value);
                        }

                    }else if(method.equals(ICache.METHOD_NAME_ADDLIST)){
                        if(null != mapping){
                        List<Map> tvs = mapping.mapping(value);
                        if(ArrayUtils.isNotEmpty(tvs)){
                            container.insert(tvs);
                            return true;
                        }
                        }else{
                            container.insert(value);
                        }
                    }
        }catch (Exception e){
            log.error(e.getMessage(),e);
        }
        return false;
    }


    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        doCacheEvent((String)input.get("op"),(String)input.get("key"),input.get("value"));
        //System.out.println(Thread.currentThread().getName()+" StoreEvent "+new Date().getTime());
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;
    }

    @Override
    public ResultCheck checkReturn(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return new ResultCheck(true,ret);  //To change body of implemented methods use File | Settings | File Templates.
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
