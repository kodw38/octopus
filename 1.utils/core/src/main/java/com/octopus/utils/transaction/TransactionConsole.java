package com.octopus.utils.transaction;

import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.SNUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.net.NetUtils;
import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.xml.auto.logic.ITradeFinish;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 方法的参数必须是所有元素为string的Map
 * User: wfgao_000
 * Date: 15-7-14
 * Time: 上午8:27
 */
public abstract class TransactionConsole {
    transient static Log log = LogFactory.getLog(TransactionConsole.class);
    ConcurrentMap<String,Map<String,Map<String,Object>>> map = new ConcurrentHashMap();
    ConcurrentMap<String,ITradeFinish> finishedListener = new ConcurrentHashMap();
    ReentrantLock lock = new ReentrantLock();
    ConcurrentMap<String,Condition> locks = new ConcurrentHashMap();
    //ConcurrentMap<String,Condition> recLocks = new ConcurrentHashMap();

    //刚增加任务的日志状态
    static int TRANSACTION_STATUS_CONSOLE_SUBMIT_RECEIVE = 1;
    //任务处理成功的日志状态
    static int TRANSACTION_STATUS_CONSOLE_SUBMIT_SUCCESS = 2;
    //任务处理失败的日志状态
    static int TRANSACTION_STATUS_CONSOLE_SUBMIT_FAULT = 3;
    static int TRANSACTION_STATUS_CONSOLE_SUBMIT_ALL_FAULT = 14;
    static int TRANSACTION_STATUS_CONSOLE_SUBMIT_ALL_SUCCESS = 15;

    static int TRANSACTION_STATUS_CONSOLE_ROLLBACK_PREPARE = 5;
    static int TRANSACTION_STATUS_CONSOLE_ROLLBACK_SUCESS = 6;
    static int TRANSACTION_STATUS_CONSOLE_ROLLBACK_FAULT = 7;
    static int TRANSACTION_STATUS_CONSOLE_GETRESULT_PREPARE = 12;
    static int TRANSACTION_STATUS_CONSOLE_GETRESULT_FAULT = 13;
    static int TRANSACTION_STATUS_CONSOLE_COMMIT_ALL_SUCCESS = 16;
    static int TRANSACTION_STATUS_CONSOLE_COMMIT_ALL_FAULT = 17;

    static int TRANSACTION_STATUS_CONSOLE_COMMIT_SUCCESS = 10;

    static int TRANSACTION_STATUS_CONSOLE_COMMIT_PREPARE = 8;
    static int TRANSACTION_STATUS_CONSOLE_COMMIT_FAULT = 9;
    static int TRANSACTION_STATUS_CONSOLE_TIMEOUT = 11;

    static String FLAG="%^%";


    protected static String instanceinfo,machineinfo;

    public TransactionConsole(String instanceId){
        machineinfo = NetUtils.getip();

        if(null == instanceId)
            instanceId = System.getProperty("instanceId");
        instanceinfo = instanceId;

        mesReceiveListener(instanceId);

    }




    /**
     * 参数都是Map，基本类型
     * @param msg
     * @return
     */
    public static Object convertParameter2Obj(String msg){
       return StringUtils.convert2MapJSONObject(msg);
    }
    public static String convertParameter2String(Object obj){
        if(obj instanceof String){
            return (String)obj;
        }else if(obj instanceof Map){
            return ObjectUtils.convertMap2String((Map)obj);
        }else{
            return obj.toString();
        }
    }

