package com.octopus.utils.cls.jcl.resource;

import com.octopus.utils.cls.jcl.resource.impl.FileGetURl;
import com.octopus.utils.cls.jcl.resource.impl.FtpGetURl;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class ResourceLoader {	
	static String[] jartypes = {"jar","zip","war","ear"};
	
	Map<String,List<URL>> loadPathMap = Collections.synchronizedMap(new HashMap<String,List<URL>>());
	List<URLFileInfo> fileList = Collections.synchronizedList(new ArrayList<URLFileInfo>());

	Map<String,byte[]> resEntryContents = Collections.synchronizedMap(new HashMap<String,byte[]>());
	Map<String,List<String>> fileresourceMap = Collections.synchronizedMap(new HashMap<String,List<String>>());
	Map<String,URL> resourcefileMap = Collections.synchronizedMap(new HashMap<String,URL>());
	
	List<IGetURL> getURLList = new ArrayList<IGetURL>();
	List<IChangeListener> changeListeners = new ArrayList<IChangeListener>();
	private String[] excludeFiles= null;
	public ResourceLoader(){
		this.addGetURL(new FileGetURl());
		this.addGetURL(new FtpGetURl());
	}
	public static boolean isJar(String file){
		if(null != file){
			String subfix = file.substring(file.lastIndexOf(".")+1);
			for(String type:jartypes){
				if(type.equals(subfix))
					return true;
			}
		}
		return false;
	}
	public static long getJudge(URL u){
    	InputStream c=null;
    	long l=0;
    	try{
    		l = u.openConnection().getContentLength();	    		
    	}catch(Exception e){
    		e.printStackTrace();
    	}finally{
    		try{
			if(null != c)
				c.close();
    		}catch(Exception e){}
		}
		return l;
    }
	
	public void load(String file,boolean isOverload){
		if(null != file){
			try{
				URL u = new File(file).toURL();
				load(u,isOverload,null);				
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	public URL[] getURLs(URL url){
		URL[] ret=null;
		for(IGetURL g: getURLList){
			ret = g.getURLs(url, loadPathMap,fileList);			
			if(null != ret)
				break;
		}		
		Collections.sort(fileList);
		return (URL[])sortURL(ret).toArray(new URL[0]);
	}
	private LinkedList sortURL(Object[] us){
		LinkedList li = new LinkedList();
		if(null != us){
			String t1=null ,t2=null;
			boolean isadd = false;
			for(int i=0;i<us.length;i++){
				isadd = false;
				t1 = us[i].toString();
				for(int j=0;j<li.size();j++){
					if(li.get(j).toString().compareTo(t1)>0){
						li.add(j, us[i]);
						isadd = true;
						break;
					}
				}
				if(!isadd)
					li.add(us[i]);
			}
		}
		return li;
	}
	
	public void load(URL url,boolean isOverload,String[] excludefilenames){
		try{			
			if(getURLList.size()>0){				
				URL[] ret = getURLs(url);
				if(null != ret){				
					System.out.println("************* load url order ,length is "+ret.length+"*************");
					if(null != excludefilenames && excludefilenames.length>0){
						excludeFiles = excludefilenames;
					}
					for(URL r:ret){
						if(null != excludeFiles){
							boolean bak = false;
							for(String s:excludeFiles){								
								if(r.getFile().indexOf(s)>0){
									bak = true;
									break;
								}
							}
							if(bak)
								continue;
						}
						System.out.println("** "+r.toString());
						InputStream in=null;
						try{		
							in = r.openStream();
							loadFile(r,in,isOverload);
						}catch(Exception e){
							e.printStackTrace();
						}finally{							
							if(null != in) in.close();
						}
					}
					System.out.println("************* load url end *************");
					
				}							
				
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	void loadFile(URL url,InputStream in,boolean isOverload){		
		BufferedInputStream bis = null;
        JarInputStream jis = null;
        InputStream fis=null;
		try {
			if(isJar(url.getFile())){
	            bis = new BufferedInputStream( in );
	            jis = new JarInputStream( bis );	           
	            JarEntry jarEntry = null;
	            boolean isin=false;
	            while (( jarEntry = jis.getNextJarEntry() ) != null) {	                
	                if (jarEntry.isDirectory()) {
	                    continue;
	                }
	                if(isJar(jarEntry.getName())){
	                	loadFile(url,jis,isOverload);
	                }else{
		                byte[] b = new byte[1024];		                
		                ByteArrayOutputStream out = new ByteArrayOutputStream();
		                //out.write(b);
		                int len = 0;
		                while (( len = jis.read( b ) ) > 0) {
		                    out.write( b, 0, len );
		                }
		                isin =  resEntryContents.containsKey(jarEntry.getName());
		                if(!isin || (isin && isOverload)){
		                	addConfig(url,jarEntry.getName(),out.toByteArray());
		                	if(isOverload)
		                		doChange(jarEntry.getName());
		                }
		                out.close();
	                }	                
	            }
			}else{				
		        fis = url.openStream();
		        byte[] content = new byte[(int) fis.available()];
		        fis.read( content );
		        fis.close();
		        String entryName= url.getFile();
		        boolean isin =  resEntryContents.containsKey(entryName);
		        if(!isin || (isin && isOverload)){
		        	addConfig(url,entryName,content);
		        	if(isOverload)
		        		doChange(entryName);    	
		        }		        	
			}
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }finally{
        	try{        	
        	if(null != fis) fis.close();
        	}catch(Exception e){}
        }

	}    
	public URL[] getURLs(){
		List<URL> ret = new ArrayList<URL>();
		Iterator its = resourcefileMap.values().iterator();
		while(its.hasNext()){
			Object o = its.next();
			if(!ret.contains(o)) ret.add((URL)o);
		}
		return ret.toArray(new URL[0]);
	}
        
	void addConfig(URL url,String name,byte[] bs){
		resEntryContents.put( name, bs );		
    	resourcefileMap.put(name, url);   
    	String urlstr = url.toString();
    	if(!fileresourceMap.containsKey(urlstr))
    		fileresourceMap.put(urlstr,new ArrayList<String>());
    	if(!fileresourceMap.get(urlstr).contains(name))
    		fileresourceMap.get(urlstr).add(name);
	}
	void doChange(String name){
		doChange(resourcefileMap.get(name),name);
	}
	void doChange(URL url,String name){
		for(IChangeListener l:changeListeners){
			l.chg(url, name);
		}
	}
	
	public void addChangeListener(IChangeListener listener){
		if(null != listener)
			changeListeners.add(listener);
	}
	
	public void addGetURL(IGetURL getUrl){
		if(null != getUrl)
			getURLList.add(getUrl);
	}
	
	public Map<String,byte[]> getResources(){
		return resEntryContents;
	}
	
	public URL getResourceURL(String name){
		URL u =  resourcefileMap.get(name);
		if(null != u){
			try {
				return new URL(u.toString()+"!"+name);
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}
	public byte[] getResource(String name){
//		System.out.println("load class "+name+" is in jar :"+this.resourcefileMap.get(name));
		return resEntryContents.get(name);
	}
	
	public List<URLFileInfo> getFileURLs(){
		return fileList;
	}
	
	public Map<String,List<URL>> getLoadPath(){
		return loadPathMap;
	}
	
	public void unLoad(String path){
		try{
			if(resEntryContents.containsKey(path)){
				removeConfig(path);
			}else{
				URL url = new File(path).toURL();
				unLoad(url);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	void removeFile(URL u){
		String urlstr=u.toString();
		if(fileresourceMap.containsKey(urlstr)){
			fileresourceMap.remove(urlstr);
			Iterator<String> its = resourcefileMap.keySet().iterator();
			while(its.hasNext()){
				String s = its.next();
				if(resourcefileMap.get(s).equals(u)){
					resourcefileMap.remove(s);
					resEntryContents.remove(s);
				}
			}			
		}
	}
	
	void removeConfig(String path){
		resEntryContents.remove(path);
		if(resourcefileMap.containsKey(path)){
			URL url = resourcefileMap.get(path);
			resourcefileMap.remove(path);
			List li= fileresourceMap.get(url.toString());
			if(null != li && li.contains(path)){
				li.remove(path);
				if(li.size()==0){
					for(URLFileInfo info:fileList){
						if(info.getUrl().equals(url))
							fileList.remove(url);									
					}
					if(loadPathMap.containsKey(url.toString())){
						loadPathMap.remove(url);
					}else{
						Iterator<String> its = loadPathMap.keySet().iterator();
						while(its.hasNext()){
							String u = its.next();
							if(((List)loadPathMap.get(u)).contains(url)){
								((List)loadPathMap.get(u)).remove(url);
								if(((List)loadPathMap.get(u)).size()==0){
									loadPathMap.remove(u);
								}
							}
						}
					}
				}
			}
		}
	}
	
	public void unLoad(URL url){
		for(IGetURL g: getURLList){
			URL[] ret = g.removeURLs(url, loadPathMap,fileList);
			if(null != ret){
				for(URL r:ret){
					removeFile(r);
				}
				break;
			}							
		}
	}
}
