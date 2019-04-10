package com.octopus.tools.dataclient.impl.trans;

import com.octopus.tools.dataclient.ITransaction;
import com.octopus.tools.jvminsmgr.InstancesUtils;
import com.octopus.utils.alone.SNUtils;
import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.thread.ds.WaitingException;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: Administrator
 * Date: 14-9-19
 * Time: 下午2:28
 */
public class Transaction extends XMLObject implements ITransaction {

    ConcurrentHashMap<String,WaitingException> activerMap = new ConcurrentHashMap<String,WaitingException>();
    ConcurrentHashMap<String,Map<String,Boolean>> statusMap = new ConcurrentHashMap<String,Map<String,Boolean>>();
    ConcurrentHashMap<String,AbstractTransactionTask[]> taskMap = new ConcurrentHashMap<String,AbstractTransactionTask[]>();

    public Transaction(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
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
    public boolean work(AbstractTransactionTask[] tasks,int timeoutMillSecond) {
        if(null == tasks)return false;
        //多线程发布任务
        String id = generatorTransactionId(tasks.length);
        for(int i=0;i<tasks.length;i++){
            tasks[i].setTransactionId(id);
            tasks[i].setTaskSeqNo("T" + i);
        }
        WaitingException activer = new WaitingException();
        activerMap.put(id,activer);
        taskMap.put(id,tasks);
        try{
            //执行等待反馈
            Object[] rets = ExecutorUtils.multiWorkSameParWaiting(tasks, "doTask", null, null, timeoutMillSecond, activer);
            //提交
            rets = ExecutorUtils.multiWorkSameParWaiting(tasks, "doCommit", null, null, timeoutMillSecond, activer);

        }catch (Exception e){
            //异常回滚处理
            rollback(id,tasks,timeoutMillSecond,activer,"error rollback:"+e.getMessage());
        }
        return false;
    }

    boolean rollback(String id,AbstractTransactionTask[] tasks,int timeoutMillSecond,WaitingException activer,String msg){
        try{

            Object[] noTimeout = ExecutorUtils.multiWorkSameParWaiting(tasks, "doRollback", null, null, timeoutMillSecond, activer);
            //回滚成功
            return true;

        }catch (Exception e){
            //回滚失败放弃，记日志
        }finally {
            activerMap.remove(id);
            taskMap.remove(id);
        }
        return false;
    }

    /**
     *
     * @param transactionId
     * @param seqNo
     * @return 0:wating 1:finished & true -1:false
     */
    int judgeAllFeedBack(String transactionId,String seqNo){
        if(!statusMap.get(transactionId).get(seqNo)){
            return -1;
        }
        if(statusMap.get(transactionId).size()==taskMap.get(transactionId).length){
            return 1;
        }
        return 0;
    }

    @Override
    public boolean receive(String transactionId,String seqNo,boolean status) {
        if(!statusMap.contains(transactionId)){
            statusMap.put(transactionId,new HashMap<String, Boolean>());
        }
        statusMap.get(transactionId).put(seqNo,status);
        if(judgeAllFeedBack(transactionId,seqNo)==1){
        //所有信息反馈成功，
            activerMap.get(transactionId).notify();
        }
        if(judgeAllFeedBack(transactionId,seqNo)<0){
            //判断该事务号是否都收到反馈，如果都是反馈成功激活，如果有一个反馈失败回滚
            activerMap.get(transactionId).addException("transactionId", transactionId);
            activerMap.get(transactionId).addException("seqNo", seqNo);
            activerMap.get(transactionId).addException("status", String.valueOf(status));
            activerMap.get(transactionId).notify();
        }
        return true;
    }

    @Override
    public String generatorTransactionId(int count) {
        return InstancesUtils.getCurrentInstance()+"_"+count+"_"+SNUtils.generatorSimpleUnionCode("TRAN","",6);
    }
}
