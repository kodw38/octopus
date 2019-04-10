package com.octopus.utils.transaction;

import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.HashMap;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-11-11
 * Time: 上午11:14
 */
public class XMLDoTradeTask extends AbstractTask {
    XMLDoObject xmlDo;
    XMLDoObject sendFeedbackObject;
    public XMLDoTradeTask(XMLDoObject obj,XMLDoObject sendFeedbackObject){
        this.xmlDo=obj;
        this.sendFeedbackObject = sendFeedbackObject;
    }
    @Override
    public void doSubmit(Map env, String taskCode, String transactionId, Object obj) throws Exception {
        Object[] os = (Object[])obj;
        boolean is = xmlDo.checkParameters(taskCode,(XMLParameter)os[0],(Map)os[1],(Map)os[2],(Map)os[3]);
        if(!is){
            throw new Exception("checkParameters false ");
        }else{

        }
    }

    @Override
    public void sendFeedbackMsg(String msg) throws Exception {
        HashMap m = new HashMap();
        m.put("message",msg);
        m.put("op","add");
        sendFeedbackObject.doCheckThing(null,null,m,null,null,null);
    }

    @Override
    public Object doCommit(String taskCode, String transactionId, Object obj) throws Exception {
        Object[] os = (Object[])obj;
        XMLParameter parameter=(XMLParameter)os[0];
        Object ret=null;
        try{
            ret= xmlDo.doSomeThing(taskCode,(XMLParameter)os[0],(Map)os[1],(Map)os[2],(Map)os[3]);
            ret = xmlDo.checkResult(taskCode,(XMLParameter)os[0],(Map)os[1],(Map)os[2],(Map)os[3],ret);
            if(ret instanceof ResultCheck)
                ret = ((ResultCheck)ret).getRet();
            if(null == ret){
                if(null !=parameter)
                    parameter.removeParameter("${return}");
            }else{
                if(null !=parameter)
                    parameter.addParameter("${return}",ret);

                    if(ret instanceof ResultCheck && !((ResultCheck)ret).isSuccess()){
                        xmlDo.rollback(taskCode,(XMLParameter)os[0],(Map)os[1],(Map)os[2],(Map)os[3],ret,null);
                        throw new Exception("checkResult false ");
                    }
            }
        }catch(Exception e){
            xmlDo.rollback(taskCode,(XMLParameter)os[0],(Map)os[1],(Map)os[2],(Map)os[3],ret,null);
            throw e;
        }finally {
            parameter.removeParameter("${return}");
        }
        return ret;
    }

    @Override
    public void doRollback(String taskCode, String transactionId, Object obj) throws Exception {
        Object[] os = (Object[])obj;
        boolean is = xmlDo.rollback(taskCode,(XMLParameter)os[0],(Map)os[1],(Map)os[2],(Map)os[3],null,null);
        if(!is){
            throw new Exception("doRollback false ");
        }
    }
}
