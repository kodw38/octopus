package com.octopus.tools.dataclient;

import com.octopus.tools.dataclient.impl.trans.AbstractTransactionTask;
import junit.framework.TestCase;

/**
 * User: Administrator
 * Date: 14-9-22
 * Time: 下午2:57
 */
public class TestTransaction extends TestCase {
    public TestTransaction(){
        super();
    }

    class  TestTask extends AbstractTransactionTask {
        int type;
        String msg;
        public TestTask(int type,String msg){
           this.type=type;
            this.msg=msg;
        }

        @Override
        public void task() throws Exception {
            if(type==2){
                throw new Exception("业务处理异常");
            }
        }

        @Override
        public Object commit() throws Exception{
            if(type==3){
                throw new Exception("commit异常");
            }
            return null;
        }

        @Override
        public boolean rollback() throws Exception{
            if(type==4){
                throw new Exception("rollback异常");
            }
            return true;
        }
    }

    public void test(){

    }
}
