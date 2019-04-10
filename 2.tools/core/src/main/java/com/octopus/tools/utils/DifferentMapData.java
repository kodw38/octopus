package com.octopus.tools.utils;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.*;

/**
 * User: wfgao_000
 * Date: 16-1-31
 * Time: 下午9:29
 */
public class DifferentMapData extends XMLDoObject{
    public DifferentMapData(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(null != input){
            String op = (String)input.get("op");
            List<Map> m1 = (List)input.get("obj1");
            List<Map> m2 = (List)input.get("obj2");
            Map chgk1 = (Map)input.get("chgKeyMap1");
            Map chg1 = (Map)input.get("chgValueMap1");
            Map chg2 = (Map)input.get("chgValueMap2");
            Map keyMap = (Map)input.get("keyMap");
            Map valueMap = (Map)input.get("valueMap");
            if("update".equals(op)){
                //keymap值一样，其他值不一样的obj1记录
                List li = new ArrayList();
                if(null != m1 && null != keyMap && null != m2){
                    for(Map n:m1){
                        Iterator its = keyMap.keySet().iterator();
                        Map t = new HashMap();
                        while(its.hasNext()){
                            String nk = (String)its.next();
                            String ok = (String)keyMap.get(nk);
                            if(null != chgk1 && chgk1.containsKey(nk) && null != n.get(nk)){
                                Object o  = ((Map)chgk1.get(nk)).get(n.get(nk) );
                                if(null != o)
                                    t.put(ok,o);

                            }else{
                                t.put(ok,n.get(nk));
                            }
                        }
                        for(Map o:m2){
                            its = t.keySet().iterator();
                            String m;
                            boolean is=true;
                            while(its.hasNext()){
                                m=(String)its.next();
                                Object o1 = t.get(m);
                                Object o2 = o.get(m);
                                if( (null == o1 && null == o2) ||
                                        ( null !=o1 && o1 instanceof String && StringUtils.isBlank((String)o1) && null==o2)
                                        || ( null !=o2 && o2 instanceof String && StringUtils.isBlank((String)o2) && null==o1) )  continue;
                                if( ((null == o1 && null != o2) || (null != o1 && null == o2) || !o1.equals(o2.toString())) ){
                                    is=false;
                                    break;
                                }
                            }
                            if(is){
                                its = valueMap.keySet().iterator();
                                is=false;
                                while(its.hasNext()){
                                    String nk = (String)its.next();
                                    String ok = (String)valueMap.get(nk);
                                    Object o1 = n.get(nk);
                                    Object o2 = o.get(ok);
                                    if(null != chg1 && null != o1){
                                        if(chg1.containsKey(nk)){
                                            o1 = ((Map)chg1.get(nk)).get(o1);
                                        }
                                    }
                                    if(null != chg2 && null !=o2){
                                        if(chg2.containsKey(ok))
                                            o2 = ((Map)chg2.get(ok)).get(o2);
                                    }
                                    if( (null == o1 && null == o2) ||
                                            ( null !=o1 && o1 instanceof String && StringUtils.isBlank((String)o1) && null==o2)
                                            || ( null !=o2 && o2 instanceof String && StringUtils.isBlank((String)o2) && null==o1) )  continue;
                                    if( ((null == o1 && null != o2) || (null != o1 && null == o2) || !o1.equals(o2.toString())) ){
                                        is=true;
                                        break;
                                    }
                                }
                                if(is){
                                    if(!li.contains(n))
                                        li.add(n);
                                }
                            }
                        }

                    }
                }
                if(li.size()>0){
                    return li;
                }
            }else if("add".equals(op)){
                //keymap obj1有，obj2没有的记录
                List li = new ArrayList();
                List<String> tempkey = new ArrayList();
                if(null != m1 && null != keyMap){
                    /*for(Map n:m1){
                        if(null != m2){
                            Iterator its = keyMap.keySet().iterator();
                            Map t = new HashMap();
                            while(its.hasNext()){
                                String nk = (String)its.next();
                                String ok = (String)keyMap.get(nk);
                                if(null != chgk1 && chgk1.containsKey(nk) && null != n.get(nk)){
                                    Object o  = ((Map)chgk1.get(nk)).get(n.get(nk));
                                    if(null != o)
                                        t.put(ok,o);
                                }else{
                                    t.put(ok,n.get(nk));
                                }
                            }
                            boolean is=true;
                            for(Map o:m2){
                                is=true;
                                its = t.keySet().iterator();
                                String m;
                                while(its.hasNext()){
                                    m=(String)its.next();
                                    Object o1 = t.get(m);
                                    Object o2 = o.get(m);

                                    if( (null == o1 && null == o2) ||
                                            ( null !=o1 && o1 instanceof String && StringUtils.isBlank((String)o1) && null==o2)
                                            || ( null !=o2 && o2 instanceof String && StringUtils.isBlank((String)o2) && null==o1) )  continue;
                                    if( ((null == o1 && null != o2) || (null != o1 && null == o2) || !o1.equals(o2.toString())) ){
                                        is=false;
                                        break;
                                    }
                                }
                                if(is){
                                   break;
                                }
                            }
                            if(!is && !li.contains(n))
                                li.add(n);
                        }else{
                            li.add(n);
                        }
                    }*/
                    Map<String,Map> oldMap = new LinkedHashMap();
                    if(m2!=null && m2.size()>0){

                        StringBuffer sb = new StringBuffer();
                        for(Map m:m2){
                            Iterator<String> its = keyMap.keySet().iterator();
                            while(its.hasNext()){
                                String k = its.next();
                                String ok = (String)keyMap.get(k);
                                Object o  = m.get(ok);
                                if(null == o)
                                    o = "null";
                                else{
                                    if(null != chgk1 && chgk1.containsKey(ok)){
                                        o  = ((Map)chgk1.get(ok)).get(o);
                                        if(null ==o)
                                            o="null";
                                    }
                                }
                                sb.append(o).append(".");
                            }
                            String tk = sb.toString();
                            if(!oldMap.containsKey(tk))
                                oldMap.put(tk,m);
                            sb.delete(0,sb.length());
                        }
                    }
                    for(Map m:m1){
                        StringBuffer sb = new StringBuffer();
                        Iterator<String> its = keyMap.keySet().iterator();
                        while(its.hasNext()){
                            String k = its.next();
                            Object o  = m.get(k);
                            if(null == o)
                                o = "null";
                            else{
                                if(null != chgk1 && chgk1.containsKey(k)){
                                    o  = ((Map)chgk1.get(k)).get(o);
                                    if(null ==o)
                                        o="null";
                                }
                            }
                            sb.append(o).append(".");
                        }
                        String tk = sb.toString();
                        if(!oldMap.containsKey(tk) && !tempkey.contains(tk)){
                            li.add(m);
                            tempkey.add(tk);
                        }
                        sb.delete(0,sb.length());
                    }

                }
                tempkey.clear();
                if(li.size()>0){
                    return li;
                }
            }else if("remove".equals(op)){
                //keymap obj1没有，obj2有的记录
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
}
