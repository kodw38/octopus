package com.octopus.tools.projecttools.restructure;

import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.alone.impl.ReplaceItem;
import com.octopus.utils.file.FileUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;

import java.io.File;
import java.util.*;

public class RestructureMain extends XMLDoObject {


    public RestructureMain(XMLMakeup xml, XMLObject parent, Object[] containers) throws Exception {
        super(xml, parent, containers);
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return null;
    }

    @Override
    public ResultCheck checkReturn(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return new ResultCheck(true,ret);
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret, Exception e) throws Exception {
        return false;
    }

    /**
     * 删除某些结尾的目录，例如\src\test,\target
     * @param rootDir
     * @param endDirNames
     */
    void removeDirs(String rootDir,String[] endDirNames){
        List<String> list = new ArrayList<String>();
        FileUtils.findDirEndWithFilesPath(new File(rootDir),endDirNames,list);
        if(log.isDebugEnabled()) {
            for(String s:list)
            log.debug(s);

        }
        int c=0;
        for(String s:list){
            if(FileUtils.deleteDir(s)) c++;
        }
        log.info("finished to remove dirs "+list.size()+" success:"+c);
    }

   /* void changePackage(String rootDir,Map pathMapping){
        pathMapping = ArrayUtils.sortMapByValueLength(pathMapping,ArrayUtils.DESC);
        List<String> list = new ArrayList<String>();
        FileUtils.getAllDirectoryMarchFiles(new File(rootDir),null,list);
        int chcount=0;
        int totalcount=list.size();
        if(null != list){
            //要移动的放入needMove中key原来的地址，value为新的地址
            //不变的放入noNeedMove中
            Map<String,String> needMove= new LinkedHashMap<String,String>();
            List<String> noNeedMove = new LinkedList();
            boolean ismat=false;
            for(String f:list){
                if(null != pathMapping){
                    Iterator its = pathMapping.keySet().iterator();
                    while(its.hasNext()){
                        String k = (String)its.next();
                        String v = (String)pathMapping.get(k);
                        if(f.indexOf(k)>=0){
                            needMove.put(f,StringUtils.replace(f,k,v));
                            ismat=true;
                            break;
                        }
                    }
                }
                if(!ismat){
                    noNeedMove.add(f);
                }
            }
            chcount=needMove.size();
            //move files needed to change dir
            if(needMove.size()>0){
                Map<String,String> packageMapping = FileUtils.getPackageMapping(needMove);
                Iterator its = needMove.keySet().iterator();
                while(its.hasNext()){
                    String k = (String)its.next();
                    String v = needMove.get(k);
                    try {
                        String endwith = ArrayUtils.indexOfEndWith(k, txtFileSuffix);
                        if (null != endwith) {
                            dealCanChangedFile(endwith,k,needMove,packageMapping);
                        } else {
                            FileUtils.copyFile(k, v);
                        }
                        FileUtils.deleteDir(k);
                    }catch (Exception e){}
                }
            }
        }
        log.info("changed files:"+chcount+" total:"+totalcount);
    }*/
    void dealCanChangedFile(String endWith,String path,Map<String,String> mapping,Map<String,String> packageMapping){
        try {
            StringBuffer content = FileUtils.getFileContentStringBuffer(path);
            //去除头版权信息

            //java 文件替换package
            Iterator its = packageMapping.keySet().iterator();
            while(its.hasNext()){
                String k = (String)its.next();
                String v = (String)packageMapping.get(k);
                String kk = k.replace("\\.","\\\\");
                String vv = k.replace("\\.","\\\\");
                String kkk = k.replace("\\.","/");
                String vvv = k.replace("\\.","/");
                content = StringUtils.replace(content,new String[]{k,kk,kkk},new String[]{v,vv,vvv});

            }

            //保存文件
            FileUtils.saveFile(content,mapping.get(path),true,false);
        }catch (Exception e){
            log.error("",e);
        }
    }

