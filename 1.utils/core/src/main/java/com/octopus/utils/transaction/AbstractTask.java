package com.octopus.utils.transaction;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-7-15
 * Time: 上午10:44
 */
public abstract class AbstractTask implements ITransactionTask {
    transient static Log log = LogFactory.getLog(AbstractTask.class);
    boolean isAsyn;
    String instanceinfo,machineinfo;
    String flag = TransactionConsole.FLAG;
    TransactionConsole console;
    public AbstractTask(){
        machineinfo="";
        String instanceId = System.getProperty("instanceId");
        instanceinfo = instanceId;
    }

    public void setAsyn(boolean asyn) {
        isAsyn = asyn;
    }

    public void setConsole(TransactionConsole console) {
        this.console = console;
    }

    //taskcode|transactionid|status_type|status_value|msg
    void sendMsg(String taskcode,String transactionid,String status_type,String status_value,String msg){
        try{
            String s = taskcode.concat(flag).concat(transactionid).concat(flag).concat(status_type).concat(flag).concat(status_value).concat(flag).concat(msg);
            sendFeedbackMsg(s);
        }catch (Exception e){
            log.error(e);
            console.asynLog(machineinfo,instanceinfo,this.getClass().getName(),taskcode,transactionid,ExceptionUtils.getMessage(e),null,TRANSACTION_STATUS_TASK_MQ_SEND_ERROR);
        }
    }

    @Override
    public void submit(Map env, String taskCode, String transactionId, Object obj) throws Exception {
        console.asynLog(machineinfo,instanceinfo,this.getClass().getName(),taskCode,transactionId,null,null,TRANSACTION_STATUS_TASK_SUBMIT_PREPARE);
        try{
            doSubmit(env, taskCode, transactionId, obj);
            if(isAsyn())
                sendMsg(taskCode,transactionId,PHRASE_SUBMIT,String.valueOf(TRANSACTION_STATUS_TASK_SUBMIT_SUCCESSFUL),"");
            console.asynLog(machineinfo,instanceinfo,this.getClass().getName(),taskCode,transactionId,null,null,TRANSACTION_STATUS_TASK_SUBMIT_SUCCESSFUL);
        }catch (Exception e){
            log.error(e);
            if(isAsyn())
                sendMsg(taskCode,transactionId,PHRASE_SUBMIT,String.valueOf(TRANSACTION_STATUS_TASK_SUBMIT_FAULT),ExceptionUtils.getMessage(e));
            console.asynLog(machineinfo,instanceinfo,this.getClass().getName(),taskCode,transactionId,null,null,TRANSACTION_STATUS_TASK_SUBMIT_FAULT);
            throw e;
        }
    }
    public  abstract void doSubmit(Map env, String taskCode, String transactionId, Object obj)throws Exception;
    public abstract void sendFeedbackMsg(String msg) throws Exception;

    @Override
    public Object commit(String taskCode, String transactionId, Object obj) throws Exception {
        console.asynLog(machineinfo,instanceinfo,this.getClass().getName(),taskCode,transactionId,null,null,TRANSACTION_STATUS_TASK_COMMIT_PREPARE);
        try{
            Object ret = doCommit(taskCode, transactionId, obj);
            if(isAsyn()){
                sendMsg(taskCode,transactionId,PHRASE_COMMIT,String.valueOf(TRANSACTION_STATUS_TASK_COMMIT_SUCCESSFUL),ret.toString());
            }
            console.asynLog(machineinfo,instanceinfo,this.getClass().getName(),taskCode,transactionId,null,null,TRANSACTION_STATUS_TASK_COMMIT_SUCCESSFUL);
            return ret;
        }catch (Exception e){
            log.error(e);
            if(isAsyn())
                sendMsg(taskCode,transactionId,PHRASE_COMMIT,String.valueOf(TRANSACTION_STATUS_TASK_COMMIT_FAULT), ExceptionUtils.getMessage(e));
            console.asynLog(machineinfo, instanceinfo, this.getClass().getName(), taskCode, transactionId, null, null, TRANSACTION_STATUS_TASK_COMMIT_FAULT);
            throw e;
        }
    }
    public  abstract Object doCommit(String taskCode, String transactionId, Object obj)throws Exception;

    @Override
    public void rollback(String taskCode, String transactionId, Object obj) throws Exception {
        console.asynLog(machineinfo,instanceinfo,this.getClass().getName(),taskCode,transactionId,null,null,TRANSACTION_STATUS_TASK_ROLLBACK_PREPARE);
        try{
            doRollback(taskCode, transactionId, obj);
            if(isAsyn()){
                sendMsg(taskCode,transactionId,PHRASE_ROLLBACK,String.valueOf(TRANSACTION_STATUS_TASK_ROLLBACK_SUCCESSFUL),"");
            }
            console.asynLog(machineinfo,instanceinfo,this.getClass().getName(),taskCode,transactionId,null,null,TRANSACTION_STATUS_TASK_ROLLBACK_SUCCESSFUL);
        }catch (Exception e){
            log.error(e);
            if(isAsyn())
                sendMsg(taskCode,transactionId,PHRASE_SUBMIT,String.valueOf(TRANSACTION_STATUS_TASK_ROLLBACK_FAULT), ExceptionUtils.getMessage(e));
            console.asynLog(machineinfo, instanceinfo, this.getClass().getName(), taskCode, transactionId, null, null, TRANSACTION_STATUS_TASK_ROLLBACK_FAULT);
            throw e;
        }
    }
    public  abstract void doRollback(String taskCode, String transactionId, Object obj)throws Exception;

    @Override
    public boolean isAsyn() {
        return isAsyn;
    }
}
