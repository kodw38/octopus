package com.octopus.tools.dataclient.utils;

import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;

import java.util.*;

/**
 * User: Administrator
 * Date: 14-10-28
 * Time: 下午1:57
 */
public class DataMapping extends XMLObject implements IDataMapping {
    public DataMapping(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
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

    @Override
    public List<Map> mapping(Object data)throws Exception{
        XMLMakeup[] ms= getXML().getChild("m");
        //todo
        // 目前假定是一个单表,data是一个map
        if(null != ms){
            Map map   = new HashMap();
            //FieldDef[] fs = new FieldDef[ms.length];
            //StringBuffer sb = new StringBuffer(" ");
            for(int i=0;i<ms.length;i++){
                String fieldcode = ms[i].getProperties().getProperty("field");
                //fs[i]= FieldContainer.getField(fieldcode);
                //sb.append(" ").append(fieldcode);
                map.put(ms[i].getText().trim(), fieldcode);
            }
            //TableValue tv = new TableValue();
            //TableDef tf = new TableDef();
            //tf.setFieldDefs(fs);
            //TableDef[] tds = TableDefContainer.getBelongTables(fs);
            //if(null == tds)throw new Exception("not find table by fields "+sb.toString());
            //tf.setDataSource(tds[0].getDataSource());

            //tf.setName(tds[0].getName());
            //tv.setTableDef(tf);
            List<Map> vs = new ArrayList<Map>();
            if(data instanceof List){
                for(int i=0;i<((List)data).size();i++){
                    appendData(vs,((List)data).get(i),map);
                }
            }else{
                appendData(vs,data,map);
            }
            return vs;
//            tv.setRecordValues(vs);
//            return new TableValue[]{tv};
        }
        return null;
    }
    void appendData(List<Map> vs,Object data,Map ps)throws Exception{

        HashMap ret = new HashMap();
        Iterator<String> its = ps.keySet().iterator();
        while(its.hasNext()){
            String v= its.next();
            String k= (String)ps.get(v);
            ret.put(k, ObjectUtils.getValueByPath(data, v));
        }
        vs.add(ret);
    }

}