    public void setFinished(String transId,ITradeFinish run){
        finishedListener.put(transId,run);
    }
    public Runnable getFinished(String transId){
        return finishedListener.get(transId);
    }
    /**
     * 要保证日志的正确到达，单个任务中的事务需要自己保证，抛出异常，console认为该任务失败，并且资源未变更。
     * @param task
     * @param transactionId
     * @param taskCode
     * @param parameters
     * @throws Exception
     */
    public void addTransactionTask(Object task,String transactionId,String taskCode,Object parameters,String asynReceiveAddress)throws Exception{
        ITransactionTask taskImp=null;
        Map p=null;
        try{
            //判断是否已经有事务交易
            Map m=map.get(transactionId);
            if(null == m){
                m = new LinkedHashMap();
                map.put(transactionId,m);
            }
            //异步服务器记录日志
            asynLog(machineinfo,instanceinfo,task.getClass().getName(),taskCode,transactionId,parameters,asynReceiveAddress,TRANSACTION_STATUS_CONSOLE_SUBMIT_RECEIVE);

            if(!ITransactionTask.class.isAssignableFrom(task.getClass())){
                throw new Exception(task.getClass().getName()+" is not implements "+ITransactionTask.class.getName());
            }
            //判断当前任务是否存在
            p = (Map)m.get(taskCode);
            if(null == p){
                p = new HashMap();
                m.put(taskCode,p);
            }
            //发送该任务的订阅消息
            taskImp = (ITransactionTask)task;
            taskImp.setConsole(this);
            p.put("task",taskImp);
            p.put("parameters",parameters);

            taskImp.submit(m,taskCode,transactionId,parameters);
            if(!taskImp.isAsyn()){
                p.put(ITransactionTask.PHRASE_SUBMIT,ITransactionTask.TRANSACTION_STATUS_TASK_SUBMIT_SUCCESSFUL);
            }
            asynLog(machineinfo,instanceinfo,task.getClass().getName(),taskCode,transactionId,null,null,TRANSACTION_STATUS_CONSOLE_SUBMIT_SUCCESS);

        }catch (Exception e){
            log.error(e);
            //网络异常，提交失败，需要回滚其他已经提交的任务
            asynLog(machineinfo,instanceinfo,task.getClass().getName(),taskCode,transactionId,null,null,TRANSACTION_STATUS_CONSOLE_SUBMIT_FAULT);
            //查找本任务是否已经完成，如果完成则不回退
            if(!taskImp.isAsyn()){
                p.put(ITransactionTask.PHRASE_SUBMIT,ITransactionTask.TRANSACTION_STATUS_TASK_SUBMIT_FAULT);
                //rollbackOther(transactionId,taskCode);
            }
            throw e;
        }
    }

    /**
     *采用Redis存储日志
     * @param machineInfo
     * @param instanceInfo
     * @param taskClass
     * @param taskCode
     * @param transactionId
     * @param parameters
     * @param asynReceiveAddress
     * @param status
     */
    public synchronized void asynLog(String machineInfo,String instanceInfo,String taskClass,String taskCode,String transactionId,Object parameters,String asynReceiveAddress,int status){
        StringBuffer sb = new StringBuffer();
        String par = "";
        if(null != parameters){
            par = convertParameter2String(parameters);
        }
        sb.append(machineInfo).append(FLAG).append(instanceInfo).append(FLAG).append(taskClass).append(FLAG).append(taskCode).append(FLAG).append(transactionId).append(FLAG).append(par).append(FLAG).append(asynReceiveAddress).append(FLAG).append(status);
        saveLog(transactionId, taskCode + "-" + status, sb.toString());

    }
    public abstract void saveLog(String transactionId,String key,String text);
    public abstract void clearAsynLog(String transactionId);
    public abstract String collectionFeedbackMsg(String queue)throws Exception;


    /**
     * 检查消息服务器返回的该任务的状态是否成功状态
     * @param taskCode
     * @param transactionID
     */
    /*boolean isTaskSubmitBack(String taskCode,String transactionID)throws Exception{
        Integer in = (Integer)map.get(transactionID).get(taskCode).get("SUBMIT_STATUS");
        boolean b=true;
        if(null == in){
            try{
                lock.lockInterruptibly();
                Condition c = lock.newCondition();
                recLocks.put(transactionID,c);
                b = c.await(30, TimeUnit.SECONDS);
                in = (Integer)map.get(transactionID).get(taskCode).get("SUBMIT_STATUS");
            }catch (Exception e){

            }finally {
                lock.unlock();
            }
        }
        //超时该任务转异步处理
        if(!b){
            //waitTimeout(transactionID);
            return true;
        }else{
            //正常返回
            if(in == ITransactionTask.TRANSACTION_STATUS_TASK_SUBMIT_SUCCESSFUL){
                return true;
            }else{
                return false;
            }
        }
    }*/

