package com.octopus.isp.actions;

import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 把一行数据转变成多行数据
 * User: wfgao_000
 * Date: 16-2-4
 * Time: 上午12:13
 */
public class OneRow2MulRow extends XMLDoObject {
    public OneRow2MulRow(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(null != input){
            List<Map> obj = (List)input.get("obj");
            Map m = (Map)input.get("compose");
            String pn = (String)m.get("parentName");
            String n = (String)m.get("name");
            String id=   (String)m.get("id");
            String rootParentId=   (String)m.get("rootvalue");
            String parentid=   (String)m.get("parentid");
            List<String> f = (List)m.get("fields");
            List<String> notcopyfields = (List)m.get("notcopyfields");
            Map newids = (Map)m.get("newid");
            Map defindids = (Map)m.get("ids");
            String idstartwith = (String)m.get("idstartwith");
            int idlength =0;
            rootMaxNum=null;
            if(null != newids){
                Iterator its = newids.values().iterator();
                while(its.hasNext()){
                    idlength+=Long.parseLong(its.next().toString());
                }
            }

            Map<String,List<Map>> ret = new LinkedHashMap<String, List<Map>>();
            Map<String,AtomicInteger> temSeq = new LinkedHashMap<String, AtomicInteger>();
            for(int i=0;i<f.size();i++){
                temSeq.put(f.get(i),new AtomicInteger(0));
            }
            Map<String,Map> ids = new HashMap();
            boolean  isLeaf=false;
            for(Map d:obj){
                HashMap pre=null;
                for(int i=0;i<f.size();i++){
                    isLeaf=false;
                    String parent=null;
                    if(i>0){
                       parent = f.get(i-1);
                    }
                    if(i==f.size()-1 || (i<f.size()-1 && null == d.get(f.get(i+1)))){
                        isLeaf=true;
                    }
                    if(null != d.get(f.get(i)) && !ret.containsKey(getKeyStr(d, f, i)) ){
                        HashMap t = new HashMap();

                        if(null != parent){
                            t.put(pn,d.get(parent));
                        }


                        if(StringUtils.isNotBlank(id)){
                            if(parent!=null){
                                String parentKey = getKeyStr(d,f,i-1);
                                //String curKey = getKeyStr(d,f,i);
                                String tc=getId(ret,parentKey,id,newids,i,temSeq,idlength,idstartwith);
                                /*if(ret.containsKey(curKey)){
                                    tc = String.valueOf(Integer.parseInt((String)ret.get(curKey).get(ret.get(curKey).size()-1).get(id))+1);
                                    temSeq.get(parentKey).addAndGet(1);
                                }else{
                                    String pids = (String)ret.get(parentKey).get(ret.get(parentKey).size()-1).get(id);
                                    int l=0;
                                    for(int k=0;k<i;k++){
                                        l+=Long.parseLong(newids.get(String.valueOf(k + 1)).toString());
                                    }
                                    //int l = Integer.parseInt(newids.get(String.valueOf(i+1)).toString());
                                    String pid=pids.substring(0,l);

                                    if(!temSeq.containsKey(parentKey)){
                                        temSeq.put(parentKey,new AtomicInteger(0));
                                    }
                                    String ph = StringUtils.leftPad(String.valueOf(temSeq.get(parentKey).addAndGet(1)),Integer.parseInt((String)newids.get(String.valueOf(i+1))),"0");
                                    pid = String.valueOf(Long.parseLong(pid)+ph);
                                    tc = StringUtils.rightPad(pid,idlength,'0');
                                }*/
                                /*String preid = ret.get();
                                if(StringUtils.isBlank(preid)){
                                    String pid = temSeq.get(parent+"."+parentKey);
                                    int l = Integer.parseInt(newids.get(String.valueOf(i+1)).toString());
                                    preid=StringUtils.rightPad(pid,pid.length()+l,'0');
                                }*/
                                //String tc = String.valueOf(Long.parseLong(preid)+1);

                                //String curid = StringUtils.leftPad(tc,s,'0');
                                //temSeq.put(parent+"."+parentKey+".cur",curid);
                                //temSeq.put(f.get(i)+"."+curKey,curid);
                                //curid = StringUtils.rightPad(curid,idlength,'0');
                                if(ids.containsKey(tc)){
                                    throw new Exception(" id has exist:"+tc+"\n"+ids.get(tc)+"\n"+d);
                                }
                                ids.put(tc,d);
                                if(!t.containsKey(id))
                                    t.put(id, tc);

                                //temSeq.put(curKey,curid);

                            }else{
                                //String curKey = getKeyStr(d,f,i);
                                //String r = temSeq.get("root");
/*
                                String tc="";
                                if(rootMaxNum!=null){
                                    tc = String.valueOf(Integer.parseInt(rootMaxNum)+1);
                                    rootMaxNum=tc;
                                }else{
                                    tc =StringUtils.rightPad("1",s,'0');
                                    rootMaxNum=tc;
                                }
                                String curid = StringUtils.rightPad(tc,idlength,'0');
*/
                                /*if(ret.containsKey(f.get(i))){
                                    tc = String.valueOf(Integer.parseInt((String)ret.get(curKey).get(ret.get(curKey).size()-1).get(id))+1);
                                }else{
                                    if(temSeq.get(f.get(i)).get()!=0){

                                    }else{
                                        tc=""+temSeq.get(f.get(i)).addAndGet(1);
                                        tc = StringUtils.rightPad(tc,s,'0');

                                    }
                                }*/
                                //temSeq.put("root",tc);
                                //String curid = StringUtils.rightPad(tc,s,'0');
                                //temSeq.put(f.get(i)+"."+curKey,curid);
                                String curid=getId(ret,null,id,newids,i,temSeq,idlength,idstartwith);
                                if(!t.containsKey(id))
                                    t.put(id, curid);

                                //temSeq.put(curKey,curid);
                            }
                        }
                        copyData(t,d,f,notcopyfields,isLeaf);
                        if(!t.containsKey(parentid)){
                            String parentKey= getKeyStr(d,f,i-1);
                            if(ret.containsKey(parentKey))
                                t.put(parentid,ret.get(parentKey).get(ret.get(parentKey).size()-1).get(id));
                            else
                                t.put(parentid,rootParentId);
                        }
                        if(null != defindids && defindids.containsKey(d.get(f.get(i)))){
                            t.put(id,defindids.get(d.get(f.get(i))));
                        }
                        t.put(n,d.get(f.get(i)));
                        t.put("UpDownLevel",i+1);
                        List array = new LinkedList();
                        array.add(t);
                        ret.put(getKeyStr(d,f,i),array);
                        pre=t;
                    }
                }
                if(null != pre) {
                    copyData(pre,d,f,notcopyfields,isLeaf);
                    //pre.putAll(d);
                    /*for(String j:f){
                        pre.remove(j);
                    }*/
                }else{
                    String key = getKeyStr(d,f,f.size()-1);
                    String parentKey = getKeyStr(d, f, f.size() - 2);
                    if(StringUtils.isNotBlank(parentKey)){
                        List<Map> tt = ret.get(key);
                        if(null != tt){
                            HashMap map = new LinkedHashMap();
/*
                            String preid = (String)tt.get(tt.size()-1).get(id);
                            String tc = String.valueOf(Long.parseLong(preid)+1);
                            if(ids.containsKey(tc)){
                                throw new Exception(" id has exist:"+tc+"\n"+ids.get(tc)+"\n"+d);
                            }
                            ids.put(tc,d);
*/
                            String tc = getId(ret,parentKey,id,newids,f.size()-1,temSeq,idlength,idstartwith);
                            //temSeq.put(key,tc);
                            map.putAll(tt.get(tt.size()-1));
                            copyData(map,d,f,notcopyfields,isLeaf);
                            map.put(id,tc);
                            ret.get(key).add(map);
                        }
                    }
                }

            }
            if(ret.size()>0){
                List r = new LinkedList();
                Iterator<List<Map>> its = ret.values().iterator();
                while(its.hasNext())
                    r.addAll(its.next());
                return r;
            }

        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
    void copyData(Map newData,Map data,List<String> fields,List<String> notcopyfields,boolean isLeaf){
        if(isLeaf){
            newData.putAll(data);
        }else{
            if(notcopyfields==null || notcopyfields.size()==0)
                newData.putAll(data);
            else{
                Iterator<String> its = data.keySet().iterator();
                String key;
                String[] ss = notcopyfields.toArray(new String[0]);
                while(its.hasNext()){
                    key = its.next();
                    if(!ArrayUtils.isLikeStringArray(ss,key)){
                        newData.put(key,data.get(key));
                    }
                }
            }
            //temSeq.put(getKeyStr(d, f, f.size() - 1),tc);
            for(String j:fields){
                newData.remove(j);
            }
        }
    }
    String rootMaxNum=null;
    String getId(Map<String,List<Map>> ret,String parentKey,String id,Map<String,String> newids,int i,Map<String,AtomicInteger> temSeq,int idlength,String idstartwith){
        if(null != newids){
            if(parentKey==null){
                String tc="";
                int s =0;
                if(null != newids){
                    for(int j=i+1;j>0;j--){
                        s += Long.parseLong(newids.get(String.valueOf(j)).toString());
                    }
                }
                if(rootMaxNum!=null){
                    tc = String.valueOf(Integer.parseInt(rootMaxNum)+1);
                    rootMaxNum=tc;
                }else{
                    tc =StringUtils.rightPad("1",s,'0');
                    rootMaxNum=tc;
                }
                String curid = StringUtils.rightPad(tc,idlength,'0');
                return curid;
            }else{
                String pids = (String)ret.get(parentKey).get(ret.get(parentKey).size()-1).get(id);
                int l=0;
                for(int k=0;k<i;k++){
                    l+=Long.parseLong(newids.get(String.valueOf(k + 1)).toString());
                }
                //int l = Integer.parseInt(newids.get(String.valueOf(i+1)).toString());
                String pid=pids.substring(0,l);

                if(!temSeq.containsKey(parentKey)){
                    temSeq.put(parentKey,new AtomicInteger(0));
                }
                String ph = StringUtils.leftPad(String.valueOf(temSeq.get(parentKey).addAndGet(1)),Integer.parseInt((String)newids.get(String.valueOf(i+1))),"0");
                pid = String.valueOf(Long.parseLong(pid)+ph);
                return StringUtils.rightPad(pid,idlength,'0');
            }
        }else{
            if(rootMaxNum==null)
                rootMaxNum=idstartwith;

            rootMaxNum= String.valueOf(Long.parseLong(rootMaxNum)+1);
            return rootMaxNum;
        }
    }
    String getKeyStr(Map d,List<String> fs,int t){
        StringBuffer sb = new StringBuffer();
        for(int i=0;i<=t;i++){
            if(sb.length()!=0)sb.append(".");
            if(null != d.get(fs.get(i))){
                sb.append(d.get(fs.get(i))).append(""+i);
            }
        }
        return sb.toString();
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
}
