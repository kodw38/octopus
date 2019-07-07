package com.octopus.isp.bridge.launchers.convert;

import com.octopus.isp.bridge.launchers.IConvert;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.XMLParameter;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

/**
 * User: Administrator
 * Date: 14-9-29
 * Time: 上午11:11
 */
public class ConvertRequestData2JSONArray extends XMLObject implements IConvert {
    public ConvertRequestData2JSONArray(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    @Override
    public void notifyObject(String op, Object obj) throws Exception {

    }

    @Override
    public void addTrigger(Object cond, InvokeTaskByObjName task) throws Exception {

    }

    @Override
    public void destroy() throws Exception {

    }

    @Override
    public void initial() throws Exception {

    }

    @Override
    public Object convert(XMLParameter env,Object par) throws IOException {
        HttpServletRequest request = (HttpServletRequest)par;
        request.setCharacterEncoding("UTF-8");
        String paramStr=null;

//        StringBuffer sb = new StringBuffer();
//        BufferedReader br = new BufferedReader(new InputStreamReader((ServletInputStream)par.getRequest().getInputStream(),"UTF-8"));
//        String line = null;
//        while((line = br.readLine())!=null){
//            sb.append(line);
//        }
        int len = request.getContentLength();
        if(len>0){
            byte buffer[] = new byte[len];

            InputStream in = request.getInputStream();
            int total = 0;
            int once = 0;
            while ((total < len) && (once >=0)) {
                once = in.read(buffer,total,len);
                total += once;
            }

            paramStr = new String(URLDecoder.decode(new String(buffer),"UTF-8")).trim();
        }

        if(null != paramStr && paramStr.length()>0){
            Map<String,Object> paramMap = new HashMap<String,Object>();
            if(paramStr.startsWith("{") && paramStr.endsWith("}")){
                paramStr = "[".concat(paramStr).concat("]");
            }else if(paramStr.contains(StringUtils.AMPERSAND)){ //多个参数  name=value&name1=value1
                String[] params = paramStr.split(StringUtils.AMPERSAND);
                if(params!=null && params.length>0){
                    for(String param:params){
                        String[] paramArray = param.split(StringUtils.EQUAL);
                        paramMap.put(paramArray[0], StringUtils.BLANK);
                        if(paramArray!=null && paramArray.length==2){
                            String paramKey = paramArray[0];
                            String paramValue =  java.net.URLDecoder.decode(paramArray[1],StringUtils.UTF8);
                            if(StringUtils.isNotEmpty(paramValue)){
                                if(StringUtils.trim(paramValue).startsWith("{")&&
                                        StringUtils.trim(paramValue).endsWith("}")){
                                    paramMap.put(paramKey,JSONObject.fromObject(paramValue));
                                }else{
                                    paramMap.put(paramKey, paramValue);
                                }
                            }
                        }
                    }
                }

            }else if(paramStr.contains(StringUtils.EQUAL)){//一个参数 name=value
                String[] paramArray = paramStr.split(StringUtils.EQUAL);
                paramMap.put(paramArray[0], "");
                if(paramArray!=null && paramArray.length==2){
                    String paramKey = paramArray[0];
                    String paramValue = java.net.URLDecoder.decode(paramArray[1],StringUtils.UTF8);
                    if(StringUtils.isNotEmpty(paramValue)){
                        if(StringUtils.trim(paramValue).startsWith("{")&&
                                StringUtils.trim(paramValue).endsWith("}")){
                            paramMap.put(paramKey,JSONObject.fromObject(paramValue));
                        }else{
                            paramMap.put(paramKey, paramValue);
                        }
                    }
                }

            }
            if(!paramMap.isEmpty()){
                paramStr = "[".concat(JSONObject.fromObject(paramMap).toString()).concat("]");
            }
            if(paramStr.startsWith("[")){
                JSONArray array = JSONArray.fromObject(paramStr);

                return array;
            }
        }
        return null;
    }
}
