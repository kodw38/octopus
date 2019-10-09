package com.octopus.tools.dataclient.v2.ds;


import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cls.POJOUtil;
import com.octopus.utils.ds.FieldBean;
import com.octopus.utils.ds.FieldType;
import com.octopus.utils.ds.TableBean;
import com.octopus.utils.ds.TableField;
import com.octopus.utils.file.FileUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.*;

/**
 * User: wf
 * Date: 2008-8-25
 * Time: 23:15:47
 */
public class TableContainer extends XMLDoObject {
    private static final Log log = LogFactory.getLog(TableContainer.class);
    
    private static final HashMap<String, com.octopus.utils.ds.TableBean> allTables = new HashMap();
    Map<String,List<TableRouterBean>> tableRouters =new HashMap<String, List<TableRouterBean>>();
    //  table      field  [reltable,relfield]
    Map<String,Map<String,List<String[]>>> tableRelation=new HashMap<String, Map<String, List<String[]>>>();
    // 分表的表
    protected List<String> splitTables = new ArrayList<String>();

    public List<String> getSplitTables() {
        return splitTables;
    }
    boolean exist(XMLDoObject dc,XMLMakeup xml,String tableName){
        /*try {
            HashMap cd = new HashMap();
            String ds = xml.getFirstCurChildKeyValue(tableName,"ds");
            if(StringUtils.isNotBlank(ds)){
                cd.put("ds",ds);
            }
            cd.put("op", "exist");
            cd.put("table", tableName);
            Object b = dc.doSomeThing(null, null, cd, null, null);
            if (null != b && b instanceof Boolean && !(Boolean) b) {
                log.error("table " + tableName + " is not existed");
                XMLMakeup[] ts = xml.getChild(tableName);
                if (null != ts && ts.length > 0) {
                    String structure = ts[0].getProperties().getProperty("structure");
                    //Map{field,notNull,fieldCode,fieldLen,usedTypes,DBFieldType
                    List<Map> str = StringUtils.convert2ListJSONObject(structure);

                    cd.put("op", "createTable");
                    cd.put("structure", str);

                    dc.doSomeThing(null, null, cd, null, null);
                    log.error(ds+" create table " + tableName + " successful");
                }
                return Boolean.FALSE;
            } else if ((null != b && b instanceof Boolean && (Boolean) b) || (b instanceof ResultCheck && ((ResultCheck) b).getRet() instanceof Boolean && (Boolean) ((ResultCheck) b).getRet())) {
                return Boolean.TRUE;
            } else {
                return Boolean.FALSE;
            }
        }catch (Exception e){
            log.error("",e);
            return Boolean.FALSE;
        }*/
        try {
            String ds = xml.getFirstCurChildKeyValue(tableName, "ds");
            XMLMakeup[] ts = xml.getChild(tableName);
            List<Map> str = null;
            if (null != ts && ts.length > 0) {
                String structure = ts[0].getProperties().getProperty("structure");
                //Map{field,notNull,fieldCode,fieldLen,usedTypes,DBFieldType
                str = StringUtils.convert2ListJSONObject(structure);
            }
            return exist(dc, ds, str, tableName);
        }catch (Exception e){
            log.error(e.getMessage(),e);
            return Boolean.FALSE;
        }
    }
    boolean exist(XMLDoObject dc,String ds,List structure,String tableName){
        try {
            HashMap cd = new HashMap();
            if(StringUtils.isNotBlank(ds)){
                cd.put("ds",ds);
            }
            cd.put("op", "exist");
            cd.put("table", tableName);
            Object b = dc.doSomeThing(null, null, cd, null, null);
            if (null != b && b instanceof Boolean && !(Boolean) b) {
                log.warn("table " + tableName + " is not existed");
                if(null != structure) {
                    cd.put("op", "createTable");
                    cd.put("structure", structure);

                    dc.doSomeThing(null, null, cd, null, null);
                    log.warn(ds + " create table " + tableName + " successful");
                }
                return Boolean.FALSE;
            } else if ((null != b && b instanceof Boolean && (Boolean) b) || (b instanceof ResultCheck && ((ResultCheck) b).getRet() instanceof Boolean && (Boolean) ((ResultCheck) b).getRet())) {
                return Boolean.TRUE;
            } else {
                return Boolean.FALSE;
            }
        }catch (Exception e){
            log.error(e.getMessage(),e);
            return Boolean.FALSE;
        }
    }
    /**
     * insert config property data to database
     * @param dc
     * @param xml
     * @param tableName
     */
    void initRecords(XMLDoObject dc,XMLMakeup xml,String tableName){
        XMLMakeup[] xs = xml.getChild("property");
        if(null != xs && xs.length>0){
            for(XMLMakeup x:xs) {
                insertRecord(dc, tableName, x.getProperties());
            }
        }
    }
    void insertRecord(XMLDoObject dc,String tableName,Properties pro){
        try {
            HashMap map = new HashMap();
            map.put("table", tableName);
            map.put("op","query");
            com.octopus.utils.ds.TableBean tb = allTables.get(tableName);
            List li=null;
            if(null != tb) {
                if(null != tb.getPkField()) {
                    String pk = tb.getPkField().getFieldCode();
                    if (StringUtils.isNotBlank(pk) && pro.containsKey(pk)) {
                        Map cd = new HashMap();
                        cd.put(pk, pro.get(pk));
                        map.put("conds", cd);
                        li = (List) dc.doSomeThing(null, null, map, null, null);
                    }
                }
            }
            if(!(null != li && li.size()>0)){
                map.put("op","add");
                map.remove("conds");
                map.put("datas",pro);
                dc.doSomeThing(null, null, map, null, null);
            }
        }catch (Exception e){
            log.error("init table container",e);
        }
    }
    List<Map> getDictionaryData(Map<String,String> map,XMLDoObject dc,XMLMakeup xml,String tableName){
        String f = (String)map.get(tableName);
        Map fm = StringUtils.convert2MapJSONObject(f) ;
        List avidlist = null;
        if(null != xml){
            String avid=xml.getFirstCurChildKeyValue(tableName,"avoid_repeat");
            if(StringUtils.isNotBlank(avid)){
                avidlist=StringUtils.convert2ListJSONObject(avid);
            }
        }
        List<Map> fd=null;
        try {
            Boolean b = exist(dc,xml,tableName);
            if(b) {
                Object o = dc.doSomeThing(null, null, fm, null, null);
                if (o instanceof ResultCheck) {
                    fd = (List<Map>) ((ResultCheck) o).getRet();
                } else {
                    fd = (List<Map>) o;
                }
            }
        }catch (Exception e){log.error("",e);}
        XMLMakeup[] ts = xml.getChild(tableName);
        if(null != ts && ts.length>0) {
            List<Map> tss = ts[0].getChildrenPropertiesByTag("property");
            if(null != tss && tss.size()>0){
                if(fd==null){
                    fd=tss;
                }else{
                    ObjectUtils.addAllMap(fd,tss,avidlist);
                }
            }
        }
        return fd;
    }


