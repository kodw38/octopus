package com.octopus.utils.cls.proxy;

import com.octopus.utils.cls.ClassUtils;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * User: Administrator
 * Date: 14-8-26
 * Time: 下午5:14
 */
public class ObjectProxy {
    private static Object nextUniqueNumberLock = new Object();
    private static long nextUniqueNumber = 0;
    private final static String proxyClassNamePrefix = "$Proxy";

    private static Map proxyClasses = Collections.synchronizedMap(new WeakHashMap());

    public static Object newProxyInstance(Class implClass,Method[] methods,IMethodAddition[] addition,Class[] constructorClass,Object[] pars,IProxyHandler handler) throws IllegalAccessException, InstantiationException {
        try {
            Class cl = null;
            if(!proxyClasses.containsKey(implClass))
                cl = getProxyClass(implClass.getClassLoader(), implClass,methods,constructorClass);
            else
                cl = (Class)proxyClasses.get(implClass);

            Object o = null;
            if(null == constructorClass){
                o = cl.newInstance();
            }else{
                o = cl.getConstructor(constructorClass).newInstance(pars);
            }
            ClassUtils.setFieldValue(o,"additions",addition,false);
            ClassUtils.setFieldValue(o,"handler",handler,false);
            return o;
        }  catch (NoSuchFieldException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (InstantiationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IllegalAccessException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (NoSuchMethodException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return implClass.newInstance();
    }

    public static boolean isProxyClass(Class<?> cl) {
        if (cl == null) {
            throw new NullPointerException();
        }
        return proxyClasses.containsValue(cl);
    }

    static Class<?> getProxyClass(ClassLoader loader,Class impl,Method[] ms,Class[] constructorClass)throws IllegalArgumentException{
        String proxyPkg = null;	// package to define proxy class in
        String name = impl.getName();
        int n = name.lastIndexOf('.');
        String pkg = ((n == -1) ? "" : name.substring(0, n + 1));
        if (proxyPkg == null) {
            proxyPkg = pkg;
        }
        if (proxyPkg == null) {
            proxyPkg = "";
        }

        long num;
        synchronized (nextUniqueNumberLock) {
            num = nextUniqueNumber++;
        }
        String proxyName = proxyPkg + proxyClassNamePrefix + num;

        try {
            Class proxyClass = GeneratorClass.generatorClass(impl,ms,proxyName,constructorClass);
            if(null != proxyClass){
                proxyClasses.put(impl,proxyClass);
                return proxyClass;
            }
        } catch (ClassFormatError e) {
            throw new IllegalArgumentException(e.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


}
