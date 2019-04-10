package com.octopus.utils.cls.jcl.classloader;

import java.io.InputStream;

/**
 * @author Kamran Zafar
 * 
 */
public interface ILoader extends Comparable<ILoader> {
	
	public void setSystemFirst(boolean b);

    public long getOrder() ;

    /**
     * Set loading order
     * 
     * @param order
     */
    public void setOrder(long order);
    /**
     * Loads the class
     * 
     * @param className
     * @param resolveIt
     * @return class
     */
    public Class load(String className, boolean resolveIt);

    /**
     * Loads the resource
     * 
     * @param name
     * @return java.io.InputStream
     */
    public InputStream loadResource(String name);

    /**
     * Checks if Loader is Enabled/Disabled
     * 
     * @return boolean
     */
    public boolean isEnabled();

    /**
     * Enable/Disable Laoder
     * 
     * @param enabled
     */
    public void setEnabled(boolean enabled);

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
}
