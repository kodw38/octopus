package com.octopus.utils.si.jvm;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by kod on 2017/6/7.
 * Modify by ligs on 2017/6/21.
 */
public class JVMMBeanManager{
    transient static Log log = LogFactory.getLog(JVMMBeanManager.class);
    private static ConcurrentHashMap<String,MBeanServerConnection> connCache = new ConcurrentHashMap<String, MBeanServerConnection>(); 
    public static MBeanManager getInstance(String ip,int port){
        try{
        	final String key = ip+":"+port;
        	if(null == connCache.get(key)){
        		synchronized (connCache) {
        			if(null == connCache.get(key)){
	        			JMXServiceURL address = new JMXServiceURL( "service:jmx:rmi:///jndi/rmi://"+ip+":"+port+"/jmxrmi");
	                    JMXConnector connector = JMXConnectorFactory.connect(address);
	                    MBeanServerConnection mbs = connector.getMBeanServerConnection();
	                    connCache.put(key, mbs);
        			}
				}
        	}
            return new MBeanManager(connCache.get(key));
        } catch(Exception e){
            log.error("JVMMBeanManager.getInstance error",e);
            return null;
        }
    }
}
