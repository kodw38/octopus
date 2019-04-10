package com.octopus.tools.cache.impl;

import com.octopus.tools.alarm.IAlarm;
import com.octopus.tools.cache.ICacheEvent;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cls.ClassUtils;
import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * User: Administrator
 * Date: 14-9-18
 * Time: 上午11:05
 */
public class EventMaxCountLimit extends XMLDoObject implements ICacheEvent {
    IAlarm alarm;

    public EventMaxCountLimit(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    @Override
    public boolean doCacheEvent(String method, String key, Object value) {
        //如果配置了最大数量、内存大小限制
        String mc = getXML().getProperties().getProperty("maxcount");
        String alarmtype = getXML().getProperties().getProperty("alarmtype");
        if(StringUtils.isNotBlank(mc)){
            int max = Integer.parseInt(mc);
            if(null != getParent()){
                try{
                    Object f = ClassUtils.getFieldValue(getParent(), "size", false);
                    if(null != f){
                        Number n = (Number)f;
                        if(n.intValue()>max){
                            if(null != alarm && StringUtils.isNotBlank(alarmtype)){
                                HashMap map = new HashMap();
                                map.put("MaxCountLimit",mc);
                                map.put("CurrentCount",n.intValue());
                                ExecutorUtils.work(alarm, "addAlarm", new Class[]{List.class, List.class, String.class, String.class, Date.class, int.class}, new Object[]{alarmtype, map});
                            }
                            return false;
                        }
                    }
                }catch (Exception e){
                    throw new RuntimeException(e);
                }
            }
        }
        String mm = getXML().getProperties().getProperty("maxmemsize");
        if(StringUtils.isNotBlank(mm)){
            int max = Integer.parseInt(mm);
            if(null != getParent()){
                try{
                    long m = ObjectUtils.getSizeOfJavaObject(getParent());
                    if(m>max){
                        if(null != alarm && StringUtils.isNotBlank(alarmtype)){
                            HashMap map = new HashMap();
                            map.put("MaxMemLimit",mm);
                            map.put("CurrentObjectMemSize",m);
                            ExecutorUtils.work(alarm, "addAlarm", new Class[]{List.class, List.class, String.class, String.class, Date.class, int.class}, new Object[]{alarmtype, map});
                        }
                        return false;
                    }
                }catch (Exception e){
                    throw new RuntimeException(e);
                }
            }
        }
        return true;
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        doCacheEvent((String)input.get("op"),(String)input.get("key"),input.get("value"));
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;  //To change body of implemented methods use File | Settings | File Templates.
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
