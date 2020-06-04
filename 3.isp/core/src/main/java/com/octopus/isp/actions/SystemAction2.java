package com.octopus.isp.actions;

import com.octopus.isp.bridge.impl.Bridge;
import com.octopus.isp.ds.RequestParameters;
import com.octopus.tools.dataclient.v2.DataClient2;
import com.octopus.tools.dataclient.v2.ds.TableContainer;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.bftask.BFParameters;
import com.octopus.utils.cachebatch.DateTimeUtil;
import com.octopus.utils.cls.proxy.GeneratorClass;
import com.octopus.utils.ds.TableBean;
import com.octopus.utils.ds.TableField;
import com.octopus.utils.exception.ExceptionUtil;
import com.octopus.utils.exception.ISPException;
import com.octopus.utils.file.FileUtils;
import com.octopus.utils.net.NetUtils;
import com.octopus.utils.si.jvm.JVMUtil;
import com.octopus.utils.time.DateTimeUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import com.octopus.utils.xml.desc.Desc;
import com.octopus.utils.zip.ZipUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigDecimal;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarOutputStream;

/**
 * User: wfgao_000
 * Date: 16-3-20
 * Time: 下午8:56
 */
public class SystemAction2 extends XMLDoObject {
    private static transient Log log = LogFactory.getLog(SystemAction.class);
    static String INSID_SRVNAME_SPLITCHAR=".";
    XMLDoObject tracelog;
    XMLDoObject command;
    XMLDoObject resource;
    XMLDoObject auth;
    XMLDoObject remote;
    XMLDoObject dataclient;
    String simreturndir;
    List<String> servicesearch;
    Map<String,List<Map>> srvIdRelIns = new ConcurrentHashMap<String, List<Map>>(); //Key is SrvId, Map is Ins info
    Map<String,List<Map>> srvInfoInCenter = new ConcurrentHashMap<String, List<Map>>(); //Key is SrvId, Map is srv info in each ins
    List<String> initpublish;
    public SystemAction2(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        String s = xml.getProperties().getProperty("servicesearch");   //search package
        if(StringUtils.isNotBlank(s)){
            servicesearch=Arrays.asList(s.split("\\,"));
        }
        if(StringUtils.isNotBlank(xml.getProperties().getProperty("simreturndir"))) {
            simreturndir = (String) getEmptyParameter().getValueFromExpress(xml.getProperties().getProperty("simreturndir"),this);
        }
        //init local services after this instance launch up
        addAfterApplicationInitialAction(this,"init",null,null);
    }
    public void doInitial(){

    }
    public void init(){
        try {

            updatePublishedSrvs();
            //load all services to local

        }catch (Exception x){
            log.error("",x);
        }
    }
    //update published srvs in zk
    void updatePublishedSrvs()throws Exception{

    }

    String[] getInsNameBySrvId(String srvName){

        List<Map> list = srvIdRelIns.get(srvName);
        if(null != list){
            List ret = new ArrayList();
            for(Map m:list){
                ret.add(m.get("insId"));
            }
            return (String[])ret.toArray(new String[0]);
        }
        return null;
    }

    void notifyBySuspendSrv(String srvName,String insId){
        if(log.isInfoEnabled()) {
            log.info("syn center info, removeSrv " +srvName+ " from " + insId);
        }
        synchronized (srvIdRelIns) {
            if (StringUtils.isBlank(insId)) {//if insId is null means remove srv in all instances
                srvIdRelIns.remove(srvName);
                srvInfoInCenter.remove(srvName);
            } else {
                removeInsInBySrv(srvName, insId);
                removeSrvInfoFromCache(srvName,insId);

            }
            if (log.isDebugEnabled()) {
                log.info("syn center info, notifyBySuspendSrv " + srvName + " " + insId);
            }
        }
        if(addressmap.size()>0){
            String k = getAddressMapKey(insId,srvName);
            if(addressmap.containsKey(k)){
                addressmap.remove(k);
            }
        }
    }
    void  notifyByRemoveIns(String insId){
        if(log.isInfoEnabled()) {
            log.info("syn center info, notifyByRemoveIns " + " from " + insId);
        }
        if(null != srvIdRelIns) {
            synchronized (srvIdRelIns) {
                Iterator<String> name = srvIdRelIns.keySet().iterator();
                while (name.hasNext()) {
                    String n = name.next();
                    removeInsInBySrv(n, insId);
                }
            }
        }
        if(null != srvInfoInCenter){
            synchronized (srvInfoInCenter){
                Iterator<String> name = srvInfoInCenter.keySet().iterator();
                while(name.hasNext()){
                    String n = name.next();
                    removeSrvInfoFromCache(n,insId);
                }
            }
        }
        if(addressmap.size()>0){
            Iterator its = addressmap.keySet().iterator();
            while(its.hasNext()){
                String k = (String)its.next();
                if(k.contains(insId)){
                    addressmap.remove(k);
                }
            }
        }
    }
    void removeInsInBySrv(String srvName,String insId){
        if(log.isDebugEnabled()) {
            log.info("syn center info, removeInsInBySrv " +srvName+ " from " + insId);
        }
        synchronized (srvIdRelIns) {
            List<Map> t = srvIdRelIns.get(srvName);
            if (null != t) {
                for (int i = t.size() - 1; i >= 0; i--) {
                    if (t.get(i).get("INS_ID").equals(insId)) {
                        t.remove(i);
                    }
                }
            }
        }

    }
    void removeSrvInfoFromCache(String srvName,String insId){
        synchronized (srvInfoInCenter){
            List<Map> t = srvInfoInCenter.get(srvName);
            if(null != t){
                for(int i=t.size()-1;i>=0;i--){
                    if(t.get(i).get("INS_ID").equals(insId)){
                        t.remove(i);
                    }
                }
            }
        }
    }

    String getRedoFlag(Object o){
        if(null != o) {
            if (o instanceof String) {
                return (String) o;
            } else if (o instanceof Map) {
                return "true";
            }
        }
        return "false";
    }


    void putIns(String id,List<Map> t){
        if(null != t && null != id) {
            boolean isin = false;
            for (Map m : t) {
                if (m.get("INS_ID").equals(id)) {
                    isin = true;
                    break;
                }
            }

            if (!isin) {
                HashMap itm = new HashMap();
                itm.put("INS_ID", id);
                t.add(itm);
            }
        }
    }

    /**
     * get all service name in xml container
     * @return
     */
    String[] getAllServices(){
        LinkedList<String> ls = new LinkedList<String>();
        Map<String,XMLObject> objs = getXMLObjectContainer();
        if(null!=objs){
            Iterator<String> aname = objs.keySet().iterator();
            while(aname.hasNext()) {
                String name = aname.next();
                XMLObject parent = objs.get(name).getParent();
                if (null != parent && (parent.getXML().getId().startsWith("Load") || parent.getXML().getId().startsWith("load"))) {
                    ls.add(name);
                } else if(null != objs.get(name).getXML().getParent() && objs.get(name).getXML().getParent().getName().startsWith("actions")){
                    ls.add(name);
                }
            }
        }
        return (String[])ls.toArray(new String[0]) ;
    }
    List<Map> getStatByCenter(String opTypeOrInsId,String name,String[] insids,Map<String,String> servicesStatus,List<String> usedList){
        try {
            if(null != servicesStatus){
                List<Map> ret = new LinkedList<Map>();
                Iterator<String> ist = servicesStatus.keySet().iterator();
                while(ist.hasNext()){
                    String k = ist.next();
                    String insid = k.substring(1,k.indexOf(".",2));
                    //.INS-NJ_115.ProvisioningDomainV1.0Op.  INS-NJ_115,INS-CONSOLE:INS-NJ_115 result:false
                    log.debug(opTypeOrInsId+" "+name+" query cond "+k+"  " + ArrayUtils.toJoinString(insids) + ":" + insid + " result:"
                            + ((StringUtils.isBlank(opTypeOrInsId) || !opTypeOrInsId.startsWith("INS-")
                            || (opTypeOrInsId.startsWith("INS-") && k.startsWith("." + opTypeOrInsId + "."))) && k.contains("." + name + ".")));
                    if(ArrayUtils.isInStringArray(insids,insid)) {
                        if ((StringUtils.isBlank(opTypeOrInsId) || !opTypeOrInsId.startsWith("INS-") || (opTypeOrInsId.startsWith("INS-") && k.startsWith("." + opTypeOrInsId + "."))) && k.contains("." + name + ".")) {
                            log.debug("add service to result "+k);
                            ret.add(StringUtils.convert2MapJSONObject((String) servicesStatus.get(k)));
                            if (null != usedList) {
                                usedList.add(k);
                            }
                        }
                    }
                }
                return ret;
            }
            return null;
        }catch (Exception e){
            log.error("getStatByCenter",e);
        }
        return null;
    }
    Map getServiceInfoAndStatus(String id,Map st,List<Map> cs,Map<String,Boolean> servicesStatus ){
        HashMap map = new HashMap();
        map.put("NAME", id);
        if(null != st) {
            map.put("PACKAGE", st.get("package"));
            map.put("REDO", getRedoFlag(st.get("redo")));
            map.put("SHARE", st.get("share"));
            map.put("IS_ALARM",isAlarm(st));
            map.put("IS_WORKTIME",isWorktime(st));
            map.put("PATH", st.get("path"));
            map.put("OP_TYPE", st.get("opType"));
            map.put("CREATE_BY", st.get("createby"));
            map.put("AUTHOR", st.get("author"));
        }else{
            if(null != cs && cs.size()>0) {
                Map m = cs.get(0);
                if(null != m){
                    map.put("PACKAGE", m.get("package"));
                    map.put("REDO", getRedoFlag(m.get("redo")));
                    map.put("SHARE", m.get("share"));
                    map.put("IS_ALARM",m.get("isalarm"));
                    map.put("PATH", m.get("path"));
                    map.put("OP_TYPE", m.get("opType"));
                    map.put("CREATE_BY", m.get("date"));
                    map.put("AUTHOR", m.get("author"));
                }
            }
        }
        map.put("IS_PUBLISH", "Y");

        map.put("IS_LOCAL",getObjectById(id)!=null);
        try {
            if(null != st) {
                if(null !=servicesStatus && servicesStatus.size()>0){
                    String k =  getZkPath("",(String)st.get("opType"),(String)st.get("package"),id);
                    if(null !=servicesStatus.get(k)){
                        map.put("STATUS", servicesStatus.get(k));
                    }
                }
                if(null == map.get("STATUS")) {
                    boolean isaa = false;//isPublishActive((String) st.get("opType"), (String) st.get("package"), id);
                    map.put("STATUS", isaa);
                }
            }
        } catch (Exception e) {
            // map.put("STATUS", "NOT EXIST");
        }
        //map.put("IS_LOCAL", "No");
        if (null != cs) {
            //set instance include the service info
            map.put("CHILDREN", cs);
            long count = 0, error = 0, time = 0, size = 0;
            //sum the instances invoke count
            for (Map c : cs) {
                if (null != c.get("INVOKE_COUNT") && !"null".equals(c.get("INVOKE_COUNT"))) {
                    count += getINVOKE_COUNTData(c,"INVOKE_COUNT");
                }
                if (null != c.get("INVOKE_ERROR_COUNT") && !"null".equals(c.get("INVOKE_ERROR_COUNT"))) {
                    error += getINVOKE_COUNTData(c,"INVOKE_ERROR_COUNT");
                }
                if (null != c.get("INVOKE_COST_TIME") && !"null".equals(c.get("INVOKE_COST_TIME"))) {
                    time += getINVOKE_COUNTData(c,"INVOKE_COST_TIME");
                }
                if (null != c.get("INVOKE_SIZE") && !"null".equals(c.get("INVOKE_SIZE"))) {
                    size += getINVOKE_COUNTData(c,"INVOKE_SIZE");
                }
                if(null != st) {
                    c.put("IS_PUBLISH", "Y");
                }else{
                    c.put("IS_PUBLISH", "N");
                }
                try {
                    String r = getPublishStateZkPath("",(String) c.get("INS_ID"), id);
                    if(null != servicesStatus && servicesStatus.containsKey(r)){
                        c.put("STATUS", servicesStatus.get(r));
                    }else {
                        boolean isa = false;//isPublishOneActive((String) c.get("INS_ID"), id);
                        c.put("STATUS", isa);
                    }
                } catch (Exception e) {
                    // map.put("STATUS","NOT EXIST");
                }

            }
            map.put("INVOKE_COUNT", count);
            map.put("INVOKE_ERROR_COUNT", error);
            map.put("INVOKE_COST_TIME", time);
            map.put("INVOKE_SIZE", size);
            if(null != st) {
                map.put("DATE", st.get("date"));
            }
        }
        return map;
    }
    long getINVOKE_COUNTData(Map d ,String key){
        if (null != d.get(key) && !"null".equals(d.get(key)) && !"".equals(d.get(key))) {
            if(d.get(key) instanceof String) {
                if("".equals(d.get(key))){
                    return 0;
                }else {
                    return Long.valueOf((String) d.get(key));
                }
            }else if(d.get(key) instanceof Integer){
                return (Integer) d.get(key);
            }else if(d.get(key) instanceof Long){
                return (Long) d.get(key);
            }
        }
        return 0;
    }


    /**
     * get all srv info list for showing in srv flow page
     * @return map<srvName,srvDesc>
     */
    List<Map> getServiceListFromZk(){
        try {


        }catch (Exception e){

        }
        return null;
    }

