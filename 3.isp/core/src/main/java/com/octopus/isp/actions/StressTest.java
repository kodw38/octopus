package com.octopus.isp.actions;

import com.alibaba.otter.canal.example.StringUtils;
import com.octopus.utils.cls.javassist.bytecode.analysis.Executor;
import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by kod on 2017/5/5.
 */
public class StressTest extends XMLDoObject {
    static Log log = LogFactory.getLog(StressTest.class);
    public StressTest(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
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
        if(null != input){
            int tc = Integer.parseInt((String)input.get("tc"));

            long dl = 0;
            if(StringUtils.isNotBlank((String)input.get("dl")) && StringUtils.isNumeric((String)input.get("dl"))) {
                dl = Long.valueOf((String) input.get("dl"));//minute
            }
            int times = 0;
            if(StringUtils.isNotBlank((String) input.get("times")) && StringUtils.isNumeric((String)input.get("times"))) {
                times = Integer.valueOf((String) input.get("times"));//times
            }

            String action = (String)input.get("action");
            Map actioninput = (Map)input.get("actionInput");
            Map copyFields = (Map)input.get("copyFields");
            XMLDoObject notify = (XMLDoObject)getObjectById("websocket");
            copyFields = env.getMapValueFromParameter(copyFields,this);

            if(log.isDebugEnabled()){
                log.debug("copy env:\n"+copyFields);
            }

            AtomicLong counter = new AtomicLong();
            AtomicLong errorCounter = new AtomicLong();
            if(StringUtils.isNotBlank(action)) {
                XMLObject o = getObjectById(action);
                if(null != o) {
                    if (dl > 0 && tc > 0) {
                        ExecutorUtils.stressWork(counter,errorCounter,notify,tc, dl * 60, o, "doThing",actioninput, new Class[]{XMLParameter.class, XMLMakeup.class, List.class}, new Object[]{env, o.getXML(), copyFields});
                    }
                    if (tc > 0 && times > 0) {
                        long l= ExecutorUtils.stressWork(tc, times, o, "doThing",actioninput, new Class[]{XMLParameter.class, XMLMakeup.class, List.class}, new Object[]{env, o.getXML(), copyFields});
                        return "{costtime:"+l+"}";
                    }
                }
            }
        }
        return true;
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
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config,Object ret,Exception e) throws Exception {
        return false;
    }
}
