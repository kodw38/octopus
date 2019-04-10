package com.octopus.tools.deploy;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.file.FileUtils;
import com.octopus.utils.net.ssh.SSHClient;
import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.thread.ds.InvokeTask;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * User: Administrator
 * Date: 15-1-15
 * Time: 上午10:58
 */
public class Deploy {
    static transient Log log = LogFactory.getLog(Deploy.class);

    /**
     *
     * @param ll
     * @param rage AppGroup:,|Group:,|HostName:,|UserName:,|AppName:,
     * @return
     */
    List<Properties> filter(List<Properties> ll,String rage){
        if(StringUtils.isNotBlank(rage)){
            String[] ss = rage.split("\\|");
            List<Properties> ret = ll;
            for(String sep:ss){
                String rangetype=sep.substring(0,sep.indexOf(":"));
                String[] seps = sep.substring(sep.indexOf(":")+1).split(",");
                List<Properties> tem= new ArrayList<Properties>();
                for(String e:seps){
                   for(Properties p:ret){
                       if(null != p.get(rangetype) && p.get(rangetype).equals(e)){
                           tem.add(p);
                       }
                   }
                }
                ret=tem;
            }
            if(ret.size()>0)
                return ret;
        }
        return null;
    }

    public String executeScript(PropertiesMgr propertiesMgr,CommandMgr commandMgr,String commandNames,String range,String parameter,OutputStream out,Object sender)throws Exception{
        String[] commands = commandNames.split(",");
        StringBuffer ret = new StringBuffer();
        for(String commandName:commands){
            List<Properties> ps = propertiesMgr.getCommandPropertyList(commandName);
            List<Properties> target = filter(ps,range);
            ICommand cmd = commandMgr.getCommand(commandName);
            if(null == cmd){
                if(null!=ps){
                    if(null != target){
                        InvokeTask[] its = new InvokeTask[target.size()];
                        for(int i=0;i<target.size();i++){

                            Properties pro = target.get(i);
                            if(StringUtils.isNotBlank(parameter))
                                pro.put("inputParameter",parameter);
                            log.info(pro.toString());
                            StringBuffer sb = exeCommand(commandMgr,pro,commandName,out,sender);
                            if(null != sb){
                                ret.append(sb);
                            }
                        }
                    }
                }
            }else{
                if(null!=ps){
                    if(null != target){
                        InvokeTask[] its = new InvokeTask[target.size()];
                        for(int i=0;i<target.size();i++){

                            Properties pro = target.get(i);
                            if(StringUtils.isNotBlank(parameter))
                                pro.put("inputParameter",parameter);
                            log.error(pro.toString());
                            String str = cmd.exeCommand(commandMgr,pro);
                            ret.append(str);
                            if(null != out)
                            out.write(str.getBytes());
                            if(null != sender)
                                ExecutorUtils.synWork(sender,"sendText",new Class[]{String.class},new Object[]{str});
                        }
                    }
                }
            }
        }
        return ret.toString();
    }

    public static StringBuffer exeCommand(CommandMgr commandMgr,Properties p,String commandName,OutputStream out,Object sender){
        SSHClient client=null;
        try{
            String scriptPath = commandMgr.getCommandContent(commandName);
            if(null == scriptPath)throw new Exception("not find the command script:"+commandName);
            client = Util.getSSHClient(p);

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
            String msg = "begin execute command in "+p.getProperty("FixedIp")+" ["+p.getProperty("UserName")+"] "+":"+name;
            log.info(msg);
            if(null != out)
                out.write(msg.toString().getBytes());
            if(null != sender)
                ExecutorUtils.synWork(sender, "sendText", new Class[]{String.class}, new Object[]{msg.toString()});
            client.copyFileToRemote(in,name);
            msg = "copy :"+name+"\n"+sw;
            log.info(msg);
            if(null != out)
                out.write(msg.toString().getBytes());
            if(null != sender)
                ExecutorUtils.synWork(sender, "sendText", new Class[]{String.class}, new Object[]{msg.toString()});
            client.exec("chmod +x "+name);
            msg = "chmod x :"+name;
            log.info(msg);
            if(null != out)
                out.write(msg.toString().getBytes());
            if(null != sender)
                ExecutorUtils.synWork(sender, "sendText", new Class[]{String.class}, new Object[]{msg.toString()});
            StringBuilder ret = client.shell("./"+name);
            if(null != out)
                out.write(ret.toString().getBytes());
            if(null != sender)
                ExecutorUtils.synWork(sender, "sendText", new Class[]{String.class}, new Object[]{ret.toString()});
            log.info("shell :"+name);
            client.exec("rm -rf "+name);
            msg = "rm -rf "+name;
            if(null != out)
                out.write(msg.toString().getBytes());
            if(null != sender)
                ExecutorUtils.synWork(sender, "sendText", new Class[]{String.class}, new Object[]{msg.toString()});
            log.info("rm :"+name);
            if(null != ret)
                log.info("return:"+ret.toString());
            msg = "finished execute command in "+p.getProperty("FixedIp")+" ["+p.getProperty("UserName")+"] "+":"+name;
            log.info(msg);
            if(null != out)
                out.write(msg.toString().getBytes());
            if(null != sender)
                ExecutorUtils.synWork(sender, "sendText", new Class[]{String.class}, new Object[]{msg.toString()});
            return new StringBuffer(ret);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(null != client)client.close();
        }
        return null;
    }
    public static void main(String[] args){
        try{
           if(null == args || args.length==0){
                args=new String[]{"copy","AppGroup:onlineTest03|UserName:oseweb|AppName:OSE-WEB|MediaFile:E:/my work/asiainfo/ISP/6.products/autodeployer/trunk/core/src/main/resource/bin/osead.war"};
                //args=new String[]{"copy","AppName:OSE-ADMIN"};
           }
           if(args.length>=2){
               Deploy d = new Deploy();
               String p = null;
               if(args.length>2)
                   p = args[2];
               CommandMgr commandMgr = new CommandMgr("configExcel",null);
               PropertiesMgr propertiesMgr = new PropertiesMgr("");
               d.executeScript(propertiesMgr,commandMgr,args[0].trim(), args[1].trim(),p,null,null);
           }

        }catch (Exception e){
           e.printStackTrace();
        }
    }
}
