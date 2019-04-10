package com.octopus.tools.deploy.command;

import com.octopus.tools.deploy.CommandMgr;
import com.octopus.tools.deploy.ICommand;
import com.octopus.tools.deploy.Util;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.file.FileUtils;
import com.octopus.utils.net.ssh.SSHClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

/**
 * User: Administrator
 * Date: 15-1-21
 * Time: 上午10:14
 */
public class InitializeDBData implements ICommand {
    static transient Log log= LogFactory.getLog(InitializeDBData.class);
    @Override
    public String exeCommand(CommandMgr commandMgr,Properties properties) {
        List<String> sqls = new ArrayList();
        String floder = properties.getProperty("SqlFloder");
        if(StringUtils.isNotBlank(floder)){
            File f = new File(floder);
            for(String fp:f.list()){
                sqls.add(fp);
            }
            if(StringUtils.isNotBlank(properties.getProperty("inputParameter"))){
                ArrayList tem = new ArrayList();
                String[] matchs = properties.getProperty("inputParameter").split(",");
                for(String m:matchs){
                    for(String s:sqls){
                        if(Pattern.matches(m,s)){
                            if(!tem.contains(s))
                                tem.add(s);
                        }
                    }
                }
                sqls=tem;
            }
            SSHClient client=null;
            try{
                client= Util.getSSHClient(properties);
                String scriptPath = commandMgr.getCommandContent("loadSqlFile");
                Collections.sort(sqls);
                for(String file:sqls){
                    properties.put("SqlFile",floder+"/"+file);
                    exeSqlFile(client,properties, scriptPath);
                }
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                if(null != client)client.close();
            }
        }

        return null;
    }

    void exeSqlFile(SSHClient client,Properties p,String scriptPath)throws Exception{
        String localFile = p.getProperty("SqlFile");
        System.out.println("exeSql:"+localFile);
        client.upload("", localFile);
        p.put("SqlFile",localFile.substring(localFile.lastIndexOf("/")+1));

        List<String> keys = new LinkedList();
        List<String> values = new LinkedList();
        Enumeration em = p.keys();
        while(em.hasMoreElements()){
            String k = (String)em.nextElement();
            Object v = p.get(k);
            keys.add("$"+k);
            values.add(v.toString());
        }
        StringBuffer sb = FileUtils.getFileContentStringBuffer(scriptPath);
        String sw = StringUtils.replaceEach(sb.toString(),keys.toArray(new String[0]),values.toArray(new String[0]));
        ByteArrayInputStream in = new ByteArrayInputStream(sw.getBytes());
        String name = scriptPath.substring(scriptPath.lastIndexOf("/")+1);
        log.error("execute command in "+p.getProperty("FixedIp")+" ["+p.getProperty("UserName")+"] "+":"+name);
        client.copyFileToRemote(in,name);
        client.exec("chmod +x "+name);
        StringBuilder ret = client.shell("./"+name);
        client.exec("rm -rf "+name);
        client.exec("rm -rf "+p.get("SqlFile"));
        if(null != ret)
            log.error("return:"+ret.toString());


    }


    public static void main(String[] args){
        try{
            List<String> sqls = new ArrayList();
            sqls.add("z.sql");
            sqls.add("osapp.sql");
            Collections.sort(sqls);
            for(String s:sqls)
                System.out.println(s);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
