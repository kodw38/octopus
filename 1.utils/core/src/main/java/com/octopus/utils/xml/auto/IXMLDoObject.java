package com.octopus.utils.xml.auto;

import com.octopus.utils.xml.XMLMakeup;

import java.util.Map;

/**
 * User: Administrator
 * Date: 14-9-4
 * Time: 上午10:13
 */
public interface IXMLDoObject {
    public void doInitial()throws Exception;
    //调用入口,如果没有指定input,config,output格式，且存在说明文档，则根据说明文档从环境数据中获取调用input,config,output参数，并返回说明文档的返回结构
    public void doThing(XMLParameter parameter,XMLMakeup xml) throws Exception;
    //检查输入,检查不通过就不执行doSomeThing,返回null
    public boolean checkInput(String xmlid,XMLParameter env,Map input,Map output,Map config)throws Exception;
    //具体实现
    public Object doSomeThing(String xmlid,XMLParameter env,Map input,Map output,Map config) throws Exception;
    //检查输出,可能是检查，可能是数据过滤
    public ResultCheck checkReturn(String xmlid,XMLParameter env,Map input,Map output,Map config,Object ret)throws Exception;
    //it is main use for trade
    public boolean commit(String xmlid,XMLParameter env,Map input,Map output,Map config,Object ret)throws Exception;

    //it is main use for trade
    public boolean rollback(String xmlid,XMLParameter env,Map input,Map output,Map config,Object ret,Exception e)throws Exception;
    /**
     * 对doSomeThing的参数做说明，及说明该xmlDoObject如何使用
     * @return
     */

    //在doThing->doSomeThing之后，如果返回ResultCheck.isSuccess是true,执行孩子
    public void doChildren(XMLParameter parameter,XMLMakeup xml,String autoType,Map config) throws Exception;

}
