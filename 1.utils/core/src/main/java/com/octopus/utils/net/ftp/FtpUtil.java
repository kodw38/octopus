package com.octopus.utils.net.ftp;

import com.octopus.utils.alone.StringUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;


public class FtpUtil {

    protected static transient Log logger = LogFactory.getLog(FtpUtil.class);

    public static final String FILE_SPEARATROR = "/";
    public static final int BIN = 0;
    public static final int ASC = 1;

    protected FTPClient client = null;
    private String localPath = null;


    private String _host = null;
    private int _port = 0;
    private String _username = null;
    private String _password = null;
    private String _remotePath = null;
    private String _localPath = null;
    private String _localPathTemp = null;
    private String remotePathHis;//FTP�ļ���ʷĿ¼,��cfg_ftp_path��remote_path_his�ֶ�����

    /**
     * @throws Exception
     */
    public FtpUtil(String host,int port,String username,String pwd, String remotepath,String remotehispath,String localpath,String localhisPath) throws Exception {
        client = new FTPClient();
        client.connect(host, port);
        client.login(username, pwd);
        if(StringUtils.isNotBlank(remotehispath)) {
            boolean b = client.changeWorkingDirectory(remotepath);
            if (!b) {
                client.mlistDir(remotepath);
            }
        }
        localPath = localpath;
        remotePathHis = remotehispath;//add by lxm 08-10-27
        //add by huangyb
        _host = host;
        _port = port;
        _username = username;
        _password = pwd;
        _remotePath = remotepath;
        _localPath = localpath;
        _localPathTemp = localhisPath;

    }

    public FtpUtil(String host,int port,String username,String pwd) throws Exception {
        client = new FTPClient();
        client.connect(host, port);

        client.login(username, pwd);
        //add by huangyb
        _host = host;
        _port = port;
        _username = username;
        _password = pwd;

    }

//    add by wanghq3 begin

    public void recycle() throws IOException {
        client.getReply();
    }


//    add by wanghq3 end

    /**
     * �����Ƹ�ʽ
     *
     * @throws Exception
     */
    public void bin() throws Exception {
        client.setFileType(FTP.BINARY_FILE_TYPE);
    }

    /**
     * ascii��ʽ
     *
     * @throws Exception
     */
    public void asc() throws Exception {
        client.setFileType(FTP.ASCII_FILE_TYPE);
    }

    public void mkdir(String path) throws Exception {
        client.makeDirectory(path);
    }

    public FTPClient getFTPClient() {
        return client;
    }

    /**
     * �г���������Ŀ¼������ļ�
     *
     * @param encoding String
     * @return String[]
     * @throws Exception
     */
    public String[] list(String encoding) throws Exception {
        List list = new ArrayList();

        FTPFile[] ftpFiles = client.listFiles(client.printWorkingDirectory());

        for (int i = 0; i < ftpFiles.length; i++) {
            if (ftpFiles[i].isFile()) {
                list.add(new String(ftpFiles[i].getName().getBytes("ISO-8859-1"), encoding));
            }
        }

        return (String[]) list.toArray(new String[0]);
    }

    /**
     * ���pathName�г���������Ŀ¼�µ��ļ�
     *
     * @param pathName �ļ�����ļ�·��
     * @return ����FTPFile�б�
     * @throws Exception
     */
    public FTPFile[] listFtpFiles(String pathName) throws Exception {
        return client.listFiles(pathName);
    }

    /**
     * @return String[]
     * @throws Exception
     */
    public String[] list() throws Exception {
        return list("GBK");
    }

    /**
     * @param remoteFileName String
     * @param input          InputStream
     * @param mode           int
     * @throws Exception
     */
    public void upload(String remoteFileName, InputStream input, int mode,boolean isneedclosefile) throws Exception {
        if (mode == BIN) {
            client.setFileType(FTPClient.BINARY_FILE_TYPE);
        } else if (mode == ASC) {
            client.setFileType(FTPClient.ASCII_FILE_TYPE);
        } else {
            throw new Exception("upload:" + mode);
        }
        upload(remoteFileName, input,isneedclosefile);
    }

    /**
     * @param remoteFileName String
     * @param input          InputStream
     * @throws Exception
     */
    public void upload(String remoteFileName, InputStream input,boolean isneedclosefile) throws Exception {
        try {
            client.setFileType(FTPClient.BINARY_FILE_TYPE);
            boolean b = client.storeFile(remoteFileName, input);
            if (!b) {
                throw new Exception("upload file [" + remoteFileName + "] fail");
            }
        }finally {
            if(isneedclosefile){
                input.close();
            }
        }
    }



