package com.octopus.tools.i18n.impl;

import com.octopus.isp.ds.RequestParameters;
import com.octopus.tools.i18n.II18N;
import com.octopus.tools.i18n.Locale;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cls.ClassUtils;
import com.octopus.utils.exception.ExceptionUtil;
import com.octopus.utils.exception.ISPException;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.hadoop.util.ClassUtil;
import org.w3c.dom.Document;

import java.util.*;

/**
 * User: wangfeng2
 * Date: 14-8-18
 * Time: 下午5:15
 */
public class I18N extends XMLDoObject implements II18N{
    private static Map<String,I18NItem> items = new HashMap<String,I18NItem>();
    private static Locale locale;
    XMLDoObject cache;
    public I18N(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }


    I18NItem getItem(String type,String localString){
        Iterator its  = items.keySet().iterator();
        String n;
        String tl = type+"|"+localString;
        while(its.hasNext()){
            n = (String)its.next();
            if(tl.indexOf(n)>=0){
                return items.get(n);
            }
        }
        return null;
    }

    String getLocaleStr(Properties locale){
        return I18N.locale.getLocaleString(locale);
    }

    public Object getLocaleValue(String type,Properties locale,Object key){
        I18NItem item =  getItem(type,getLocaleStr(locale));
        if(null != item) return item.getLocaleValue(key,locale);
        return null;
    }



    public Object getLocaleValue(String type,Properties locale){
        I18NItem item =  getItem(type,getLocaleStr(locale));
        if(null != item) return item.getLocaleValue(locale);
        return null;
    }

