package com.octopus.tools.synchro.canal;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.*;

/**
 * Created by kod on 2017/2/7.
 * binlog送入每个表的变更记录进来，根据表关联配置，当满足定义的所有表关联时，把关联的所有记录送出，执行后面的动作。如果超时记录日志，清除内存中的数据。
 * input
 *    op：binlog中的操作，INSERT,UPDATE,DELETE
 *    olddata: 老的数据记录
 *    newdata: 新的数据记录
 * config
 *    timeout:定义的超时时间，单位ms
 *    mapping：定义的表之间的关系
 *    maxMemRecNum:最大内存存放的记录数，控制内存量，达到这个数量时，放入磁盘等待，当内存小于时再装入内存。
 *    tempFilePath:磁盘存放文件的目录
 */
public class ActionMutiTableOneRecCondition extends XMLDoObject{
    long timeout=0;
    String tempFilePath;
    int maxMemRecNum=0;


    LinkedList<Map> container = new LinkedList();

    public ActionMutiTableOneRecCondition(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
        ExecutorUtils.work(new Runnable(){

            @Override
            public void run() {
                while(true) {
                    try {
                        checkFinished();
                        Thread.sleep(300000);
                    }catch (Exception e){
                        log.error(e);
                    }
                }
            }
        });

    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(null != config){
            if(timeout==0) {
                Object o = config.get("timeout");
                if (null != o) {
                    if (o instanceof Integer)
                        timeout = (Integer) o;
                    else if (o instanceof String && StringUtils.isNotBlank((String) o))
                        timeout = Long.parseLong((String) o);
                }
            }
            if(maxMemRecNum==0) {
                Object o = config.get("maxMemRecNum");
                if (null != o) {
                    if (o instanceof Integer)
                        maxMemRecNum = (Integer) o;
                    else if (o instanceof String && StringUtils.isNotBlank((String) o))
                        maxMemRecNum = Integer.parseInt((String) o);
                }
            }
            if(null == tempFilePath)
                tempFilePath = (String)config.get("tempFilePath");

        }
        return true;
    }



