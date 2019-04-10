package com.octopus.utils.xml.auto;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Administrator on 2018/5/4.
 */
public class ClearTimeoutCountRunnable implements Runnable {

    Map<String,AtomicInteger> map;
    String key;
    public ClearTimeoutCountRunnable(Map map,String key){
      this.map=map;
      this.key=key;
    }

    @Override
    public void run() {
        map.get(key).addAndGet(-1);
    }
}
