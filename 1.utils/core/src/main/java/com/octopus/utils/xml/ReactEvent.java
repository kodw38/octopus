package com.octopus.utils.xml;/**
 * Created by admin on 2020/6/1.
 */

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cls.ClassUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * @ClassName ReactEvent
 * @Description ToDo
 * @Author Kod Wong
 * @Date 2020/6/1 9:57
 * @Version 1.0
 **/
public class ReactEvent implements Runnable{
    XMLObject o;
    String reactObjectName;
    String reactMethod;
    Object[] pars;
    Exception exp;
    Object ret;
    Map mapping;
    String xmlid;
    public ReactEvent(XMLObject o ,String reactObjectName,String reactMethod,String xmlid,Object[] reactInputParams,Exception exception,Object ret,Map<String,List> mapping){
        this.o = o;
        this.reactObjectName=reactObjectName;
        this.reactMethod=reactMethod;
        this.pars=reactInputParams;
        this.exp=exception;
        this.ret=ret;
        this.mapping=mapping;
        this.xmlid=xmlid;
    }

    @Override
    public void run() {
        if(StringUtils.isBlank(reactObjectName)){
            if(null !=mapping) {

                    String key = (reactObjectName==null ?"":reactObjectName)+"#"+reactMethod;
                    List<String> ls = (List)mapping.get(key);
                    if(null != ls) {
                        for(String s:ls) {
                            String[] ps = s.split("#");//0 objectname,1 method
                            XMLObject t = o.getObjectById(ps[0]);
                            if(null != t) {
                                try {
                                    ClassUtils.invokeMethod(t,ps[1],new Class[]{String.class,Object[].class,Exception.class,Object.class},new Object[]{xmlid,pars,exp,ret});
                                }catch (Exception e){
                                    e.printStackTrace();
                                }
                            }
                        }
                    }


            }
        }
    }
}