    public Object getSystemValue(String type,Properties locale,Object key){
        I18NItem item =  getItem(type,getLocaleStr(locale));
        if(null != item) return item.getSystemValue(key,locale);
        return null;
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
        if(null != input) {
            if(null != input.get("data") && input.get("data") instanceof String && null !=input.get("url") && env instanceof RequestParameters ){//i18n for page
                /*String[] local = getLocal(env);
                if(((String)input.get("url")).endsWith("htm")
                        ||((String)input.get("url")).endsWith("html")
                        ||((String)input.get("url")).endsWith("js")) {
                    Document doc = JSoup.parse((String) input.get("data"));
                    //String ret=chgLanguage((String)input.get("data"),"en",local[0]);
                }*/
                String td = (String)input.get("data");
                List<String> ls = StringUtils.getTagsIncludeMark((String)input.get("data"),"${","}");
                if(null != ls && ls.size()>0) {
                    HashMap in = new HashMap();
                    in.put("cache", "cache_i18n");
                    in.put("op", "getList");
                    in.put("key", ArrayUtils.toJoinString(ls));
                    String[] ln = getLocal(env);
                    String i8 = getI18n(ln[0],ln[1],null);
                    Map t = new HashMap();
                    t.put("I18N",i8);
                    in.put("conds",t);
                    Map m = (Map)cache.doSomeThing(null, env, in, null, null);
                    if(null !=m) {
                        Iterator its = m.keySet().iterator();
                        List lk=new LinkedList();
                        List lv=new LinkedList();
                        while(its.hasNext()){
                            Object tk = its.next();
                            List<Map> lm = (List)m.get(tk);
                            if(null != lm){
                                Object tv= lm.get(0).get("FIELD_VALUE");
                                if(null != tv){
                                    lk.add(tk);
                                    lv.add(tv);
                                }
                            }
                        }
                        if(lk.size()>0 && lv.size()>0 && lk.size()==lv.size()) {
                            td = StringUtils.replace((String) input.get("data"), lk, lv);
                        }
                    }
                }
                return td;
            }else{// i18n for service
                Object r = input.get("^needi18nret");
                if (null != env && (null == r || (null != r && r instanceof String && ((String) r).startsWith("${result}")))) {
                    r = env.get("${^needi18nret}");
                }
                if ("^Exception".equals(r)) {
                    r = env.getException();
                }
                if (null != cache) {
                    HashMap in = new HashMap();
                    in.put("cache", "cache_i18n");
                    in.put("op", "size");
                    Object o = cache.doSomeThing(null, env, in, null, null);
                    if (null != o) {
                        if (o instanceof ResultCheck) {
                            o = ((ResultCheck) o).getRet();
                        }
                        if (o instanceof Integer && ((Integer) o) > 0) {
                            Object tr = chgI18nObj(r, env);
                            if(null != tr)r=tr;
                        }
                    }
                }

                return r;
            }
        }
        return null;
    }
    String[] getLocal(XMLParameter env){
        String lan=null,country=null;
        if(env instanceof RequestParameters){
            if(null != ((RequestParameters)env).getSession()) {
                Object i18n =  ((RequestParameters) env).getSession().get("I18N");
                if (null != i18n) {
                    Map m=null;
                    if(i18n instanceof String && ((String) i18n).startsWith("{")) {
                        m = StringUtils.convert2MapJSONObject((String)i18n);
                    }
                    if(i18n instanceof Map){
                        m = (Map)i18n;
                    }
                    if(null != m) {
                        lan = (String) m.get("language");
                        country = (String) m.get("country");
                    }
                }
            }
            if(StringUtils.isBlank(lan) && null != ((RequestParameters)env).getClientInfo()){
                lan = ((RequestParameters)env).getClientInfo().getLanguage();
                country=((RequestParameters)env).getClientInfo().getCountry();
            }
        }
        return new String[]{lan,country};
    }
    Object chgI18nObj(Object obj,XMLParameter env){
        String[] rr = getLocal(env);
        String lan=rr[0];
        String country=rr[1];
        return chgByLanguageAndCountry(env,lan,country,obj);

    }
    boolean isKeyInCache(XMLParameter env,String key)throws Exception{
        HashMap tem = new HashMap();
        tem.put("cache", "cache_i18n");
        tem.put("op", "exist");
        tem.put("key", key);
        Object o = cache.doSomeThing(null, env, tem, null, null);
        if(null!=o) {
            if(o instanceof ResultCheck){
                o = ((ResultCheck)o).getRet();
            }
            if (null != o && o instanceof Boolean && ((Boolean) o)) {
                    return true;
            }
        }
        return false;
    }
    List getValueInCacheByKey(XMLParameter env,String key) throws Exception {
        HashMap tem = new HashMap();
        tem.put("cache", "cache_i18n");
        tem.put("op", "get");
        tem.put("key", key);
        Object cl = cache.doSomeThing(null, env, tem, null, null);
        if (null != cl && cl instanceof ResultCheck) {
            if (((ResultCheck) cl).getRet() instanceof List) {
                return (List)((ResultCheck) cl).getRet();
            }
        }
        return null;
    }
    Object chgByLanguageAndCountry(XMLParameter env,String lan,String country,Object obj){
        if(null != obj) {
            try {
                if(obj instanceof Exception) {  // i18n for exception
                    if (null != ((Throwable) obj).getCause()) {
                        obj = ExceptionUtil.getRootCase((Exception) obj);
                    }
                    Object k = ClassUtils.getFieldValue(obj, "code", false);
                    if (null != k) {
                        String v = checkWithConfigAndSet(env, k.toString(), obj instanceof ISPException ? ((ISPException) obj).getMsg() : ((Throwable) obj).getMessage(), obj instanceof ISPException ? ((ISPException) obj).getMsgArgs() : null, lan, country, env);
                        if (null != v) {
                            if (obj instanceof ISPException) {
                                ((ISPException) obj).setRealMsg(v);
                            } else {
                                ClassUtils.setFieldValue(obj, "detailMessage", v, false);
                            }
                        }
                    }

                }else{
                    if (isKeyInCache(env, env.getTargetNames()[0])) {// i18n for service result
                        if (obj instanceof Map) {
                            checkWithConfigAndSet(env, env.getTargetNames()[0], null, null, lan, country, (Map) obj);
                            /*Iterator its = ((Map) obj).keySet().iterator();
                            boolean isc = false;
                            while (its.hasNext()) {
                                Object k = its.next();
                                isc = false;
                                if (k instanceof String) {
                                    String tv = checkWithConfigAndSet(env, k.toString(), ((Map) obj).get(k), null, lan, country, (Map) obj);
                                    if (null != tv) {
                                        isc = true;
                                    }
                                }
                                if (!isc) {
                                    chgByLanguageAndCountry(env, lan, country, ((Map) obj).get(k));
                                }
                            }*/
                        } else if (obj instanceof List) {
                            if (null != obj) {
                                for (Object o : (List) obj) {
                                    chgByLanguageAndCountry(env, lan, country, o);
                                }
                            }
                        }else if(obj instanceof String){
                            return checkWithConfigAndSet(env, env.getTargetNames()[0], obj, null, lan, country,  null);
                        }
                    }
                }
            }catch(Exception ex){
                log.error("i18n error",ex);
            }

        }
        return null;
    }

    //todo 根据环境lan，country和老的值判断需要转换的i18n项
    String getI18n(String lan,String country,Object v){
        return lan;
    }

