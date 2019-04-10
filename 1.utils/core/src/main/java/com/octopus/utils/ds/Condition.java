package com.octopus.utils.ds;

import com.octopus.utils.cls.POJOUtil;
import com.octopus.utils.ds.TableBean;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-8-15
 * Time: 下午5:09
 */
public class Condition {
    public static String OP_EQUAL="=";
    public static String OP_IN="in";
    public static String OP_BETWEEN="between";
    public static String OP_MORE=">";
    public static String OP_LESS="<";
    public static String OP_LIKE="like";
    public static String OP_IS="is";

    public String fieldName;
    public String op;
    Object values;

    protected Condition(){

    }
    public static Condition createCondition(XMLParameter env,String fv,Object o)throws Exception{
        String f=fv;
        if(null != env) {
            f = (String) env.getExpressValueFromMap(fv, null);
            if(!f.equals(fv)){
                f="'"+f+"'";
            }
        }
        if(o ==null){
            return createNullCondition(f);
        }else if(!f.equals("limit") && o instanceof List){
            return createInCondition(f, (List) o) ;
        }else if(f.equals("limit") && o instanceof List){
            return createLimitCondition(f, ((List) o).get(0), ((List) o).get(1)) ;
        }else if(o instanceof Map){
            Map p=(Map)o;
            Object m = null;
            Object l = null;
            if(p.containsKey("M"))
                m  = p.get("M");
            if(p.containsKey("L"))
                l  = p.get("L");

            if(null != m && null !=l){
                return createBetweenCondition(f,m,l);
            }else if(null == m && null !=l){
                return createLessCondition(f,l);
            }else if(null != m && null ==l){
                return createMoreCondition(f,m);
            }

        }else if(POJOUtil.isPrimitive(o.getClass().getName())){
            return createEqualCondition(f,o);
        }

        throw new Exception("not support conditon["+f+" "+o+"]");
    }
    public static Condition createCondition(String cond)throws Exception{
        if(cond.contains("=")){
            int n = cond.indexOf("=");
            String name = cond.substring(0,n);
            String v = cond.substring(n+1);
            return createEqualCondition(name,v);
        }
        throw new Exception("not support conditon["+cond+"]");
    }
    public static Condition createEqualCondition(String fieldName,Object value){
        Condition cond = new Condition();
        cond.fieldName=fieldName;
        cond.op=OP_EQUAL;
        cond.values=value;
        return cond;
    }
    public static Condition createNullCondition(String fieldName){
        Condition cond = new Condition();
        cond.fieldName=fieldName;
        cond.op=OP_IS;
        cond.values=null;
        return cond;
    }
    public static  Condition createInCondition(String fieldName,List value){
        Condition cond = new Condition();
        cond.fieldName=fieldName;
        cond.op=OP_IN;
        cond.values=value;
        return cond;
    }
    public static Condition createLimitCondition(String fieldName,Object start,Object end){
        Condition cond = new Condition();
        cond.fieldName=fieldName;
        cond.op="";
        cond.values=new Object[]{start,end};
        return cond;
    }
    public  static Condition createBetweenCondition(String fieldName,Object start,Object end){
        Condition cond = new Condition();
        cond.fieldName=fieldName;
        cond.op=OP_BETWEEN;
        cond.values=new Object[]{start,end};
        return cond;
    }
    public  static Condition createMoreCondition(String fieldName,Object value){
        Condition cond = new Condition();
        cond.fieldName=fieldName;
        cond.op=OP_MORE;
        cond.values=value;
        return cond;
    }
    public static  Condition createLessCondition(String fieldName,Object value){
        Condition cond = new Condition();
        cond.fieldName=fieldName;
        cond.op=OP_LESS;
        cond.values=value;
        return cond;
    }
    public static  Condition createLikeCondition(String fieldName,Object value){
        Condition cond = new Condition();
        cond.fieldName=fieldName;
        cond.op=OP_LIKE;
        cond.values=value;
        return cond;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getOp() {
        return op;
    }

    public void setOp(String op) {
        this.op = op;
    }

    public Object getValues() {
        return values;
    }

    public void setValues(Object values) {
        this.values = values;
    }

    public String toString(Map<String,Object> pars,TableBean tb){
        StringBuffer sb = new StringBuffer(fieldName);
        String vf = fieldName;
        if(fieldName.startsWith("'")){
            vf = StringUtils.trimRemainNumAndLetter(fieldName.substring(1, fieldName.length() - 1));
        }
        sb.append(" ").append(op).append(" ");
        if(null !=values && values instanceof String && values.equals("^Non")){//表示没有输入，改条件不考虑
            return "";
        }else if(values ==null){
            sb.append(" null ");
        }else if(values instanceof Collection){
            sb.append(" ( ");
            Iterator its = ((Collection)values).iterator();
            int count=0;
            while(its.hasNext()){
                String k = ":"+vf+count;
                if(count==0){
                    sb.append(k);
                }else{
                    sb.append(",").append(k);
                }
                pars.put(k,its.next());
                count++;
            }
            sb.append(" ) ");
        }else if(op.equals(OP_BETWEEN) && values.getClass().isArray()){
            String k1=null;
            if( ((Object[])values)[0] instanceof String && null != tb){
                if(tb.existField((String)((Object[])values)[0])){
                    k1= (String)((Object[])values)[0];
                }else{
                    k1 = ":" + vf + 1;
                    pars.put(k1,((Object[])values)[0]);
                }
            }else {
                k1 = ":" + vf + 1;
                pars.put(k1,((Object[])values)[0]);
            }
            String k2 = null;
            if( ((Object[])values)[1] instanceof String && null != tb){
                if(tb.existField((String)((Object[])values)[1])){
                    k2= (String)((Object[])values)[1];
                }else{
                    k2 = ":" + vf + 2;
                    pars.put(k2,((Object[])values)[1]);
                }
            }else {
                k2 = ":" + vf + 2;
                pars.put(k2,((Object[])values)[1]);
            }

            sb.append(" ").append(k1).append(" and ").append(k2);
        }else{
            String k="";
            if(vf.contains("(")){
                k = ":" + vf.substring(0,vf.indexOf("(")) + this.hashCode();
            }else {
                k = ":" + vf + this.hashCode();
            }

            if( values instanceof String && null != tb){
                if(tb.existField((String)values)){
                    k= (String)values;
                }else{
                    if(OP_LIKE==op){
                        pars.put(k,"%"+values+"%");
                    }else{
                        pars.put(k,values);
                    }
                }
            }else {
                if(OP_LIKE==op){
                    pars.put(k,"%"+values+"%");
                }else{
                    pars.put(k,values);
                }
            }

            sb.append(k);
        }

        return sb.toString();
    }
}