    public TableContainer(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    XMLDoObject getDC(Map cfg){
        XMLDoObject dc = (XMLDoObject)getObjectById((String)cfg.get("dataclient"));
        if(dc == null){
            XMLObject obj = getParent();
            if(obj instanceof XMLDoObject) {
                dc = (XMLDoObject)obj;
            }
        }
        return dc;
    }
    public void initial()throws Exception{
        Map cfg = StringUtils.convert2MapJSONObject(getXML().getProperties().getProperty("config"));
        List<com.octopus.utils.ds.FieldBean> fs=null;
        List<Map> td=null;
        List<Map> trel=null;

        Map map = getXML().getChildrenKeyValue("property");
        if(null == map){
            map = new HashMap();
            map.put("ISP_DICTIONARY_FIELD","{op:'query',table:'ISP_DICTIONARY_FIELD',fields:['FIELD_NAME','FIELD_CODE','FIELD_TYPE','FIELD_LEN','FIELD_NUM']}");
            map.put("ISP_DICTIONARY_TABLE","{op:'query',table:'ISP_DICTIONARY_TABLE',fields:['IS_CACHE','TABLE_NAME','FIELD_CODE','USED_TYPES','NOT_NULL','TABLE_NUM'],format:{order by:'TABLE_NAME'}}");
            //map.put("ISP_DICTIONARY_TABLE_APP","{op:'query',table:'ISP_DICTIONARY_TABLE_APP',fields:['APP_ID','TABLE_NAME','STORE_TYPE','STORE_PATH','STORE_SPLIT','CACHE_APP']}");
            map.put("ISP_DICTIONARY_TABLE_ROUTER","{op:'query',table:'ISP_DICTIONARY_TABLE_ROUTER',fields:['ROUTER_ID','DATA_SOURCE','TABLE_NAME','SPLIT_EXPRESS','ROUTE_EXPRESS']}");
            map.put("ISP_DICTIONARY_TABLE_REL","{op:'query',table:'ISP_DICTIONARY_TABLE_REL',fields:['TABLE_NAME','FIELD_CODE','TABLE_NAME_REL','FIELD_CODE_REL']}");
        }
        if(null != map){
            List<XMLMakeup> ls = getXML().getChildren();
            if(null != ls) {
                List<TableRouterBean> rb=null;
                XMLDoObject dc = getDC(cfg);
                for(XMLMakeup x:ls) {
                    if("ISP_DICTIONARY_FIELD".equals(x.getName())) {
                        List<Map> fd = getDictionaryData(map, dc, getXML(), x.getName());
                        fs = (List<com.octopus.utils.ds.FieldBean>) ObjectUtils.convertMapList2BeanList(fd, com.octopus.utils.ds.FieldBean.class);
                    }else if("ISP_DICTIONARY_TABLE".equals(x.getName())) {
                        td = getDictionaryData(map, dc, getXML(), x.getName());
                    }else if("ISP_DICTIONARY_TABLE_ROUTER".equals(x.getName())) {
                        List<Map> rd = getDictionaryData(map, dc, getXML(), "ISP_DICTIONARY_TABLE_ROUTER");
                        rb = ObjectUtils.convertMapList2BeanList(rd, TableRouterBean.class);
                    }else if("ISP_DICTIONARY_TABLE_REL".equals(x.getName())) {
                        trel = getDictionaryData(map, dc, getXML(), "ISP_DICTIONARY_TABLE_REL");
                    }else {
                        exist(dc, getXML(), x.getName());
                    }
                }
                if (null != rb) {
                    for (TableRouterBean r : rb) {
                        if (!tableRouters.containsKey(r.getTableName()))
                            tableRouters.put(r.getTableName(), new ArrayList<TableRouterBean>());
                        tableRouters.get(r.getTableName()).add(r);
                    }
                }
                createTableRelation(trel);
                if(log.isDebugEnabled()){
                    if(td.size()>0){
                        log.debug("load table count:"+td.size());
                    }else{
                        log.debug("load table count:0");
                    }
                }
                settle(fs, td);
                //insert init data
                for(XMLMakeup x:ls) {
                    if(ArrayUtils.isInStringArray(new String[]{"ISP_DICTIONARY_FIELD","ISP_DICTIONARY_TABLE","ISP_DICTIONARY_TABLE_ROUTER","ISP_DICTIONARY_TABLE_REL"},x.getName())){
                        continue;
                    }
                    initRecords(dc, x, x.getName());
                }
                //judge to create table
                Map ts = getAllTables();
                if(null != ts && StringUtils.isTrue((String)getEnvProperties().get("isCreateTableStart")) && isfirstCreate()){
                    Iterator its = ts.keySet().iterator();
                    while(its.hasNext()){
                        String t= (String)its.next();
                        if(StringUtils.isNotBlank(t)){
                             checkAndCreate(t);
                        }
                    }
                    logFirstCreate();
                }
            }
        }
    }
    //判断是否第一次执行创建表
    boolean isfirstCreate(){
        try {
            String log = (String) getEnvProperties().get("logDir");
            if(StringUtils.isNotBlank(log)){
                String c =FileUtils.getFileContentString(log.endsWith("/")?(log+"createtable.log"):(log+"/createtable.log"));
                if(StringUtils.isNotBlank(c) && c.trim().equals("1")){
                    return false;
                }
            }
            return true;
        }catch (Exception e){
            return true;
        }
    }
    //执行完,记录已经第一次执行过
    void logFirstCreate(){
        try {
            String log = (String) getEnvProperties().get("logDir");
            if (StringUtils.isNotBlank(log)) {
                FileUtils.saveFile(new StringBuffer("1"),log.endsWith("/")?(log+"createtable.log"):(log+"/createtable.log"),true,false);
            }
        }catch (Exception e){

        }
    }


    boolean checkAndCreate(String table) throws Exception {
        if(StringUtils.isTrue((String)getEnvProperties().get("isCreateTableStart"))) {
            Map cfg = StringUtils.convert2MapJSONObject(getXML().getProperties().getProperty("config"));
            XMLDoObject dc = getDC(cfg);
            com.octopus.utils.ds.TableBean tb = getAllTables().get(table);
            if (null != tb) {
                List<TableRouterBean> rs = getTableRouters().get(table);
                if (null != rs) {
                    for (TableRouterBean b : rs) {
                        if (null != b) {
                            exist(dc, b.getDataSource(), tb.getStructure(), table);
                        }
                    }
                } else {
                    exist(dc, null, tb.getStructure(), table);
                }
            }
            return true;
        }else{
            return false;
        }

    }

    @Override
    public void notifyObject(String op, Object obj) throws Exception {

    }

    @Override
    public void destroy() throws Exception {

    }

    void createTableRelation(List<Map> tablerel){
       if(null != tablerel){
           for(Map m:tablerel){
               addRelate(tableRelation,(String)m.get("TABLE_NAME"),(String)m.get("FIELD_CODE"),(String)m.get("TABLE_NAME_REL"),(String)m.get("FIELD_CODE_REL"));
               addRelate(tableRelation,(String)m.get("TABLE_NAME_REL"),(String)m.get("FIELD_CODE_REL"),(String)m.get("TABLE_NAME"),(String)m.get("FIELD_CODE"));
           }
       }
    }
    public String getPkField(String table){
        TableBean tb = allTables.get(table);
        if(null != tb) {
            FieldBean fb = tb.getPkField();
            if(null != fb) {
                return fb.getFieldCode();
            }
        }
        return null;
    }
    public String getTableNameByFields(String[] fields)throws Exception{
        Iterator<String> its= allTables.keySet().iterator();
        boolean ismatch=true;
        while(its.hasNext()){
            ismatch=true;
            String tableName = its.next();
            com.octopus.utils.ds.TableBean td = allTables.get(tableName);
            List<com.octopus.utils.ds.TableField> fds = td.getTableFields();

            for(String f:fields){
                if(!isIN(f,fds)){
                    ismatch=false;
                    break;
                }
            }

            if(ismatch)
                return tableName;
        }
        throw new Exception("not find table by fields:"+ ArrayUtils.toString(fields));
    }
    boolean isIN(String f,List<com.octopus.utils.ds.TableField> fs){
        for(com.octopus.utils.ds.TableField t:fs){
            try{
            if(t.getField().getFieldCode().equalsIgnoreCase(f))
                return true;
            }catch (Exception e){
                log.error("fields ["+fs+"] has null field");
                return false;
            }

        }
        return false;
    }
    void addRelate(Map<String,Map<String,List<String[]>>> outkeys,String k,String v,String rk,String rv){
        if(!outkeys.containsKey(k)) outkeys.put(k,new HashMap<String, List<String[]>>());
        if(!outkeys.get(k).containsKey(v)) outkeys.get(k).put(v,new ArrayList<String[]>());
        outkeys.get(k).get(v).add(new String[]{rk,rv});
    }

    void settle(List<com.octopus.utils.ds.FieldBean> fs,List<Map> td){
        if(null != td){
            try {
                Map<String, com.octopus.utils.ds.FieldBean> tem = new HashMap();
                for (com.octopus.utils.ds.FieldBean f : fs) {
                    try {
                        if (null != f.getFieldType()) {
                            f.setRealFieldType(com.octopus.utils.ds.FieldType.convertFrom(f.getFieldType()));
                            f.setDBFieldType(FieldType.convertDBFrom(f.getFieldType(), f.getFieldLen()));
                        }
                        tem.put(f.getFieldCode(), f);
                    }catch (Exception e){
                        log.error("cache load field",e);
                    }
                }

                for (Map t : td) {
                    try {

                        if (!allTables.containsKey(t.get("TABLE_NAME"))) {
                            com.octopus.utils.ds.TableBean b = new com.octopus.utils.ds.TableBean();
                            b.setTableName((String) t.get("TABLE_NAME"));
                            b.setTableFields(new LinkedList<com.octopus.utils.ds.TableField>());
                            if (log.isDebugEnabled()) {
                                log.debug("cache load table:" + b.getTableName());
                            }
                            allTables.put(b.getTableName(), b);
                        }

                        com.octopus.utils.ds.TableField tf = createTableField(t, tem);
                        if (null != tf) {
                            allTables.get(t.get("TABLE_NAME")).getTableFields().add(tf);
                        }
                    }catch (Exception e){
                        log.error("cache load table",e);
                    }
                }
            }catch (Exception e){
                log.error("load tables information",e);
            }
        }
    }
    com.octopus.utils.ds.TableField createTableField(Map t,Map<String, com.octopus.utils.ds.FieldBean> temf){
        com.octopus.utils.ds.TableField f = new TableField();
        if(t.containsKey("USED_TYPES") && null!= ((String)t.get("USED_TYPES")) && ((String)t.get("USED_TYPES")).contains(",")) {
            t.put("USED_TYPES", ((String) t.get("USED_TYPES")).split(","));
        }
        POJOUtil.setValues(t, f);
        FieldBean fb = temf.get(t.get("FIELD_CODE"));
        if(null != fb) {
            f.setField(fb);
            return f;
        }else{
            log.error("not find field ["+t.get("FIELD_CODE")+"]");
        }
        return null;
    }

    public Map<String, Map<String, List<String[]>>> getTableRelation() {
        return tableRelation;
    }

    public HashMap<String, com.octopus.utils.ds.TableBean> getAllTables() {
        return allTables;
    }

    public Map<String,List<TableRouterBean>> getTableRouters() {
        return tableRouters;
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
            String table = (String)input.get("table");
            if("delete".equals(op)){
                if("ISP_DICTIONARY_FIELD".equals(table)){
                    removeFieldFromCache(table, (Map) input.get("datas"));
                }else if("ISP_DICTIONARY_TABLE".equals(table)){
                    removeTableFromCache(table, (Map) input.get("datas"));
                }else if("ISP_DICTIONARY_TABLE_REL".equals(table)){
                    removeTableRelFromCache(table, (Map) input.get("datas"));
                }else if("ISP_DICTIONARY_TABLE_ROUTER".equals(table)){
                    removeRouterFromCache(table,(Map)input.get("datas"));
                }
            }else if("add".equals(op)){
                if("ISP_DICTIONARY_FIELD".equals(table)){
                    addFieldFromCache(table, (Map) input.get("datas"));
                }else if("ISP_DICTIONARY_TABLE".equals(table)){
                    addTableFromCache(table, (Map) input.get("datas"));
                }else if("ISP_DICTIONARY_TABLE_REL".equals(table)){
                    addTableRelFromCache(table, (Map) input.get("datas"));
                }else if("ISP_DICTIONARY_TABLE_ROUTER".equals(table)){
                    addRouterFromCache(table, (Map) input.get("datas"));
                    checkAndCreate((String)input.get("TABLE_NAME"));
                }
            }else if("update".equals(op)){
                if("ISP_DICTIONARY_FIELD".equals(table)){
                    updateFieldFromCache(table, (Map) input.get("datas"));
                }else if("ISP_DICTIONARY_TABLE".equals(table)){
                    updateTableFromCache(table, (Map) input.get("datas"));
                }else if("ISP_DICTIONARY_TABLE_REL".equals(table)){
                    updateTableRelFromCache(table, (Map) input.get("datas"));
                }else if("ISP_DICTIONARY_TABLE_ROUTER".equals(table)){
                    updateRouterFromCache(table,(Map)input.get("datas"));
                }
            }else if("checkAndCreate".equals(op)){
                checkAndCreate((String)input.get("table"));
            }else if("generatorTableContainer".equals(op)){
                List<String> ts = (List)input.get("tables");
                if(null != ts){
                    return getTableContent(ts);
                }
            }
        }
        return null;
    }
    //generator table contianer xml by table names
    StringBuffer getTableContent(List<String> names) throws IOException {
        String rtn = FileUtils.getFileContentString(this.getClass().getClassLoader().getResourceAsStream("tablecontainer.tmp"));
        if(null != names) {
            StringBuffer fs = new StringBuffer();
            StringBuffer ts = new StringBuffer();
            List<String> tm = new ArrayList();
            if(StringUtils.isNotBlank(rtn)) {
                for (String t : names) {
                    if (getAllTables().containsKey(t)) {
                        TableBean tb = getAllTables().get(t);
                        if(null != tb) {
                            List<TableField> tfs = tb.getTableFields();
                            if(null != tfs) {
                                for(TableField tf:tfs) {
                                    ts.append("<property IS_CACHE=\""+(tf.isCache()?"1":"0")+"\" TABLE_NAME=\""+tb.getTableName()
                                            +"\" FIELD_CODE=\""+tf.getField().getFieldCode()+"\" USED_TYPES=\""+ArrayUtils.toJoinString(tf.getUsedTypes())
                                            +"\" NOT_NULL=\""+(tf.isNotNull()?"1":"0")+"\" TABLE_NUM=\""+tf.getTableNum()+"\"/>\r\n");

                                    if(!tm.contains(tf.getField().getFieldCode())) {
                                        fs.append("<property FIELD_NAME=\""+tf.getField().getFieldName()+"\" FIELD_CODE=\""+tf.getField().getFieldCode()
                                                +"\" FIELD_TYPE=\""+tf.getField().getFieldType()+"\" FIELD_LEN=\""+tf.getField().getFieldLen()
                                                +"\" FIELD_NUM=\""+tf.getField().getFieldNum()+"\" STATE=\"1\"/>\r\n");
                                        tm.add(tf.getField().getFieldCode());
                                    }
                                }
                            }
                        }
                    }
                }
                rtn = rtn.replace("<!-- APP_FIELDS -->", fs.toString());
                rtn = rtn.replace("<!-- APP_TABLES -->", ts.toString());
            }

        }
        if(null != rtn) {
            return new StringBuffer(rtn);
        }else{
            return null;
        }
    }

