package com.octopus.isp.actions;

import com.octopus.utils.file.FileUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;

/**
 *
 * Created by Administrator on 2019/4/17.
 */
public class ServiceGuideAction extends XMLDoObject {
    public ServiceGuideAction(XMLMakeup xml, XMLObject parent, Object[] containers) throws Exception {
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
        if(null != input){
            String op = (String)input.get("op");
            String name = (String)input.get("name");
            if("getEmptyBody".equals(op)){
                return getEmptyBody(name);
            }else if("getCommonServiceProperties".equals(op)){
                return getCommonServiceProperties();
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
        return true;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret, Exception e) throws Exception {
        return true;
    }


    String getEmptyBody(String name){
        try {
            String body = FileUtils.getProtocolFile("classpath:com/octopus/isp/actions/templet/ServiceBody.tpl");
            if(StringUtils.isNotBlank(name)) {
                return body.replace("key=\"\"", "key=\"" + name + "\"");
            }else{
                return body;
            }
        }catch (Exception e){
            log.error("",e);
            return "";
        }
    }


    List<Map<String,Object>> getCommonServiceProperties(){
        return null;
    }
    List<String> getActionNames(){
        return null;
    }
    String getAction(){
        return null;
    }
    void getEnvVars(){

    }
    void getServiceParameters(){

    }
    List<String> getStringFunction(){
        return null;
    }

    List<Map<String,Object>> getTags(){
        return null;
    }

    List getTagProperties(String tagName){
        return null;
    }

    List getTagPropertiesValue(String tagName,String propertyName,String path){
        return null;
    }

}
