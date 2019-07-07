package com.octopus.tools.utils;

import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cls.ClassUtils;
import com.octopus.utils.cls.POJOUtil;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import com.octopus.utils.xml.desc.Desc;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * User: wfgao_000
 * Date: 16-1-11
 * Time: 下午2:12
 */
public class XmlObjProxy extends XMLDoObject {
    transient static Log log = LogFactory.getLog(XmlObjProxy.class);

    public XmlObjProxy(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(log.isDebugEnabled()){
            log.debug("-----------------------------------------");
            log.debug(input);
            //log.debug("-----------------------------------------");
            //log.debug(getXML());
            //log.debug("-----------------------------------------");
            //log.debug(config);
            //log.debug("-----------------------------------------");
        }
        if((null != env.get("${targetNames}")||(null !=input && StringUtils.isNotBlank(input.get("outsvid")))) && null != config){
            try {
                //refer outside api
                String parameterType = (String)config.get("parameterType");
                if(StringUtils.isNotBlank(parameterType)) {// not null , is outside invoke
                    String srvid=null;
                    if(null != input){
                        srvid=(String)input.get("outsvid");
                    }
                    if(StringUtils.isBlank(srvid)) {
                        Object ns = env.get("${targetNames}");
                        if (null != ns && ns.getClass().isArray()) {
                            String[] srvnames = (String[]) env.get("${targetNames}");
                            if (null != srvnames && srvnames.length == 1) {
                                srvid = srvnames[0];
                            }
                        } else if (ns instanceof List) {
                            if ((((List) ns)).size() > 0) {
                                srvid = (String) ((List) ns).get(0);
                            }
                        }
                    }
                    if (StringUtils.isNotBlank(srvid)) {
                        XMLObject obj = getObjectById(srvid);
                        log.debug("used obj:"+srvid);
                        Map stru = obj.getDescStructure();
                        if (null != stru) {
                            Map m = (Map) stru.get("original");
                            if (null != m) {
                                    String invokerClazz = (String) m.get("invoker");
                                    List<String> parnames = (List) m.get("parNames");
                                    String originalClazz = obj.getXML().getProperties().getProperty("package");

                                    String originalMethod = obj.getXML().getId();
                                    Object[] pars = null;
                                    Class[] c_pars = null;
                                    if (StringUtils.isNotBlank(parameterType) && "pojo".equals(parameterType)) {
                                        Method method = ClassUtils.getMethodByName(Class.forName(originalClazz), originalMethod);
                                        if(null == method){
                                            throw new Exception("not find outter method ["+originalClazz+"#originalMethod");
                                        }
                                        String[] ps = null;
                                        c_pars = method.getParameterTypes();
                                        if (null != parnames) {
                                            ps = (String[]) parnames.toArray(new String[0]);
                                            pars = POJOUtil.convertMapData2POJOs(input, method.getParameterTypes(), method.getGenericParameterTypes(), ps);
                                        }
                                    } else {
                                        pars = new Object[]{input};
                                        c_pars = new Class[]{Map.class};
                                    }
                                    if(log.isInfoEnabled()) {
                                        log.info("\ninvokerClazz: " + invokerClazz + "\n" + "method: " + originalMethod + "\npars: " + ArrayUtils.toJoinString(c_pars) + "\ndata: " + ArrayUtils.toJoinString(pars));
                                    }
                                    long l = System.currentTimeMillis();
                                    Map session = null;
                                    if(null != env) {
                                        session = (Map) env.getParameter("${session}");
                                        if(log.isInfoEnabled()){
                                            log.info("session:\n"+session);
                                        }
                                    }
                                        //log.error("-----"+originalMethod);
                                    Object ret = ClassUtils.invokeStaticMethod(Class.forName(invokerClazz), "invoke", new Class[]{Map.class, String.class, String.class, Class[].class, Object[].class}, new Object[]{session, originalClazz, originalMethod, c_pars, pars});
                                    if (log.isInfoEnabled()) {
                                        if(null != ret) {
                                            Object o = ClassUtils.getFieldValue(ret,"receiptOrderInformations",false);
                                            if(null != o) {
                                                if(o.getClass().isArray()) {
                                                    Object ov = ClassUtils.getFieldValue(((Object[])o)[0],"eRechargeTrxNo",false);
                                                    log.error("return eRechargeTrxNo:" + ov);
                                                }
                                            }
                                            log.info(originalMethod + " costTime:" + (System.currentTimeMillis() - l) + "\n" + ArrayUtils.toJoinString(new Object[]{POJOUtil.convertPOJO2USDL(ret, new AtomicLong(0))}));
                                        }
                                    }
                                    //convert return bean to usdl
                                    ret = POJOUtil.convertPOJO2USDL(ret, null);
                                    return ret;

                            }
                        }
                    }
                }
                }catch(Exception e){
                log.error("",e);
                throw e;
            }
        }
        //通过op属性来指定具体的java方法
        if(null != input && input.size()>0 && null != config){
            List data=(List)input.get("pars");
            String op = (String)input.get("op");
            if(StringUtils.isNotBlank(op)) {
                if (config.containsKey(op)) {
                    List d = (List) ((Map) config.get(op)).get("data");
                    if ((data == null || data.size() == 0) && (null != d && d.size() > 0)) {
                        data = new LinkedList();
                        for (int i = 0; i < d.size(); i++) {
                            if (null != d.get(i) && d.get(i) instanceof String)
                                data.add(env.getValueFromExpress((String) d.get(i), this));
                            else
                                data.add(d.get(i));
                        }
                    } else if ((null != d && d.size() > 0) && null != data && data.size() == d.size()) {
                        List li = new LinkedList();
                        for (int i = 0; i < data.size(); i++) {
                            if (null == data.get(i) || !StringUtils.isNotBlank(data.get(i))) {
                                if (null != d.get(i) && d.get(i) instanceof String)
                                    li.add(env.getValueFromExpress((String) d.get(i), this));
                                else
                                    li.add(d.get(i));
                            } else {
                                if (null != data.get(i) && data.get(i) instanceof String)
                                    li.add(env.getValueFromExpress((String) data.get(i), this));
                                else
                                    li.add(data.get(i));
                            }
                        }
                        data = li;
                    }


                    Map m = (Map) config.get(op);
                    String c = (String) m.get("clazz");
                    String n = (String) m.get("method");
                    List pars = (List) m.get("pars");
                    Object o = Class.forName(c).newInstance();
                    Class[] cs = null;
                    if (null != pars) {
                        cs = ClassUtils.getClasses((String[]) pars.toArray(new String[0]));
                    }
                    try {
                        Object r = o.getClass().getMethod(n, ((pars != null && pars.size() > 0) ? cs : null)).invoke(o, convertParams(cs, data));
                        return r;
                    }catch (Exception e){
                        throw new Exception("invoke "+c+"#"+n+" error\n"+m.get("pars"),e);
                    }
                } else {
                    throw new Exception("not load [" + op + "] service");
                }
            }
        }else if(StringUtils.isNotBlank(xmlid)){

            Map m = getDescStructure(xmlid);
            if(null != m) {
                String protocol = (String) m.get("protocol");
                if ("bean".equals(protocol)) {
                    String clazz = (String) m.get("clazz");
                    if (StringUtils.isNotBlank(clazz)) {
                        //这个类为具体java方法的代理xmlObject对象
                        //从环境参数中根据调用报文转换为外围java方法的输入参数
                        Object pars = Desc.getBeanInputParameters(env.getReadOnlyParameter(), input);
                        String method = (String) m.get("method");
                        Class[] cs = Desc.getBeanInputParametersType(m);
                        Class c = ClassUtils.getClass(null, clazz);
                        Object o = c.newInstance();
                        Object ret = c.getMethod(method, cs).invoke(o, pars);
                        if (null != ret) {
                            return Desc.convertBeanReturn(m, ret);
                        }
                    }
                } else if ("ws".equals(protocol)) {

                } else if ("http".equals(protocol)) {

                } else if ("restful".equals(protocol)) {

                }
            }

        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
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
    Object[] convertParams(Class[] type,List<String> value)throws Exception{

            if (null != type) {
                Object[] ret = new Object[type.length];
                for (int i = 0; i < type.length; i++) {
                    if (null != value.get(i))
                        ret[i] = ClassUtils.chgValue(null, type[i], value.get(i));
                    else
                        ret[i] = null;
                }
                return ret;
            } else {
                return null;
            }


    }
}
