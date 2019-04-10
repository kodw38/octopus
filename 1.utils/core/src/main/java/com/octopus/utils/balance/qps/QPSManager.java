package com.octopus.utils.balance.qps;

import com.octopus.isp.tools.IDataGet;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hbase.master.SplitLogManager;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 1.最快通道可以空任务,等待快速任务过来
 2.最慢通道可以处理快速的任务
 3.n是奇数
 3.获取的一批task按cost 从小到大排序。小的优先进入assign。同时优先的放在前面，排除是重要task。
 4.没有名次类型时，取0-n最短等待队列，放入。
 4.处理慢的后几名，且n队列数量最少，放入n队列。否则从1-n-1找一个最短等待队列
 5.中等task找从1-n找最小等待队列，放入任务。
 6.快速的task从0-n找最短等待队列，放入。
 7.几名=任务种类/Q数
 8. 如果任务种类数=1，直接0-n取最小等待Q。种类数〉1 and < Q, 最慢的为慢类型，最快的为快类型，其他中等。
 10.Queue组有自动调整的功能，保证快速task优先处理。找到最短的队列，找其中最大的task，如果不是优先任务，且属于慢任务，把这个任务移入n队列。有个定时任务，当get不到数据时，把n的任务安处理最短时间重新分配，防止有些Q空闲。nQ过多task。
 11。如果一个平时正常的任务，执行超过2倍时间，需要把相同的任务还在其它队列中的task移入慢队列。
 * Created by Administrator on 2018/11/6.
 */
public class QPSManager {
    transient static Log log = LogFactory.getLog(QPSManager.class);

    List<Queue> queues = new LinkedList<Queue>();
    int quickQueueIndex=0;
    int slowQueueIndex=-1;
    Stat stat = null;
    String[] taskKindKeyPaths;
    String[] importants;
    String importantTaskKeyPath;
    static long cacheMaxSize=5000; //max doing with order in member
    long receiveTime=0;
    String iDPath;
    Timer t = new Timer();
    Map processingDataCaches = new HashMap();
    boolean isAssigned;
    boolean isStopReceiver=false;
    IDataGetter dataGetter =null;
    public QPSManager(String iDPath,int queueCount,int queueSize,String[] taskKindKeyPaths,String importantTaskKeyPath,String[] importantValues, IProcess process,int AfterQuickCost,int BeginSlowCost) {
        this(null,0,iDPath,queueCount, queueSize, taskKindKeyPaths, importantTaskKeyPath, importantValues, process,AfterQuickCost,BeginSlowCost);

    }


    /**
     *
     * @param queueCount  队列的数量，一个快队列，一个慢队列，多个normal队列
     * @param queueSize   每个队列大小，可以存放排队的任务
     * @param taskKindKeyPaths   任务数据中能够表明同一种处理业务的属性名称，可以有多个属性表明一种业务类型
     * @param importantTaskKeyPath 任务数据中能够表明重要任务的属性名称，重要的任务不做快慢队列处理
     * @param importantValues  重要任务的属性值
     * @param process  任务处逻辑
     */
    public QPSManager(IDataGetter dataGetter,long getDataIntervalTime,String iDPath,int queueCount,int queueSize,String[] taskKindKeyPaths,String importantTaskKeyPath,String[] importantValues, IProcess process,long AfterQuickCost,long BeginSlowCost){
        this.taskKindKeyPaths=taskKindKeyPaths;
        this.importantTaskKeyPath=importantTaskKeyPath;
        this.importants=importantValues;
        stat = new Stat(BeginSlowCost,AfterQuickCost);
        for(int i=0;i<queueCount;i++) {
            queues.add(new Queue(queueSize,process,stat,processingDataCaches));
        }
        quickQueueIndex=0;
        this.iDPath = iDPath;
        slowQueueIndex=queueCount-1;

        DoSlowTimeTask sl = new DoSlowTimeTask(this);
        t.schedule(sl,1000,1000);
        this.dataGetter =dataGetter;
        if(null != dataGetter){
            t.schedule(new GetDataTask(dataGetter),1000,getDataIntervalTime);
        }

    }

    public boolean stopReceiver(){
        isStopReceiver=true;
        return true;
    }
    public boolean startReceiver(){
        isStopReceiver=false;
        return true;
    }

