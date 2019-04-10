package com.octopus.isp.actions;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by kod on 2017/5/5.
 */
public class SIAction extends XMLDoObject {
    Map cacheSI = new HashMap();
    public SIAction(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
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
        if(null != input){
            String op = (String)input.get("op");
            if(StringUtils.isNotBlank(op)){
                if("getSI".equals(op)){
                    return cacheSI;
                }else if("getPID".equals(op) && StringUtils.isNotBlank(input.get("pid"))){
                    return null;//SIUtil.getPidSig(Long.valueOf((String)input.get("pid")));
                }
            }else {
                if (StringUtils.isNotBlank(input.get("pid"))) {
                    if (input.get("pid") instanceof String) {
                        cacheSI = null;//SIUtil.getPidSig(Long.valueOf((String) input.get("pid")));
                        return cacheSI;
                    } else {
                        cacheSI = null;//SIUtil.getPidSig((Long) input.get("pid"));
                        return cacheSI;
                    }
                }
            }
        }
        cacheSI=null; //SIUtil.getSigInfo();
        /*List ps = (List)cacheSI.get("PIDS");
        Map tem = new HashMap();
        if(null != ps){
            for(Object o:ps){
                tem.put(o,SIUtil.getPidSig((Long)o));
            }
        }
        HashMap m = new HashMap();
        m.put("INFO",cacheSI);
        m.put("PIDS",tem);*/
        return cacheSI;
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
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config,Object ret,Exception e) throws Exception {
        return false;
    }
}
