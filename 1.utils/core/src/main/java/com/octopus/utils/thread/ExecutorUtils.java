package com.octopus.utils.thread;

import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.BooleanUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.bftask.BFParameters;
import com.octopus.utils.cls.ClassUtils;
import com.octopus.utils.exception.Logger;
import com.octopus.utils.thread.ds.*;
import com.octopus.utils.time.DateTimeUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import com.octopus.utils.xml.auto.defpro.impl.utils.ConcurrenceExeRun;
import com.octopus.utils.xml.auto.defpro.impl.utils.DoAction;
import com.octopus.utils.xml.auto.defpro.impl.utils.DoTimeTask;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 需要提供定时，循环执行、分线程执行、协同执行、依赖执行
 * User: Administrator
 * Date: 14-8-23
 * Time: 下午6:36
 */
public class ExecutorUtils {
    static transient Log log = LogFactory.getLog(ExecutorUtils.class);
    /**
     * 异步单线程执行
     * @param impl
     * @param methodName
     * @param parclasses
     * @param pars
     */
    public static void work(Object impl,String methodName,Class[] parclasses,Object[] pars){
        if(null != impl){
            Executor[] es = ThreadPool.getInstance().getExecutor(1);
            es[0].execute(new InvokeTask(impl,methodName,parclasses,pars));
        }
    }
    public static void work(final Runnable runnable,int afterMillStart){
        Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                runnable.run();
            }
        },new Date(new Date().getTime()+afterMillStart));
    }

    static Map<String,Timer> timerManager = new HashMap();
    public static void delayWork(String key,Object obj,String methodName,Class[] parclasses,Object[] pars,int milliseconds){
        if(StringUtils.isNotBlank(key)) {
            if (timerManager.containsKey(key)){
                timerManager.get(key).cancel();
            }
            timerManager.put(key,new Timer());
            timerManager.get(key).schedule(new DoTimeTask(obj, methodName, parclasses, pars), milliseconds);
        }
    }

    /**
     * 执行时如果超时，返回timeout异常
     * @param impl
     * @param methodName
     * @param parclasses
     * @param pars
     * @param timeout
     */
    public static void synWork(Object impl,String methodName,Class[] parclasses,Object[] pars,int timeout) throws InterruptedException, ExecutionException, TimeoutException {
        if(null != impl){
            ExecutorService exc = ThreadPool.getInstance().getExecutorService(1);

            InvokeTask task = new InvokeTask(impl,methodName,parclasses,pars);
            Future f = exc.submit(task);
            f.get(timeout, TimeUnit.SECONDS);

        }
    }
    public static ThreadPool.MyExecutor getMyExecutor(){
        return (ThreadPool.MyExecutor)ThreadPool.getInstance().getExecutor();
    }

    public static Thread[] findAllThreads() {
        ThreadGroup group = Thread.currentThread().getThreadGroup();
        ThreadGroup topGroup = group;

        // 遍历线程组树，获取根线程组
        while ( group != null ) {
            topGroup = group;
            group = group.getParent();
        }
        // 激活的线程数加倍
        int estimatedSize = topGroup.activeCount() * 2;
        Thread[] slackList = new Thread[estimatedSize];
        //获取根线程组的所有线程
        int actualSize = topGroup.enumerate(slackList);
// copy into a list that is the exact size
        Thread[] list = new Thread[actualSize];
        System.arraycopy(slackList, 0, list, 0, actualSize);
        return list;
    }

    public static Object synWork(Object impl,String methodName,Class[] parclasses,Object[] pars)throws Exception{
        if(null != impl){
            return ClassUtils.invokeMethod(impl,methodName,parclasses,pars);
        }
        return null;
    }


    static String TriggerGroupName="tgroup";
    static String JobGroupName="jgroup";
    static SchedulerFactory schedulerFactory =null;
    public static void workTimeTask(Map env,String[] exp,Object impl,String methodName,Class[] parclasses,Object[] pars,boolean isConcurrent)throws Exception{
        if(null == schedulerFactory && null!=env){
            Properties p = new Properties();
            p.putAll(env);
            schedulerFactory = new StdSchedulerFactory(p);
        }
        InvokeTask task = new InvokeTask(impl,methodName,parclasses,pars);
        try{
            for(int i=0;i<exp.length;i++){
                if(CronExpression.isValidExpression(exp[i])){
                    Scheduler scheduler = schedulerFactory.getScheduler();
                    String triggerid = null;
                    if(impl instanceof XMLObject){
                        triggerid = ((XMLObject)impl).getXML().getId();
                    }else if(impl instanceof DoAction){
                        triggerid=(String)((DoAction)impl).getXml().getId();
                    }else{
                        triggerid=""+impl.hashCode();
                    }
                    triggerid+=i;
                    JobDetail detail=null;
                    JobDetail old = scheduler.getJobDetail(triggerid,JobGroupName);
                    if(null != old){
                        log.info("remove old scheduler id:"+triggerid+" group:"+JobGroupName);
                        scheduler.deleteJob(triggerid,JobGroupName);
                    }
                    if(isConcurrent)
                        detail = new JobDetail(triggerid,JobGroupName,RunnableJob.class);
                    else
                        detail = new JobDetail(triggerid,JobGroupName,RunnableStatefulJob.class);
                    CronTrigger cronTrigger = new CronTrigger(triggerid, TriggerGroupName);
                    cronTrigger.getJobDataMap().put("Runnable",task);
                    CronExpression cexp = new CronExpression(exp[i]);
                    cronTrigger.setCronExpression(cexp);
                    scheduler.scheduleJob(detail, cronTrigger);
                    scheduler.start();
                    if(log.isInfoEnabled()){
                        log.info("add new cron task ["+triggerid+"] ["+exp[i]+"]");
                    }
                }else{
                    System.out.println("error cornExpression:"+exp[i]);
                }
            }
        }catch (Exception e){
            log.error("expression:"+ArrayUtils.toJoinString(exp),e);

        }

    }


    public static void clearAllWorkTimeTasks(String[] ext) throws SchedulerException {
        if(null != schedulerFactory) {
            Iterator its = schedulerFactory.getAllSchedulers().iterator();
            while (its.hasNext()) {
                Scheduler scheduler = (Scheduler) its.next();
                if (null != ext) {
                    String[] ts = scheduler.getJobNames(JobGroupName);
                    for (String t : ts) {
                        if (!ArrayUtils.isLikeStringArray(ext, t)) {
                            scheduler.deleteJob(t, JobGroupName);
                        }
                    }
                } else {
                    scheduler.shutdown();
                }
            }
        }
    }
    public static Map<String,Map> getWorkTimeTaskInfo() throws SchedulerException {
        Iterator its = schedulerFactory.getAllSchedulers().iterator();
        HashMap<String,Map> ret = new HashMap();
        while(its.hasNext()){
            Scheduler scheduler = (Scheduler)its.next();
            String[] ts = scheduler.getJobNames(JobGroupName);
            for(String t:ts){
                JobDetail jd = scheduler.getJobDetail(t,JobGroupName);
                Trigger[] tt = scheduler.getTriggersOfJob(t,JobGroupName);
                if(null != tt && tt.length>0){
                    if(!ret.containsKey(t)) ret.put(t,new HashMap());
                    String nextTime=null;
                    Date startTime=null;
                    String cronExp=null;
                    for(Trigger t1:tt) {
                        if(null ==startTime)
                            startTime = t1.getStartTime();
                        String nexttime="";
                        if(null != t1.getNextFireTime()){
                            nexttime = DateTimeUtils.DATA_FORMAT_YYYY_MM_DD_HH_MM_SS.format(t1.getNextFireTime());
                        }
                        if(null==nextTime){
                            nextTime= nexttime;
                        }else{
                            nextTime+=","+nexttime;
                        }
                        if (t1 instanceof CronTrigger) {
                            if(null==cronExp) {
                                cronExp = ((CronTrigger) t1).getCronExpression();
                            }else{
                                cronExp+=","+((CronTrigger) t1).getCronExpression();
                            }
                        }
                    }
                    ret.get(t).put("NextFireTime", nextTime);
                    ret.get(t).put("StartTime", startTime);
                    ret.get(t).put("CronExpression", cronExp);
                }
            }
        }
        return ret;
    }

    public static void removeWorkTimeTask(String id)throws SchedulerException{
        if(null !=schedulerFactory) {
            Iterator its = schedulerFactory.getAllSchedulers().iterator();
            while (its.hasNext()) {
                Scheduler scheduler = (Scheduler) its.next();
                if (StringUtils.isNotBlank(id)) {
                    String[] ts = scheduler.getJobNames(JobGroupName);
                    for (String t : ts) {
                        if (t.indexOf(id) >= 0) {
                            scheduler.deleteJob(t, JobGroupName);
                        }
                    }
                }
            }
        }
    }
    public static void work(Runnable runnable){
        if(null != runnable){
            ThreadPool.getInstance().getExecutor().execute(runnable);
        }
    }

    /**
     * 根据时间和线程数压测
     * @param threadCount
     * @param durSeconds
     * @param obj
     * @param method
     * @param cs
     * @param pars
     */
    public static void stressWork(AtomicLong counter,AtomicLong errorCounter,XMLDoObject notify,int threadCount,long durSeconds,Object obj,String method,Map actioninput,Class[] cs,Object[] pars){
        Executor[] es = ThreadPool.getInstance().getExecutor(threadCount);
        for(int i=0;i<threadCount;i++){
            StressTask invokeTasks=new StressTask(counter,errorCounter,notify,durSeconds,obj,method,cs,pars);
            es[i].execute(invokeTasks);
        }
    }

    /**
     * 根据线程数和总共处理的次数压测,返回结束消耗时间毫秒
     * @param threadCount
     * @param counter
     * @param obj
     * @param method
     * @param cs
     * @param pars
     */
    public static long stressWork(int threadCount,int counter,Object obj,String method,Map actioninput,Class[] cs,Object[] pars) throws Exception {
        long m = System.currentTimeMillis();
        ForkJoin fj = new ForkJoin(threadCount);
        //ThreadPool es = ThreadPool.getInstance().getThreadPool("stress_count",threadCount);
        for(int i=0;i<counter;i++){
            XMLParameter op = (XMLParameter)pars[0];
            Map env = (Map)pars[2];
            BFParameters np = new BFParameters();
            if(null != env) {
                Iterator its = env.keySet().iterator();
                while (its.hasNext()) {
                    String k = (String)its.next();
                    np.addGlobalParameter("${" + k + "}", env.get(k));
                }
            }
            fj.submit(new InvokeTask(obj, method, new Class[]{XMLParameter.class, XMLMakeup.class}, new Object[]{np, (XMLMakeup) pars[1]}));
            //es.getExecutor().execute(new InvokeTask(obj,method, new Class[]{XMLParameter.class, XMLMakeup.class}, new Object[]{np,(XMLMakeup)pars[1]}));
        }
        fj.join();
        fj.shutdown();

        //es.waitfinished();

        /*for(int i=0;i<counter;i++){
            XMLParameter op = (XMLParameter)pars[0];
            Map env = (Map)pars[2];
            BFParameters np = new BFParameters();
            if(null != actioninput){
                np.put("^${input}",actioninput);
            }
            if(null != env) {
                Iterator its = env.keySet().iterator();
                while (its.hasNext()) {
                    String k = (String)its.next();
                    np.put("${"+k+"}",env.get(k));
                }
            }
            ExecutorUtils.synWork(obj, method, new Class[]{XMLParameter.class, XMLMakeup.class}, new Object[]{np, (XMLMakeup) pars[1]});
        }*/
        return (System.currentTimeMillis()-m);
    }

    public static void exc(String cmd,String logpathName){
        try {
            Process process=null;
            log.info("command:"+cmd);
            if(StringUtils.isNotBlank(logpathName)) {
                cmd+=" >"+logpathName+" 2>&1";

            }
            if(System.getProperty("os.name").equals("Linux")) {
                String[] cmds = {"/bin/sh", "-c", cmd};
                process = Runtime.getRuntime().exec(cmds);
            }else{
                process = Runtime.getRuntime().exec(cmd);
            }
            InputStream in  = process.getErrorStream();
            BufferedReader strCon = new BufferedReader(new InputStreamReader(in));
            StringBuffer errmsg = new StringBuffer();
            String line;
            while ((line = strCon.readLine()) != null) {
                errmsg.append(line).append("\n");
            }
            if(errmsg.length()>0){
                log.info("command error result:"+errmsg.toString());
            }
            in  = process.getInputStream();
            strCon = new BufferedReader(new InputStreamReader(in));
            errmsg.delete(0,errmsg.length());
            while ((line = strCon.readLine()) != null) {
                errmsg.append(line).append("\n");
            }
            if(errmsg.length()>0){
                log.info("command result:"+errmsg.toString());
            }

            process.waitFor();
        }catch (Exception e){
            log.error("exc error",e);
        }
    }
    public static void exe(String[] cmd) throws IOException {
        //log.info(cmd);
        ProcessBuilder pb = new ProcessBuilder(cmd);

        Process process = pb.start();
        BufferedReader strCon = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = strCon.readLine()) != null) {
            System.out.println(line);
        }
    }
    public static void exc(String cmd){
        exc(cmd,null);
    }
    public static void runMain(String jdkHome,List<String> jars,String opts,String main,String args,List<String> AddJars,List<String> excludeJars,String logFile)throws Exception{
        List<String> ls=null;
        if(null == jars) {
            ls = ClassUtils.getLibJarPath(null);
        }else{
            ls = jars;
        }
        if(null != AddJars){
            ls.addAll(AddJars);
        }
        if(null != excludeJars){
            for(int i=ls.size()-1;i>=0;i--) {
                for (String ex : excludeJars) {
                    if(ls.get(i).contains(ex)){
                        ls.remove(i);
                    }
                }
            }
        }
        String cls =null;
        if(System.getProperty("os.name").contains("Windows")){
            cls = ArrayUtils.toString(ls,";");
        }else{
            cls = ArrayUtils.toString(ls,":");
        }
        String ja =null;
        if(StringUtils.isNotBlank(jdkHome)){
            ja = jdkHome+"/bin/java";
        }else if(StringUtils.isNotBlank(System.getProperty("JAVA_HOME"))){
            ja = System.getProperty("JAVA_HOME")+"/bin/java";
        }else{
            ja = System.getProperty("java.home");
            if(ja.endsWith("jre")){
                if(ja.indexOf("/")>=0) {
                    ja = ja.substring(0, ja.lastIndexOf("/")) + "/bin/java";
                }else{
                    ja = ja.substring(0, ja.lastIndexOf("\\")) + "/bin/java";
                }
            }else{
                ja = ja+"/bin/java";
            }
        }
        if(null == opts){
            opts="";
        }
        if(null == args){
            args="";
        }
        exc(ja + " " + opts + " -classpath \"" + cls + "\" " + main + " " + args, logFile);

    }
    public static ThreadPool getFixedThreadPool(String name,int count){
        //throw new RuntimeException("now not support thread pool");
        return ThreadPool.getInstance().getThreadPool(name,count);

    }

    public static LinkedBlockingQueue<Runnable> createThreadPoolQueue(int threadCount){
        ThreadPoolQueue tq = new ThreadPoolQueue(ThreadPool.getExecutorService(threadCount));
        return tq.getQueue();
    }

    public static Object workEachSamePar(Object[] objects,String name,Class[] pcs,Object[] pars)throws InterruptedException{
        InvokeTask[] invokeTasks = new InvokeTask[objects.length];
        for(int i=0;i<invokeTasks.length;i++){
            invokeTasks[i]=new InvokeTask(objects[i],name,pcs,pars);
        }
        return workEach(invokeTasks);
    }
    public static Object workEach(final InvokeTask[] invokeTasks)throws InterruptedException {
        if(null != invokeTasks){

            Executor exe = ThreadPool.getInstance().getExecutor();
            exe.execute(new Runnable(){
                @Override
                public void run() {
                    for(InvokeTask t:invokeTasks){
                        t.run();
                    }
                }
            });
            ((ThreadPool.MyExecutor)exe).join();
            return invokeTasks[invokeTasks.length-1].getResult();
        }
        return null;
    }

    public static Object[] multiWorkSameParWaiting(Object[] objs,String name,Class[] parclass,Object[] pars){
        if(null != objs){
            try{
                InvokeTask[] invokeTasks = new InvokeTask[objs.length];
                for(int i=0;i<invokeTasks.length;i++){
                    invokeTasks[i]=new InvokeTask(objs[i],name,parclass,pars);
                }
                Executor[] es = ThreadPool.getInstance().getExecutor(invokeTasks.length);
                for(int i=0;i<es.length;i++){
                    es[i].execute(invokeTasks[i]);
                }
                for(int i=0;i<es.length;i++){
                    ((ThreadPool.MyExecutor)es[i]).join();
                }
                Object[] rets = new Object[invokeTasks.length];
                for(int i=0;i<invokeTasks.length;i++){
                    if(invokeTasks[i].isSuccess()) {
                        rets[i] = invokeTasks[i].getResult();
                    }else{
                        rets[i]=invokeTasks[i].getException();
                    }
                }
                return rets;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return null;
    }

    public static void multiWorkSameMethodWaiting(Object obj,String name,Class[] parclass,List<Object[]> pars,int threadsCount){
        ThreadPool p = ThreadPool.getInstance().getThreadPool("multiWorkSameMethodWaiting_"+obj.hashCode(),threadsCount);
        for(int i=0;i<pars.size();i++){
            p.getExecutor().execute(new InvokeTask(obj, name, parclass, pars.get(i)));
        }
        p.waitfinished();
        ThreadPool.getInstance().returnThreadPool(p);

    }

    public static Object[] multiWorkSameParWaitingWithCachePool(Object[] objs,String name,Class[] parclass,Object[] pars){
        if(null != objs){
            try{
                InvokeTask[] invokeTasks = new InvokeTask[objs.length];
                for(int i=0;i<invokeTasks.length;i++){
                    invokeTasks[i]=new InvokeTask(objs[i],name,parclass,pars);
                }
                //Executor[] es = ThreadPool.getInstance().getExecutor(invokeTasks.length);
                Future[] fs = new Future[invokeTasks.length];
                for(int i=0;i<invokeTasks.length;i++){
                    fs[i] = cachedThreadPool.submit(invokeTasks[i]);
                }

                Object[] rets = new Object[invokeTasks.length];
                for(int i=0;i<fs.length;i++){
                    fs[i].get();
                    if(invokeTasks[i].isSuccess()) {
                        rets[i] = invokeTasks[i].getResult();
                    }else{
                        rets[i]=invokeTasks[i].getException();
                    }
                }
                return rets;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return null;
    }

    public static Object[] multiWorkSameObjectWaiting(Object objs,String name,Class[] parclass,List<Object[]> pars){
        if(null != objs){
            try{
                InvokeTask[] invokeTasks = new InvokeTask[pars.size()];
                for(int i=0;i<invokeTasks.length;i++){
                    invokeTasks[i]=new InvokeTask(objs,name,parclass,pars.get(i));
                }
                Executor[] es = ThreadPool.getInstance().getExecutor(invokeTasks.length);
                for(int i=0;i<es.length;i++){
                    es[i].execute(invokeTasks[i]);
                }
                for(int i=0;i<es.length;i++){
                    ((ThreadPool.MyExecutor)es[i]).join();
                }
                Object[] rets = new Object[invokeTasks.length];
                for(int i=0;i<invokeTasks.length;i++){
                    rets[i]=invokeTasks[i].getResult();
                }
                return rets;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return null;
    }
    public static Object[] multiWorkWaiting(InvokeTask[] invokeTasks) {
        if(null != invokeTasks){
            try{
                Executor[] es = ThreadPool.getInstance().getExecutor(invokeTasks.length);
                for(int i=0;i<es.length;i++){
                    es[i].execute(invokeTasks[i]);
                }
                for(int i=0;i<es.length;i++){
                    ((ThreadPool.MyExecutor)es[i]).join();
                }
                Object[] rets = new Object[invokeTasks.length];
                for(int i=0;i<invokeTasks.length;i++){
                    rets[i]=invokeTasks[i].getResult();
                }
                return rets;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return null;
    }
    public static void multiWorkWaiting(ConcurrenceExeRun[] invokeTasks) {
        if(null != invokeTasks){
            try{
                Executor[] es = ThreadPool.getInstance().getExecutor(invokeTasks.length);
                for(int i=0;i<es.length;i++){
                    es[i].execute(invokeTasks[i]);
                }
                for(int i=0;i<es.length;i++){
                    ((ThreadPool.MyExecutor)es[i]).join();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    /**
     * 执行并等待超时,超时返回false
     * @param timeoutMillSecond
     * @return
     */
    public static Object[] multiWorkWaiting(InvokeTask[] invokeTasks,int timeoutMillSecond,WaitingException activer)throws Exception{
        if(null != invokeTasks){
            Executor[] es = ThreadPool.getInstance().getExecutor(invokeTasks.length);
            for(int i=0;i<es.length;i++){
                es[i].execute(invokeTasks[i]);
            }
            synchronized (activer){
            if(timeoutMillSecond>0)
                activer.wait(timeoutMillSecond);
            else
                activer.wait();
            }
            if(activer.isException()){
                throw activer;
            }
            Object[] ret = new Object[invokeTasks.length];
            for(int i=0;i<invokeTasks.length;i++){
                ret[i]=invokeTasks[i].getResult();
            }
            return ret;
        }
        return null;
    }
    public static Object[] multiWorkSameParWaiting(Object[] objects,String name,Class[] pcs,Object[] pars,int timeoutMillSecond,WaitingException activer)throws Exception{

        if(null != objects){
            InvokeTask[] invokeTasks = new InvokeTask[objects.length];
            for(int i=0;i<invokeTasks.length;i++){
                invokeTasks[i]=new InvokeTask(objects[i],name,pcs,pars);
            }
            Executor[] es = ThreadPool.getInstance().getExecutor(invokeTasks.length);
            for(int i=0;i<es.length;i++){
                es[i].execute(invokeTasks[i]);
            }
            synchronized (activer){
            if(timeoutMillSecond>0)
                activer.wait(timeoutMillSecond);
            else
                activer.wait();
            }
            if(activer.isException()){
                throw activer;
            }
            Object[] ret = new Object[invokeTasks.length];
            for(int i=0;i<invokeTasks.length;i++){
                ret[i]=invokeTasks[i].getResult();
            }
            return ret;
        }
        return null;
    }

    public static void multiWork(InvokeTask[] invokeTasks){
        if(null != invokeTasks){
            Executor[] es = ThreadPool.getInstance().getExecutor(invokeTasks.length);
            for(int i=0;i<es.length;i++){
                es[i].execute(invokeTasks[i]);
            }
        }
    }
    public static void multiWork(ConcurrenceExeRun[] invokeTasks){
        if(null != invokeTasks){
            Executor[] es = ThreadPool.getInstance().getExecutor(invokeTasks.length);
            for(int i=0;i<es.length;i++){
                es[i].execute(invokeTasks[i]);
            }
        }
    }

    public static void multiWorkSamePar(Object[] objects,String name,Class[] pclass,Object[] pars){
        if(null != objects){
            InvokeTask[] invokeTasks= new InvokeTask[objects.length];
            for(int i=0;i<invokeTasks.length;i++){
                invokeTasks[i] = new InvokeTask(objects[i],name,pclass,pars);
            }
            Executor[] es = ThreadPool.getInstance().getExecutor(invokeTasks.length);

            for(int i=0;i<es.length;i++){
                es[i].execute(invokeTasks[i]);
            }
        }
    }
    static ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
    public static void multiWorkSameParWithCachePool(Object[] objects,String name,Class[] pclass,Object[] pars){
        if(null != objects){
            InvokeTask[] invokeTasks= new InvokeTask[objects.length];
            for(int i=0;i<invokeTasks.length;i++){
                invokeTasks[i] = new InvokeTask(objects[i],name,pclass,pars);
            }

            for(int i=0;i<invokeTasks.length;i++){
                cachedThreadPool.execute(invokeTasks[i]);
            }
        }
    }

    public static boolean teamWork2AllTrue(Object[] impls,String methodName,Class[] parClasses,Object[] pars){
        if(null != impls){
            InvokeTask[] its = new InvokeTask[impls.length];
            Thread[] ts = new Thread[impls.length];
            for(int i=0;i<ts.length;i++){
                its[i]=new InvokeTask(impls[i],methodName,parClasses,pars);
                ts[i] = new Thread(its[i]);
                ts[i].start();
            }
            for(int i=0;i<ts.length;i++){
                try {
                    ts[i].join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            for(InvokeTask t:its){
                if(!BooleanUtils.isTrue(t.getResult()))
                    return false;
            }
            return true;
        }
        return false;
    }

    public static void systemThreadPoolSynWork(Runnable runnable){

    }

    public static void teamWork(Runnable[] runnables) throws InterruptedException {
        List<Thread> li = new ArrayList();
        for(Runnable r:runnables){
            Thread t = new Thread(r);
            t.setDaemon(false);
            t.start();
            li.add(t);
        }
        for(Thread t:li){
            t.join();
        }
    }

    public static boolean isSubThread(String threadName){
        return threadName.contains(DefaultThreadFactory.ISP_THREAD_NAME_PREX);
    }
    /*
    public static void multWork(Runnable[] runnables){
        for(Runnable r:runnables){
            Thread t = new Thread(r);
            t.setDaemon(false);
            t.start();
        }
    }

    public static void systemThreadPoolWork(Runnable runnable){
        if (objPooledExecutor == null){
            synchronized (objPooledExecutor) {
                if (objPooledExecutor == null) {
                    objPooledExecutor = new ThreadPoolExecutor(SYSTEM_THREADPOOL_CoreSIZE,
                            SYSTEM_THREADPOOL_MaxiNumPoolSize,
                            SYSTEM_THREADPOOL_KeepAliveTime,
                            TimeUnit.SECONDS,
                    new LinkedBlockingDeque<Runnable>());

                }
            }
        }
        objPooledExecutor.execute(runnable);
    }
    */
}