    public Object receive(Object data){
        if(!isStopReceiver) {
            isAssigned = false;
            receiveTime = System.currentTimeMillis();
            assign(appendData(data));
            isAssigned = true;
        }
        return null;
    }
    static int point=0;
    public void assignByRoundRobin(TaskData data){
        if(null != data) {
            try {
                int n = point++ % queues.size();
                queues.get(n).put(data);
                log.debug("queue "+n+" add task "+data.getCost());
                processingDataCaches.put(data.getId(), data);
            }catch (Exception e){

            }
        }
    }


    //get key by keypths
    String getKindKey(Object data){
        StringBuffer sb = new StringBuffer();
        if(null != taskKindKeyPaths){
            for(String k:taskKindKeyPaths) {
                String p = (String)ObjectUtils.getValueByPath(data, k);
                if(null != p) {
                    sb.append(p.concat("_"));
                }
            }
        }
        return sb.toString();
    }
    String getId(Object data){
        return (String)ObjectUtils.getValueByPath(data, iDPath);
    }
    boolean isImportantTask(Object data){
        if(importantTaskKeyPath!=null) {
            String p = (String) ObjectUtils.getValueByPath(data, importantTaskKeyPath);
            if (null != p) {
                if(ArrayUtils.isArrayLikeString(importants,p)){
                    return true;
                }
            }
        }
        return false;
    }

    TaskData appendData(Object data){
        String k = getKindKey(data);
        String id = getId(data);
        long cost = stat.getCost(k);
        TaskData t = new TaskData();
        t.setObj(data);
        t.setCost(cost);
        t.setKindKey(k);
        t.setPutQueueTime(System.currentTimeMillis());
        t.setImportant(isImportantTask(data));
        t.setId(id);
        return t;
    }
    private List appendListCost(Object[] data){
        if(null != data){
            List ret = new LinkedList();
            for(Object o:data){
                if(null !=o){
                    ret.add(appendData(o));
                }
            }
            return ret;
        }
        return null;
    }
    public Object receives(Object[] data){
        if(!isStopReceiver) {
            isAssigned = false;
            receiveTime = System.currentTimeMillis();
            List<TaskData> ll = appendListCost(data);
            List<TaskData> ret = sortByCost(ll);
            if (null != ret) {
                for (TaskData o : ret) {
                    assign(o);
                }
            }
            isAssigned = true;
        }
        return null;
    }

