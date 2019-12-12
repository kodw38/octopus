package com.octopus.tools.mbeans;

import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.si.jvm.JVMUtil;
import com.octopus.utils.si.jvm.MBeanManager;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import com.octopus.utils.xml.auto.pointparses.PointParseGetErrorTrace;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.ObjectName;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Created by admin on 2019/12/9.
 */
public class MBeanClient extends XMLDoObject {
    XMLDoObject system;
    public MBeanClient(XMLMakeup xml, XMLObject parent, Object[] containers) throws Exception {
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
        try {
            if (null != input) {
                if (null != config) {
                    List<String> hs = new ArrayList();
                    if (null != system) {
                        Map m = new HashMap();
                        m.put("op", "getInsList");
                        Object ins = system.doSomeThing(null, null, m, null, null);
                        if (null != ins && ins instanceof List) {
                            if (null != ins && ((List) ins).size() > 0) {
                                for (int i = 0; i < ((List) ins).size(); i++) {
                                    if (null != ((List) ins).get(i) && ((List) ins).get(i) instanceof Map) {
                                        Map t = ((Map) ((List) ins).get(i));
                                        //log.error("mbean remote :"+t);
                                        if (StringUtils.isNotBlank(t.get("jmxRemotePort")) && StringUtils.isNotBlank(t.get("ip"))) {
                                            hs.add(t.get("ip").toString().trim() + ":" + t.get("jmxRemotePort").toString().trim());
                                        }
                                    }
                                }
                                List<String> ns = (List) input.get("names");
                                List<String> as = (List) input.get("attributes");

                                if (null == ns || ns.size() == 0) {
                                    ns = (List) config.get("names");
                                }
                                if (null == as || as.size() == 0) {
                                    as = (List) config.get("attributes");
                                }

                                String op = (String) input.get("op");
                                if (StringUtils.isNotBlank(op)) {
                                    getAs(as, op);
                                }

                                List<Map> ret = new LinkedList<>();
                                for (String h : hs) {
                                    try {
                                        String[] ipport = h.split(":");
                                        HashMap c = new HashMap();
                                        c.put("host", ipport[0]);
                                        c.put("port", ipport[1]);
                                        //log.error("get mbean info:" + c);
                                        MBeanManager mt = JVMUtil.checkJMXConnect(c);
                                        Map d = getData(mt, h, ns, as, c);
                                        if (null != d) {
                                            ret.add(d);
                                        }
                                    } catch (Exception e) {
                                        //log.error("",e);
                                    }

                                }

                                ret = setRet(ret, op);
                                return ret;
                            }
                        }
                    }
                }
            }
        }catch (Exception e){
            log.error("",e);
        }

        return null;
    }
    void getAs(List<String> as,String op){
        if("getInstances".equals(op)){
            as.clear();
            as.add("Instance");
        }else if("getTimeTask".equals(op)){
            as.clear();
            as.add("QuartzInfo");
        }else if("getRunningServices".equals(op)){
            as.clear();
            as.add("CountApiInvoke");
            as.add("CountApiInvokeDurTime");
        }else if("getUserRequest".equals(op)){
            as.clear();
            as.add("UserApiInvoke");
            as.add("RunningOutApis");
            as.add("RunningSql");
        }else if("getSqls".equals(op)){
            as.clear();
            as.add("Sqls");
        }else if("getOutApis".equals(op)){
            as.clear();
            as.add("OutApis");
        }else if("getThreads".equals(op)){
            as.clear();
            as.add("ActiveThreadLoad");
        }
    }
    List<Map> setRet(List<Map> ret,String op){
        if(null != ret && ret.size()>0 && StringUtils.isNotBlank(op)) {
            if ("getInstances".equals(op)) {
                List<Map> nret = new ArrayList();
                for (Map m : ret) {
                    Map n = (Map) m.get("Instance");
                    if (null != n) {
                        Map nn = new HashMap();
                        Iterator it = n.keySet().iterator();
                        while (it.hasNext()) {
                            String k = (String) it.next();
                            if (null != n.get(k) && n.get(k) instanceof Map) {
                                nn.putAll((Map) n.get(k));
                            } else {
                                nn.put(k, n.get(k));
                            }
                        }
                        nret.add(nn);
                    }
                }
                return nret;
            } else if ("getTimeTask".equals(op)) {
                List<Map> nret = new ArrayList();
                for (Map m : ret) {
                    Map n = (Map) m.get("QuartzInfo");
                    if (null != n) {
                        Map nn = new HashMap();
                        Iterator it = n.keySet().iterator();
                        while (it.hasNext()) {
                            String k = (String) it.next();
                            if (null != n.get(k) && n.get(k) instanceof Map) {
                                nn.put("TaskName", k);
                                nn.putAll((Map) n.get(k));
                            } else {
                                nn.put(k, n.get(k));
                            }
                        }
                        nret.add(nn);
                    }
                }
                return nret;
            } else if ("getRunningServices".equals(op)) {
                List<Map> nret = new ArrayList();
                for (Map m : ret) {
                    Map n = (Map) m.get("CountApiInvoke");
                    Map dn = (Map) m.get("CountApiInvokeDurTime");
                    if (null != n) {
                        Iterator it = n.keySet().iterator();
                        while (it.hasNext()) {
                            String k = (String) it.next();
                            Map nn = null;
                            for (int i = 0; i < nret.size(); i++) {
                                if (nret.get(i).get("ServiceName").equals(k)) {
                                    nn = nret.get(i);
                                    break;
                                }
                            }
                            if (null == nn) {
                                nn = new HashMap();
                                nret.add(nn);
                            }
                            if (null != n.get(k) && n.get(k) instanceof Map) {
                                nn.put("ServiceName", k);
                                if (!nn.containsKey("Cost")) nn.put("Cost", 0);
                                if (!nn.containsKey("Count")) nn.put("Count", 0);
                                nn.put("Cost", (Integer) nn.get("Cost") + ObjectUtils.getInt(((Map) n.get(k)).get("Cost")));
                                nn.put("Count", (Integer) nn.get("Count") + ObjectUtils.getInt(((Map) n.get(k)).get("Count")));
                                if (StringUtils.isNotBlank(nn.get("Cost")) && StringUtils.isNotBlank(nn.get("Count")))
                                    nn.put("AveCost", ObjectUtils.getLong(nn.get("Cost")) / ObjectUtils.getLong(nn.get("Count")));
                                if (null != dn) {
                                    Map dm = (Map) dn.get(k);
                                    if (null != dm) {
                                        if (!nn.containsKey("DurTime")) nn.put("DurTime", new HashMap());
                                        Iterator tt = dm.keySet().iterator();
                                        while (tt.hasNext()) {
                                            String tim = (String) tt.next();
                                            if (!((Map) nn.get("DurTime")).containsKey(tim)) {
                                                ((Map) nn.get("DurTime")).put(tim, 0);
                                            }
                                            ((Map) nn.get("DurTime")).put(tim, (Integer) ((Map) nn.get("DurTime")).get(tim) + ObjectUtils.getInt(dm.get(tim)));

                                        }
                                    }
                                }
                            }
                        }

                    }
                }
                if (nret.size() > 0) {
                    for (int i = 0; i < nret.size(); i++) {
                        Map m = (Map) ((Map) nret.get(i)).get("DurTime");
                        if (null != m) {
                            List ll = new LinkedList();
                            Map tm = ArrayUtils.sortMapByKey(m);
                            Iterator its = tm.keySet().iterator();
                            while (its.hasNext()) {
                                Object o = its.next();
                                HashMap t = new HashMap();
                                t.put("Time", o);
                                t.put("Count", tm.get(o));
                                ll.add(t);
                            }
                            ((Map) nret.get(i)).put("DurTime", ll);
                        }
                    }

                }
                return nret;
            } else if ("getUserRequest".equals(op)) {
                List<Map> nret = new ArrayList();
                for (Map m : ret) {
                    Map n = (Map) m.get("UserApiInvoke");
                    List<Map> rapi = (List) m.get("RunningOutApis");
                    List<Map> rsql = (List) m.get("RunningSql");
                    if (null != n) {
                        Iterator it = n.keySet().iterator();
                        while (it.hasNext()) {
                            String k = (String) it.next();
                            Map nn = init(nret, k, "UserCode");
                            if (null != nn && null != n.get(k) && n.get(k) instanceof Map) {
                                Iterator its = ((Map) n.get(k)).keySet().iterator();
                                while (its.hasNext()) {
                                    String kk = (String) its.next();
                                    if ("expireTime".equals(kk)) {
                                        if (null != ((Map) n.get(k)).get("expireTime"))
                                            nn.put("expireTime", ((Map) n.get(k)).get("expireTime"));
                                    } else if ("loginTime".equals(kk)) {
                                        if (null != ((Map) n.get(k)).get("loginTime"))
                                            nn.put("loginTime", ((Map) n.get(k)).get("loginTime"));
                                    }else if("Client".equals(kk)){
                                        continue;
                                    } else {
                                        if (!nn.containsKey("apis")) {
                                            nn.put("apis", new HashMap());
                                        }
                                        if (!((Map) nn.get("apis")).containsKey(kk)) {
                                            ((Map) nn.get("apis")).put(kk, new HashMap());
                                        }
                                        ((Map) (((Map) nn.get("apis")).get(kk))).put("ServiceName", kk);
                                        if(null !=((Map) n.get(k)).get(kk)) {
                                            Integer c = null;
                                            if(((Map) n.get(k)).get(kk) instanceof Integer)
                                                c = (Integer)((Map) n.get(k)).get(kk);
                                            if(((Map) n.get(k)).get(kk) instanceof Map && null != ((Map) ((Map) n.get(k)).get(kk)).get("Count") ){
                                                c = ObjectUtils.getInt(((Map) ((Map) n.get(k)).get(kk)).get("Count"));
                                            }
                                            if (!((Map) (((Map) nn.get("apis")).get(kk))).containsKey("Count"))
                                                ((Map) (((Map) nn.get("apis")).get(kk))).put("Count", 0);
                                            ((Map) (((Map) nn.get("apis")).get(kk))).put("Count", (Integer) ((Map) (((Map) nn.get("apis")).get(kk))).get("Count") + c);
                                        }
                                        if (!((Map) (((Map) nn.get("apis")).get(kk))).containsKey("sqls"))
                                            ((Map) (((Map) nn.get("apis")).get(kk))).put("sqls", new ArrayList());
                                        appendSql((List) ((Map) (((Map) nn.get("apis")).get(kk))).get("sqls"), rsql, k, kk);
                                        if (!((Map) (((Map) nn.get("apis")).get(kk))).containsKey("outapis"))
                                            ((Map) (((Map) nn.get("apis")).get(kk))).put("outapis", new ArrayList());
                                        appendOutApi((List) ((Map) (((Map) nn.get("apis")).get(kk))).get("outapis"), rsql, k, kk);
                                    }
                                }
                            }
                        }
                        if(nret.size()>0){
                            for(Map mt :nret){
                                if(null != mt.get("apis") && mt.get("apis") instanceof Map){
                                    mt.put("apis",new ArrayList(((Map)mt.get("apis")).values()));
                                }
                            }
                        }
                    }
                }
                return nret;
            } else if ("getSqls".equals(op)) {
                List<Map> nret = new ArrayList();
                for (Map m : ret) {
                    Map n = (Map) m.get("Sqls");
                    if (null != n) {
                        Iterator it = n.keySet().iterator();
                        while (it.hasNext()) {
                            String k = (String) it.next();
                            Map nn = init(nret, k, "SQL");
                            if (null != n.get(k) && n.get(k) instanceof Map) {
                                if (!nn.containsKey("Count")) nn.put("Count", new Long(0));
                                if (!nn.containsKey("Cost")) nn.put("Cost", new Long(0));
                                nn.put("Count", (Long) nn.get("Count") + ObjectUtils.getLong(((Map) n.get(k)).get("Count")));
                                nn.put("Cost", (Long) nn.get("Cost") + ObjectUtils.getLong(((Map) n.get(k)).get("Cost")));
                                long l = ObjectUtils.getLong(((Map) n.get(k)).get("Count"));
                                if(l>0)
                                nn.put("AveCost", ObjectUtils.getLong(nn.get("Cost")) / l);
                            }
                        }
                    }
                }
                return nret;
            } else if ("getOutApis".equals(op)) {
                List<Map> nret = new ArrayList();
                for (Map m : ret) {
                    Map n = (Map) m.get("OutApis");
                    if (null != n) {
                        Iterator it = n.keySet().iterator();
                        while (it.hasNext()) {
                            String k = (String) it.next();
                            Map nn = init(nret, k, "OutApi");
                            if (null != n.get(k) && n.get(k) instanceof Map) {
                                if (!nn.containsKey("Count")) nn.put("Count", new Long(0));
                                if (!nn.containsKey("Cost")) nn.put("Cost", new Long(0));
                                nn.put("Count", (Long) nn.get("Count") + ObjectUtils.getLong(((Map) n.get(k)).get("Count")));
                                nn.put("Cost", (Long) nn.get("Cost") + ObjectUtils.getLong(((Map) n.get(k)).get("Cost")));
                                long l = ObjectUtils.getLong(((Map) n.get(k)).get("Count"));
                                if(l>0)
                                    nn.put("AveCost", ObjectUtils.getLong(nn.get("Cost")) / l);
                            }
                        }
                    }
                }
                return nret;
            } else if ("getThreads".equals(op)) {
                List<Map> nret = new ArrayList();
                for (Map m : ret) {
                    List<Map> n = (List) m.get("ActiveThreadLoad");
                    nret.addAll(n);
                }
                return nret;
            }
        }
        return ret;
    }
    void appendSql(List list,List<Map> sqls,String userCode,String serviceName){
        if(null != sqls && StringUtils.isNotBlank(userCode) && StringUtils.isNotBlank(serviceName)){
            for(Map m :sqls){
                if(userCode.equals(m.get("UserCode")) && serviceName.equals(m.get("APIName"))){
                    HashMap map = new HashMap();
                    map.put("InsPath",m.get("InsPath"));
                    map.put("Cost",m.get("Cost"));
                    map.put("Sql",m.get("Sql"));
                    map.put("SqlParameters",m.get("SqlParameters"));
                    list.add(map);
                }
            }
        }
    }
    void appendOutApi(List list,List<Map> apis,String userCode,String serviceName){
        if(null != apis && StringUtils.isNotBlank(userCode) && StringUtils.isNotBlank(serviceName)){
            for(Map m :apis){
                if(userCode.equals(m.get("UserCode")) && serviceName.equals(m.get("APIName"))){
                    HashMap map = new HashMap();
                    map.put("InsPath",m.get("InsPath"));
                    map.put("Cost",m.get("Cost"));
                    map.put("OutApi",m.get("OutApi"));
                    map.put("InputParameters",m.get("InputParameters"));
                    list.add(map);
                }
            }
        }
    }
    Map init(List<Map> ret,String k,String type){
        if(null != ret){
            for(Map m:ret){
                if(null != m.get(type) && m.get(type).equals(k)){
                    return m;
                }
            }
            HashMap m = new HashMap();
            m.put(type,k);
            ret.add(m);
            return m;
        }
        return null;
    }

