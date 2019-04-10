package com.octopus.utils.net.ftp;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * User: wangfeng2
 * Date: 14-8-14
 * Time: 下午5:25
 */
public class FTPClient extends org.apache.commons.net.ftp.FTPClient {

    String encodingName(String remote) throws UnsupportedEncodingException {
        return new String(remote.getBytes(super.getControlEncoding()),"ISO-8859-1");
    }

    @Override
    public boolean storeFile(String remote, InputStream local)throws IOException{
        return super.storeFile(encodingName(remote), local);
    }

    @Override
    public boolean retrieveFile(String remote, OutputStream local)throws IOException{
        return super.retrieveFile(encodingName(remote), local);
    }

    @Override
    public InputStream retrieveFileStream(String remote) throws IOException{
        return super.retrieveFileStream(encodingName(remote));
    }

    @Override
    public OutputStream storeFileStream(String remote) throws IOException{
        return super.storeFileStream(encodingName(remote));
    }

    @Override
    public boolean appendFile(String remote, InputStream local)throws IOException{
        return super.appendFile(encodingName(remote), local);
    }

    @Override
    public OutputStream appendFileStream(String remote) throws IOException{
        return super.appendFileStream(encodingName(remote));
    }

    @Override
    public boolean storeUniqueFile(String remote, InputStream local)throws IOException{
        return super.storeUniqueFile(encodingName(remote), local);
    }

    @Override
    public OutputStream storeUniqueFileStream(String remote) throws IOException{
        return super.storeUniqueFileStream(encodingName(remote));
    }

    @Override
    public boolean rename(String from, String to) throws IOException{
        return super.rename(encodingName(from), encodingName(to));
    }

    @Override
    public boolean deleteFile(String pathname) throws IOException{
        return super.deleteFile(encodingName(pathname));
    }

    @Override
    public boolean removeDirectory(String pathname) throws IOException{
        return super.removeDirectory(encodingName(pathname));
    }

    @Override
    public boolean makeDirectory(String pathname) throws IOException{
        return super.makeDirectory(encodingName(pathname));
    }

    @Override
    public boolean changeWorkingDirectory(String pathname) throws IOException{
        return super.changeWorkingDirectory(encodingName(pathname));
    }

    public String getStatus(String pathname)throws IOException{
        return super.getStatus(encodingName(pathname));
    }

    /**
     * 存储文件，如果没有目录则创建目录存储
     * @param pathname
     * @param in
     * @return
     * @throws IOException
     */
    public boolean storeFileWithCheckPath(String pathname,InputStream in)throws IOException{
        if(pathname.contains("/")||pathname.contains("\\")){
            if(!isExist(pathname)){
                int dex = pathname.lastIndexOf("/");
                int dex2 = pathname.lastIndexOf("\\");
                if(dex2>dex) dex=dex2;
                if(makeMultiDirectory(pathname.substring(0,dex))){
                    return storeFile(pathname,in);
                }else{
                    return false;
                }
            }else{
                return storeFile(pathname,in);
            }
        }else{
            return storeFile(pathname,in);
        }
    }

    public boolean retrieveFile(String remotepath,String localpath)throws IOException{
        File lf = new File(localpath);
        if(!lf.exists()){
            new File(lf.getParent()).mkdirs();
        }
        FileOutputStream out = new FileOutputStream(lf);
        boolean ret =  retrieveFile(remotepath,out);
        out.close();
        return ret;
    }

    public boolean isExist(String pathname)throws IOException{
        String state = getStatus(pathname);
        if(state.split("\n").length>2){
            return true;
        }
        return false;
    }

    public boolean makeMultiDirectory(String pathname)throws IOException{
        List<String> li = new ArrayList();
        if(pathname.indexOf("\\")>=0 || pathname.indexOf("/")>=0){
            String[] ps = pathname.split("\\\\");
            for(String s:ps){
                if(s.indexOf("/")>=0){
                    String[] ts = s.split("/");
                    for(String t:ts){
                        if(null != t && !"".equals(t.trim())){
                            li.add(t);
                        }
                    }
                }else{
                    li.add(s);
                }
            }
        }else{
            li.add(pathname);
        }
        int count=0;
        for(int i=0;i<li.size();i++){
            if(changeWorkingDirectory(li.get(i))){
                count++;
                continue;
            }else{
                if(makeDirectory(li.get(i))){
                    if(changeWorkingDirectory(li.get(i))){
                        count++;
                    }else {
                        return false;
                    }
                }else{
                    return false;
                }
            }
        }
        for(int i=0;i<count;i++){
            changeToParentDirectory();
        }
        return true;
    }



}
