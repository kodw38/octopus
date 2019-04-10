package com.octopus.utils.balance.qps;

import java.util.Map;

/**
 * Created by Administrator on 2019/2/25.
 */
public class QPSMBean {
    public boolean stopAll(){
        return false;
    }

    public boolean reassignStart(int mod){
        return false;
    }

    public Stat getStat(){
        return null;
    }

    public Map getQueueInfo(){
        return null;
    }


}
