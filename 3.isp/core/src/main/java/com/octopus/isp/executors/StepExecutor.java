package com.octopus.isp.executors;

import com.octopus.utils.bftask.BFParameters;
import com.octopus.utils.bftask.IBFExecutor;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;

/**
 * User: Administrator
 * Date: 14-10-9
 * Time: 下午3:35
 */
public class StepExecutor extends XMLObject implements IBFExecutor {
    //HashMap<String,IAction> actionsMap = new HashMap();
    public StepExecutor(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    @Override
    public void notifyObject(String op, Object obj) throws Exception {

    }

    @Override
    public void addTrigger(Object cond, InvokeTaskByObjName task) throws Exception {

    }

    @Override
    public void destroy() throws Exception {

    }

    @Override
    public void initial() throws Exception {

    }

    @Override
    public void execute(XMLMakeup xml,String action, BFParameters parameters, Throwable error) throws Exception {
        /*IAction step;
        if(actionsMap.containsKey(action)){
            step=actionsMap.get(action);
        }else{
            step=(IAction)Class.forName(action).newInstance();
            actionsMap.put(action,step);
        }
        step.doCellAction((RequestParameters) parameters);*/
    }
}