    /**
     * 异步回滚其他已经处理的任务，需要结合消息服务器返回的状态
     * @param transactionId
     */
    void rollbackOther(String transactionId,String taskCode){
        Map m = map.get(transactionId);
        Iterator<String> its = m.keySet().iterator();
        while(its.hasNext()){
            String taskcode = its.next();
            if(taskCode.equals(taskcode))
                continue;
            Map pa = (Map)m.get(taskcode);
            try{
                if(null == pa.get("ROLLBACK_STATUS") && null != pa.get(ITransactionTask.PHRASE_COMMIT) && pa.get(ITransactionTask.PHRASE_COMMIT).equals(ITransactionTask.TRANSACTION_STATUS_TASK_COMMIT_SUCCESSFUL)){
                    pa.put("ROLLBACK_STATUS",TRANSACTION_STATUS_CONSOLE_ROLLBACK_PREPARE);
                    asynLog(machineinfo,instanceinfo,pa.get("task").getClass().getName(),taskcode,transactionId,null,null,TRANSACTION_STATUS_CONSOLE_ROLLBACK_PREPARE);

                    ((ITransactionTask)pa.get("task")).rollback(taskcode,transactionId,pa.get("parameters"));

                    asynLog(machineinfo,instanceinfo,pa.get("task").getClass().getName(),taskcode,transactionId,null,null,TRANSACTION_STATUS_CONSOLE_ROLLBACK_SUCESS);
                    pa.put("ROLLBACK_STATUS",TRANSACTION_STATUS_CONSOLE_ROLLBACK_SUCESS);
                }
            }catch (Exception e){
                log.error(e);
                asynLog(machineinfo,instanceinfo,pa.get("task").getClass().getName(),taskcode,transactionId,null,null,TRANSACTION_STATUS_CONSOLE_ROLLBACK_FAULT);
                pa.put("ROLLBACK_STATUS",TRANSACTION_STATUS_CONSOLE_ROLLBACK_FAULT);
            }
        }
    }

    /**
     * 超时处理，抛出异常中断当前线程，转异步处理
     * @throws Exception
     */
    void waitTimeout(String transactionId)throws Exception{

        asynLog(machineinfo, instanceinfo, null, null, transactionId, null, null, TRANSACTION_STATUS_CONSOLE_TIMEOUT);
        map.remove(transactionId);
        locks.remove(transactionId);
        //recLocks.remove(transactionId);
        throw new Exception("system is busy. the business become asynchronous, will give the message please.");
    }

