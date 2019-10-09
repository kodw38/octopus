package com.octopus.utils.net.ssh;


import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.file.FileUtils;
import com.octopus.utils.zip.ReplaceZipItem;
import com.octopus.utils.zip.ZipUtil;
import com.jcraft.jsch.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.*;

public class SSHClient {
    static transient Log log = LogFactory.getLog(SSHClient.class);
    Session sshSession = null;
    String host;
    int port;
    String username;
    String password;

    /**
     * ����sftp������
     *
     * @param host
     *            ����
     * @param port
     *            �˿�
     * @param username
     *            �û���
     * @param password
     *            ����
     * @return
     */
    public SSHClient(String host, int port, String username, String password) throws Exception{
        this.host=host;
        this.port=port;
        this.username=username;
        this.password=password;
        connect(host,port,username,password);
    }
    protected void connect(String host, int port, String username, String password) throws Exception{
        JSch jsch = new JSch();
        sshSession = jsch.getSession(username, host, port);
        sshSession.setPassword(password);
        Properties sshConfig = new Properties();
        sshConfig.put("StrictHostKeyChecking", "no");
        sshSession.setConfig(sshConfig);
        sshSession.connect();

    }
    Channel getChannel(String type,String cmd) throws Exception {
        try{
            if(!sshSession.isConnected())
                sshSession.connect();
            Channel chl = sshSession.openChannel(type);
            if(type.equals("exec") && null != cmd){
                ((ChannelExec)chl).setCommand(cmd);
            }
            chl.connect(1000);
            return chl;
        }catch (Exception e){
            sshSession.disconnect();
            connect(host,port,username,password);
            Channel chl = sshSession.openChannel(type);
            if(type.equals("exec") && null != cmd){
                ((ChannelExec)chl).setCommand(cmd);
            }
            chl.connect(1000);
            return chl;

        }
    }
    public StringBuilder shell(String cmd)throws Exception{
        try{
            ChannelShell channel = (ChannelShell)getChannel("shell",null);
            //获取输入流和输出流
            InputStream instream = channel.getInputStream();
            OutputStream outstream = channel.getOutputStream();
            log.info("executing shell in "+username+"@"+host+":"+port+", command:"+cmd);
            //发送需要执行的SHELL命令，需要用\n结尾，表示回车
            String shellCommand = cmd+" \necho **cmdend**\n";
            outstream.write(shellCommand.getBytes());
            outstream.flush();
    /*
            long l = System.currentTimeMillis();
            while(instream.available()<=0 && (System.currentTimeMillis()-l)<3000){
                Thread.sleep(10);
            }
            long c = instream.available();
            Thread.sleep(1000);
            while(instream.available()!=c){
                c = instream.available();
                Thread.sleep(1000);
            }
    */
            //获取命令执行的结果
            StringBuilder temp=new StringBuilder();
            while (!temp.toString().contains("\n**cmdend**")) {
                byte[] data = new byte[instream.available()];
                int nLen = instream.read(data);
                if (nLen < 0) {
                    throw new Exception("network error.");
                }
                String s = new String(data, 0, nLen,"iso8859-1");
                if(log.isDebugEnabled()){
                    if(StringUtils.isNotBlank(s)) {
                        log.debug("get return msg:" + s);
                    }
                }
                //转换输出结果并打印出来
                temp.append(s);

                Thread.sleep(100);
            }

            outstream.close();
            instream.close();

            channel.disconnect();
            log.info("finished shell in "+username+"@"+host+":"+port+", command:"+cmd);
            if(temp.length()>0)
                return temp;
            else
                return null;
        }catch (Exception e){
            log.error("execute shell "+cmd,e);
            return null;
        }

    }

    public Object[] getShellInputStream(String cmd)throws Exception{
        Channel channel = getChannel("shell",null);
        InputStream in = printShell(channel,cmd+"\n");
        return new Object[]{channel,in};
    }

