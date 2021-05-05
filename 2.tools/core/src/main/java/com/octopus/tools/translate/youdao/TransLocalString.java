package com.octopus.tools.translate.youdao;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class TransLocalString {
    static char[] ChinaChars = new char[]{'。','，','！','；','：','“','”'};
    public static void main(String[] args){
        try{
            //1.transMain("C:\\work\\flyingserver_aigit\\flyingserver-app-repository","LocalStrings.properties","zh_CN");
            //changeName("C:\\work\\flyingserver_aigit\\flyingserver-app-repository","LocalStrings_zh_CN.properties",".bak");
            //toUnicode("c:\\logs\\i18n","C:\\work\\flyingserver_aigit\\flyingserver-app-repository","LocalStrings_zh_CN.properties.bak");
            //toUnicode(null,"C:\\work\\flyingserver_aigit\\flyingserver-app-repository","LocalStrings_zh_CN.properties.bak","LocalStrings_zh_CN.properties");
            //copyto("C:\\work\\flyingserver_aigit\\flyingserver-app-repository","LocalStrings_zh_CN.properties","c:\\logs\\i18n");

            //System.out.println("[ {port-pattern} ]");
            //System.out.println("[ {port-pattern} ]".replaceAll(" ","").trim());
//System.out.println((chinaToUnicode("。")));

            //chgStrings();

            //toUnicode(null,"C:\\work\\flyingserver_aigit\\flyingserver-app-repository","Strings_zh_CN.properties.bak",true,"Strings_zh_CN.properties");
            //copyto("C:\\work\\flyingserver_aigit\\flyingserver-app-repository","Strings_zh_CN.properties","c:\\logs\\i18n",true);

            chgLocalStrings();
            chgStrings();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    static void chgLocalStrings()throws Exception {
        transMain("C:\\work\\flyingserver_aigit\\flyingserver-app-repository","LocalStrings.properties",true,"zh_CN");
        changeName("C:\\work\\flyingserver_aigit\\flyingserver-app-repository","LocalStrings_zh_CN.properties",".bak",true);
        toUnicode(null,"C:\\work\\flyingserver_aigit\\flyingserver-app-repository","LocalStrings_zh_CN.properties.bak",true,"LocalStrings_zh_CN.properties");
        //copyto("C:\\work\\flyingserver_aigit\\flyingserver-app-repository","Strings_zh_CN.properties","c:\\logs\\i18n",true);
    }
    static void chgStrings()throws Exception {
        transMain("C:\\work\\flyingserver_aigit\\flyingserver-app-repository","Strings.properties",true,"zh_CN");
        changeName("C:\\work\\flyingserver_aigit\\flyingserver-app-repository","Strings_zh_CN.properties",".bak",true);
        toUnicode(null,"C:\\work\\flyingserver_aigit\\flyingserver-app-repository","Strings_zh_CN.properties.bak",true,"Strings_zh_CN.properties");
        //copyto("C:\\work\\flyingserver_aigit\\flyingserver-app-repository","Strings_zh_CN.properties","c:\\logs\\i18n",true);
    }
    static void copyto(String srcDir, String fileName, String targetDir, boolean isFullMatch)throws Exception {
        List<String> fs = findAllLocalStringFile(srcDir,fileName,isFullMatch);
        for(String f:fs){
            String fp = f.replaceAll("\\\\","/");
            String head = srcDir.replaceAll("\\\\","/");
            String subpath = fp.substring(head.length()+1);
            int n = subpath.indexOf("src");
            if(n>=0) {
                subpath = subpath.substring(n);
                String path = null;
                if (null != targetDir) {
                    path = targetDir + "/" + subpath;
                } else {
                    path = fp;
                }
                if(null != path && !path.equals(fp)){
                    makeFilePath(path);
                    String[] lines = getFileLines(new File(f));
                    StringBuffer sb = new StringBuffer();
                    for(String line:lines) {
                        sb.append(line).append("\n");
                    }
                    saveFile(sb,path,true);
                }else{
                    System.out.println("保存名称和源文件相同不能保存:"+fp);
                }
            }
        }
    }
    static  void toUnicode(String targetDir, String srcDir, String fileName, boolean isFullMatch, String newFileName)throws Exception {
        List<String> fs = findAllLocalStringFile(srcDir,fileName,isFullMatch);
        for(String f:fs){
            String[] lines = getFileLines(new File(f));
            StringBuffer sb = new StringBuffer();
            for(String line:lines) {
                String a = line;
                int n = line.indexOf("=");
                if (n > 0) {
                    String k = line.substring(0, n);
                    String v = line.substring(n + 1);
                    if (needToTrans(v)) {
                        a = k + "=" + chinaToUnicode(v);

                    }
                }
                sb.append(a).append("\n");
            }
            String fp = f.replaceAll("\\\\","/");
            String head = srcDir.replaceAll("\\\\","/");
            String subpath = fp.substring(head.length()+1);
            int n = subpath.indexOf("src");
            if(n>=0) {
                subpath = subpath.substring(n);
                String path=null;
                if(null != targetDir) {
                    path = targetDir + "/" + subpath;
                }else{
                    path = fp;
                }
                if(null != newFileName){
                    path = path.substring(0,path.lastIndexOf("/")+1)+newFileName;
                }
                if(null != path && !path.equals(fp)){
                    saveFile(sb, path, true);
                }else{
                    System.out.println("保存名称和源文件相同不能保存:"+fp);
                }


            }else{
                System.out.println(f);
            }
        }
    }
    static void changeName(String srcDir, String findFileName, String appendName, boolean isFullMatch){
        List<String> fs = findAllLocalStringFile(srcDir,findFileName,isFullMatch);
        for(String f:fs){
            File file= new File(f);
            File nfile = new File(f+appendName);
            file.renameTo(nfile);
        }
    }
    //获取目录下所有资源文件
    static List<String> findAllLocalStringFile(String dir, String findFileName, boolean isFullMatch){
        List<String> ret= new ArrayList();
        getAllDirectoryMarchFiles(new File(dir),findFileName,isFullMatch,ret);
        return ret;
    }

    static HttpURLConnection initConnection()throws Exception {
        //URL u = new URL("https://aidemo.youdao.com/trans");
        //URL u = new URL("http://fanyi.youdao.com/translate");
        URL u = new URL("https://openapi.youdao.com/api");
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestProperty("accept", "*/*");
        conn.setRequestProperty("connection", "Keep-Alive");
        conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        return conn;
    }
    //翻译
    static String trans(String str) throws Exception {
        OutputStream os = null;
        InputStream is = null;
        BufferedReader br = null;
        HttpURLConnection conn=null;
        try {
            String APP_KEY="1a6ce4cea96a0948";
            String APP_SECRET="UHxDf9PHcd6oqc4o5AN88y9wuiiSLcNe";
            conn=initConnection();
            String q = URLEncoder.encode(str, "UTF-8");
            String curtime = String.valueOf(System.currentTimeMillis() / 1000);
            String salt = String.valueOf(System.currentTimeMillis());
            String signStr = APP_KEY + truncate(q) + salt + curtime + APP_SECRET;
            String sign = getDigest(signStr);
            String param = "q=" + q + "&from=Auto&to=Auto&appKey="+APP_KEY+"&signType=v3&curtime="+curtime
                    +"&salt="+salt+"&sign="+sign+"";
            //String param = "doctype=json&type=AUTO&i="+str;

            os = conn.getOutputStream();
            os.write(param.getBytes("UTF-8"));

            if (conn.getResponseCode() == 200) {
                is = conn.getInputStream();
                br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                StringBuffer sbf = new StringBuffer();
                String line = null;
                while ((line = br.readLine()) != null) {
                    sbf.append(line);
                    sbf.append("\r\n");
                }
                String result = sbf.toString();
                if(null != result){
                    //int n =result.indexOf("\"translation\":[")+"\"translation\":[".length()+1;
                    int n =result.indexOf("\"tgt\":\"")+"\"tgt\":\"".length()+1;
                    if(n>=0) {
                        int en = result.indexOf("\"", n);
                        result = result.substring(n,en);
                        return result;
                    }
                }

            }
        }catch (Exception e){

        }finally {
            if (null != br) {
                br.close();
                }

                if (null != is) {
            is.close();
            }

            if (null != os) {
            os.close();
            }
            if(null != conn)
            conn.disconnect();
        }
        return null;
    }

    static boolean isInchar(int c,char[] cs){
        if(null != cs){
            for(char i:cs){
                if(i==c)
                    return true;
            }
        }
        return false;
    }
    //转换Unicode

    public static String chinaToUnicode(String str) {
        String result = "";
        for (int i = 0; i < str.length(); i++) {
            int chr1 = (char) str.charAt(i);
            if ((chr1 >= 19968 && chr1 <= 171941)||isInchar(chr1,ChinaChars)) {// 汉字范围 \u4e00-\u9fa5 (中文)
                result += "\\u" + Integer.toHexString(chr1);
            } else {
                result += str.charAt(i);
            }
        }
        return result;
    }

    /**
     * 判断是否为中文字符
     *
     * @param c
     * @return
     */
    public static boolean isChinese(char c) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS) {
            return true;
        }
        return false;
    }

    public static String getDigest(String string) {
        if (string == null) {
            return null;
        }
        char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        byte[] btInput = string.getBytes(StandardCharsets.UTF_8);
        try {
            MessageDigest mdInst = MessageDigest.getInstance("SHA-256");
            mdInst.update(btInput);
            byte[] md = mdInst.digest();
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (byte byte0 : md) {
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private static void byte2File(byte[] result, String file) {
        File audioFile = new File(file);
        FileOutputStream fos = null;
        try{
            fos = new FileOutputStream(audioFile);
            fos.write(result);

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(fos != null){
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }


    public static String truncate(String q) {
        if (q == null) {
            return null;
        }
        int len = q.length();
        String result;
        return len <= 20 ? q : (q.substring(0, 10) + len + q.substring(len - 10, len));
    }


    static void transMain(String srcDir, String fileName, boolean isFullMatch, String localKey)throws Exception {
        List<String> fs = findAllLocalStringFile(srcDir,fileName,isFullMatch);
        for(String f:fs){
            System.out.println("file:"+f);
            String[] lines = getFileLines(new File(f));
            StringBuffer nfb = new StringBuffer();
            for(String line:lines){
                String a= line;
                int n = line.indexOf("=");
                if(n>0) {
                    String k = line.substring(0, n);

                    String v = line.substring(n + 1);
                    if (needToTrans(v)) {
                        String ta = YouDaoTranslateV3.trans(v);
                        JSONObject json = (JSONObject) JSON.parse(ta);
                        if("0".equals(json.get("errorCode"))) {
                            ta = (String) ((JSONArray) json.get("translation")).get(0);
                            //ta = cnToUnicode(a);
                            a = k + "=" + ta;
                        }
                    }
                }
                nfb.append(a+"\n");
            }
            int index = f.lastIndexOf(".");
            String nfp = f.substring(0,index)+"_"+localKey+f.substring(index);

            saveFile(nfb,nfp,true);

        }
    }

    static boolean needToTrans(String s){
        if(s.startsWith("\\")) {
            return false;
        }else{
            return true;
        }
    }

    static void getAllDirectoryMarchFiles(File f, String marchName, boolean fullmatch, List list) {
        File[] fs;
        if (null != f && f.isDirectory()) {
            fs = f.listFiles();
            for (int i = 0; i < fs.length; i++) {
                if (fs[i].isDirectory()) {
                    getAllDirectoryMarchFiles(fs[i], marchName,fullmatch, list);
                }else{
                    if (null != marchName) {
                        if ((!fullmatch && getFileSimpleName(fs[i].getName()).indexOf(marchName)>=0)
                                ||
                                (fullmatch && fs[i].getName().equals(marchName))) {
                            list.add(fs[i].getPath());
                        }
                    } else {
                        list.add(fs[i].getPath());
                    }
                }

            }
        }
    }

    static String getFileSimpleName(String filename) {
        String s = filename.replaceAll("/", "\\\\");
        String[] ss = s.split("\\\\");
        if (null != ss && ss.length > 0) {
            return ss[ss.length - 1];
        } else {
            return null;
        }

    }
    //获取一个文件中的内容
    static String[] getFileLines(File f) throws IOException {
        if (null != f) {
            //
            InputStreamReader read = new InputStreamReader(new FileInputStream(f));
            BufferedReader in = new BufferedReader(read);
            String line;
            List tem = new ArrayList();
            Hashtable hs = new Hashtable();
            while ((line = in.readLine()) != null) {
                tem.add(line);
            }
            return (String[]) tem.toArray(new String[tem.size()]);
        } else {
            return null;
        }

    }

    public static void saveFile(StringBuffer sb, String filePath, boolean isover) throws Exception {

        File f = new File(filePath);
        if(!isover && f.exists())
            return;

        if(!f.exists()){
            makeFilePath(filePath);
        }

        FileOutputStream file = new FileOutputStream(f);
        file.write(sb.toString().trim().getBytes());
        file.close();
    }
    public static boolean makeFilePath(String fileName) throws Exception {
        File file = new File(fileName);
        if (!file.exists()) {
            File file2 = new File(getFilePath(fileName));
            if (file2.exists()) {
                FileOutputStream fos = new FileOutputStream(file);
                fos.close();
                return true;
            } else {
                if (file2.mkdirs()) {
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.close();
                    return true;
                }
                return false;
            }
        }
        return false;
    }
    public static String getFilePath(String filename) {
        String s = filename.replaceAll("/", "\\\\");
        String[] ss = s.split("\\\\");
        String ret = "";
        for (int i = 0; i < ss.length - 1; i++) {
            if (i == 0)
                ret = ss[i];
            else
                ret += File.separator + ss[i];
        }
        return ret;
    }

}
