package com.octopus.utils.rule;

import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.commons.jexl2.JexlThreadedArithmetic;
import org.apache.commons.jexl2.MapContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;

/**
 * User: Administrator
 * Date: 14-8-23
 * Time: 下午11:31
 */
public class RuleUtil {
    transient static Log log = LogFactory.getLog(RuleUtil.class);
    /**
     * 分析文本中的内容做逻辑判断、逻辑计算、数学运算、方法执行。返回最终结果
     * @param ruleTxt
     * @param context
     * @return
     * @throws Exception
     */
    public static Object doRule(String ruleTxt,Map context)throws Exception{
        try{
        JexlEngine engine =  new JexlEngine(null, new JexlThreadedArithmetic(true), null, null);
        JexlContext c = new MapContext(context);
        return engine.createExpression(ruleTxt).evaluate(c);
        }catch (Exception e){
           log.error(ruleTxt,e);
            throw e;
        }
    }
}