    /*public void upload(String remoteFileName,InputStream input,boolean isneedclosefile)throws Exception {
        OutputStream output=null;
        try {
            client.setFileType(FTPClient.BINARY_FILE_TYPE);
            output = client.storeFileStream(remoteFileName);
            if (output == null) {
                throw new Exception("not find the target save path:" + remoteFileName);
            }
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = input.read(buffer)) > 0) {
                output.write(buffer, 0, len);
                output.flush();
            }
        }finally {
            if(null != output){
                output.close();
            }
            if(isneedclosefile){
                input.close();
            }

        }

    }*/

    /**
     * @param remoteFileName String
     * @param localFileName  String
     * @throws Exception
     */
    public void upload(String remoteFileName, String localFileName) throws Exception {
        InputStream in = null;
        try {
            in = new BufferedInputStream(new FileInputStream(localPath + "/" + localFileName));
            upload(remoteFileName,in,true);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    /**
     * @param remoteFileName String
     * @param localFileName  String
     * @throws Exception
     */
    public void uploadTts(String remoteFileName, String localFileName, int mode) throws Exception {
        if (mode == BIN) {
            client.setFileType(FTP.BINARY_FILE_TYPE);
        } else if (mode == ASC) {
            client.setFileType(FTP.ASCII_FILE_TYPE);
        } else {
            throw new Exception("upload:" + mode);
        }
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(localFileName));
            boolean b= client.storeFile(remoteFileName, is);
            if(!b){
                throw new Exception("upload file ["+remoteFileName+"] fail");
            }
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    /**
     * @param remoteFileName String
     * @param localFileName  String
     * @param mode           int
     * @throws Exception
     */
    public void upload(String remoteFileName, String localFileName, int mode) throws Exception {
        if (mode == BIN) {
            client.setFileType(FTP.BINARY_FILE_TYPE);
        } else if (mode == ASC) {
            client.setFileType(FTP.ASCII_FILE_TYPE);
        } else {
            throw new Exception("��֧�ֵĴ���ģʽ:" + mode);
        }
        upload(remoteFileName, localFileName);
    }


    public void uploadWithGzip( String fileName, List lines) throws Exception {
        fileName = fileName + ".gz";
        OutputStream out = getOutputStream(fileName);
        if (out == null) {
            throw new Exception("FTP OutputStream Ϊ��,����ԭ��FTPδ���ӻ����ӳ�ʱ");
        }
        GZIPOutputStream gzout = null;
        try {
            gzout = new GZIPOutputStream(out);
            IOUtils.writeLines(lines, null, gzout, "GBK");
        } catch (Exception e) {
            throw e;
        } finally {
            if (null != gzout) gzout.close();

            if (null != out) out.close();

            completePendingCommand();
        }
    }

    /**
     * @param remoteFileName String
     * @param output         OutputStream
     * @param mode           int
     * @throws Exception
     */
    public void download(String remoteFileName, OutputStream output, int mode) throws Exception {
        if (mode == BIN) {
            client.setFileType(FTP.BINARY_FILE_TYPE);
        } else if (mode == ASC) {
            client.setFileType(FTP.ASCII_FILE_TYPE);
        } else {
            throw new Exception("��֧�ֵĴ���ģʽ:" + mode);
        }
        download(remoteFileName, output);
    }

    /**
     * ��������,���ݸ���·��
     *
     * @param remoteFileName String
     * @param output         OutputStream
     * @throws Exception
     */
    public void complexDownload(String remoteFileName, OutputStream output) throws Exception {
        //����Ϊbinģʽ
        client.setFileType(FTP.BINARY_FILE_TYPE);

        String filePath = null;
        String downFile = remoteFileName.trim();
        if (downFile.indexOf("\\") >= 0 || downFile.indexOf("/") >= 0) {
            if (downFile.charAt(0) != '.' && downFile.charAt(0) == '/') {
                downFile = "." + downFile;
            } else {
                downFile = "./" + downFile;
            }
            if (downFile.indexOf("\\") > downFile.indexOf("/")) {
                filePath = downFile.substring(0, downFile.lastIndexOf("\\"));
                downFile = downFile.substring(downFile.lastIndexOf("\\") + 1, downFile.length());
            } else {
                filePath = downFile.substring(0, downFile.lastIndexOf("/"));
                downFile = downFile.substring(downFile.lastIndexOf("/") + 1, downFile.length());
            }
        }
        if (null != filePath) {
            this.changeWorkingDirectory(filePath);
        }
        if (logger.isDebugEnabled()) {
            logger.debug(client.printWorkingDirectory());
        }

        boolean rtn = client.retrieveFile(new String(downFile.getBytes("GBK"), "iso-8859-1"), output);
        if (rtn == false) {
            throw new Exception("����Զ���ļ�:" + remoteFileName + ",���ɹ�");
        }
    }


    /**
     * @param remoteFileName String
     * @param output         OutputStream
     * @throws Exception
     */
    public void download(String remoteFileName, OutputStream output) throws Exception {
        String filePath = null;
        String downFile = remoteFileName.trim();
        if (downFile.indexOf("\\") >= 0 || downFile.indexOf("/") >= 0) {
            if (downFile.charAt(0) != '.' && downFile.charAt(0) == '/') {
                downFile = "." + downFile;
            } else {
                downFile = "./" + downFile;
            }
            if (downFile.indexOf("\\") > downFile.indexOf("/")) {
                filePath = downFile.substring(0, downFile.lastIndexOf("\\"));
                downFile = downFile.substring(downFile.lastIndexOf("\\") + 1, downFile.length());
            } else {
                filePath = downFile.substring(0, downFile.lastIndexOf("/"));
                downFile = downFile.substring(downFile.lastIndexOf("/") + 1, downFile.length());
            }
        }
        if (null != filePath) {
            this.changeWorkingDirectory(filePath);
        }
        if (logger.isDebugEnabled()) {
            logger.debug(client.printWorkingDirectory());
        }

        boolean rtn = client.retrieveFile(downFile, output);
        if (rtn == false) {
            throw new Exception("download file:" + remoteFileName + " error");
        }
    }

    public void downloadSimple(String remoteFileName, OutputStream output) throws Exception {
        boolean rtn = client.retrieveFile(remoteFileName, output);
        if (rtn == false) {
            throw new Exception("download file:" + remoteFileName + "error");
        }
    }

    /**
     * @param remoteFileName String
     * @param localFileName  String
     * @param mode           int
     * @throws Exception
     */
    public void download(String remoteFileName, String localFileName, int mode) throws Exception {
        if (mode == BIN) {
            client.setFileType(FTP.BINARY_FILE_TYPE);
        } else if (mode == ASC) {
            client.setFileType(FTP.ASCII_FILE_TYPE);
        } else {
            throw new Exception("download file:" + mode);
        }
        download(remoteFileName, localFileName);
    }

    /**
     * @param remoteFileName String
     * @param localFileName  String
     * @throws Exception
     */
    public void download(String remoteFileName, String localFileName) throws Exception {
        OutputStream os = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(localPath + "/" + localFileName));
            boolean rtn = client.retrieveFile(remoteFileName, os);
            if (rtn == false) {
                throw new Exception("download file:" + remoteFileName + " error");
            }
        } finally {
            if (os != null) {
                os.close();
            }
        }
    }