    private void assign(TaskData o){
        if(processingDataCaches.containsKey(o.getId())){
            return;// it has existed, so ignore it
        }
        int slowQueueSize=-1;
        long quickQueueCost=0;
        int minCostQueueNotFastAndSlowIndex=0;
        //int minCostQueueIndexNotQuickIndex=0;
        int minSizeQueueIndex=0;
        int minQueueSize=-1;
        int quickUseIndex=quickQueueIndex;
        int slowUseIndex=slowQueueIndex;
        long l=-1;
        for(int i=1;i<queues.size()-1;i++){
            long t = queues.get(i).getWaitingTime();
            if(t==0)t=queues.get(i).size();
            int s = queues.get(i).size();
            if(l==-1 || l>t){
                l=t;
                minCostQueueNotFastAndSlowIndex=i;
                /*if(i!=quickQueueIndex){
                    minCostQueueIndexNotQuickIndex=i;
                }*/
            }
            if(minQueueSize==-1 || minQueueSize>s){
                minQueueSize=s;
                minSizeQueueIndex=i;
            }
        }
        if(queues.get(quickQueueIndex).getWaitingTime()>queues.get(minCostQueueNotFastAndSlowIndex).getWaitingTime()||(queues.get(quickQueueIndex).getWaitingTime()==queues.get(minCostQueueNotFastAndSlowIndex).getWaitingTime() && queues.get(quickQueueIndex).size()>queues.get(minCostQueueNotFastAndSlowIndex).size())){
            quickUseIndex=minCostQueueNotFastAndSlowIndex;
        }
        if(queues.get(slowQueueIndex).getWaitingTime()>queues.get(minCostQueueNotFastAndSlowIndex).getWaitingTime()||(queues.get(slowQueueIndex).getWaitingTime()==queues.get(minCostQueueNotFastAndSlowIndex).getWaitingTime() && queues.get(slowQueueIndex).size()>queues.get(minCostQueueNotFastAndSlowIndex).size())){
            slowUseIndex=minCostQueueNotFastAndSlowIndex;
        }

        //if slow task and slow queue size is min then put the task to slow queue
        if(o.getCost()>stat.getBeginSlowCost()){
            if(slowUseIndex!=slowQueueIndex) {
                TaskData mc = queues.get(slowUseIndex).getMaxCostTask();
                if (null != mc) {
                    if (mc.getCost() > stat.getBeginSlowCost()) {
                        if(queues.get(slowUseIndex).remove(mc)) {
                            log.debug(" queue "+slowUseIndex+" remove task "+mc.getCost());
                            try {
                                synchronized (mc) {
                                    queues.get(slowQueueIndex).put(mc);
                                    log.debug("5 queue "+slowQueueIndex+" add task "+mc.getCost()+" slow "+stat.getBeginSlowCost()+" quick "+stat.getAfterQuickCost());
                                    processingDataCaches.put(mc.getId(), mc);
                                }
                            }catch (Exception e){
                                log.error("put queue error",e);
                            }
                        }
                    }
                }
            }
            try {
                //System.out.println("-P:"+(minCostQueueIndex+" - "+((TaskData)o).getCost()));
                synchronized (o) {
                    queues.get(slowUseIndex).put(o);
                    log.debug("7 queue "+slowUseIndex+" add task "+o.getCost()+" slow "+stat.getBeginSlowCost()+" quick "+stat.getAfterQuickCost());
                    processingDataCaches.put(o.getId(), o);
                }
            }catch (Exception e){
                log.error("put queue error",e);
            }
            /*try {
                if (slowUseIndex != slowQueueIndex) {
                    //System.out.println("-P:"+(slowQueueIndex+" - "+((TaskData)o).getCost()));
                    synchronized (o) {
                        queues.get(slowQueueIndex).put(o);
                        log.debug("1 queue "+slowQueueIndex+" add task "+o.getCost()+" slow "+stat.getBeginSlowCost()+" quick "+stat.getAfterQuickCost());
                        processingDataCaches.put(o.getId(), o);
                    }
                } else {
                    //System.out.println("-P:"+(minCostQueueIndexNotQuickIndex+" - "+((TaskData)o).getCost()));
                    synchronized (o) {
                        queues.get(minCostQueueNotFastAndSlowIndex).put(o);
                        log.debug("2 queue "+minCostQueueNotFastAndSlowIndex+" add task "+o.getCost()+" slow "+stat.getBeginSlowCost()+" quick "+stat.getAfterQuickCost());
                        processingDataCaches.put(o.getId(), o);
                    }
                }
            }catch (Exception e){
                log.error("put queue error",e);
            }*/
        }else if(o.getCost()>stat.getAfterQuickCost()){
            //if it is normal task
            //if min cost queue is not slow queue and exist slow task then move slow task to slow queue
            if(minCostQueueNotFastAndSlowIndex!=slowQueueIndex) {
                TaskData mc = queues.get(minCostQueueNotFastAndSlowIndex).getMaxCostTask();
                if (null != mc) {
                    if (mc.getCost() > stat.getBeginSlowCost()) {
                        if(queues.get(minCostQueueNotFastAndSlowIndex).remove(mc)) {
                            log.debug(" queue "+minCostQueueNotFastAndSlowIndex+" remove task "+mc.getCost());
                            try {
                                synchronized (mc) {
                                    queues.get(slowQueueIndex).put(mc);
                                    log.debug("3 queue "+slowQueueIndex+" add task "+mc.getCost()+" slow "+stat.getBeginSlowCost()+" quick "+stat.getAfterQuickCost());
                                    processingDataCaches.put(mc.getId(), mc);
                                }
                            }catch (Exception e){
                                log.error("put queue error",e);
                            }
                        }
                    }
                }
            }
            //put normal task to min cost queue exclude quick queue
            try {
                //System.out.println("-P:"+(minCostQueueIndexNotQuickIndex+" - "+((TaskData)o).getCost()));
                synchronized (o) {
                    queues.get(minCostQueueNotFastAndSlowIndex).put(o);
                    log.debug("4 queue "+minCostQueueNotFastAndSlowIndex+" add task "+o.getCost()+" slow "+stat.getBeginSlowCost()+" quick "+stat.getAfterQuickCost());
                    processingDataCaches.put(o.getId(), o);
                }
            }catch(Exception e){
                log.error("put queue error",e);
            }
        }else{
            //if it quick task
            if(quickUseIndex!=slowQueueIndex) {
                TaskData mc = queues.get(quickUseIndex).getMaxCostTask();
                if (null != mc) {
                    if (mc.getCost() > stat.getBeginSlowCost()) {
                        if(queues.get(quickUseIndex).remove(mc)) {
                            log.debug(" queue "+quickUseIndex+" remove task "+mc.getCost());
                            try {
                                synchronized (mc) {
                                    queues.get(slowQueueIndex).put(mc);
                                    log.debug("5 queue "+slowQueueIndex+" add task "+mc.getCost()+" slow "+stat.getBeginSlowCost()+" quick "+stat.getAfterQuickCost());
                                    processingDataCaches.put(mc.getId(), mc);
                                }
                            }catch (Exception e){
                                log.error("put queue error",e);
                            }
                        }
                    }
                }
            }
            try {
                //System.out.println("-P:"+(minCostQueueIndex+" - "+((TaskData)o).getCost()));
                synchronized (o) {
                    queues.get(quickUseIndex).put(o);
                    log.debug("6 queue "+quickUseIndex+" add task "+o.getCost()+" slow "+stat.getBeginSlowCost()+" quick "+stat.getAfterQuickCost());
                    processingDataCaches.put(o.getId(), o);
                }
            }catch (Exception e){
                log.error("put queue error",e);
            }
        }


    }


