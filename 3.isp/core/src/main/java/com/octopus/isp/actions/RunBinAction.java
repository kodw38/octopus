package com.octopus.isp.actions;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;

/**
 * Created by Administrator on 2018/2/15.
 */
public class RunBinAction extends XMLDoObject {
    static transient Log log = LogFactory.getLog(RunBinAction.class);
    public RunBinAction(XMLMakeup xml, XMLObject parent, Object[] containers) throws Exception {
        super(xml, parent, containers);
    }

    @Override
    public void doInitial(){
        ExecutorUtils.work(new Runnable() {
            @Override
            public void run() {
                try {
                    /*SingleClassLoader cl = new SingleClassLoader("C:/Users/robai/Downloads/elasticsearch-6.1.1/lib", ElasticsearchAction.class.getClassLoader());
                    Class c = cl.loadClass("org.elasticsearch.bootstrap.Elasticsearch");
                    System.setProperty("elasticsearch","");
                    System.setProperty("es.path.home","C:\\Users\\robai\\Downloads\\elasticsearch-6.1.1");
                    System.setProperty("es.path.conf","C:\\Users\\robai\\Downloads\\elasticsearch-6.1.1\\config");
                    Policy.setPolicy(new AWSPolicy());
                    ClassUtils.invokeStaticMethod(c, "main", new Class[]{String[].class}, new Object[]{new String[]{""}});*/
                    XMLMakeup[] bins = getXML().getChild("bins")[0].getChild("bin");
                    for (XMLMakeup bin : bins) {
                        if (StringUtils.isNotBlank(bin.getText())) {
                            //String bin = home + "/bin/elasticsearch";
                        /*if (System.getProperty("os.name").contains("Windows")) {
                            bin += ".bat";
                        }*/
                            //log.info("executing command " + bin.getText());
                            ExecutorUtils.exc(bin.getText());
                            //log.info("has executed command " + bin.getText());
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("load elasticsearch error", e);
                }
            }
        });
    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return false;
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return null;
    }

    @Override
    public ResultCheck checkReturn(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return null;
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret, Exception e) throws Exception {
        return false;
    }
}