    /**
     * @param remoteFileName String
     * @param localFileName  String
     * @throws Exception
     */
    public void downloadTts(String remoteFileName, String localFileName, int mode) throws Exception {
        OutputStream os = null;
        if (mode == BIN) {
            client.setFileType(FTP.BINARY_FILE_TYPE);
        } else if (mode == ASC) {
            client.setFileType(FTP.ASCII_FILE_TYPE);
        } else {
            throw new Exception("download:" + mode);
        }
        try {
            os = new BufferedOutputStream(new FileOutputStream(localFileName));
            boolean rtn = client.retrieveFile(remoteFileName, os);
            if (rtn == false) {
                throw new Exception("download file:" + remoteFileName + " error");
            }
        } finally {
            if (os != null) {
                os.close();
            }
        }
    }

    /**
     * @param remoteFileName String
     * @return InputStream
     * @throws Exception
     */
    public InputStream readRemote(String remoteFileName) throws Exception {
        return client.retrieveFileStream(remoteFileName);
    }

    /**
     * @param remoteFileName String
     * @param mode           int
     * @return InputStream
     * @throws Exception
     */
    public InputStream readRemote(String remoteFileName, int mode) throws Exception {
        if (mode == BIN) {
            client.setFileType(FTP.BINARY_FILE_TYPE);
        } else if (mode == ASC) {
            client.setFileType(FTP.ASCII_FILE_TYPE);
        } else {
            throw new Exception("read mode:" + mode);
        }
        return readRemote(remoteFileName);
    }

