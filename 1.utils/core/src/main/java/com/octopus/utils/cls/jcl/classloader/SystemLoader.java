package com.octopus.utils.cls.jcl.classloader;

import java.io.InputStream;


public class SystemLoader extends ClassLoader implements ILoader {
    private long order = 1;
    private boolean isEnable =true;
    private boolean systemFirst = true;
    
    protected int min_system_order=1;
    protected int max_system_order=999999991;
    
    public void setSystemFirst(boolean b){
    	systemFirst = b;
    }
    public SystemLoader() {
    	if(systemFirst){
    		order = min_system_order;      
    	}else{
    		order = max_system_order;
    	}
    }
    
    public Class load(String className, boolean resolveIt) {
        Class result;
        try {
            result = findSystemClass( className );
        } catch (ClassNotFoundException e) {
            return null;
        }
        return result;
    }
    
    public InputStream loadResource(String name) {
        InputStream is = getSystemResourceAsStream( name );
        if( is != null ) {
            return is;
        }
        return null;
    }

	public long getOrder() {
		
		return order;
	}

	public boolean isEnabled() {
		return isEnable;
	}

	public void setEnabled(boolean enabled) {
		isEnable = enabled;
		
	}

	public void setOrder(long order) {
		this.order = order;
	}

	public int compareTo(ILoader o) {
		return (int)(order - o.getOrder());
	}
	
}
