package com.octopus.utils.file;

import com.google.googlejavaformat.java.Formatter;
import com.octopus.tools.projecttools.restructure.JavaForm;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.StringUtils;
import junit.framework.TestCase;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public class TestFileUtils extends TestCase {

    public static  void main(String[] args){
        /*Map chgs = new LinkedHashMap();
        chgs.put("mmmm","111111111");
        chgs.put("12322222222222","11");
        chgs.put("1",null);
        Map m = ArrayUtils.sortMapByKeyLength(chgs,ArrayUtils.DESC);
        System.out.println(m);*/
        try {
            //StringBuffer sb = FileUtils.getFileContentStringBuffer("C:\\work\\flyingserver_aigit\\flyingserver-app-repository\\nucleus\\security\\services\\src\\main\\java\\org\\glassfish\\security\\services\\common\\SecureServiceAccessPermission.java");
            //String formattedSource = new Formatter().formatSource(sb.toString());
            //System.out.println(formattedSource);

            //String s = "TypeVariable/*<?>*/[] formals = rawType.getTypeParameters();";
            //String ss = StringUtils.replace(s, "/*", "*/", "");
//System.out.print(ss);

            //String s = FileUtils.getFileContentString("C:\\work\\flyingserver_aigit\\flyingserver-app-repository\\nucleus\\admin\\util\\src\\main\\java\\com\\sun\\enterprise\\admin\\remote\\RemoteRestAdminCommand.java");
            //StringBuffer sb =FileUtils.deleteComments(new StringBuffer(s));
            //System.out.println(sb);
            FileUtils.copyFiles(new File("C:\\work\\flyingserver_aigit\\flyingserver\\flyingserver-core_2")
                    ,new File("C:\\work\\flyingserver_aigit\\flyingserver\\temp"),new String[]{"pom.xml"});
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    public void testCountJavaFileInDir(){
        try {
            int n = FileUtils.countFileInDir("C:\\work\\flying_server_ui\\web",new String[]{"css"});
            System.out.println("源代码共有:"+n);
            //api 221
            //app 13007+4207
            //package 4
            //redis 7
            //ui html:5 js:568 css:8
            //共18027个源文件
        }catch (Exception e){

        }
    }

    public void testCountClassInJarDir(){
        try{
            String[] ss = new String[]{"antlr:antlr:2.7.7","aopalliance:aopalliance:1.0","bouncycastle:bcprov-jdk15:136","ch.qos.logback:logback-classic:1.2.3","ch.qos.logback:logback-core:1.2.3","com.alibaba:fastjson:1.2.74","com.beust:jcommander:1.64","com.fasterxml.jackson.core:jackson-annotations:2.9.0","com.fasterxml.jackson.core:jackson-annotations:2.9.8","com.fasterxml.jackson.core:jackson-core:2.9.8","com.fasterxml.jackson.core:jackson-databind:2.9.8","com.fasterxml.jackson.module:jackson-module-jaxb-annotations:2.9.8","com.fasterxml.woodstox:woodstox-core:5.1.0","com.fasterxml:classmate:1.3.4","com.google.guava:guava:18.0","com.google.inject:guice:no_aop:4.0","com.ibm.jbatch:com.ibm.jbatch.container:1.0.2","com.ibm.jbatch:com.ibm.jbatch.spi:1.0.2","com.javax0.license3j:license3j:3.1.5","com.squareup.okhttp3:okhttp:4.3.1","com.squareup.okio:okio:2.4.1","com.sun.activation:jakarta.activation:2.0.0-RC3","com.sun.jsftemplating:jsftemplating:2.1.3","com.sun.jsftemplating:jsftemplating-dt:2.1.3","com.sun.mail:jakarta.mail:2.0.0-RC5","com.sun.mail:javax.mail:1.6.2","com.sun.messaging.mq:imqjmx:4.3","com.sun.woodstock.dependlibs:dataprovider:1.0","com.sun.woodstock.dependlibs:dojo-ajax-nodemo:1.12.4","com.sun.woodstock.dependlibs:json:2.0","com.sun.woodstock.dependlibs:prototype:1.7.3","com.sun.woodstock:webui-jsf:4.0.2.15","com.sun.woodstock:webui-jsf-suntheme:4.0.2.15","com.sun.xml.bind:jaxb-core:3.0.0-M3","com.sun.xml.bind:jaxb-impl:3.0.0-M3","com.sun.xml.bind:jaxb-osgi:2.3.1","com.sun.xml.bind:jaxb-osgi:3.0.0-M3","com.sun.xml.fastinfoset:FastInfoset:2.0.0-M2","com.sun.xml.messaging.saaj:saaj-impl:2.0.0-M1","com.sun.xml.stream.buffer:streambuffer:2.0.0-M2","com.sun.xml.ws:jaxws-rt:3.0.0-M3","com.sun:tools-jar:1","commons-codec:commons-codec:1.11","commons-fileupload:commons-fileupload:1.3.3","commons-io:commons-io:2.5","commons-logging:commons-logging:1.1.2","commons-logging:commons-logging:1.2","jakarta.activation:jakarta.activation-api:2.0.0-RC3","jakarta.annotation:jakarta.annotation-api:1.3.5","jakarta.annotation:jakarta.annotation-api:2.0.0-RC1","jakarta.jws:jakarta.jws-api:3.0.0-RC2","jakarta.persistence:jakarta.persistence-api:3.0.0-RC2","jakarta.servlet:jakarta.servlet-api:5.0.0","jakarta.validation:jakarta.validation-api:2.0.2","jakarta.ws.rs:jakarta.ws.rs-api:3.0.0","jakarta.xml.bind:jakarta.xml.bind-api:3.0.0-RC2","jakarta.xml.bind:jakarta.xml.bind-api:3.0.0-RC3","jakarta.xml.soap:jakarta.xml.soap-api:2.0.0-RC3","jakarta.xml.ws:jakarta.xml.ws-api:3.0.0-RC3","javax.activation:activation:1.1","javax.activation:javax.activation-api:1.2.0","javax.annotation:javax.annotation-api:1.3.2","javax.annotation:jsr250-api:1.0","javax.batch:javax.batch-api:1.0.1","javax.ejb:javax.ejb-api:3.2.2","javax.el:el-api:2.2","javax.el:javax.el-api:3.0.1-b06","javax.enterprise.concurrent:javax.enterprise.concurrent-api:1.1","javax.enterprise.deploy:javax.enterprise.deploy-api:1.7","javax.enterprise:cdi-api:1.0","javax.enterprise:cdi-api:2.0","javax.faces:javax.faces-api:2.3","javax.help:javahelp:2.0.05","javax.inject:javax.inject:1","javax.interceptor:javax.interceptor-api:1.2.2","javax.jms:javax.jms-api:2.0.1","javax.json.bind:javax.json.bind-api:1.0","javax.json:javax.json-api:1.1.4","javax.mail:javax.mail-api:1.6.2","javax.management.j2ee:javax.management.j2ee-api:1.1.2","javax.resource:javax.resource-api:1.7.1","javax.security.auth.message:javax.security.auth.message-api:1.1.1","javax.security.enterprise:javax.security.enterprise-api:1.0","javax.security.jacc:javax.security.jacc-api:1.6","javax.servlet.jsp.jstl:javax.servlet.jsp.jstl-api:1.2.2","javax.servlet.jsp.jstl:jstl-api:1.2","javax.servlet.jsp:javax.servlet.jsp-api:2.3.3","javax.servlet.jsp:jsp-api:2.1","javax.servlet:javax.servlet-api:3.1.0","javax.servlet:javax.servlet-api:4.0.1","javax.servlet:servlet-api:2.3","javax.servlet:servlet-api:2.4","javax.servlet:servlet-api:2.5","javax.transaction:javax.transaction-api:1.3","javax.validation:validation-api:2.0.1.Final","javax.websocket:javax.websocket-api:1.1","javax.ws.rs:javax.ws.rs-api:2.1","javax.xml.bind:jaxb-api:2.3.1","javax.xml.registry:javax.xml.registry-api:1.0.8","javax.xml.rpc:javax.xml.rpc-api:1.1.2","javax.xml.soap:javax.xml.soap-api:1.4.0","javax.xml.stream:stax-api:1.0-2","jetty:jetty:4.2.27","jline:jline:2.14.5","log4j:log4j:1.2.17","net.bytebuddy:byte-buddy:1.8.5","net.bytebuddy:byte-buddy-agent:1.8.5","net.java.dev.jna:jna:4.5.1","net.jxta:jxta-jxse:2.5","net.lingala.zip4j:zip4j:2.6.4","org.apache.commons:commons-collections4:4.0","org.apache.commons:commons-lang3:3.3","org.apache.commons:commons-lang3:3.4","org.apache.felix:org.apache.felix.bundlerepository:2.0.10","org.apache.felix:org.apache.felix.configadmin:1.8.16","org.apache.felix:org.apache.felix.eventadmin:1.4.10","org.apache.felix:org.apache.felix.fileinstall:3.6.4","org.apache.felix:org.apache.felix.gogo.command:1.0.2","org.apache.felix:org.apache.felix.gogo.runtime:1.0.10","org.apache.felix:org.apache.felix.gogo.shell:1.0.0","org.apache.felix:org.apache.felix.main:5.6.10","org.apache.felix:org.apache.felix.scr:2.1.14","org.apache.felix:org.apache.felix.shell:1.4.3","org.apache.felix:org.apache.felix.webconsole:4.3.4","org.apache.felix:org.osgi.foundation:1.2.0","org.apache.httpcomponents:httpclient:4.5.13","org.apache.httpcomponents:httpcore:4.4.13","org.apache.santuario:xmlsec:1.5.8","org.codehaus.jettison:jettison:1.3.7","org.codehaus.plexus:plexus-classworlds:2.5.2","org.codehaus.plexus:plexus-component-annotations:1.6","org.codehaus.plexus:plexus-interpolation:1.21","org.codehaus.plexus:plexus-utils:3.0.22","org.codehaus.woodstox:stax2-api:4.1","org.easymock:easymock:3.4","org.easymock:easymock:3.5","org.eclipse.aether:aether-api:1.0.2.v20150114","org.eclipse.aether:aether-impl:1.0.2.v20150114","org.eclipse.aether:aether-spi:1.0.2.v20150114","org.eclipse.aether:aether-util:1.0.2.v20150114","org.eclipse.persistence:javax.persistence:2.2.0","org.eclipse.persistence:org.eclipse.persistence.antlr:2.7.0","org.eclipse.persistence:org.eclipse.persistence.asm:2.7.0","org.eclipse.persistence:org.eclipse.persistence.core:2.7.0","org.eclipse.persistence:org.eclipse.persistence.dbws:2.7.0","org.eclipse.persistence:org.eclipse.persistence.jpa.jpql:2.7.0","org.eclipse.persistence:org.eclipse.persistence.jpa.modelgen.processor:2.7.0","org.eclipse.persistence:org.eclipse.persistence.jpa:2.7.0","org.eclipse.persistence:org.eclipse.persistence.moxy:2.7.0","org.eclipse.persistence:org.eclipse.persistence.oracle:2.7.0","org.eclipse.sisu:org.eclipse.sisu.inject:0.3.2","org.eclipse.sisu:org.eclipse.sisu.plexus:0.3.2","org.eclipse:yasson:1.0.2","org.glassfish.annotations:logging-annotation-processor:1.8","org.glassfish.corba:exception-annotation-processor:4.2.0-b006","org.glassfish.corba:glassfish-corba-csiv2-idl:4.2.0-b006","org.glassfish.corba:glassfish-corba-internal-api:4.2.0-b006","org.glassfish.corba:glassfish-corba-omgapi:4.2.0-b006","org.glassfish.corba:glassfish-corba-orb:4.2.0-b006","org.glassfish.external:ant:1.10.1","org.glassfish.external:antlr:2.7.7","org.glassfish.external:asm-all:6.0_ALPHA","org.glassfish.external:dbschema:6.6","org.glassfish.external:jsch:0.1.55","org.glassfish.external:management-api:3.2.1","org.glassfish.external:management-api:3.2.1-b003","org.glassfish.external:management-api:3.2.3","org.glassfish.external:schema2beans:6.6","org.glassfish.fighterfish:osgi-cdi:1.0.5","org.glassfish.fighterfish:osgi-ee-resources:2.0.1","org.glassfish.fighterfish:osgi-ejb-container:1.0.4","org.glassfish.fighterfish:osgi-http:1.0.7","org.glassfish.fighterfish:osgi-javaee-base:1.0.8","org.glassfish.fighterfish:osgi-jdbc:1.0.2","org.glassfish.fighterfish:osgi-jpa:1.0.3","org.glassfish.fighterfish:osgi-jpa-extension:1.0.3","org.glassfish.fighterfish:osgi-jta:1.0.2","org.glassfish.fighterfish:osgi-web-container:2.0.1","org.glassfish.gmbal:gmbal:3.0.0-b023","org.glassfish.gmbal:gmbal:4.0.0","org.glassfish.gmbal:gmbal:4.0.0-b002","org.glassfish.gmbal:gmbal-api-only:4.0.2","org.glassfish.grizzly:grizzly-comet:2.4.3","org.glassfish.grizzly:grizzly-config:2.3.2","org.glassfish.grizzly:grizzly-framework:2.4.3","org.glassfish.grizzly:grizzly-http2:2.4.3","org.glassfish.grizzly:grizzly-http:2.4.3","org.glassfish.grizzly:grizzly-http-ajp:2.4.3","org.glassfish.grizzly:grizzly-http-server:2.4.3","org.glassfish.grizzly:grizzly-http-servlet:2.4.3","org.glassfish.grizzly:grizzly-npn-api:1.8.1","org.glassfish.grizzly:grizzly-npn-api:2.0.0","org.glassfish.grizzly:grizzly-npn-bootstrap:1.8.1","org.glassfish.grizzly:grizzly-npn-osgi:1.8.1","org.glassfish.grizzly:grizzly-portunif:2.4.3","org.glassfish.grizzly:grizzly-websockets:2.4.3","org.glassfish.ha:ha-api:3.1.11","org.glassfish.ha:ha-api:3.1.12","org.glassfish.hk2.external:aopalliance-repackaged:2.5.0-b62","org.glassfish.hk2.external:asm-repackaged:2.5.0-b62","org.glassfish.hk2.external:jakarta.inject:2.6.1","org.glassfish.hk2.external:javax.inject:2.5.0-b62","org.glassfish.hk2:class-model:2.5.0-b62","org.glassfish.hk2:hk2:2.5.0-b62","org.glassfish.hk2:hk2-api:2.5.0-b62","org.glassfish.hk2:hk2-core:2.5.0-b62","org.glassfish.hk2:hk2-junitrunner:2.5.0-b62","org.glassfish.hk2:hk2-locator:2.5.0-b62","org.glassfish.hk2:hk2-runlevel:2.5.0-b62","org.glassfish.hk2:hk2-utils:2.5.0-b62","org.glassfish.hk2:osgi-adapter:2.5.0-b62","org.glassfish.hk2:osgi-resource-locator:1.0.2","org.glassfish.hk2:osgi-resource-locator:1.0.3","org.glassfish.jaxb:codemodel:2.3.0","org.glassfish.jersey.containers.glassfish:jersey-gf-ejb:2.27","org.glassfish.jersey.containers:jersey-container-grizzly2-http:2.27","org.glassfish.jersey.containers:jersey-container-servlet:2.27","org.glassfish.jersey.containers:jersey-container-servlet-core:2.27","org.glassfish.jersey.core:jersey-client:2.27","org.glassfish.jersey.core:jersey-client:3.0.0-M1","org.glassfish.jersey.core:jersey-common:2.27","org.glassfish.jersey.core:jersey-common:3.0.0-M1","org.glassfish.jersey.core:jersey-server:2.27","org.glassfish.jersey.core:jersey-server:3.0.0-M1","org.glassfish.jersey.ext.cdi:jersey-cdi1x:2.27","org.glassfish.jersey.ext.cdi:jersey-cdi1x-servlet:2.27","org.glassfish.jersey.ext.cdi:jersey-cdi1x-transaction:2.27","org.glassfish.jersey.ext:jersey-bean-validation:2.27","org.glassfish.jersey.ext:jersey-entity-filtering:2.27","org.glassfish.jersey.ext:jersey-mvc:2.27","org.glassfish.jersey.ext:jersey-mvc-jsp:2.27","org.glassfish.jersey.inject:jersey-hk2:2.27","org.glassfish.jersey.media:jersey-media-jaxb:2.27","org.glassfish.jersey.media:jersey-media-jaxb:3.0.0-M1","org.glassfish.jersey.media:jersey-media-json-binding:2.27","org.glassfish.jersey.media:jersey-media-json-jackson:2.27","org.glassfish.jersey.media:jersey-media-json-jettison:2.27","org.glassfish.jersey.media:jersey-media-json-processing:2.27","org.glassfish.jersey.media:jersey-media-moxy:2.27","org.glassfish.jersey.media:jersey-media-multipart:2.27","org.glassfish.jersey.media:jersey-media-sse:2.27","org.glassfish.main.core:logging:5.0.1","org.glassfish.main.deployment:sun-as-jsr88-dm:5.0.1-SNAPSHOT","org.glassfish.metro:webservices-api-osgi:2.4.2","org.glassfish.metro:webservices-api-osgi:3.0.0-M1","org.glassfish.metro:webservices-extra-jdk-packages:2.4.2","org.glassfish.metro:webservices-extra-jdk-packages:3.0.0-M1","org.glassfish.metro:webservices-osgi:2.4.2","org.glassfish.metro:webservices-osgi:3.0.0-M1","org.glassfish.metro:wstx-services:war:2.4.2","org.glassfish.pfl:pfl-asm:4.0.1","org.glassfish.pfl:pfl-asm:4.0.1-b003","org.glassfish.pfl:pfl-basic:4.0.1","org.glassfish.pfl:pfl-basic:4.0.1-b003","org.glassfish.pfl:pfl-basic-tools:4.0.1","org.glassfish.pfl:pfl-basic-tools:4.0.1-b003","org.glassfish.pfl:pfl-dynamic:4.0.1","org.glassfish.pfl:pfl-dynamic:4.0.1-b003","org.glassfish.pfl:pfl-tf:4.0.1","org.glassfish.pfl:pfl-tf:4.0.1-b003","org.glassfish.pfl:pfl-tf-tools:4.0.1","org.glassfish.pfl:pfl-tf-tools:4.0.1-b003","org.glassfish.soteria:javax.security.enterprise:1.0","org.glassfish.tyrus:tyrus-client:1.14","org.glassfish.tyrus:tyrus-container-glassfish-cdi:1.14","org.glassfish.tyrus:tyrus-container-glassfish-ejb:1.14","org.glassfish.tyrus:tyrus-container-grizzly-client:1.14","org.glassfish.tyrus:tyrus-container-servlet:1.14","org.glassfish.tyrus:tyrus-core:1.14","org.glassfish.tyrus:tyrus-server:1.14","org.glassfish.tyrus:tyrus-spi:1.14","org.glassfish.web:javax.el:2.2.4","org.glassfish.web:javax.servlet.jsp.jstl:1.2.5","org.glassfish.web:javax.servlet.jsp:2.3.4","org.glassfish:javax.el:3.0.1-b10","org.glassfish:javax.enterprise.concurrent:1.1","org.glassfish:javax.faces:2.3.2","org.glassfish:javax.json:1.1.4","org.glassfish:jsonp-jaxrs:1.1.4","org.hamcrest:hamcrest-core:1.1","org.hamcrest:hamcrest-core:1.3","org.hibernate.validator:hibernate-validator:6.0.10.Final","org.hibernate.validator:hibernate-validator-cdi:6.0.10.Final","org.hibernate:hibernate-validator:5.1.3.Final","org.javassist:javassist:3.22.0-GA","org.jboss.classfilewriter:jboss-classfilewriter:1.2.1.Final","org.jboss.logging:jboss-logging:3.3.1.Final","org.jboss.logging:jboss-logging-annotations:2.1.0.Final","org.jboss.spec.javax.ejb:jboss-ejb-api_3.2_spec:1.0.0.Final","org.jboss.spec.javax.el:jboss-el-api_3.0_spec:1.0.7.Final","org.jboss.spec.javax.interceptor:jboss-interceptors-api_1.2_spec:1.0.0.Final","org.jboss.spec.javax.transaction:jboss-transaction-api_1.2_spec:1.0.0.Final","org.jboss.weld.module:weld-ejb:3.0.0.Final","org.jboss.weld.module:weld-jsf:3.0.0.Final","org.jboss.weld.module:weld-jta:3.0.0.Final","org.jboss.weld.module:weld-web:3.0.0.Final","org.jboss.weld.probe:weld-probe-core:3.0.0.Final","org.jboss.weld.se:weld-se-shaded:3.0.0.Final","org.jboss.weld:weld-api:3.0.Final","org.jboss.weld:weld-core-impl:3.0.0.Final","org.jboss.weld:weld-osgi-bundle:3.0.0.Final","org.jboss.weld:weld-spi:3.0.Final","org.jetbrains.kotlin:kotlin-stdlib:1.3.61","org.jetbrains.kotlin:kotlin-stdlib-common:1.3.50","org.jetbrains:annotations:13.0","org.jmockit:jmockit:1.36","org.jvnet.mimepull:mimepull:1.9.10","org.jvnet.mimepull:mimepull:1.9.13","org.jvnet.staxex:stax-ex:2.0.0-M2","org.mockito:mockito-all:1.9.5","org.mockito:mockito-core:2.18.3","org.objenesis:objenesis:2.2","org.objenesis:objenesis:2.6","org.osgi:org.osgi.compendium:4.0.0","org.osgi:org.osgi.compendium:5.0.0","org.osgi:org.osgi.core:4.1.0","org.osgi:org.osgi.core:6.0.0","org.osgi:osgi.annotation:6.0.0","org.osgi:osgi.cmpn:6.0.0","org.osgi:osgi.core:6.0.0","org.osgi:osgi.enterprise:6.0.0","org.shoal:shoal-cache:1.6.52","org.shoal:shoal-gms-api:1.6.52","org.shoal:shoal-gms-impl:1.6.52","org.slf4j:jul-to-slf4j:1.7.25","org.slf4j:slf4j-api:1.7.9","org.slf4j:slf4j-api:1.7.25","org.slf4j:slf4j-log4j12:1.7.9","org.sonatype.plexus:plexus-cipher:1.4","org.sonatype.plexus:plexus-sec-dispatcher:1.3","org.springframework:spring-aop:4.3.9.RELEASE","org.springframework:spring-beans:4.3.9.RELEASE","org.springframework:spring-context:4.3.9.RELEASE","org.springframework:spring-core:4.3.9.RELEASE","org.springframework:spring-expression:4.3.9.RELEASE","org.springframework:spring-web:4.3.9.RELEASE","org.springframework:spring-webmvc:4.3.9.RELEASE","org.yaml:snakeyaml:1.17","org.yaml:snakeyaml:1.27","stax:stax-api:1.0.1","xalan:serializer:2.7.2","xalan:xalan:2.7.2","xml-apis:xml-apis:1.3.04"};
            int c=0;
            for(String s:ss) {
                String p = "C:\\Users\\ROBAI\\.m2\\repository\\"+s.replaceAll("\\:","/");
                c += FileUtils.countClassInJarDir(p,null,"class");
            }
            System.out.println("jar共有class:"+c);
            //5479
            //共18027/23506
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void testSumFilesLineCount(){
        try {
            int n = FileUtils.sumFilesLineCount("C:\\work\\flyingserver_aigit\\flyingserver-admin-api-repository\\src\\main\\java\\com",new String[]{"java"});
            System.out.println("sum:" + n);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void testRepleace(){
        try {
            StringBuffer sb = FileUtils.getFileContentStringBuffer("C:\\work\\flyingserver_aigit\\flyingserver\\flyingserver-core\\appserver\\admin\\admin-core\\src\\main\\java\\com\\asiainfo\\mw\\enterprise\\admin\\UpgradeService.java");
            String str = StringUtils.replace(sb.toString(), "/*", "*/", "");
            System.out.println(str);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public int[] sumTotal(String url,String[] endwith){
        int n = FileUtils.countFileInDir(url,endwith);
        int lineCount = FileUtils.sumFilesLineCount(url,endwith);
        System.out.println(url);
        System.out.println("文件数："+n+" 总行数:"+lineCount);
        return new int[]{n,lineCount};
    }

    public void testSum(){
        int[] html = sumTotal("C:\\work\\flyingserver_aigit\\flyingserver\\flying_server_ui\\web",new String[]{"html","css","js"});
        int[] java = sumTotal("C:\\work\\flyingserver_aigit\\flyingserver\\flyingserver-api-repository\\src",new String[]{"java"});
        int[] core = sumTotal("C:\\work\\flyingserver_aigit\\flyingserver\\flyingserver-core",new String[]{"java","properties"});
        //int[] xml = sumTotal("C:\\work\\flyingserver_aigit\\flyingserver\\flyingserver-core","xml");
        System.out.println("代码自主率（文件数）:"+(html[0]+java[0]+core[0]));
        System.out.println("代码自主率（行数）:"+(html[1]+java[1]+core[1]));
        System.out.println("核心代码自主率（文件数）:"+core[0]);
        System.out.println("核心代码自主率（行数）:"+core[1]);
    }
}
