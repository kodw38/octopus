package com.octopus.tools.client.hadoop;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.exception.ISPException;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;

import java.io.ByteArrayOutputStream;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by admin on 2020/3/1.
 */
public class HDFSClient extends XMLDoObject{
    FileSystem fileSystem;
    public HDFSClient(XMLMakeup xml, XMLObject parent, Object[] containers) throws Exception {
        super(xml, parent, containers);
    }

    /**
     * fs.DefaultFS
     * @throws Exception
     */
    @Override
    public void doInitial() throws Exception {
        Configuration config = new Configuration();
        String s = getXML().getProperties().getProperty("config");
        if(StringUtils.isNotBlank(s)){
            Map m = StringUtils.convert2MapJSONObject(s);
            Iterator its = m.keySet().iterator();
            while(its.hasNext()) {
                String k = (String)its.next();
                String v = (String)m.get(k);
                config.set(k,v);
            }
            fileSystem = FileSystem.get(config);
        }
    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(null != fileSystem && null != input){
            String op = (String)input.get("op");
            if("getOutputStream".equals(op)){
                String path = (String)input.get("remoteFile");
                if(StringUtils.isBlank(path)) throw new ISPException("NOT_PARAMETER","HDFS getOutputStream please input [targetFile] parameter");
                FSDataInputStream in = fileSystem.open(new Path(path));
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                IOUtils.copy(in,out);
                return out;
            }else if("uploadFile".equals(op)){
                String targetFile = (String)input.get("remoteFile");
                String srcFile = (String)input.get("localFile");
                fileSystem.copyFromLocalFile(new Path(srcFile),new Path(targetFile));
                return true;
            }else if("list".equals(op)){
                String path = (String)input.get("remoteFile");
                if(StringUtils.isBlank(path)) throw new ISPException("NOT_PARAMETER","HDFS getOutputStream please input [targetFile] parameter");
                FileStatus[] fs = fileSystem.listStatus(new Path(path));
                if(null != fs){
                    return fs;
                }
            }else if("delete".equals(op)){
                String path = (String) input.get("remoteFile");
                if (StringUtils.isBlank(path))
                    throw new ISPException("NOT_PARAMETER", "HDFS getOutputStream please input [targetFile] parameter");
                return fileSystem.delete(new Path(path),true);
            }else if("listFiles".equals(op)) {
                String path = (String) input.get("remoteFile");
                if (StringUtils.isBlank(path))
                    throw new ISPException("NOT_PARAMETER", "HDFS getOutputStream please input [targetFile] parameter");
                return fileSystem.listFiles(new Path(path),false);
            }else if("listAllFiles".equals(op)){
                String path = (String) input.get("remoteFile");
                if (StringUtils.isBlank(path))
                    throw new ISPException("NOT_PARAMETER", "HDFS getOutputStream please input [targetFile] parameter");
                return fileSystem.listFiles(new Path(path),true);
            }

        }
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
}
