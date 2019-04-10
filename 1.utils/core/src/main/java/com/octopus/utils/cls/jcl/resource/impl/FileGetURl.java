package com.octopus.utils.cls.jcl.resource.impl;

import com.octopus.utils.cls.jcl.resource.IGetURL;
import com.octopus.utils.cls.jcl.resource.ResourceLoader;
import com.octopus.utils.cls.jcl.resource.URLFileInfo;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileGetURl implements IGetURL{

	URL[] deal(URL url, Map<String, List<URL>> path, List<URLFileInfo> list,boolean isadd){
		List<URL> ret = new ArrayList<URL>();
		try{			
			if(url.getProtocol().equals("file")){
				File f = new File(url.getFile());
				if(f.isDirectory()){
	        		File[] fs = f.listFiles();
	        		for(File ff:fs){
	        			URL[] us = deal(ff.toURL(),path,list,isadd);
	        			if(null != us && us.length>0){
	        				for(URL u:us){
	        					ret.add(u);
	        				}
	        			}
	        		}
	        	}else if(ResourceLoader.isJar(f.getName())){        		        		
					ret.add(f.toURL());
	        	}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		if(ret.size()>0){
			URL[] us =  (URL[])ret.toArray(new URL[0]);
			if(isadd && !path.containsKey(url.toString())){
				path.put(url.toString(), new ArrayList());
			}			
			for(URL u:us){
				try{
					if(isadd){
						boolean isin = false;
						for(URLFileInfo info :list){
							if(info.getUrl().equals(u)){
								isin = true;
								break;
							}
						}
						if(!isin)
							list.add(new URLFileInfo(u,ResourceLoader.getJudge(u)));
						if(!path.get(url.toString()).contains(u))
							path.get(url.toString()).add(u);
					}else{
						for(URLFileInfo info :list){
							if(info.getUrl().equals(u))
								list.remove(u);
						}
						path.get(url.toString()).remove(u);
						if((path.get(url.toString())).size()==0)
							path.remove(url.toString());
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}
			return us;
		}else{
			return null;
		}
	}
	
	public URL[] getURLs(URL url, Map<String, List<URL>> path, List<URLFileInfo> list) {
		return deal(url,path,list,true);
	}

	public URL[] removeURLs(URL url, Map<String, List<URL>> path, List<URLFileInfo> list) {
		return deal(url,path,list,false);	
	}

}