    Map appendData(String table,String field,String value,List<Map<String,String>> bind,String type,Map newData,Map oldData,String op){
//组装加入的结构
        HashMap d = new HashMap();
        d.put("new",newData);
        d.put("old",oldData);
        HashMap t = new HashMap();
        t.put(table,d);
        t.put("op",op);
        t.put("actionType",type);

        HashMap<String,String> bs = new HashMap();
        if(null != bind) {
            for (Map<String,String> bi:bind) {
                Map.Entry<String,String> e = bi.entrySet().iterator().next();
                bs.put(e.getKey(),e.getValue());
                bs.put(e.getValue(),e.getKey());
            }
        }

        boolean isin= false;
        for(Map m:container){

                String tablefield = table+"."+field;
                String relField = bs.get(tablefield);
                if(m.containsKey(relField)){
                    if(((Map)m.get(relField)).containsKey(value) && m.get("actionType").equals(type)){
                        if((!m.containsKey(tablefield))){
                            m.put(tablefield,new HashMap<String, Map>());
                        }
                        if(((Map)m.get(tablefield)).size()==0){
                            ((Map)m.get(tablefield)).put(value,t);
                            isin=true;
                        }
                    }
                }
        }



        if(!isin){
            List<String> tf = new ArrayList();
            for(Map<String,String> i:bind){
                Map.Entry<String,String> e = i.entrySet().iterator().next();
                String k = e.getKey();
                String v = e.getValue();
                if(!tf.contains(k))
                    tf.add(k);
                if(!tf.contains(v))
                    tf.add(v);
            }
            HashMap m = new HashMap();
            for(String f:tf){
                m.put(f,new HashMap());
                m.put("actionType",type);
                m.put("startDate",System.currentTimeMillis());
                if(f.equals(table+"."+field)){
                    ((Map)m.get(f)).put(value,t);
                }
            }
            container.add(m);
        }

        return null;

    }
    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        //获取当前记录
        String op = (String)input.get("op");
        Map oldData = (Map)input.get("olddata");
        Map newData = (Map)input.get("newdata");
        String table = (String)input.get("table");
        Map defined=null;
        if(defined==null) {
            defined = (Map) config.get("defined");

        }
        //获取容器中的key
        if(null != defined){
            Iterator<String> its = defined.keySet().iterator();
            while(its.hasNext()){
                String type = its.next();
                //table:cond
                Map<String,String> cond = (Map)((Map)defined.get(type)).get("cond");
                if(null != cond){
                    if(StringUtils.isTrue(cond.get(table))){
                        //当前记录满足一个条件
                        //table.field=table.field
                        List<Map<String,String>> bind = (List)((Map)defined.get(type)).get("bind");
                        if(null != bind) {
                            List ret=new ArrayList();
                            for (Map<String,String> i:bind) {
                                Map.Entry<String,String> e = i.entrySet().iterator().next();
                                String tf = e.getKey();
                                String[] tfs = StringUtils.split(tf, ".");
                                if(tfs.length!=2){
                                    throw new Exception(tf+" is not match [TableName.FieldName]");
                                }
                                if (tfs[0].equals(table)) {
                                    if(null != newData && newData.size()>0) {
                                        if(null == newData.get(tfs[1]))
                                            throw new Exception(newData+"  \n"+tfs[1]+" is null");
                                        Map  li = appendData(tfs[0], tfs[1], newData.get(tfs[1]).toString(), bind, type, newData, oldData, op);
                                        ret.add(li);
                                    }else {
                                        if(null == oldData.get(tfs[1]))
                                            throw new Exception(oldData+"  \n"+tfs[1]+" is null");
                                        Map li = appendData(tfs[0], tfs[1], oldData.get(tfs[1]).toString(), bind, type, newData, oldData, op);
                                        ret.add(li);
                                    }
                                    /*某个表再多表条件的情况下会同时满足多个条件，由另外表的变化决定类型,所以要把满足的所有类型都记录下来
                                    if(null != li){
                                        return li;
                                    }*/
                                }
                                String vf = e.getValue();
                                String[] vfs = StringUtils.split(vf, ".");
                                if (vfs[0].equals(table)) {

                                    if(null != newData && newData.size()>0) {
                                        if(null == newData.get(vfs[1]))
                                            throw new Exception(newData+"  \n"+tfs[1]+" is null");
                                        Map li = appendData(vfs[0], vfs[1], newData.get(vfs[1]).toString(), bind, type, newData, oldData, op);
                                        ret.add(li);
                                    }else {
                                        if(null == oldData.get(vfs[1]))
                                            throw new Exception(oldData+"  \n"+tfs[1]+" is null");
                                        Map li = appendData(vfs[0], vfs[1], oldData.get(vfs[1]).toString(), bind, type, newData, oldData, op);
                                        ret.add(li);
                                    }
                                    /*某个表再多表条件的情况下会同时满足多个条件，由另外表的变化决定类型,所以要把满足的所有类型都记录下来
                                    if(null != li){
                                        return li;
                                    }*/
                                }

                            }
                            //return ret;

                        }else{
                            HashMap d = new HashMap();
                            d.put("new",newData);
                            d.put("old",oldData);
                            HashMap t = new HashMap();
                            t.put(table,d);
                            t.put("op",op);
                            t.put("actionType",type);
                            return t;

                        }


                    }
                }

            }
        }
        return checkFinished();
    }
    synchronized Object checkFinished(){
        //检查是否完成
        /**
         *  outkeyvalue1   tableA.field    reltableB.field
         *  outkeyvalue2   tableA.field    reltableB.field
         */
        for(Map m:container) {
            //检查是否完成
            Iterator<String> its = m.keySet().iterator();
            int n = m.size() - 2;
            Map li = new HashMap();
            while (its.hasNext()) {
                String k = its.next();
                if (m.get(k) instanceof Map && ((Map)m.get(k)).size() > 0) {
                    li.putAll((Map)((Map.Entry)((Map)m.get(k)).entrySet().iterator().next()).getValue());
                    n--;
                }
            }

            if (n == 0) {
                //完成
                container.remove(m);
                return li;
            }
            if(n>0 && (System.currentTimeMillis()-((Long)m.get("startDate")))>timeout){
                container.remove(m);
                li.put("event","timeout");
                return li;
            }
        }
        return null;
    }

    @Override
    public ResultCheck checkReturn(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        if(null != ret)
        return new ResultCheck(true,ret);
        else
            return new ResultCheck(false,null);
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config,Object ret,Exception e) throws Exception {
        return false;
    }
}
