package com.octopus.tools.utils;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-6-16
 * Time: 上午11:09
 */
public class StringJoin extends XMLDoObject{
    public StringJoin(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    @Override
    public Object doSomeThing(String xmlid,XMLParameter env, Map input,Map output, Map cfg) throws Exception {
        Object src = input.get("src");
        String each=null;
        env.removeParameter("${each}");
        try{
            if(null != input){
                each=(String)input.get("each");
            }
            StringBuffer sb = new StringBuffer();
            if(src.getClass().isArray()){
                HashMap map = new HashMap();
                for(int i=0;i<((Object[])src).length;i++){
                    Object o = ((Object[])src)[i];
                    if(o instanceof String){
                        if(StringUtils.isNotBlank(each)){
                            env.addParameter("${each}",o);
                            o = env.getExpressValueFromMap(each,this);
                        }
                        if(sb.length()==0){
                            sb.append(o);
                        }else{
                            sb.append(",").append(o);
                        }

                    }
                }
                return sb.toString();
            }else if(src instanceof Collection){
                Iterator its = ((Collection)src).iterator();
                while(its.hasNext()){
                    Object o = its.next();
                    if(o instanceof String){
                        if(sb.length()==0){
                            sb.append(o);
                        }else{
                            sb.append(",").append(o);
                        }

                    }
                }
                return sb.toString();
            }
            throw new Exception(" now only support Collection StringJoin");
        }finally {
            env.removeParameter("${each}");
        }
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid,XMLParameter env, Map input,Map output, Map jsonPar) throws Exception {
        if(null != input && null !=input.get("src")){
            return true;
        }
        return false;
    }

    @Override
    public ResultCheck checkReturn(String xmlid,XMLParameter env, Map input,Map output, Map cfg, Object ret) throws Exception {
        if(null != ret && ret instanceof String){
            return new ResultCheck(true,ret);
        }
        return new ResultCheck(false,null);
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config,Object ret,Exception e) throws Exception {
        throw new Exception("now support rollback");
    }
}