    public static void main(String[] args){
        try{
            String[] chgContentFilesSuffix=new String[]{"java","txt","properties","json","xml","js","html","css","bundle","jsf","sh","jsp","sql"
                    ,"inc","bak","1","5asc","html","dtd","layout","xsl","xsd","bat","verifier","policy","appclient","py","Processor"
                    ,"tld","Extension","1m","readme","Provider","-schema","keyfile","conf","javadoc",".g","_check","jnlp",".container"
                    ,".template","stopserv","startserv","nadmin","html_","wsimport","wsgen","xjc","schemagen","wscompile","wsdeploy"
                    ,"runtest","asadmin","config-files","jspc","javascript","wsdl"};

            String from="C:\\work\\flyingserver_aigit\\flyingserver-app-repository";
            //String from="C:\\logs\\temp\\app-repository";
            String to = "C:\\work\\flyingserver_aigit\\flyingserver\\temp";
            //String to = "C:\\logs\\temp\\target";

            RestructureMain r = new RestructureMain(null,null,null);

            Map chgPackageStartWith = new HashMap();
            chgPackageStartWith.put("com.sun.enterprise","com.asiainfo.enterprise");
            chgPackageStartWith.put("org.glassfish","com.asiainfo.mw.flyingserver");
            chgPackageStartWith.put("org.jvnet","com.asiainfo");
            List<ReplaceItem> reps= new ArrayList();
            ReplaceItem item = new ReplaceItem();
            item.setStartMark("<!--");
            item.setEndMark("-->");
            item.setReplaceContent("");
            reps.add(item);

            ReplaceItem item2 = new ReplaceItem();
            //item2.setStartMark("/*");
            //item2.setEndMark("*/");
            //item2.setReplaceContent("");
            item2.setDeleteLineStartWith(new String[]{"#"});
            item2.setExcludeForReplaceInLine("import");
            HashMap mapping =  new HashMap();
            //mapping.put("org.glassfish","com.asiainfo.mw.flyingserver");
            //mapping.put("oracle","asiainfo");
            //mapping.put("GlassFish","FlyingServer");
            //mapping.put("glassfish","flyingserver");
            mapping.put("Oracle","Asiainfo");
            mapping.put("ORACLE","ASIAINFO");
            mapping.put("GLASSFISH","FLYINGSERVER");

            item2.setReplaceInLine(ArrayUtils.sortMapByKeyLength(mapping,ArrayUtils.DESC));
            reps.add(item2);

            String[] pkgParentDirNames = new String[]{"java","resources"};
            HashMap chgDirNames = new HashMap();
            chgDirNames.put("GlassFish","FlyingServer");
            chgDirNames.put("GLASSFISH","FLYINGSERVER");
            chgDirNames.put("glassfish","flyingserver");

            HashMap chgFileNames = new HashMap();
            chgFileNames.put("GlassFish","FlyingServer");
            chgFileNames.put("Glassfish","FlyingServer");
            chgFileNames.put("GF","FS");

            //StringBuffer sb = FileUtils.getFileContentStringBuffer("C:\\work\\flyingserver_aigit\\flyingserver\\appserver\\admin\\admin-core\\src\\main\\java\\com\\asiainfo\\mw\\enterprise\\admin\\AdminContext.java");
            //String ss = StringUtils.replace(sb.toString(),item.getStartMark(),item.getEndMark(),item.getReplaceContent());
            //System.out.println(ss);
            FileUtils.refactor(from,to
                    ,new String[]{"\\target",".idea",".git","classes","licenses"}
                    ,new String[]{"iml","LICENSE","log","md","ReadMe","README","mdd"}
                    ,pkgParentDirNames,chgDirNames,chgPackageStartWith,chgFileNames,reps,chgContentFilesSuffix,20);
            Thread.sleep(5000);
            System.exit(1);

            //next todo
            /**
             * 1. pom.xml com.asiainfo.mw.flyingserver.hk2  org.glassfish.hk2
             * 2. 目录没有迁移的glassfish-api glassfish-ee-api
             */
        }catch (Exception e){
            log.error("",e);
        }
    }
}
