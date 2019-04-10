package com.octopus.isp.cell.actions;

import com.octopus.isp.ds.RequestParameters;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.flow.FlowParameters;
import com.octopus.utils.flow.impl.Flow;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.*;

/**
 * User: Administrator
 * Date: 14-11-18
 * Time: 下午3:28
 */
public class DefaultCellAction extends Flow {
    public DefaultCellAction(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }
    @Override
    public Object doSomeThing(String xmlid,XMLParameter env,Map input,Map output,Map cfg) throws Exception {
        String dos = null;//parameter.getXmlParameter().getProperties().getProperty("do");
        if(StringUtils.isNotBlank(dos) && dos.equals("for")){
            Collection ls = (Collection)env.getResult();
            Iterator is = ls.iterator();
            List ret = new LinkedList();
            while(is.hasNext()){
                ((RequestParameters)env).setRequestData(is.next());
                doFlow((FlowParameters)env);
                ret.add(env.getResult());
            }
            return ret;
        }

        return null;
    }
}
