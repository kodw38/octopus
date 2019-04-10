package com.octopus.utils.cls.jcl;

import java.net.URL;

public class TestMulti {

	public static void main(String[] args){
		try{
			MultiClassLoader.setRecycle(false);
			MultiClassLoader.setResMonitorInterval(10000);
			MultiClassLoader cl = new MultiClassLoader();			
			cl.addResource(new URL("file:/D:/poc/"),new String[]{"2.axis2.jar","log4j","commons-logging.jar"});
			//Object o = cl.loadClass("com.test.TestS").newInstance();
			//o.getClass().getMethod("getS", null).invoke(o, null);
			/*Thread.sleep(60000);
			o = cl.loadClass("com.test.TestS").newInstance();
			o.getClass().getMethod("getS", null).invoke(o, null);*/
			Class c = cl.loadClass("com.octopus.crm.lint.out.Bean.BusiAvailablyOfferQueryReq");
			System.out.println(c.getName());
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
