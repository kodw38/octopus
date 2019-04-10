package com.octopus.isp.actions;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Administrator on 2018/2/27.
 */
public class RunMainAction extends XMLObject {
    static transient Log log = LogFactory.getLog(RunMainAction.class);

    public RunMainAction(XMLMakeup xml, XMLObject parent, Object[] containers) throws Exception {
        super(xml, parent, containers);

    }

    @Override
    public void notifyObject(String op, Object obj) throws Exception {

    }

    @Override
    public void addTrigger(Object cond, InvokeTaskByObjName task) throws Exception {

    }

    @Override
    public void destroy() throws Exception {

    }

    @Override
    public void initial() throws Exception {
        XMLMakeup xml = getXML();
        if(null != xml) {
            XMLMakeup[] ms = xml.getChild("main");
            if (null != ms && ms.length>0) {
                for(XMLMakeup m:ms){
                    String javahome = m.getFirstCurChildText("jdkhome");
                    String excludejars = m.getFirstCurChildText("excludejars");
                    List exclude=null;
                    if(StringUtils.isNotBlank(excludejars)){
                        exclude= Arrays.asList(excludejars.split(","));
                    }
                    String externaljars = m.getFirstCurChildText("externaljars");
                    List external=null;
                    if(StringUtils.isNotBlank(externaljars)){
                        external= Arrays.asList(externaljars.split(","));
                    }
                    String opts = m.getFirstCurChildText("opts");
                    String mainClass = m.getFirstCurChildText("mainClass");
                    String args = m.getFirstCurChildText("args");
                    String logpath = m.getFirstCurChildText("logpath");
                    ExecutorUtils.work(new MainRun(javahome,opts,mainClass,args,external,exclude,logpath));
                }
            }
        }


    }
    class MainRun implements Runnable{
        String javahome,opts,mainClass,args,logPath;
        List excludeJars,externalJars;
        public MainRun(String javahome,String opts,String mainClass,String args,List externalJars,List excludeJars,String logPath){
            this.javahome=javahome;
            this.opts=opts;
            this.mainClass=mainClass;
            this.args=args;
            this.excludeJars=excludeJars;
            this.externalJars=externalJars;
            this.logPath=logPath;
        }
        @Override
        public void run() {
            try {
                ExecutorUtils.runMain(javahome, null, opts, mainClass, args, externalJars, excludeJars, logPath);
            } catch (Exception e) {
                log.error("runMain error",e);
            }
        }
    }
}
