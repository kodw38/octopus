package com.octopus.utils.xml.auto.pointparses;

import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.time.DateTimeUtils;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.XMLUtil;
import com.octopus.utils.xml.auto.IPointParse;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sun.misc.BASE64Decoder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 15-12-31
 * Time: 上午10:03
 */
public class PointParseFormat implements IPointParse {
    transient static Log log = LogFactory.getLog(PointParseFormat.class);
    @Override
    public String parse(String str, Map data,XMLObject obj) {
        try{
            String tm =str.substring("format(".length(), str.length() - 1);
            if(tm.startsWith("{")) {
                tm = StringUtils.replace(tm, "\\'", "'");
                Map map = StringUtils.convert2MapJSONObject(tm);
                if (null != map && map.size() > 0 && null != data.get("${return}")) {
                    if (StringUtils.isNotBlank((String) map.get("return")) && null != data.get("${return}")) {
                        Object ret = ObjectUtils.getValueByPath(data, (String) map.get("return"));
                        data.put("${return}", ret);
                    }
                    Object r = data.get("${return}");

                    String decode = (String) map.get("decode");
                    if (StringUtils.isNotBlank(decode)) {
                        if (decode.equals("base64") && r instanceof String) {
                            r = new BASE64Decoder().decodeBuffer(((String) r));
                        }
                        if (decode.equals("base64") && r instanceof ByteArrayOutputStream) {
                            r = new BASE64Decoder().decodeBuffer(new String(((ByteArrayOutputStream) r).toByteArray()));
                            //System.out.println("base64 "+new String(new String((byte[])r,"ISO-8859-1").getBytes(),"UTF-8"));
                            //System.out.println("base64 "+new String(new String((byte[])r)));
                            //System.out.println("base64 "+new String(new String((byte[])r,"GBK")));
                            //System.out.println("base64 "+new String(new String((byte[])r,"UTF-8")));
                            log.debug(r);
                        }
                    }
                    String charset = (String) map.get("charset");
                    if (null != charset) {
                        if (StringUtils.isNotBlank(charset)) {
                            if (r instanceof ByteArrayOutputStream) {
                                //System.out.println("charset "+new String(new String(((ByteArrayOutputStream)r).toByteArray(),"ISO-8859-1").getBytes("ISO-8859-1"),"GB2312"));
                                //System.out.println("charset "+new String(((ByteArrayOutputStream)r).toByteArray()));
                                //System.out.println("charset "+new String(((ByteArrayOutputStream)r).toByteArray(),"GBK"));
                                //System.out.println("charset "+new String(((ByteArrayOutputStream)r).toByteArray(),"UTF-8"));
                                r = new String(((ByteArrayOutputStream) r).toByteArray(), charset);
                                //System.out.println(r);
                                //System.out.println(System.getProperties());
                            } else if (r instanceof byte[]) {
                                r = new String((byte[]) r, charset);
                            } else if (r instanceof String) {
                                r = new String(((String) r).getBytes(), charset);
                            }
                        } else {
                            if (r instanceof ByteArrayOutputStream) {
                                r = new String(((ByteArrayOutputStream) r).toByteArray());
                            } else if (r instanceof byte[]) {
                                r = new String((byte[]) r);
                            }
                        }
                    }

                    String clazz = (String) map.get("clazz");
                    //System.out.println("format ret:"+r+"  c:"+clazz);
                    if (StringUtils.isNotBlank(clazz)) {
                        Class target = Class.forName(clazz);
                        if (Map.class.isAssignableFrom(target) && r instanceof String) {
                            if(((String)r).startsWith("{")) {
                                r = StringUtils.convert2MapJSONObject((String) r);
                            }
                        }else if(List.class.isAssignableFrom(target) && r instanceof String){
                            if(((String)r).contains("\n")){
                                r = Arrays.asList(StringUtils.split((String) r,"\n"));
                            }else if(((String)r).contains(",")){
                                r = Arrays.asList(((String) r).split(","));
                            }

                        } else if (ByteArrayInputStream.class.isAssignableFrom(target) && r instanceof ByteArrayOutputStream) {
                            r = ObjectUtils.convert((ByteArrayOutputStream) r);
                        } else if (List.class.isAssignableFrom(target) && r instanceof String) {
                            r = StringUtils.convert2ListJSONObject((String) r);
                        }
                    }
                    String type = (String) map.get("type");
                    if (StringUtils.isNotBlank(type)) {
                        if (type.equalsIgnoreCase("xml") && r instanceof String)
                            r = XMLUtil.getDataFromString((String) r);
                    }
                    data.put("${return}", r);
                } else {
                    String[] ts = tm.split(",");
                    if (null != ts && ts.length == 2) {
                        if(StringUtils.isBlank(ts[0]) && ts[0] instanceof String){
                            return "";
                        }
                        if (null != ts[0] && ts[0] instanceof String && StringUtils.isNotBlank(ts[1]) && DateTimeUtils.isDatePattern(ts[1])) {
                            return DateTimeUtils.getFormattedDate(ts[0], ts[1]);
                        }

                    }
                }
            }else {
                String[] ts = StringUtils.splitBase(tm, ",");
                if(StringUtils.isBlank(ts[0]) && ts[0] instanceof String){
                    return "";
                }
                if(ts.length==2 && !XMLParameter.isHasRetainChars(ts[0]) && DateTimeUtils.isDatePattern(ts[1])){
                    return DateTimeUtils.formatDate(ts[0],ts[1]);
                }
            }
        }catch (Exception e){
            log.error("not support format result", e);
        }
        return str;
    }
    public static void main(String[] args){
        try{
            PointParseFormat f = new PointParseFormat();
            f.parse("format(,yyyyMMddhhmmss)",null,null);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
