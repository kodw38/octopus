package com.octopus.isp.actions;

import com.octopus.tools.dataclient.v2.DataClient2;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.XMLUtil;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import com.octopus.utils.xml.desc.Desc;

import java.util.*;

/**
 * User: wfgao_000
 * Date: 15-8-28
 * Time: 上午11:52
 */
public class ISPDictionary extends XMLDoObject{
    public static String SERVICE_CLASS_BUSINESS="BUS";
    public static String SERVICE_CLASS_QUERY="QRY";
    public static String SERVICE_CLASS_CHECK="CHCK";
    public static String SERVICE_CLASS_LOGIN="LGN";
    public static String SERVICE_CLASS_LOGOUT="LGT";
    // pojoname,proname,protype
    Map<String,Map<String,String>> pojoMap;
    //catalog ,name,data
    Map<String,Map<String,XMLMakeup>> services = new HashMap<String, Map<String, XMLMakeup>>();
    Map<String,Map> descs = new HashMap();
    Map<String,String> serviceNameCatalog = new HashMap<String, String>();
    public ISPDictionary(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        List<XMLMakeup> ls = xml.getChildren();
        if(null !=ls){
            for(XMLMakeup x:ls){
                if("services".equals(x.getId())){
                    List slist= new ArrayList();
                    XMLMakeup[] ss = x.getChild("property") ;
                    if(null  !=ss){
                        for(XMLMakeup s:ss){
                            slist.add(s.getProperties());
                        }
                    }
                    loadServices(slist);
                }
            }
        }
    }
    public String getOpType(String serviceName){
        String catalog = serviceNameCatalog.get(serviceName);
        if(StringUtils.isNotBlank(catalog)){
            return (String)services.get(catalog).get(serviceName).getProperties().getProperty("opType");
        }
        return null;
    }
    void loadFields(List<Map<String,Object>> ds){
        //todo
    }
    void loadTables(List<Map<String,Object>> ds){
        //todo
    }

