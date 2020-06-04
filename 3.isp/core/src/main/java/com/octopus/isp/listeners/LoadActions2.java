package com.octopus.isp.listeners;/**
 * Created by admin on 2020/5/28.
 */

import com.octopus.isp.bridge.impl.Bridge;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.XMLUtil;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import com.octopus.utils.xml.desc.Desc;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * @ClassName LoadServices
 * @Description ToDo
 * @Author Kod Wong
 * @Date 2020/5/28 11:35
 * @Version 1.0
 **/
public class LoadActions2 extends XMLDoObject {
    transient Log log = LogFactory.getLog(LoadActions.class);
    Map<String,IBodyCreator> bodyCreators= new HashMap();
    String optype;
    List<String> services;
    public LoadActions2(XMLMakeup xml, XMLObject parent, Object[] containers) throws Exception {
        super(xml, parent,containers);
        XMLMakeup[] ms = xml.getChild("bodycreater");
        optype = xml.getProperties().getProperty("optype");
        if(null != ms){
            for(XMLMakeup x:ms){
                if(null != x){
                    String k = x.getProperties().getProperty("key");
                    if(StringUtils.isNotBlank(k)){
                        String c = x.getText();
                        if(StringUtils.isNotBlank(c)){
                            bodyCreators.put(k,(IBodyCreator)Class.forName(c).newInstance());
                        }
                    }
                }
            }
        }


    }

    /**
     * zk service first load, than load local , local service can not recover zk service .
     * @param srs
     * @throws Exception
     */
    public void init(Map<String,Map<String,XMLMakeup>> srs,Map descs,List<String> localfirstServiceList,boolean isinit,List<String> exclude)throws Exception{
        log.info("----------------begin load actions----------------");

        if(null != srs){
            Iterator<Map<String,XMLMakeup>> it = srs.values().iterator();
            while(it.hasNext()){
                try {
                    Iterator<XMLMakeup> ss = it.next().values().iterator();
                    while(ss.hasNext()) {
                        XMLMakeup x = ss.next();
                        if(null != exclude && null !=x.getId() && ArrayUtils.isInStringArray(exclude,x.getId())){
                            continue;
                        }
                        if (null == services) services = new ArrayList<String>();
                        Map desc = null;
                        if (null != descs) {
                            desc = (Map) descs.get(x.getId());
                        }

                        XMLObject o = addService(x, desc);
                        if (null != o && !services.contains(o.getXML().getId())) {
                            services.add(o.getXML().getId());
                        } else {
                            log.error("load service is null or disable:" + x);
                        }

                    }


                }catch (Exception e){
                    log.error("convert service String to XMLMakeup error", e);
                }

            }

        }

        log.info("----------------end load actions----------------");

    }

    String getSelfInstanceId(){
        Bridge b = (Bridge)getObjectById("bridge");
        return b.getInstanceId();
    }


    String getname(String root,String optype,String pk,String name){
        String r=root;
        if(StringUtils.isNotBlank(optype)){
            r+="/"+optype;
        }
        boolean is=false;
        if(StringUtils.isNotBlank( pk)){
            r+="/"+pk;
            is=true;
        }
        if(is){
            r += "." + name;
        }else {
            r +=  name;
        }
        return r;
    }

    boolean checkDesc(Map desc)throws Exception{
        if(desc.containsKey("opType") && null!= desc.get("opType") && ((String)desc.get("opType")).startsWith("INS-")){
            throw new Exception("the service desc opType is not must start with [INS-]");
        }
        return true;
    }
    void addServiceByDesc(Map desc,boolean isactive)throws Exception{
        try {
            checkDesc(desc);
            createXMLObjectByDesc(Desc.removeNotServiceProperty(desc), null, this, isactive, getSingleContainers());

        }catch (Exception e){
            log.error("load service error "+(null == desc?"name is null":desc.get("name")),e);
        }
    }
    //add service to container
    XMLObject addService(XMLMakeup serviceBody,Map desc)throws Exception{
        XMLObject o  = createXMLObject(serviceBody,null,this,getSingleContainers());
        if(null != o) {
            if (null != desc) {
                addInvokeDescStructure(desc);
            }
            log.info("has loaded service " + serviceBody.getId());
            return o;
        }else {
            return null;
        }
    }

    void remoteService(String srvName)throws Exception{
        if(StringUtils.isNotBlank(srvName)) {
            if(removeXMLObjectById(srvName))
                log.info("has remoted service: "+srvName);
        }
    }
    void updateService(XMLMakeup serviceBody)throws Exception{
        if(null !=serviceBody){
            remoteService(serviceBody.getId());
            addService(serviceBody,null);

            System.out.println("has updated service: "+serviceBody.getId());
        }

    }
    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(null != input && input.size()>0){
            String op = (String)input.get("op");
            if(op.equals("add")){
                String m = (String)input.get("data");
                try {
                    addService(XMLUtil.getDataFromString(m),null);
                }catch (Exception e){
                    log.error("when add service , convert string to XMLMakeup error",e);
                }
            }else if(op.equals("delete")){
                String srv = (String)input.get("data");
                try {
                    remoteService(srv);
                }catch (Exception e){
                    log.error("remove service["+srv+"] error",e);
                }
            }else if(op.equals("update")){
                String m = (String)input.get("data");
                try {
                    updateService(XMLUtil.getDataFromString(m));
                }catch (Exception e){
                    log.error("when update service , convert string to XMLMakeup error",e);
                }
            }else if("init".equalsIgnoreCase(op) && null != input.get("services")){
                if(null !=input.get("services") && input.get("services") instanceof Map ) {
                    //log.info("add services"+input.get("services"));
                    List<String> first=null;
                    if(null != input.get("localfirst") && input.get("localfirst") instanceof List){
                        first=(List)input.get("localfirst");
                    }
                    boolean isinit=false;
                    if(null != input.get("isDoInit") && StringUtils.isTrue((String)input.get("isDoInit"))){
                        isinit=true;
                    }
                    init((Map) input.get("services"),(Map) input.get("descs"),first,isinit,(List)input.get("exclude"));
                }
            }else if("reConZkInit".equals(op)){
                log.info("reload Services form zk by reconnect zk ");
            }
        }
        return null;
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;
    }

    @Override
    public ResultCheck checkReturn(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return new ResultCheck(true,null);
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