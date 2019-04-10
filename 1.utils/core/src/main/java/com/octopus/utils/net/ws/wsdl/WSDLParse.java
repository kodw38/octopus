package com.octopus.utils.net.ws.wsdl;

import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cls.ClassUtils;
import com.octopus.utils.cls.POJOUtil;
import com.octopus.utils.file.FileInfo;
import com.octopus.utils.file.FileUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

/**
 * Created by kod on 2017/4/25.
 */
public class WSDLParse {
    static transient Log log = LogFactory.getLog(WSDLParse.class);
    List<XMLMakeup> types = new ArrayList<XMLMakeup>();
    List<XMLMakeup> services = new ArrayList<XMLMakeup>();
    List<XMLMakeup> operations = new ArrayList<XMLMakeup>();
    Map<String,String> operaHeadMapping = new HashMap();
    String path = "com.wsdl";
    List<XMLMakeup> binding = null;
    List<XMLMakeup> messages = null;
    String className= "";
    String address = "";
    XMLMakeup root;
    Map<String,String> nsp = new HashMap();
    private static Map<String, String> nameSpaceMap = new HashMap<String, String>();
    private static final String[] elementSign = new String[]{"ref","type","base","name"};
    static Pattern pattern = null;
    static final String NSP_SPLIT = "#@#";
    static final String NSP_KV_SPLIT = "::";
    static final String TB_NSP = "tb_nsp";
    static final String regEx = "(\\d{1,4}[-|\\/|年|\\.]\\d{1,2}[-|\\/|月|\\.]\\d{1,2}([日|号])?(\\s)*(\\d{1,2}([点|时])?((:)?\\d{1,2}(分)?((:)?\\d{1,2}(秒)?)?)?)?(\\s)*(PM|AM)?)";
    
    private static final ThreadLocal<SimpleDateFormat> format = new ThreadLocal<SimpleDateFormat>(){
        @Override
           protected SimpleDateFormat initialValue()
           {
               return new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
           }
       };
       private static final ThreadLocal<SimpleDateFormat> format1 = new ThreadLocal<SimpleDateFormat>(){
           @Override
              protected SimpleDateFormat initialValue()
              {
                  return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
              }
          };
          
