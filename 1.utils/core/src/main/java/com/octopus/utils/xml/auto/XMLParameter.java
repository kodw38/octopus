package com.octopus.utils.xml.auto;

import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.SNUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.exception.ExceptionUtil;
import com.octopus.utils.exception.ISPException;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.logic.XMLLogic;
import com.octopus.utils.xml.auto.pointparses.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sun.misc.BASE64Decoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.*;

/**
 * User: Administrator
 * Date: 14-9-3
 * Time: 上午10:33
 */
public class XMLParameter extends ParameterMap implements Serializable {
    public static int HAPPEN_TIMEOUT_EXCEPTION = 9;//超时服务调用发生超时
    public static int HAPPEN_EXCEPTION = 3;//异常
    public static int HAPPEN_TIMEOUT = 5;//超时
    public static int HAPPEN_NOTIFICATION = 1;//主动中断，返回信息，让异步过程处理再发送消息通知
    public static int HAPPEN_PREJUDGETIMEOUT = 4;//预计超时
    public static int TIMEOUT_DELETE = 10;//超时后需要删除
    //public static int SAVE_REDO_TIMEOUT_EXCEPTION = 10;//已经保存超时的redo
    public static String XMLLOGIC_BACK_CALL_KEY="${backCall}";
    private static String TRADE_ID="${transactionid}";
    private static String TRADE_ASYN_NOTIFY_ADDRESS="${notifyaddress}";
    transient static Log log = LogFactory.getLog(XMLParameter.class);
    protected boolean isError=false;
    protected Throwable exception;
    //ParameterMap allParameter = new ParameterMap();
    static ParameterMap staticParameter = new ParameterMap();
    List<Object[]> tradeList = new LinkedList();
    ParameterMap go = new ParameterMap();

    public static char[][] NestTagsBegin = new char[][]{
            "getallparameters(".toCharArray()
            ,"isnotnullbypath(".toCharArray()
            ,"base64_encode(".toCharArray()
            ,"geterrortrace(".toCharArray()
            ,"substrnotag(".toCharArray()
            ,"isnotnull(".toCharArray()
            ,"startwith(".toCharArray()
            ,"numformat(".toCharArray()
            ,"classtype(".toCharArray()
            ,"notexist(".toCharArray()
            ,"getvalue(".toCharArray()
            ,"contains(".toCharArray()
            ,"indexof(".toCharArray()
            ,"decrypt(".toCharArray()
            ,"encrypt(".toCharArray()
            ,"isrnull(".toCharArray()
            ,"substr(".toCharArray()
            ,"isnull(".toCharArray()
            ,"ifnull(".toCharArray()
            ,"tojson(".toCharArray()
            ,"todate(".toCharArray()
            ,"remain(".toCharArray()
            ,"format(".toCharArray()
            ,"varcal(".toCharArray()
            ,"getvar(".toCharArray()
            ,"upper(".toCharArray()
            ,"lower(".toCharArray()
            ,"exist(".toCharArray()
            ,"times(".toCharArray()
            ,"case(".toCharArray()
            ,"len(".toCharArray()
            ,"#{".toCharArray()
            ,"#(".toCharArray()
            ,"(".toCharArray()
    };

