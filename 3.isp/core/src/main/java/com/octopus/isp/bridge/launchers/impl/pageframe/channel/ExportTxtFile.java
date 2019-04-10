package com.octopus.isp.bridge.launchers.impl.pageframe.channel;

import com.octopus.isp.ds.RequestParameters;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.Map;

/**
 * Created by robai on 2017/12/20.
 */
public class ExportTxtFile extends XMLDoObject {
    public ExportTxtFile(XMLMakeup xml, XMLObject parent, Object[] containers) throws Exception {
        super(xml, parent, containers);
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(null != env && env instanceof RequestParameters){
            String type = (String)((RequestParameters)env).getQueryStringMap().get("export");
            String name = (String)((RequestParameters)env).getQueryStringMap().get("filename");
            HttpServletResponse response = (HttpServletResponse)env.get("${response}");
            if(null != type && null != response){
                if("txt".equals(type)){
                    if(env.getResult() instanceof ResultCheck){
                        exportTxt(response,((ResultCheck)env.getResult()).getRet(),name);

                    }else{
                        exportTxt(response,env.getResult(),name);
                    }
                    ((RequestParameters)env).setStop();
                }
            }
        }
        return null;
    }
    void exportTxt(HttpServletResponse response,Object obj,String name)throws Exception{
        if(null != obj) {
            String fileName = URLDecoder.decode(name, "ISO8859_1");
            response.setContentType("application/x-msdownload");
            response.setHeader("Content-disposition", "attachment; filename=" + fileName);
            response.getOutputStream().write(obj.toString().getBytes());
            response.flushBuffer();
        }
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