    /**
     * 处理消息服务器接受到的消息
     */
    void mesReceiveListener(final String queue){
        ExecutorUtils.work(new Runnable() {
            @Override
            public void run() {

                while(true){
                    try{

                        String rec=collectionFeedbackMsg(queue);
                        String[] ps = StringUtils.split(rec,FLAG);  //taskcode|transactionid|status_type|status_value|msg
                        if(ps.length>=3) {
                            int state = Integer.parseInt(ps[3]);
                            if (ps.length > 4)
                                asynLog(machineinfo, instanceinfo, null, ps[0], ps[1], ps[4], null, state);
                            else
                                asynLog(machineinfo, instanceinfo, null, ps[0], ps[1], null, null, state);
                            if (!map.containsKey(ps[1])) continue;
                            if (log.isDebugEnabled())
                                log.debug("MQ R " + System.currentTimeMillis() + " " + ps[1] + "-" + ps[0] + "-" + ps[2]);
                            map.get(ps[1]).get(ps[0]).put(ps[2], state);
                            if (ps.length > 4 && StringUtils.isNotBlank(ps[4])) {
                                map.get(ps[1]).get(ps[0]).put("RESULT", ps[4]);
                            }
                            if (ps[2].equals(ITransactionTask.PHRASE_ROLLBACK)
                                    && (Integer) map.get(ps[1]).get(ps[0]).get(ITransactionTask.PHRASE_ROLLBACK) == ITransactionTask.TRANSACTION_STATUS_TASK_ROLLBACK_FAULT) {
                                asynLog(machineinfo, instanceinfo, null, ps[0], ps[1], null, null, TRANSACTION_STATUS_CONSOLE_ROLLBACK_FAULT);
                                //该任务执行失败,回滚该事务编号的其他任务
                                //rollbackOther(ps[1]);
                            } else if (ps[2].equals(ITransactionTask.PHRASE_COMMIT)
                                    && (Integer) map.get(ps[1]).get(ps[0]).get(ITransactionTask.PHRASE_COMMIT) == ITransactionTask.TRANSACTION_STATUS_TASK_COMMIT_FAULT) {
                                rollbackOther(ps[1], ps[0]);
                            } else if (ps[2].equals(ITransactionTask.PHRASE_SUBMIT)
                                    && (Integer) map.get(ps[1]).get(ps[0]).get(ITransactionTask.PHRASE_SUBMIT) == ITransactionTask.TRANSACTION_STATUS_TASK_SUBMIT_FAULT) {
                                rollbackOther(ps[1], ps[0]);
                            }
                            try {
                                lock.lockInterruptibly();
                                Condition s = locks.get(ps[1]);
                                if (null != s) s.signal();
                            } finally {
                                lock.unlock();
                            }
                        }
                        /*Condition c = recLocks.get(ps[1]);
                        if(null !=c) c.signal();*/
                    }catch (Exception e){
                        log.error("get message from queue error:",e);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    }
                }

            }
        });


    }

