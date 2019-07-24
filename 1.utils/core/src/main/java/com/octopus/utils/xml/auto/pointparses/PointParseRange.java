package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cls.ClassUtils;
import com.octopus.utils.exception.ExceptionUtil;
import com.octopus.utils.exception.ISPException;
import com.octopus.utils.time.DateTimeUtils;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-6-8
 * Time: 下午6:39
 */
public class PointParseRange implements IPointParse {
    transient static Log log = LogFactory.getLog(PointParseRange.class);
    @Override
    public String parse(String str, Map data,XMLObject obj) throws ISPException{
        String t = str.substring(1,str.length()-1);
        if(t.equals("${msg}")){
            //System.out.println();
        }
        if("${yyyymmdd}".equals(t)){
            return DateTimeUtils.getNoLineYYYYMMDD(new Date());
        }
        if("${yyyy-mm-dd}".equals(t)){
            return DateTimeUtils.getYYYYMMDD(new Date());
        }
        if("${system-HH_MM_SS}".equals(t)){
            try{
                String pattern = t.substring(0,t.length()-1).split("-")[1];
                return DateTimeUtils.getCurrHHMMSS(pattern);
            }catch (Exception e){
                return str;
            }
        }
        if(t.contains("${queryStringMap}")){
            //System.out.println();
        }
        if(t.startsWith("${")){
            List<String> m = StringUtils.getTagsNoMark(t,"${","}");
            if(null != m){
                String tim = m.get(0);
                if("first_day_of_month".equals(tim)){
                    return DateTimeUtils.getFirstDayOfMonth(new Date()).toString();
                }
                if("last_day_of_month".equals(tim)){
                    return DateTimeUtils.getLastDayOfMonth(new Date()).toString();
                }
                if(StringUtils.isNotBlank(tim) && DateTimeUtils.isDatePattern(tim)){
                    String date= DateTimeUtils.getStringDate(tim,null);
                    //System.out.println("=="+date);
                    return date;
                }
            }
        }
        if("${systemdate}".equals(t)){
            try{
                return DateTimeUtils.getCurrDateTime();
            }catch (Exception e){
                return str;
            }
        }

        if("${systemtime}".equals(t)){
            try{
                Object o = data.get("${context}");
                if(null != o){
                    try {
                        String tem = (String)ClassUtils.invokeMethod(o, "getSystemDate", null, null);
                        return tem;
                    }catch (Exception e){

                    }
                }
                Date d = new Date();
                return String.valueOf(Integer.parseInt(d.getHours()+""+((d.getMinutes()+"").length()==1?("0"+d.getMinutes()):(d.getMinutes()+""))+((""+d.getSeconds()).length()==1?("0"+d.getSeconds()):(""+d.getSeconds()))));
            }catch (Exception e){
                return str;
            }
        }

        /*if(XMLParameter.isHasRetainChars(t,XMLParameter.NestTagsBegin)){
            return str;
        }*/
        try {
            String ret=null;
            if(t.startsWith("@{") && null != obj){
                List li = StringUtils.getTagsNoMark((String)t,"@{","}");
                if(null !=li && li.size()>0){
                    Object o = obj.getObjectById((String) li.get(0));
                    if(null != o){

                        String p = ((String)t).substring(((String)t).indexOf("}")+2);

                        if(o instanceof XMLDoObject && p.startsWith("do(")&&p.endsWith(")")){
                            Object v = null;
                            if(p.length()==4){
                                try {
                                    XMLParameter pars = ((XMLDoObject) o).getEmptyParameter();
                                    pars.putAll(data);
                                    ((XMLDoObject) o).doThing(pars, null);
                                    v = ((ResultCheck)pars.getResult()).getRet();
                                }catch (Exception e){}
                            }else{
                                //execute object doThing and get string return
                                List ss = StringUtils.getTagsNoMark(p,"do(",")");
                                if(null != ss && ss.size()>0) {
                                    try {
                                        XMLParameter pars = ((XMLDoObject) o).getEmptyParameter();
                                        pars.putAll(data);
                                        Map input = StringUtils.convert2MapJSONObject((String)ss.get(0));
                                        input = pars.getManualMapValueFromParameter(input,(XMLDoObject)o);
                                        pars.put("^${input}",input);
                                        ((XMLDoObject) o).doThing(pars, null);
                                        v = ((ResultCheck)pars.getResult()).getRet();
                                    }catch (Exception e){
                                        throw ExceptionUtil.getRootCase(e);
                                    }
                                }
                            }
                            if(null != v){
                                ret = v.toString();
                            }
                        }else {
                            Object r = ObjectUtils.getValueByPath(o, p);
                            log.debug("get object properties path:" + p + " result:" + r);
                            if (null != r)
                                ret = r.toString();
                        }
                    }
                }else{
                    ret = ObjectUtils.getStringValueByPath(data, t);
                }
            }else {
                ret = ObjectUtils.getStringValueByPath(data, t);
            }
            if (null != ret) {
                return ret;
            } else {
                return str;
            }
        }catch (ISPException e){
            log.error("PointParseRange",e);
            throw e;
        }catch (Throwable e){
            log.error("PointParseRange",e);
        }
        return str;
    }


}