    public WSDLParse(){
    }
    public WSDLParse(String content)throws Exception{
        content = StringUtils.replace(content,"<![CDATA[","");
        content = StringUtils.replace(content,"]]>","");
        root= XMLUtil.getDataFromString(content);
        operations =root.findByEndWithNameAndEndWithParentName("operation","portType");
        binding = root.getChildrenEndWithName("wsdl:binding");
        messages = root.getChildrenEndWithName("message");
        services = root.getChildrenEndWithNameAndExistProperty("service","name");
        //获取服务报文头映射
        operaHeadMapping = getServiceHeaderMapping();
        List simpleType = root.getChildrenEndWithNameAndExistProperty("simpleType","name");
        List complexType = root.getChildrenEndWithNameAndExistProperty("complexType","name");
        List element = root.getChildrenEndWithNameAndExistProperty("element","name");
        getNsp();
        getServiceUrl();
        if(null != className)
        	path = path+"."+className.replaceAll("\\.", "_");
        types.addAll(simpleType);
        types.addAll(complexType);
        types.addAll(element);
    }
    //生成TB服务描述文件
    public Map<String,Map> getOperationDesc()throws Exception{
    	LinkedHashMap ret = new LinkedHashMap();
        for(XMLMakeup op:operations){
        	
            String operationName = op.getProperties().getProperty("name");
            String opRequestName = op.getFirstChildrenEndWithName("input").getProperties().getProperty("name");
            String opRequestType =removeNsp(op.getFirstChildrenEndWithName("input").getProperties().getProperty("message"));
            if(null == opRequestName)
            	opRequestName = op.getFirstChildrenEndWithName("input").getProperties().getProperty("message").split(":")[1];
            String opResponseName = op.getFirstChildrenEndWithName("output").getProperties().getProperty("name");
            String opResponseType =removeNsp(op.getFirstChildrenEndWithName("output").getProperties().getProperty("message"));
            if(null == opResponseName)
            	opResponseName = op.getFirstChildrenEndWithName("output").getProperties().getProperty("message").split(":")[1];
            Object nsp_in =null;
            /**Desc参数:parNames**/
            List<String> parNames = new ArrayList();
            XMLMakeup operationBinding = binding.get(0).findFirstTextByTitleProperty("wsdl:operation", "name", operationName);
            List<XMLMakeup> soapActionXml = operationBinding.getChildrenEndWithNameAndExistProperty("operation", "soapAction");
            /**Desc参数:soapAction**/
            String soapAction = null;
            if(null !=soapActionXml && soapActionXml.size()>0){
            	soapAction = soapActionXml.get(0).getProperties().getProperty("soapAction");
            }
            List<XMLMakeup> inputBody = operationBinding.findByEndWithNameAndEndWithParentName("body", "wsdl:input");
            String inputBodyParts = inputBody.get(0).getProperties().getProperty("parts");
            String reqtypename = null;
            try{
            	reqtypename = root.findFirstTextByTitleProperty("wsdl:message","name",opRequestType).findFirstTextByTitleProperty("wsdl:part", "name", inputBodyParts).getProperties().getProperty("element");
            }catch (Exception e) {
            	reqtypename = root.findFirstTextByTitleProperty("wsdl:message","name",opRequestType).getFirstChildrenEndWithName("part").getProperties().getProperty("element");
			}
            if(null != reqtypename){
            	String reqtypename1=removeNsp(reqtypename);
                XMLMakeup requestElem = root.findFirstTextByTitleProperty("element","name",reqtypename1);
                if(null == requestElem){
                	requestElem = root.findFirstTextByTitleProperty("xsd:element","name",reqtypename1);
                }
                if(requestElem.getType() != null) {
                	String inputTypeName = requestElem.getType();
//                if(inputTypeName.indexOf(":")>0){
//                    inputTypeName = inputTypeName.substring(inputTypeName.indexOf(":")+1);
                    nsp_in = parseType(getTypeXMLMakeup(removeNsp1(inputTypeName)),getNspByTypeName(inputTypeName));
                }
                else
                	nsp_in = parseType(requestElem,getNspByTypeName(reqtypename));
                if(nsp_in instanceof Map ){
                    parNames.addAll(((LinkedHashMap) nsp_in).keySet());
//                }
                }
            }
            /**Desc参数:nspInputMapping**/
            LinkedHashMap  nsp_input = new LinkedHashMap();
            nsp_input.put(changeNspSplit(reqtypename),nsp_in);
            /**Desc参数:nspOutputMapping**/
            LinkedHashMap nsp_output = new LinkedHashMap();
            //List<XMLMakeup> outputHeader = operationBinding.findByEndWithNameAndEndWithParentName("header", "wsdl:output");
            List<XMLMakeup> outputBody = operationBinding.findByEndWithNameAndEndWithParentName("body", "wsdl:output");
            String restypename = null;
            try{
            	//String outputHeadParts = outputHeader.get(0).getProperties().getProperty("parts");
                String outputBodyPart = outputBody.get(0).getProperties().getProperty("parts");
                restypename = root.findFirstTextByTitleProperty("wsdl:message","name",opResponseType).findFirstTextByTitleProperty("wsdl:part", "name", outputBodyPart).getProperties().getProperty("element");
            }catch (Exception e) {
            	restypename = root.findFirstTextByTitleProperty("wsdl:message","name",opResponseType).getFirstChildrenEndWithName("part").getProperties().getProperty("element");
			}
            //构造返回参数对象
            Object nsp_out = null;
            if(null != restypename){
            	String restypename1 = removeNsp(restypename);
                XMLMakeup outputElem = root.findFirstTextByTitleProperty("element","name",restypename1);
                if(null == outputElem){
                	outputElem = root.findFirstTextByTitleProperty("xsd:element","name",restypename1);
                }
                if(outputElem.getType() != null) {
                	String outputTypeName = outputElem.getType();
                	nsp_out = (LinkedHashMap) parseType(getTypeXMLMakeup(removeNsp1(outputTypeName)),getNspByTypeName(outputTypeName));
                }
                else
                	nsp_out = (LinkedHashMap) parseType(outputElem,getNspByTypeName(restypename));
//                List<XMLMakeup> cs = new LinkedList();
//                if(outputElem.getType() != null)
//                	cs = getTypeXMLMakeup(removeNsp1(outputElem.getType())).getChildren();
//                else
//                	cs = outputElem.getChildren();
//                for(XMLMakeup c:cs){
//                	nsp_output = parseType(c,getNspByTypeName(restypename));
//                    if(null!=nsp_output)
//                    	break;
//                }
            }
            nsp_output.put(changeNspSplit(restypename), nsp_out);
            Set<String> serviceNsp = new HashSet();
            /**Desc参数:input**/
            Map  input = removeNsp(nsp_input,serviceNsp);
            /**Desc参数:output**/
            Map  output = removeNsp(nsp_output,serviceNsp);
            for(int i=0;i<parNames.size();i++){
            	parNames.set(i, removeNsp(parNames.get(i)));
            }
            String nsp = constructNspForService(serviceNsp,this.nsp);
            //723
            Map inputNspMapping = getInputAndOutputNameNspMap(nsp_input,null);
            Map outputNspMapping = getInputAndOutputNameNspMap(nsp_output,null);
            //Map<String,String> operaHeadMapping = getServiceHeaderMapping();
            /**Desc参数:header**/
            Map nsp_header_input = getheaderByServiceName(operationName+"_input");
            Map nsp_header_output = getheaderByServiceName(operationName+"_output");
            ret.put(operationName,getDesc(operationName,path,address,parNames,input,output,nsp,inputNspMapping,outputNspMapping,nsp_header_input,nsp_header_output,soapAction));
        }
        return ret;
    }
    private String changeNspSplit(String str) {
    	return str.replace(":", NSP_SPLIT);
    }
    static String constructNspForService(Set<String> serviceNsp,Map<String,String> nsp){
    	StringBuffer ret = new StringBuffer();
    	for(String temp : serviceNsp){
    		if(null != nsp.get(temp)){
    			ret.append(temp).append(NSP_KV_SPLIT).append(nsp.get(temp)).append(";");
    		}
    	}
    	return ret.toString();
    }
    static String getOperationHeadName(XMLMakeup operation,XMLMakeup root,String partType){
    	XMLMakeup input = operation.getFirstChildrenEndWithName(partType);
    	if(null != input){
	    	XMLMakeup reqheader = input.getFirstChildrenEndWithName("header");
	    	if(null != reqheader){
	    		String partName = (String) reqheader.getProperties().get("part");
	    		String messageName = (String) reqheader.getProperties().get("message");
	    		messageName = messageName.contains(":")?messageName.split(":")[1]:messageName;
	    		XMLMakeup message = root.findFirstTextByTitleProperty("wsdl:message", "name", messageName);
	    		if(null != message){
	    			XMLMakeup part = message.findFirstTextByTitleProperty("wsdl:part", "name", partName);
	    			if(null != part){
	    				return part.getProperties().getProperty("element");
	    			}
	    		}
	    	}
    	}
    	return null;
    }
    //获取服务名和message中header部分的element的映射
    Map<String,String> getServiceHeaderMapping(){
    	Map<String,String> operaHeadMapping = new HashMap();
        if(null != binding){
        	List<XMLMakeup> operations = binding.get(0).getChildrenEndWithName("wsdl:operation");
        	for(XMLMakeup operation : operations){
        		operaHeadMapping.put(operation.getProperties().getProperty("name")+"_input", getOperationHeadName(operation,root,"input"));
        		operaHeadMapping.put(operation.getProperties().getProperty("name")+"_output", getOperationHeadName(operation,root,"output"));
        	}
        }
        return operaHeadMapping;
    }
    /**
     * 解析xs的WSDL
     * @param
     * @return
     */
    public Map<String,Map> parseXSWSDL(String content)throws Exception{
    	LinkedHashMap ret = new LinkedHashMap();
        /**收集wsdl元素 begin**/
        List<XMLMakeup> schemas = root.getFirstChildrenEndWithName("types").getChildrenEndWithName("schema");
        XMLMakeup portType = root.getFirstChildrenEndWithName("portType");
        if(null != schemas){
            Map<String,Map<String,XMLMakeup>> mt = new HashMap();
            //获取element、complex元素名和元素结构映射
            Map<String,XMLMakeup> paramStructures = getParamsStructures(schemas);
            //针对schema中没有参数类型,拼在了文中其他位置
            if(null == paramStructures || paramStructures.isEmpty()){
            	try{
	                return getOperationDesc();
            	}catch (Exception e) {
                    List<XMLMakeup> simpleTypes = root.getChildrenEndWithNameAndExistProperty("simpleType","name");
                    List<XMLMakeup> elements = root.getChildrenEndWithNameAndExistProperty("element","name");
                    List<XMLMakeup> complexTypes = root.getChildrenEndWithNameAndExistProperty("complexType","name");
                    for(XMLMakeup simpleType : simpleTypes){
                    	paramStructures.put(simpleType.getProperties().getProperty("name"), simpleType);
    	            }
    	            for(XMLMakeup element : elements){
    	            	paramStructures.put(element.getProperties().getProperty("name"), element.getFirstChildrenEndWithName("complexType"));
    	            }
    	            for(XMLMakeup complexType : complexTypes){
    	            	paramStructures.put(complexType.getProperties().getProperty("name"), complexType);
    	            }
				}
            }
            /**开始构造描述文件 begin**/
            if(null == mt || mt.size()<=0){
//            	Map messageMap = new HashMap();
//            	for(XMLMakeup message : messages){
//            			messageMap.put(message.getProperties().get("name"),message.getFirstChildrenEndWithName("part").getProperties().get(message.getName().startsWith("wsdl:")?"element":"type").toString());
//            	}
            	List<XMLMakeup> pts = portType.getChildren();
            	//获取服务入参出参结构
            	for(XMLMakeup p:pts){
            		if("wsdl:operation".endsWith((p.getName()))){
            			String name = p.getProperties().getProperty("name");
            			/**获取绑定在input和output上的body参数类型名称**/
            			String operationName = p.getProperties().getProperty("name");
            			XMLMakeup operationBinding = binding.get(0).findFirstTextByTitleProperty("wsdl:operation", "name", operationName);
            			String reqtypename = null;
            			if(null != p.getChildrenEndWithNameAndExistProperty("input", "type")){
	            			String opRequestMessage =removeNsp1(p.getFirstChildrenEndWithName("input").getProperties().getProperty("message"));
	            			XMLMakeup inputBody = operationBinding.findByEndWithNameAndEndWithParentName("body", "wsdl:input").get(0);
	            			String inputBodyParts = inputBody.getProperties().getProperty("parts");
	                    	XMLMakeup reqMessage = root.findFirstTextByTitleProperty("wsdl:message","name",opRequestMessage);
	                        try{
	                        	reqtypename = reqMessage.findFirstTextByTitleProperty("wsdl:part", "name", inputBodyParts).getProperties().getProperty("element");
	                        }catch (Exception e) {
	                        	reqtypename = reqMessage.getFirstChildrenEndWithName("part").getProperties().getProperty("element");
	            			}
            			}
            			String restypename = null;
            			if(null != p.getChildrenEndWithNameAndExistProperty("output", "type")){
            				String opResponseMessage =removeNsp1(p.getFirstChildrenEndWithName("output").getProperties().getProperty("message"));
                			XMLMakeup outputBody = operationBinding.findByEndWithNameAndEndWithParentName("body", "wsdl:output").get(0);
                			String outputBodyParts = outputBody.getProperties().getProperty("parts");
                			XMLMakeup resMessage = root.findFirstTextByTitleProperty("wsdl:message","name",opResponseMessage);
                			try{
                            	restypename = resMessage.findFirstTextByTitleProperty("wsdl:part", "name", outputBodyParts).getProperties().getProperty("element");
                            }catch (Exception e) {
                            	restypename = resMessage.getFirstChildrenEndWithName("part").getProperties().getProperty("element");
                			}
            			}
            			List<XMLMakeup> childs = p.getChildren();
            			//将引用对象的类型统一放在type属性
            			if(null != restypename || null != reqtypename){
	            			for(XMLMakeup child : childs){
	            				if("wsdl:input".endsWith(child.getName())){
	            					if(null != reqtypename)
	            						child.getProperties().put("type", reqtypename);
	            				}
	            				else if("wsdl:output".endsWith(child.getName())){
	            					if(null != restypename)
	            						child.getProperties().put("type", restypename);
	            				}
	            			}
            			}
            			//("wsdl:output")[0]
    					//map.put("ResponseBody", p.getFirstChildrenEndWithName("output"));
    					//("wsdl:input")[0]
            			//map.put("RequestBody", p.getFirstChildrenEndWithName("input"));
            			//mt.put(p.getProperties().getProperty("name"), map);
            			//获取TB描述文件
            			try{
            				ret.put(name,constructDescForXs(p,operationBinding,paramStructures,name));
            			}catch (Exception e) {
            				throw e;
						}
            		}
            	}
            }
//            Iterator<String> its = mt.keySet().iterator();
//            while(its.hasNext()){
//                String name = its.next();
//                Map<String,XMLMakeup> s = mt.get(name);
//                XMLMakeup responseBody = s.get("ResponseBody");
//                Map response = null;
//                //构造返回参数结构 
//                if(null == responseBody){
//                	if(null != (responseBody = paramStructures.get(name+"Response"))){
//                		response = convertWSDLElementType2USDLString(path,responseBody,paramStructures,getNspByTypeName(responseBody.getProperties().getProperty("type")));
//                	}
//                	else
//                		continue;
//                }
//                else{
//                	response = convertWSDLElementType2USDLString(path,paramStructures.get(removeNsp(responseBody.getProperties().getProperty("type"))),paramStructures,getNspByTypeName(responseBody.getProperties().getProperty("type")));
//                }
//                Set<String> serviceNsp = new HashSet();
//                Object o = response.get("return");
//                //带nsp前缀的返回参数结构
//                Map nsp_rm = null==o?response:(Map)o;
//                /**Desc参数:output**/
//                Map rm = removeNsp((Map)nsp_rm,serviceNsp);
//                /**Desc参数:nspOutputMapping**/
//                Map outputNspMapping = getinputNameNspMap((Map)nsp_rm,null);
//                
//                XMLMakeup requestBody = s.get("RequestBody");
//                String typeName = requestBody.getProperties().getProperty("type");
//                //带nsp前缀的入参结构
//                Map nsp_pm = new HashMap();
//                nsp_pm.put(typeName.replaceFirst(":", NSP_SPLIT), convertWSDLElementType2USDLString(path,paramStructures.get(removeNsp(typeName)),paramStructures,getNspByTypeName(typeName)));
//                /**Desc参数:input**/
//                Map pm = removeNsp(nsp_pm,serviceNsp);
//                String nsp = constructNspForService(serviceNsp, this.nsp);
//                /**Desc参数:nspInputMapping**/
//                Map inputNspMapping = getinputNameNspMap(nsp_pm,null);
//                
//                /**Desc参数:parNames**/
//                String[] parNames = getParamNamesFromWSDLElement(path,paramStructures.get(removeNsp(typeName)));
//                /**Desc参数:inputHeader**/
//                Map<String,Object> nsp_header_input = new HashMap();
//                //根据服务名称和参数类型获取binding中header的elementType
//                String inputHeaderType = operaHeadMapping.get(name+"_input");
//                if(null != inputHeaderType){
//                	nsp_header_input.put(inputHeaderType.replace(":", NSP_SPLIT), convertWSDLElementType2USDLString(path,paramStructures.get(removeNsp(inputHeaderType)),paramStructures,getNspByTypeName(inputHeaderType)));
//                	nsp_header_input = (Map)convertHeadType(nsp_header_input);
//                }
//                /**Desc参数:outputHeader**/
//                Map nsp_header_output = new HashMap();
//                String outputHeaderType = operaHeadMapping.get(name+"_output");
//                if(null != outputHeaderType){
//                	nsp_header_output.put(outputHeaderType.replace(":", NSP_SPLIT), convertWSDLElementType2USDLString(path,paramStructures.get(removeNsp(outputHeaderType)),paramStructures,getNspByTypeName(outputHeaderType)));
//                	nsp_header_output = (Map)convertHeadType(nsp_header_output);
//                }
//                /**开始构造描述文件 end**/
//                //获取TB描述文件
//                ret.put(name,getDesc(name,path,address,Arrays.asList(parNames),pm,rm,nsp,inputNspMapping,outputNspMapping,nsp_header_input,nsp_header_output,));
//            }
        }
        return ret;
    }
    Map<String,XMLMakeup> getParamsStructures(List<XMLMakeup> schemas){
    	Map<String,XMLMakeup> paramStructures = new HashMap();
        Map<String,String> elementMapping = new HashMap();
        for(int i=0;i<schemas.size();i++){
        	List<XMLMakeup> ms = schemas.get(i).getChildren();
            for(XMLMakeup m:ms){
            	if("xs:complexType".endsWith(m.getName())){
            		paramStructures.put(m.getProperties().getProperty("name"),m);
                }
            	else if("xs:element".endsWith(m.getName())){
            		//包含复杂对象的
            		if(CollectionUtils.isNotEmpty(m.getChildrenEndWithName("complexType"))){
            			paramStructures.put(m.getProperties().getProperty("name"), m.getFirstChildrenEndWithName("complexType"));
            		}
            		//element只包含一个type映射的
            		else{
            			elementMapping.put(m.getProperties().getProperty("name"), m.getProperties().getProperty("type"));
            		}
            	}
//            	else if(0 == i){
//	                if(StringUtils.isNotBlank(m.getProperties().getProperty("name")) && StringUtils.isNotBlank(m.getProperties().getProperty("type"))
//	                        && m.getProperties().getProperty("name").equals(m.getProperties().getProperty("type").replace("tns:", ""))){
//	                    if(m.getProperties().getProperty("name").contains("Response") && mt.containsKey(m.getProperties().getProperty("name").substring(0,m.getProperties().getProperty("name").length()-8))){
//	                        mt.get(m.getProperties().getProperty("name").substring(0,m.getProperties().getProperty("name").length()-8)).put("Response",m);
//	                    }else{
//	                        HashMap map = new HashMap();
//	                        map.put("Body",m);
//	                        mt.put(m.getProperties().getProperty("name"),map);
//	                    }
//	                }
//            	}
            }
        }
        //转换element映射关系
        if(!elementMapping.isEmpty()){
        	for(Entry<String, String> elementMapper : elementMapping.entrySet()){
        		if(null == paramStructures.get(elementMapper.getKey())){
        			paramStructures.put(elementMapper.getKey(), paramStructures.get(elementMapper.getValue().contains(":")?elementMapper.getValue().split(":")[1]:elementMapper.getValue()));
        		}
        	}
        }
        return paramStructures;
    }
    Map constructDescForXs(XMLMakeup operation,XMLMakeup operationBinding,Map<String, XMLMakeup> paramStructures,String name) throws Exception{
    	XMLMakeup responseBody = operation.getFirstChildrenEndWithName("output");
		XMLMakeup requestBody = operation.getFirstChildrenEndWithName("input");
		
		Map response = null;
        //构造返回参数结构 
        if(null == responseBody){
        	if(null != (responseBody = paramStructures.get(name+"Response"))){
        		response = convertWSDLElementType2USDLString(path,responseBody,paramStructures,getNspByTypeName(responseBody.getProperties().getProperty("type")));
        	}
        	else
        		throw new Exception("cannot get response from wsdl:"+name);
        }
        else{
        	response = convertWSDLElementType2USDLString(path,paramStructures.get(removeNsp1(responseBody.getProperties().getProperty("type"))),paramStructures,getNspByTypeName(responseBody.getProperties().getProperty("type")));
        }
        Set<String> serviceNsp = new HashSet();
        Object o = response.get("return");
        //带nsp前缀的返回参数结构
        Map nsp_rm = null==o?response:(LinkedHashMap)o;
        /**Desc参数:output**/
        Map rm = removeNsp((Map)nsp_rm,serviceNsp);
        /**Desc参数:nspOutputMapping**/
        Map outputNspMapping = getInputAndOutputNameNspMap((Map)nsp_rm,null);
        String typeName = requestBody.getProperties().getProperty("type");
        //构造带nsp前缀的入参结构
        Map nsp_pm = new LinkedHashMap();
        nsp_pm.put(typeName.replaceFirst(":", NSP_SPLIT), convertWSDLElementType2USDLString(path,paramStructures.get(removeNsp1(typeName)),paramStructures,getNspByTypeName(typeName)));
        /**Desc参数:input**/
        Map pm = removeNsp((Map)nsp_pm,serviceNsp);
        String nsp = constructNspForService(serviceNsp, this.nsp);
        /**Desc参数:nspInputMapping**/
        Map inputNspMapping = getInputAndOutputNameNspMap(nsp_pm,null);
        
        /**Desc参数:parNames**/
        String[] parNames = getParamNamesFromWSDLElement(path,paramStructures.get(removeNsp(typeName)));
        /**Desc参数:inputHeader**/
        Map<String,Object> nsp_header_input = new LinkedHashMap();
        //根据服务名称和参数类型获取binding中header的elementType
        String inputHeaderType = operaHeadMapping.get(name+"_input");
        if(null != inputHeaderType){
        	nsp_header_input.put(inputHeaderType.replace(":", NSP_SPLIT), convertWSDLElementType2USDLString(path,paramStructures.get(removeNsp(inputHeaderType)),paramStructures,getNspByTypeName(inputHeaderType)));
        	nsp_header_input = (Map)convertHeadType(nsp_header_input);
        }
        /**Desc参数:outputHeader**/
        Map nsp_header_output = new LinkedHashMap();
        String outputHeaderType = operaHeadMapping.get(name+"_output");
        if(null != outputHeaderType){
        	nsp_header_output.put(outputHeaderType.replace(":", NSP_SPLIT), convertWSDLElementType2USDLString(path,paramStructures.get(removeNsp(outputHeaderType)),paramStructures,getNspByTypeName(outputHeaderType)));
        	nsp_header_output = (Map)convertHeadType(nsp_header_output);
        }
        //获取soapAction
		List<XMLMakeup> soapActionXml = operationBinding.getChildrenEndWithNameAndExistProperty("operation", "soapAction");
        /**Desc参数:soapAction**/
        String soapAction = null;
        if(null !=soapActionXml && soapActionXml.size()>0){
        	soapAction = soapActionXml.get(0).getProperties().getProperty("soapAction");
        }
        return getDesc(name,path,address,Arrays.asList(parNames),pm,rm,nsp,inputNspMapping,outputNspMapping,nsp_header_input,nsp_header_output,soapAction);
    }
    public Map getheaderByServiceName(String serviceName) throws Exception{
    	Map header = new LinkedHashMap();
    	String headerType = operaHeadMapping.get(serviceName);
        if(null != headerType){
        	header.put(headerType.replace(":", NSP_SPLIT), parseType(getTypeXMLMakeup(headerType.substring(headerType.indexOf(':')+1)),getNspByTypeName(headerType)));
        	header = (Map)convertHeadType(header);
        }
        return header;
    } 
    //将header中参数类型去除键值如：type:java.lang.String(TB结构)转为java.lang.String
    Object convertHeadType(Map<String,Object> head){
    	Map ret = new LinkedHashMap();
    	for(Entry<String,Object> it : head.entrySet()){
    		if(Map.class.isAssignableFrom(it.getValue().getClass())){
    			ret.put(it.getKey(), convertHeadType((Map)it.getValue()));
    		}
    		else{
    			return it.getValue();
    		}
    	}
    	return ret;
    }
   
