package com.octopus.utils.cls.jcl;

import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;

/**
 * Created by Administrator on 2018/2/26.
 */
public class MyURLClassLoader extends URLClassLoader {

    public MyURLClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public MyURLClassLoader(URL[] urls) {
        super(urls);
    }

    public MyURLClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }

    public Class loadClass(String name) throws ClassNotFoundException {
        try {
            return super.loadClass(name);
        }catch (Exception e){
            return super.loadClass(name);
        }
    }
}
