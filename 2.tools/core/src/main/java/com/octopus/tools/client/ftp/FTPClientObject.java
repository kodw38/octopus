package com.octopus.tools.client.ftp;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.net.NetUtils;
import com.octopus.utils.net.ftp.FTPClient;
import com.octopus.utils.net.ftp.FtpUtil;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sun.net.TelnetOutputStream;
import sun.net.ftp.FtpClient;
import sun.net.ftp.FtpProtocolException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 16-4-21
 * Time: 上午10:30
 */
public class FTPClientObject extends XMLDoObject {
    transient static Log log = LogFactory.getLog(FTPClientObject.class);
    FtpUtil ftpclient;
    public FTPClientObject(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        FtpUtil ftp = getClient(xml.getProperties());
        if(null != ftp){
            ftpclient=ftp;
        }
    }

    FtpUtil getClient(Map map)throws Exception{
        if(null != map){
            String ftpHost = (String)map.get("host");
            String ftpPort = (String)map.get("port");
            String ftpEncoding = (String)map.get("encoding");
            String userName = (String)map.get("name");
            String userPwd = (String)map.get("password");
            String isbin = (String)map.get("isBin");
            String islocalActive = (String)map.get("isLocalActive");
            String isipv4 = (String)map.get("isIpv4");
            String keepAliveTimeout = (String)map.get("keepAliveTimeout");
            String controlKeepAliveReplyTimeout = (String)map.get("controlKeepAliveReplyTimeout");
            String keepAlivetimeout = (String)map.get("timeout");
            String islistHiddenFiles = (String)map.get("isListHiddenFiles");
            if(StringUtils.isNotBlank(ftpHost) && StringUtils.isNotBlank(userName) && StringUtils.isNotBlank(userPwd)){
                if(StringUtils.isBlank(ftpEncoding))
                    ftpEncoding="utf-8";
                if(StringUtils.isBlank(ftpPort))
                    ftpPort="22";
                if(StringUtils.isBlank(isbin))
                    isbin="true";
                if(StringUtils.isBlank(keepAlivetimeout))
                    keepAlivetimeout="0";
                if(StringUtils.isBlank(controlKeepAliveReplyTimeout))
                    controlKeepAliveReplyTimeout="0";
                return new FtpUtil(ftpHost,Integer.valueOf(ftpPort),userName,userPwd);
/*
                return NetUtils.getFTPClient(ftpHost,Integer.valueOf(ftpPort),ftpEncoding,userName,userPwd
                        ,StringUtils.isTrue(isbin),StringUtils.isTrue(islocalActive),StringUtils.isTrue(isipv4)
                        ,Integer.valueOf(keepAliveTimeout),Integer.valueOf(controlKeepAliveReplyTimeout),StringUtils.isTrue(islistHiddenFiles),null,null);
*/
            }
        }
        return null;
    }
    void upfile(FtpClient fc,String remotename,String localname) throws Exception {
        OutputStream os = null;
        FileInputStream is = null;
        try {
            os = fc.putFileStream(remotename);
            java.io.File file_in = new java.io.File(localname);
            is = new FileInputStream(file_in);
            byte[] bytes = new byte[1024];
            int c;
            while ((c = is.read(bytes)) != -1) {
                os.write(bytes, 0, c);
            }
        } finally {
            if (is != null) {
                is.close();
            }
            if (os != null) {
                os.close();
            }
        }
    }
    void rename(FtpClient fc,String oldName,String newName) throws Exception {
        fc.rename(oldName,newName);
    }
    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        //boolean isl=false;
        //FtpUtil ftp=null;
        FtpClient fc=null;
        try {
            /*ftp = getClient(config);
            if (null != ftp) {
                isl = true;
            } else {
                ftp = ftpclient;
            }
            if (null != ftp) {

            }*/
            fc = FtpClient.create((String)config.get("host"));
            fc.login((String)config.get("name"),((String)config.get("password")).toCharArray());
            fc.setBinaryType();
            if (null != input) {
                if ("upload".equals(input.get("op"))
                        && StringUtils.isNotBlank("remote") && StringUtils.isNotBlank("local")) {
                    if(null == input.get("remote_temp") && StringUtils.isBlank((String)input.get("remote_temp"))) {
                        //ftp.upload((String) input.get("remote")+".tmp", new FileInputStream((String) input.get("local")),true);
                        upfile(fc,(String) input.get("remote")+".tmp",(String) input.get("local"));
                        log.debug("upload file["+(String) input.get("remote")+".tmp"+"]");
                        //ftp.rename((String) input.get("remote")+".tmp",(String) input.get("remote"));
                        rename(fc,(String) input.get("remote")+".tmp",(String) input.get("remote"));
                        log.debug("upload file["+(String) input.get("remote")+"]" );
                    }else{
                        //ftp.upload((String) input.get("remote_temp"), new FileInputStream((String) input.get("local")),true);
                        upfile(fc,(String) input.get("remote_temp"),(String) input.get("local"));
                        log.debug("upload file["+(String) input.get("remote")+".tmp"+"]");
                        //ftp.rename((String) input.get("remote_temp"),(String) input.get("remote"));
                        rename(fc,(String) input.get("remote_temp"),(String) input.get("remote"));
                        log.debug("upload file["+(String) input.get("remote")+"]" );
                    }
                    return input.get("remote");
                } else {
                    throw new Exception("ftp not support now");
                }
            }
            return true;  //To change body of implemented methods use File | Settings | File Templates.
        }catch(Throwable e){
            throw new Exception("connection ftp error: "+config+"",e);
        }finally {
            if(null != fc){
                fc.close();
            }
            /*if(null != ftp && isl) {
                ftp.close();
            }*/
        }
    }

    @Override
    public void doInitial() throws Exception {

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