    void loadPojos(List<Map<String,Object>> ds){
        //get pojo
        pojoMap = new HashMap<String, Map<String, String>>();
        if(null != ds){
            String type,proName,proType;
            for(Map<String,Object> r:ds){
                type = (String)r.get("TYPE_NAME");
                proName = (String)r.get("PROPERTY_NAME");
                proType = (String)r.get("PROPERTY_TYPE");
                if(!pojoMap.containsKey(type))pojoMap.put(type,new HashMap<String, String>());
                if(!pojoMap.get(type).containsKey(proName))
                    pojoMap.get(type).put(proName,proType);
            }
        }
        /*// set complex by pro type
        Iterator<String> ik = pojoMap.keySet().iterator();
        while(ik.hasNext()){
            String pojoname = ik.next();
            Map<String,String> ps = pojoMap.get(pojoname);
            Iterator<String> k = ps.keySet().iterator();
            int complex=0;
            while(k.hasNext()){
                String v = ps.get(k.next());
                if(StringUtils.isNotBlank(v)){
                    int n= getComplex(v,pojoMap);
                    if(n>complex)complex=n;
                }
            }
            ps.put("complex",String.valueOf(complex));
        }
        //sort map by value
        Map<String, Map<String,String>> sortedMap = new LinkedHashMap<String, Map<String,String>>();
        if (pojoMap != null && !pojoMap.isEmpty()) {
            List<Map.Entry<String, Map<String,String>>> entryList = new ArrayList<Map.Entry<String, Map<String,String>>>(pojoMap.entrySet());
            Collections.sort(entryList,
                            new Comparator<Map.Entry<String, Map<String,String>>>() {
                                public int compare(Map.Entry<String, Map<String,String>> entry1,
                                        Map.Entry<String, Map<String,String>> entry2) {
                                    int value1 = 0, value2 = 0;
                                    try {
                                        value1 = Integer.valueOf(entry1.getValue().get("complex"));
                                        value2 = Integer.valueOf(entry2.getValue().get("complex"));
                                    } catch (NumberFormatException e) {
                                        value1 = 0;
                                       value2 = 0;
                                    }
                                    return value2 - value1;
                                }
                            });
            Iterator<Map.Entry<String, Map<String,String>>> iter = entryList.iterator();
            Map.Entry<String, Map<String,String>> tmpEntry = null;
            while (iter.hasNext()) {
                tmpEntry = iter.next();
               sortedMap.put(tmpEntry.getKey(), tmpEntry.getValue());
            }
        }
        pojoMap=sortedMap;
        */
    }
    void loadServices(List<Map<String,Object>> ds){
         if(null != ds){
             String catalog,name,optype,busiName,desc;
             for(Map<String,Object> m:ds){
                 catalog=(String)m.get("CATALOG");
                 name=(String)m.get("NAME");
                 String body = (String)m.get("BODY");
                 optype = (String)m.get("OP_CLASS");
                 busiName = (String)m.get("BUSI_NAME");
                 desc = (String)m.get("desc");
                 try {
                     XMLMakeup srv = XMLUtil.getDataFromString(body);
                     if(!srv.isEnable()) continue;
                     if(StringUtils.isNotBlank(name))
                     srv.getProperties().setProperty("key",name);
                     if(StringUtils.isNotBlank(catalog))
                     srv.getProperties().setProperty("package",catalog);
                     if(StringUtils.isNotBlank(optype))
                     srv.getProperties().setProperty("opType",optype);
                     if(StringUtils.isNotBlank(busiName))
                     srv.getProperties().setProperty("busiName",busiName);
                     if(StringUtils.isNotBlank(desc))
                     srv.getProperties().setProperty("desc",desc);

                     if(!serviceNameCatalog.containsKey(name)) serviceNameCatalog.put(name,catalog);
                     if(!services.containsKey(catalog)) services.put(catalog,new HashMap<String, XMLMakeup>());
                     if(!services.get(catalog).containsKey(name))services.get(catalog).put(name,srv);
                 }catch (Exception e){
                     log.error("load service ["+name+"] error",e);
                 }
             }
         }
    }
    //match pojo by pro names . the par may be less the class pro names
    public Class getClassByFields(Set<String> fieldNames) throws Exception {
        Iterator<String> its = pojoMap.keySet().iterator();
        while(its.hasNext()){
            String type =its.next();
            Map<String,String> po = pojoMap.get(type);
            if(po.keySet().containsAll(fieldNames)){
                return Class.forName(type);
            }
        }
        return null;
    }
    //pojoname,proname,protype
    public Map<String,Map<String,String>> getPojoList(){
        return pojoMap;
    }
    // catalog(wsname),name,[0] parameter,[1]return,[2] desc ,[3] op_class ,[4]business
    /*public Map<String,Map<String,String[]>> getWSServiceList(){
        Map<String,Map<String,String[]>> ret= new HashMap<String, Map<String, String[]>>();
        Iterator<String> its = serviceMap.keySet().iterator();
        while(its.hasNext()){
            String catalog = its.next();
            if(!ret.containsKey(catalog)) ret.put(catalog,new HashMap<String, String[]>());
            Map<String,Map<String,Object>> s = serviceMap.get(catalog);
            Iterator<String> ss = s.keySet().iterator();
            while(ss.hasNext()){
                String name = ss.next();
                if(!ret.get(catalog).containsKey(name)){
                    String[] ps=new String[5];
                    ps[0]=(String)s.get(name).get("PARAMETER_TYPE");
                    ps[1]=(String)s.get(name).get("RETURN_TYPE");
                    ps[2]=(String)s.get(name).get("REMARK");
                    ps[3]=(String)s.get(name).get("OP_CLASS");
                    ps[4]=(String)s.get(name).get("BUSI_CLASS");
                    ret.get(catalog).put(name,ps);
                }
            }
        }
        return ret;
    }*/

    public Map<String,Map<String,XMLMakeup>> getServices(){
        return services;
    }
    @Override
    public Object doSomeThing(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(null != input && StringUtils.isNotBlank(input.get("op"))){
            //增加调用报文服务
            if("addService".equalsIgnoreCase((String)input.get("op"))){
                if(null != input.get("data")){
                    XMLMakeup m=null;
                    if(input.get("data") instanceof XMLMakeup)
                        m = (XMLMakeup)input.get("data");
                    else if(input.get("data") instanceof String )
                        m = XMLUtil.getDataFromString((String)input.get("data"));

                    if(null != m) {
                        Map id=null;
                        if(null != input.get("desc")){
                            Map d = StringUtils.convert2MapJSONObject((String)input.get("desc"));
                            id = Desc.getInvokeDescStructure(d);

                        }
                        if(log.isDebugEnabled())
                            log.debug("add server:"+m.getId());

                        addService(m,id);
                    }
                }else if(null != input.get("desc")){
                    Map desc = (Map)input.get("desc");
                    XMLMakeup body = Desc.getInvokeStructure(desc);
                    if(null != desc && null != body){
                        addService(body,desc);
                    }
                }
            }
            return null;
        }
        return null;
    }