    void removeFieldFromCache(String table,Map datas){
    }
    void addFieldFromCache(String table,Map datas){
    }
    void updateFieldFromCache(String table,Map datas){
    }
    void removeTableFromCache(String table,Map datas){
    }
    void addTableFromCache(String table,Map datas){
    }
    void updateTableFromCache(String table,Map datas){
    }
    void removeTableRelFromCache(String table,Map datas){
    }
    void addTableRelFromCache(String table,Map datas){
    }
    void updateTableRelFromCache(String table,Map datas){
    }

    void removeRouterFromCache(String table,Map datas){
        String name = ((String)datas.get("TABLE_NAME")).toUpperCase();

        getTableRouters().remove(name);
    }
    void addRouterFromCache(String table,Map datas){
        String name = ((String)datas.get("TABLE_NAME")).toUpperCase();
        if(getTableRouters().containsKey(name)){
            try {
                getTableRouters().get(name).add((TableRouterBean)POJOUtil.convertDBMap2POJO(datas, TableRouterBean.class));
            }catch (Exception e){
                e.printStackTrace();
            }
        }else{
            try {
                List li = new ArrayList();
                li.add((TableRouterBean) POJOUtil.convertDBMap2POJO(datas, TableRouterBean.class));
                getTableRouters().put(name, li);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    void updateRouterFromCache(String table,Map datas){
        List<TableRouterBean> ls = getTableRouters().get(((String)datas.get("TABLE_NAME")).toUpperCase());
        if(null != ls){
            for(TableRouterBean b :ls){
                if(b.getRouterId().equals(datas.get("ROUTER_ID"))){
                    POJOUtil.setValues(datas,b);
                }
            }
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
