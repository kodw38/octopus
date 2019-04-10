package com.octopus.utils.xml;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.file.FileInfo;
import com.octopus.utils.file.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.*;


/**
 *
 * User: wf
 * Date: 2008-8-19
 * Time: 19:08:03
 */
public class XMLUtil {
    transient static Log log = LogFactory.getLog(XMLUtil.class);
    private static byte startTag = '<';
    private static byte endTag = '>';
    private static byte bias = '/';
    private static byte ques = '?';
    private static byte space = ' ';
    private static byte sigh = '!';
    private static byte conv = '\\';
    private static byte across = '-';
    private static byte equal = '=';
    private static byte singleq = "'".getBytes()[0];
    private static byte doubleq  = '"';
    private static String xmlstart = "<?xml";
    private static String xmlend = "?>";

    public static List<XMLMakeup> getXmlFromDir(String directory){
        List<FileInfo> fs = FileUtils.getAllProtocolFiles(directory, null,false);
        if(null != fs){
            List<XMLMakeup> ts = new ArrayList();
            for(FileInfo in:fs){
                if(null != in){
                    try{
                        XMLMakeup x = getDataFromStream(in.getInputStream());
                        if(null != x){
                            x.setSourcePath(in.getAllName());
                            ts.add(x);
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
            if(ts.size()>0)
                return ts;
        }
        return null;
    }

    public static XMLMakeup getDataFromXml(String xmlpath) throws IOException {
        List<FileInfo> fis = FileUtils.getAllProtocolFiles(xmlpath, null, false);
        if(null != fis){
            try{
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis.get(0).getInputStream()));
            XMLMakeup xml =  getDataFromBufferedReader(reader);
            xml.setSourcePath(xmlpath);
            xml.getProperties().setProperty("path",fis.get(0).getPath());
            return xml;
            }catch (Exception e){
                throw new IOException(xmlpath,e);
            }
        }
        throw new IOException("not find file:"+xmlpath);
    }

    public static XMLMakeup getDataFromStream(InputStream stream) throws IOException {
        if(null != stream){
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            XMLMakeup makeup =  getDataFromBufferedReader(reader);
            stream.close();
            return makeup;
        }
        return null;
    }
    
    public static XMLMakeup getDataFromString(String xmlContent) throws Exception{
        try{
    	BufferedReader bufReader = new BufferedReader(new StringReader(xmlContent));
    	return getDataFromBufferedReader(bufReader);
        }catch (Exception e){
            log.error(xmlContent,e);
            throw e;
        }
    }
    
    

    /**
     * @return
     * @throws java.io.IOException
     */
    private static XMLMakeup getDataFromBufferedReader(BufferedReader reader) throws IOException {
        if(null != reader){
            String line ;
            XMLMakeup whole=null,cur=null;
            StringBuffer sb = new StringBuffer();
            String left="" ,right="";
            int state = 0;// ��ʼ:0 <:1  �� >  ��/> :0��</:2 >�� <!--:3  -->  ':4' ":5"
            boolean isfirstline = true;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if(isfirstline){
                    isfirstline = false;
                    int start = line.indexOf(xmlstart);
                    int end = line.indexOf(xmlend);
                    if(start>=0 && end>start ){
                        line = line.substring(end+xmlend.length());
                        if("".equals(line))
                            continue;
                    }/*else{
                        throw new IOException("the xml content must contans <?xml  ?>");
                    }*/
                }

                for(int i=0;i<line.length();i++){

                    if(state==1 && (line.charAt(i)==space) && null != cur && (null == cur.getName() || "".equals(cur.getName())) ){//�� "<"��ʼ���������һ���ո�
                        cur.setName(sb.toString());
                        sb.delete(0,sb.length());
                    }
                    if(state !=0 && state!=5 && state!=4 )
                        if(line.charAt(i)==space) continue;

                    if(state==0 && line.charAt(i)==startTag && ((line.length()>=(i+1) && line.charAt(i+1) != bias && line.charAt(i+1) !=sigh ) || (line.length()<(i+1) )) ){//���� "<"��ʼ��
                        state=1;
                        sb.delete(0,sb.length());
                        if(null == whole){
                            whole = new XMLMakeup();
                            cur = whole;
                        }else{
                            XMLMakeup temp = new XMLMakeup();
                            if(null != cur) {
                                temp.setParent(cur);
                                cur.addChild(temp);
                            }
                            cur = temp;

                        }

                        continue;
                    }

                    if(state==0 && line.charAt(i)==startTag && (line.length()>=(i+1) && line.charAt(i+1) == bias)){//���� "</" ����ʼ��
                        state=2;
                        if(null != cur && sb.length()>0) {
                            cur.setText(StringUtils.toXMLRetainChar(sb.toString()));
                            sb.delete(0, sb.length());
                        }
                        cur = cur.getParent();

                        i++;
                        continue;
                    }

                    if(state==0 && line.charAt(i)==startTag && (line.length()>=(i+3) && line.charAt(i+1) == sigh && line.charAt(i+2) == across && line.charAt(i+3) == across )){//���� "<!--" ע�Ϳ�ʼ��
                        state=3;

                        i =i+3;
                        continue;
                    }
                    

                    if(state==1 && line.charAt(i)==equal ){
                        left = sb.toString();
                        sb.delete(0,sb.length());
                        continue;
                    }

                    if(state==1 && line.charAt(i)== doubleq && (i>0 && line.charAt(i-1)!=conv) ){
                        state=5;
                        continue;
                    }

                    if(state==5 && line.charAt(i)== doubleq && (i>0 && line.charAt(i-1)!=conv) ){
                        state=1;
                        right = StringUtils.toXMLRetainChar(sb.toString());
                        sb.delete(0,sb.length());
                        cur.getProperties().setProperty(left,right);
                        continue;
                    }

                    if(state==1 && line.charAt(i)== singleq && (i>0 && line.charAt(i-1)!=conv)){
                        state=4;
                        continue;
                    }

                    if(state==4 && line.charAt(i)== singleq && (i>0 && line.charAt(i-1)!=conv)){
                        state=1;
                        right = StringUtils.toXMLRetainChar(sb.toString());
                        sb.delete(0,sb.length());
                        cur.getProperties().setProperty(left,right);
                        continue;
                    }

                    if(state==1 && line.charAt(i)==bias && line.length()>=(i+1) && line.charAt(i+1)==endTag ){
                        state=0;
                        if(null != cur && (null == cur.getName() || "".equals(cur.getName()))){
                            cur.setName(sb.toString());
                            sb.delete(0,sb.length());
                        }
                        cur = cur.getParent();

                        i++;
                        continue;
                    }

                    if(state==1 && line.charAt(i)==endTag ){
                        state=0;
                        if(null != cur && (null == cur.getName() || "".equals(cur.getName()))){
                            cur.setName(sb.toString());
                            sb.delete(0,sb.length());
                        }
                        
                        continue;
                    }

                    if(state==2 && line.charAt(i)==endTag ){//"</"  ">"
                        state=0;
                        continue;
                    }
                    
                    if(state==2){//"</" ">"
                        continue; 
                    }

                    if(state==3 && line.charAt(i)== across && line.length()>(i+2) && line.charAt(i+1)==across && line.charAt(i+2)==endTag ){// "<!--"ʼ "-->"
                        state=0;

                        i=i+2;
                        continue;
                    }
                    if(state==3){//��"<!--"��"-->"֮����ַ���˵�
                        continue;
                    }
                    if(state==4 && line.charAt(i)== singleq ){
                        if(i>1 && line.charAt(i-1)==conv && line.charAt(i-2)==conv && line.charAt(i-3)==conv) {// change \\\' to \'
                            if (sb.charAt(sb.length() - 1) == conv && sb.charAt(sb.length() - 2) == conv) {
                                sb.delete(sb.length()-2,sb.length());
                                sb.append(line.charAt(i));
                                continue;
                            }
                        }else if(i>0 && line.charAt(i-1)==conv) { // change \' '
                            if (sb.charAt(sb.length() - 1) == conv) {
                                sb.setCharAt(sb.length() - 1, line.charAt(i));
                                continue;
                            }
                        }
                    }
                    if(state==5 && line.charAt(i)== doubleq){
                        if(i>1 && line.charAt(i-1)==conv && line.charAt(i-2)==conv && line.charAt(i-3)==conv){// change \\\" to \"
                            if (sb.charAt(sb.length() - 1) == conv && sb.charAt(sb.length() - 2) == conv) {
                                sb.delete(sb.length()-2,sb.length());
                                sb.append(line.charAt(i));
                                continue;
                            }
                        }else if(i>0 && line.charAt(i-1)==conv) {  // change \" to "
                            if (sb.charAt(sb.length() - 1) == conv) {
                                sb.setCharAt(sb.length() - 1, line.charAt(i));
                                continue;
                            }
                        }
                    }
                    sb.append(line.charAt(i));
                }
		    }

            sortChildren(whole);

            return whole;
        }
        return null;
    }

    static void sortChildren(XMLMakeup xml){
        if(null != xml){
            List<XMLMakeup> li = xml.getChildren();
            if(li.size()>0 && StringUtils.isNotBlank((li.get(0)).getProperties().getProperty("seq"))){
                Collections.sort(li);
            }
            for(XMLMakeup x:li){
                sortChildren(x);
            }
        }
    }

    /**
     * convert xml string to map
     * @param
     * @return
     * @throws Exception
     */
    /*public static Map getMapIgnorePropertiesFromXMLString(String xml)throws Exception{
        try{
            BufferedReader bufReader = new BufferedReader(new StringReader(xml));
            return getMapIgnorePropertiesFromBufferedReader(bufReader);
        }catch (Exception e){
            log.error(xml,e);
            throw e;
        }
    }*/

    /*private static Map getMapIgnorePropertiesFromBufferedReader(BufferedReader reader) throws Exception {
        if(null != reader){
            String line ;
            Map whole=null,cur=null;
            Map<Integer,Map> parent = new HashMap();
            Map<Integer,String> names=new HashMap<Integer, String>();
            StringBuffer sb = new StringBuffer();
            String left="" ,right="";
            int state = 0;// ʼ:0 <:1   >  /> :0</:2 > <!--:3  -->  ':4' ":5"
            boolean isfirstline = true;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if(isfirstline){
                    isfirstline = false;
                    int start = line.indexOf(xmlstart);
                    int end = line.indexOf(xmlend);
                    if(start>=0 && end>start ){
                        line = line.substring(end+xmlend.length());
                        if("".equals(line))
                            continue;
                    }*//*else{
                        throw new IOException("the xml content must contans <?xml  ?>");
                    }*//*
                }

                for(int i=0;i<line.length();i++){

                    if(state==1 && (line.charAt(i)==space) && null != cur && sb.length()>0 ){
                        names.put(System.identityHashCode(cur),sb.toString());
                        sb.delete(0,sb.length());
                    }
                    if(state !=0 && state!=5 && state!=4 )
                        if(line.charAt(i)==space) continue;

                    if(state==0 && line.charAt(i)==startTag && ((line.length()>=(i+1) && line.charAt(i+1) != bias && line.charAt(i+1) !=sigh ) || (line.length()<(i+1) )) ){//���� "<"��ʼ��
                        state=1;

                        if(null == whole){
                            whole = new LinkedHashMap();
                            cur = whole;
                        }else{
                            Map temp = new LinkedHashMap();
                            if(null != cur) {
                                parent.put(System.identityHashCode(temp),cur);

                                putMap(cur,names.get(System.identityHashCode(cur)),temp);
                            }
                            cur = temp;

                        }

                        continue;
                    }

                    if(state==0 && line.charAt(i)==startTag && (line.length()>=(i+1) && line.charAt(i+1) == bias)){//���� "</" ����ʼ��
                        state=2;
                        if(null != cur && sb.length()>0) {
                            Map p = parent.get(System.identityHashCode(cur));
                            if(p.size()==0) {
                                putMap(cur, names.get(System.identityHashCode(cur)), StringUtils.toXMLRetainChar(sb.toString()));
                            }else{
                                putMap(getLastMap(p), names.get(System.identityHashCode(cur)), StringUtils.toXMLRetainChar(sb.toString()));
                            }
                            sb.delete(0, sb.length());
                        }
                        if(null!= cur) {
                            cur = parent.get(System.identityHashCode(cur));
                        }
                        i++;
                        continue;
                    }

                    if(state==0 && line.charAt(i)==startTag && (line.length()>=(i+3) && line.charAt(i+1) == sigh && line.charAt(i+2) == across && line.charAt(i+3) == across )){//���� "<!--" ע�Ϳ�ʼ��
                        state=3;

                        i =i+3;
                        continue;
                    }


                    if(state==1 && line.charAt(i)==equal ){
                        left = sb.toString();
                        sb.delete(0,sb.length());
                        continue;
                    }

                    if(state==1 && line.charAt(i)== doubleq && (i>0 && line.charAt(i-1)!=conv) ){
                        state=5;
                        continue;
                    }

                    if(state==5 && line.charAt(i)== doubleq && (i>0 && line.charAt(i-1)!=conv) ){
                        state=1;
                        right = StringUtils.toXMLRetainChar(sb.toString());
                        sb.delete(0,sb.length());
                        //cur.getProperties().setProperty(left,right);
                        continue;
                    }

                    if(state==1 && line.charAt(i)== singleq && (i>0 && line.charAt(i-1)!=conv)){
                        state=4;
                        continue;
                    }

                    if(state==4 && line.charAt(i)== singleq && (i>0 && line.charAt(i-1)!=conv)){
                        state=1;
                        right = StringUtils.toXMLRetainChar(sb.toString());
                        sb.delete(0,sb.length());
                        //cur.getProperties().setProperty(left,right);
                        continue;
                    }

                    if(state==1 && line.charAt(i)==bias && line.length()>=(i+1) && line.charAt(i+1)==endTag ){
                        state=0;
                        if(null != cur && sb.length()>0){
                            names.put(System.identityHashCode(cur),sb.toString());
                            sb.delete(0,sb.length());
                        }
                        if(null != cur) {
                            cur = parent.get(System.identityHashCode(cur));
                        }
                        i++;
                        continue;
                    }

                    if(state==1 && line.charAt(i)==endTag ){
                        state=0;
                        if(null != cur && sb.length()>0){
                            names.put(System.identityHashCode(cur),sb.toString());
                            sb.delete(0,sb.length());
                        }

                        continue;
                    }

                    if(state==2 && line.charAt(i)==endTag ){//"</"  ">"
                        state=0;
                        continue;
                    }

                    if(state==2){//"</" ">"
                        continue;
                    }

                    if(state==3 && line.charAt(i)== across && line.length()>(i+2) && line.charAt(i+1)==across && line.charAt(i+2)==endTag ){// "<!--"ʼ "-->"
                        state=0;

                        i=i+2;
                        continue;
                    }
                    if(state==3){//��"<!--"��"-->"֮����ַ���˵�
                        continue;
                    }

                    sb.append(line.charAt(i));
                }
            }
            return whole;
        }
        return null;
    }*/
    static Map getLastMap(Map p){
        Iterator its = p.keySet().iterator();
        Map ret=null;
        while(its.hasNext()){
            Object o = p.get(its.next());
            if(o instanceof Map) {
                ret = (Map)o;
            }
        }
        return ret;
    }
    static void putMap(Map o,String k,Object v){
        if(!o.containsKey(k)){
            o.put(k,v);
        }else{
            if(o.get(k) instanceof Map && v instanceof Map){
                ((Map) o.get(k)).putAll((Map)v);
            }else {
                List ls = new LinkedList();
                ls.add(o.get(k));
                ls.add(v);
                o.put(k, ls);
            }
        }
    }

    public static void main(String[] args){
    	try{
/*
    		HashMap map = new HashMap<String, String>();
    		map.put("WWW", "XXXX");
    		map.put("WWW2", "1111");
    		OutputStream out = new FileOutputStream(new File("c:\\ics\\test.xml"));    		
    		XStream xstream = new XStream();   
    		xstream.toXML(map, out);
    		
    		XStream xr = new XStream();
    		FileInputStream in = new FileInputStream(new File("c:\\ics\\test.xml"));
    		HashMap m  = (HashMap)xr.fromXML(in);
    		System.out.println(m.get("WWW"));
*/
            XMLMakeup x = getDataFromString("<action key=\"test_05\" result=\"${end}\" xmlid=\"Logic\">    <do key='end' action='invoke' config='{parameterType:pojo}'></do></action>");
            //XMLMakeup x = getDataFromString("<a>    <d></d></a>");
            System.out.println(x);

        }catch(Exception e){
    		e.printStackTrace();
    	}
    }
}
