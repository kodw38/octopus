package com.octopus.utils.si.jvm;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.rmi.registry.LocateRegistry;
import java.util.Map;

/**
 * Created by kod on 2017/6/12.
 */
public class JVMUtil {
    public static  String getPid(){
        String name = ManagementFactory.getRuntimeMXBean().getName();
        return name.split("@")[0];
    }
    public static VirtualMachine getVM() throws IOException, AttachNotSupportedException {
        return VirtualMachine.attach(getPid());
    }

    /**
     * 
     * @Title: checkJMXConnect 
     * @Description: 校验action传入的JMXConnect参数
     * @param input
     * @return
     * @return: MBeanManager
     */
    public static MBeanManager checkJMXConnect(Map input){
		MBeanManager mBeanManager = null;
    	String host = (String) input.get("host");
		int rmiPort = Integer.parseInt((String)input.get("port"));
		rmiPort = 0 == rmiPort?Integer.parseInt((String)input.get("defalutPort")):rmiPort;
		try{
			if(null != LocateRegistry.getRegistry(host, rmiPort)){
				mBeanManager =  JVMMBeanManager.getInstance(host, rmiPort);
			}
		}catch (Exception e) {
			return null;
		}
		return mBeanManager;
    }

    public static boolean removeCache(Map input){
        String host = (String) input.get("host");
        int rmiPort = Integer.parseInt((String)input.get("port"));
        rmiPort = 0 == rmiPort?Integer.parseInt((String)input.get("defalutPort")):rmiPort;
        JVMMBeanManager.removeCache(host, rmiPort);
        return true;
    }


}
