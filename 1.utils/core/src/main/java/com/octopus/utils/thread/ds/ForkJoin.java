package com.octopus.utils.thread.ds;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * User: Administrator
 * Date: 14-12-3
 * Time: 下午6:32
 */
public class ForkJoin {
	Log logger = LogFactory.getLog(ForkJoin.class);
    ExecutorService executorService;
    boolean executorManagedBySelf = false;
    TreeMap<Integer,Future<Object>> fs = new TreeMap<Integer,Future<Object>>();
    AtomicInteger counter = new AtomicInteger(0);
    List<String> ignoreExceptionCodeList  =  new ArrayList<String>();
    List ret = new ArrayList();
    List successList = new ArrayList();
    boolean isSuccess;
    List<Exception> errorList = new ArrayList<Exception>();
    long l;
    public ForkJoin(int count){
        //l = System.currentTimeMillis();
    	if(count > 0){
    		executorService= Executors.newFixedThreadPool(count);
    		executorManagedBySelf = true;
    	}
    }
    /*public ForkJoin(){
        l = System.currentTimeMillis();
    	//executorService= SpringBeanService.getBean("taskExecutor",ExecutorService.class);
    }*/
    public void submit(Runnable runnable){
        Future f = executorService.submit(runnable);
        fs.put(counter.addAndGet(1), f);
    }
    public void submit(Callable callable){
        Future f = executorService.submit(callable);
        fs.put(counter.addAndGet(1), f);
    }
    
    /**
     * 增加忽略异常的Code，例如，在某实例中需要忽略CRM-1045，则在通过此方法增加忽略Code
     * forkJoin.addIgnoreExceptionCode("CRM-1045");
     * 这样异步执行中如果捕获到5001的"CRM-1045"异常则忽略，继续其他的并发执行，如果非此异常则在join时会抛出异步线程执行的异常信息
     * @param codes
     */
    public void addIgnoreExceptionCode(String... codes){
    	for (String code : codes) {
    		ignoreExceptionCodeList.add(code);
		}
    }

    public void shutdown(){
        executorService.shutdown();
    }
    public void join(){
        if(fs.size()>0){
            Iterator its = fs.keySet().iterator();
            int n=0;
            while(its.hasNext()){
                try{
                    Future<Object> f = fs.get(its.next());
                    ret.add(f.get());
                    successList.add(f.get());
                    n++;
                }catch (Exception e) {
                	 errorList.add(new Exception("5000|There's an error in ForkJoin Thread ",e));
				}
            }
            if(n==fs.size()){
                isSuccess=true;
            }else{
                isSuccess=false;
            }
            if(executorManagedBySelf)
            	executorService.shutdown();

        }
    }
    public List getResult(){
        return ret;
    }

	public List getSuccessResult() {
		return successList;
	}

	public List<Exception> getErrorResult() {
		return errorList;
	}
	
}

