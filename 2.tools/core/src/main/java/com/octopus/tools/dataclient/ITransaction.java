package com.octopus.tools.dataclient;

import com.octopus.tools.dataclient.impl.trans.AbstractTransactionTask;

/**
 * 事务:一批业务在不同的实例中执行，一个失败，全部失败.
 * 负责数据操作级的事务，
 * 1.任务的发布: 任务发布到对应实例或线程执行，一个发布失败，全部撤消或回滚。
 * 2.接收每个任务的执行情况，如果一个任务回馈失败，其他任务撤消或回滚
 * 3.等待接收超时回滚所有任务
 * 4.所有任务都反馈成功，向所有任务发出提交命令，如果一个提交失败，其他任务回滚。
 * transcationId:起始实例信息(起始实例为该事务的管理实例)+任务总数+所有任务序号
 * User: Administrator
 * Date: 14-9-16
 * Time: 下午4:24
 */
public interface ITransaction {
    /**
     * 执行需要事务保证的多任务
     * @param tasks
     * @return
     */
    public boolean work(AbstractTransactionTask[] tasks,int timeoutMillSecond);

    /**
     * 接受任务反馈的状态
     * @param transactionId
     * @return
     */
    public boolean receive(String transactionId,String seqNo,boolean status);

    /**
     * 产生事务编号
     * @return
     */
    public String generatorTransactionId(int count);


}
