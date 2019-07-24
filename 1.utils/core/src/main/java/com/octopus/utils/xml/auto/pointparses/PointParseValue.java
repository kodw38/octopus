package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.exception.ISPException;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-6-8
 * Time: 下午4:37
 * getvalue(list,condExpress,value)
 * getvalue(object,path)
 * getvalue(object)
 */
public class PointParseValue implements IPointParse {
    static transient Log log = LogFactory.getLog(PointParseValue.class);
    @Override
    public String parse(String str, Map data,XMLObject obj) throws ISPException {
        String tm =str.substring("getvalue(".length(), str.length() - 1);
        String[] ts = tm.split(",");
        if(ts.length==3){
            if(ts[0].startsWith("${")){
                Object o = ObjectUtils.getValueByPath(data,ts[0]);
                if(null == o)return str;
                Object target=null;
                if(StringUtils.isNotBlank(ts[1])){
                    if(o instanceof Collection){
                        Iterator its = ((Collection)o).iterator();
                        while(its.hasNext()){
                            Object r = its.next();
                            data.put("${each}",r);
                            Object ov = ObjectUtils.getExpressValueFromMap(ts[1], XMLParameter.NestTagsBegin,XMLParameter.NestTagsEnd,data,XMLParameter.PointParses,obj);

                            if(null != ov && StringUtils.isTrue(ov.toString())){
                                target = r;
                                break;
                            }/*else{
                                log.error("getvalue cond is false:"+ts[1]);
                            }*/
                        }
                        data.remove("${each}");
                    }
                    if(o.getClass().isArray()){
                        Object[] its = ((Object[])o);
                        for(Object r:its){
                            data.put("${each}",r);
                            Object ov = ObjectUtils.getExpressValueFromMap(ts[1], XMLParameter.NestTagsBegin,XMLParameter.NestTagsEnd,data,XMLParameter.PointParses,obj);
                            if(null != ov && StringUtils.isTrue(ov.toString())){
                                target = r;
                                break;
                            }
                        }
                        data.remove("${each}");
                    }
                }
                if(null != target && StringUtils.isNotBlank(ts[2])){
                    try{
                        Object v = ObjectUtils.getValueByPath(target,ts[2]);
                        if(null != v){
                            return v.toString();
                        }
                    }catch (Exception e){
                        log.error("["+o+"["+ts[2]+"] ",e);
                    }
                }else {
                    return o.toString();
                }



            }else if(XMLParameter.isHasRetainChars(ts[0])){
                return "";
            }else{
                return tm;
            }
        }else if(ts.length==2){
            if(ts[0].startsWith("${")){
                Object o = ObjectUtils.getValueByPath(data,ts[0]);
                if(null == o)return "";
                String v =  (String)ObjectUtils.getValueByPath(o,ts[1]);
                if(null == v)
                    return "";
                else
                    return v;
            }else if(XMLParameter.isHasRetainChars(ts[0])){
                return "";
            }else{
                return tm;
            }

        }else if(ts.length==1){
            if(ts[0].startsWith("${")){
                Object o = ObjectUtils.getValueByPath(data,ts[0]);
                if(o==null)
                    return "";
                else
                    return o.toString();
            }
            if(XMLParameter.isHasRetainChars(ts[0])){
                return "";
            }else if(StringUtils.isBlank(ts[0]) || "null".equalsIgnoreCase(ts[0])){
                return "";
            }else{
                return ts[0];
            }
        }else{
            return tm;
        }
        return str;
    }


}
