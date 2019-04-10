package com.octopus.tools.deploy.command;

import com.octopus.tools.deploy.CommandMgr;
import com.octopus.tools.deploy.ICommand;
import com.octopus.tools.deploy.Util;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.file.FileUtils;
import com.octopus.utils.net.NetUtils;
import com.octopus.utils.net.ssh.SSHClient;
import com.octopus.utils.file.impl.excel.ExcelReader;
import com.octopus.utils.zip.ReplaceZipItem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * User: Administrator
 * Date: 15-1-19
 * Time: 下午5:03
 */
public class CopyCommand implements ICommand {
    static transient Log log = LogFactory.getLog(CopyCommand.class);
    HashMap<String,Map<String,Map<String,String>>> cache = new HashMap<String, Map<String, Map<String, String>>>();
    HashMap<String,Map<String,List<String>>> addCache = new HashMap<String, Map<String, List<String>>>();
    public CopyCommand(String configExcel) {
        try{
            ExcelReader ex = new ExcelReader(new FileInputStream(configExcel));
            List<Map<String,String>> tm = ex.getSheepData("Variable");
            for(Map<String,String> m:tm){
                String id = m.get("ChangeId");
                String f = m.get("ChangeFile");
                String k = m.get("Variable");
                String v = m.get("Value");
                if(StringUtils.isBlank(k)){
                    File ft = new File(v);
                    if(ft.exists() && ft.isDirectory()){
                        if(!addCache.containsKey(id))addCache.put(id,new HashMap<String, List<String>>());
                        if(!addCache.get(id).containsKey(f))addCache.get(id).put(f,new ArrayList<String>());
                        addCache.get(id).get(f).add(v);
                    }
                }else{
                    if(null == f)f="";
                    if(!cache.containsKey(id))cache.put(id,new HashMap<String, Map<String, String>>());
                    if(!cache.get(id).containsKey(f))cache.get(id).put(f,new HashMap<String, String>());
                    cache.get(id).get(f).put(k,v);
                }
            }
        }catch (Exception e){

        }
    }
    String getRemoteFile(Properties p,String f ){
        String a = p.getProperty("RemoteDir");
        String fa = a.substring(a.lastIndexOf("/")+1);
        if(StringUtils.isNotBlank(fa) && fa.contains(".")){
            return a;
        }else{
            return p.getProperty("RemoteDir")+"/"+f.substring(f.lastIndexOf("/")+1);
        }
    }
    String getRemoteDir(String p){
        String fa = p.substring(p.lastIndexOf("/")+1);
        if(StringUtils.isNotBlank(fa) && fa.contains(".")){
            return p.substring(0,p.lastIndexOf("/"));
        }
        return p;
    }
    String getRemoteName(String p){
        String fa = p.substring(p.lastIndexOf("/")+1);
        if(StringUtils.isNotBlank(fa) && fa.contains(".")){
            return fa;
        }
        return null;
    }
    @Override
    public String exeCommand(CommandMgr commandMgr,Properties p)throws Exception{
        SSHClient client=null;
        try{
            client = Util.getSSHClient(p);
            String f = p.getProperty("MediaFile");
            File fe = new File(f);
            if(StringUtils.isBlank(f) || !fe.exists()) return "";
            if(f.endsWith("jar") || f.endsWith("tar") || f.endsWith("war")|| f.endsWith("ear")||f.endsWith("gz") || f.endsWith("zip")){
                Map<String,ReplaceZipItem> items = new HashMap<String, ReplaceZipItem>();
                Map<String,List<String>> addFiles = new HashMap();
                if(StringUtils.isNotBlank(p.getProperty("ChangeId"))){
                    String[] chgids = p.getProperty("ChangeId").split(",");
                    for(String chgid:chgids){
                        Map m = cache.get(chgid);
                        if(null != m){
                            Iterator<String> is = m.keySet().iterator();
                            while(is.hasNext()){
                                String file = is.next();
                                String filename=file;
                                ReplaceZipItem item = new ReplaceZipItem();
                                if(file.contains("!")){
                                    String[] ts = file.split("!");
                                    filename = ts[1];
                                    item.setJars(new String[]{ts[0]});
                                }
                                item.setFileName(filename);
                                item.setChgvalue(cache.get(chgid).get(file));

                                items.put(file,item);


                            }
                        }
                        Map mt = addCache.get(chgid);
                        if(null != mt){
                            addFiles.putAll(mt);
                        }

                    }
                }

                if(f.startsWith("sftp://")){
                    int s = f.indexOf("//")+2;
                    int e = f.indexOf("/",s);
                    String t = f.substring(s,e);
                    String[] up = t.split("@");
                    int pi = f.indexOf("/",e+1);
                    String d = f.substring(e+1,pi);
                    String[] ds = d.split(":");
                    SSHClient c=null;
                    try{
                        c = NetUtils.getSSHClient(ds[0],22,up[0],up[1]);
                        String dir = f.substring(pi,f.lastIndexOf("/"));
                        String name = f.substring(f.lastIndexOf("/")+1);
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        c.download(dir,name,out);
                        ByteArrayInputStream  in = new ByteArrayInputStream(out.toByteArray());
                        if(null != in){
                            log.error("copy "+f+" to ["+p.getProperty("FixedIp")+" "+p.getProperty("UserName")+"] "+p.getProperty("RemoteDir"));
                            boolean ret = client.copyZipFileToRemote(in,getRemoteFile(p,f),addFiles,items.values().toArray(new ReplaceZipItem[0]));
                            return String.valueOf(ret);
                        }
                    }catch (Exception ex){
                        log.error("copy error:",ex);
                    }finally {
                        if(null != c)c.close();
                    }
                }else if(f.startsWith("ftp://")){
                    URL url = new URL(f);
                    InputStream in = url.openStream();
                    if(null != in){
                        boolean ret = client.copyZipFileToRemote(in,getRemoteFile(p,f),addFiles,items.values().toArray(new ReplaceZipItem[0]));
                        log.error("copy "+f+" to ["+p.getProperty("FixedIp")+"] "+p.getProperty("UserName")+p.getProperty("RemoteDir"));
                        return String.valueOf(ret);
                    }
                }else{
                    File ef = new File(f);
                    if(ef.exists()){
                        String rd = p.getProperty("RemoteDir");
                        String name = getRemoteName(rd);
                        if(StringUtils.isBlank(name)){
                            name = ef.getName();
                        }
                        String dir = getRemoteDir(rd);
                        boolean ret = client.copyZipFileToRemote(ef,null,dir,name,addFiles,items.values().toArray(new ReplaceZipItem[0]),p.getProperty("FileEncoding"));
                        log.error("copy "+ef+" to ["+p.getProperty("FixedIp")+"] "+p.getProperty("UserName")+"@"+p.getProperty("RemoteDir"));
                        return String.valueOf(ret);
                    }
                }
            }else if(f.endsWith("sh") || f.endsWith("txt")){
                File ef = new File(f);
                if(ef.exists()){
                    client.copyTextFileToRemote(ef,null,getRemoteDir(p.getProperty("RemoteDir")),cache.get(p.getProperty("ChangeId")).get(""));
                    log.error("copy "+ef+" to ["+p.getProperty("FixedIp")+"] "+p.getProperty("UserName")+p.getProperty("RemoteDir"));
                }
            }else{
                client.upload(getRemoteDir(p.getProperty("RemoteDir")),f);
            }
        }catch (Exception e){
            throw e;
        }finally {
            if(null != client)client.close();
        }
        return "false";
    }
}
