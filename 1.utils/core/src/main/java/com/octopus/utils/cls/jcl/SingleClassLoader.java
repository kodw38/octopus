
package com.octopus.utils.cls.jcl;

import com.octopus.utils.cls.jcl.classloader.CurrentLoader;
import com.octopus.utils.cls.jcl.classloader.ILoader;
import com.octopus.utils.cls.jcl.classloader.ParentLoader;
import com.octopus.utils.cls.jcl.classloader.SystemLoader;
import com.octopus.utils.cls.jcl.resource.DynamicResourceLoader;
import com.octopus.utils.cls.jcl.resource.IChangeListener;
import com.octopus.utils.cls.jcl.resource.ResourceLoader;
import com.octopus.utils.cls.jcl.resource.URLFileInfo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * if one outside url contain multiple jar, the classloader used same one resourceloader and multiple the classloader load save class.
 * if used no parameter construct function then please used initWhenNoParStruct function init the classloader.
 * the classloader loader file with filename-order each time.
 * if systemFirst parameter is true ,then first load resource from systemloader then load from the classloader,default value is true. 
 * @author wang feng
 * 
 */
public class SingleClassLoader extends ClassLoader implements ILoader {
	
	protected static final Map<String,ILoader> loaders = Collections.synchronizedMap(new HashMap<String,ILoader>());
    
	protected final Map<String, Class> classes=Collections.synchronizedMap( new HashMap<String, Class>() );
	protected ResourceLoader classpathResources;
	protected char classNameReplacementChar;
    // Default order
    protected static volatile long synorder = 100;
    protected long order = 100;    
    
    // Enabled by default
    protected boolean enabled = true;
    
    protected boolean systemFirst = true;
    
    protected void initOrder(){
    	synorder++;
    	order = synorder;
    }
    public void setSystemFirst(boolean b){
    	systemFirst = b;
    }
    public static void setResMonitorInterval(int seconds){
    	DynamicResourceLoader.setResMonitorInterval(seconds);
    }
    public static void setRecycle(boolean b){
    	DynamicResourceLoader.setRecycle(b);
    }
	
    public void initParentLoader(){   
    	
    	if(!loaders.containsKey("-^systemloader"))
    	loaders.put("-^9999",new SystemLoader() );
    	if(!loaders.containsKey("-^parentloader"))
        loaders.put("-^8888",new ParentLoader() );
    	if(!loaders.containsKey("-^currentloader"))
        loaders.put("-^7777",new CurrentLoader() );  
    }    
        
    public void removeLoader(URL key){
    	if(loaders.containsKey(key.toString())){
    		loaders.remove(key);
    	}
    }
    
    public Class loadClass(String className) throws ClassNotFoundException {
        return ( loadClass( className, true ) );
    }
    
    public Class loadClass(String className, boolean resolveIt) throws ClassNotFoundException {
    	List<ILoader> li = new ArrayList(loaders.values());
        Collections.sort( li );
        Class clazz = null;      
        
        //clazz = this.load( className, resolveIt );
        
        if(null == clazz){
	        for( ILoader l : li ) {	            
	        	if( l.isEnabled() ) {
	                clazz = l.load( className, resolveIt );
	                if( clazz != null ){
	                	System.out.println(className +" used classloader is "+l.toString());
	                    break;
	                }
	            }
	        }
        }
        if( clazz == null )
            throw new ClassNotFoundException( className );
        return clazz;
    }
    
    public InputStream getResourceAsStream(String name) {
    	List<ILoader> li = new ArrayList(loaders.values());
        Collections.sort( li );
        InputStream is = null;
        for( ILoader l : li ) {
            if( l.isEnabled() ) {
                is = l.loadResource( name );
                if( is != null )
                    break;
            }
        }
        return is;
    }
    
	
    class FileUpdateListener implements IChangeListener{
    	public void chg(URL url,String name) {
    		if(null != name){
	    		if(name.endsWith("class")){
					name = name.substring(0, name.lastIndexOf("."));
					name = name.replace('/','.');		
					if(classes.containsKey(name)){
						if(loaders.containsKey(url.toString())){
							new SingleClassLoader(url,this.getClass().getClassLoader().getParent());	
							removeLoader(url);
						}
					}
	    		}
	    	}else{
	    		if(null != url){
	    			new SingleClassLoader(url,this.getClass().getClassLoader().getParent());
	    		}
	    	}
		}  
    }

    void createLoaders(List<URLFileInfo> us){    	
    	if(null != us){
    		boolean is = true;
    		for(URLFileInfo u:us){
    			if(is){
    				loaders.put(u.getUrl().toString(), this);
    				is = false;
    			}else{    				
    				loaders.put(u.getUrl().toString(), new SingleClassLoader(this.classpathResources,u.getUrl()));
    			}
    		}
    	}
    	System.out.println("*********************current loader count are:"+loaders.size()+"***********************");
    	List<ILoader> li = new ArrayList(loaders.values());
        Collections.sort( li );
        for( ILoader l : li ) {
        	Iterator its= loaders.keySet().iterator();
        	while(its.hasNext()){
        		String key  =(String)its.next();
        		if(loaders.get(key).equals(l)){
        			System.out.println(key+"  "+l.getOrder());
        		}
        			
        	}
        }    	
    	System.out.println("***********************************************************************************");
    }
    SingleClassLoader(ResourceLoader resResources,URL url){
    	initOrder();
    	initParentLoader();
    	classpathResources = resResources;    	  	 
    }
    
