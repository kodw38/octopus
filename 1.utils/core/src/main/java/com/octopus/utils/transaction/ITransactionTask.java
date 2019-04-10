package com.octopus.utils.transaction;

import java.util.Map;

/**
 * 事务:事务是保证一批逻辑的一致性，提供对一批业务逻辑要么一起成功要么一起失败的保证。
 * User: wfgao_000
 * Date: 15-7-13
 * Time: 下午2:44
 */
public interface ITransactionTask {
    public static int TRANSACTION_STATUS_TASK_SUBMIT_PREPARE =102;
    public static int TRANSACTION_STATUS_TASK_SUBMIT_SUCCESSFUL = 100;
    public static int TRANSACTION_STATUS_TASK_SUBMIT_FAULT = 101;
    public static int TRANSACTION_STATUS_TASK_COMMIT_PREPARE = 202;
    public static int TRANSACTION_STATUS_TASK_COMMIT_SUCCESSFUL = 200;
    public static int TRANSACTION_STATUS_TASK_COMMIT_FAULT = 201;
    public static int TRANSACTION_STATUS_TASK_ROLLBACK_PREPARE = 302;
    public static int TRANSACTION_STATUS_TASK_ROLLBACK_SUCCESSFUL = 300;
    public static int TRANSACTION_STATUS_TASK_ROLLBACK_FAULT = 301;
    public static int TRANSACTION_STATUS_TASK_MQ_SEND_ERROR = 400;
    public static String PHRASE_SUBMIT="SUBMIT_STATUS";
    public static String PHRASE_COMMIT="COMMIT_STATUS";
    public static String PHRASE_ROLLBACK="ROLLBACK_STATUS";
    /**
     * 主要校验逻辑和预处理逻辑。
     *    这步主要逻辑处理，这步发生的异常概率较大，这步不涉及到数据的修改，生产逻辑的变更。
     * @param obj
     * @return
     * @throws Exception
     */
    public void submit(Map env,String taskCode,String transactionId,Object obj)throws Exception;

    /**
     * 提交变更结果。
     *   这步主要是保存数据，调用外围接口
     * @return
     * @throws Exception
     */
    public Object commit(String taskCode,String transactionId,Object obj)throws Exception;

    /**
     * 这步一般是在commit发生异常时回滚的操作
     * @return
     * @throws Exception
     */
    public void rollback(String taskCode,String transactionId,Object obj)throws Exception;

    /**
     * 是否异步执行任务
     * @return
     */
    public boolean isAsyn();

    public void setAsyn(boolean asyn);

    public void setConsole(TransactionConsole console);

}
