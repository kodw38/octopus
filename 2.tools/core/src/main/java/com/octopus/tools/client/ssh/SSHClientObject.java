package com.octopus.tools.client.ssh;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.file.FileUtils;
import com.octopus.utils.net.NetUtils;
import com.octopus.utils.net.ssh.SSHClient;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * User: wfgao_000
 * Date: 15-11-24
 * Time: 下午4:20
 */
public class SSHClientObject extends XMLDoObject{
    static transient Log log = LogFactory.getLog(SSHClientObject.class);
    SSHClient ssh;
    public SSHClientObject(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    public void doInitial()throws Exception{
        try {
            Map map = getXML().getProperties();
            map = XMLParameter.getMapValueFromParameter(getEmptyParameter(), map,this);
            ssh = getClient(map);
        }catch (Exception e){
            log.error("",e);
        }
    }
    SSHClient getClient(Map map)throws Exception{
        try {
            String host = (String) map.get("host");
            int port = 22;
            if (StringUtils.isNotBlank(map.get("port")))
                port = Integer.parseInt((String) map.get("port"));
            String username = (String) map.get("username");
            String password = (String) map.get("password");
            log.info("ssh login with "+username);
            if (StringUtils.isNotBlank(host) && StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
                return NetUtils.getSSHClient(host, port, username, password);
            }
        }catch(Exception e){
            log.error("can not connect ssh ["+map+"]");
        }

        return null;
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        SSHClient client =ssh;
        if(null != input && input.containsKey("host")){
            client = getClient(input);
        }
        if(null != config && config.size()>0){
            client=getClient(config);
            if(null == client)
                client=ssh;
        }

        if(null == client)
            getClient(getXML().getProperties());
        if(null != client && !client.getSession().isConnected()){
            client=getClient(getXML().getProperties());
        }

        String type = (String)input.get("type");
        Object command = input.get("data");
        String remotepath = (String)input.get("rpath");
        if("command".equals(type)){
            String cm = ((String)command).replaceAll("\\\\'","'");
            log.debug(cm);
            StringBuffer sb = client.exec(cm);
            log.debug(sb.toString());
            if(null != sb){
                return sb.toString();
            }
        }else if("shell".equals(type)){
            StringBuilder sb = client.shell((String)command) ;
            if(null != sb){
                return sb.toString();
            }
        }else if("copy".equals(type)){
            if(command instanceof InputStream)
                return client.copyFileToRemote((InputStream)command,remotepath);
            if(command instanceof String){
                File f = new File((String)command);
                if(f.isFile()){
                    if(remotepath.endsWith("/")){
                        remotepath+=f.getName();
                    }
                    FileInputStream fin = new FileInputStream((String)command);
                    boolean  ret =client.copyFileToRemote(fin,remotepath);
                    fin.close();
                    return ret;
                }else if(f.isDirectory()){
                    List<File> fs=FileUtils.getAllFile((String)command,null);
                    int n=0;
                    for(File t:fs){
                        FileInputStream fin = new FileInputStream(t);
                        client.copyFileToRemote(fin,remotepath+"/"+t.getName());
                        n++;
                        fin.close();
                    }
                    return n;
                }
            }
        }else if("exec".equals(type)){
            Process pro=null;
            try {
                String cm = ((String) command).replaceAll("\\\\'", "'");
                String[] cmds = {"/bin/sh","-c",cm};
                pro = Runtime.getRuntime().exec(cmds);
                InputStream in = pro.getErrorStream();
                Scanner s = new Scanner(in).useDelimiter("\\A");
                String result = s.hasNext() ? s.next() : "";
                if(log.isDebugEnabled()) {
                    log.debug("exec shell:" + "\n" + result);
                }
                in = pro.getInputStream();
                s = new Scanner(in).useDelimiter("\\A");
                result = s.hasNext() ? s.next() : "";
                if(log.isDebugEnabled()) {
                    log.debug("exec shell:" + "\n" + result);
                }
                pro.waitFor();
                return result;
            }catch (Exception e){
                log.error("exec shell error:" + command,e);
            }finally {
                if(null != pro){
                    pro.destroy();
                }
            }
        }
        return null;

    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResultCheck checkReturn(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return new ResultCheck(true,ret);  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config,Object ret,Exception e) throws Exception {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
