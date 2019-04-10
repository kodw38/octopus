package com.octopus.utils.si.jvm;

import java.io.IOException;
import java.lang.management.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.management.GarbageCollectorMXBean;
import com.sun.management.HotSpotDiagnosticMXBean;
import com.sun.management.OperatingSystemMXBean;
//import com.sun.management.ThreadMXBean;
import com.sun.management.UnixOperatingSystemMXBean;

/**
 * Created by kod on 2017/6/7.
 */
public class MBeanManager {
    transient static Log log = LogFactory.getLog(JVMMBeanManager.class);
    MBeanServerConnection conn;
    public MBeanManager(MBeanServerConnection conn){
        this.conn=conn;
    }

    public int MBeanCount()throws IOException{
        return conn.getMBeanCount();
    }
    public String getDefaultDomain()throws IOException{
        return conn.getDefaultDomain();
    }
    public Set<ObjectName> getMBeanNames()throws IOException{
        return conn.queryNames(null,null);
    }
    public MBeanInfo getMBeanInfo(ObjectName name)throws IOException, InstanceNotFoundException, IntrospectionException, ReflectionException{
        return conn.getMBeanInfo(name);
    }
    public Set<ObjectInstance> getMBean(ObjectName name)throws IOException{
        return conn.queryMBeans(name,null);
    }
    public ObjectInstance getMBeanByName(ObjectName name)throws IOException{
        return conn.queryMBeans(name,null).iterator().next();
    }
    public Object mBeanMethodInvoke(ObjectName name,String operationName,Object[] params,String[] signature)throws IOException, InstanceNotFoundException, MBeanException, ReflectionException{
        return conn.invoke(name, operationName, params, signature);
    }
    public ThreadMXBean getThreadBean() {
        try {
            return ManagementFactory.newPlatformMXBeanProxy(conn, ManagementFactory.THREAD_MXBEAN_NAME, ThreadMXBean.class);
        }catch (Exception e){
            log.error("MBeanManager.getThreadBean error",e);
            return null;
        }
    }
    public RuntimeMXBean getRuntimeMXBean() {
        try {
            return ManagementFactory.newPlatformMXBeanProxy(conn, ManagementFactory.RUNTIME_MXBEAN_NAME, RuntimeMXBean.class);
        }catch (Exception e){
            log.error("MBeanManager.getRuntimeMXBean error",e);
            return null;
        }
    }
    public OperatingSystemMXBean getOperatingSystemMXBean() {
        try {
            return ManagementFactory.newPlatformMXBeanProxy(conn, ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME, OperatingSystemMXBean.class);
        }catch (Exception e){
            log.error("MBeanManager.getOperatingSystemMXBean error",e);
            return null;
        }
    }
    public CompilationMXBean getCompilationMXBean() {
        try {
            return ManagementFactory.getPlatformMXBean(conn, CompilationMXBean.class);
        }catch (Exception e){
            log.error("MBeanManager.getCompilationMXBean error",e);
            return null;
        }
    }
    public ThreadMXBean getThreadMXBean() {
        try {
            return ManagementFactory.newPlatformMXBeanProxy(conn, ManagementFactory.THREAD_MXBEAN_NAME, ThreadMXBean.class);
        }catch (Exception e){
            log.error("MBeanManager.getThreadMXBean error",e);
            return null;
        }
    }
    public ClassLoadingMXBean getClassLoadingMXBean() {
        try {
            return ManagementFactory.newPlatformMXBeanProxy(conn, ManagementFactory.CLASS_LOADING_MXBEAN_NAME, ClassLoadingMXBean.class);
        }catch (Exception e){
            log.error("MBeanManager.getClassLoadingMXBean error",e);
            return null;
        }
    }
    public List<GarbageCollectorMXBean> getGarbageCollectorMXBean() {
        try {
        	List<GarbageCollectorMXBean> garbageList = new ArrayList();
            ObjectName gcName = new ObjectName(ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE + ",*");
            for (ObjectName name : conn.queryNames(gcName, null)) {
                GarbageCollectorMXBean gc = ManagementFactory.newPlatformMXBeanProxy(conn, name.getCanonicalName(), GarbageCollectorMXBean.class);
                garbageList.add(gc);
            }
            return garbageList;
            //ManagementFactory.newPlatformMXBeanProxy(conn, ManagementFactory.GARBAGE_COLLECTOR_MXBEAN_DOMAIN_TYPE, GarbageCollectorMXBean.class);
        }catch (Exception e){
            log.error("MBeanManager.getGarbageCollectorMXBean error",e);
            return null;
        }
    }

    public MemoryMXBean getMemoryMXBean() {
        try {
            return ManagementFactory.newPlatformMXBeanProxy(conn, ManagementFactory.MEMORY_MXBEAN_NAME, MemoryMXBean.class);
        }catch (Exception e){
            log.error("MBeanManager.getMemoryPoolMXBean error",e);
            return null;
        }
    }
    public UnixOperatingSystemMXBean getUnixOperatingSystemMXBean() {
        try {
            return ManagementFactory.getPlatformMXBean(conn, UnixOperatingSystemMXBean.class);
        }catch (Exception e){
            log.error("MBeanManager.UnixOperatingSystemMXBean error",e);
            return null;
        }
    }
    public HotSpotDiagnosticMXBean getHotSpotDiagnosticMXBean() {
        try {
            return ManagementFactory.getPlatformMXBean(conn, HotSpotDiagnosticMXBean.class);
        }catch (Exception e){
            log.error("MBeanManager.HotSpotDiagnosticMXBean error",e);
            return null;
        }
    }
}
