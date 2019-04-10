package com.octopus.utils.thread.ds;

import com.octopus.isp.bridge.launchers.impl.pageframe.SessionManager;
import com.octopus.isp.ds.Session;
import com.octopus.utils.bftask.BFParameters;
import com.octopus.utils.cls.ClassUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by kod on 2017/5/5.
 */
public class StressTask implements Runnable {
    transient static Log log = LogFactory.getLog(StressTask.class);
    Object impl;
    String methodName;
    Class[] parsClass;
    Object[] pars;
    XMLDoObject notify;
    AtomicLong counter;
    AtomicLong errorCounter;
    long secs;
    public StressTask(AtomicLong counter,AtomicLong errorCounter,XMLDoObject notify,long durSeconds,Object impl,String methodName,Class[] parsClass,Object[] pars){
        this.impl=impl;
        this.methodName=methodName;
        this.pars=pars;
        this.parsClass=parsClass;
        secs = durSeconds;
        this.notify = notify;
        this.counter=counter;
        this.errorCounter=errorCounter;
    }
    @Override
    public void run() {
        long l = System.currentTimeMillis();
        if(secs>0) {
            log.debug("will run within "+secs+" seconds");
            long n=0;
            XMLParameter op = (XMLParameter)pars[0];
            startNotify(l,op);
            while (true) {
                try {
                    if((System.currentTimeMillis()-l)>secs*1000){
                        break;
                    }

                    Map env = (Map)pars[2];
                    BFParameters np = new BFParameters();
                    if(null != env) {
                        Iterator its = env.keySet().iterator();
                        while (its.hasNext()) {
                            String k = (String)its.next();
                            np.put("${"+k+"}",env.get(k));
                        }
                    }
                    ClassUtils.invokeMethod( this.impl, methodName, new Class[]{XMLParameter.class, XMLMakeup.class}, new Object[]{np,(XMLMakeup)pars[1]});
                    n++;
                    counter.incrementAndGet();
                } catch (Exception e) {
                    errorCounter.incrementAndGet();
                    log.error("", e);
                }
            }
            stopNotify();
            log.error("this exe count:" + n);
        }
    }
    Timer timer = new Timer();
    Session session=null;
    SessionManager sm = null;
    static boolean globalFlag=false;
    void startNotify(long l,XMLParameter op){
        try {
            if(!globalFlag) {
                globalFlag=true;
                session = (Session) op.get("${session}");
                log.debug(session);
                sm = (SessionManager) op.getParameter("${sessionManager}");
                timer.schedule(new TimeTask(op, l, secs, counter, errorCounter, session, secs), 0, 1000);
            }

        }catch (Exception e){
            log.error("",e);
        }
    }
    void stopNotify(){
        try {
            Thread.sleep(5000);
        }catch(Exception e){}
        timer.cancel();
        session=null;
        globalFlag=false;
        sm=null;
    }
    class TimeTask extends TimerTask {
        AtomicLong counter;
        AtomicLong errorCounter;
        long currentTime,limitTime;
        XMLParameter op;
        Session session;
        long secs;
        public TimeTask(XMLParameter op,long currenttime,long limittime,AtomicLong counter,AtomicLong errorCounter,Session session,long secs){
            this.currentTime=currenttime;
            this.limitTime=limittime;
            this.counter=counter;
            this.op=op;
            this.session=session;
            this.secs=secs;
            this.errorCounter=errorCounter;
        }
        @Override
        public void run() {
            try {
                //HashMap map = new HashMap();
                //map.put("data", "{LeftTime:"+(secs-(System.currentTimeMillis()-currentTime)/1000)+",Count:"+counter.longValue() + "}");
                long left = (secs-(System.currentTimeMillis()-currentTime)/1000);
                if(left<=1)left=0;
                String msg = "{\"LeftTime\":"+left+",\"SuccessCount\":"+counter.longValue() + ",\"ErrorCount\":"+errorCounter.longValue()+"}";
                if (null != sm) {
                    List<Session> ss =  sm.getSessions();
                    if(null != ss){
                        for(Session si:ss) {
                            if(null != si && si.getUserName().equals(session.getUserName())){
                                Object remote = si.get("websocket-remote");
                                if (null != remote) {
                                    if (remote instanceof RemoteEndpoint) {
                                        try {
                                            ((RemoteEndpoint) remote).sendString(msg);
                                        }catch (Exception e){
                                            //e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                /*Object remote =session.get("websocket-remote");

                log.debug(msg);
                if (null != remote) {
                    if (remote instanceof RemoteEndpoint) {
                        ((RemoteEndpoint) remote).sendString( msg);
                    }
                }else{
                    log.info("websocket-remote not exist");
                }
*/
            }catch (Exception e){
                log.error("",e);
            }
        }
    }
}
