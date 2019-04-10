package com.octopus.utils.cls.jcl;

import com.octopus.utils.cls.jcl.resource.ResourceLoader;
import junit.framework.TestCase;

import java.io.InputStream;
import java.net.URL;



public class Test2 extends TestCase{
	public static void printResource(SingleClassLoader loader,String name) {
		try{
			InputStream f =  loader.getResourceAsStream(name);
			byte[] b = new byte[f.available()];
			f.read(b);
			System.out.println(new String(b));
		}catch(Exception e){e.printStackTrace();}
	}
	public static void invoke(SingleClassLoader loader,String name){
		try{
		Class t = Class.forName(name);
		Object o = t.newInstance();
		o.getClass().getMethod("test", null).invoke(o,null);
		}catch(Exception e){e.printStackTrace();}
	}
	
	public static void testFtp(){
		try{
			//SingleClassLoader loader = new SingleClassLoader("10.11.20.104","weblogic","weblogic","test");
			SingleClassLoader loader = new SingleClassLoader("C:\\test\\ws\\lib",Test2.class.getClassLoader());
			while(true){			
				System.out.println("-----------------------------");
				/*invoke(loader,"com.test.LoaderTest");
				printResource(loader,"readme.txt");
				
				printResource(loader,"com/test2/mm.txt");
				invoke(loader,"com.test2.Test");
				
				invoke(loader,"com.test2.Test2");*/
				Thread.currentThread().sleep(30000);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	public static void main(String[] args){
		try{
			//SingleClassLoader loader = new SingleClassLoader("C:\\test\\ejbinvoke\\dev");
			//loader.addFtpDirectory("10.11.20.104", "weblogic", "weblogic", "test");
			/*while(true){			
				System.out.println("-----------------------------");
				invoke(loader,"com.test.LoaderTest");
				printResource(loader,"readme.txt");
				
				printResource(loader,"com/test2/mm.txt");
				invoke(loader,"com.test2.Test");
				
				invoke(loader,"com.test2.Test2");
				Thread.currentThread().sleep(30000);
			}*/
			
			//testFtp();
			ResourceLoader r = new ResourceLoader();
			r.getURLs(new URL("file:/d:/poc"));
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
