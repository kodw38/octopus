package com.octopus.isp.executors;

import com.octopus.tools.dataclient.IDataClient;
import com.octopus.utils.bftask.BFParameters;
import com.octopus.utils.bftask.IBFExecutor;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;

/**
 * User: Administrator
 * Date: 14-10-22
 * Time: 上午9:52
 */
public class DataExecutor extends XMLObject implements IBFExecutor {
    //HashMap<String,ISaveStep> actionsMap = new HashMap();
    public DataExecutor(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
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
        IDataClient dataClient = (IDataClient)getPropertyObject("dataclient");
        /*ISaveStep step;
        if(actionsMap.containsKey(action)){
            step=actionsMap.get(action);
        }else{
            step=(ISaveStep)Class.forName(action).newInstance();
            actionsMap.put(action,step);
        }
        step.doCellAction(dataClient,(RequestParameters)parameters);*/
    }
}