    Map getData(MBeanManager mt,String h,List<String> ns,List<String> as,Map c)throws Exception{
        //log.error("get remote info:"+ns.size()+" "+as.size()+" "+mt);
        if(null != ns && ns.size()>0 && null != as && as.size()>0) {
            Map d = new HashMap();
            for(String nt:ns) {
                d.put("RemoteHost", h);
                ObjectName na = new ObjectName(nt);
                AttributeList ins=null;
                try {
                    ins = mt.getAttributes(na, as.toArray(new String[0]));
                }catch (IOException ex){
                    JVMUtil.removeCache(c);
                    mt = JVMUtil.checkJMXConnect(c);
                    ins = mt.getAttributes(na, as.toArray(new String[0]));
                }
                if(null != ins && ins.size()>0) {
                    for(int i=0;i<ins.size();i++) {
                        Attribute t = (Attribute)ins.get(i);
                        if (null != t.getValue() && t.getValue() instanceof String && ((String)t.getValue()).startsWith("{")) {
                            d.put(t.getName(),StringUtils.convert2MapJSONObject((String)t.getValue()));
                        } else if(null != t.getValue() && t.getValue() instanceof String && ((String)t.getValue()).startsWith("[")){
                            d.put(t.getName(), StringUtils.convert2ListJSONObject((String)t.getValue()));
                        }else {
                            d.put(t.getName(),t.getValue());
                        }
                    }
                }
            }
            return d;


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
