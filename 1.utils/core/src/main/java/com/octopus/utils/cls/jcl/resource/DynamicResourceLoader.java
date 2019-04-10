package com.octopus.utils.cls.jcl.resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.URL;
import java.util.Iterator;
import java.util.List;

public class DynamicResourceLoader extends ResourceLoader{
	private static Log log= LogFactory.getLog(DynamicResourceLoader.class);
	static int recseconds=60000;
	static boolean iscycle = false;
	public static void setResMonitorInterval(int seconds){
		recseconds= seconds;
	}
	public static void setRecycle(boolean b){
		iscycle = b;
	}
	public DynamicResourceLoader(){
		Thread t = new Thread(new Runnable(){
			public void run() {
				try{
					while(iscycle){									
						for(int i=fileList.size()-1;i>=0;i--){
							URLFileInfo info = fileList.get(i);
							long l = getJudge(info.getUrl());
							if(0!=l && info.getJudge()!=l){
								load(info.getUrl(),true,null);
								info.setJudge(l);								
							}
						}
						Iterator<String> its = loadPathMap.keySet().iterator();
						while(its.hasNext()){
							String k = its.next();
							List li = (List<URL>)loadPathMap.get(k);
							if(li.size()>1 || (li.size()==1 && !li.get(0).equals(k)) || li.size()==0 ){
								URL[] us = getURLs(new URL(k));
								for(URL s:us){
									if(!fileresourceMap.containsKey(s.toString())){
										System.out.println("change jar:"+s.toString());
										doChange(s,null);
										log.debug("change resource:"+s.toString());
									}
								}
							}
							
						}
						Thread.currentThread().sleep(recseconds);
					}
				
				}catch(Exception e){
					e.printStackTrace();
				}
				
			}			
		},Thread.currentThread().getName()+"_monitor_resource_update");		
		t.setDaemon(true);
		t.start();
	}
}
