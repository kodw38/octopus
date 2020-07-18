package com.octopus.tools.utils;/**
 * Created by admin on 2020/7/4.
 */

import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import com.octopus.utils.xml.auto.pointparses.PointParseGetErrorTrace;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @ClassName NumberUtils
 * @Description ToDo
 * @Author Kod Wong
 * @Date 2020/7/4 21:36
 * @Version 1.0
 **/
public class NumberUtils extends XMLDoObject {

    private AtomicInteger num =new AtomicInteger(0);
    int max=0;
    public NumberUtils(XMLMakeup xml, XMLObject parent, Object[] containers) throws Exception {
        super(xml, parent, containers);
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(null != config){
            if(max==0){
                max = (Integer)config.get("max");
            }
        }
        if(null != input){
            String n = (String)input.get("op");
            if("isFull".equalsIgnoreCase(n)){
                int c = num.incrementAndGet();
                if(c==max) {
                    num.set(0);
                    return true;
                }
            }else {
                num.incrementAndGet();
            }
        }
        return false;
    }

    @Override
    public ResultCheck checkReturn(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return new ResultCheck(true,ret);
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret, Exception e) throws Exception {
        return false;
    }
}
