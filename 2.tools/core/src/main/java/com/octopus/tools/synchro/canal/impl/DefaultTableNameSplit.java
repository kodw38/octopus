package com.octopus.tools.synchro.canal.impl;

import com.octopus.tools.synchro.canal.ITableSplit;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cachebatch.DateTimeUtil;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * User: wfgao_000
 * Date: 15-11-23
 * Time: 下午2:12
 */
public class DefaultTableNameSplit extends XMLObject implements ITableSplit{
    static transient Log log = LogFactory.getLog(DefaultTableNameSplit.class);

    public DefaultTableNameSplit(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
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

    public String split(String tables , Map map){
        String[] ts = tables.split(",");
        StringBuffer sb = new StringBuffer();
        if(null != ts){
            for(String t:ts){
                List<String> li = StringUtils.getTagsIncludeMark(t, "${", "}");
                if(null != li && li.size()>0){
                    List<String> tem = new LinkedList();
                    tem.add(t);
                    for(String m:li){
                        List tt = new LinkedList();
                        for(String te:tem){
                            try{
                                if(te.contains("${")){
                                    String tab = te.substring(0,te.indexOf("_${"));
                                    String[] tms = splitTableName(tab,m.substring(2,m.length()-1),null,te.substring(te.indexOf("_${")+m.length()+1));
                                    for(String s:tms) {
                                        tt.add(s);
                                        if(null != map){
                                            map.put(s.toUpperCase(),tab.toUpperCase());
                                        }
                                    }
                                }
                            }catch (Exception e){
                                log.error(te,e);
                            }
                        }
                        tem = tt;

                    }
                    if(null !=tem){
                        for(String tm:tem){
                            if(sb.length()>0) {
                                sb.append(",");
                            }

                            sb.append(tm);
                        }
                    }
                }else{
                    if(sb.length()>0) {
                        sb.append(",");
                    }
                    if(null != map){
                        map.put(t.toUpperCase(),t.toUpperCase());
                    }
                    sb.append(t);
                }
            }
        }
        return sb.toString();
    }
    @Override
    public String split(String tables) {
        return split(tables,null);
    }

    public static synchronized String[] splitTableName(String table,String e,List pharseList,String endtag) throws Exception {
        return StringUtils.splitTableName(table,e,pharseList,endtag);
    }

    public static void main(String[] args){
        try{
            DefaultTableNameSplit t = new DefaultTableNameSplit(null,null,null);
            //System.out.println(t.split("mysql.TL_M_OSE_LOG_${01-31}_${201601-202012}"));
            HashMap map = new HashMap();
            System.out.println(t.split("idstest.ca_busi_rec_22_0_${01-12},cs.M",map));
            System.out.println(map);
            //System.out.println(t.split("mysql.TL_M_OSE_LOG_${01-31},mysql.TL_M_ABILITY_LOG_${01-31}"));
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
