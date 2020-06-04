package com.octopus.isp.bridge.launchers.impl.wsext;

import com.octopus.isp.bridge.launchers.impl.CXFWebServiceLauncher;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import com.octopus.utils.xml.desc.Desc;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Administrator on 2019/1/21.
 */
public class GeneratorWSClass extends XMLDoObject {
    transient static Log log = LogFactory.getLog(GeneratorWSClass.class);
    boolean isThrowException;
    boolean isdevelop = true;
    String compilepath = null;
    static Map<String,Class> cache = new ConcurrentHashMap();

    public GeneratorWSClass(XMLMakeup xml, XMLObject parent, Object[] containers) throws Exception {
        super(xml, parent, containers);
    }

    @Override
    public void doInitial() throws Exception {
        String s = getXML().getProperties().getProperty("config");
        if(StringUtils.isNotBlank(s)){
            initGenerator((Map) getEmptyParameter().getMapValueFromParameter(StringUtils.convert2MapJSONObject(s), null));
        }
    }

    Map<String,Class> initGenerator(Map config){
        try {
            if(null != config) {
                Map<String,Class> ret = new ConcurrentHashMap();
                List<String> list = (List) config.get("generatorpackage");
                List<XMLDoObject> sers = getDeployServices(list);
                String develop = (String) config.get("isdevelop");
                isThrowException = StringUtils.isTrue((String) config.get("isthrowexception"));
                if (StringUtils.isNotBlank(develop)) {
                    isdevelop = StringUtils.isTrue(develop);
                }
                if (StringUtils.isBlank(compilepath)) {
                    compilepath = (String) config.get("compilepath");
                }
                if (null != sers && sers.size() > 0) {
                    ArrayList ls = new ArrayList();
                    for(XMLDoObject ser:sers){
                        generator(ser,ret);
                    }
                    System.out.println("generator class count:"+ret.size());
                }
                return ret;
            }
        }catch (Exception e){
            log.error("",e);
        }
        return null;

    }

    void generator(XMLDoObject ser,Map<String,Class> ret){
        try {
            String pack = ser.getXML().getProperties().getProperty("package");
            if (ser.getXML().getId().startsWith("("))
                return ;
            if (StringUtils.isNotBlank(pack) && pack.lastIndexOf(".") > 0) {
                pack = pack.substring(0, pack.lastIndexOf("."));
            }
            Class serClass = null;
            if (isdevelop) {
                serClass = Desc.getServiceWrapClassByDesc(ser.getXML().getId(), ser.getInvokeDescStructure(), compilepath, CXFWebServiceLauncher.class.getName(), "invoke", isThrowException);
            } else {
                if (null != pack) {
                    try {
                        serClass = Class.forName(pack + "." + "proxy_" + ser.getXML().getId());
                    } catch (Exception e) {
                        log.error("not find class [" + pack + "." + "proxy_" + ser.getXML().getId() + "]", e);
                    }
                } else {
                    try {
                        serClass = Class.forName("proxy_" + ser.getXML().getId());
                    } catch (Exception e) {
                        log.error("not find class [" + "proxy_" + ser.getXML().getId() + "]", e);
                    }
                }
            }
            if(null !=serClass) {
                ret.put(ser.getXML().getId(), serClass);
                cache.put(ser.getXML().getId(), serClass);
            }
        }catch (NoClassDefFoundError e){
            log.error("generator class by desc["+ser.getXML().getId()+"] error",e);
        }catch (Exception e){
            log.error("generator class by desc["+ser.getXML().getId()+"] error",e);
        }
    }

    //filter loaded xmldoobject by path
    List<XMLDoObject> getDeployServices(List<String> name) {
        Map<String, XMLObject> all = getXMLObjectContainer();
        Iterator<String> its = all.keySet().iterator();
        List<XMLDoObject> ret = new ArrayList();
        while (its.hasNext()) {
            String m = its.next();
            try {
                if (all.get(m) instanceof XMLDoObject) {
                    Map stru = ((XMLDoObject) all.get(m)).getInvokeDescStructure();
                    if (null != stru && StringUtils.isNotBlank(stru.get("package"))) {
                        String path = (String) stru.get("package");
                        if(null != name && name.size()>0) {
                            for (String n : name) {
                                if (path.startsWith(n))
                                    ret.add((XMLDoObject) all.get(m));
                            }
                        }else{
                            ret.add((XMLDoObject) all.get(m));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(null != input) {
            String op = (String) input.get("op");
            if ("generatorClassByDesc".equals(op)) {
                Map desc = input;
                Class serClass = Desc.getServiceWrapClassByDesc((String) desc.get("name"), Desc.getInvokeDescStructure(desc), compilepath, CXFWebServiceLauncher.class.getName(), "invoke", isThrowException);
                if(null !=serClass)
                    cache.put((String) desc.get("name"),serClass);
                return serClass;
            }else if("getGeneratorClasses".equals(op)){
                return cache;
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
        return false;
    }
}