    public SingleClassLoader(String file,ClassLoader parent) throws MalformedURLException{
    	super(parent);
    	initOrder();
    	initParentLoader();    	
    	classpathResources = new DynamicResourceLoader();
    	classpathResources.load(file, false);
    	List<URLFileInfo> us = classpathResources.getFileURLs();
    	createLoaders(us);
    	classpathResources.addChangeListener(new FileUpdateListener());
    }
    public SingleClassLoader(String[] files,ClassLoader parent) throws MalformedURLException{
        super(parent);
        initOrder();
        initParentLoader();
        classpathResources = new DynamicResourceLoader();
        for(String f:files) {
            classpathResources.load(f, false);
        }
        List<URLFileInfo> us = classpathResources.getFileURLs();
        createLoaders(us);
        classpathResources.addChangeListener(new FileUpdateListener());
    }
    public SingleClassLoader(){
    	initOrder();
    }
    public void initWhenNoParStruct(URL source){    	
    	initParentLoader();       	
    	classpathResources = new DynamicResourceLoader();
    	classpathResources.load(source, false,null);
    	List<URLFileInfo> us = classpathResources.getFileURLs();
    	createLoaders(us);
    	classpathResources.addChangeListener(new FileUpdateListener());
    }
    public SingleClassLoader(URL url,ClassLoader parent){
    	super(parent);
    	initOrder();
    	initParentLoader();
    	classpathResources = new DynamicResourceLoader();
    	classpathResources.load(url, false,null);
    	List<URLFileInfo> list= classpathResources.getFileURLs();
    	createLoaders(list);
    	classpathResources.addChangeListener(new FileUpdateListener());
    }
    URL getFtpURL(String ip,String userName,String pwd,String filepath) throws MalformedURLException{
    	StringBuffer sb = new StringBuffer("ftp://");
		if(null != userName && null != pwd)
			sb.append(userName).append(":").append(pwd);
		if(null != ip)
			sb.append("@").append(ip);
		sb.append("/").append(filepath);		
		URL url = new URL(sb.toString());			
		return url;
    }
    public SingleClassLoader(String ip, String userName,String  pwd, String filepath,ClassLoader parent) throws IOException{
    	super(parent);
    	initOrder();
    	initParentLoader();
    	classpathResources = new DynamicResourceLoader();
    	classpathResources.load(getFtpURL(ip,userName,pwd,filepath), false,null);
    	List<URLFileInfo> list= classpathResources.getFileURLs();
    	createLoaders(list);  
    	classpathResources.addChangeListener(new FileUpdateListener());
    }
        
    
    public long getOrder() {
        return order;
    }

    /**
     * Set loading order
     * 
     * @param order
     */
    public void setOrder(long order) {
        this.order = order;
    }

    public void setClassNameReplacementChar(char replacement) {
        classNameReplacementChar = replacement;
    }

    /**
     * @return char
     */
    public char getClassNameReplacementChar() {    	
    	return classNameReplacementChar;    	
    }

    protected String formatClassName(String className) {
        if( getClassNameReplacementChar() == '\u0000' ) {
            // '/' is used to map the package to the path
            return className.replace( '.', '/' ) + ".class";
        } else {
            // Replace '.' with custom char, such as '_'
            return className.replace( '.', getClassNameReplacementChar() ) + ".class";
        }
    }
    
    protected byte[] loadClassBytes(String className) {
        className = formatClassName( className );
        return classpathResources.getResource( className );
    }
    
    Class generatorClass(String className,boolean resolveIt){
    	Class result=null;
    	try{
	    	byte[] classBytes;        
	
	        classBytes = loadClassBytes( className );
	        if( classBytes == null ) {
	            return null;
	        }
	
	        result = defineClass( className, classBytes, 0, classBytes.length );
	
	        if( result == null ) {
	            return null;
	        }
	
	        if( resolveIt )
	            resolveClass( result );
    	}catch(Throwable e){
    		e.printStackTrace();
    	}
        return result;
    }
    /**
     * Loads the class
     * 
     * @param className
     * @param resolveIt
     * @return class
     */
    public Class load(String className, boolean resolveIt){
    	//System.out.println("load class "+className);
    	Class result = classes.get( className );
        if( result != null ) {                
            return result;
        }else{
        	result = generatorClass(className,resolveIt);
        }
        if(null != result)
        	classes.put( className, result );   
        //System.out.println("success load class "+className);
        return result;
    }

    /**
     * Loads the resource
     * 
     * @param name
     * @return java.io.InputStream
     */
    public InputStream loadResource(String name){
    	byte[] arr = classpathResources.getResource( name );
        if( arr != null ) {
            return new ByteArrayInputStream( arr );
        }

        return null;
    }

    /**
     * Checks if Loader is Enabled/Disabled
     * 
     * @return boolean
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enable/Disable Laoder
     * 
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(ILoader o) {
        return (int)(order - o.getOrder());
    }
}

