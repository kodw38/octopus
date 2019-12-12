package com.octopus.tools.jvminsmgr;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by admin on 2019/12/7.
 */
public class MBeanRegiter extends XMLDoObject {
    public MBeanRegiter(XMLMakeup xml, XMLObject parent, Object[] containers) throws Exception {
        super(xml, parent, containers);
    }

    @Override
    public void doInitial() throws Exception {
        try
        {
            String path = (String)getXML().getProperties().get("path");
            if(StringUtils.isBlank(path)){
                path="tb.monitor";
            }
            XMLMakeup[] ps = getXML().getChild("mbean");
            if(null != ps && ps.length>0) {
                for(XMLMakeup x:ps) {
                    if(null != x) {
                        String name = x.getProperties().getProperty("key");
                        try {
                            Object o = getObjectById(name);
                            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
                            ObjectName objectName = new ObjectName(path + ":type=" + name);
                            if (!server.isRegistered(objectName)) {
                                //server.unregisterMBean(objectName);
                                server.registerMBean(o, objectName);
                                log.info("reg mbean:" + name);
                            } else {
                                log.error("exist reg mbean , can't reg :" + name);
                            }
                        } catch (Exception e) {
                            log.error("", e);
                        }
                    }
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }finally {
            log.info("finished reg MBeans");
        }
    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return false;
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return null;
    }

    @Override
    public ResultCheck checkReturn(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return null;
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