    /**
     * 根据WSDL文件转换成USDL的服务描述结构，一个operation一个服务， key为operation名称
     * @param filename
     * @return
     * @throws Exception
     */
    public static Map<String,Map> parseWSDL(String filename){
        List<FileInfo> fs = FileUtils.getAllProtocolFiles(filename, null, false);
        try{
	        if(null != fs && fs.size()>0) {
	            String content = FileUtils.getFileContentString(fs.get(0).getInputStream());
	            if (content.contains("xs:")) {
	            	WSDLParse wp = new WSDLParse(content);
	                return wp.parseXSWSDL(content);
	            } else {
	                WSDLParse wp = new WSDLParse(content);
	                return wp.getOperationDesc();
	            }
	        }
        }catch (Exception e) {
			log.error("please check is wsdlFile complete(including other imported file)");
			e.printStackTrace();
		}
        return null;
    }

    /**
     * json数据转换为WSDL String 用于调用外部的WSDL服务
     * @param json
     * @param srvInfo
     * @return
     * @throws Exception 
     */
    public static String convertMap2WSDLString(Map json,Map srvInfo) throws Exception{
        String nsp = (String)((Map)srvInfo.get("original")).get("namespace");
        String serviceName = String.valueOf(srvInfo.get("name"));
        String inputName = String.valueOf(((Map)srvInfo.get("input")).keySet().iterator().next());
        Map<String,Map> input = (Map)((Map)srvInfo.get("original")).get("nspInputMapping");
        Map<String,Object> header = (Map)((Map)srvInfo.get("original")).get("inputHeader");
        //Map<String,String> paramNameNspMap = getinputNameNspMap(input,null);
        try{
        	json = formatJson(json,input);
        }catch (Exception e) {
			log.error("format json to soap failed ,please check request params and the nspInputMapping in desc");
			e.printStackTrace();
			throw e;
		}
        //获取命名空间
        if(org.apache.commons.lang.StringUtils.isEmpty(nsp)){
        	if(null != nameSpaceMap.get(serviceName)){
        		nsp = nameSpaceMap.get(serviceName);
        	}
        	else{
		        String wsdlUrl = (String)((Map)srvInfo.get("original")).get("address");
		        nsp = getTargetNamespaceByUrl(wsdlUrl);
		        nameSpaceMap.put(serviceName, nsp);
        	}
        }
        //构造xml请求报文
        StringBuffer sb = new StringBuffer();
        //sb.append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:msg=\""+nsp+"\"><soapenv:Header/><soapenv:Body><msg:"+inputName+">");
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" ");
        //构造命名空间头
        if(null != nsp){
        	for(String one : nsp.split(";")){
        		if(one.length()>0){
	        		String[] sign_nsp = one.split(NSP_KV_SPLIT); 
	        		sb.append("xmlns:").append(sign_nsp[0]).append("=\"").append(sign_nsp[1]).append("\" ");
        		}
        	}
        }
        sb.append(">");
        //拼接header
        if(null != header && !header.isEmpty()){
        	sb.append("<soapenv:Header>");
        	sb.append(convertJsonMap2XMLNameSpaceString(convertJsonNSP_SPLIT(header),null));
        	sb.append("</soapenv:Header>");
        }
        else
        	sb.append("<soapenv:Header/>");
        //拼接body
        sb.append("<soapenv:Body>");
        //将json转化为xml(包括过滤空值)
        sb.append(convertJsonMap2XMLNameSpaceString(json, null));
        sb.append("</soapenv:Body></soapenv:Envelope>");
        //log.info("------"+serviceName+" requestXML-------/n"+sb.toString());
        return sb.toString();
    }
    //将tb中设置的header参数中命名空间的'_'转换为标准格式的':'
    static Map convertJsonNSP_SPLIT(Map<String,Object> json){
    	Map ret = new HashMap();
    	for(Entry<String,Object> entry : json.entrySet()){
    		if(Map.class.isAssignableFrom(entry.getValue().getClass())){
    			ret.put(entry.getKey().replaceFirst(NSP_SPLIT, ":"), convertJsonNSP_SPLIT((Map)entry.getValue()));
    		}
    		else{
    			ret.put(entry.getKey().replaceFirst(NSP_SPLIT, ":"), entry.getValue());
    		}
    	}
    	return ret;
    }   

    /**
     * 把wsdl返回的结构转换为json
     * convert xmlString to JSON map
     * @param xmlString
     * @return
     */
    public static Object convertWSDLXMLString2Map(String xmlString,Object type)throws Exception{
    	//XMLUtil.getMapIgnorePropertiesFromXMLString(xmlString);
        if(null == type){
            return "NO Desc Initialize value";
        }
    	
        XMLMakeup root = XMLUtil.getDataFromString(xmlString);
        if(null != root){
            XMLMakeup b = root.getFirstChildrenEndWithName(":Body");
            if(null != b){
                if(b.getChildren().size()>0){
                    XMLMakeup srvret = b.getChildren().get(0);
                    if(null != srvret){
                        //if(type.getClass().isArray() || Collection.class.isAssignableFrom(type.getClass())) {
                    	//复杂对象返回值
                    	if(List.class.isAssignableFrom(type.getClass())
                    			|| Map.class.isAssignableFrom(type.getClass())){
                    		
                    		List<XMLMakeup> sr = new ArrayList<XMLMakeup>();
                    		sr.add(srvret);
                    		Object ret = convertXMLString2Map(null, srvret.getText(), sr, type);
//                    		Object retFirstName = null;
                    		String srvretName = removeNsp(srvret.getName());
                    		if(null != ret 
                    				&& Map.class.isAssignableFrom(ret.getClass())
                    				&& ((Map)type).containsKey(srvretName)){
                    			ret = ((Map)ret).get(srvretName);
                    		}
                            return ret;
                    	}
                    	//针对返回值非复杂对象情况
                    	else if(null == srvret.getText()
                    				&& null != srvret.getChildren()){
                    		return convertXMLString2Map(null, srvret.getChildren().get(0).getText(), srvret.getChildren(), type);
                    	}
                        /*}else {
                            return convertXMLString2Map(srvret.getChildren().get(0).getName(), srvret.getChildren().get(0).getText(), srvret.getChildren().get(0).getChildren(), type);
                        }*/
                    }
                }
            }
        }
        //返回错误报文或无返回值
        return null;
    }

    public static String getTargetNamespaceByUrl(String url)throws Exception{
        //XMLUtil.getMapIgnorePropertiesFromXMLString(xmlString);
    	String targetNamespace = null;
    	InputStream stream = getUrlConn(url);
        XMLMakeup root = XMLUtil.getDataFromStream(stream);
        if(null != root){
            targetNamespace = root.getProperties().getProperty("targetNamespace");
        }
        return targetNamespace;
    }
    /**
     * convert wsdl xml parameter to USDL , maybe privimate type , List,Map
     * @param os
     * @param descType
     * @return
     */
    public static Object convertXMLString2Map(String name,String text,List<XMLMakeup> os,Object descType){
        if(null != descType) {
            Object ret =null;
            if (List.class.isAssignableFrom(descType.getClass())) {
                List li = new LinkedList();
                //防止descType为数组，但os参数为空情况
                if((null == os || os.size() <=0)
                		&& org.apache.commons.lang.StringUtils.isNotEmpty(text)
                			&& org.apache.commons.lang.StringUtils.isNotEmpty(name))
                {
                	li.add(ClassUtils.chgValue(String.class, text));
                	ret = li;
                }
                else{
                	/**update by ligs 2017-09-30 修改ProvisioningDomainV1.0Op接口返回值UsometTransactionError为数组时取不到值问题，原因是转换时参数结构和报文结构不一致**/
//	                for(XMLMakeup x:os){
//	                    li.add(convertXMLString2Map(x.getName(),x.getText(), x.getChildren(), ((List) descType).get(0)));
//	                }
                    li.add(convertXMLString2Map(name,text, os, ((List) descType).get(0)));
	                ret = li;
                }
            } else if (Map.class.isAssignableFrom(descType.getClass())) {
                Map map = new LinkedHashMap();
                //List<XMLMakeup> ss = os.get(0).getChildren();
                for(XMLMakeup s:os){
                	String paramName = removeNameSpaceForParam(s.getName());
                	Object descObj = ((Map)descType).get(paramName);
                	Object s_ret = convertXMLString2Map(paramName,s.getText(),s.getChildren(),(descObj == null && s.getText() == null)?(Map)descType:descObj);
                	if (null != descObj
                			&& List.class.isAssignableFrom(descObj.getClass())
                			&& null != map.get(paramName)) {
	                	((LinkedList)map.get(paramName)).addAll((Collection) s_ret);
                	}
                	else
                		map.put(paramName,s_ret);
                }
                ret = map;
            } else {
            	ret = ClassUtils.chgValue(descType.getClass(), text);
            }
            return ret;
        }
        return null;
    }
    private static String removeNameSpaceForParam(String name){
    	return name.contains(":")?name.substring(name.indexOf(':')+1):name;
    }
    public static String convertJsonMap2XMLString(Map jsonMap,String nsp) throws Exception{
        return convertJsonMap2XMLNameSpaceString(jsonMap,null);
    }
    public static String convertJsonMap2XMLNameSpaceString(Map jsonMap,String nsp) throws Exception{
        if(null != jsonMap){
        	if(null == pattern)
        		pattern = Pattern.compile(regEx);
            StringBuffer sb = new StringBuffer();
            Iterator its = jsonMap.keySet().iterator();
            String sp = nsp==null?"":nsp+":";
            while(its.hasNext()){
                String k = (String)its.next();
                Object o = jsonMap.get(k);
                //过滤空值(1.字符串)
                if(null == o 
                		|| (String.class.isAssignableFrom(o.getClass()) && StringUtils.isEmpty((String)o))){
                	continue;
                }
                else if(List.class.isAssignableFrom(o.getClass())){
                    sb.append(convertJsonList2XMLNameSpaceString(k,(List)o,nsp));
                }else if(Map.class.isAssignableFrom(o.getClass())){
                    sb.append("<").append(sp).append(k).append(">").append(convertJsonMap2XMLNameSpaceString((Map) o,nsp)).append("</").append(sp).append(k).append(">");
                }else if(o.toString().trim().startsWith("<")&&!o.toString().trim().startsWith("<![CDATA[")){
                	sb.append("<").append(sp).append(k).append(">").append("<![CDATA[").append(o.toString().replaceAll("\n\t", "")).append("]]>").append("</").append(sp).append(k).append(">");
                }
                else if(Date.class.isAssignableFrom(o.getClass())){
                	SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                    sb.append("<").append(sp).append(k).append(">").append(format.format((Date)o)).append("</").append(sp).append(k).append(">");
                }
                else if(String.class.isAssignableFrom(o.getClass())
                		&& pattern.matcher((String)o).matches()){
                	//TODO 时间格式转换待完善
                	String value = (String)o;
                	//value = value.contains(":")?value:value.trim()+" 00:00:00";
                	//Timestamp timeStamp = Timestamp.valueOf(value);
            		value = value.trim();
            		int length = value.split(":").length;
                	if(length<2){
                		if(value.contains(" "))
                			value = value+":00:00";
                		else
                			value = value+" 00:00:00";
                	}
                	else if(length == 2){
                		value = value+":00";
//                		format = new SimpleDateFormat("yyyy-MM-dd");
//                		format1 = new SimpleDateFormat("yyyy-MM-dd'Z'");
                	}
                	//SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                	Date date = format.get().parse(value);
                    sb.append("<").append(sp).append(k).append(">").append(format1.get().format(date)).append("</").append(sp).append(k).append(">");
                }
                else {
                    sb.append("<").append(sp).append(k).append(">").append(o.toString()).append("</").append(sp).append(k).append(">");
                }
            }
            return sb.toString();
        }
        return "";
    }

    public static String convertJsonList2XMLString(String name,List o) throws Exception{
        return convertJsonList2XMLNameSpaceString(name,o,null);
    }
    public static String convertJsonList2XMLNameSpaceString(String name,List o,String nsp) throws Exception{
        if(null != o){
            StringBuffer sb = new StringBuffer();
            String sp = nsp==null?"":nsp+":";
            for(Object oo:o){
                if(null== oo)continue;
                if(List.class.isAssignableFrom(oo.getClass())){
                    sb.append(convertJsonList2XMLNameSpaceString(name,(List)oo,nsp));
                }else if(Map.class.isAssignableFrom(oo.getClass())){
                    sb.append("<").append(sp).append(name).append(">").append(convertJsonMap2XMLNameSpaceString((Map)oo,nsp)).append("</").append(sp).append(name).append(">");
                }else{
                    sb.append("<").append(sp).append(name).append(">").append(oo.toString()).append("</").append(sp).append(name).append(">");
                }
            }
            return sb.toString();
        }
        return "";
    }

    public static void main(String[] args) throws Exception{
    	Float descType = new Float(0l);
    	String text = null;
    	if ((descType.getClass().isAssignableFrom(Float.class)) || ((descType.getClass().isAssignableFrom(float.class)) && (text instanceof String))) {
    		Float.valueOf(Float.parseFloat((String)text));
    	}
    	Map header = new HashMap();
    	Map test = new HashMap();
    	test.put("ns2_username", "java.lang.String");
    	test.put("ns2_passwd", "java.lang.String");
    	test.put("ns2_channel", "java.lang.String");
    	header.put("ns2_authorHeader", test);
    	log.debug(convertJsonMap2XMLNameSpaceString(convertJsonNSP_SPLIT(header),null));
        try{
        	for(Entry ret : WSDLParse.parseWSDL("file:///C:/Users/Peter/Desktop/BillingDomainV1.0_build.wsdl").entrySet()){
        		System.out.println(ret);
        	}
        }catch (Exception e){
            e.printStackTrace();
        }
    }


	Object parseComplexType(XMLMakeup x,String parentNsp)throws Exception{
        String name = x.getProperties().getProperty("name");
        List<XMLMakeup> ls = x.getChildren();
        LinkedHashMap  ps = new LinkedHashMap();
        //x是element型数据
        for(XMLMakeup l:ls){
        	if(l.getName().indexOf("simpleType")>0){
        		String type = getElementType(x);
                String simpleType = getSimpleTypeBase(l);
                boolean isList = isList(x);
                Object obj = chgType2Normal(null==simpleType?type:simpleType,isList,parentNsp);
                //参数带命名空间 ligs,1.有命名空间时取文中命名空间加参数名作为tb参数名2.没有时直接取文中参数名
                //ps.put(type==null?name:type.indexOf(':')<0?name:type.substring(0,type.indexOf(':'))+NSP_SPLIT+name,obj);
                return obj;
        	}
        	//复杂对象中可能包含的标签
        	else if(l.getName().indexOf("complexContent")>=0){
        		String thisType = getElementType(l.getFirstChildrenEndWithName("extension"));
        		//XMLMakeup complex = root.findFirstTextByTitleProperty("complexType", "name", removeNsp(thisType));
        		XMLMakeup complex = getTypeXMLMakeup(removeNsp(thisType));
        		String thisNsp = getNspByTypeName(thisType);
        		Map ret = new LinkedHashMap();
        		Object obj = parseComplexType(complex,thisNsp);
        		ret.putAll((Map)obj);
        		if(null != l.getFirstChildrenEndWithName("extension").getChildren()) {
        			Object obj2 = parseComplexType(l.getFirstChildrenEndWithName("extension"),thisNsp);
        			ret.putAll((Map)obj2);
        		}
	        		return ret;
        		//参数带命名空间 ligs,1.有命名空间时取文中命名空间加参数名作为tb参数名2.没有时直接取文中参数名
                //ps.put(thisNsp==null?(null == parentNsp?name:parentNsp+NSP_SPLIT+name):thisNsp+NSP_SPLIT+name,obj);
        	}
        	else if(l.getName().indexOf("sequence")>=0
            		||l.getChildrenEndWithName("element").size()>0){
                List<XMLMakeup> el = l.getChildren();
                if(null != el){
                    for(XMLMakeup e:el){
                        if(e.getName().indexOf("element")>=0){
                            String pname = e.getProperties().getProperty("name");
                            String thisType = getElementType(e);
                            String thisNsp = getNspByTypeName(thisType);
                            String simpleType = null;
                            boolean isList = isList(e);
                            List<XMLMakeup> st = e.getChildren();
                            boolean isc=false;
                            Object obj = null;
                            if(st.size()>0){
                            	//element中的元素可能包含一个复杂对象或者一个简单对象或者没有,最多只有一个，过滤掉无用信息
	                            for(XMLMakeup s:st){
	                                if(s.getName().indexOf("simpleType")>=0){
	                                	simpleType = getSimpleTypeBase(s);
	                                }else if(s.getName().indexOf("complexType")>=0){
	                                    obj = parseComplexType(s,thisNsp);
	                                    isc = true;
	                                }
	                            }
	                            //处理简单对象类型和没有对象的element
	                            if(!isc){
	                            	//简单类型直接转换
	                            	if(null!=simpleType)
	                            		obj = chgType2Normal(simpleType,isList,thisNsp);
	                            	//element类型的先从文中取出完整内容再进行转换
	                            	else{
	                            		XMLMakeup element = getElementContent(removeNsp(thisType));
	                            		if(null != element)
	                            			obj = isList?Arrays.asList(parseType(element,thisNsp)):parseType(element,thisNsp);
	                            		//element的type直接是基本类型
	                            		else
	                            			obj = chgType2Normal(thisType,isList,thisNsp);
	                            	}
                                }
                                //参数带命名空间 ligs,1.有命名空间时取文中命名空间加参数名作为tb参数名2.没有时直接取文中参数名
                                ps.put(parentNsp==null?pname:parentNsp+NSP_SPLIT+pname,obj);
                            }
                            else{
                                obj = chgType2Normal(null==simpleType?thisType:simpleType,isList,thisNsp);
                                ps.put(null!=pname?(null != parentNsp?parentNsp+NSP_SPLIT+pname:pname):(null != parentNsp?parentNsp+NSP_SPLIT+removeNsp(thisType):removeNsp(thisType)), obj);
                            }
                        }
                    }
                }
            }
        	
        	else if(l.getName().indexOf("choice")>=0){
        		boolean isList = isList(x);
        		return chgType2Normal(null,isList,parentNsp);
        	}
        }
        return ps;
    }
	XMLMakeup getElementContent(String elementName){
		XMLMakeup m = null;
        m = root.findFirstTextByTitleProperty("simpleType", "name", elementName);
        m = null == m?root.findFirstTextByTitleProperty("complexType", "name", elementName):m;
        m = null == m?getTypeXMLMakeup(elementName):m;
        return m;
	}
	static boolean isList(XMLMakeup e){
		String occurs = e.getProperties().getProperty("maxOccurs");
        boolean isList=false;
        if("unbounded".equals(occurs) ||((StringUtils.isNotBlank(occurs) && Integer.parseInt(occurs)>1))){
            isList=true;
        }
        return isList;
	}
	static String getSimpleTypeBase(XMLMakeup simpleType){
		String simpleTypeBase = null;
		List<XMLMakeup> sl = simpleType.getChildren();
        for(XMLMakeup m:sl){
            if(m.getName().indexOf("restriction")>=0){
            	simpleTypeBase = m.getProperties().getProperty("base");
            	break;
            }
        }
        return simpleTypeBase;
	}
    Object parseSimpleType(XMLMakeup x)throws Exception{
        String name = x.getProperties().getProperty("name");
        String type=null;
        type = getSimpleTypeBase(x);
        return chgType2Normal(type,false,null);
    }
    Object parseType(XMLMakeup x,String parentNsp)throws Exception{
    	String typeName = x.getName().substring(x.getName().indexOf(":")+1);
        if("simpleType".equals(typeName)){
            return parseSimpleType(x);
        }else if("complexType".equals(typeName)){
            return parseComplexType(x,parentNsp);
        }else if("element".equals(typeName)){
        	return parseComplexType(x.getChildrenEndWithName("complexType").size()>0?x.getFirstChildrenEndWithName("complexType"):x.getFirstChildrenEndWithName("element"),parentNsp);
        }else if("annotation".equals(typeName)){
        	return null;
        }
        throw new Exception("not support parseType:"+x.getName());
    }
    Object chgType2Normal(String s,boolean isList,String parentNsp)throws Exception{
        Object t=null;
        s = removeNsp1(s);
        if(null == s){
        	 t = new HashMap();
             ((Map)t).put("@type","java.lang.String");
        }
        if(s.contentEquals("string")||s.indexOf("NMTOKEN")>=0){
            t = new HashMap();
            ((Map)t).put("@type","java.lang.String");
        }else if(s.contentEquals("int")){
            t = new HashMap();
            ((Map)t).put("@type","java.lang.Integer");
        }else if(s.contentEquals("dateTime")||s.contentEquals("date")){
            t = new HashMap();
            ((Map)t).put("@type","java.util.Date");
        }else if(s.contentEquals("long")){
            t = new HashMap();
            ((Map)t).put("@type","java.lang.Long");
        }else if(s.contentEquals("short")){
            t = new HashMap();
            ((Map)t).put("@type","java.lang.Short");
        }else if(s.contentEquals("float")){
            t = new HashMap();
            ((Map)t).put("@type","java.lang.Float");
        }else if(s.contentEquals("double")){
            t = new HashMap();
            ((Map)t).put("@type","java.lang.Double");
        }
        else if(s.contentEquals("boolean")){
            t = new HashMap();
            ((Map)t).put("@type","java.lang.Boolean");
        }
        else{
            //可能是简单类型也可能是复杂类型的element
            XMLMakeup m = getElementContent(s);
            String thisNsp = getNspByTypeName(s);
            thisNsp = null == thisNsp?parentNsp:thisNsp;
            if(null != m) {
                t = parseType(m,thisNsp);
            }else{
                log.debug("not find type by name:"+s);
            }
        }
        if(!isList){
            return t;
        }else{
            return Arrays.asList(t);
        }
    }

    XMLMakeup getTypeXMLMakeup(String name){
        for(XMLMakeup type : types){
        	//由endwith改为直接匹配防止取到后缀为name但前缀不同的参数
            if(removeNsp(type.getProperties().getProperty("name")).equals(name)){
                return type;
            }
        }
        return null;
    }
    void getServiceUrl(){
    	List<XMLMakeup> ma = root.getChildrenEndWithName("service");
        if(null != ma && ma.size()>0){
            className = ma.get(0).getProperties().getProperty("name");
            XMLMakeup ad = ma.get(0).findFirstByTitle("soap:address");
            ad = null != ad ? ad : ma.get(0).findFirstByTitle("wsdlsoap:address");
            if(null != ad){
                address=ad.getProperties().getProperty("location");
            }
        }
    }

    Map getDesc(String name,String pk,String wsdlAddress,List<String> parNames,Object input,Object output,String nsp,Object nsp_input,Object nsp_output,Object nsp_header_input,Object nsp_header_output,String soapAction){
        Map m = new LinkedHashMap();
        m.put("name",name);
        m.put("package",pk);
        Map original = new LinkedHashMap();
        original.put("address",wsdlAddress);
        original.put("namespace",nsp);
        original.put("parNames",parNames);
        original.put("nspInputMapping", nsp_input);
        original.put("nspOutputMapping", nsp_output);
        original.put("inputHeader", nsp_header_input);
        original.put("outputHeader", nsp_header_output);
        original.put("soapAction", soapAction);
        m.put("original",original);
        m.put("input",input);
        m.put("output",output);
        return m;
    }
    
    LinkedHashMap convertWSDLElementType2USDLString(String path,XMLMakeup x,Map<String,XMLMakeup> types,String nsp) throws Exception{
    	if("xs:simpleType".endsWith(x.getName())){
    		for(XMLMakeup p: x.getChildren()){
    			if(null != getElementType(p)) {
    				return convertSimpleType(p);
    			}
    		}
    		throwException("There is no propertie "+ArrayUtils.toString(elementSign)+" in xs:simpleType");
    	}else if("xs:complexType".endsWith(x.getName())) {
            //LinkedHashMap map = new LinkedHashMap();
        	//String name = (String) x.getProperties().get("name");
        	//String t = getElementType(x);
            //参数中有ref
            //name = null == name?t:name;
            //String parentNsp = getNspByTypeName(t);
        	//map.put(formatInputName(t,true,nsp),parseComplexType(x,nsp));
        	return (LinkedHashMap) parseComplexType(x,nsp);
    	}else {
	    	List<XMLMakeup> xs = x.getChildrenEndWithName("sequence");
	        if(null != xs) {
	        	LinkedHashMap nspMappingMap = new LinkedHashMap();
	            LinkedHashMap map = new LinkedHashMap();
	            for (XMLMakeup m : xs) {
	                List<XMLMakeup> ps = m.getChildrenEndWithName("element");
	                if(null != ps && ps.size()>0){
	                    for(XMLMakeup p:ps){
	                        String t = getElementType(p);
	                        if(t == null)continue;
	                        String name = (String) p.getProperties().get("name");
	                        //参数中有ref
	                        name = null == name?t:name;
	                        boolean islist = isList(p);
	                        //基本数据类型处理
	                        if(t.startsWith("xs:")||t.startsWith("xsd:")){
	                        	Object val = convertBasicType(islist,t);
	                        	//input参数过滤命名空间
	                            map.put(formatInputName(name,true,nsp),val);
	                        }else{
	                        	String parentNsp = getNspByTypeName(t);
	                            XMLMakeup subt = types.get(t.substring(t.indexOf(":")+1));
	                            if(null != subt) {
	                            	LinkedHashMap sm = convertWSDLElementType2USDLString(path, subt, types,parentNsp);
	                                if(!islist){
	                                	//添加命名空间
	                                    map.put(formatInputName(name,true,nsp),sm);
	                                }else{
	                                    List list = new ArrayList();
	                                    list.add(sm);
	                                    //添加命名空间
	                                    map.put(formatInputName(name,true,nsp),list);
	                                }
	                            }
	                            //在element元素下直接挂对象类型的
	                            else if (null != p.getFirstChildrenEndWithName("complexType")) {
	                            	XMLMakeup complexType = p.getFirstChildrenEndWithName("complexType");
	                            	map.put(formatInputName(t,true,nsp),parseComplexType(complexType,parentNsp));
	//                            	if(null != complexType.getFirstChildrenEndWithName("complexContent")) {
	//                            		XMLMakeup contentType = complexType.getFirstChildrenEndWithName("complexContent");
	//                            	}
	                            }
	                            //有的基本类型element没有对应的simpleType,自动构造类型为String
	                            else{
	                            	Object val = convertBasicType(islist,"xs:auto");
	                            	//添加命名空间
	                            	map.put(formatInputName(t,true,nsp), val);
	                            }
	                        }
	                    }
	                }
	            }
	            return map;
	        }
    	}
        return null;
    }
    private static String formatInputName(String target,boolean isNeedNsp,String nsp){
    	boolean isContainNsp = target.indexOf(':')>0;
    	if(isNeedNsp)
    		return isContainNsp?nsp+NSP_SPLIT+target.split(":")[1]:nsp+NSP_SPLIT+target;
    	else
    		return isContainNsp?target.substring(target.indexOf(':')+1):target;
    }
    static String[] getParamNamesFromWSDLElement(String pack,XMLMakeup x){
        List<XMLMakeup> xs = x.getChildrenEndWithName("sequence");
        if(null != xs) {
            List<String> li = new ArrayList<String>();
            for (XMLMakeup m : xs) {
            	 List<XMLMakeup> ps = m.getChildrenEndWithName("element");
                if(null != ps && ps.size()>0){
                    for(XMLMakeup p:ps){
                    	for(String sign : elementSign){
                    		String name = p.getProperties().getProperty(sign);
                    		if(null != name){
                    			li.add(removeNsp(name));
                    			break;
                    		}
                    	}
                    }
                }
            }
            return li.toArray(new String[0]);
        }
        return null;
    }

    private static String removeNsp1(String name){
    	int index = name.indexOf(":");
    	return index > 0?name.substring(index+1):name;
    }
    private static String removeNsp(String name){
    	String newName = null;
    	int index = name.indexOf(":");
    	newName = index > 0?name.substring(index+1):name;
    	int index2 = newName.indexOf(NSP_SPLIT);
    	return index2 > 0?newName.substring(index2+NSP_SPLIT.length()):newName;
    }
    private static String removeNsp(String name,Set<String> serviceNsp){
    	int index = name.indexOf(":");
    	if(index > 0){
    		serviceNsp.add(name.substring(0,index));
    		return name.substring(index+1);
    	}
    	return name;
    }
    //移除参数中的命名空间枚举
    private static Map  removeNsp(Map nsp_pm,Set<String> serviceNsp){
    	//构造不含命名空间map
    	Map  pm = new LinkedHashMap();
    	if(null != nsp_pm) {
	    	Iterator<Entry> it = nsp_pm.entrySet().iterator();
	    	for(;it.hasNext();){
	    		Entry entry = it.next();
	    		//参数名
	    		String key = (String)entry.getKey();
	    		Object val = entry.getValue();
	    		//检索分隔符位置
	    		int index = key.indexOf(NSP_SPLIT);
	    		//去除命名空间,并记录nsp
	    		String name;
	    		if(index>0){
	    			name = key.substring(index+NSP_SPLIT.length());
	    			serviceNsp.add(key.substring(0,index));
	    		}
	    		else
	    			name = key;
	    		if(Map.class.isAssignableFrom(val.getClass())){
	    			pm.put(name, removeNsp((Map)val,serviceNsp));
	    		}
	    		else if(List.class.isAssignableFrom(val.getClass())){
	    			Map  listVal = (Map) ((List)val).get(0);
	    			pm.put(name, Arrays.asList(removeNsp(listVal,serviceNsp)));
	    		}
	    		else
		    		pm.put(name, val);
	    	}
	    	return pm;
	    	}
    	return null;
    }
    /****************----------xs-------------*/

    static String cvNL2Package(String s){
        List<String> r = StringUtils.getTagsNoMark(s,"/","/");
        if(null != r && r.size()>0){
            for(String m:r){
                String ret = StringUtils.bottomUpPath(m);
                if(StringUtils.isNotBlank(ret)){
                    return ret;
                }
            }
        }
        return "";
    }
    static String chgWSDLType2Normal(String s){
        if("string".equals(s)){
            return "java.lang.String";
        }
        else if("short".equals(s)){
            return "java.lang.Short";
        }
        else if("int".equals(s)){
            return "java.lang.Integer";
        }
        else if("float".equals(s)) {
        	return "java.lang.Float";
        }
        else if("double".equals(s)) {
        	return "java.lang.Double";
        }
        else if("long".equals(s)) {
        	return "java.lang.Long";
        }
        else if("dateTime".equals(s)||"date".equals(s)) {
        	return "java.util.Date";
        }
        else if("boolean".equals(s)) {
        	return "java.lang.Boolean";
        }	
        else if("auto".equals(s)){
        	return "java.lang.String";
        }
        return s;
    }
    
    private static Map formatJson(Map json,Map<String,Map> format)throws Exception{
        try {
            Map newJson = new LinkedHashMap();
            Iterator<Entry> it = json.entrySet().iterator();
            while (it.hasNext()) {
                Entry<String, Object> entry = (Entry) it.next();
                Map formatValue = format.get(entry.getKey());
                String ownner_Nsp = (String) formatValue.get(TB_NSP);
                try {
                    if (null != entry.getValue()) {
                        if (Map.class.isAssignableFrom(entry.getValue().getClass())) {
                            newJson.put(null == ownner_Nsp ? entry.getKey() : ownner_Nsp + ":" + entry.getKey(), formatJson((Map) entry.getValue(), formatValue));
                        } else if (List.class.isAssignableFrom(entry.getValue().getClass())) {
                            List newList = new ArrayList();
                            List values = (List) entry.getValue();
                            for (Object listVO : values) {
                            	if(null != listVO) {
	                            	if(Map.class.isAssignableFrom(listVO.getClass())) {
	                            		newList.add(formatJson((Map) listVO, formatValue));
	                            	}else if(String.class.isAssignableFrom(listVO.getClass())) {
	                            		newList.add((String)listVO);
	                            	}else {
	                            		newList.add(listVO);
	                            	}
                            	}
                            }
                            newJson.put(null == ownner_Nsp ? entry.getKey() : ownner_Nsp + ":" + entry.getKey(), newList);
                            continue;
                        } else
                            newJson.put(null == ownner_Nsp ? entry.getKey() : ownner_Nsp + ":" + entry.getKey(), entry.getValue());
                    }
                } catch (Exception e) {
                    continue;
                }
            }
            return newJson;
        }catch(Exception e){
            throw new Exception("wsdl formatJson error\n"+json,e);
        }
    }
    private static InputStream getUrlConn(String turl) throws Exception{
    	URL url = new URL(turl+"?wsdl");  
        // 得到URLConnection对象  
        URLConnection connection = url.openConnection();
        InputStream is = connection.getInputStream(); 
        return is;
    }
    static String getElementType(XMLMakeup element){
    	String t = null;
    	for(String sign : elementSign){
    		if(null != (t = element.getProperties().getProperty(sign))){
				break;
			}
    	}
    	return t;
    }
    //转换simpleType类型的数据
    private static LinkedHashMap convertSimpleType(XMLMakeup p){
    	String t = getElementType(p);
		boolean islist = false;
        if(StringUtils.isNotBlank(p.getProperties().getProperty("maxOccurs"))){
            islist = true;
        }
		if(t.startsWith("xs:")||t.startsWith("xsd:")){
			return convertBasicType(t);
        }
		return null;
    }
    //转换基本类型数据
    private static Object convertBasicType(boolean isList,String type){
    	LinkedHashMap pm = new LinkedHashMap();
        if(!isList){
            pm.put("@type",chgWSDLType2Normal(type.substring(3)));
            return pm;
        }else{
            pm.put("@type",chgWSDLType2Normal(type.substring(3)));
            List list = new ArrayList();
            list.add(pm);
            return list;
        }
    }
    private static LinkedHashMap convertBasicType(String type){
    	LinkedHashMap pm = new LinkedHashMap();
    	pm.put("@type",chgWSDLType2Normal(type.substring(3)));
    	return pm;
    }
    private static void throwException(String e) throws Exception{
    	log.error(e);
		throw new Exception(e);
    }
    //获取参数名与nsp的映射map
    private static Map getInputAndOutputNameNspMap(Map input,String parentNsp){
    	Map nameNspMap = new LinkedHashMap();
    	Iterator it = input.entrySet().iterator();  
    	for(;it.hasNext();){
    		Map tempMap = new LinkedHashMap();
    		Entry<String,Object> entry = (Entry) it.next();
    		try{
	    		if(null!=entry.getValue()){
	    			if(Map.class.isAssignableFrom(entry.getValue().getClass())){
	    				tempMap.putAll(getInputAndOutputNameNspMap((Map)entry.getValue(),entry.getKey().split(NSP_SPLIT).length>1?entry.getKey().split(NSP_SPLIT)[0]:parentNsp));
	    				//nameNspMap.putAll(getinputNameNspMap((Map)entry.getValue(),entry.getKey().split("_").length>1?entry.getKey().split("_")[0]:parentNsp));
	    			}
	    			else if(List.class.isAssignableFrom(entry.getValue().getClass())){
	    				for(Object listVO : (List)entry.getValue()){
	    					if(Map.class.isAssignableFrom(listVO.getClass())){
	    						tempMap.putAll(getInputAndOutputNameNspMap((Map)listVO,entry.getKey().split(NSP_SPLIT).length>1?entry.getKey().split(NSP_SPLIT)[0]:parentNsp));
	    						//nameNspMap.putAll(getinputNameNspMap((Map)listVO,entry.getKey().split("_").length>1?entry.getKey().split("_")[0]:parentNsp));
	    	    			}
	    				}
	    			}
	    			else
	    				continue;
	    		}
    		}catch (Exception e) {
				continue;
			}
    		
    		if(entry.getKey().split(NSP_SPLIT).length>1)
    			tempMap.put(TB_NSP, entry.getKey().split(NSP_SPLIT)[0]);
    			//nameNspMap.put(entry.getKey().split("_")[1], entry.getKey().split("_")[0]);
    		else{
    			tempMap.put(TB_NSP, parentNsp);
    			//nameNspMap.put(entry.getKey(), parentNsp);
    		}
    		//
    		nameNspMap.put(removeNsp(entry.getKey()), tempMap);
    	}
    	return nameNspMap;
    }
    /**
     * 
     * @Title: getNsp 
     * @Description: 获取命名空间
     * @return  返回样例tns1:nsp1;tns2:nsp2;tns3:nsp3;
     * @return: String
     */
    private void getNsp(){
    	XMLMakeup body = root.getFirstChildrenEndWithName("soapenv:Body");
        //nsp = root.getChildrenEndWithNameAndExistProperty("schema", "targetNamespace").get(0).getProperties().getProperty("targetNamespace");
        Set<String> signs =  new HashSet<String>();
        //先从body取
//        if(null != body){
//            String name = body.getChildren().get(0).getName();
//            if(name.indexOf(":")>0){
//            	signs.add(name.substring(0,name.indexOf(":")));
//           }
//        }
        //从body取不到再通过全文取type
        if(CollectionUtils.isEmpty(signs)){
        	signs.addAll(getNspByType("type"));
        	signs.addAll(getNspByType("element"));
        	signs.addAll(getNspByType("message"));
        }
        //根据xmlns:sign样式获取命名空间
        StringBuilder nspSb = new StringBuilder();
        for(String sign : signs){
        	if(!CollectionUtils.isEmpty(root.getChildrenEndWithNameAndExistProperty("", "xmlns:"+sign))){
        		nsp.put(sign, root.getChildrenEndWithNameAndExistProperty("", "xmlns:"+sign).get(0).getProperties().getProperty("xmlns:"+sign));
        		//nspSb.append(sign).append(NSP_KV_SPLIT).append(root.getChildrenEndWithNameAndExistProperty("", "xmlns:"+sign).get(0).getProperties().getProperty("xmlns:"+sign)).append(";");
        	}
        }
        //return nspSb.toString();
    }
    //按照元素的标签类型取
    Set<String> getNspByType(String type){
    	Set<String> ret = new HashSet();
    	for(XMLMakeup xml : root.getChildrenEndWithNameAndExistProperty("", type)){
        	if(xml.getProperties().getProperty(type).contains(":"))
        		ret.add(xml.getProperties().getProperty(type).split(":")[0]);
        }
    	return ret;
    }
    String getNspByTypeName(String TypeName){
    	return null == TypeName?null:(TypeName.contains(":")?TypeName.substring(0,TypeName.indexOf(":")):null);
    }

    /**
     * 获取参数webservice参数，以string返回 ， 例如:@WebParam(partName = "in", name = "ProvisioningRequest", targetNamespace = "http://www.usomet.com/cdm/service/message/v1.0/MsgProvisioningRequests")
     * @param classType 参数类型
     * @param parName  参数名称
     * @param desc  desc结构
     * @return
     */
    public static String getAnnotationWebParam(String classType,String parName,Map desc){
        return null;
    }

    /**
     * 获取方法的webservice参数，返回字符串，例如：@WebService(targetNamespace="")
     * @param desc
     * @return
     */
    public static String getAnnotationWebServiceParameter(Map desc){
        return null;
    }

    /**
     * 获取方法返回参数的webservice描述，例如 @WebResult(name = "", targetNamespace = "nspInputMapping中的命名空间tb_nsp", partName = "out")
     * @param methodName  方法名称
     * @param desc
     * @return
     */
    public static String getAnnotationWebResult(String methodName,Map desc){
        return null;
    }


    public static Map getNameSpaces(Map des){
        String s = (String)ObjectUtils.getValueByPath(des, "original.namespace");
        if(null != s){
            Map m = new HashMap();
            String[] ns = s.split(";");
            if(null != ns) {
                for (String n:ns) {
                    String[] ni = n.split("::");
                    if(null != ni && ni.length==2){
                        m.put(ni[0],ni[1]);
                    }

                }
            }
            if(m.size()>0){
                return m;
            }
            return null;
        }
        return null;
    }
}
