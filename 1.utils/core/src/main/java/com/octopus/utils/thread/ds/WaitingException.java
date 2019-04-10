package com.octopus.utils.thread.ds;

import java.util.HashMap;

/**
 * User: Administrator
 * Date: 14-9-22
 * Time: 上午11:24
 */
public class WaitingException extends Exception{
    boolean isException=false;
    HashMap map = new HashMap();
    public boolean isException(){
       return isException;
    }

    public void addException(String key,String value){
        if(!isException) isException=true;
        map.put(key,value);
    }
}
