package com.octopus.utils.cls.jcl.resource.impl;

import com.octopus.utils.cls.jcl.resource.IGetURL;
import com.octopus.utils.cls.jcl.resource.ResourceLoader;
import com.octopus.utils.cls.jcl.resource.URLFileInfo;
import com.octopus.utils.net.ftp.FTPClient;
import sun.net.TelnetInputStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class FtpGetURl implements IGetURL {

	public URL getURL(String ip,String userName,String pwd,String filepath) throws MalformedURLException{
		//ftp://<userName>:<pwd>@<ip>/fileURL;type=<FTPBIN>		
		StringBuffer sb = new StringBuffer("ftp://");
		if(null != userName && null != pwd)
			sb.append(userName).append(":").append(pwd);
		if(null != ip)
			sb.append("@").append(ip);
		sb.append("/").append(filepath);
		//sb.append(";").append("type=BIN");	
		URL url = new URL(sb.toString());			
		return url;
	}
	
	URL[] getURLs(String ip,String userName,String pwd,String filepath) throws IOException{		
		FTPClient client = new FTPClient();
        client.connect(ip,21);
		client.login(userName, pwd);
		try{
		if(null != filepath){
			client.changeWorkingDirectory(filepath);
		}	
		}catch(Exception e){
			return new URL[]{getURL(ip,userName,pwd,filepath)};
		}
		List ret = new ArrayList();
		String[] res = findZipJar(client,null);
		if(null != res)
		for(String r:res){
			if(null != filepath)
				ret.add(this.getURL(ip, userName, pwd, filepath+"/"+r));
			    
			else
				ret.add(this.getURL(ip, userName, pwd, r));
		}
		return (URL[])ret.toArray(new URL[0]);
	}

	
	private String[] findZipJar(FTPClient client,String parent) throws IOException{
		TelnetInputStream stream = null;//todo client.listFiles();
		InputStreamReader read = new InputStreamReader(stream);// "utf-8"
        BufferedReader in = new BufferedReader(read);
        String line;       
        List ret = new ArrayList();
        String t ;
        while ((line = in.readLine()) != null) {
        	try{        		
        		client.changeWorkingDirectory(line);
        		if(null != parent)
        			parent +="/"+line;
        		else
        			parent = line;
        		String[] subs = findZipJar(client,parent);
        		if(null != subs){
        			for(String s:subs)
        				ret.add(s);
        		}
        		int i = parent.lastIndexOf("/");
        		if(i>0)
        			parent = parent.substring(0,i);
        		else
        			parent = null;
        		client.changeWorkingDirectory("..");
        	}catch(Exception e){
        		t = line.toLowerCase();
        		if(ResourceLoader.isJar(t)){
	        		if(null != parent)
	        			ret.add(parent+"/"+line);
	        		else
	        			ret.add(line);
        		}
        	}
        }
        if(ret.size()>0){
        	return (String[])ret.toArray(new String[0]);
        }
        return null;
	}
	
	URL[] deal(URL url, Map<String, List<URL>> path, List<URLFileInfo> list, boolean b) {
		URL[] ret=null;
		String ip = url.getHost();
		String[] author = url.getUserInfo().split(":");		
		String p = url.getPath();
		if(p.charAt(0)=='/')p=p.substring(1);
		if(null != ip && !"".equals(ip) && null != author && author.length==2){
			try{			
				ret = this.getURLs(ip, author[0], author[1], p);
				if(b){
					List li = new ArrayList();
					path.put(url.toString(),li);
					for(URL u :ret){
						li.add(u);
						list.add(new URLFileInfo(u,ResourceLoader.getJudge(u)));
					}
				}else{
					String urlstr = url.toString();
					if(path.containsKey(urlstr)){
						List<URL> us = path.get(urlstr);
						path.remove(urlstr);
						for(URL u:us){
							for(URLFileInfo info:list){
								if(info.getUrl().equals(u)){
									list.remove(u);
								}
							}
						}
					}else{
						
						Iterator<String> its = path.keySet().iterator();
						while(its.hasNext()){
							List<URL> li = path.get(its.next());
							for(URL u :ret){
								if(li.contains(u)){
									li.remove(u);
								}
							}
						}
						for(URLFileInfo info:list){
							for(URL u :ret){
								if(info.getUrl().equals(u)){
									list.remove(u);
								}
							}
						}
						
					}
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		return ret;
	}
	
	public URL[] getURLs(URL url, Map<String, List<URL>> path, List<URLFileInfo> list) {
		if(url.getProtocol().equals("ftp")){	
			return deal(url,path,list,true);
		}
		return null;
	}

	

	public URL[] removeURLs(URL url, Map<String, List<URL>> path, List<URLFileInfo> list) {
		if(url.getProtocol().equals("ftp")){	
			return deal(url,path,list,false);
		}
		return null;
	}

}