    /**
     * 等待所有任务结束
     * @return
     */
    public Object getResult(String transactionId) throws Exception{
        try{
            asynLog(machineinfo,instanceinfo,null,null,transactionId,null,null,TRANSACTION_STATUS_CONSOLE_GETRESULT_PREPARE);

            lock.lockInterruptibly();
            try {
                boolean flag=true;
                Map m = map.get(transactionId);
                Iterator its = m.keySet().iterator();
                String taskCode=null;
                while(its.hasNext()){
                    taskCode = (String)its.next();
                    Map ret = (Map)m.get(taskCode);
                    while(null == ret.get(ITransactionTask.PHRASE_SUBMIT)){
                        Condition c=locks.get(transactionId);
                        if(null == c){
                            c = lock.newCondition();
                            locks.put(transactionId,c);
                        }
                        boolean b = c.await(30,TimeUnit.SECONDS);
                        if(!b){
                            waitTimeout(transactionId);
                        }
                    }
                    if(flag && (Integer)ret.get(ITransactionTask.PHRASE_SUBMIT)!=ITransactionTask.TRANSACTION_STATUS_TASK_SUBMIT_SUCCESSFUL){
                        flag=false;
                    }
                }
                if(log.isDebugEnabled())
                    log.debug("--------submit:" + flag);
                if(flag){
                    asynLog(machineinfo,instanceinfo,null,null,transactionId,null,null,TRANSACTION_STATUS_CONSOLE_SUBMIT_ALL_SUCCESS);
                    //发送提交请求
                    its = m.keySet().iterator();
                    while(its.hasNext()){
                        taskCode = (String)its.next();
                        Map ret = (Map)m.get(taskCode);
                        try{
                            Object obj = ((ITransactionTask)ret.get("task")).commit(taskCode,transactionId,ret.get("parameters"));
                            if(!((ITransactionTask)ret.get("task")).isAsyn()){
                                if(null != obj)
                                    ret.put("RESULT",obj);
                                ret.put(ITransactionTask.PHRASE_COMMIT,ITransactionTask.TRANSACTION_STATUS_TASK_COMMIT_SUCCESSFUL);
                            }

                        }catch (Exception e){
                            log.error(e);
                            //如果同步提交异常回滚
                            if(flag)
                                flag=false;
                            ret.put(ITransactionTask.PHRASE_COMMIT,ITransactionTask.TRANSACTION_STATUS_TASK_COMMIT_FAULT);
                            ret.put("RESULT",e);
                            rollbackOther(transactionId,taskCode);
                            break;
                        }
                    }
                    if(log.isDebugEnabled())
                        log.debug("--------commit:" + flag);
                    if(flag){
                        its = m.keySet().iterator();
                        while(its.hasNext()){
                            taskCode = (String)its.next();
                            Map ret = (Map)m.get(taskCode);
                            while(null == ret.get(ITransactionTask.PHRASE_COMMIT)){
                                Condition c=locks.get(transactionId);
                                if(null == c){
                                    c = lock.newCondition();
                                    locks.put(transactionId,c);
                                }
                                boolean b = c.await(30,TimeUnit.SECONDS);
                                if(!b){
                                    waitTimeout(transactionId);
                                }

                            }
                            if(flag && (Integer)ret.get(ITransactionTask.PHRASE_COMMIT)!=ITransactionTask.TRANSACTION_STATUS_TASK_COMMIT_SUCCESSFUL){
                                flag=false;
                            }
                        }
                        if(log.isDebugEnabled())
                            log.debug("--------wait commit:" + flag);
                        if(flag){
                            Object obj = ((Map)m.get(taskCode)).get("RESULT");
                            asynLog(machineinfo,instanceinfo,null,null,transactionId,obj,null,TRANSACTION_STATUS_CONSOLE_COMMIT_ALL_SUCCESS);
                            clearAsynLog(transactionId);
                            if(finishedListener.size()>0){
                                Runnable run = finishedListener.get(transactionId);
                                if(null != run){
                                    run.run();
                                }
                            }
                            return obj;
                        }else{
                            asynLog(machineinfo,instanceinfo,null,null,transactionId,null,null,TRANSACTION_STATUS_CONSOLE_COMMIT_ALL_FAULT);
                        }
                    }else{
                        asynLog(machineinfo,instanceinfo,null,null,transactionId,null,null,TRANSACTION_STATUS_CONSOLE_COMMIT_ALL_FAULT);
                    }
                }else{
                    asynLog(machineinfo,instanceinfo,null,null,transactionId,null,null,TRANSACTION_STATUS_CONSOLE_SUBMIT_ALL_FAULT);

                }

                int i=0;
                StringBuffer sb = new StringBuffer();
                its = m.keySet().iterator();
                while(its.hasNext()){
                    i++;
                    taskCode = (String)its.next();
                    Object o = ((Map)m.get(taskCode)).get("RESULT");
                    if(o instanceof Exception){
                        sb.append(i).append(". ").append(ExceptionUtils.getStackTrace((Exception) o)).append("\n");
                    }else if(o instanceof String){
                        sb.append(i).append(". ").append(o).append("\n");
                    }
                }
                asynLog(machineinfo,instanceinfo,null,null,transactionId,null,null,TRANSACTION_STATUS_CONSOLE_GETRESULT_FAULT);
                throw new Exception(sb.toString());
            }catch (Exception e){
                throw e;
            }finally {
                lock.unlock();
            }

        }catch (Exception e){
            log.error(e);
            throw e;
        }finally {
            if(log.isDebugEnabled())
                log.debug("end");
            map.remove(transactionId);
            locks.remove(transactionId);
            //recLocks.remove(transactionId);
        }

    }

    public void clear(String trade){
        if(finishedListener.size()>0){
            ITradeFinish run = finishedListener.get(trade);
            if(null != run){
                run.clear();
            }
        }
    }


    public String newTranslactionId(){
        return SNUtils.getNewId();
    }

}
