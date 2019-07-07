package com.octopus.isp.cell.actions.rule;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.rule.RuleUtil;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Iterator;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-4-22
 * Time: 下午6:09
 */
public class DefaultRule extends XMLDoObject {
    transient static Log log = LogFactory.getLog(DefaultRule.class);
    public DefaultRule(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    @Override
    public Object doSomeThing(String xmlid,XMLParameter env, Map input,Map output, Map cfg) throws Exception {
        try{
            Iterator<String> its = input.keySet().iterator();
            while(its.hasNext()){
                String k = its.next();
                Object v = input.get(k);
                if(v instanceof String && ((String) v).startsWith("clazz:")){
                    IRuleExecutor executor = (IRuleExecutor) Class.forName(((String) v).substring(6)).newInstance();
                    executor.setEnv(input);
                    input.put(k,executor);

                }
            }
            Object obj= RuleUtil.doRule((String)input.get("rule"),input);
            if(log.isDebugEnabled())
                log.debug(input.get("rule")+"  "+obj+"\n"+input);
            return obj;

        }catch (Exception e){
            log.error("execute rule:",e);
        }
        return null;
    }


    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid,XMLParameter env, Map input,Map output, Map cfg) throws Exception {
        if(null != input && StringUtils.isNotBlank(input.get("rule"))){
            if(log.isDebugEnabled()){
                System.out.println(" the will executing rule is:"+input.get("rule"));
            }
            return true;
        }
        return false;
    }

    @Override
    public ResultCheck checkReturn(String xmlid,XMLParameter env, Map input,Map output, Map cfg, Object ret) throws Exception {
        if(log.isDebugEnabled()){
            System.out.println("    default rule return:"+ret );
        }

        return new ResultCheck(true,ret);
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config,Object ret,Exception e) throws Exception {
        throw new Exception("now support rollback");
    }

    public static void main(String[] args){
        try {
            Object o = RuleUtil.doRule("(1==1 and true==true) or false==false", null);
            System.out.println(o);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