    String checkWithConfigAndSet(XMLParameter env,String k,Object oldvalue,Map args,String lan,String country,Map obj){
        try {
            if (isKeyInCache(env,k)) {
                List cl = getValueInCacheByKey(env,k);
                if(null != cl) {
                    String tvalue =null;
                    boolean isc=false;
                    String ti18n=getI18n(lan,country,obj);
                    Map rep=new HashMap();
                    for (Object c : cl) {
                        if(null == c){
                            isc = true;
                        }
                        if ((null != c && c instanceof Map)) {
                            if(null != c && (StringUtils.isBlank((String)((Map)c).get("I18N")) || (null != ti18n && ti18n.equals((String)((Map)c).get("I18N"))))){
                                if(StringUtils.isBlank((String)((Map)c).get("CONDITIONS"))){
                                    isc = true;
                                    tvalue=(String)((Map)c).get("FIELD_VALUE");
                                    String path=(String)((Map)c).get("PATH");
                                    if(StringUtils.isNotBlank(path)) {
                                        rep.put(path,tvalue);
                                    }else{
                                        rep.put("",tvalue);
                                    }
                                }
                                if (null != c && null != obj && obj instanceof Map && StringUtils.isNotBlank(((Map)c).get("CONDITIONS"))) {
                                    Object ret = XMLParameter.getExpressValueFromMap((String)((Map)c).get("CONDITIONS"), (Map) obj, this);
                                    if (null != ret && ret instanceof String && StringUtils.isTrue((String) ret)) {
                                        isc = true;
                                        tvalue=(String)((Map)c).get("FIELD_VALUE");
                                        String path=(String)((Map)c).get("PATH");
                                        if(StringUtils.isNotBlank(path)) {
                                            rep.put(path,tvalue);
                                        }else{
                                            rep.put("",tvalue);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (isc) {
                        return chgItem(lan, country, (String) k, oldvalue,rep,args, (Map) obj);
                    }
                }

            }

        }catch(Exception e){
            log.error("",e);
        }
        return null;
    }
    /**
     * 国际化每个数据项
     * @param lan
     * @param country
     * @return
     */
    String chgItem(String lan,String country,String key,Object oldvalue,Map<String,String> nvalue,Map args,Map obj){
        if(StringUtils.isNotBlank(key) && null != oldvalue && null != nvalue && null != args && null != obj && obj instanceof Map ){//exception
              if(nvalue.size()==1){
                  String k = nvalue.keySet().iterator().next();
                  if(StringUtils.isBlank(k)){
                      String nv = nvalue.get(k);
                      return (String)XMLParameter.getExpressValueFromMap(nv,args,null);
                  }
              }
        }
        if(null != obj && null != nvalue){//service return map
            Iterator its = nvalue.keySet().iterator();
            while(its.hasNext()) {
                String s = (String)its.next();
                ObjectUtils.setValueByPath(obj,s,nvalue.get(s));
            }
        }
        if(null ==obj && null != oldvalue && oldvalue instanceof String && null == args && null != nvalue){//service return String
            Iterator its = nvalue.keySet().iterator();
            String ostr = (String)oldvalue;
            while(its.hasNext()){
                String s = (String)its.next();
                if(StringUtils.isBlank(s)){
                    return nvalue.get(s);  //replace oldvalue
                }
                if(ostr.indexOf(s)>=0){
                    ostr=StringUtils.replace(ostr,s,nvalue.get(s));
                }
            }
            return ostr;
        }
        /*if(null != nvalue && null != args){
            nvalue = (String)XMLParameter.getExpressValueFromMap(nvalue,args,null);
        }
        if(StringUtils.isNotBlank(key) && null == oldvalue && null != nvalue){
            if(null !=obj){
                obj.put(key,nvalue);
            }
            return nvalue;
        }
        if(null != oldvalue && oldvalue instanceof String){
            if(StringUtils.isNotBlank(nvalue)) {
                if(!oldvalue.equals(nvalue)) {
                    if(null != obj) {
                        obj.put(key, nvalue);
                    }
                    return nvalue;
                }
            }else{
                String oldlan = getLanguage((String)oldvalue);
                String v =  chgLanguage((String)oldvalue,oldlan,lan);
                obj.put(key,v);
                return v;
            }
            return oldvalue.toString();
        }*/
        return null;

        //todo other type to i18n change
    }

    /**
     * 判断一个字符串的语言
     * @param v
     * @return
     */
    String getLanguage(String v){
        return StringUtils.getLanguage(v);
    }

    /**
     * 把字符串从一种语言转换为另一种语言
     * @param v
     * @param oldLan
     * @param newLan
     * @return
     */
    String chgLanguage(String v,String oldLan,String newLan){
        if(StringUtils.isNotBlank(v) && StringUtils.isNotBlank(newLan)){
            if(StringUtils.isNotBlank(getXML().getProperties().getProperty("isfrominternet"))) {
                String s = com.octopus.tools.translate.BaiDuTranslate.translate(v, oldLan, newLan);
                if (StringUtils.isNotBlank(s)) {
                    Map m = StringUtils.convert2MapJSONObject(s);
                    if (null != m && null != m.get("trans_result")) {
                        return (String) ((java.util.LinkedHashMap) ((java.util.LinkedList) m.get("trans_result")).get(0)).get("dst");
                    }
                }
            }
        }
        return v;
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