    public InputStream printShell(Channel channel,String cmd)throws Exception{
        try{

            //获取输入流和输出流
            InputStream instream = channel.getInputStream();
            OutputStream outstream = channel.getOutputStream();
            //发送需要执行的SHELL命令，需要用\n结尾，表示回车
            String shellCommand = cmd;
            outstream.write(shellCommand.getBytes());
            outstream.flush();
    /*
            long l = System.currentTimeMillis();
            while(instream.available()<=0 && (System.currentTimeMillis()-l)<3000){
                Thread.sleep(10);
            }
            long c = instream.available();
            Thread.sleep(1000);
            while(instream.available()!=c){
                c = instream.available();
                Thread.sleep(1000);
            }
    */
            return instream;
        }catch (Exception e){
            log.error("execute shell "+cmd,e);
            return null;
        }

    }

    public StringBuffer exec(String cmd) throws Exception {

        try{
        ChannelExec channelExec = (ChannelExec) getChannel("exec",cmd);
        channelExec.run();

        InputStream in = channelExec.getInputStream();

        StringBuffer buf = new StringBuffer(1024);
        byte[] tmp = new byte[1024];
        while (true) {
            while (in.available() > 0) {
                int i = in.read(tmp, 0, 1024);
                if (i < 0)
                    break;
                buf.append(new String(tmp, 0, i));
            }
            if (channelExec.isClosed()) {
                break;
            }
        }
        channelExec.disconnect();
            if(log.isDebugEnabled())
                log.debug("exec:"+cmd+"\n"+buf.toString());
        return buf;
        }catch (Exception e){
           log.error("execute exec "+cmd,e);
            return null;
        }
    }


    public void close(){
        if(null !=sshSession && sshSession.isConnected())
            sshSession.disconnect();

    }

    /**
     * �ϴ��ļ�
     *
     * @param directory
     *            �ϴ���Ŀ¼
     * @param uploadFile
     *            Ҫ�ϴ����ļ�
     */
    public void upload(String directory, String uploadFile) throws Exception{
        try{
        ChannelSftp sftp = (ChannelSftp)getChannel("sftp",null);
        try{
            if(null != directory && !"".equals(directory))
                cd(sftp,directory);
            File file = new File(uploadFile);
            sftp.put(new FileInputStream(file), file.getName());
            sftp.cd(sftp.getHome());
        }finally {
            sftp.disconnect();
        }
        }catch (Exception e){
            log.error("upload "+uploadFile,e);
        }
    }

    public void upload(String directory, String name, InputStream stream) throws Exception{
        try{
        ChannelSftp sftp = (ChannelSftp)getChannel("sftp",null);
        try{
        if(StringUtils.isNotBlank(directory))
            cd(sftp,directory);
        sftp.put(stream, name);
        sftp.cd(sftp.getHome());
        }finally {
            sftp.disconnect();
        }
        }catch (Exception e){
            log.error("upload "+name,e);
        }
    }
    public void upload(String filename, InputStream stream) throws Exception{
        try{
        ChannelSftp sftp = (ChannelSftp)getChannel("sftp",null);
        try{
        filename = filename.replaceAll("\\\\","/");
        String path = filename.substring(0,filename.lastIndexOf("/"));
        String fileName = filename.substring(filename.lastIndexOf("/")+1);
        cd(sftp,path);
        upload(path,fileName, stream);
        sftp.cd(sftp.getHome());
        }finally {
            sftp.disconnect();
        }
        }catch (Exception e){
            log.error("uplaod "+filename,e);
        }
    }
    public Session getSession(){
        return sshSession;
    }

    /**
     * �����ļ�
     *
     * @param directory
     *            ����Ŀ¼
     * @param downloadFile
     *            ���ص��ļ�
     * @param saveFile
     *            ���ڱ��ص�·��
     * @param
     */
    public void download(String directory, String downloadFile, String saveFile) throws Exception{
        try{
        ChannelSftp sftp = (ChannelSftp)getChannel("sftp",null);
        try{
        cd(sftp,directory);
        File file = new File(saveFile);
        sftp.get(downloadFile, new FileOutputStream(file));
        sftp.cd(sftp.getHome());
        }finally {
            sftp.disconnect();
        }
        }catch (Exception e){
            log.error("download "+downloadFile,e);
        }
    }
    public void download(String directory, String downloadFile, OutputStream saveFile) throws Exception{
        try{
        ChannelSftp sftp = (ChannelSftp)getChannel("sftp",null);
        try{
        if(StringUtils.isNotBlank(directory))
            cd(sftp,directory);
        sftp.get(downloadFile, saveFile);
        sftp.cd(sftp.getHome());
        }finally {
            sftp.disconnect();
        }
        }catch (Exception e){
            log.error("download "+downloadFile,e);
        }
    }

