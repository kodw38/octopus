package com.octopus.tools.utils;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.file.FileUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.Iterator;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 16-1-12
 * Time: 下午9:20
 */
public class FileUpload extends XMLDoObject {
    public FileUpload(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        try{
            if(null != input){
                Map map = (Map)input.get("files");
                String path = (String)input.get("path");
                if(StringUtils.isBlank(path)){
                    path = (String)config.get("path");
                }
                String name = (String)input.get("name");
                if(StringUtils.isBlank(name)){
                    name = (String)config.get("name");
                }
                String op = (String)input.get("op");
                if(StringUtils.isBlank(op)){
                    op = (String)config.get("op");
                }
                if(null != map && StringUtils.isNotBlank(path)){
                    if(StringUtils.isNotBlank(name) && map.size()==1){
                        Iterator its = map.keySet().iterator();
                        while(its.hasNext()){
                            String k = (String)its.next();
                            map.put(name,map.get(k));
                            map.remove(k);
                        }
                    }
                    if("upload".equals(op)){
                        FileUtils.saveFiles(map,path,true);
                        if(null !=input.get("rtn") && "downloadPath".equals(input.get("rtn"))){
                            if(map.size()==1){
                                String sp =  "{\"error\":0,\"url\":\"download?file="+path+"/"+map.keySet().iterator().next()+"\"}";
                                return sp;
                            }else {
                                return "<script>alert('upload success');window.history.back();</script>";  //To change body of implemented methods use File | Settings | File Templates.
                            }
                        }else {
                            return "<script>alert('upload success');window.history.back();</script>";  //To change body of implemented methods use File | Settings | File Templates.
                        }
                    }else if("download".equals(config.get("op"))){

                    }else{
                        log.error("not match op[upload/download] , actual is "+op);
                    }
                }else{
                    log.error("not find upload file , or path is null "+path);
                }
            }
            return "false";
        }catch (Exception e){
            throw new Exception("upload file error",e);
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
