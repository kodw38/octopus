package com.octopus.utils.cls.jcl;

import com.octopus.utils.cls.jcl.classloader.*;
import com.octopus.utils.cls.jcl.resource.DynamicResourceLoader;
import com.octopus.utils.cls.jcl.resource.IChangeListener;
import com.octopus.utils.cls.jcl.resource.ResourceLoader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * one outside url contain multiple jar, used one the classloader and one resourceloader.
 * first loader the classloader resource when loader resource each time,if not find then from current classloader ,parent classloader ,system classloader.
 * the classloader loader file with filename-order each time.
 * if systemFirst parameter is true ,then first load resource from systemloader then load from the classloader,default value is true.
 * @author wangfeng
 *
 */
public class MultiClassLoader extends ClassLoader implements ILoader{

	//class cache
	protected final Map<String, Class> classes=Collections.synchronizedMap( new HashMap<String, Class>() );
	
	private HashMap packages = new HashMap();
	
	//resource loader
	protected ResourceLoader classpathResources;
	
	protected char classNameReplacementChar;
	protected boolean enabled = true;
	protected long order = 0;
	protected boolean systemFirst = true;
	
	SystemLoader systemLoader = new SystemLoader();	
	ParentLoader parentLoader = new ParentLoader();
	CurrentLoader currentLoader = new CurrentLoader();
	
	JDKLoader jdkLoader = new JDKLoader();
	
	public MultiClassLoader(){
		classpathResources = new DynamicResourceLoader();
		classpathResources.addChangeListener(new FileUpdateListener());
	}
	public MultiClassLoader(URL url){
		classpathResources = new DynamicResourceLoader();
		classpathResources.addChangeListener(new FileUpdateListener());
		addResource(url);
	}
	
	public void addResource(URL jarurl){
		classpathResources.load(jarurl, false,null);
	}
	public void addResource(URL jarsurl,String[] excludejarfiles){
		classpathResources.load(jarsurl, false,excludejarfiles);
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
    
	public long getOrder() {		
		return order;
	}

	public boolean isEnabled() {		
		return enabled;
	}
	public void setEnabled(boolean enabled) {		
		this.enabled = enabled;
	}

	public void setOrder(long order) {
		this.order = order;
		
	}

	public int compareTo(ILoader o) {
		return (int)(order - o.getOrder());
	}

	public Class loadClass(String className) throws ClassNotFoundException {
        return ( loadClass( className, true ) );
    }
	
	public URL[] getURLs(){
		return classpathResources.getURLs();
	}
	
	public Class loadClass(String className, boolean resolveIt) throws ClassNotFoundException {
		Class clazz=null;
		clazz = jdkLoader.load(className,resolveIt);		
		if(null == clazz){			
			clazz = this.load( className, resolveIt );
		}
        if(null == clazz){
        	clazz = currentLoader.load(className,resolveIt);
        }
        if(null == clazz){
        	clazz = parentLoader.load(className,resolveIt);
        }
        if(null == clazz){
        	clazz = systemLoader.load(className,resolveIt);
        }
        if( clazz == null )
            throw new ClassNotFoundException( className );
        return clazz;
    }
	
	public char getClassNameReplacementChar() {    	
    	return classNameReplacementChar;    	
    }
	public void setClassNameReplacementChar(char replacement) {
        classNameReplacementChar = replacement;
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
        return  classpathResources.getResource( className );        
    }
	
	public URL getResource(String name){
		URL is = null;
        is = jdkLoader.getResource(name);
        if(null == is)
        	is = classpathResources.getResourceURL(name);        
        if(null == is)
        	is = currentLoader.getResource(name);
        if(null == is)
        	is = parentLoader.getResource(name);
        if(null == is)
        	is = systemLoader.getResource(name);
        return is;
	}
	
	public InputStream getResourceAsStream(String name) {    	
        InputStream is = null;
        is = jdkLoader.loadResource(name);
        if(null == is)
        	is = loadResource( name );        
        if(null == is)
        	is = currentLoader.loadResource(name);
        if(null == is)
        	is = parentLoader.loadResource(name);
        if(null == is)
        	is = systemLoader.loadResource(name);
        return is;
    }
	
	class FileUpdateListener implements IChangeListener{
		public void chg(URL url,String name) {
    		if(null != name){
	    		if(name.endsWith("class")){
					name = name.substring(0, name.lastIndexOf("."));
					name = name.replace('/','.');		
					if(classes.containsKey(name)){
						classes.remove(name);						
					}
	    		}
	    	}
		}
	}
	
	public Class load(String className, boolean resolveIt) {				
		Class result = classes.get( className );
        if( result != null ) {                
            return result;
        }else{        	
        	result = generatorClass(className,resolveIt);
        }
        if(null != result)
        	classes.put( className, result );   
        return result;
	}

	public InputStream loadResource(String name) {		
		byte[] arr = classpathResources.getResource( name );
        if( arr != null ) {
            return new ByteArrayInputStream( arr );
        }

        return null;
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

}