    /**
     * ɾ���ļ�
     *
     * @param directory
     *            Ҫɾ���ļ�����Ŀ¼
     * @param deleteFile
     *            Ҫɾ����ļ�
     * @param
     */
    public void delete(String directory, String deleteFile) throws Exception{
        try{
        ChannelSftp sftp = (ChannelSftp)getChannel("sftp",null);
        try{
        cd(sftp,directory);
        sftp.rm(deleteFile);
        }finally {
            sftp.disconnect();
        }
        }catch (Exception e){
            log.error("delete "+deleteFile,e);
        }
    }

    private void cd(ChannelSftp sftp,String directory) throws Exception{
        try{
            sftp.cd(directory);
        }catch(Exception e){
            mkdir(sftp,directory);
        }

    }

    protected void mkdir(ChannelSftp sftp,String directory) throws Exception{
        if (directory.indexOf("\\") >= 0 || directory.indexOf("/") >= 0) {
            directory = directory.replaceAll("\\\\", "/");
            String[] li = directory.split("/");
            boolean isfromboot=false;
            if(directory.startsWith("/")){
                isfromboot=true;
            }
            for (int i = 0; i < li.length; i++) {
                if(i==0&& isfromboot){
                    sftp.cd((isfromboot?"/":"")+li[i]);
                    continue;
                }
                if (null != li[i] && !"".equals(li[i])) {

                    try {
                        sftp.cd(li[i]);
                        continue;
                    } catch (Exception e) {
                        try {
                            sftp.mkdir(li[i]);
                            sftp.cd(li[i]);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        } else {
            try {
                sftp.mkdir(directory);
                sftp.cd(directory);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


    }

    /**
     * �г�Ŀ¼�µ��ļ�
     *
     * @param directory
     *            Ҫ�г���Ŀ¼
     * @param
     * @return
     * @throws SftpException
     */
    public Vector listFiles(String directory)
            throws Exception {
        ChannelSftp sftp = (ChannelSftp)getChannel("sftp",null);

        Vector vs = sftp.ls(directory);
        sftp.disconnect();
        return vs;
    }
    public boolean copyTextFileToMutilRemoteFile(File localFile,String ignorePathHead,String remotePath,Map<String,Map<String,String>> chgValues){
        try{
            //处理路径
            InputStream input = new FileInputStream(localFile);
            String de = "";
            String fp = localFile.getPath();
            fp = fp.replaceAll("\\\\", "/");
            ignorePathHead = ignorePathHead.replaceAll("\\\\", "/");
            if(StringUtils.isNotBlank(ignorePathHead) && fp.indexOf(ignorePathHead)+ignorePathHead.length()+1 < fp.lastIndexOf("/")){
                fp = fp.substring(fp.indexOf(ignorePathHead)+ignorePathHead.length()+1,fp.lastIndexOf("/"));
            }else{
                fp = "";
            }
            if(null == remotePath) remotePath="";
            remotePath = remotePath.replaceAll("\\\\", "/");
            if(remotePath.endsWith("/")|| remotePath.endsWith("\\")){
                de = remotePath+ fp;
            }else if(!"".equals(remotePath)){
                de = remotePath+"/"+fp;
            }
            Map<String,byte[]> splitMap =new HashMap<String,byte[]>();
            if(null != chgValues && chgValues.size()>0){
                String sb = FileUtils.getFileContentStringBuffer(new FileInputStream(localFile)).toString();
                Iterator<String> its = chgValues.keySet().iterator();
                while(its.hasNext()){
                    String subName = its.next();
                    Map<String,String> cvs = chgValues.get(subName);
                    if(null != cvs){
                        Iterator<String> ts = cvs.keySet().iterator();
                        String pp = sb;
                        while(ts.hasNext()){
                            String k = ts.next();
                            String v = cvs.get(k);
                            pp = StringUtils.replace(pp,k,v);
                        }
                        splitMap.put(subName,pp.getBytes());
                    }
                }
            }

            //根据文件名变换为多个文件
            if(null != splitMap && splitMap.size()>0){
                Iterator its = splitMap.keySet().iterator();
                boolean isf = true;
                while(its.hasNext()){
                    String sub = (String)its.next();
                    if(!isf)de="";
                    upload(de, localFile.getName().substring(0, localFile.getName().lastIndexOf(".")) + "_" + sub + "." + localFile.getName().substring(localFile.getName().lastIndexOf(".") + 1)
                            , new ByteArrayInputStream(splitMap.get(sub)));
                    if(isf) isf = false;
                }
                return true;
            }else{
                upload(de, localFile.getName(),input );
                return true;
            }
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public boolean copyFileToRemote(InputStream in,String remoteFile) {
        try{
            String dir=null;
            String name = null;
            remoteFile = remoteFile.replaceAll("\\\\", "/");
            if(remoteFile.contains("/")){
                dir = remoteFile.substring(0,remoteFile.lastIndexOf("/"));
                name=remoteFile.substring(remoteFile.lastIndexOf("/")+1);
            }else{
                name=remoteFile;
            }
            upload(dir,name,in);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }

    }

    public boolean copyTextFileToRemote(File localFile,String ignorePathHead,String remotePath,Map<String,String> chgValues){

        try{
            //处理路径
            InputStream input = new FileInputStream(localFile);
            String de = "";
            String fp = localFile.getPath();
            fp = fp.replaceAll("\\\\", "/");
            ignorePathHead = ignorePathHead.replaceAll("\\\\", "/");
            if(StringUtils.isNotBlank(ignorePathHead) && fp.indexOf(ignorePathHead)+ignorePathHead.length()+1 < fp.lastIndexOf("/")){
                fp = fp.substring(fp.indexOf(ignorePathHead)+ignorePathHead.length()+1,fp.lastIndexOf("/"));
            }else{
                fp = "";
            }
            if(null == remotePath) remotePath="";
            remotePath = remotePath.replaceAll("\\\\", "/");
            if(remotePath.endsWith("/")|| remotePath.endsWith("\\")){
                de = remotePath+ fp;
            }else if(!"".equals(remotePath)){
                de = remotePath+"/"+fp;
            }

            if(null != chgValues && chgValues.size()>0){
                String sb = FileUtils.getFileContentStringBuffer(new FileInputStream(localFile)).toString();
                Iterator<String> its = chgValues.keySet().iterator();
                while(its.hasNext()){
                    String key = its.next();
                    sb = StringUtils.replace(sb,key,chgValues.get(key));
                }
                input = new ByteArrayInputStream(sb.getBytes());
            }
            upload(de, localFile.getName(),input );
            return true;

        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
    static String getName(String header,String catalog,String filePath){
        return header+filePath.replaceAll("\\\\","/").substring(catalog.length());
    }

    public boolean copyZipFileToRemote(InputStream file,String remotePath,Map<String,List<String>> addFiles,ReplaceZipItem[] zipChgValues) {
        try{
            Map<String,InputStream> m = new HashMap();
            if(null != addFiles && addFiles.size()>0){
                Iterator<String> its = addFiles.keySet().iterator();
                while(its.hasNext()){
                    String head = its.next();
                    List<String> ps = addFiles.get(head);
                    for(String p:ps){
                        File f = new File(p);
                        if(f.exists()){
                            if(f.isDirectory()){
                                List<File> ls =FileUtils.getAllFile(p,null);
                                for(File ff:ls){
                                    m.put(getName(head,p,ff.getPath()),new FileInputStream(ff));
                                }
                            }else{
                                m.put(getName(head,"",p),new FileInputStream(f));
                            }
                        }
                    }
                }
            }
            if(null != m || null != zipChgValues){
                file = ZipUtil.changeFile(file,m, zipChgValues,null);
            }
            remotePath = remotePath.replaceAll("\\\\","/");
            String dir=null,name=null;
            dir = remotePath.substring(0,remotePath.lastIndexOf("/"));
            name=remotePath.substring(remotePath.lastIndexOf("/")+1);
            upload(dir, name,file );
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
    /**
     * 把压缩文件在本地替换完变量后上传导远程服务器
     * @param localFile
     * @param ignorePathHead
     * @param remotePath
     * @param zipChgValues
     * @return
     */
    public boolean copyZipFileToRemote(File localFile,String ignorePathHead,String remotePath,String newFileName,Map<String,List<String>> addFiles,ReplaceZipItem[] zipChgValues,String fileencoding) {
        try{
            log.debug("copy convert file "+localFile +" with "+fileencoding);
            //处理路径
            InputStream input = new FileInputStream(localFile);
            String de = "";
            String fp = localFile.getPath();
            fp = fp.replaceAll("\\\\", "/");
            if(null != ignorePathHead)
                ignorePathHead = ignorePathHead.replaceAll("\\\\", "/");
            if(StringUtils.isNotBlank(ignorePathHead) && fp.indexOf(ignorePathHead)+ignorePathHead.length()+1 < fp.lastIndexOf("/")){
                fp = fp.substring(fp.indexOf(ignorePathHead)+ignorePathHead.length()+1,fp.lastIndexOf("/"));
            }else{
                fp = "";
            }
            if(null == remotePath) remotePath="";
            remotePath = remotePath.replaceAll("\\\\", "/");
            if(remotePath.endsWith("/")|| remotePath.endsWith("\\")){
                de = remotePath+ fp;
            }else if(!"".equals(remotePath)){
                de = remotePath+"/"+fp;
            }

            //如果是压缩文件替换,替换必须是文本文件
            InputStream in = new FileInputStream(localFile);
            Map<String,InputStream> m = new HashMap();
            if(null != addFiles && addFiles.size()>0){
                Iterator<String> its = addFiles.keySet().iterator();
                while(its.hasNext()){
                    String head = its.next();
                    List<String> ps = addFiles.get(head);
                    for(String p:ps){
                        File f = new File(p);
                        if(f.exists()){
                            if(f.isDirectory()){
                                List<File> ls =FileUtils.getAllFile(p,null);
                                for(File ff:ls){
                                    m.put(getName(head,p,ff.getPath()),new FileInputStream(ff));
                                }
                            }else{
                                m.put(getName(head,"",p),new FileInputStream(f));
                            }
                        }
                    }
                }
            }
            if((null != m && m.size()>0) || (null != zipChgValues && zipChgValues.length>0)){
                if(StringUtils.isBlank(fileencoding)) {
                    input = ZipUtil.changeFile(in, m, zipChgValues, "utf-8");
                }else{
                    input = ZipUtil.changeFile(in,m,zipChgValues,fileencoding);
                }
            }

            /*ByteArrayOutputStream of = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int len = 0;
            while (( len = input.read( b ) ) > 0) {
                of.write( b, 0, len );
            }
            FileOutputStream out = new FileOutputStream("c:\\logs\\auth.bak.war");
            out.write(of.toByteArray());
            out.close();
*/
            upload(de, newFileName,input );
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public static void main(String[] args) {
        SSHClient u =null;
        try {
            u = new SSHClient("119.28.66.159",22,"umobile_ftp","umobile@ftp");
            Channel  channel= u.getChannel("shell",null);
            BufferedReader lr = new BufferedReader(new InputStreamReader(u.printShell(channel,"nmon -tlp\n")));
//获取命令执行的结果
            String line;
            int c=0;
            System.out.println("--------begin------------");
            while ((line = lr.readLine())!=null) {
                System.out.println(line);
                c++;
                if(c>20){
                    break;
                }
            }
            channel.disconnect();
            System.out.println("--------end------------");

        } catch (Exception e) {
            e.printStackTrace();
        }finally{
            if(null!=u)
                u.close();
        }
    }
}