    public static char[][] NestTagsEnd = new char[][]{
            ")".toCharArray()
            ,")".toCharArray()
            ,")".toCharArray()
            ,")".toCharArray()
            ,")".toCharArray()
            ,")".toCharArray()
            ,")".toCharArray()
            ,")".toCharArray()
            ,")".toCharArray()
            ,")".toCharArray()
            ,")".toCharArray()
            ,")".toCharArray()
            ,")".toCharArray()
            ,")".toCharArray()
            ,")".toCharArray()
            ,")".toCharArray()
            ,")".toCharArray()
            ,")".toCharArray()
            ,")".toCharArray()
            ,")".toCharArray()
            ,")".toCharArray()
            ,")".toCharArray()
            ,")".toCharArray()
            ,")".toCharArray()
            ,")".toCharArray()
            ,")".toCharArray()
            ,")".toCharArray()
            ,")".toCharArray()
            ,")".toCharArray()
            ,")".toCharArray()
            ,")".toCharArray()
            ,"}".toCharArray()
            ,")".toCharArray()
            ,")".toCharArray()

    };
    public static IPointParse[] PointParses = {
             new PointParseGetAllParameters()
            ,new PointParseNotNullPath()
            ,new PointParseBase64()
            ,new PointParseGetErrorTrace()
            ,new PointParseSubStrNoTag()
            ,new PointParseNotNull()
            ,new PointParseStartwith()
            ,new PointParseNumFormat()
            ,new PointParseClassType()
            ,new PointParseNotExist()
            ,new PointParseValue()
            ,new PointParseContains()
            ,new PointParseIndexOf()
            ,new PointParseDecrypt()
            ,new PointParseEncrypt()
            ,new PointParseNullNotCal()
            ,new PointParseSubStr()
            ,new PointParseNull()
            ,new PointParseIfNull()
            ,new PointParseToJson()
            ,new PointParseToDate()
            ,new PointParseRemain()
            ,new PointParseFormat()
            ,new PointParseVarCal()
            ,new PointParseVar()
            ,new PointParseUpperCase()
            ,new PointParseLowerCase()
            ,new PointParseExist()
            ,new PointParseTimes()
            ,new PointParseCase()
            ,new PointParseLength()
            ,new PointParseCalculate()
            ,new PointParseRule()
            ,new PointParseRange()
    };
    //return is a String
    public static char[][] StringReturnNestTagsBegin = new char[][]{
            "getallparameters(".toCharArray()
            ,"base64_encode(".toCharArray()
            ,"geterrortrace(".toCharArray()
            ,"substrnotag(".toCharArray()
            ,"indexof(".toCharArray()
            ,"decrypt(".toCharArray()
            ,"encrypt(".toCharArray()
            ,"substr(".toCharArray()
            ,"tojson(".toCharArray()
            ,"getvar(".toCharArray()
            ,"upper(".toCharArray()
            ,"lower(".toCharArray()
            ,"(".toCharArray()
    };
    static char[][] AllTagsBegin = new char[][]{
            "getallparameters(".toCharArray()
            ,"isnotnullbypath(".toCharArray()
            ,"base64_encode(".toCharArray()
            ,"geterrortrace(".toCharArray()
            ,"substrnotag(".toCharArray()
            ,"isnotnull(".toCharArray()
            ,"startwith(".toCharArray()
            ,"numformat(".toCharArray()
            ,"classtype(".toCharArray()
            ,"notexist(".toCharArray()
            ,"getvalue(".toCharArray()
            ,"contains(".toCharArray()
            ,"indexof(".toCharArray()
            ,"decrypt(".toCharArray()
            ,"encrypt(".toCharArray()
            ,"isrnull(".toCharArray()
            ,"substr(".toCharArray()
            ,"isnull(".toCharArray()
            ,"ifnull(".toCharArray()
            ,"tojson(".toCharArray()
            ,"todate(".toCharArray()
            ,"remain(".toCharArray()
            ,"format(".toCharArray()
            ,"varcal(".toCharArray()
            ,"getvar(".toCharArray()
            ,"upper(".toCharArray()
            ,"lower(".toCharArray()
            ,"exist(".toCharArray()
            ,"times(".toCharArray()
            ,"case(".toCharArray()
            ,"len(".toCharArray()
            ,"${".toCharArray()
            ,"#{".toCharArray()
            ,"#(".toCharArray()};

    public static char[][] FilterTagsBegin = new char[][]{
            "getallparameters(".toCharArray()
            ,"isnotnullbypath(".toCharArray()
            ,"base64_encode(".toCharArray()
            ,"geterrortrace(".toCharArray()
            ,"substrnotag(".toCharArray()
            ,"isnotnull(".toCharArray()
            ,"startwith(".toCharArray()
            ,"numformat(".toCharArray()
            ,"classtype(".toCharArray()
            ,"getvalue(".toCharArray()
            ,"contains(".toCharArray()
            ,"notexist(".toCharArray()
            ,"indexof(".toCharArray()
            ,"decrypt(".toCharArray()
            ,"encrypt(".toCharArray()
            ,"isrnull(".toCharArray()
            ,"isnull(".toCharArray()
            ,"ifnull(".toCharArray()
            ,"tojson(".toCharArray()
            ,"todate(".toCharArray()
            ,"remain(".toCharArray()
            ,"format(".toCharArray()
            ,"varcal(".toCharArray()
            ,"getvar(".toCharArray()
            ,"substr(".toCharArray()
            ,"upper(".toCharArray()
            ,"lower(".toCharArray()
            ,"exist(".toCharArray()
            ,"times(".toCharArray()
            ,"case(".toCharArray()
            ,"len(".toCharArray()
            ,"#{".toCharArray()
            ,"#(".toCharArray()
            ,"${".toCharArray()

    };

