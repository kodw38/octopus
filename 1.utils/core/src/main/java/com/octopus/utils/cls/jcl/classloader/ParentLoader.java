package com.octopus.utils.cls.jcl.classloader;

import java.io.InputStream;


public class ParentLoader extends ClassLoader implements ILoader {
    private long order;
    private boolean isEnable=true;
    private boolean systemFirst = true;
    
    protected int min_parent_order=2;
    protected int max_parent_order=999999992;
    
    public void setSystemFirst(boolean b){
    	systemFirst = b;
    }
    
    public ParentLoader() {
    	if(systemFirst){
    		order = min_parent_order;      
    	}else{
    		order = max_parent_order;
    	}
    }
   
    public Class load(String className, boolean resolveIt) {
        Class result;
        try {
        	
            result = getParent().loadClass( className );
        } catch (Exception e) {
            return null;
        }
        return result;
    }

    public InputStream loadResource(String name) {
        InputStream is = getParent().getResourceAsStream( name );

        if( is != null ) {
            return is;
        }
        return null;
    }

	public long getOrder() {
		
		return order;
	}

	public boolean isEnabled() {
		// TODO Auto-generated method stub
		return isEnable;
	}

	public void setEnabled(boolean enabled) {
		// TODO Auto-generated method stub
		isEnable = enabled;
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