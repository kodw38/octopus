package com.octopus.tools.dataclient.impl.trans;


import com.octopus.tools.jvminsmgr.InstancesUtils;
import com.octopus.tools.jvminsmgr.ds.WaitResults;
import com.octopus.utils.alone.BooleanUtils;

import java.io.Serializable;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 1.负责与该事务的主机通信，通信失败，回滚本任务
 * 2.执行失败/成功，回馈执行信息
 * 3.提交,反馈信息
 * 4.回滚,反馈信息
 * 提供执行、提交、回滚接口，参数一样
 * doTask,commit,rollback需要有锁的机制
 * User: Administrator
 * Date: 14-9-19
 * Time: 下午1:51
 */
public abstract class AbstractTransactionTask implements Serializable {
    String tranId,seqNo;
    String srcInstance;
    final ReentrantLock lock = new ReentrantLock();

    public void setTransactionId(String id){
        this.tranId=id;
        srcInstance = id.substring(0,id.indexOf("_"));
    }
    public void setTaskSeqNo(String seqNo){
        this.seqNo=seqNo;
    }
    public AbstractTransactionTask(){

    }

    boolean feedBack(boolean b){
        WaitResults wr = InstancesUtils.remoteWaitInvokeInstances(srcInstance, Transaction.class.getName(), "receive", new Object[]{tranId, seqNo, b}, null);
        if(null != wr && null != wr.getResults() && BooleanUtils.isTrue(wr.getResults()[0])){
            return true;
        }
        return false;
    }

    void doTask()throws Exception{
        try{
            lock.lock();
            task();
            boolean  b = feedBack(true);
            if(!b){
                doRollback();
                throw new Exception("feedback error in task. transactionId["+tranId+"]");
            }
        }catch (Exception e){
            throw e;
        }finally {
            lock.unlock();
        }
    }
    Object doCommit()throws Exception{
        try{
            lock.lock();
            return doCommit();
        }catch (Exception e){
            throw e;
        }finally {
            boolean  b = feedBack(true);
            if(!b){
                doRollback();
                throw new Exception("feedback error in task. transactionId["+tranId+"]");
            }
            lock.unlock();
        }
    }
    boolean doRollback()throws Exception{
        try{
            lock.lock();
            return rollback();
        }catch (Exception e){
            throw e;
        }finally {
            lock.unlock();
        }
    }


    public abstract void task()throws Exception;
    public abstract Object commit()throws Exception;
    public abstract boolean rollback()throws Exception;

}