    Map<String,Integer[]> getSrvTotalStatFromDB(){
        if(null != dataclient && dataclient instanceof DataClient2){
            try {
                Map map = new HashMap();
                map.put("op", "query");
                map.put("sqls", Arrays.asList("SELECT srv_name ,SUM(invoke_count) invoke_count,SUM(invoke_cost_time) invoke_cost_time,SUM(invoke_error_count) invoke_error_count,SUM(invoke_size) invoke_size FROM isp_srv_stat_log GROUP BY srv_name"));
                List<Map> ls = (List)dataclient.doSomeThing(null, null, map, null, null);
                if(null != ls){
                    Map<String,Integer[]> ret  = new HashMap<>();
                    for(Map m:ls){
                        Integer[] c = new Integer[4];
                        c[0]=((BigDecimal) m.get("INVOKE_COUNT")).intValue();
                        c[1]=((BigDecimal) m.get("INVOKE_COST_TIME")).intValue();
                        c[2]=((BigDecimal) m.get("INVOKE_ERROR_COUNT")).intValue();
                        c[3]=((BigDecimal) m.get("INVOKE_SIZE")).intValue();
                        ret.put((String)m.get("SRV_NAME"),c);
                    }
                    return ret;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return null;
    }
    /**
     *
     * @param name
     * @return
     */
    List<Map> findServicesByCenter(Map<String,List<Map>> othersInsServices,Map<String,Map> services,XMLParameter env,String name){
        try {
            if(null != othersInsServices) {
                List<Map> ret = new ArrayList();
                Map<String,Integer[]> totalStat = getSrvTotalStatFromDB();
                List<Map> ls = findServices(name,totalStat);
                List<String> temp = new ArrayList();
                if(null != ls){
                    for(Map m:ls){
                        temp.add((String)m.get("NAME"));
                        //匹配查找的路径
                        if(null != servicesearch && servicesearch.size()>0){
                            String pk = (String)m.get("package");
                            if(null ==pk){
                                pk = (String)m.get("PACKAGE");
                            }
                            if(!ArrayUtils.isLikeArrayInString(pk,servicesearch)){
                                continue;
                            }
                        }
                        if(null== othersInsServices || !othersInsServices.containsKey(m.get("NAME"))){
                            //本实例独有的服务
                            m.put("IS_PUBLISH","N");
                            List<Map> lt = (List)m.get("CHILDREN");
                            if(null != lt){
                                for(Map s:lt){
                                    s.put("IS_PUBLISH","N");
                                }
                            }
                            //加入到返回列表中
                            ret.add(m);

                        }else{
                            //othersInsServices中也含有该服务
                            List<Map> ins= othersInsServices.get(m.get("NAME"));
                            if(null != ins) {
                                for (Map mx : ins) {
                                    if (null != ((List) m.get("CHILDREN"))) {
                                        ((List)m.get("CHILDREN")).add(ConvertToPage(mx,m));
                                    }

                                }
                            }
                            ret.add(m);
                        }
                    }
                }
                appendOthers(temp,othersInsServices,services,ret,totalStat,name);
                if(log.isDebugEnabled()){
                    log.debug("after self search services "+ret);
                }
                List<Map> res = new LinkedList<Map>();
                for(Map m:ret){
                    if(null != m.get("CHILDREN") && ((List)m.get("CHILDREN")).size()>0){
                        res.add(m);
                    }
                }
                if(log.isDebugEnabled()){
                    log.debug("query srv result:"+res);
                }
                filterAuth(env,res);
                if(log.isDebugEnabled()){
                    log.debug("query srv result2:"+res);
                }
                return res;
            }
        }catch (Exception e){
            log.error("get service list error",e);
        }
        return null;
    }
    boolean ismatch(String searchKey,String name,Map desc){
        try {
            if(log.isDebugEnabled())
                log.debug("find service by "+searchKey+" name:"+name+" desc:"+desc);
            if (StringUtils.isNotBlank(name) && name.contains(searchKey)) return true;
            if(null != desc) {
                String n = ObjectUtils.convertMap2String((Map)desc);

                if(StringUtils.isNotBlank(n)) {
                    n = n.replaceAll("\"","");
                    return n.indexOf(searchKey) > 0;
                }
            }
            return false;
        }catch (Exception e){
            return false;
        }
    }


    public List getTreeServices(List<Map> ss){
        if(null != ss){
            List ret = new LinkedList();
            for(Map s:ss){
                putTreeNode("",(String) s.get("PACKAGE"), (String) s.get("NAME"), ret);
            }
            return ret;
        }
        return null;
    }
    void putTreeNode(String pid,String pk,String name,List<Map> ret){
        if(StringUtils.isNotBlank(pk)){
            if(pk.contains(".")) {
                int n = pk.indexOf(".");
                String s = pk.substring(0,n);
                String ls = pk.substring(n+1);
                boolean is=false;
                for(Map m :ret){
                    if(m.get("id").equals(s)){
                        putTreeNode(s,ls,name,(List)m.get("children"));
                        is=true;
                        break;
                    }
                }
                if(!is){
                    Map c = new LinkedHashMap();
                    c.put("id",s);
                    c.put("name",s);
                    List ll = new LinkedList();
                    c.put("children",ll);
                    c.put("pId",pid);
                    ret.add(c);
                    putTreeNode(s,ls,name,ll);
                }

            }else{
                boolean is=false;
                for(Map m :ret){
                    if(m.get("id").equals(pk)){
                        List<Map> ll = (List)m.get("children");
                        Map c = new LinkedHashMap();
                        c.put("id",name);
                        c.put("name",name);
                        c.put("pId",pk);
                        ll.add(c);
                        is=true;
                        break;
                    }
                }
                if(!is){
                    Map c = new LinkedHashMap();
                    c.put("name",pk);
                    c.put("id",pk);
                    c.put("pId",pid);
                    List ll = new LinkedList();
                    c.put("children",ll);
                    Map m = new LinkedHashMap();
                    m.put("id",name);
                    m.put("name",name);
                    m.put("pId",pk);
                    ll.add(m);
                    ret.add(c);
                }

            }
        }else{
            Map c = new LinkedHashMap();
            c.put("pId","");
            c.put("id",name);
            c.put("name",name);
            ret.add(c);
        }
    }


    void putTreeNode2(String pk,String name,Map ret){
        if(StringUtils.isNotBlank(pk)){
            if(pk.contains(".")) {
                int n = pk.indexOf(".");
                String s = pk.substring(0,n);
                String ls = pk.substring(n+1);

                if (ret.containsKey(s)) {
                    putTreeNode2(ls,name,(Map)ret.get(s));
                }else{
                    Map c = new LinkedHashMap();
                    ret.put(s, c);
                    putTreeNode2(ls,name,c);
                }

            }else{
                if(ret.containsKey(pk)){
                    ((Map)ret.get(pk)).put(name,"");
                }else {
                    Map m = new LinkedHashMap();
                    m.put(name, "");
                    ret.put(pk, m);
                }
            }
        }else{
            ret.put(name,"");
        }
    }
    /**
     * below sv can be show
     * 1. create by user
     * 2. hava auth config
     * 3. other share
     * @param res
     */
    void filterAuth(XMLParameter env,List<Map> res){
        if(null != auth && null != res){
            HashMap map = new HashMap();
            for(int i=res.size()-1;i>=0;i--){
                map.put("op","isCanSee");
                map.put("srvId",res.get(i).get("NAME"));
                try {
                    if (!((Boolean)auth.doSomeThing(null,env, map, null, null))) {
                        res.remove(i);
                    }
                }catch (Exception e){
                    res.remove(i);
                }
                map.clear();
            }

        }
    }
    void appendCount(Map t,Map s){
        List<Map> tt = (List)t.get("CHILDREN");
        if(null != tt) {
            for (Map m : tt) {
                if (m.get("INS_ID").equals(s.get("INS_ID"))) return;
            }
        }
        if(null != t.get("INVOKE_COUNT"))
            t.put("INVOKE_COUNT",(Long)t.get("INVOKE_COUNT")+ObjectUtils.getLong(s.get("INVOKE_COUNT")));
        if(null != t.get("INVOKE_ERROR_COUNT"))
            t.put("INVOKE_ERROR_COUNT",(Long)t.get("INVOKE_ERROR_COUNT")+ObjectUtils.getLong(s.get("INVOKE_ERROR_COUNT")));
        if(null != t.get("INVOKE_COST_TIME"))
            t.put("INVOKE_COST_TIME",(Long)t.get("INVOKE_COST_TIME")+ObjectUtils.getLong(s.get("INVOKE_COST_TIME")));
        if(null!= t.get("INVOKE_SIZE"))
            t.put("INVOKE_SIZE",(Long)t.get("INVOKE_SIZE")+ObjectUtils.getLong(s.get("INVOKE_SIZE")));
        if(null != tt)
            tt.add(s);
    }

    void appendOthers(List<String> exists,Map<String,List<Map>> othersrvins,Map<String,Map> services,List<Map> ret,Map totalStat,String name){
        Iterator<String> its = othersrvins.keySet().iterator();
        while(its.hasNext()){
            String k = its.next();
            if(null== exists || !exists.contains(k)) {
                if(services.get(k)!=null && (StringUtils.isBlank(name)||(null != name && k.contains(name)))) {
                    List<Map> ins = othersrvins.get(k);
                    Map map = new HashMap();
                    Integer[] stats = getSrvTotalStat(k, totalStat);
                    convertPageServiceInfo(map, services.get(k), stats, true, false, false);
                    map.put("CHILDREN", new ArrayList());
                    for (Map in : ins)
                        ((List) map.get("CHILDREN")).add(ConvertToPage(in, services.get(k)));
                    ret.add(map);
                }
            }
        }
    }
    Map ConvertToPage(Map insInfo,Map srvInfo){
        Map ret = new HashMap();
        ObjectUtils.appendDeepMapNotReplaceKey(insInfo,ret);
        ret.put("IP",ret.get("ip"));
        ret.put("INS_ID",ret.get("insId"));
        ret.put("INS_STATUS","Running");
        if(srvInfo.get("isable")!=null)
        ret.put("STATUS",StringUtils.isTrue((String)srvInfo.get("isable")));
        if(srvInfo.get("STATUS")!=null)
            ret.put("STATUS",srvInfo.get("STATUS"));
        ret.put("PID",ret.get("pid"));
        convertPageInsInfo(ret,null,null,null,null,null);
        return ret;
    }

    /**
     * find service info, the service must has description file
     * 服务名称、包路径、操作类型、存储路径
     * @param linkename
     * @return
     * [
     *  {"IS_WORKTIME":false,"PATH":"file:../data/sv/","INVOKE_COST_TIME":0,"OP_TYPE":"business","AUTHOR":"LiGS","SHARE":""
     *  ,"INVOKE_SIZE":0,"NAME":"ViewCustomerInfo4NewCRM","IS_PUBLISH":"Y"
     *  ,"CHILDREN":[
     *      {"IP":"10.11.20.115","package":"com.test","isable":"true","redo":{"overTime":"11111","count":"1","duringTime":"111","message":"sfsfs"}
     *      ,"share":"","author":"LiGS","isalarm":"false","path":"file:../data/sv/","opType":"business","date":"2018-08-09 11:34:26"
     *      ,"INS_ID":"INS-CONSOLE","INS_STATUS":"STOPPED","PID":""
     *      ,"INVOKE_COUNT":"","INVOKE_ERROR_COUNT":"","INVOKE_COST_TIME":"","INVOKE_SIZE":"","IS_PUBLISH":"Y","STATUS":true}]
     */
    List<Map> findServices(String linkename,Map totalStat) throws IOException {
        LinkedList<Map> ls = new LinkedList<Map>();
        Map<String,XMLObject> objs = getXMLObjectContainer();
        String ip = NetUtils.getip();
        String pid = JVMUtil.getPid();
        String insId = "";
        XMLObject root = getRoot();
        if(null != root){
            if(root instanceof Bridge){
                insId=((Bridge)root).getInstanceId();
            }
        }
        if(null!=objs && objs.size()>0){
            Iterator<String> aname = objs.keySet().iterator();
            while (aname.hasNext()) {
                String name = aname.next();
                XMLObject m = objs.get(name);
                if (null == linkename || ismatch(linkename, name, m.getDescStructure())) {
                    XMLObject o = objs.get(name);
                    try {

                        Map st = o.getInvokeDescStructure();
                        HashMap d = new HashMap();
                        if (null != st && StringUtils.isNotBlank(st.get("name"))) {
                            if (null != st.get("name") && st.get("name") instanceof String) {
                                //set service to page
                                Integer[] stats=getSrvTotalStat((String)st.get("name"),totalStat);
                                convertPageServiceInfo(d,st,stats,o.isActive(),true,false);

                                List chl = new ArrayList();
                                Map ins = new HashMap();
                                convertPageInsInfo(ins,ip,insId,pid,o.isActive(),(String)st.get("package"));
                                chl.add(ins);
                                d.put("CHILDREN",chl);

                            } else {
                                log.error("find [" + name + "] desc incorrect please check:\n" + st);
                            }
                        }
                        if (d.size() > 0)
                            ls.add(d);
                    } catch (Exception e) {
                        log.error("findServices error:", e);
                    }
                }
            }


        }

        log.debug("findServices ["+linkename+"] \n"+ls);
        return ls ;
    }

    Integer[] getSrvTotalStat(String srv,Map totalStat){
        Integer[] stats=null;
        if(null != totalStat){
            stats = (Integer[])totalStat.get(srv);
        }
        if(null == stats){
            stats=new Integer[4];
            stats[0]=0;
            stats[1]=0;
            stats[2]=0;
            stats[3]=0;
        }
        return stats;
    }

    void convertPageServiceInfo(Map target,Map src,Integer[] stats,boolean isactive,boolean islocal,boolean ispublish){
        target.put("NAME", src.get("name"));
        target.put("PATH", src.get("path"));
        target.put("PACKAGE", src.get("package"));
        target.put("REDO", getRedoFlag(src.get("redo")));
        target.put("SHARE", src.get("share"));
        target.put("IS_ALARM",isAlarm(src));
        target.put("IS_WORKTIME",isWorktime(src));
        target.put("PATH", src.get("path"));
        target.put("CREATE_BY", src.get("createby"));
        target.put("OP_TYPE", src.get("opType"));
        target.put("DATE", src.get("date"));
        target.put("AUTHOR",src.get("author"));
        target.put("STATUS", isactive);
        target.put("IS_LOCAL", islocal);
        target.put("IS_PUBLISH", (ispublish?"Y":"N"));
        target.put("INVOKE_COUNT", stats[0]);
        target.put("INVOKE_COST_TIME", stats[1]);
        target.put("INVOKE_ERROR_COUNT", stats[2]);
        target.put("INVOKE_SIZE", stats[3]);
    }
    void convertPageInsInfo(Map target,String ip,String insId,String pid,Boolean isactive,String pak){
        if(StringUtils.isNotBlank(ip))
        target.put("IP",ip);
        if(StringUtils.isNotBlank(pak))
        target.put("package",pak);
        if(StringUtils.isNotBlank(insId))
        target.put("INS_ID",insId);
        target.put("INS_STATUS","Running");
        if(null != isactive)
        target.put("STATUS",isactive);
        target.put("INVOKE_COST_TIME", 0);
        target.put("INVOKE_ERROR_COUNT", 0);
        target.put("INVOKE_COUNT", 0);
        target.put("INVOKE_SIZE", 0);
        if(StringUtils.isNotBlank(pid))
        target.put("PID",pid);
    }
    //get service parameter ,include input ,output,config
    Map getServiceParameters(String name){
        XMLObject o = getXMLObjectContainer().get(name);
        if(null != o){
            try {
                Map st = o.getDescStructure();
                if (null != st) {
                    HashMap ret = new HashMap();
                    ret.put("INPUT",st.get("input"));
                    ret.put("OUTPUT", st.get("output"));
                    ret.put("CONFIG",st.get("config"));
                    if(ret.size()>0)
                        return ret;
                }
            }catch (Exception e){}
        }
        return null;
    }
    Map getServiceOutput(String name){
        XMLObject o = getXMLObjectContainer().get(name);
        if(null != o){
            try {
                Map st = o.getDescStructure();
                if (null != st) {
                    HashMap ret = new HashMap();
                    ret.put("OUTPUT", st.get("output"));
                    if(ret.size()>0)
                        return ret;
                }
            }catch (Exception e){}
        }
        return null;
    }
    //get a service description info , include description、body、error、depend,original
    Map getServiceDescription(String name){
        XMLObject o = getXMLObjectContainer().get(name);
        if(null != o){
            try {
                Map st = o.getDescStructure();
                if (null != st) {
                    HashMap ret = new HashMap();
                    ret.put("DESCRIPTION",st.get("desc"));
                    ret.put("BODY",st.get("body"));
                    ret.put("ERROR",st.get("error"));
                    ret.put("DEPEND",st.get("depend"));
                    ret.put("ORIGINAL",st.get("original"));
                    if(ret.size()>0)
                        return ret;
                }
            }catch (Exception e){}
        }
        return null;
    }
    String getServiceBody(String name){
        Map m = getServiceDescription(name);
        if(null != m){
            HashMap r = new HashMap();
            r.put("BODY", (String)m.get("BODY"));
            r.put("ORIGINAL", (Map)m.get("ORIGINAL"));
            return ObjectUtils.convertMap2String(r);
        }
        return null;
    }
    Map getServiceUsage(String name){
        XMLObject o = getXMLObjectContainer().get(name);
        if(null != o){
            try {
                Map st = o.getDescStructure();
                if (null != st) {
                    HashMap ret = new HashMap();
                    ret.put("SCENE",st.get("scene"));
                    ret.put("EXAMPLE",st.get("example"));
                    ret.put("ORIGINAL",st.get("original"));
                    if(ret.size()>0)
                        return ret;
                }
            }catch (Exception e){}
        }
        return null;
    }
    //get test invoke parameter
    Map getTestParameter(String name){
        XMLObject o = getXMLObjectContainer().get(name);
        if(null != o){
            try {
                Object r = o.getInvokeDescStructure().get("input");
                if(null != r && r instanceof Map) {
                    return (Map) r;
                }else{
                    return null;
                }
            }catch (Exception e){}
        }
        return null;
    }

    boolean checkDesc(Map desc)throws Exception{
        if(desc.containsKey("opType") && null!= desc.get("opType") && ((String)desc.get("opType")).startsWith("INS-")){
            throw new Exception("the service desc opType is not must start with [INS-]");
        }
        return true;
    }
    /**
     * 1. need to instance the new service and add to container
     * 2. need save the service description to file
     * 3. update out service , ext: webService class
     */
    boolean addService(Map allDesc,XMLParameter parameter)throws Exception{
        try {
            checkDesc(allDesc);
            String s = (String)allDesc.get("body");
            s  = StringUtils.replace(s,"\\n","");
            allDesc.put("body",s);
            if(StringUtils.isBlank((String)allDesc.get("author")) && null !=parameter && parameter instanceof RequestParameters){
                if(null != ((RequestParameters)parameter).getSession()) {
                    String author = ((RequestParameters) parameter).getSession().getUserName();
                    if(StringUtils.isNotBlank(author)) {
                        allDesc.put("author", author);
                    }
                }
            }
            if(null != allDesc.get("name") && allDesc.get("name") instanceof String) {
                if(null == getObjectById((String)allDesc.get("name") )) {
                    Map data = Desc.removeNotServiceProperty(allDesc);
                    boolean isa = false;//isPublishActive((String)data.get("opType"),(String)data.get("package"),(String)data.get("name"));
                    createXMLObjectByDesc(data, this.getClass().getClassLoader(), this, isa,getSingleContainers());
                    return true;
                }else{
                    throw new Exception("the service ["+allDesc.get("name") +"] has exist, can not new create.");
                }
            }
        }catch (Exception e){
            log.error("addService error:",e);
            throw e;
        }
        return false;
    }
    boolean deleteService(String name)throws Exception{
        try {
            removeObject(name);
            return true;
        }catch (Exception e){
            log.error("remove Service error:",e);
            throw e;
        }
    }
    boolean updateService(Map allDesc)throws Exception{
        try {
            if(log.isInfoEnabled()){
                log.info("update service "+allDesc);
            }
            checkDesc(allDesc);
            String s = (String)allDesc.get("body");
            s = StringUtils.replace(s,"\\n","");
            allDesc.put("body",s);
            Map data= Desc.removeNotServiceProperty(allDesc);
            if(null == getObjectById((String)data.get("name"))){
                createXMLObjectByDesc(data,this.getClass().getClassLoader(),this,false,getSingleContainers());
                return true;
            }else {
                return updateObjectByDesc(data);
            }
        }catch (Exception e){
            log.error("updateService error:",e);
            throw e;
        }
    }
    boolean addUpdateSerivce(Map desc,XMLParameter parameter)throws Exception{
        if(null == desc || !Desc.isDescriptionService(desc)){
            throw new Exception("not a service desc file , please check the file.");
        }
        if(StringUtils.isNotBlank((String) desc.get("name"))){
            XMLObject oo = getObjectById((String)desc.get("name"));
            String s = (String)desc.get("body");
            s = StringUtils.replace(s,"\\n","");
            desc.put("body",s);
            if(null !=oo ){
                updateService(desc);
            }else{
                addService(desc,parameter);
            }
            return true;
        }
        return false;
    }
    boolean suspendService(String name){
        XMLObject o = getXMLObjectContainer().get(name);
        if(null != o){
            try {
                boolean is= o.suspendObject();
                return is;
            }catch (Exception e){}
        }
        return false;
    }


    /**
     * 设置对象是否在异常经过时是否可以重做。
     * @param svName
     * @param isRedo
     * @return
     * @throws Exception
     */
    boolean setRedo(String svName,String isRedo)throws Exception{
        XMLObject o = getXMLObjectContainer().get(svName);
        if(StringUtils.isBlank(isRedo)){
            isRedo="true";
        }
        boolean ret = o.setProperty("redo",isRedo);
        return ret;
    }
    boolean publishSetRedo(String svName,String opType,String pack,String isRedo)throws Exception{
        Map m = new HashMap();
        if(StringUtils.isBlank(isRedo)){
            isRedo="true";
        }
        m.put("redo",isRedo);

        return true;
    }

    /**
     * if the service open for other user , will set share =true
     * @param svName
     * @param isShare
     * @return
     * @throws Exception
     */
    boolean setShare(String svName,String isShare)throws Exception{
        XMLObject o = getXMLObjectContainer().get(svName);
        if(StringUtils.isBlank(isShare)){
            isShare="true";
        }
        boolean ret = o.setProperty("share",isShare);
        return ret;
    }

    boolean isWorktime(Map desc){
        String b= (String)desc.get("body");
        if(StringUtils.isNotBlank(b)){
            if(b.contains("worktime") && b.contains("crons")){
                return true;
            }
        }
        return false;
    }
    boolean isAlarm(Map desc){
        String b = (String)desc.get("body");
        if(StringUtils.isNotBlank(b)){
            List s = StringUtils.getTagsNoMark(b,"alarm","}");
            if(null != s && s.size()>0 && StringUtils.isNotBlank(s.get(0))){
                if(((String)s.get(0)).contains("check")) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean activeService(String name){
        XMLObject o = getXMLObjectContainer().get(name);
        if(null != o){
            try {
                boolean is = o.activeObject();
                return is;

            } catch (Exception e) {
            }

        }
        return false;
    }

    boolean activeThisService(String localip,String name,String ip,String insid){
        Bridge b = (Bridge)getObjectById("bridge");
        if(localip.equals(ip) && b.getInstanceId().equals(insid)){
            return activeService(name);
        }
        return false;
    }
    String getSelfInstanceId(){
        Bridge b = (Bridge)getObjectById("bridge");
        return b.getInstanceId();
    }
    boolean suspendThisService(String localip,String name,String ip,String insid){
        Bridge b = (Bridge)getObjectById("bridge");
        if(localip.equals(ip) && b.getInstanceId().equals(insid)){
            return suspendService(name);
        }
        return false;
    }
    //get service status active or disactive in zk
    public static String getPublishStateZkPath(String zktype,String insid,String name)throws Exception{
        String r=zktype;
        /*if(StringUtils.isNotBlank(optype)){
            r+="/"+optype;
        }*/
        r+="/"+insid+INSID_SRVNAME_SPLITCHAR+name;
        /*boolean is=false;
        if(StringUtils.isNotBlank(pkg)){
            r+="_"+pkg;
            is=true;
        }
        if(is){
            r += "." + name;
        }else {
            r +=  name;
        }*/

        return r;
    }
    //get service path in zk
    String getZkPath(String zktype,String opType,String pk,String name)throws Exception{
        String r=zktype;
        if(StringUtils.isNotBlank(opType)){
            r+="/"+opType;
        }
        boolean is=false;
        if(StringUtils.isNotBlank(pk)){
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

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(null != input){
            String op = (String)input.get("op");
            String sleep=(String)input.get("sleep");
            if(StringUtils.isNotBlank(sleep)){
                Thread.sleep(Integer.parseInt(sleep)*1000);
            }
            if(StringUtils.isNotBlank(op)){
                if("exit".equals(op)){
                    System.exit(3);
                }else if("getActions".equals(op)){
                    //获取所有服务
                    return getAllServices();

                }else if("getServices".equals(op)){
                    //获取查询的服务列表，包括：服务名称、包路径、操作类型、存储路径

                    return findServicesByCenter((Map)config.get("othersInsServices"),(Map)config.get("serviceStatusMap"),env,(String) input.get("name"));

                }else if("getServiceInfoList".equals(op)){
                    return getServiceListFromZk();
                }else if("getTreeServices".equals(op)){
                    List<Map> ls = findServicesByCenter((Map)config.get("othersInsServices"),(Map)config.get("serviceStatusMap"),env,null);
                    return getTreeServices(ls);
                }else if("getServiceParameters".equals(op)){
                    //获取一个服务的参数，包括：入参，配置参数，出参
                    return getServiceParameters((String)input.get("name"));
                }else if("getServiceDescription".equals(op)){
                    //获取服务简要描述，报文体，异常，依赖等信息
                    return getServiceDescription((String) input.get("name"));
                }else if("getServiceBody".equals(op)){
                    return getServiceBody((String) input.get("name"));
                }else if("getServiceUsage".equals(op)){
                    //获取使用手册，使用场景，这个文件会很大单独存储
                    return getServiceUsage((String) input.get("name"));
                }else if("getServiceOutput".equals(op)){
                    return getServiceOutput((String)input.get("name"));
                }else if("getTestParameter".equals(op)){
                    //获取测试报文
                    return getTestParameter((String) input.get("name"));


                }else if("addService".equals(op)){//add service description
                    //增加服务
                    Map desc = (Map)input.get("data");
                    boolean b = addService(desc,env);
                    //notifyByAddSrv((String)desc.get("name"),null);
                    //addSrvInfoToCacheByAddSrv(env,desc);

                    return b;
                }else if("copyService".equals(op)){//add service description
                    //copy服务
                    String name = (String)input.get("name");
                    String nname = (String)input.get("newName");
                    XMLObject o = getObjectById(name);
                    if(null ==o)throw new Exception("not find object by id ["+name+"]");
                    Map pros = (Map)input.get("pros");
                    if(StringUtils.isNotBlank(name)){
                        Map nmap= copyDesc(name,nname,pros);
                        boolean b = addService(nmap,env);
                        //notifyByAddSrv(name,null);
                        //addSrvInfoToCacheByAddSrv(env,nmap);
                        return b;
                    }

                }else if("setObjectProperties".equals(op)){
                    String name = (String)input.get("name");
                    Map pros = (Map)input.get("pros");
                    if(StringUtils.isNotBlank(name)){
                        XMLObject o = getObjectById(name);
                        if(null !=o){
                            o.setObjectProperties(pros);
                        }else{
                            throw new Exception("not find action ["+name+"]");
                        }
                    }
                }else if("removeObjectProperties".equals(op)){
                    String name = (String)input.get("name");
                    Object ps = input.get("paths");
                    if(StringUtils.isNotBlank(name)){
                        XMLObject o = getObjectById(name);
                        if(null !=o){
                            return o.removeObjectProperties(ps);
                        }
                    }
                    return false;
                }else if("deleteService".equals(op)){
                    //删除服务
                    //notifyByRemoveSrv((String) input.get("name"),null);
                    return deleteService((String) input.get("name"));
                }else if("updateService".equals(op)){
                    //修改服务
                    Map m = (Map)input.get("data");
                    m.put("date",DateTimeUtil.getCurrDateTime());
                    return addUpdateSerivce(m,env);
                }else if("addUpdateService".equals(op)){
                    addUpdateSerivce((Map)input.get("data"),env);

                }else if("suspendService".equals(op)){
                    //暂停服务
                    //notifyBySuspendSrv((String) input.get("name"),null);
                    return suspendService((String) input.get("name"));
                }else if("activeService".equals(op)) {
                    //激活服务
                    //notifyByActiveSrv((String) input.get("name"),null);
                    return activeService((String) input.get("name"));
                }else if("activeThisService".equals(op)){
                    //notifyByActiveSrv((String) input.get("name"),new String[]{(String) input.get("insid")});
                    return activeThisService(((String)((Map)env.get("${env}")).get("${ip}")),(String) input.get("name"),(String) input.get("ip"),(String) input.get("insid"));
                }else if("suspendThisService".equals(op)) {
                    notifyBySuspendSrv((String) input.get("name"),(String) input.get("insid"));
                    return suspendThisService(((String)((Map)env.get("${env}")).get("${ip}")),(String) input.get("name"), (String) input.get("ip"), (String) input.get("insid"));
                }else if("publishAddUpdateService".equals(op)){
                    //集群服务的话，只用使用这个方法发布发布就可以
                    return null;//publishAddUpdateService((String)input.get("insIds"),(Map)input);
                }else if("publishService".equals(op)){
                    //集群服务的话，只用使用这个方法发布发布就可以
                    String srvName = (String)input.get("name");
                    if(StringUtils.isNotBlank(srvName)){

                    }
                    return null;//publishAddUpdateService(ids,input);
                }else if("publishDeleteService".equals(op)){
                    //集群服务的话，只用使用这个方法发布发布就可以
                    return null;//publishDeleteService((String) input.get("name"));
                }else if("publishSuspendService".equals(op)){
                    //集群服务的话，只用使用这个方法发布发布就可以
                    return null;//publishSuspendService((String) input.get("name"));
                }else if("publishActiveService".equals(op)){
                    //集群服务的话，只用使用这个方法发布发布就可以
                    return null;//publishActiveService((String) input.get("name"));
                }else if("publishSuspendOneService".equals(op)){
                    //集群服务的话，只用使用这个方法发布发布就可以
                    return null;//publishSuspendOneService((String) input.get("ip"), (String) input.get("insid"),(String)input.get("opType"),(String)input.get("package"), (String) input.get("name"));
                }else if("publishActiveOneService".equals(op)){
                    //集群服务的话，只用使用这个方法发布发布就可以
                    return null;//publishActiveOneService((String) input.get("ip"), (String) input.get("insid"), (String)input.get("opType"),(String)input.get("package"),(String) input.get("name"));
                }

                else if("getUpdateDesc".equals(op)){
                    return getDescStructure((String) input.get("name"));
                }else if("setRedo".equals(op)){
                    if(null != input.get("redo")){
                        if(input.get("redo") instanceof String){
                            return setRedo((String) input.get("name"), (String) input.get("redo"));
                        }
                        if(input.get("redo") instanceof Map){
                            return setRedo((String) input.get("name"), ObjectUtils.convertMap2String((Map) input.get("redo")));
                        }
                    }

                }else if("publishSetRedo".equals(op)){
                    if(null != input.get("redo")){
                        if(input.get("redo") instanceof String){
                            return publishSetRedo((String) input.get("name"),(String) input.get("opType"),(String) input.get("package"), (String) input.get("redo"));
                        }
                        if(input.get("redo") instanceof Map){
                            return publishSetRedo((String) input.get("name"),(String) input.get("opType"),(String) input.get("package"), ObjectUtils.convertMap2String((Map) input.get("redo")));
                        }
                    }
                }else if("setShare".equals(op)){
                    return setShare((String) input.get("name"), (String) input.get("share"));
                }else if("publishSetShare".equals(op)){
                    return publishSetRedo((String) input.get("name"), (String) input.get("opType"), (String) input.get("package"), (String) input.get("share"));
                }else if("getNewDesc".equals(op)){
                    return Desc.getEnptyDescStructure();
                }else if("publishStartTrace".equals(op)){
                    return null;//startTrace();
                }else if("publishStopTrace".equals(op)){
                    return null;//stopTrace();
                }else if("getTraceList".equals(op)){
                    HashMap map = new HashMap();
                    map.put("op","getTraceList");
                    map.put("data",input.get("data"));
                    return tracelog.doSomeThing(null,null,map,null,null);
                }else if("getTrace".equals(op)){
                    HashMap map = new HashMap();
                    map.put("op","getTrace");
                    map.put("data",input.get("data"));
                    return tracelog.doSomeThing(null,null,map,null,null);
                }else if("removePath".equals(op)){
                    //set INS_STATUS
                    HashMap map = new HashMap();
                    String p = (String)input.get("path");
                    log.info("remove path:"+p);

                }else if("saveUploadFile".equals(op)){
                    String s = (String)input.get("savepath");
                    String fs = null;
                    if(StringUtils.isNotBlank(s) && env instanceof RequestParameters){
                        Map m = (Map)((Map)((RequestParameters)env).getRequestData()).get("files");
                        if(null != m){
                            Iterator<String> its = m.keySet().iterator();
                            while(its.hasNext()){
                                String name = its.next();
                                InputStream in=null;
                                if(m.get(name) instanceof InputStream) {
                                    in = (InputStream) m.get(name);
                                }else if(m.get(name) instanceof String){
                                    in = ObjectUtils.convertBase64String2InputStream((String)m.get(name));
                                }
                                FileUtils.saveFile(s+"/"+name,in);
                                fs = s+"/"+name;
                            }
                        }
                    }
                    return fs;
                }else if("addServiceByDesc".equals(op)){
                    Object m = input.get("desc");
                    if(null != m){
                        if(m instanceof String){
                            m = StringUtils.convert2MapJSONObject((String)m);
                        }
                        addUpdateSerivce((Map)m,env);
                    }
                }else if("isExistService".equals(op)){
                    String s = (String)input.get("name");
                    if(StringUtils.isNotBlank(s)){
                        return getObjectById(s)==null?false:true;
                    }
                }else if("addServiceByFlowDesc".equals(op)){
                    Map m = (Map)input.get("flowdesc");
                    String type = (String)input.get("flowType");

                    Map desc = Desc.convertFlowStructure2DescMap(m,type,this);
                    if (null != m) {
                        addUpdateSerivce(desc, env);
                    }

                    return true;
                }else if("isFlowSV".equals(op)){
                    String m = (String)input.get("name");
                    if("flow".equals(getObjectById(m).getDescStructure().get("createby"))){
                        return true;
                    }else{
                        return false;
                    }

                }else if("getFlowDescByDesc".equals(op)){
                    String name = (String)input.get("name");
                    XMLObject o = getObjectById(name);
                    if(null != o){
                        Map m = o.getDescStructure();
                        return Desc.convertDescMap2FlowStructure(m);
                    }

                }else if("addPath".equals(op)){
                    //set INS_STATUS
                    HashMap map = new HashMap();
                    String p = (String)input.get("path");
                    log.info("will update path srv to RUNNING:" + p);



                }else if("getServicesFileRel".equals(op)){
                    List<String> fs = (List)input.get("data");
                    if(null != fs){
                        Map<String,List<String>> map = new HashMap();
                        for(String s:fs){
                            Map<String,XMLObject> all = getXMLObjectContainer();
                            Iterator<String> its = all.keySet().iterator();
                            while(its.hasNext()){
                                XMLObject o = all.get(its.next());
                                Map desc = o.getDescStructure();
                                if(null != desc){
                                    Map org = (Map)desc.get("original");
                                    if(null != org){
                                        if(s.equals(org.get("src"))){
                                            if(!map.containsKey(s)){
                                                map.put(s,new ArrayList());
                                            }
                                            map.get(s).add(o.getXML().getId());
                                        }
                                    }
                                }
                            }

                        }
                        return map;

                    }
                }else if("getAddressByInstanceName".equals(op)){
                    String name = (String)input.get("name");
                    String instanceid = (String)input.get("instanceid");
                    String port = ObjectUtils.getStringValueByPath(env,"${requestProperties}.ServerPort");
                    if(StringUtils.isBlank(port)){
                        Map m = (Map)env.get("${env}");
                        if(null != m && instanceid.equals(m.get("${local_instanceId}"))) {
                            port = (String) m.get("webport");
                        }

                    }
                    Map ins = (Map)((Map)config.get("instanceMap")).get(instanceid);
                    String ret= getAddress(ins,instanceid, port, name);
                    if(log.isDebugEnabled()){
                        log.debug("get remote instance address "+ret+" by instance id "+instanceid+" input "+input);
                    }
                    return ret;
                }else if("getInsList".equals(op)){
                    Map instanceMap = (Map)config.get("instanceMap");
                    if(null != instanceMap) {
                        return new ArrayList(instanceMap.values());
                    }else{
                        return null;
                    }
                }else if("getOtherInsList".equals(op)){
                    Map instanceMap = (Map)config.get("instanceMap");
                    if(null != instanceMap) {
                        List<Map> ms = new ArrayList(instanceMap.values());
                        if (null != ms) {
                            for (Map m : ms) {
                                if (m.get("insId").equals(getSelfInstanceId())) {
                                    ms.remove(m);
                                    break;
                                }
                            }
                        }
                        return ms;
                    }else{
                        return null;
                    }
                }else if("getInsListBySrvId".equals(op)){
                    Map<String,List<Map>> maping = (Map)config.get("othersInsServices");
                    String srv = (String)input.get("srvId");
                    List limitIn = (List)input.get("limitIn");
                    if(StringUtils.isNotBlank(srv)){
                        List<Map> ret= maping.get(srv);
                        if(null != limitIn && limitIn.size()>0){
                            ret = ArrayUtils.innerList(ret,limitIn);
                        }
                        if(log.isInfoEnabled()){
                            log.info("find instance list by "+srv+"["+ret+"]");
                        }
                        return ret;
                    }
                }else if("getInstanceInfo".equals(op)){
                    String instanceid = (String)input.get("instanceid");
                    if(StringUtils.isNotBlank(instanceid)){
                        return ((Map)config.get("instanceMap")).get(instanceid);
                        //return getInstanceInfo(instanceid);
                    }
                }else if("getErrorMessage".equals(op)){
                    int lines = 200;
                    if(StringUtils.isNotBlank(input.get("lineNum"))) {
                        if(StringUtils.isNumeric((String)input.get("lineNum"))) {
                            lines = Integer.parseInt((String) input.get("lineNum"));
                        }
                    }
                    return getErrorMessage((Map)config.get("instanceMap"),(String)input.get("srvid"),(String)((Map)env.get("${env}")).get("logUserName"),(String)((Map)env.get("${env}")).get("logPassword"),(String)input.get("date"),lines);
                }else if("init".equals(op)){
                    log.info("reInit system");
                    doInitial();
                }else if("getDesc".equals(op)){
                    String srvName = (String)input.get("name");
                    Map ret=null;
                    if(StringUtils.isNotBlank(srvName)){
                        ret= getDescStructure(srvName);
                    }
                    if(log.isDebugEnabled())
                        log.debug("get desc "+srvName+" desc:"+ret);
                    return ret;
                }else if("getRemoteDesc".equals(op)){
                    String name = (String)input.get("name");
                    if(StringUtils.isNotBlank(name)){
                        return getRemoteDesc(name);
                    }else{
                        return null;
                    }
                }else if("getDescInAllIns".equals(op)){
                    String srvName = (String)input.get("name");
                    Object ret= getDescStructure(srvName);
                    if(null == ret){
                        ret = getRemoteDesc(srvName);
                    }
                    return ret;
                }else if("getActionPackage".equals(op)){
                    String srvName = (String)input.get("name");
                    if(StringUtils.isNotBlank(srvName)){
                        Map m= getDescStructure(srvName);
                        if(null != m){
                            return m.get("package");
                        }
                        return null;

                    }
                }else if("eventListener".equals(op)){
                    String path = (String)input.get("path");
                    String srvid = path.substring(path.lastIndexOf(".")+1);
                    String insid = null;

                    Object data = input.get("data");
                    if(StringUtils.isNotBlank(path) && null != data){

                    }
                }else if("getEnv".equals(op)){
                    if(null != getPropertyObject("env")) {
                        Map ret = new LinkedHashMap();
                        XMLMakeup xml = ((XMLObject) getPropertyObject("env")).getXML();
                        List<XMLMakeup> ps = xml.getChildren();
                        if (null != ps) {
                            for (XMLMakeup p : ps) {
                                String k = (String) p.getProperties().get("key");
                                String v = (String) p.getText();
                                ret.put(k, v);
                            }
                        }
                        return ret;
                    }
                    return null;
                }else if("isredo".equals(op)){
                    String name = (String)input.get("name");
                    if(StringUtils.isNotBlank(name)){
                        XMLObject x = getObjectById(name);
                        if(null != x){
                            return x.isRedo();
                        }
                    }
                    return false;
                }else if("getHelpMenus".equalsIgnoreCase(op)){
                    String helppath = (String)((Map)env.get("${env}")).get("ServicesDir");
                    if(null != resource && StringUtils.isNotBlank(helppath)){
                        Map map = new HashMap();
                        map.put("url",helppath+"../menus.json");
                        map.put("op","getTextContent");
                        String content = (String)resource.doSomeThing(null,null,map,null,null);
                        if(StringUtils.isNotBlank(content)){
                            return content;
                        }
                    }
                }else if("getAlarmActions".equals(op)){
                    Map map = getXMLObjectContainer();
                    Iterator its = map.keySet().iterator();
                    List list = new ArrayList();
                    list.add("OtherErrorLogAlarm");// it is fix alarm action for ELK collected log files
                    while(its.hasNext()){
                        String name = (String)its.next();
                        Object o = map.get(name);
                        if(o instanceof XMLDoObject){
                            if(((XMLDoObject)o).isAlarm()){
                                list.add(name);
                            }
                        }

                    }
                    if(list.size()>0) {
                        return list;
                    }

                }else if("getSrvInfoBySrvIdFromCenter".equals(op)){
                    String id = (String)input.get("srvId");
                    if(StringUtils.isNotBlank(id)){
                        return srvInfoInCenter.get(id);
                    }
                }else if("getServicesDescJar".equals(op)){
                    List<String> ids = (List)input.get("srvIds");
                    String jarName = (String)input.get("jarName");
                    String savepath = (String)input.get("savePath");
                    Map<String,String> descs = new HashMap();
                    getAllChildrenDescStringList(env,ids,descs,new ArrayList<String>());
                    getServicesDescJar(env,descs,jarName,savepath);
                }else if("getLibJar".equals(op)){
                    String lib = (String)input.get("libpath");
                    List<String> descjars = (List)input.get("descjars");
                    String jarname = (String)input.get("jarname");
                    String temp = (String)input.get("temppath");
                    List<String> excludes= (List)input.get("excludes");
                    combineJars(null,lib,excludes,jarname,temp,descjars,null);
                    return jarname;
                }else if("generatorApplication".equals(op)){
                    List<String> srvNames = (List)input.get("services");
                    String newJarName = (String)input.get("newJarName");
                    String appName = (String)input.get("appName");
                    String webname = (String)input.get("webName");
                    List excludes = (List)input.get("excludes");
                    List descjars = (List)input.get("descjars");
                    String acctCode=(String)((Map)env.get("${session}")).get("UserName");

                    String buildroot =(String)((Map)env.get("${env}")).get("buildDir");
                    if(StringUtils.isNotBlank(buildroot)) {
                        if (!(buildroot.endsWith("/") || buildroot.endsWith("\\"))) {
                            buildroot = buildroot + "/";
                        }
                        Map<String, Map<String, String>> pars = (Map) input.get("parameters");
                        return generatorApplication(newJarName,env, srvNames,excludes, appName, acctCode,webname, buildroot, pars,descjars);
                    }else{
                        throw new ISPException("SYSTEM.NOT_CONFIG_BUILD_PATH","please config [buildDir] property in main file");
                    }
                }else if("generatorApplications".equals(op)){
                    if(null != input.get("AppInfo") && input.get("AppInfo") instanceof Map) {
                        Map appInfo = (Map) input.get("AppInfo");
                        List<Map> pty3=null;
                        if(null != input.get("3Pty3") && input.get("3Pty3") instanceof List)
                            pty3 = (List) input.get("3Pty3");
                        List<Map> tables = null;
                        if(null != input.get("DataObject") && input.get("DataObject") instanceof List)
                            tables = (List) input.get("DataObject");
                        List<Map> dbaccts = null;
                        if(null != input.get("DBAcct") && input.get("DBAcct") instanceof List)
                            dbaccts=(List) input.get("DBAcct");
                        List<Map> deployIns = null;
                        if(null != input.get("DeployIns") && input.get("DeployIns") instanceof List)
                            deployIns=(List) input.get("DeployIns");
                        List<Map> deployReq = null;
                        if(null != input.get("DeployReq") && input.get("DeployReq") instanceof List)
                            deployReq=(List) input.get("DeployReq");
                        List<Map> deployTools = null;
                        if(null != input.get("DeployTools") && input.get("DeployTools") instanceof List)
                            deployTools=(List) input.get("DeployTools");
                        List<Map> webs = null;
                        if(null != input.get("Webs") && input.get("Webs") instanceof List)
                            webs = (List) input.get("Webs");
                        List<Map> component = null;
                        if(null != input.get("Component") && input.get("Component") instanceof List)
                            component=(List) input.get("Component");
                        List<Map> adminUser = null;
                        if(null !=input.get("AdminUser") && input.get("AdminUser") instanceof List)
                            adminUser=(List) input.get("AdminUser");
                        List<Map> contexts = null;
                        if(null !=input.get("Context") && input.get("Context") instanceof List)
                            contexts=(List) input.get("Context");
                        List<Map> pars = null;
                        if(null != input.get("parameters") && input.get("parameters") instanceof List)
                            pars=(List) input.get("parameters");
                        List configFiles = null;
                        if(null !=input.get("ConfigFiles") && input.get("ConfigFiles") instanceof List)
                            configFiles=(List) input.get("ConfigFiles");
                        List<Map> services = null;
                        if(null != input.get("Services") && input.get("Services") instanceof List)
                            services=(List) input.get("Services");
                        Map svnSrvs = null;
                        if(null != input.get("SVNServices") && input.get("SVNServices") instanceof Map)
                            svnSrvs=(Map) input.get("SVNServices");

                        List<Map> srvs = new ArrayList();
                        if(null != services && null !=svnSrvs) {
                            for (Map t : services) {
                                if (!svnSrvs.containsKey(t.get("SRV_NAME"))) {
                                    srvs.add(t);
                                }
                            }
                        }
                        String userCode = (String) ((Map) env.get("${session}")).get("UserName");


                        String buildRoot = (String) ((Map) env.get("${env}")).get("buildUsrDir");
                        if (StringUtils.isNotBlank(buildRoot)) {
                            buildRoot = buildRoot.replaceAll("\\\\","/");
                            /*if (!(buildRoot.endsWith("/") || buildRoot.endsWith("\\"))) {
                                buildRoot = buildRoot + "/";
                            }*/

                            generatorApplication2(buildRoot, userCode, env, appInfo, pty3, contexts, tables, dbaccts, deployIns, deployReq, deployTools, pars
                                    , srvs, svnSrvs, webs, component, adminUser, configFiles);
                        } else {
                            throw new ISPException("SYSTEM.NOT_CONFIG_BUILD_PATH", "please config [buildUsrDir] property in main file");
                        }
                    }else{
                        throw new ISPException("GENERATOR_NOT_APP","Can not find app information ");
                    }
                }else if("getWebNames".equals(op)){
                    String buildroot =(String)((Map)env.get("${env}")).get("buildDir");
                    if(StringUtils.isNotBlank(buildroot)) {
                        if (!(buildroot.endsWith("/") || buildroot.endsWith("\\"))) {
                            buildroot = buildroot + "/";
                        }
                        return getWebNames(env, buildroot);
                    }else{
                        throw new ISPException("SYSTEM.NOT_CONFIG_BUILD_PATH","please config [buildDir] property in main file");
                    }
                }else if("getServicesIntroduction".equals(op)){ //only for the input services exclude children services
                    List<String> names = (List)input.get("names");
                    if(null != names){
                        return getServicesIntroduction(env,names);
                    }
                    return null;
                }else if("getServiceAllAuthors".equals(op)){
                    String name = (String)input.get("name");
                    if(StringUtils.isNotBlank(name)) {
                        return getServiceAuthors(env, name);
                    }
                }

            }

        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
    List getWebNames(XMLParameter env,String buildroot){
        if(StringUtils.isNotBlank(buildroot)) {
            String web = buildroot + "user/webs";
            File f = new File(web);
            if(f.exists()) {
                File[] fs = f.listFiles();
                if(null != fs){
                    List<String> ret = new ArrayList();
                    for(File fi:fs){
                        ret.add(fi.getName());
                    }
                    return ret;
                }
            }
        }
        return null;
    }
    List getServicesIntroduction(XMLParameter env,List<String> names) throws Exception {
        List ret =new ArrayList();
        for(String s:names){
            String desc = getServiceDescString(s,env);
            if(null != desc) {
                Map d = StringUtils.convert2MapJSONObject(desc);
                if(null != d){
                    HashMap i = new HashMap();
                    i.put("name",d.get("name"));
                    i.put("package",d.get("package"));
                    i.put("input",d.get("input"));
                    i.put("output",d.get("output"));
                    i.put("date",d.get("date"));
                    i.put("desc",d.get("desc"));
                    i.put("error",d.get("error"));
                    i.put("depend",d.get("depend"));
                    i.put("scene",d.get("scene"));
                    i.put("example",d.get("example"));
                    i.put("busiArchitecture",d.get("busiArchitecture"));
                    i.put("techArchitecture",d.get("techArchitecture"));
                    ret.add(i);
                }
            }
        }
        return ret;
    }
    Object generatorApplication2(String buildroot,String userCode,XMLParameter env,Map appInfo,List<Map> pty3,List<Map> contexts,List<Map> tables,List<Map> dbaccts
            ,List<Map> deployIns,List<Map> deployReq,List<Map> deployTools
            ,List<Map> pars,List<Map> srvs,Map<String,String> svnSrvs,List<Map> webs,List<Map> component,List<Map> adminUser,List<String> configFiles) throws Exception {
        if(null != appInfo) {
            String appName = (String) appInfo.get("APP_NAME");
            if (StringUtils.isNotBlank(appName)) {
                //build path
                String buildpath = buildroot +"/" + userCode + "/" + appName;

                for(Map insInfo :deployIns) {
                    boolean isConsole=false;
                    if(StringUtils.isNotBlank(insInfo.get("INSTANCE_NAME"))) {
                        if (((String) insInfo.get("INSTANCE_NAME")).equalsIgnoreCase("CONSOLE")) {
                            isConsole = true;
                        }
                        Map parameters = getParameters(appName, (String) insInfo.get("INSTANCE_NAME"), pars);
                        String targetName = buildpath + "/dist/" + appName + "_" + insInfo.get("INSTANCE_NAME")+"_"+insInfo.get("INS_TYPE")+".zip";

                        generatorIns(targetName, isConsole, env, appName, buildroot, buildpath, configFiles, pty3, contexts, tables, dbaccts
                                , insInfo, deployReq, deployTools, parameters, component, adminUser, srvs, svnSrvs);
                    }
                }
                File f = new File(buildpath+"/dist");
                File[] fs = f.listFiles();
                String[] tas = new String[fs.length];
                for(int i=0;i<fs.length;i++){
                    tas[i]=fs[i].getPath();
                }
                ZipUtil.zipFiles(buildpath+"/"+appName+".zip",tas);//(,new String[]{buildpath+"/dist"});

                cleanDirs(new String[]{buildpath+"/components",buildpath+"/dist",buildpath+"/descs",buildpath+"/bin",buildpath+"/lib",buildpath+"/classes",buildpath+"/data",buildpath+"/logs",buildpath+"/web",buildpath+"/doc"});

            }
        }
        return null;
    }
    Map getParameters(String appName,String instanceName,List<Map> pars){
        Map ret = new HashMap();
        if(null != pars){
            for(Map m:pars){
                if(((String)m.get("INSTANCE_NAME")).equalsIgnoreCase(instanceName)){
                    String f = (String)m.get("FILE_NAME");
                    if(null != f){
                        Object seq = m.get("REQ_ID");
                        if(null !=seq && seq instanceof Integer){
                            if(!ret.containsKey(f)) ret.put(f,new ArrayList());
                            for(int i=0;i<(Integer)seq;i++){
                                if(((ArrayList)ret.get(f)).size()<=i){
                                    ((ArrayList)ret.get(f)).add(new HashMap());
                                }
                            }
                            //if(null != ((ArrayList)ret.get(f)).get((Integer)seq-1)) ((ArrayList)ret.get(f)).add((Integer)seq-1,new HashMap());
                            ((Map)((ArrayList)ret.get(f)).get((Integer)seq-1)).put(m.get("SRC_PAR"),m.get("TARGET_PAR"));
                        }else{
                            if(!ret.containsKey(f)) ret.put(f,new HashMap());
                            ((Map)ret.get(f)).put(m.get("SRC_PAR"),m.get("TARGET_PAR"));
                        }
                    }

                }
            }
        }
        if(null != ret && ret.size()>0)
            return ret;
        else
            return null;
    }
    Map getInsInfo(String appName,String name,List<Map> deployIns){
        if(null !=deployIns){
            for(Map m:deployIns){
                if(((String)m.get("APP_NAME")).equalsIgnoreCase(appName) && ((String)m.get("INSTANCE_NAME")).equalsIgnoreCase(name)){
                    return m;
                }
            }
        }
        return null;
    }
    void prepareEnv(String buildroot,String buildpath,Map insInfo,Map<String,Object> parameters,List<String> configFiles){

        chgFiles(buildroot+"/define",buildpath,parameters,configFiles);
        //chgFiles(buildroot+"/define",buildpath,parameters,configFiles);
        //chgFiles(buildroot+"/define",buildpath,parameters,configFiles);
        //chgFiles(buildroot+"/define",buildpath,parameters,configFiles);
        ////chgFiles(buildroot+"/define/web",buildpath+"/web",parameters,configFiles);
        //chgFiles(buildroot+"/define",buildpath,parameters,configFiles);
    }

    void generatorIns(String targetFileName,boolean isConsole,XMLParameter env,String appName,String buildRoot,String buildpath
            ,List<String> consoleConfigFiles,List<Map> pty3,List<Map> context,List<Map> tables,List<Map> dbaccts,Map insInfo,List<Map> deployReq
            ,List<Map> deployTools,Map<String,Object> pars,List<Map> component,List<Map> adminUser,List<Map> srvs,Map<String,String> svnSrvs){
        if(null != pty3){
            //copy 3pty examples zookeeper, hbase
            for(Map pty:pty3){
                String name = (String)pty.get("3PTY_NAME");
                if(StringUtils.isNotBlank(name)){
                    File f = new File(buildRoot+"/define/components/"+name);
                    if(f.exists()){
                        try {
                            //FileUtils.copyFile(f, new File(buildpath + "/components/" + name));
                            ZipUtil.unZipFile(buildRoot+"/define/components/"+name,buildpath + "/components/" );
                        }catch (Exception e){
                            log.error("unzip file error "+buildRoot+"/define/components/"+name,e);
                        }
                    }
                }
            }
        }
        //generator context file and add start app file
        if(null != context){
            generatorContextFile(buildpath+"/classes/funs/contexts.xml",context);
            Object o = pars.get("classes/tb_web.app");
            if(null != o && o instanceof Map){
                ((Map) o).put("<!-- contexts -->","<contexts xml=\"classpath:funs/contexts.xml\"/>");
            }
        }
        //change defines file to specific instance
        prepareEnv(buildRoot,buildpath,insInfo,pars,consoleConfigFiles);
        try {
            FileUtils.copyFile(buildRoot + "/define/lib/treasurebag_lib.jar", buildpath + "/lib/treasurebag_lib.jar");
            FileUtils.copyFile(buildRoot + "/define/lib/treasurebag_services.jar", buildpath + "/lib/treasurebag_services.jar");
        }catch (Exception e){

        }
        //generator tableContainer.xml
        if(null != tables){
            updateTables(buildpath+"/classes/funs/tablecontainer.xml",tables,adminUser);
        }
        //generator dataclient.xml
        if(null != dbaccts){
            updateDatabase(buildpath+"/classes/funs/dataclient.xml",dbaccts,"CONSOLE");
        }
        //package deploy tools
        if(null != deployTools){

        }
        //console functions
        if(null != component){

        }

        try {
            if(isConsole) {
                File tar = new File(buildpath + "/web");
                if(!tar.exists()) tar.mkdirs();
                FileUtils.copyDict(new File(buildRoot + "/define/webs/console"),tar,null,null,null,null);
            }else{
                if("BALANCE".equalsIgnoreCase((String)insInfo.get("INS_TYPE"))){
                    File tar = new File(buildpath + "/web");
                    if(!tar.exists()) tar.mkdirs();
                    FileUtils.copyDict(new File(buildRoot + "/define/webs/"+insInfo.get("INSTANCE_NAME")),tar,null,null,null,null);
                    //generator server version jar from isp_user_app_services
                }else
                    generatorServiceJar(buildpath+"/lib/",appName+"_services.jar",env,srvs,svnSrvs);
            }
        }catch (Exception e){
            log.error("copy web error ",e);
        }
        if(isConsole)
            ZipUtil.zipFiles(targetFileName,new String[]{buildpath+"/lib",buildpath+"/bin",buildpath+"/classes",buildpath+"/data",buildpath+"/logs"
                    ,buildpath+"/web",buildpath+"/components",buildpath+"/doc"});
        else
            ZipUtil.zipFiles(targetFileName,new String[]{buildpath+"/lib",buildpath+"/bin",buildpath+"/classes",buildpath+"/data",buildpath+"/logs"
                    ,buildpath+"/web",buildpath+"/doc"});
        cleanDirs(new String[]{buildpath+"/bin",buildpath+"/classes",buildpath+"/components",buildpath+"/lib",buildpath+"/data",buildpath+"/logs",buildpath+"/web",buildpath+"/doc"});
    }

    void updateDatabase(String saveFile,List<Map> accs,String insName){
        if (StringUtils.isNotBlank(saveFile) && null != accs){
            FileInputStream fi=null;
            try {
                TableContainer tb = (TableContainer)getObjectById("tablecontainer");
                if(null != tb) {
                    File tf = new File(saveFile);
                    fi = new FileInputStream(tf);
                    HashMap m = new HashMap();

                    StringBuffer sb = new StringBuffer();
                    for(Map a :accs) {
                        if(a.get("INSTANCE_NAME").equals(insName)) {
                            Object hostip = m.get("HOST_IP");
                            Object port = m.get("HOST_PORT");
                            Object userName = m.get("USER_NAME");
                            Object userPwd = m.get("USER_PWD");
                            sb.append("<datasource key=\"" + a.get("SOURCE_NAME") + "\" isenable=\"true\" xmlid=\"dbsource\">\n" +
                                    "            <property name=\"driverClassName\" value=\"com.mysql.jdbc.Driver\" />\n" +
                                    "            <property name=\"url\" value=\"jdbc:mysql://" + hostip + ":" + port + "/mysql?rewriteBatchedStatements=true&amp;cachePrepStmts=true&amp;useServerPrepStmts=true&amp;useUnicode=true&amp;characterEncoding=UTF-8&amp;autoReconnect=true&amp;failOverReadOnly=false\" />\n" +
                                    "            <property name=\"username\" value=\"" + userName + "\" />\n" +
                                    "            <property name=\"password\" value=\"" + userPwd + "\" />\n" +
                                    "            <property name=\"initialSize\" value=\"0\" />\n" +
                                    "            <property name=\"maxActive\" value=\"20\" />\n" +
                                    "            <property name=\"maxIdle\" value=\"20\" />\n" +
                                    "            <property name=\"testWhileIdle\" value=\"true\" />\n" +
                                    "            <property name=\"validationQuery\" value=\"SELECT COUNT(*) FROM DUAL\" />\n" +
                                    "            <property name=\"timeBetweenEvictionRunsMillis\" value=\"60000\" />\n" +
                                    "            <property name=\"minEvictableIdleTimeMillis\" value=\"10000\" />\n" +
                                    "            <property name=\"removeAbandoned\" value=\"false\" />\n" +
                                    "        </datasource>\n");
                        }
                    }
                    m.put("<!-- datasource -->",sb.toString());

                    byte[] bs = FileUtils.replaceFile(fi, m, "UTF-8");
                    FileUtils.saveStringBufferFile(new StringBuffer(new String(bs)), saveFile, false);
                }
            }catch (Exception e){

            }finally {
                try {
                    if (null != fi) fi.close();
                }catch (Exception e){}
            }
        }
    }
    void updateTables(String saveFile,List<Map> tables,List<Map> adminUser) {
        if (StringUtils.isNotBlank(saveFile) && null != tables){
            FileInputStream fi=null;
            try {
                TableContainer tb = (TableContainer)getObjectById("tablecontainer");
                if(null != tb) {
                    File tf = new File(saveFile);
                    fi = new FileInputStream(tf);


                    Map m = new HashMap();
                    StringBuffer fields = new StringBuffer();
                    StringBuffer tabs = new StringBuffer();
                    StringBuffer route = new StringBuffer();
                    StringBuffer sequeces = new StringBuffer();
                    StringBuffer users = new StringBuffer();
                    List<String> tempFields =new ArrayList();
                    for (Map t : tables) {
                        //<property IS_CACHE="0" TABLE_NAME="ISP_STATIC_DATA" FIELD_CODE="EXTERN_CODE_TYPE" USED_TYPES="Q" NOT_NULL="0" TABLE_NUM="3"/>
                        String tab = (String) t.get("TABLE_NAME");
                        TableBean tbean = tb.getAllTables().get(tab);
                        if (null != tbean) {
                            List<TableField> tfs = tbean.getTableFields();
                            if (null != tfs) {
                                for (TableField f : tfs) {
                                    tabs.append("<property IS_CACHE=\"" + (f.isCache() ? 1 : 0) + "\" TABLE_NAME=\"" + tab + "\" FIELD_CODE=\"" + f.getField().getFieldCode() + "\" USED_TYPES=\"" + f.getUsedTypes().get(0) + "\" NOT_NULL=\"" + (f.isNotNull() ? 1 : 0) + "\" TABLE_NUM=\"" + f.getTableNum() + "\"/>\n");
                                    if (!tempFields.contains(f.getField().getFieldCode())) {
                                        fields.append("<property FIELD_NAME=\"" + f.getField().getFieldName() + "\" FIELD_CODE=\"" + f.getField().getFieldCode() + "\" FIELD_TYPE=\"" + f.getField().getFieldType() + "\" FIELD_LEN=\"" + f.getField().getFieldLen() + "\" FIELD_NUM=\"" + f.getField().getFieldNum() + "\" STATE=\"1\"/>\n");
                                        tempFields.add(f.getField().getFieldCode());
                                    }
                                }
                            }
                        }
                        String source = (String) t.get("SOURCE_NAME");
                        String spe = (String) t.get("SPLIT_EXPRESS");
                        String routee = (String) t.get("ROUTE_EXPRESS");
                        String range = (String) t.get("SPLIT_RANGE");
                        if (StringUtils.isNotBlank(spe) || StringUtils.isNotBlank(routee) || StringUtils.isNotBlank(range)) {
                            route.append("<property ROUTER_ID=\"\" DATA_SOURCE=\""+source+"\" TABLE_NAME=\""+tab+"\" SPLIT_EXPRESS=\""+spe+"\" ROUTE_EXPRESS=\""+routee+"\" SPLIT_RANGE=\"" + range + "\"/>\n");
                        }
                        sequeces.append("<property SEQUENCE_NAME=\""+tab+"$SEQ\" INCREMENT_BY=\"1\" LAST_NUMBER=\"10000\"/>\n");
                    }
                    if(null != adminUser){
                        for(Map u:adminUser) {
                            String i18n="";
                            Map n = new HashMap();
                            chgI18nKey(u,n);
                            if(null != n) {
                                i18n = ObjectUtils.convertMap2String(n);
                            }
                            users.append("<property USER_CODE=\"" + u.get("USER_CODE") + "\" USER_NAME=\""+u.get("USER_NAME")+"\" LOGIN_ACCT=\""+u.get("LOGIN_ACCT")+"\" LOGIN_PWD=\""+u.get("LOGIN_PWD")+"\" I18N=\""+i18n+"\" TENANT_CODE=\""+u.get("TENANT_CODE")+"\" PHONE=\"\" MAIL=\"\" USER_TYPE=\"ADMIN\" STATUS=\"1\"/>\n");
                        }
                    }
                    m.put("<!-- FIELDS-->", fields.toString());
                    m.put("<!-- TABLES -->", tabs.toString());
                    m.put("<!-- ROUTER -->", route.toString());
                    //<property SEQUENCE_NAME="ISP_USER_APP_PAYMENT$SEQ" INCREMENT_BY="1" LAST_NUMBER="10000"/>
                    m.put("<!-- SEQUENCES -->", sequeces.toString());
                    m.put("<!-- USERS -->", users.toString());
                    byte[] bs = FileUtils.replaceFile(fi, m, "UTF-8");
                    FileUtils.saveStringBufferFile(new StringBuffer(new String(bs)), saveFile, false);
                }
            }catch (Exception e){
                log.error("update tablecontainer.xml error",e);
            }finally {
                try {
                    if (null != fi) fi.close();
                }catch (Exception e){}
            }
        }
    }
    void chgI18nKey(Map u,Map n){
        String language = (String)u.get("LANGUAGE");
        if(StringUtils.isNotBlank(language))
            n.put("language",language);
        String country = (String)u.get("COUNTRY");
        if(StringUtils.isNotBlank(country))
            n.put("country",country);
        String dateformat = (String)u.get("DATE_FORMAT");
        if(StringUtils.isNotBlank(dateformat))
            n.put("date",dateformat);
        String timezone = (String)u.get("TIME_ZONE");
        if(StringUtils.isNotBlank(timezone))
            n.put("zone",timezone);
        String currency = (String)u.get("CURRENCY");
        if(StringUtils.isNotBlank(currency))
            n.put("currency",currency);
    }
    void generatorContextFile(String saveFile,List<Map> context) {
        if (StringUtils.isNotBlank(saveFile) && null != context){
            StringBuffer sb = new StringBuffer();
            sb.append("<contexts clazz=\"com.octopus.isp.ds.Contexts\">\n");
            for (Map m : context) {
                if(StringUtils.isTrue((String)m.get("IS_DEFAULT").toString())) {
                    sb.append("<context default=\"true\">\n");
                }else{
                    sb.append("<context>\n");
                }
                HashMap u = new HashMap();
                chgI18nKey(m,u);
                Iterator its = u.keySet().iterator();
                while(its.hasNext()){
                    String k = (String)its.next();
                    Object o = u.get(k);
                    if(null !=k && null != o && StringUtils.isNotBlank(o))
                        sb.append("<property key=\""+k+"\">"+o+"</property>\n");
                }
                sb.append("</context>\n");
            }
            sb.append("<i18n ref=\"i18n\"/>\n");
            sb.append("</contexts>");
            try {
                FileUtils.saveFile(sb, saveFile, false, false);
            }catch(Exception e){
                log.error("generator context error:",e);
            }
        }
    }

    void cleanDirs(String[] dirs){
        if(null != dirs){
            for(String s:dirs){
                FileUtils.deleteDir(s);
            }
        }
    }
    void chgFiles(String srcDir,String targetDir,Map<String,Object> parameters,List<String> configFiles){
        if(null != configFiles){
            for(String fp:configFiles){
                File f = new File(srcDir+"/"+fp);
                if(f.exists()){
                    try {
                        if(f.isDirectory()){

                            FileUtils.copyCurFloderFiles(srcDir + "/" + fp, targetDir + "/" + fp, null, "");

                        }else {
                            FileUtils.copyFile(f, new File(targetDir + "/" + fp));
                        }
                    }catch (Exception e){
                        log.error("copy dir error:",e);
                    }
                }
            }
        }
        if(null != parameters) {
            Iterator its = parameters.keySet().iterator();
            while(its.hasNext()) {
                String k = (String)its.next();
                String fn = srcDir+"/"+k;
                File ff = new File(fn);
                FileInputStream fi=null;
                if(ff.exists()) {
                    try {
                        Object obj = parameters.get(k);
                        if(obj instanceof Map) {
                            fi = new FileInputStream(ff);
                            String tf = targetDir+"/"+k;
                            byte[] bs = FileUtils.replaceFile(fi, (Map)obj, "UTF-8");
                            FileUtils.saveStringBufferFile(new StringBuffer(new String(bs)), tf, false);
                            fi.close();
                        }else if(obj instanceof List){
                            int c=0;
                            for(Object m :(List)obj){
                                if(m instanceof Map){
                                    fi = new FileInputStream(ff);
                                    String n = k.substring(0,k.lastIndexOf("."));
                                    String e = k.substring(k.lastIndexOf("."));
                                    String tf = targetDir+"/"+n+"_"+(c++)+e;
                                    byte[] bs = FileUtils.replaceFile(fi, (Map)m, "UTF-8");
                                    FileUtils.saveStringBufferFile(new StringBuffer(new String(bs)), tf, false);
                                    fi.close();
                                }
                            }
                        }

                    }catch (Exception e){
                        log.error("chgFiles ",e);
                    }finally {
                        try {
                            if (null != fi) fi.close();
                        }catch (Exception e){}
                    }
                }
            }
        }
    }

    /**
     *
     * @param savepath
     * @param targetjarName
     * @param env
     * @param srvs
     * @param svnSrvs include children action
     * @throws Exception
     */
    void generatorServiceJar(String savepath,String targetjarName,XMLParameter env,List<Map> srvs,Map<String,String>svnSrvs) throws Exception {
        Map<String,String> descs = new HashMap();
        List<String> filterOutNotSvnSrvNames = new ArrayList();
        if(null !=srvs){
            for(Map m:srvs){
                if(StringUtils.isNotBlank(m.get("SRV_NAME"))) {
                    if(!(null != svnSrvs && svnSrvs.containsKey(m.get("SRV_NAME")))){
                        filterOutNotSvnSrvNames.add((String) m.get("SRV_NAME"));
                    }
                }
            }
        }
        //service name list not in svn, these is latest version
        getAllChildrenDescStringList(env,filterOutNotSvnSrvNames,descs,new ArrayList());
        //add svn service to descs
        if(null != descs){
            Iterator its = descs.keySet().iterator();
            while(its.hasNext()){
                String k = (String)its.next();
                String desc = descs.get(k);
                descs.put(k,desc);
            }
        }
        //generator app services desc jar
        getServicesDescJar(env, descs, targetjarName,savepath);

    }
    Object generatorApplication(String newJarName,XMLParameter env,List<String> srvNames,List<String> excludejars,String appName,String acctCode
            ,String webname,String approot,Map<String,Map<String,String>> parameters,List<String> descjars)throws Exception{
        //service jar
        log.info("root path:"+approot);
        String web=null;
        if(StringUtils.isNotBlank(webname)) {
            web = approot + "user/webs/"+webname;
        }
        String path = approot+"user/"+acctCode+"/"+appName;
        log.info("user path:"+path);
        new File(path+"/lib/").mkdirs();
        //get desc list
        Map<String,String> descs = new HashMap();
        getAllChildrenDescStringList(env,srvNames,descs,new ArrayList());
        //generator desc jar
        getServicesDescJar(env, descs, appName + "_services.jar", path + "/lib/");
        log.info("finished services jar:" + path + "/lib/" + appName + "_services.jar");
        //copy
        new File(path+"/bin").mkdirs();
        new File(path+"/classes").mkdirs();
        new File(path+"/data").mkdirs();
        new File(path+"/logs").mkdirs();
        new File(path+"/web").mkdirs();
        new File(path+"/doc").mkdirs();
        FileUtils.copyDict(new File(approot+"user/define/bin"),new File(path+"/bin"),null,null,null,null);
        FileUtils.copyDict(new File(approot+"user/define/classes"),new File(path+"/classes"),null,null,null,null);
        FileUtils.copyDict(new File(approot+"user/define/data"),new File(path+"/data"),null,null,null,null);
        FileUtils.copyDict(new File(approot+"user/define/logs"),new File(path+"/logs"),null,null,null,null);
        FileUtils.copyDict(new File(approot+"user/define/doc"),new File(path+"/doc"),null,null,null,null);
        //generator tablecontainer.xml

        generatorTableContainer(env,descs, path + "/classes/funs/tablecontainer.xml");
        //get authors
        Map authors = getServiceCreateUsers(env,descs);
        if(null != authors){
            FileUtils.saveFile(new StringBuffer(ObjectUtils.convertMap2String(authors)),path+"/doc/Authors.txt",true,false);
        }
        //get services introduction
        List ls = getServicesIntroduction(env,srvNames);
        if(null != ls){
            FileUtils.saveFile(new StringBuffer(ObjectUtils.convertList2String(ls)),path+"/doc/ServicesIntroduction.txt",true,false);
        }
        //lib jar
        String lib = approot+"../lib";
        log.info("lib path:"+lib);
        String jarname = path+"/lib/"+newJarName+".jar";
        String temp = path+"/temp";
        FileUtils.deleteDir(temp);
        File f = new File(temp);
        f.mkdirs();
        File lf = new File(lib);
        if(lf.exists()) {
            File[] fs = lf.listFiles();
            if(null != fs ) {
                if(fs.length==1){
                    FileUtils.copyFile(fs[0],new File(jarname));
                }else {
                    combineJars(approot, lib, excludejars, jarname, temp, descjars, parameters);
                }
            }
        }
        //remove excludejars from zip/lib
        /*if(null != excludejars) {
            for(String e:excludejars) {
                FileUtils.copyFile(lib+"/"+e,path+"/lib/"+e);
            }
        }*/
        log.info("finished lib jar:"+jarname);
        FileUtils.deleteDir(temp);

        if(StringUtils.isNotBlank(web)){
            File webfile = new File(web);
            if(webfile.exists()){
                FileUtils.copyDict(webfile, new File(path + "/web"), null, null, null, null);
            }else {
                FileUtils.copyDict(new File(approot + "user/define/web"), new File(path + "/web"), null, null, null, null);
            }
        }else{
            FileUtils.copyDict(new File(approot + "user/define/web"), new File(path + "/web"), null, null, null, null);
        }
        log.info("finished copy config files.");
        if(new File(path+"/web/WEB-INF/web.xml").exists()){
            Map mt = new HashMap();
            mt.put("tb_web.app",appName+".app");
            parameters.put("web.xml",mt);
        }
        log.info("finished change web.xml start config files.");
        //parameters
        if(null != parameters) {
            Iterator its = parameters.keySet().iterator();
            while(its.hasNext()) {
                String k = (String)its.next();
                String fn = path+"/"+k;
                File ff = new File(fn);
                if(ff.exists()) {
                    FileInputStream fi = new FileInputStream(ff);
                    byte[] bs = FileUtils.replaceFile(fi, parameters.get(k), "UTF-8");
                    FileUtils.saveStringBufferFile(new StringBuffer(new String(bs)), fn, false);
                    fi.close();
                }
            }
        }
        log.info("finished parameters changed");
        new File(path+"/classes/tb_web.app").renameTo(new File(path+"/classes/"+appName+".app"));
        log.info("finished change start file name");
        //zip
        String zipfile=path+"/"+appName+".zip";
        log.info("ziping file name:"+zipfile);
        File wf = new File(path+"/web");
        String isweb = "false";
        if(wf.exists()){
            File[] l = wf.listFiles();
            if(null != l && l.length>0){
                isweb="true";
            }
        }
        ZipUtil.zipFiles(zipfile,new String[]{path+"/bin",path+"/doc",path+"/classes",path+"/data",path+"/logs",path+"/web",path+"/lib"});
        log.info("finished zip file:"+zipfile);
        FileUtils.deleteDir(path+"/bin");
        FileUtils.deleteDir(path+"/classes");
        FileUtils.deleteDir(path+"/data");
        FileUtils.deleteDir(path+"/logs");
        FileUtils.deleteDir(path+"/doc");
        FileUtils.deleteDir(path+"/web");
        FileUtils.deleteDir(path+"/lib");
        long size = new File(zipfile).length();
        HashMap m = new HashMap();
        m.put("path",zipfile);
        m.put("size",size);
        m.put("isweb",isweb);
        m.put("count",descs.size());
        File logof = new File(path+"/upload/logo.png");
        if(logof.exists()){
            m.put("logo_path",path+"/upload/logo.png");
        }
        String s= ArrayUtils.toDistinctString(authors.values());
        if(null !=s) {
            if(s.length()>1024) {
                m.put("authors", s.substring(0, 1024));
            }else{
                m.put("authors", s);
            }
        }
        return m;

    }

    void combineJars(String rootpath,String lib,List extjars,String jarnamepath,String temppath,List<String> descjars,Map<String,Map<String,String>> parameters)throws Exception{
        List<String> ls =FileUtils.getAllFileNames(lib,"jar");
        ArrayUtils.sortByLen(ls,ArrayUtils.ABS);
        List<String> es = new LinkedList<String>();
        if(null != ls && ls.size()>0) {
            for(int i=ls.size()-1;i>=0;i--) {
                if(null != descjars && ArrayUtils.isLikeArrayInString(ls.get(i), descjars)){
                    es.add(ls.get(i));
                    continue;
                }
                String n = ls.get(i).contains("/")?ls.get(i).substring(ls.get(i).lastIndexOf("/")+1):ls.get(i).substring(ls.get(i).lastIndexOf("\\")+1);
                if(null != extjars && ArrayUtils.isInStringArray(extjars,n))
                    continue;
                try {
                    ZipUtil.unZipFile(ls.get(i), temppath);
                }catch(Exception e){
                    log.error("unable to unzip file:"+ls.get(i),e);
                    throw e;
                }
                log.info("unzip jar:"+ls.get(i)+ " to temp:"+temppath);
            }

            if(null != descjars) {
                for(int j=descjars.size()-1;j>=0;j--) {
                    for (int i = es.size() - 1; i >= 0; i--) {
                        if(es.get(i).indexOf(descjars.get(j))>=0) {
                            String n = es.get(i).contains("/") ? es.get(i).substring(es.get(i).lastIndexOf("/") + 1) : es.get(i).substring(es.get(i).lastIndexOf("\\") + 1);
                            if (null != extjars && ArrayUtils.isInStringArray(extjars, n))
                                continue;
                            try {
                                ZipUtil.unZipFile(es.get(i), temppath);
                            } catch (Exception e) {
                                log.error("unable to unzip file:" + es.get(i), e);
                                throw e;
                            }
                            log.info("unzip jar:" + es.get(i) + " to temp:" + temppath);
                            es.remove(i);
                        }
                    }
                }
            }
        }
        if(null != parameters){
            Collection s = parameters.values();
            if(null != s){
                Iterator<Map<String,String>> its = s.iterator();
                while(its.hasNext()){
                    Map<String,String> ts = its.next();
                    Iterator<String> ti = ts.keySet().iterator();
                    String sv=null;
                    while(ti.hasNext()){
                        String k= ti.next();
                        String v= ts.get(k);
                        if(null != k && k.equals("<property key=\"ServerHost\"></property>") && StringUtils.isNotBlank(v)){
                            List l = StringUtils.getTagsNoMark(v,">","<");
                            if(null != l && l.size()>0) {
                                String host = (String)l.get(0);
                                if(StringUtils.isNotBlank(host)) {
                                    GeneratorClass.appendMethodInClassPath(temppath, false, "com.octopus.utils.xml.XMLObject", new String[]{"getXMLInstance"}, "if(!\"" + host + "\".equals(com.octopus.utils.net.NetUtils.getip())){System.out.println(\"the server not match the ip machine! please check the server ip[" + host + "]\");System.exit(1);}", null, null, temppath);
                                    sv = host;
                                }
                            }
                        }
                        if(null != sv && null != k && k.equals("<property key=\"ServerHost.ServerCount\"></property>") && StringUtils.isNotBlank(v)){
                            List l = StringUtils.getTagsNoMark(v,">","<");
                            if(null != l && l.size()>0) {
                                String count = (String)l.get(0);
                                if(StringUtils.isNotBlank(count)) {
                                    GeneratorClass.appendMethodInClassPath(temppath, false, "com.octopus.tools.pubsub.ZkClientListen", new String[]{"connzk"}, null, null, "if(getServerCount(\"" + sv + "\")>" + count + "){System.out.println(\"Over the max server count[" + count + "] in this Host [" + sv + "]\");System.exit(1);}", temppath);
                                }
                            }
                        }
                    }
                }
            }
        }
        //over META-INF
        if(StringUtils.isNotBlank(rootpath)) {
            FileUtils.copyDict(new File(rootpath + "user/define/META-INF"), new File(temppath+"/META-INF"), null, null, null, null);
        }
        ZipUtil.jarFolder(temppath,jarnamepath);
    }
    //get desc String content
    String getServiceDescString(String n,XMLParameter env)throws Exception{
        String d=null;
        if (null != getObjectById(n)) {
            Map m = getDescStructure(n);
            d = ObjectUtils.convertMap2String(m);
        } else {
            String[] insids = getInsNameBySrvId(n);
            if (null != insids && insids.length > 0) {
                String ins = insids[0];
                if (StringUtils.isNotBlank(ins)) {
                    XMLDoObject re = (XMLDoObject) getObjectById("restfulClient");
                    HashMap in = new HashMap();
                    in.put("insid", ins);
                    in.put("srvid", "getDesc");
                    in.put("data", "{name:'" + n + "'}");
                    re.doCheckThing(null, env, in, null, null, null);
                    Object o = env.getResult();
                    if (null != o) {
                        if(((ResultCheck) o).getRet() instanceof String) {
                            d = (String) ((ResultCheck) o).getRet();
                        }
                        if(((ResultCheck) o).getRet() instanceof Map){
                            d = ObjectUtils.convertMap2String((Map)((ResultCheck) o).getRet());
                        }
                    }
                }
            }
        }
        return  d;
    }



    /**
     * get desc file zip to jar outStream
     * @return
     * @throws Exception
     */
    ByteArrayOutputStream getServicesJarOutputStream(Map<String,String> descs)throws Exception{
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        JarOutputStream jout = new JarOutputStream(out);
        if(null != descs) {
            Iterator<String> ist = descs.keySet().iterator();
            while (ist.hasNext()) {
                String n = ist.next();
                String d = descs.get(n);
                if (null != d) {
                    ByteArrayInputStream in = new ByteArrayInputStream(d.getBytes());
                    ZipUtil.addFile(jout, n + ".desc", in);
                    in.close();
                }
            }

            jout.close();
            return out;
        }
        return null;
    }
    void getServicesDescJar(XMLParameter env,Map<String,String> descs,String jarName,String savepath)throws Exception{
        ByteArrayOutputStream out = getServicesJarOutputStream(descs);
        if(null != out){
            if (StringUtils.isBlank(savepath)) {
                HttpServletResponse response = (HttpServletResponse) env.get("${response}");
                if (null != response) {
                    response.setContentType("application/octet-stream;charset=UTF-8");
                    response.setHeader("Content-disposition", "attachment; filename=" + jarName.toString());
                    //写入
                    response.getOutputStream().write(out.toByteArray());
                    //关闭
                    response.flushBuffer();
                    env.put("^isstop", "true");
                }
            } else {
                FileOutputStream o = new FileOutputStream(savepath + "/" + jarName);
                o.write(out.toByteArray());
                o.close();
            }
        }
    }

    void generatorTableContainer(XMLParameter env,Map<String,String> descs,String fileName) throws Exception {
        if(null != descs){
            Iterator<String> its = descs.keySet().iterator();
            List<String>ts = new ArrayList();
            while(its.hasNext()){
                String c= descs.get(its.next());
                getTablesFromDesc(c, ts);
            }


            XMLDoObject tc = (XMLDoObject)getObjectById("tablecontainer");
            if(null != tc) {
                HashMap input = new HashMap();
                input.put("op", "generatorTableContainer");
                input.put("tables", ts);
                StringBuffer sb = (StringBuffer) tc.doSomeThing(null, env, input, null, null);
                if (null != sb) {
                    FileUtils.makeFilePath(fileName);
                    FileUtils.saveFile(sb, fileName, true, false);
                }
            }

        }
    }
    Map<String,String> getServiceAuthors(XMLParameter env,String name) throws Exception {
        String s = getServiceDescString(name,env);
        if(StringUtils.isNotBlank(s)) {
            HashMap tm = new HashMap();
            tm.put(name,s);
            return getServiceCreateUsers(env,tm);
        }
        return null;
    }
    //get user in desc return {servicename:'',user:''}
    Map<String,String> getServiceCreateUsers(XMLParameter env,Map<String,String> servicesDesc){
        if(null != servicesDesc) {
            Map ret = new HashMap();
            Iterator<String> its = servicesDesc.keySet().iterator();
            while(its.hasNext()){
                String k = its.next();
                String s =servicesDesc.get(k);
                if(StringUtils.isNotBlank(s)){
                    try {
                        Map t = StringUtils.convert2MapJSONObject(s);
                        if(null != t) {
                            if(StringUtils.isNotBlank(t.get("author"))){
                                ret.put(k,t.get("author"));
                            }
                        }
                    }catch (Exception e){

                    }
                }
            }
            return ret;
        }
        return null;
    }

    //get desc content by name
    void getAllChildrenDescStringList(XMLParameter env,List<String> ids,Map<String,String> rtn,List<String> temp)throws Exception{
        if(null != ids) {
            for (String n : ids) {
                if(!temp.contains(n)) {
                    String d = getServiceDescString(n, env);
                    if (StringUtils.isNotBlank(d)) {
                        rtn.put(n, d);
                        temp.add(n);
                        List<String> srs = StringUtils.getTagsNoMark(d, " action=\\\"", "\\\"");
                        getAllChildrenDescStringList(env,srs,rtn,temp);
                    }
                }
            }
        }
    }

    //get table names from desc
    void getTablesFromDesc(String desContent,List targetNames){
        XMLDoObject tc = (XMLDoObject)getObjectById("tablecontainer");
        if(tc instanceof TableContainer){
            Set tables = ((TableContainer)tc).getAllTables().keySet();
            List<String> ls = StringUtils.getTagsNoMark(desContent,"table:'","'");
            if(null != ls){
                for(String s:ls){
                    String us = s.toUpperCase();
                    if(tables.contains(us) && !targetNames.contains(us)){
                        targetNames.add(us);
                    }
                }
            }
            List<String> qs = StringUtils.getTagsNoMark(desContent,"sqls:[","]");
            if(null != ls){
                for(String s:qs){
                    s = s.toUpperCase();
                    Iterator<String> ta = tables.iterator();
                    while(ta.hasNext()){
                        String t =  ta.next();
                        if(s.indexOf(t)>0){
                            if (!targetNames.contains(s)) {
                                targetNames.add(s);
                            }
                        }
                    }

                }
            }

        }
    }

    String getErrorMessage(Map instanceMap , String srvid,String username,String pwd,String yyyymmddhh,int linenum){
        if(linenum==0)linenum=200;
        if(StringUtils.isBlank(yyyymmddhh)) yyyymmddhh=DateTimeUtils.getStringDateByPattern("yyyyMMddhh",null);
        String f = "SRV_ERROR_LOG_"+srvid.trim()+"."+ yyyymmddhh;
        List<Map> ls = new ArrayList(instanceMap.values());
        if(log.isDebugEnabled()) {
            log.debug("getErrorMessage:" + ls);
        }
        if(null!=ls){
            StringBuffer sb = new StringBuffer();
            for(Map m:ls){

                String s = getErrorOneMessage((String)m.get("ip"),"22",username,pwd,(String)m.get("logPath")+"/"+f,linenum);
                if(StringUtils.isNotBlank(s)) {
                    sb.append((String)m.get("ip")).append("-").append(m.get("logPath")+"/"+f).append("\n").append(s).append("\n");
                }
            }
            return sb.toString();
        }else{
            try {
                return getErrorOneMessage("127.0.0.1", "22", "", "", ((Map) getEmptyParameter().get("${env}")).get("logPath") + "/" + f,linenum);
            }catch (Exception e){}
        }
        return null;
    }
    String getErrorOneMessage(String host,String port,String userName,String userPasswd,String logFilePath,int lineNum){
        if(null != command){
            try {
                HashMap map = new HashMap();
                map.put("type", "shell");
                map.put("host",host);
                map.put("port",port);
                map.put("username",userName);
                map.put("password",userPasswd);
                map.put("data", "tail -n  "+lineNum+" " + logFilePath);
                return (String) command.doSomeThing(null, null, map, null, null);
            }catch (Exception e){

            }

        }
        return null;
    }


    ConcurrentHashMap addressmap = new ConcurrentHashMap();
    String getAddressMapKey(String insid,String name){
        return insid+"_"+"_"+name;
    }
    String getCacheInsAddress(Map insinfo,String name){
        if(null != insinfo) {
            String insid = (String) insinfo.get("insId");
            if (log.isDebugEnabled()) {
                log.debug("get instance info by " + insid + " " + insinfo);
            }
            if (null != insinfo) {
                String ip = (String) insinfo.get("ip");
                String webport = (String) insinfo.get("port");

                if (StringUtils.isNotBlank(ip)) {
                    String ad = "http://" + ip + ":" + webport + "/" + "service?actions=" + name;
                    addressmap.put(getAddressMapKey(insid, name), ad);
                    if (log.isDebugEnabled()) {
                        log.debug(insid + " address " + ad);
                    }
                    return ad;
                }
            }
        }
        return null;
    }
    String getAddress(Map insinfo,String insid,String port,String name){
        if(addressmap.containsKey(getAddressMapKey(insid,name))){
            String ad = (String)addressmap.get(getAddressMapKey(insid,name));
            if(log.isDebugEnabled()) {
                log.debug("instance "+insid + " address " + ad);
            }
            return ad;
        }
        try {
            String ad =getCacheInsAddress(insinfo,name);
            if(StringUtils.isNotBlank(ad)){
                return ad;
            }

        }catch (Exception e){
            log.error("",e);
        }
        Bridge root = (Bridge)getObjectById("bridge");
        if(root.getInstanceId().equals(insid)) {
            String ad = "http://127.0.0.1:" + port + "/" + "service?actions=" + name;
            addressmap.put(getAddressMapKey(insid,name), ad);
            return ad;
        }else{
            return null;
        }
    }


    Map getRemoteDesc(String name){
        String[] insname = getInsNameBySrvId(name);
        if(null != insname && insname.length>0){
            return getRemoteDesc(name,insname[0]);
        }
        return null;
    }
    Map getRemoteDesc(String name,String insId){
        if(null != remote){
            HashMap in = new HashMap();
            in.put("name",name);
            Object o = doRemoteAction(insId,"getDesc",in);
            if(null != o){
                if(o instanceof Map)
                    return (Map)o;
                if(o instanceof String){
                    Map t = StringUtils.convert2MapJSONObject((String)o);
                    if(null != t){
                        return t;
                    }
                }
            }
        }
        return null;
    }

    Object doRemoteAction(String insId,String srvName,Map input){
        try {
            if(null !=insId) {
                BFParameters p = new BFParameters(false);
                p.addParameter("${targetNames}", new String[]{srvName});
                p.addParameter("${input_data}", input);
                p.addParameter("${insid}", insId);
                Hashtable hb = new Hashtable();
                hb.put("targetinsid", insId);
                p.addParameter("${requestHeaders}", hb);
                remote.doThing(p, null);
                Object o = p.getResult();
                if (null != o) {
                    if (o instanceof ResultCheck) {
                        o = ((ResultCheck) o).getRet();
                    }
                    return o;
                }
            }
        }catch (Exception e){
            log.error("invoke remote error", ExceptionUtil.getRootCase(e));
        }
        return null;
    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getLog(URL url){
        try{
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

        }catch (Exception e){

        }
        return null;
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
