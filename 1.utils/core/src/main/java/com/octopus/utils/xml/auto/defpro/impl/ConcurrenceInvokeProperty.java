package com.octopus.utils.xml.auto.defpro.impl;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.thread.ThreadPool;
import com.octopus.utils.thread.ds.InvokeTask;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import com.octopus.utils.xml.auto.defpro.IObjectInvokeProperty;
import com.octopus.utils.xml.auto.defpro.impl.utils.ConcurrenceQueueTask;
import com.octopus.utils.xml.auto.defpro.impl.utils.ConcurrenceWaitTask;
import com.octopus.utils.xml.auto.logic.XMLLogic;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 给定执行数量，没有达到执行数量时，改任务一直等待，当达到执行数量时，往下执行
 * User: Administrator
 * Date: 15-1-12
 * Time: 上午9:37
 */
public class ConcurrenceInvokeProperty implements IObjectInvokeProperty {
    static transient Log log = LogFactory.getLog(ConcurrenceInvokeProperty.class);

    @Override
    public Object exeProperty(Map proValue, XMLDoObject obj,XMLMakeup xml, XMLParameter parameter,Map input, Map output,Map config) {
        //queue concurrent
        Object num = proValue.get("threadnum");
        String size = (String)proValue.get("size");
        if(null != proValue.get("queue") && StringUtils.isNotBlank((String)proValue.get("queue"))){
            if(proValue.get("queue").equals("block")){
                String id = Thread.currentThread().getName()+"_queue_"+xml.getId();
                LinkedBlockingQueue queue= (LinkedBlockingQueue)parameter.getParameter(id);
                if(null == queue){
                    int i_size=0;
                    if(StringUtils.isNotBlank(size)){
                        i_size = Integer.parseInt(size);
                    }
                    if(i_size>0)
                        queue = new LinkedBlockingQueue(i_size);
                    else
                        queue = new LinkedBlockingQueue();
                    int threadnum=0;
                    ThreadPool threadPool=null;
                    if(null != num){
                        threadnum=(Integer)num;
                        threadPool=ExecutorUtils.getFixedThreadPool(xml.getId(),threadnum);
                        new ConcurrenceQueueTask(queue,threadPool);
                    }
                    parameter.addParameter(id, queue);
                }
                try {
                    queue.put(new Object[]{obj,parameter,input,output,config,parameter.getParameter(XMLParameter.XMLLOGIC_BACK_CALL_KEY),xml});
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }else if(StringUtils.isNotBlank(num) && StringUtils.isNotBlank(size)){
            String xmlid = xml.getId();
            String id = Thread.currentThread().getName()+"."+Thread.currentThread().hashCode()+"_concurrentwait_"+xmlid;

                ConcurrenceWaitTask list = (ConcurrenceWaitTask) parameter.getParameter(id);
                if (null == list) {
                    Object wait = proValue.get("iswait");
                    boolean iswait = false;
                    if (wait instanceof String)
                        iswait = StringUtils.isTrue((String) proValue.get("iswait"));
                    else
                        iswait = (Boolean) wait;
                    int b_num = 0;
                    if (null != num) {
                        b_num = Integer.parseInt(String.valueOf(num));
                    }
                    int b_size = 0;
                    if (StringUtils.isNotBlank(size)) {
                        b_size = Integer.parseInt(size);
                    }
                    if (b_size > 0) {
                        list = new ConcurrenceWaitTask(xml.getId(),b_size, b_num, iswait);
                        list.addFinishedListener(new ClearFun(parameter, id));
                        parameter.addParameter(id, list);
                        list.start();
                    } else {
                        log.error("concurrence property size is null." + xml.toString());
                    }
                }
                if (null != list)
                    list.put(new Object[]{obj, parameter, input, output, config, parameter.getParameter(XMLParameter.XMLLOGIC_BACK_CALL_KEY), xml});


        }else{
            boolean iswait = StringUtils.isTrue((String) proValue.get("iswait"));
            if(obj instanceof XMLLogic){
                XMLLogic lg = (XMLLogic)obj;
                InvokeTask[] ts = null;
                List<XMLMakeup> xmls = lg.getXML().getChildren();
                if(null != xmls && xmls.size()>0){
                    ts = new InvokeTask[xmls.size()];
                    for(int i=0;i<ts.length;i++){
                        ts[i]=new InvokeTask(lg,"doElement",new Class[]{XMLParameter.class,XMLMakeup.class},new Object[]{parameter,xmls.get(i)});
                    }
                    if(iswait){
                       ExecutorUtils.multiWorkWaiting(ts);
                    }else{
                       ExecutorUtils.multiWork(ts);
                    }
                }
            }

        }
        return null;
    }



    @Override
    public boolean isAsyn() {
        return true;
    }


    class ClearFun implements Runnable{
        XMLParameter env;
        String id;
        String threadName;
        public ClearFun(XMLParameter env,String id){
            this.env=env;
            this.id=id;
            threadName=Thread.currentThread().getName();
        }

        @Override
        public void run() {
            env.removeParameter(threadName,id);
        }
    }
}
