package com.octopus.isp.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.Bus;
import org.apache.cxf.configuration.jsse.TLSServerParameters;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory;
import org.apache.cxf.transport.http_jetty.JettyHTTPServerEngineFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.HashMap;

/**
 * 包装cxf发布的ws服务，包括：
 * 客户端
 * 1. ws服务的获取，包装成XMLDoObject服务
 * 2. XMLDoObject服务调用转化到cxf ws的调用
 * 服务端
 * 1. 可以把XMLDoObject服务发布成ws服务
 * Created by kod on 2017/2/16.
 */
public class CXFUtil {
    transient static Log log = LogFactory.getLog(CXFUtil.class);
    //static HashMap<String,ServerFactoryBean> cache = new HashMap<String, ServerFactoryBean>();
    static HashMap<String,JaxWsServerFactoryBean> cache = new HashMap<String, JaxWsServerFactoryBean>();
    /**
     * 远程调用CXF WebService
     * @param wsdlpath
     * @param method
     * @param parameters
     * @return
     * @throws Exception
     */
    public static Object invoke(String wsdlpath,String method,Object... parameters) throws Exception {
        JaxWsDynamicClientFactory dcf = JaxWsDynamicClientFactory.newInstance();
        org.apache.cxf.endpoint.Client client = dcf.createClient(wsdlpath);
        return client.invoke(method, parameters);
    }
    static TLSServerParameters tlsParams;
    public static synchronized void addService(String host,String wsName,Class interfaceClass,Class implClass,String keypath,String password,String storePassword)throws Exception{
        try {
            //ServerFactoryBean svrFactory = new ServerFactoryBean();
            JaxWsServerFactoryBean svrFactory = new JaxWsServerFactoryBean();
            if(host.startsWith("https") && null == tlsParams){
                Bus bus = svrFactory.getBus();
                JettyHTTPServerEngineFactory serverEngineFactory = bus.getExtension(JettyHTTPServerEngineFactory.class);
                File file = new File(keypath);
                tlsParams = new TLSServerParameters();
                KeyStore keyStore = KeyStore.getInstance("JKS");
                //String password = "treasure bag";
                //String storePassword = "treasure bag";

                FileInputStream is = new FileInputStream(file);
                keyStore.load(is, storePassword.toCharArray());
                is.close();

                KeyManagerFactory keyFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyFactory.init(keyStore, password.toCharArray());
                KeyManager[] keyManagers = keyFactory.getKeyManagers();
                tlsParams.setKeyManagers(keyManagers);

                TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustFactory.init(keyStore);
                TrustManager[] trustManagers = trustFactory.getTrustManagers();
                tlsParams.setTrustManagers(trustManagers);
                String port = host.substring(host.lastIndexOf(":")+1);
                serverEngineFactory.setTLSServerParametersForPort(Integer.parseInt(port), tlsParams);
            }
            if(!cache.containsKey(wsName)) {
                svrFactory.setServiceClass(interfaceClass);

                String url = host + "/" + wsName;
                svrFactory.setAddress(url);
                //svrFactory.getInInterceptors().add(new ReadSoapHeader());
                svrFactory.getInInterceptors().add(new CXFInInterceptor());
                svrFactory.getOutInterceptors().add(new CXFOutInterceptor());

                svrFactory.setServiceBean(implClass.newInstance());
                //svrFactory.getFeatures().add(new LoggingFeature());
                svrFactory.create();
                cache.put(wsName, svrFactory);
                log.info("cxf add service " + host + "/" + wsName);

            }

        }catch (Exception e){
            log.error("add web Service error:class:"+interfaceClass+" to "+host + "/" + wsName,e);
            throw e;
        }

    }
    public static void addService2(String host,String wsName,Class interfaceClass,Class implClass)throws Exception{
        try {
            JaxWsServerFactoryBean jwsFactory = new JaxWsServerFactoryBean();
            String url = host + "/" + wsName;
            jwsFactory.setAddress(url);

            //jwsFactory.getInInterceptors().add(new CXFInInterceptor());
            //jwsFactory.getOutInterceptors().add(new CXFOutInterceptor());

            jwsFactory.setServiceClass(interfaceClass);
            Object o = implClass.newInstance();

            jwsFactory.setServiceBean(o);
            jwsFactory.create();
            log.info("cxf add service "+host+"/"+wsName);
            cache.put(wsName,jwsFactory);
        }catch (Exception e){
            log.error("add web Service error:",e);
            throw e;
        }
    }
    public static void deleteService(String wsName){
        try {
            if(cache.containsKey(wsName)){
                cache.get(wsName).setStart(false);
                cache.remove(wsName);
            }

        }catch (Exception e){
            log.error("add web Service error:",e);
        }
    }


    public static void parseWSDLByNewAddress(String wsdlAddress){

    }


}
