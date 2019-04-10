package com.octopus.utils.balance.qps;

import java.util.List;
import java.util.Map;
import java.util.TimerTask;

/**
 * Created by Administrator on 2019/2/18.
 */
public interface IDataGetter {
    public List getDataEachTime();
    public void stopReceive();
    public void startReceive();
    public boolean setConfig(Map m);
}
