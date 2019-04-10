package com.octopus.utils.cls.jcl.classloader;

import java.io.InputStream;


public class CurrentLoader extends ClassLoader implements ILoader{
	long order;
    boolean isEnable=true;
    private boolean systemFirst = true;
    
    protected int min_current_order=3;
    protected int max_current_order=999999993;
    
    public void setSystemFirst(boolean b){
    	systemFirst = b;
    }
    
    public CurrentLoader() {
    	if(systemFirst){
    		order = min_current_order;      
    	}else{
    		order =max_current_order;
    	}
    }

    
    public Class load(String className, boolean resolveIt) {
        Class result;
        try {
            result = getClass().getClassLoader().loadClass( className );
        } catch (ClassNotFoundException e) {
            return null;
        }
        return result;
    }

    public InputStream loadResource(String name) {
        InputStream is = getClass().getClassLoader().getResourceAsStream( name );

        if( is != null ) {
            return is;
        }

        return null;
    }

	public long getOrder() {
		// TODO Auto-generated method stub
		return order;
	}

	public boolean isEnabled() {
		// TODO Auto-generated method stub
		return isEnable;
	}

	public void setEnabled(boolean enabled) {
		// TODO Auto-generated method stub
		isEnable=enabled;
	}

	public void setOrder(long order) {
		// TODO Auto-generated method stub
		this.order=order;
	}

	public int compareTo(ILoader o) {
		// TODO Auto-generated method stub
		return (int)(order-o.getOrder());
	}

}