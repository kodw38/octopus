package com.octopus.utils.transaction;

import com.octopus.utils.alone.StringUtils;
import redis.clients.jedis.Jedis;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * 根据日志稽核任务，修正，和事务控制分开部署，最好部署在不同的主机上，该类是个后台进程任务类。
 * User: wfgao_000
 * Date: 15-7-15
 * Time: 下午5:32
 * 超时的异步处理，异常的回滚处理
 */
public class LogCheck {
    static Jedis jedis = new Jedis("127.0.0.1",6379);

    public void start(){
        while(true){
            Set kset = jedis.keys("*");
            Iterator ks = kset.iterator();
            while(ks.hasNext()){
                String trad = (String)ks.next();
                Map<String,String> map = jedis.hgetAll(trad);

                //没有任何提交的删除
                String[] fd = findTaskCode(map,String.valueOf(ITransactionTask.TRANSACTION_STATUS_TASK_COMMIT_PREPARE),null);
                if(null == fd){
                    jedis.del(trad);
                    continue;
                }
                //失败都回滚成功的删除
                if(findAllTaskStatus(map,ITransactionTask.PHRASE_ROLLBACK)==ITransactionTask.TRANSACTION_STATUS_TASK_ROLLBACK_SUCCESSFUL){
                    jedis.del(trad);
                    continue;
                }

                //超时
                if(map.containsKey("null-11")){
                    boolean fin=true;
                    //任务都已经成功提交
                    if(map.containsKey("null-15")){
                        //没有回滚，没有异常 重做
                        fd = findTaskCode(map,String.valueOf(ITransactionTask.TRANSACTION_STATUS_TASK_COMMIT_FAULT),null);
                        if(null == fd){
                            fd = findTaskCode(map,String.valueOf(ITransactionTask.TRANSACTION_STATUS_TASK_SUBMIT_PREPARE),String.valueOf(ITransactionTask.TRANSACTION_STATUS_TASK_COMMIT_PREPARE));
                            if(null != fd){
                                String msgaddress=map.get("title-nd");
                                String serverName=map.get("title-sn");

                                for(String code:fd){
                                    String v = map.get(code.concat("-").concat(String.valueOf(ITransactionTask.TRANSACTION_STATUS_TASK_SUBMIT_PREPARE)));
                                    if(StringUtils.isNotBlank(v)){
                                        String[] fs = StringUtils.split(v,TransactionConsole.FLAG);
                                        try{
                                            ((ITransactionTask)Class.forName(fs[2]).newInstance()).submit(null,fs[3],fs[4],TransactionConsole.convertParameter2Obj(fs[5]));
                                            //TransactionConsole.asynLog(fs[0], fs[1], this.getClass().getName(), fs[2], fs[3], fs[4], null, ITransactionTask.TRANSACTION_STATUS_TASK_SUBMIT_SUCCESSFUL);
                                        }catch (Exception e){
                                            //TransactionConsole.asynLog(fs[0], fs[1], this.getClass().getName(), fs[2], fs[3], fs[4], null, ITransactionTask.TRANSACTION_STATUS_TASK_SUBMIT_FAULT);
                                            rollOther(map,fs[3]);
                                            break;
                                        }
                                        try{
                                            //TransactionConsole.asynLog(fs[0], fs[1], this.getClass().getName(), fs[2], fs[3], fs[4], null, ITransactionTask.TRANSACTION_STATUS_TASK_COMMIT_PREPARE);
                                            Object ret = ((ITransactionTask)Class.forName(fs[2]).newInstance()).commit(fs[3],fs[4],TransactionConsole.convertParameter2Obj(fs[5]));

                                            //TransactionConsole.asynLog(fs[0], fs[1], this.getClass().getName(), fs[2], fs[3], fs[4], null, ITransactionTask.TRANSACTION_STATUS_TASK_COMMIT_SUCCESSFUL);
                                        }catch (Exception e){
                                            //TransactionConsole.asynLog(fs[0], fs[1], this.getClass().getName(), fs[2], fs[3], fs[4], null, ITransactionTask.TRANSACTION_STATUS_TASK_COMMIT_FAULT);
                                            rollOther(map,fs[3]);
                                            break;
                                        }
                                    }
                                }

                                sendMsg(msgaddress,serverName,getResult(trad));
                            }
                        }
                    }else{//任务未ei所有提交,则撤销
                        jedis.del(trad);
                    }
                }

                //调用失败，则撤销
                if(jedis.hexists(trad,"null-13")){
                    boolean fin=true;
                    //有成功的，没有回滚的进行回滚,有200，没300
                    fd=findTaskCode(map,String.valueOf(ITransactionTask.TRANSACTION_STATUS_TASK_COMMIT_SUCCESSFUL),ITransactionTask.TRANSACTION_STATUS_TASK_ROLLBACK_SUCCESSFUL+","+ITransactionTask.TRANSACTION_STATUS_TASK_ROLLBACK_FAULT);
                    if(null != fd){
                        for(String f:fd){
                            String v = map.get(f.concat("-").concat(String.valueOf(TransactionConsole.TRANSACTION_STATUS_CONSOLE_SUBMIT_RECEIVE)));
                            if(StringUtils.isNotBlank(v)){
                                String[] fs = StringUtils.split(v,TransactionConsole.FLAG);
                                try{
                                    rollback(fs[2],fs[3],fs[4],TransactionConsole.convertParameter2Obj(fs[5]));
                                }catch (Exception e){
                                    if(fin)
                                        fin=false;
                                    //TransactionConsole.asynLog(fs[0], fs[1], this.getClass().getName(), fs[2], fs[3], fs[4], null, ITransactionTask.TRANSACTION_STATUS_TASK_ROLLBACK_FAULT);
                                }
                            }
                        }
                        //处理完删除
                        if(fin){
                            jedis.del(trad);
                        }
                    }

                }


            }
        }
    }

    void rollback(String c,String taskCode,String tradeId,Object parameters)throws Exception{
        ((ITransactionTask)Class.forName(c).newInstance()).rollback(taskCode,tradeId,parameters);
    }

    String[] findTaskCode(Map map,String include,String exclude){
        String[] ins = null;
        if(null != include)
            ins = include.split(",");
        String[] ons = null;
        if(null != exclude)
            ons=exclude.split(",");
        //todo
        return null;
    }

    void sendMsg(String address,String title,String result){

    }

    /**
     * 从redis中获取最后一个task的返回结果送到前台
     * @param trade
     * @return
     */
    String getResult(String trade){
        return null;
    }

    void rollOther(Map map,String thisTaskcode){

    }

    int findAllTaskStatus(Map map ,String type){
        return 0;
    }

}
