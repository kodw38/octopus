package com.octopus.utils.balance.qps;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Administrator on 2018/11/20.
 */
public class TestTask implements IProcess {
    transient static Log log = LogFactory.getLog(TestTask.class);

    @Override
    public Object process(Object o) throws Exception {

        if(null != o && o instanceof Map){
            Thread.sleep((Integer)((Map)o).get("sleep"));
            log.error(((Map) o).get("ID")+ " sleep:"+((Map)o).get("sleep"));

        }

        return null;
    }
}
