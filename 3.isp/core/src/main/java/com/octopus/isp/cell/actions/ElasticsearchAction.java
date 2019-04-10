package com.octopus.isp.cell.actions;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cls.ClassUtils;
import com.octopus.utils.cls.jcl.SingleClassLoader;
import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.Permission;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.util.Map;

/**
 * Created by robai on 2018/1/4.
 */
public class ElasticsearchAction extends XMLDoObject{
    public ElasticsearchAction(XMLMakeup xml, XMLObject parent, Object[] containers) throws Exception {
        super(xml, parent, containers);
    }

    public void doInitial(){
        ExecutorUtils.work(new Runnable(){
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
                    for(XMLMakeup bin:bins) {
                        if(StringUtils.isNotBlank(bin.getText())) {
                            //String bin = home + "/bin/elasticsearch";
                        /*if (System.getProperty("os.name").contains("Windows")) {
                            bin += ".bat";
                        }*/
                            ExecutorUtils.exc(bin.getText());
                            log.info("has executed command " + bin);
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    log.error("load elasticsearch error",e);
                }
            }
        });
    }
    private static class AWSPolicy extends Policy {
        private final Policy defaultPolicy;

        public AWSPolicy() {
            super();
            defaultPolicy = Policy.getPolicy();
        }

        @Override
        public boolean implies(ProtectionDomain domain, Permission permission) {
            if (permission instanceof javax.management.MBeanTrustPermission) {
                return true;
            } else {
                return defaultPolicy.implies(domain, permission);
            }
        }
    }
    public static void main(String[] args){
        try{
            try {
                /*SingleClassLoader cl = new SingleClassLoader("C:/Users/robai/Downloads/elasticsearch-6.1.1/lib", ElasticsearchAction.class.getClassLoader());
                Class c = cl.loadClass("org.elasticsearch.bootstrap.Elasticsearch");
                System.setProperty("elasticsearch","");
                System.setProperty("es.path.home","C:\\Users\\robai\\Downloads\\elasticsearch-6.1.1");
                System.setProperty("es.path.conf","C:\\Users\\robai\\Downloads\\elasticsearch-6.1.1\\config");
                Policy.setPolicy(new AWSPolicy());
                Object m = c.getClassLoader().loadClass("java.lang.String");

                ClassUtils.invokeStaticMethod(c, "main", new Class[]{String[].class}, new Object[]{new String[]{"-d"}});
                */
                Process process = Runtime.getRuntime().exec("C:/Users/robai/Downloads/elasticsearch-6.1.1//bin/elasticsearch.bat");
                BufferedReader strCon = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = strCon.readLine()) != null) {
                    System.out.println(line);
                }
                while(true){
                    Thread.sleep(300);
                }
            }catch (Exception e){
                e.printStackTrace();
                log.error("load elasticsearch error",e);
            }
        }catch (Exception e)
        {
            e.printStackTrace();
        }
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