    /**
     * @param oldRemoteFileName String
     * @param newRemoteFileName String
     * @throws Exception
     */
    public void rename(String oldRemoteFileName, String newRemoteFileName) throws Exception {
        boolean flag=client.rename(oldRemoteFileName, newRemoteFileName);
        if(!flag){
            throw new Exception("rename file from "+oldRemoteFileName+" to "+newRemoteFileName+" fail");
        }
    }

    /**
     * @param remoteFileName String
     * @throws Exception
     */
    public void delete(String remoteFileName) throws Exception {
        boolean rtn = client.deleteFile(remoteFileName);
        if (rtn == false) {
            throw new Exception("delete file:" + remoteFileName + " fail");
        }
    }

    /**
     * @throws Exception
     */
    public void completePendingCommand() throws Exception {
        client.completePendingCommand();
    }

    /**
     * @throws Exception
     */
    public void close() throws Exception {

        if (client.isConnected()) {
            client.logout();
            client.disconnect();

        }
    }

    /**
     * @throws Exception
     */
    public void reconnect() throws Exception {
        if (!client.isConnected()) {
            client = new FTPClient();
            client.connect(_host, _port);
            client.login(_username, _password);
            client.changeWorkingDirectory(_remotePath);

            localPath = _localPath;
        }
    }

    /**
     * @throws Exception
     */
    public void forceReconnect() throws Exception {
        if (!client.isConnected()) {
            client.disconnect();
        }

        client = new FTPClient();
        client.connect(_host, _port);
        client.login(_username, _password);
        client.changeWorkingDirectory(_remotePath);

        localPath = _localPath;
    }

    /**
     * ���������
     *
     * @param
     * @throws Exception
     * @author �����
     */
    public OutputStream getOutputStream(String fileName) throws Exception {
        return client.storeFileStream(fileName);
    }

    /**
     * ��Զ���ļ��ƶ���Զ����ʷĿ¼
     *
     * @param fileName
     * @throws Exception
     * @author ½����(Mail:luxm@asiainfo.comMSN:lxm_js@hotmail.com)
     */
    public void moveFileToRemoteHisDir(String fileName) throws Exception {
        if (client.listFiles(fileName).length == 0) {
            throw new Exception("move file " + fileName + " to his error");
        }
        StringBuffer newFileName = new StringBuffer();
        newFileName.append(getRemotePathHis());
        newFileName.append(FILE_SPEARATROR);
        newFileName.append(fileName);
        rename(fileName, newFileName.toString());
    }

    /**
     * ��ȡԶ���ļ���ʷĿ¼
     *
     * @return
     * @author ½����(Mail:luxm@asiainfo.comMSN:lxm_js@hotmail.com)
     */
    public String getRemotePathHis() throws Exception {
        if (remotePathHis == null || remotePathHis.length() < 1) {
            throw new Exception("get his path error");
        }
        return remotePathHis;
    }

    //add by ������ 2008-11-19
    public String getWorkingDirectory() throws Exception {
        if (this.client != null) {
            return this.client.printWorkingDirectory();
        }
        return null;
    }

    public void changeWorkingDirectory(String pathName) throws Exception {
        if (this.client != null) {
            this.client.changeWorkingDirectory(pathName);
        }
    }

    //add by ������ 2009-2-4
    public String getLocalPath() {
        return this._localPath;
    }

    public String getLocalPathTemp() {
        return this._localPathTemp;
    }

    public String getRemotePath() {
        return _remotePath;
    }

    public void enterLocalPassiveMode() throws Exception {
        client.enterLocalPassiveMode();
    }



    /**
     * add by xingnn
     * �������ļ�����׷�����
     *
     * @param remoteName
     * @param in
     * @return
     * @throws Exception
     */
    public boolean appendFile(String remoteName, InputStream in) throws Exception {
        return client.appendFile(remoteName, in);
    }

    /**
     * �л���ָ��Ŀ¼(�����ھʹ���Ŀ¼ͬʱ�л����½���Ŀ¼)
     *
     * @param directoryId
     * @throws Exception
     */
    public void changeToRightDir(String directoryId) throws Exception {
        if (client != null) {
            String ftpDir = client.printWorkingDirectory();
            if (ftpDir == null)
                throw new Exception("FTP is NULL");
            String curDir = ftpDir + FILE_SPEARATROR + directoryId;
            boolean flag = client.changeWorkingDirectory(curDir);
            if (flag == false) {
                //�����ھʹ���Ŀ¼ͬʱ�л����½���Ŀ¼
                client.makeDirectory(directoryId);
                client.changeWorkingDirectory(curDir);
            }
        }
    }




}
