package com.octopus.utils.xml.auto.defpro;

import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.Map;

/**
 * User: Administrator
 * Date: 15-1-12
 * Time: 上午9:48
 */
public interface IExeProperty {

    //返回true,在XMODoObject对象中将不再调用doThing方法。
    public Object
    exeProperty(Map proValue,XMLDoObject obj,XMLMakeup xml,XMLParameter parameter,Map input,Map output,Map config) throws Exception;

    boolean isAsyn();

}
