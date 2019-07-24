package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.exception.ISPException;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * (obj,{each})
 *  第二个参数是逻辑表达式，为true时从列表中保存
 * User: wfgao_000
 * Date: 15-12-30
 * Time: 下午9:22
 */
public class PointParseRemain implements IPointParse {
    transient static Log log = LogFactory.getLog(PointParseRemain.class);
    @Override
    public String parse(String str, Map data,XMLObject obj)throws ISPException {
        String tm =str.substring("remain(".length(), str.length() - 1);
        //System.out.println("=1:"+tm);
        String[] ts = StringUtils.splitExcludeToken(tm,",",new char[][]{{'\'','\''},{'\"','\"'},{'{','}'}},true);
        if(ts.length==2){
            //System.out.println("=2:"+ts[0]);
            if(ts[0].startsWith("${")){
                Object o = ObjectUtils.getValueByPath(data, ts[0]);
                if(null == o)return str;
                //System.out.println("=3:"+ts[1]);
                if(StringUtils.isNotBlank(ts[1])){
                    List list = new ArrayList();
                    if(o instanceof Collection){
                        Iterator its = ((Collection)o).iterator();
                        while(its.hasNext()){
                            Object r = its.next();
                            data.put("${each}",r);
                            //System.out.println("=each:"+r);
                            String t = StringUtils.replace(ts[1],"~","");
                            Object ov = ObjectUtils.getExpressValueFromMap(t, XMLParameter.NestTagsBegin,XMLParameter.NestTagsEnd,data,XMLParameter.PointParses,obj);
                            //System.out.println("=4:"+ov);
                            if(null != ov && !StringUtils.isTrue(ov.toString())){
                                list.add(r);
                            }
                        }
                        if(list.size()>0){
                            for(int i=0;i<list.size();i++){
                                ((Collection)o).remove(list.get(i));
                            }
                        }
                        data.remove("${each}");
                    }

                    if(o.getClass().isArray()){
                        log.error("now not support filter array");
                    }
                }
            }
        }
        return str;
    }
}
