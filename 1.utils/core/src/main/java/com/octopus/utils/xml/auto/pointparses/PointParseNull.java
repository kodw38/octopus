package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.List;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-9-11
 * Time: 下午5:09
 */
public class PointParseNull implements IPointParse {
    @Override
    public String parse(String str, Map data,XMLObject obj) {
        try{
            String tm =str.substring("isnull(".length(), str.length() - 1);
            if(StringUtils.isNotBlank(tm)){
                if(tm.startsWith("${")||tm.startsWith("^${")){
                    Object o = ObjectUtils.getValueByPath(data, tm);
                    if(o==null)
                        return "true";
                    else if(o instanceof String && StringUtils.isBlank((String)o))
                        return "true";
                    //注释掉的原因,页面传了个数据,里面有个字段包含了TB运算符,要保存数据库. 过程中用isnull判断该字段是否为null,应该不为空,但错误返回true了
                    else if(o instanceof String && StringUtils.isNotBlank(o) && XMLParameter.isHasRetainChars((String)o)){
                        return "true";
                    }
                    if(o instanceof List){
                        if(((List)o).size()==0) return "true";
                        int i=0;
                        for(;i<((List)o).size();i++){
                            if(null !=((List)o).get(i)) break;
                        }
                        if(i==((List)o).size()){
                            return "true";
                        }
                    }else if(o instanceof Map){
                        if(((Map)o).size()==0) return "true";
                    }
                    return "false";
                }else if(XMLParameter.isHasRetainChars(tm)){
                    return "true";
                }else{
                   return "false";
                }
            }

            return "false";
        }catch (Exception e){
            e.printStackTrace();
            return "false";
        }
    }
}