    //一批task按cost 从小到大排序
    private List sortByCost(List<TaskData> data){
        Collections.sort(data, new Comparator(){

            @Override
            public int compare(Object o1, Object o2) {
                if(o1 instanceof TaskData && o2 instanceof TaskData ){
                    if(((TaskData) o1).isImportant || ((TaskData) o2).isImportant) return 0;
                    return ((TaskData) o1).getCost()>((TaskData) o2).getCost()?1:-1;
                }
                return 0;
            }
        });
        return data;
    }

    class GetDataTask extends  TimerTask{
        IDataGetter getter;
        AtomicBoolean can;
        public GetDataTask(IDataGetter dataGetter){
            this.getter=dataGetter;
        }
        @Override
        public void run() {
            if(!isStopReceiver && processingDataCaches.size()<cacheMaxSize) {
                if (null != getter) {
                    List dl = getter.getDataEachTime();
                    if(null != dl && dl.size()>0) {
                        receives(dl.toArray());
                    }

                }
            }
        }
    }

    class DoSlowTimeTask extends TimerTask{
        QPSManager qps;
        boolean isrun =false;
        public DoSlowTimeTask(QPSManager m){
            qps = m;
        }
        @Override
        public void run() {
            //not receive data in 3 seconds
            if(!isrun && System.currentTimeMillis()-qps.receiveTime>3000){
                isrun=true;
                int minCostQueueIndexNotQuickIndex=-1;
                isrun=true;
                while(queues.get(slowQueueIndex).size()>2 && minCostQueueIndexNotQuickIndex!=slowQueueIndex) {
                    long l = -1;
                    for (int i = 0; i < queues.size(); i++) {
                        long t = queues.get(i).getWaitingTime();
                        if (l == -1 || l > t) {
                            l = t;
                            if (i != quickQueueIndex) {
                                minCostQueueIndexNotQuickIndex = i;
                            }
                        }
                    }
                    if (minCostQueueIndexNotQuickIndex>0 && minCostQueueIndexNotQuickIndex != slowQueueIndex) {
                        TaskData td = queues.get(slowQueueIndex).getLast();
                        if (null != td) {
                            try {
                                queues.get(minCostQueueIndexNotQuickIndex).put(td);
                            }catch (Exception e){
                                log.error("put queue error",e);
                            }
                        }
                    }
                }
                isrun=false;
            }
        }
    }

    public boolean shutdown(){
        t.cancel();
        if(null != queues){
            for(Queue q :queues){
                q.shutdown();
            }
        }
        return true;
    }

    public boolean isAssigned() {
        return isAssigned;
    }

    public boolean isEmpty(){
        for(Queue q:queues){
            if(!q.isEmpty()){
                return false;
            }
        }
        return true;
    }

    public boolean waitingFinished(){
        if(null != dataGetter) {//停止采集任务
            dataGetter.stopReceive();
        }
        stopReceiver();//停止队列接收任务
        for(Queue q:queues){
            if(!q.waitingFinished()){
                return false;
            }
        }
        return true;
    }
}
