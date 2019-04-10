package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.expression.ExpressionParse;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.IPointParse;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-6-10
 * Time: 上午8:38
 */
public class PointParseCalculate implements IPointParse {
    transient static Log log = LogFactory.getLog(PointParseCalculate.class);
    @Override
    public String parse(String str, Map data,XMLObject obj) {
        String t = str.substring(2,str.length()-1);

        try {
            //long l = System.currentTimeMillis();
            //t = StringUtils.toXMLRetainChar(t);
            if(XMLParameter.isHasRetainChars(t,XMLParameter.FilterTagsBegin)){
                return str;
            }
            String s= String.valueOf(ExpressionParse.parse(t));
            //System.out.println("cal:"+(System.currentTimeMillis()-l));
            if(s.equals("null")){
                return "null";
            }
            log.debug("RET:"+data.get("${table}")+"    "+s+"   "+t+"\n"+data.get("${olddata}")+"\n"+data.get("${newdata}"));
            //System.out.println("cal:"+(System.currentTimeMillis()-l));
            return s;
        } catch (Exception e) {
            log.error("calculate :"+str,e);
        }
        return str;
    }



    public static void main(String[] args){
        try{
            XMLParameter x = new XMLParameter();
            /*HashMap m = new HashMap();
            m.put("cprNumber","");
            m.put("regType","2");
            m.put("snsId","");
            x.addParameter("${input_data}",m);
            String xml = "{mi:'1'," +
                    "tt:" +
                    "'case(" +
                    "#{(${input_data}.regType)!1&(${input_data}.regType)!5}" +
                    "," +
                    "[" +
                    "[1,\"[{IdentifyValue:(${input_data}.snsId),IdentifyType:case((${input_data}.regType),[[2,1],[3,2],[4,3]])}]\"]])'}";
            Object o = x.getExpressValueFromMap(xml);
            System.out.println(o);
            Map map = StringUtils.convert2MapJSONObject(xml);
            o = x.getMapValueFromParameter(map);
            System.out.println(o);*/
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