    //will overwrite pre object,将覆盖之前加入的相同对象
    void addService(XMLMakeup s,Map desc){
        String path = s.getProperties().getProperty("package");
        String name = s.getProperties().getProperty("key");

        if(StringUtils.isNotBlank(path)&& StringUtils.isNotBlank(name)){
            if(!services.containsKey(path))services.put(path,new HashMap());
            services.get(path).put(name,s);
            if(null != desc)
                descs.put(name,desc);
        }else{
            log.error("load service ["+s.getId()+"], package is null, please set package");
        }
    }

    void init()throws Exception{
        List<Map<String, Object>> fields = null, tables = null, pojos = null, servers = null;
        XMLMakeup c = (XMLMakeup) ArrayUtils.getFirst(getXML().getChild("dataquerys"));
        if (null != c) {
            DataClient2 dc = (DataClient2) getObjectById("DataClient");
            XMLMakeup[] ms = c.getChild("dataquery");
            if (null != ms) {
                for (XMLMakeup m : ms) {
                    if(null != m.getProperties().getProperty("input")) {
                        String k = m.getId();
                        if ("fields".equals(k)) {
                            fields = (List<Map<String, Object>>) dc.doSomeThing(null, null, StringUtils.convert2MapJSONObject(m.getProperties().getProperty("input")), null, null);
                        }
                        if ("tables".equals(k)) {
                            tables = (List<Map<String, Object>>) dc.doSomeThing(null, null, StringUtils.convert2MapJSONObject(m.getProperties().getProperty("input")), null, null);
                        }
                        if ("pojos".equals(k)) {
                            pojos = (List<Map<String, Object>>) dc.doSomeThing(null, null, StringUtils.convert2MapJSONObject(m.getProperties().getProperty("input")), null, null);
                        }
                        if ("services".equals(k)) {
                            servers = (List<Map<String, Object>>) dc.doSomeThing(null, null, StringUtils.convert2MapJSONObject(m.getProperties().getProperty("input")), null, null);
                        }
                    }
                }
            }
        }
        XMLMakeup[] xs = getXML().getChild("cfg");
        if (null != xs && xs.length > 0) {
            for (XMLMakeup x : xs) {
                List li = new ArrayList();
                XMLMakeup[] ps = x.getChild("property");
                for (XMLMakeup p : ps) {
                    li.add(p.getProperties());
                }
                if ("fields".equals(x.getId())) {
                    if (null != fields)
                        fields.addAll(li);
                    else
                        fields = li;
                }
                if ("tables".equals(x.getId())) {
                    if (null != tables)
                        tables.addAll(li);
                    else
                        tables = li;
                }
                if ("pojos".equals(x.getId())) {
                    if (null != pojos)
                        pojos.addAll(li);
                    else
                        pojos = li;
                }
                if ("services".equals(x.getId())) {
                    if (null != servers)
                        servers.addAll(li);
                    else
                        servers = li;
                    for (Object o : li) {
                        Map m = (Map) o;
                        m.put("BODY", StringUtils.toXMLRetainChar((String) m.get("BODY")));
                    }
                }
            }
        }

        //addTableto pojo
        /*if (null != input) {
            Object o = input.get("extendpojo");
            if(null != o && o instanceof Collection) {
                List extendpojo = (List)o;
                if (null != extendpojo) {
                    pojos.addAll(extendpojo);
                }
            }
        }*/

        if (null != fields)
            loadFields(fields);
        if (null != tables)
            loadTables(tables);
        if (null != pojos)
            loadPojos(pojos);
        if (null != servers)
            loadServices(servers);

    }

    @Override
    public void doInitial() throws Exception {
        init();
        /*Map m = new HashMap();
        m.put("services",services);
        m.put("descs",descs);
        return m;*/
    }

    @Override
    public boolean checkInput(String xmlid,XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResultCheck checkReturn(String xmlid,XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
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
