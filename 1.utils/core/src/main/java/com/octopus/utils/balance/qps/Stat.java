package com.octopus.utils.balance.qps;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Administrator on 2018/11/6.
 */
public class Stat {
    Map map = new HashMap();
    long beginSlowCost=0;
    long afterQuickCost=0;
    long fastest=0;
    long slowest =0;
    boolean isAuto=true;
    public Stat(){
        new Timer().schedule(new InitTask(),600000,600000);
    }
    public Stat(long beginSlowCost , long afterQuickCost){
        if(beginSlowCost==0 && afterQuickCost==0){
            new Timer().schedule(new InitTask(),600000,600000);
        }else {
            this.beginSlowCost = beginSlowCost;
            this.afterQuickCost = afterQuickCost;
            isAuto = false;
        }

    }
    public long getCost(String key){
       Object o =  map.get(key);
        if(null != o){
            return (Long)o;
        }else{
            return 0;
        }
    }
    class InitTask extends TimerTask{

        @Override
        public void run() {
            if(isAuto){
                fastest=0;
                slowest=0;
                beginSlowCost=0;
                afterQuickCost=0;
            }
        }
    }

    public long getBeginSlowCost() {
        return beginSlowCost;
    }


    public long getAfterQuickCost() {
        return afterQuickCost;
    }


    public void putCost(String key,long cost){
        map.put(key,cost);
        if(isAuto) {
            if (fastest == 0 || cost < fastest) fastest = cost;
            if (cost > slowest) slowest = cost;

            if (fastest >= slowest) {
                afterQuickCost = slowest + 3;
            } else {
                long dur = slowest - fastest;
                if (dur > 200) {
                    afterQuickCost = fastest + 20;
                } else {
                    afterQuickCost = fastest + dur / 3;
                }
            }


            if (slowest <= fastest) {
                beginSlowCost = fastest + 10;
            } else {
                long dur = slowest - fastest;
                if (dur > 200) {
                    beginSlowCost = slowest - 200;
                } else {
                    beginSlowCost = slowest - dur / 3;
                }
            }
        }

    }
}