    static HashMap NestMap = new HashMap();
    static {
        NestMap.put("getallparameters(",")");//获取环境所有参数json字符串
        NestMap.put("isnotnullbypath(",")");//根据路径获取到的末尾值不能为空
        NestMap.put("base64_encode(",")");//base64字符编码
        NestMap.put("geterrortrace(",")");//base64字符编码
        NestMap.put("substrnotag(",")");//字符运算
        NestMap.put("isnotnull(",")");//base64字符编码
        NestMap.put("startwith(",")");//以什么开始的字符
        NestMap.put("numformat(",")");//
        NestMap.put("classtype(",")");//
        NestMap.put("getvalue(",")");//base64字符编码
        NestMap.put("contains(",")");//base64字符编码
        NestMap.put("notexist(",")");//base64字符编码
        NestMap.put("indexof(",")");//base64字符编码
        NestMap.put("decrypt(",")");//base64字符编码
        NestMap.put("encrypt(",")");//base64字符编码
        NestMap.put("isrnull(",")");//base64字符编码
        NestMap.put("substr(",")");//字符运算
        NestMap.put("isnull(",")");//base64字符编码
        NestMap.put("ifnull(",")");//base64字符编码
        NestMap.put("tojson(",")");//base64字符编码
        NestMap.put("todate(",")");//base64字符编码
        NestMap.put("remain(",")");//base64字符编码
        NestMap.put("format(",")");//base64字符编码
        NestMap.put("varcal(",")");//base64字符编码
        NestMap.put("getvar(",")");//base64字符编码
        NestMap.put("upper(",")");
        NestMap.put("lower(",")");
        NestMap.put("exist(",")");//base64字符编码
        NestMap.put("times(",")");//base64字符编码
        NestMap.put("case(",")");//字符运算
        NestMap.put("len(",")");//字符运算
        NestMap.put("#{","}");  //规则表达式
        NestMap.put("#(",")");  //规则表达式
        NestMap.put("${","}");  //变量
        NestMap.put("(",")");   //值域
    }
    //判断表达式是否是逻辑表达式
    public static boolean isBooleanPoint(String exper){
        if(exper.startsWith("#{") || exper.startsWith("#("))
            return true;
        return false;
    }
    public static Object newInstance(XMLMakeup xml,Class c,Object data,boolean isGlobal,XMLObject obj)throws ISPException{
        XMLParameter x = null;
        try {
            x = (XMLParameter)c.newInstance();
            setProperties(x,xml,data,isGlobal,obj);
            x.init();
            return x;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     *  从XMLMakeup转换为XLParameter数据结构
     *  "key"为属性名称，txt为属性值
     *  可以有上下级
     *  <a>
     *      <property key="k1">v1</property>
     *      <property key="k2">v2</property>
     *      <b>
     *          <property key="k3">v3</property>
     *          <property key="k4">v4</property>
     *      </b>
     *  </a>
     */

    public static void setProperties(Map p,XMLMakeup x,Object data,boolean isGlobal,XMLObject obj)throws ISPException{
       if(null != x && StringUtils.isNotBlank(x.getId())){
           if(x.getChildren().size()>0){
               if(!x.getId().equals(x.getName())) {
                   HashMap t = new HashMap();
                   for (XMLMakeup sx : x.getChildren())
                       setProperties(t, sx,data,isGlobal,obj);
                   if (t.size() > 0) {
                       p.put(x.getId(), t);
                   }
               }else{
                   for (XMLMakeup sx : x.getChildren())
                     setProperty(p,sx,data,isGlobal,obj);
               }
           }else{
               setProperty(p, x,data,isGlobal,obj);
           }
       }
    }
    static void setProperty(Map p,XMLMakeup x,Object data,boolean isGlobal,XMLObject obj)throws ISPException{
        String key = x.getId();
        if(StringUtils.isNotBlank(key)) {
            if(null != data){
                String o = (String) ObjectUtils.getValueByPath(data, key);
                if(p instanceof ParameterMap && isGlobal){
                    if (null != o) {
                        ((ParameterMap)p).putGlobal(key, o);
                    } else {
                        ((ParameterMap)p).putGlobal(key, (null==x.getText()?"":getExpressValueFromMap(x.getText(),p,obj)));
                    }
                }else {
                    if (null != o) {
                        p.put(key, o);
                    } else {
                        p.put(key, (null==x.getText()?"":getExpressValueFromMap(x.getText(),p,obj)));
                    }
                }
            }else {
                if(p instanceof ParameterMap && isGlobal){
                    ((ParameterMap)p).putGlobal(key, (null==x.getText()?"":getExpressValueFromMap(x.getText(),p,obj)));
                }else {
                    p.put(key, (null==x.getText()?"":getExpressValueFromMap(x.getText(),p,obj)));
                }
            }
        }
    }

    public XMLParameter(){
        if(staticParameter.size()>0)
            super.putAll(staticParameter);
    }

    public void init (){
        //sub todo
    }

    /**
     *  解析参数Map中的Value值
     * @param jsonMap
     * @return
     */
    public Map getMapValueFromParameter(Map jsonMap,XMLObject obj)throws ISPException{
        return ObjectUtils.getValueMapFromMapDataByJsonMapExp(getReadOnlyParameter(),jsonMap,NestTagsBegin,NestTagsEnd,PointParses,StringReturnNestTagsBegin,null,obj);
    }

    /**
     * 根据begin字符启用部分转换功能
     * @param jsonMap
     * @param nestTagsBegins
     * @return
     */
    public Map getMapValueFromParameter(Map jsonMap,String[] nestTagsBegins,XMLObject obj)throws Exception{
        if(null == nestTagsBegins) {
            return ObjectUtils.getValueMapFromMapDataByJsonMapExp(getReadOnlyParameter(), jsonMap, NestTagsBegin, NestTagsEnd, PointParses, StringReturnNestTagsBegin, null,obj);
        }else{
            char[][] begins = new char[nestTagsBegins.length][];
            LinkedList ls=new LinkedList();
            for(int i=0;i<nestTagsBegins.length;i++){
                boolean notin=true;
                for(int j=0;j<NestTagsBegin.length;j++) {
                    if(new String(NestTagsBegin[j]).equals(nestTagsBegins[i])) {
                        begins[i] = nestTagsBegins[i].toCharArray();
                        notin=false;
                        ls.add(j);
                        break;
                    }
                }
                if(notin){
                    throw new Exception("not find function by begin tag [" + nestTagsBegins[i] + "]");

                }
            }
            Integer[] points = (Integer[])ls.toArray(new Integer[0]);
            char[][] ends = new char[nestTagsBegins.length][];
            IPointParse[] parse = new IPointParse[nestTagsBegins.length];
            for(int i=0;i<points.length;i++){
                ends[i]=NestTagsEnd[points[i]];
                parse[i]=PointParses[points[i]];
            }
            return ObjectUtils.getValueMapFromMapDataByJsonMapExp(getReadOnlyParameter(), jsonMap, begins, ends, parse, StringReturnNestTagsBegin, null,obj);
        }
    }

    //手工替换的符号，遇到这些需要手工转换
    static Map manualChars = new HashMap();
    static {
        manualChars.put("~(","(");
        manualChars.put("~{","{");
    }

    /**
     * 如果自动转换带有~，需要人工调用改方法转换
     * @param jsonMap
     * @return
     */
    public Map getManualMapValueFromParameter(Map jsonMap,XMLObject obj)throws ISPException{
        return ObjectUtils.getValueMapFromMapDataByJsonMapExp(getReadOnlyParameter(),jsonMap,NestTagsBegin,NestTagsEnd,PointParses,StringReturnNestTagsBegin,manualChars,obj);
    }

    /**
     *
     * @param data
     * @param jsonMap
     * @return
     */
    public static Map getMapValueFromParameter(Map data,Map jsonMap,XMLObject obj)throws ISPException{
        return ObjectUtils.getValueMapFromMapDataByJsonMapExp(data,jsonMap,NestTagsBegin,NestTagsEnd,PointParses,StringReturnNestTagsBegin,null,obj);
    }
    public static List getValueListFromMapDataByJsonMapExp(Map data,List strlist,XMLObject obj)throws ISPException{
        return ObjectUtils.getValueListFromMapDataByJsonMapExp(data,strlist,XMLParameter.NestTagsBegin,XMLParameter.NestTagsEnd,XMLParameter.PointParses,StringReturnNestTagsBegin,null,obj);
    }
    public static Object getExpressValueFromMap(String exp,Map data,XMLObject obj)throws ISPException{
        return ObjectUtils.getExpressValueFromMap(exp,XMLParameter.NestTagsBegin,XMLParameter.NestTagsEnd,data,XMLParameter.PointParses,obj);
    }
    public static Object getValueFromExpress(String exp,Map data,XMLObject obj)throws ISPException{
        return ObjectUtils.getValueFromExpress(exp,data,XMLParameter.NestTagsBegin,XMLParameter.NestTagsEnd,XMLParameter.PointParses,StringReturnNestTagsBegin,obj);
    }
    /**
     * 解析参数字符串
     * @param exp
     * @return
     */
    public Object getExpressValueFromMap(String exp,XMLObject obj)throws ISPException{
        return ObjectUtils.getExpressValueFromMap(exp,XMLParameter.NestTagsBegin,XMLParameter.NestTagsEnd,getReadOnlyParameter(),XMLParameter.PointParses,obj);
    }
    public Object getValueFromExpress(Object exp,XMLObject obj)throws ISPException{
        return ObjectUtils.getValueFromExpress(exp,getReadOnlyParameter(),XMLParameter.NestTagsBegin,XMLParameter.NestTagsEnd,XMLParameter.PointParses,StringReturnNestTagsBegin,obj);
    }

    public Object format(Object obj,Map map)throws Exception{
        try{
            if(null != obj){
                Object ret= map.get("return");
                if(null !=ret){
                    //Object o = ObjectUtils.getValueByPath(getReadOnlyParameter(),ret);
                    addParameter("${return}",ret);
                }
                Object r = getParameter("${return}");

                String decode = (String)map.get("decode");
                if(StringUtils.isNotBlank(decode)){
                    if(decode.equals("base64") && r instanceof String){
                       r = new BASE64Decoder().decodeBuffer(((String)r)) ;
                    }
                    if(decode.equals("base64") && r instanceof ByteArrayOutputStream){
                        r = new BASE64Decoder().decodeBuffer(new String(((ByteArrayOutputStream)r).toByteArray())) ;
                        //System.out.println("base64 "+new String(new String((byte[])r,"ISO-8859-1").getBytes(),"UTF-8"));
                        //System.out.println("base64 "+new String(new String((byte[])r)));
                        //System.out.println("base64 "+new String(new String((byte[])r,"GBK")));
                        //System.out.println("base64 "+new String(new String((byte[])r,"UTF-8")));
                        log.debug(r);
                    }
                }
                String charset = (String)map.get("charset");
                if(null !=charset){
                    if(StringUtils.isNotBlank(charset)){
                        if(r instanceof ByteArrayOutputStream){
                            //System.out.println("charset "+new String(new String(((ByteArrayOutputStream)r).toByteArray(),"ISO-8859-1").getBytes("ISO-8859-1"),"GB2312"));
                            //System.out.println("charset "+new String(((ByteArrayOutputStream)r).toByteArray()));
                            //System.out.println("charset "+new String(((ByteArrayOutputStream)r).toByteArray(),"GBK"));
                            //System.out.println("charset "+new String(((ByteArrayOutputStream)r).toByteArray(),"UTF-8"));
                            r = new String(((ByteArrayOutputStream)r).toByteArray(),charset);
                            //System.out.println(r);
                            //System.out.println(System.getProperties());
                        }else if(r instanceof byte[]){
                            r = new String((byte[])r,charset);
                        }else if(r instanceof String){
                            r = new String(((String)r).getBytes(),charset);
                        }
                    }else{
                        if(r instanceof ByteArrayOutputStream){
                            r = new String(((ByteArrayOutputStream)r).toByteArray());
                        }else if(r instanceof byte[]){
                            r = new String((byte[])r);
                        }
                    }
                }

                String clazz = (String)map.get("clazz");
                if(StringUtils.isNotBlank(clazz)){
                    Class target = Class.forName(clazz);
                    if(Map.class.isAssignableFrom(target) && r instanceof String){
                        r = StringUtils.convert2MapJSONObject((String)r);
                    }else if(List.class.isAssignableFrom(target) && r instanceof String){
                        r = StringUtils.convert2ListJSONObject((String)r);
                    }else if(ByteArrayInputStream.class.isAssignableFrom(target) && r instanceof ByteArrayOutputStream){
                        r = ObjectUtils.convert((ByteArrayOutputStream)r);
                    }
                }

                addParameter("${return}",r);
                return r;
            }
            return null;
        }catch (Exception e){
            throw new Exception("not support format " + obj, e);
        }
    }

    //根据值条件获取对象值，目前简单对象，集合对象的相等条件实现
    Object getCondValue(Object obj ,String cond,String getfield){
        Object ret=null;
        if(obj.getClass().isArray()){
            for(Object o:(Object[])obj){
                ret = getSingleCondValue(o,cond,getfield);
                if(null != ret)
                    return ret;
            }
        }
        if(Collection.class.isAssignableFrom(obj.getClass())){
            Iterator iits = ((Collection)obj).iterator();
            while(iits.hasNext()){
                ret = getSingleCondValue(iits.next(),cond,getfield);
                if(null != ret)
                    return ret;
            }
        }
         return null;
    }
    Object getSingleCondValue(Object o ,String cond,String getfield){
        String[] cs = cond.split(",");
        for(String c:cs){
            String[] fv = c.split("==");
            if(null == ObjectUtils.getValueByPath(o,fv[0]) || !ObjectUtils.getValueByPath(o,fv[0]).equals(fv[1])){
                return null;
            }
        }
        if(StringUtils.isNotBlank(getfield))
            return ObjectUtils.getValueByPath(o,getfield);
        else
            return o;
    }
    String expressReplace(Map temp,String orginalstr){
        Iterator<String> its = temp.keySet().iterator();
        while(its.hasNext()){
            String k = its.next();
            if(null != temp.get(k) && temp.get(k) instanceof String)
                orginalstr = StringUtils.replace(orginalstr,k,temp.get(k).toString());
        }
        return orginalstr;
    }


    public static boolean isHasRetainChars(String o){
        return ObjectUtils.isContainsExpressBeginFlag(o,AllTagsBegin);
    }
    public static boolean startWithRetainChars(String o){
        for(char[] s:AllTagsBegin){
            if(o.startsWith(new String(s)))
                return true;
        }
        return false;
    }
    public static boolean isHasRetainChars(String o,char[][] tags){
        return ObjectUtils.isContainsExpressBeginFlag(o,tags);
    }
    Object getRangeValue(String o,Object map){
        String tem = o;
        List<String> ls = StringUtils.getTagsIncludeMark((String)o,"(",")");
        if(null != ls && ls.size()>0){
            for(String s:ls){
                String t = s.substring(1,s.length()-1);
                Object r = ObjectUtils.getValueByPath(map,t);
                if(r instanceof String){
                    if(StringUtils.isNotBlank(r)){
                        tem = StringUtils.replace(tem,s,(String)r);
                    }else {
                        if(!isHasRetainChars(t)){
                            tem = StringUtils.replace(tem,s,t);
                        }
                    }
                }else {
                    return r;
                }
            }
        }
        return tem;
    }

    //设置当前发生异常的节点id
    public void setSuspendXMlId(String id){
        addGlobalParameter("^${SuspendXMLID}",id);
    }
/*
    //当服务调用超时时记录超时节点信息
    public void setTimeoutXMlId(String id){
        addGlobalParameter("^TimeoutXMlId",id);
    }
*/
    //记录请求过程中的状态
    public void setStatus(int n){
        addGlobalParameter("^ENV_STATUS",n);
    }
    public int getStatus(){
        Object n= getGlobalParameter("^ENV_STATUS");
        if(null ==n)return 0;
        if(n instanceof Integer){
            return (Integer)n;
        }
        if(n instanceof String){
            return Integer.parseInt((String)n);
        }
        return 0;
    }
/*
    public String getTimeoutXMlId(){
        return (String)getGlobalParameter("^TimeoutXMlId");
    }
*/

    public void setOnlyInputCheck(){
        addParameter("^${OnlyInputCheck}",true);
    }
    public boolean isOnlyInputCheck(){
        return getParameter("^${OnlyInputCheck}")==null?false:true;
    }
    public void removeOnlyInputCheck(){
        removeParameter("^${OnlyInputCheck}");
    }
    //获取发生异常节点的id
    public String getSuspendXMlId(){
        return (String)getGlobalParameter("^${SuspendXMLID}");
    }
    public void removeSuspendXMlId(){
        removeParameter("^${SuspendXMLID}");
    }
    //设置重做日志中的请求id，表明该请求是重做请求，在xmllogic中通过该标志已经做过了逻辑不需要再做
    public void setSuspend(String id){
        addParameter("^${SuspendActiveID}",id);
    }

    //判断当前是否是重做请求，且处理逻辑在已经处理的过程中
    public boolean isSuspend(){
        return containsKey("^${SuspendActiveID}");
    }
    public boolean idSuspendDo(){
        Object o = getParameter("^${SuspendActiveID_Do}");
        if(null !=o && o instanceof Boolean && (Boolean)o){
            return true;
        }else{
            return false;
        }
    }
    public void setSuspendDo(boolean b){
        addParameter("^${SuspendActiveID_Do}",b);
    }
    //删除重做标志，在成功处理完重做请求的异常节点后，删除该标志，删除重做日志，后续逻辑继续处理，如果继续发生异常重新记录重做日志
    public void removeSuspend(){
        remove("^${SuspendActiveID}");
    }
    //获取重做请求的id
    public String getSuspend(){
        return (String)getParameter("^${SuspendActiveID}");
    }

    //判断当前请求阶段是否设置了服务发生异常是否重做，只用在xmlLogic中过程逻辑
    public boolean isRedoService(){
        Boolean o =  (Boolean)getParameter("^BeginRedo");
        if(null != o)return o;
        return false;
    }
    //设置重做标志，后续子逻辑如果发生异常将记录重做，并抛出异常
    public void setRedoService(){
        addParameter("^BeginRedo",true);
    }
    public void removeRedoService(){
        removeParameter("^BeginRedo");
    }

    public String newTradeId(){
        return "TRADE_ID-"+SNUtils.getNewId();
    }
    public boolean addTradeTask(Object[] objs){
        return tradeList.add(objs);
    }
    public List<Object[]> getTradeTaskList(){
        return tradeList;
    }
    public void clearTradeTasks(){
        tradeList.clear();
    }

    public String getTradeId(){
        return (String)getParameter(TRADE_ID);
    }
    public boolean setTradeId(String tradeId){
        if(StringUtils.isNotBlank(tradeId)) {
            addParameter(TRADE_ID,tradeId);
            return true;
        }else{
            return false;
        }
    }

    /**
     * 从环境中移除该事务的内容
     * @return
     */
    public boolean removeTrade(){
        String id = getTradeId();
        if(StringUtils.isNotBlank(id)){
            removeParameter("TASKS_"+id);
            removeParameter("CONSOLE_"+id);
            removeParameter(TRADE_ID);
            tradeList.clear();

            return true;
        }
        return false;
    }
    public Map getTradeConsoles(){
        return (Map)getParameter("CONSOLE_"+getTradeId());
    }
    public String createTradeTaskId(String xmlid){
        return xmlid+"|"+SNUtils.getSequenceNum();
    }

    // 在一个tradeConsole中增加一个task的console
    public boolean addTradeConsole(String taskId,Object console){
        String s = getTradeId();
        if(StringUtils.isNotBlank(s)){
            Map m = getTradeConsoles();
            if(null == m){
                s="CONSOLE_"+s;
                m = new LinkedHashMap();
                addParameter(s,m);
            }
            m.put(taskId,console);
            return true;
        }
        return false;
    }


    //向当前事务中增加任务
    public boolean addTradeTask(XMLDoObject obj,String taskId,Object pars){
        String tradeId = getTradeId();
        if(StringUtils.isNotBlank(tradeId)){
            Map m = getTradeTasks();
            if(null == m){
                String s ="TASKS_"+ tradeId;
                m = new LinkedHashMap();
                addParameter(s,m);
            }
            m.put(taskId,new Object[]{obj,tradeId,taskId,pars,getParameter(TRADE_ASYN_NOTIFY_ADDRESS)});
            return true;
        }
        return false;
    }
    //获取当前事务的任务
    public Map getTradeTasks(){
        return (Map)getParameter("TASKS_"+getTradeId());
    }

    public Object getResult() {
            return getParameter("${result}");
    }
    public Object getThreadResult(String name){
        return getParameter("${result}["+name+"]");
    }
    public void setBreakPoint(String key){
        go.put("${break_point}",key);
    }
    public void setAutoProcess(boolean is){
        addParameter("^isautoprocess_",is);
    }
    public boolean isAutoProcess(){
        Boolean o = (Boolean)getParameter("^isautoprocess_");
        if(null !=o)
            return o;
        return false;
    }
    public String getBreakPoint(){
        return (String)go.getSub("${break_point}");
    }
    public void removeBreakPoint(){
        go.removeSub("${break_point}");
    }
    public Object getInputParameter(){
        return getParameter("${input_data}");
    }
    public void setInputParameter(Object par){
        addGlobalParameter("${input_data}",par);
    }

    public String[] getTargetNames() {
        if(getParameter("${targetNames}") instanceof List){
            return (String[])((List) getParameter("${targetNames}")).toArray(new String[0]);
        }else {
            return (String[]) getParameter("${targetNames}");
        }
    }

    public void setTargetNames(String[] targetNames) {
        addParameter("${targetNames}",targetNames);
    }

    public boolean containsParameter(String key){
        return  containsKey(key);
    }


    public void setResult(Object ret) {
        addParameter("${result}",ret);
/*
        this.result = ret;
        setText(StringUtils.toXMLShiftChar(ret.toString()));
*/
    }

    public void setResult(String path,Object ret){
        addParameter(path,ret);
/*
        ObjectUtils.setValue(this,null,path,ret);
*/
    }

    public boolean isError() {
        if(StringUtils.isNotBlank(get("^iserror"))){
            isError=Boolean.valueOf((String)get("^iserror"));
        }
        return isError;
    }

    public void setError(boolean error) {
        isError = error;
        put("^iserror", String.valueOf(error));
    }

    public void addXmlParameter(XMLMakeup xmlParameter,boolean isGlobal,XMLObject obj)throws ISPException{
        if(null != xmlParameter){
            HashMap m = new HashMap();
            setProperties(m,xmlParameter,null,isGlobal,obj);
            if(m.size()>0){
                put(xmlParameter.getId(),m);
            }
        }
    }
    public void addParameter(String key,Object value){
        if(null == value){
            remove(key);
        }else{
            put(key, value);
        }
    }

    public void addGlobalParameter(String key,Object value){
        if(null == value){
            remove(key);
        }else{
            putGlobal(key, value);
        }
    }
    public void addAuthInfo(Object value){
        addGlobalParameter("${AuthInfo}",value);
    }
    public Map getAuthInfo(){
        Object o = getGlobalParameter("${AuthInfo}");
        if(null != o && o instanceof Map){
            return (Map)o;
        }
        return null;
    }
    public Object getGlobalParameter(String key){
        return getGlobal(key);
    }
    public void addStaticParameter(String key,Object value){
        if(null == value){
            staticParameter.remove(key);
            remove(key);
        }else{
            staticParameter.putGlobal(key,value);
            putGlobal(key, value);
        }
    }
    public void removeParameter(String key){
        remove(key);
    }
    public void removeStaticParameter(String key){
        staticParameter.remove(key);
        remove(key);
    }
    public void removeParameter(String threadName,String key){
        remove(threadName, key);
    }
    public Object getParameter(String key){
        return get(key);
    }
    public static Object getStaticParameter(String key){
        return staticParameter.get(key);
    }
    public boolean existStatic(String key){
        return staticParameter.containsKey(key);
    }

    public Map getReadOnlyParameter(){
        return this;
    }

    public Throwable getException() {
        if(null == exception && isError() && StringUtils.isNotBlank(get("^Exception"))){
            exception = new Exception((String)get("^Exception"));
        }
        return exception;
    }
    public void putAll(Map map){
        super.putAll(map);
    }
    public void setException(Throwable exception) {
        this.exception = exception;
        if(null != exception)
            put("^Exception", StringUtils.toXMLShiftChar(ExceptionUtil.getRootString(exception)));
    }

    protected String getExceptionStackTrace(Throwable e) {
        StringBuffer sb = new StringBuffer();
        sb.append(e);
        StackTraceElement[] trace = e.getStackTrace();
        for (int i = 0; i < trace.length; i++)
            sb.append("\n " + trace[i]);

        Throwable ourCause = e.getCause();
        if (ourCause != null)
            getCauseStackTrace(sb, ourCause);
        return sb.toString();
    }
    void getCauseStackTrace(StringBuffer sb, Throwable cause) {
        if (null != cause) {
            StackTraceElement[] trace = cause.getStackTrace();
            for (int i = 0; i < trace.length; i++)
                sb.append("\n " + trace[i]);
            Throwable ourCause = cause.getCause();
            if (ourCause != null)
                getCauseStackTrace(sb, ourCause);
        }
    }

    /*public void printTime(String message){
        if(log.isDebugEnabled()) {
            log.debug(get("${requestId}") + "    " + System.currentTimeMillis() + "  " + message);
        }
    }*/
    public Object getParameterWithoutThreadName(String key){
        Object o = getParameter(key);
        if(null != o){
            return o;
        }else{
            Iterator its = keySet().iterator();
            while(its.hasNext()){
                Object k = its.next();
                if(null != k && k.toString().startsWith(key+"[")){
                    return get(k);
                }
            }
        }
        return null;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }


    public String getLoginUserName()throws ISPException{
       return (String)getValueFromExpress("${session}.UserName",null);
    }

}
