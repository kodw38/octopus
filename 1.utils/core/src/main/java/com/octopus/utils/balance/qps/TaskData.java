package com.octopus.utils.balance.qps;

/**
 * Created by Administrator on 2018/11/6.
 */
public class TaskData {
    Object obj;
    long cost;
    long putQueueTime;
    long takeQueueTime;
    long finishedTime;
    boolean isImportant;
    String id;
    String kindKey;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKindKey() {
        return kindKey;
    }

    public void setKindKey(String kindKey) {
        this.kindKey = kindKey;
    }

    public boolean isImportant() {
        return isImportant;
    }

    public long getTakeQueueTime() {
        return takeQueueTime;
    }

    public void setTakeQueueTime(long takeQueueTime) {
        this.takeQueueTime = takeQueueTime;
    }

    public void setImportant(boolean isImportant) {
        this.isImportant = isImportant;
    }

    public Object getObj() {
        return obj;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }

    public long getCost() {
        return cost;
    }

    public void setCost(long cost) {
        this.cost = cost;
    }

    public long getPutQueueTime() {
        return putQueueTime;
    }

    public void setPutQueueTime(long putQueueTime) {
        this.putQueueTime = putQueueTime;
    }

    public long getFinishedTime() {
        return finishedTime;
    }

    public void setFinishedTime(long finishedTime) {
        this.finishedTime = finishedTime;
    }
}
