package com.octopus.utils.net;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.net.ftp.FTPClient;
import com.octopus.utils.net.ftp.FTPHTTPClient;
import com.octopus.utils.net.ftp.FTPSClient;
import com.octopus.utils.net.mail.*;
import com.octopus.utils.net.ssh.SSHClient;
import com.octopus.utils.net.tftp.TFTPClient;
import org.apache.commons.net.ProtocolCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.io.CopyStreamListener;
import org.apache.commons.net.smtp.SMTPReply;
import org.apache.commons.net.util.TrustManagerUtils;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.SocketException;

/**
 * User: wangfeng2
 * Date: 14-8-13
 * Time: 上午10:47
 */
public class NetUtils {
    public static enum TrustMgrType {ALL,VALID,NONE};

    /**************************************************** FTP ***********************************/

    static void setFtp(FTPClient ftp
            ,String ftpHost,int ftpPort,String ftpEncoding,String userName,String userPwd,boolean binaryTransfer,boolean localActive,boolean useEpsvWithIPv4,int keepAliveTimeout,int controlKeepAliveReplyTimeout
            ,boolean listHiddenFiles,CopyStreamListener copyStreamListener,ProtocolCommandListener protocolCommandListener)throws Exception{
        if (null != copyStreamListener) {
            ftp.setCopyStreamListener(copyStreamListener);
        }
        if (keepAliveTimeout >= 0) {
            ftp.setControlKeepAliveTimeout(keepAliveTimeout);
        }
        if (controlKeepAliveReplyTimeout >= 0) {
            ftp.setControlKeepAliveReplyTimeout(controlKeepAliveReplyTimeout);
        }
        ftp.setListHiddenFiles(listHiddenFiles);
        if(null != protocolCommandListener){
            // suppress login details
            ftp.addProtocolCommandListener(protocolCommandListener);
        }
        try{
            int reply;
            if (ftpPort > 0) {
                ftp.connect(ftpHost, ftpPort);
            } else {
                ftp.connect(ftpHost);
            }
            reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)){
                ftp.disconnect();
                System.err.println("FTP server refused connection.");
                System.exit(1);
            }
            if (!ftp.login(userName, userPwd)){
                ftp.logout();
            }
            System.out.println("Remote system is " + ftp.getSystemType());
            if (binaryTransfer) {
                ftp.setFileType(FTP.BINARY_FILE_TYPE);
            } else {
                // in theory this should not be necessary as servers should default to ASCII
                // but they don't all do so - see NET-500
                ftp.setFileType(FTP.ASCII_FILE_TYPE);
            }
            // Use passive mode as default because most of us are
            // behind firewalls these days.
            if (localActive) {
                ftp.enterLocalActiveMode();
            } else {
                ftp.enterLocalPassiveMode();
            }
            ftp.setUseEPSVwithIPv4(useEpsvWithIPv4);
            if(StringUtils.isNotBlank(ftpEncoding)){
                ftp.setControlEncoding(ftpEncoding);
            }
        }catch (IOException e){
            if (ftp.isConnected()){
                try{
                    ftp.disconnect();
                }catch (IOException f){
                    // do nothing
                }
            }
            throw e;


        }

    }

    public static FTPClient getFTPHttpProxyClient(String proxyHost, int proxyPort, String proxyUser, String proxyPassword
            ,String ftpHost,int ftpPort,String ftpEncoding,String userName,String userPwd,boolean binaryTransfer,boolean localActive,boolean useEpsvWithIPv4,int keepAliveTimeout,int controlKeepAliveReplyTimeout
            ,boolean listHiddenFiles,CopyStreamListener copyStreamListener,ProtocolCommandListener protocolCommandListener)throws Exception{
        FTPClient ftp = new FTPHTTPClient(proxyHost, proxyPort, proxyUser, proxyPassword);
        setFtp(ftp,ftpHost,ftpPort,ftpEncoding,userName,userPwd,binaryTransfer, localActive, useEpsvWithIPv4,keepAliveTimeout,controlKeepAliveReplyTimeout,listHiddenFiles,copyStreamListener,protocolCommandListener);
        return ftp;
    }

    public static FTPClient getFTPClient(String ftpHost,int ftpPort,String ftpEncoding,String userName,String userPwd,boolean binaryTransfer,boolean localActive,boolean useEpsvWithIPv4,int keepAliveTimeout,int controlKeepAliveReplyTimeout
            ,boolean listHiddenFiles,CopyStreamListener copyStreamListener,ProtocolCommandListener protocolCommandListener)throws Exception{
        FTPClient ftp = new FTPClient();
        setFtp(ftp,ftpHost,ftpPort,ftpEncoding,userName,userPwd, binaryTransfer, localActive, useEpsvWithIPv4,keepAliveTimeout,controlKeepAliveReplyTimeout,listHiddenFiles,copyStreamListener,protocolCommandListener);
        return ftp;
    }

    public static FTPClient getFTPSClient(String protocol,boolean protocolBoolean,TrustMgrType trustmgrtype
            ,String ftpHost,int ftpPort,String ftpEncoding,String userName,String userPwd,boolean binaryTransfer,boolean localActive,boolean useEpsvWithIPv4,int keepAliveTimeout,int controlKeepAliveReplyTimeout
            ,boolean listHiddenFiles,CopyStreamListener copyStreamListener,ProtocolCommandListener protocolCommandListener)throws Exception{
        FTPSClient ftp = null;
        if(StringUtils.isNotBlank(protocol)){
            ftp = new FTPSClient(protocol,protocolBoolean);
        }else{
            ftp = new FTPSClient(protocolBoolean);
        }
        if (TrustMgrType.ALL.equals(trustmgrtype)) {
            ftp.setTrustManager(TrustManagerUtils.getAcceptAllTrustManager());
        } else if (TrustMgrType.VALID.equals(trustmgrtype)) {
            ftp.setTrustManager(TrustManagerUtils.getValidateServerCertificateTrustManager());
        } else if (TrustMgrType.ALL.equals(trustmgrtype)) {
            ftp.setTrustManager(null);
        }
        setFtp(ftp,ftpHost,ftpPort,userName,userPwd,ftpEncoding, binaryTransfer, localActive,useEpsvWithIPv4,keepAliveTimeout,controlKeepAliveReplyTimeout,listHiddenFiles,copyStreamListener,protocolCommandListener);
        return ftp;
    }

    /**
     * 从一台FTP主机的文件转到另一台FTP主机上
     * @param clientFrom
     * @param fromFile
     * @param clientTo
     * @param toFile
     * @return
     */
    public static boolean server2server(FTPClient clientFrom,String fromFile, FTPClient clientTo,String toFile)throws IOException{
        clientTo.enterRemotePassiveMode();
        clientFrom.enterRemoteActiveMode(InetAddress.getByName(clientTo.getPassiveHost()),
                clientTo.getPassivePort());
        if (clientFrom.remoteRetrieve(fromFile) && clientTo.remoteStoreUnique(toFile)){
            //      if(ftp1.remoteRetrieve(file1) && ftp2.remoteStore(file2)) {
            // We have to fetch the positive completion reply.
            clientFrom.completePendingCommand();
            clientTo.completePendingCommand();
        } else{
            System.err.println("Couldn't initiate transfer.  Check that filenames are valid.");
            return false;
        }

        return true;
    }

    public static TFTPClient getTFTPClient(int responseWaitTimeout)throws IOException{
        TFTPClient tftp = new TFTPClient();
        if(responseWaitTimeout>=0)
            tftp.setDefaultTimeout(responseWaitTimeout);
        try{
            tftp.open();
        }catch (SocketException e){
            System.err.println("Error: could not open local UDP socket.");
            System.exit(1);
        }
        return tftp;
    }



    /*************************************   MAIL *************************************/

    public static IMAPClient getIMapClient(String protocol,int responseWaitTimeout,String serverhost,String username,String password)throws IOException{
        IMAPClient imap;
        if(StringUtils.isNotBlank(protocol)){
            imap = new IMAPSClient(protocol,true);
        }else{
            imap = new IMAPClient();
        }
        if(responseWaitTimeout>=0)
            imap.setDefaultTimeout(responseWaitTimeout);
        try{
            imap.connect(serverhost);
        }catch (IOException e){
            throw new RuntimeException("Could not connect to server.", e);
        }
        try{
            if (!imap.login(username, password)){
                imap.disconnect();
                System.exit(3);
            }
        }catch (IOException e){
            e.printStackTrace();
            System.exit(10);
        }
        return imap;
    }

    public static POP3Client getPOP3Client(String protocol,boolean implicit,int responseWaitTimeout,String serverhost,String username,String password)throws IOException{
        POP3Client pop3;
        if(StringUtils.isNotBlank(protocol)){
            pop3 = new POP3SClient(protocol,implicit);
        }else{
            pop3 = new POP3Client();
        }
        if(responseWaitTimeout>=0)
            pop3.setDefaultTimeout(responseWaitTimeout);
        try{
            pop3.connect(serverhost);
        }catch (IOException e){
            e.printStackTrace();
            System.exit(1);
        }
        try{
            if (!pop3.login(username, password)){
                pop3.disconnect();
                System.exit(1);
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        return pop3;
    }

    public static SMTPClient getSMTPClient(String server,Proxy proxy)throws IOException{
        SMTPClient client = new SMTPClient();
        if(null != proxy)
            client.setProxy(proxy);
        client.connect(server);
        if (!SMTPReply.isPositiveCompletion(client.getReplyCode())){
            client.disconnect();
            System.exit(1);
        }
        client.login();
        return client;
    }
    public static SSHClient getSSHClient(String host, int port, String username, String password) throws Exception {
        return new SSHClient(host, port, username, password);
    }

    public static String getip(){
        try{
            InetAddress addr = InetAddress.getLocalHost();
            return addr.getHostAddress().toString();
        }catch (Exception e){
            return "";
        }
    }

    public static void main(String[] args){
        try{
            System.out.println(NetUtils.getip());
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
