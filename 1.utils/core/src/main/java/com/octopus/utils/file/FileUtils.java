package com.octopus.utils.file;

import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.NumberUtils;
import com.octopus.utils.alone.SNUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.file.impl.excel.ExcelReader;
import com.octopus.utils.zip.ReplaceZipItem;
import com.octopus.utils.zip.ZipUtil;
import net.sf.json.regexp.RegexpUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sun.misc.BASE64Encoder;

import javax.swing.*;
import java.io.*;
import java.net.URLDecoder;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * User: robai
 * Date: 2010-8-30
 * Time: 15:31:07
 */
public class FileUtils {
    static transient Log log = LogFactory.getLog(FileUtils.class);
    /**
     * ִ��һ��.exe�ļ�
     *
     * @param exePath
     * @throws java.io.IOException
     */
    public static void exec(String exePath) throws IOException {
        Runtime rn = Runtime.getRuntime();
        Process p = null;
        p = rn.exec(exePath);

    }

    public static Map<String,InputStream> convertFileListToMap(File[] fs) throws FileNotFoundException {
        if(null == fs)return null;
        Map map = new HashMap();
        for(File f:fs){
            if(f.isFile())
                map.put(f.getName(),new FileInputStream(f));
        }
        return map;
    }

    /**
     * һ���ļ���һ��һ�еļ�¼��ĳ���ַ�ָ�<br>
     * �÷�������һ�еļ�¼����һ���ָ��ָ�������ַ�ת����Hashtable<br>
     * keyΪ��һ���ָ��ǰ���ַ�
     *
     * @param path
     * @param spliter
     * @return
     * @throws java.io.IOException
     * @throws java.io.IOException
     */
    public static Hashtable getFileContent(String path, String spliter) throws IOException {

        BufferedReader in = new BufferedReader(new FileReader(path));
        String line;
        Hashtable hs = new Hashtable();
        while ((line = in.readLine()) != null) {
            String[] ss = line.split(spliter);
            if (ss.length == 1) {
                hs.put(ss[0], "");
            }
            if (ss.length == 2) {
                hs.put(ss[0], ss[1]);
            }
            if (ss.length > 2) {
                String tem = "";
                for (int i = 1; i < ss.length - 1; i++)
                    tem += ss[i];
                hs.put(ss[0], tem);
            }
        }
        return hs;
    }

    /**
     * ��ȡһ���ļ��е�����<br>
     * ����һ��String[] һ��Ϊһ�����
     *
     * @param path
     * @return
     * @throws java.io.IOException
     * @throws java.io.IOException
     */
    public static String[] getFileContent(String path) throws IOException {
        if (null != path && !"".equals(path)) {
            //
            InputStreamReader read = new InputStreamReader(new FileInputStream(path));// "utf-8"
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

    public static String[] getFileContent(InputStream fileStream)throws Exception{
        InputStreamReader read = new InputStreamReader(fileStream);// "utf-8"
        BufferedReader in = new BufferedReader(read);
        String line;
        List tem = new ArrayList();
        Hashtable hs = new Hashtable();
        while ((line = in.readLine()) != null) {
            tem.add(line);
        }
        return (String[]) tem.toArray(new String[tem.size()]);
    }
    
    public static String[] getFileLines(String path) throws IOException {
        if (null != path && !"".equals(path)) {
            //
            InputStreamReader read = new InputStreamReader(new FileInputStream(path));
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

    public static String[] getFileLines(File f) throws IOException {
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

    static HashMap<String,LinkedList> tem = new HashMap<String,LinkedList>();
    public static boolean checkFileContent(String fileType,String file,List includes,Boolean isFilterSame,String filterSameStartTags,String filterSameEndTags)throws IOException {
        InputStreamReader read = new InputStreamReader(new FileInputStream(file));
        BufferedReader in = new BufferedReader(read);
        try{
        String line;
        List<String> incount=new ArrayList<String>();

        if(null != includes){
            incount = new ArrayList<String>(includes);
        }
        boolean isget=false;
        String martch=null;
        while ((line = in.readLine()) != null) {
            for(int i=incount.size()-1;i>=0;i--){
                /*Pattern pattern = Pattern.compile(s);
                Matcher m = pattern.matcher(line);*/
                if(line.indexOf(incount.get(i))!=-1){
                    incount.remove(i);
                }
            }
            if(isFilterSame){
                List<String> t = StringUtils.getTagsIncludeMark(line,filterSameStartTags,filterSameEndTags);
                if(t.size()>0 && StringUtils.isNotBlank(t.get(0))){
                    martch = t.get(0);
                    List<String> tl = tem.get(fileType);
                    if(null == tl) {
                        tem.put(fileType,new LinkedList()) ;
                        tl = tem.get(fileType);
                    }
                    if(tl.contains(t.get(0)))
                        return false;
                    else{
                        tl.add(t.get(0));
                        isget=true;
                    }
                }
            }
            if(incount.size()==0 && ((isFilterSame && isget)||!isFilterSame))
                break;
        }
        if(incount.size()==0){
            if(log.isDebugEnabled())
                log.debug(file+" "+martch);
            return true;
        }else{
            return false;
        }
        }finally {
            in.close();
            read.close();
        }
    }

    public static String getClassPath(String[] javacontent) throws Exception {
        if (null != javacontent) {
            for (int i = 0; i < javacontent.length; i++) {
                if (javacontent[i].contains("package")) {
                    String tem = (javacontent[i].substring(javacontent[i].indexOf("package") + 7)).trim();
                    return tem.substring(0, tem.length() - 1);
                }

            }
        }
        return "";
    }

    public static String getClassName(String[] javacontent) throws Exception {
        if (null != javacontent) {
            for (int i = 0; i < javacontent.length; i++) {
                if (javacontent[i].contains("public") && javacontent[i].contains("interface")) {
                    String tem = (javacontent[i].substring(javacontent[i].indexOf("interface") + 9)).trim();
                    return tem.substring(0, tem.length() - 1);
                }

            }
        }
        return "";
    }

    public static String[] getPublicFuncName(String[] javacontent) throws Exception {
        if (null != javacontent) {
            List li = new ArrayList();
            for (int i = 0; i < javacontent.length; i++) {
                if (javacontent[i].contains("public") && javacontent[i].contains("throws")) {
                    String[] tem = javacontent[i].trim().split("\\ ");
                    for (int j = 0; j < tem.length; j++) {
                        if (tem[j].indexOf("(") > 0) {
                            li.add(tem[j].substring(0, tem[j].indexOf("(")));
                            break;
                        }
                    }
                }

            }
            return (String[]) li.toArray(new String[0]);
        }
        return null;
    }

    /**
     * ��ȡһ���ļ��е�����
     *
     * @param path
     * @return
     * @throws java.io.IOException
     */
    public static StringBuffer getFileContentStringBuffer(String path) throws IOException {
        try {
            StringBuffer sb = new StringBuffer();
            InputStream in = FileUtils.class.getClassLoader().getResourceAsStream(path);
            if (null == in) {
                File f = new File(path);
                if (f.exists()) {
                    in = new FileInputStream(f);
                }
            }
            if (null != in) {
                int len = in.available();
                if (len > 0) {
                    byte[] inb = new byte[len];
                    int x;
                    while ((x = in.read(inb, 0, len)) != -1) {
                        sb.append(new String(inb));
                    }
                }
                in.close();
            }
            if (log.isDebugEnabled()) {
                log.debug("get desc content size " + sb.length() + " " + path);
            }
            return sb;
        }catch (Exception e){
            log.error("getFileContentStringBuffer error",e);

        }
        return null;
    }

    public static String getFileContentString(String path) throws IOException {
        StringBuffer sb = getFileContentStringBuffer(path);
        if(null !=sb) {
            return sb.toString();
        }else{
            return null;
        }
    }
    
    public static StringBuffer getFileContentStringBuffer(InputStream filestream) throws IOException {
        if(null == filestream)return null;
        StringBuffer sb = new StringBuffer();
        int len = filestream.available();
        byte[] inb = new byte[len];
        int x;
        while ((x = filestream.read(inb, 0, len)) != -1) {
            sb.append(new String(inb));            
        }                
        filestream.close();
        return sb;
    }
    public static String getFileContentString(InputStream filestream) throws IOException {
        if(null == filestream)return null;
        StringBuffer sb = new StringBuffer();
        int len = filestream.available();
        byte[] inb = new byte[len];
        int x;
        while ((x = filestream.read(inb, 0, len)) != -1) {
            sb.append(new String(inb));
        }
        filestream.close();
        return sb.toString();
    }

    public static String getFileStringContent(String path)throws Exception{
        StringBuffer sb = new StringBuffer();
        InputStream in = FileUtils.class.getClassLoader().getResourceAsStream(path);
        if (null == in) {
            File f = new File(path);
            if (f.exists()) {
                in = new FileInputStream(f);
            }
        }
        int len = in.available();
        byte[] inb = new byte[len];
        int x;
        while ((x = in.read(inb, 0, len)) != -1) {
            sb.append(new String(inb));
        }
        in.close();

        return sb.toString();
    }
    public static String getFileContentByFile(File file)throws Exception{
        StringBuffer sb = new StringBuffer();
        InputStream in = new FileInputStream(file);
        int len = in.available();
        byte[] inb = new byte[len];
        int x;
        while ((x = in.read(inb, 0, len)) != -1) {
            sb.append(new String(inb));
        }
        in.close();

        return sb.toString();
    }
    /**
     * ��ȡ�ļ���׺��
     *
     * @param filename
     * @return
     */
    public static String getExtension(String filename) {

        if ((filename != null) && (filename.length() > 0)) {
            int i = filename.lastIndexOf('.');
            if ((i > 0) && (i < (filename.length() - 1))) {
                return filename.substring(i + 1);
            }
        }
        return "";
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

    public static String getFileSimpleName(String filename) {
        String s = filename.replaceAll("/", "\\\\");
        String[] ss = s.split("\\\\");
        if (null != ss && ss.length > 0) {
            return ss[ss.length - 1];
        } else {
            return null;
        }

    }

    /**
     * ���ļ���ĳ��λ��һ��ֵ����Ϊ���ļ�
     *
     * @param inFilePath
     * @param outFilePath
     * @param pos
     * @param b
     * @throws java.io.IOException
     */
    public static void changeByteFile(String inFilePath, String outFilePath, int pos, byte b) throws IOException {

        FileInputStream in = new FileInputStream(new File(inFilePath));
        FileOutputStream out = new FileOutputStream(new File(outFilePath));
        byte[] inb = new byte[1];
        int x;

        int index = 0;
        while ((x = in.read(inb, 0, 1)) != -1) {
            index++;
            if (index == pos)
                inb[0] = b;
            out.write(inb, 0, 1);
        }
        in.close();
        out.close();

    }

    public static boolean  isExist(String f){
        return new File(f).exists();
    }
    public static boolean isEmpty(File f){
        if(f.exists()){
            try{
                boolean ret=true;
                FileReader fr=new FileReader(f);
                if(fr.read()!=-1)
                   ret=false;
                fr.close();
                return ret;
            }catch (Exception e){
                return true;
            }
        }
        return true;
    }

    /**
     * Ŀ¼�µ������ļ���ĳ��λ��һ��byteֵ����Ϊ���ļ�
     *
     * @param inDir
     * @param outDir
     * @param fileExp
     * @param pos
     * @param b
     * @throws Exception
     */
    public static void changeByteDirectory(String inDir, String outDir, String fileExp, int pos, byte b) throws Exception {

        File in = new File(inDir);
        File out = new File(outDir);
        if (in.exists()) {
            if (in.isDirectory()) {
                if (!out.exists()) {
                    if (!out.mkdirs()) {
                        throw new Exception("����Ŀ��Ŀ¼ʧ�ܣ�");
                    }
                }
                File[] fs = in.listFiles();
                for (int i = 0; i < fs.length; i++) {
                    boolean isExe = false;
                    if (fs[i].isDirectory()) {
                        changeByteDirectory(fs[i].getAbsolutePath(), outDir + File.separator + fs[i].getName(), fileExp, pos, b);
                    } else {
                        if (null == fileExp) {
                            isExe = true;
                        } else {
                            if (getExtension(fs[i].getName()).equals(fileExp)) {
                                isExe = true;
                            }

                        }
                        if (isExe)
                            changeByteFile(fs[i].getAbsolutePath(), outDir + File.separator + fs[i].getName(), pos, b);
                    }
                }
            } else {
                throw new Exception("Դ�ļ�Ŀ¼����Ŀ¼��");
            }
        } else {
            throw new Exception("Դ�ļ�Ŀ¼�����ڣ�");
        }

    }

    public static void saveFile(File f,InputStream in)throws Exception{
        FileOutputStream out = new FileOutputStream(f);
        int len = in.available();
        byte[] inb = new byte[len];
        int x;
        while ((x = in.read(inb, 0, len)) != -1) {
            out.write(inb);
        }
        in.close();
        out.close();
    }
    public static void saveFile(String f,InputStream in)throws Exception{
        saveFile(f,in,"overwrite");
    }
    public static String saveFile(String filename,InputStream in,String sameNameType)throws Exception{
    	File f = new File(filename);
    	if(!f.exists() || (null != sameNameType&& "overwrite".equals(sameNameType))){
    		FileUtils.makeFilePath(filename);
    		f = new File(filename);
    		saveFile(f,in);
    		return filename;
    	}else{
            if(null != sameNameType&& "rename".equals(sameNameType)){
                String n = getRename(filename);
                f = new File(n);
                saveFile(f,in);
                return n;
            }
            throw new Exception("the file "+filename+" is exist, can not to save.");
        }
    }
    private static String getRename(String filename){
        String end = filename.substring(filename.lastIndexOf(".")+1,filename.length());
        String name = filename.substring(0,filename.lastIndexOf("."));
        name = name + "_" + System.currentTimeMillis()+"."+end;
       return name;
    }
    public static void saveFile(String filename,ByteArrayOutputStream data,boolean isOverwrite)throws Exception{
        File f = new File(filename);
        if(!f.exists()){
            FileUtils.makeFilePath(filename);
            f = new File(filename);
        }else{
            if(isOverwrite) {
                f.delete();
                f = new File(filename);
            }else{
                //if name and size same as exist, it will be cancel
                if(f.length()==data.size()){
                    return ;
                }
                if(filename.indexOf(".")>0){
                    String name = getRename(filename);
                    f = new File(name);
                }else {
                    f = new File(filename + "_" + System.currentTimeMillis());
                }
            }
        }
        FileOutputStream out = new FileOutputStream(f);
        out.write(data.toByteArray());
        out.close();
    }
    public static String file2Base64Encode(ByteArrayOutputStream stream) {
        return base64Encode(stream.toByteArray());
    }

    public static String base64Encode(byte[] b){
        if(null != b){
            BASE64Encoder base64Encoder = new BASE64Encoder();
            return base64Encoder.encode(b);
        }
        return null;
    }
    public static void saveTextFile(String fileName,String text)throws Exception{
        fileName = URLDecoder.decode(fileName);
        text = URLDecoder.decode(text);
        saveStringBufferFile(new StringBuffer(text),fileName,false);
    }
    public static void saveFiles(Map<String,InputStream> files,String tarage,boolean isover) throws Exception {
        if(null != files){
            Iterator its = files.keySet().iterator();
            while(its.hasNext()){
                String n = (String)its.next();
                InputStream in = files.get(n);
                saveFile(tarage+"/"+n,in,"overwrite");
            }
        }
    }
    public static List saveFiles(Map<String,InputStream> files,String tarage,String sameNameType) throws Exception {
        if(null != files){
            List li = new ArrayList();
            Iterator its = files.keySet().iterator();
            while(its.hasNext()){
                String n = (String)its.next();
                InputStream in = files.get(n);
                String s = saveFile(tarage+"/"+n,in,sameNameType);
                li.add(s);
            }
            return li;
        }else {
            return null;
        }
    }
    public static void saveStringBufferFile(StringBuffer sb, String filePath, boolean isAlert) throws Exception {
        saveFile(sb,filePath,true,isAlert);
    }
    /**
     * �����ַ��ļ�
     */
    public static void saveFile(StringBuffer sb, String filePath,boolean isover, boolean isAlert) throws Exception {

        File f = new File(filePath);
        if(!isover && f.exists())
            return;
        if (f.exists() && isAlert) {
            int answer = JOptionPane.showConfirmDialog(null, "", "yes", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            boolean is = false;
            if (answer == JOptionPane.YES_OPTION) // ѡ���ǡ�
            {
                is = true;
            } else if (answer == JOptionPane.NO_OPTION) // ѡ�񡰷�
            {
                is = false;
            }
            if (!is) {
                String name = JOptionPane.showInputDialog("yes or no:");
                if (null == name || "".equals(name))
                    return;
                String path = FileUtils.getFilePath(f.getAbsolutePath()) + File.separator + name + "." + FileUtils.getExtension(filePath);
                f = new File(path);
            }
        }
        if(!f.exists()){
            makeFilePath(filePath);
        }

        FileOutputStream file = new FileOutputStream(f);
        file.write(sb.toString().trim().getBytes());
        file.close();
    }

    /**
     * ��ʾ�����
     *
     * @param msg
     * @return
     */
    public static String alertInputDialog(String msg) {
        return JOptionPane.showInputDialog(msg);
    }

    /**
     * ��һ���ļ��ж�������
     *
     * @param path
     * @throws java.io.IOException
     * @throws java.io.FileNotFoundException
     * @throws ClassNotFoundException
     */
    public static Object readObjectData(String path) throws FileNotFoundException, IOException, ClassNotFoundException {

        ObjectInputStream obj = new ObjectInputStream(new FileInputStream(new File(path)));
        Object o = obj.readObject();
        obj.close();
        return o;

    }

    /**
     * �Ѷ���д��һ���ļ���
     *
     * @param path
     * @throws java.io.IOException
     * @throws java.io.FileNotFoundException
     */
    public static void writeObjectData(String path, Object o) throws FileNotFoundException, IOException {

        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(new File(path)));
        out.writeObject(o);
        out.close();

    }

    /** ********************************************�ļ�ѹ��************************************************ */
    /**
     * ���ļ��б��е��ļ�ѹ����zip�ļ��Ķ���Ŀ¼
     */

    public static void createDirectory(String directory, String subDirectory) {
        String dir[];
        File fl = new File(directory);
        try {
            if (subDirectory == "" && fl.exists() != true)
                fl.mkdir();
            else if (subDirectory != "") {
                dir = subDirectory.replace('\\', '/').split("/");
                for (int i = 0; i < dir.length; i++) {
                    File subFile = new File(directory + File.separator + dir[i]);
                    if (subFile.exists() == false)
                        subFile.mkdir();
                    directory += File.separator + dir[i];
                }
            }
        } catch (Exception ex) {
        }
    }




   
    /**
     * ��sourceDirĿ¼���ļ�ȫ��copy��destDir��ȥ
     */
    public static void deleteSourceBaseDir(File curFile) throws Exception {

        File[] lists = curFile.listFiles();
        String line = null;
        String url = null;
        File parentFile = null;
        for (int i = 0; i < lists.length; i++) {
            File f = lists[i];
            if (f.isFile()) {
                f.delete();
                // ����ĸ�Ŀ¼û���ļ��ˣ�˵���Ѿ�ɾ�꣬Ӧ��ɾ��Ŀ¼
                parentFile = f.getParentFile();
                if (parentFile.list().length == 0)
                    parentFile.delete();
            } else {
                deleteSourceBaseDir(f); // �ݹ����
            }
        }
    }

    /** **************************************************end****************************************************** */

    public static void appendStringToFile(String fileName, String content) throws Exception {
        File file = new File(fileName);
        if (!file.exists()) {
            File file2 = new File(FileUtils.getFilePath(fileName));
            if (file2.mkdirs()) {
                FileOutputStream fos = new FileOutputStream(file);
                fos.close();
            }
        }
        RandomAccessFile raf = new RandomAccessFile(fileName, "rw");
        raf.seek(raf.length());
        raf.writeBytes(content + "\n");
        raf.close();
    }

    public static void appendStringArrayToFile(String fileName, String[] content) throws Exception {
        File file = new File(fileName);
        if (!file.exists()) {
            File file2 = new File(getFilePath(fileName));
            if (file2.mkdirs()) {
                FileOutputStream fos = new FileOutputStream(file);
                fos.close();
            }
        }
        RandomAccessFile raf = new RandomAccessFile(fileName, "rw");
        raf.seek(raf.length());
        for (int i = 0; i < content.length; i++) {
            raf.writeBytes(content[i] + "\n");
        }
        raf.close();
    }

    public static void insertStringToFile(String fileName, int index, String content) throws Exception {
        String[] cs = getFileContent(fileName);
        if (null != cs) {
            Vector v = new Vector();
            for (int i = 0; i < cs.length; i++) {
                v.add(cs[i]);
            }
            v.insertElementAt(content, index);
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < v.size(); i++) {
                sb.append((String) v.get(i) + "\n");
            }
            saveStringBufferFile(sb, fileName, false);
        }
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

    public static boolean makeDirectoryPath(String path) {
        File file = new File(path);
        if (!file.exists()) {
            return file.mkdirs();
        }
        return false;
    }

    public static boolean removeFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            if (file.delete()) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }

    }

    public static boolean removeDirectory(String path) {
        File file = new File(path);
        if (file.exists() && file.isDirectory()) {
            File[] fs = file.listFiles();
            if (null != fs && fs.length > 0) {
                int n = 0;
                for (int i = 0; i < fs.length; i++) {
                    if (fs[i].isFile()) {
                        n++;
                    }
                }
                if (n == fs.length) {
                    for (int i = 0; i < fs.length; i++) {
                        fs[i].delete();
                    }
                    if (file.delete()) {
                        return true;
                    }
                    return false;
                } else {
                    return false;
                }
            } else {
                if (file.delete()) {
                    return true;
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    public static boolean deleteDir(String path) {
        File dir = new File(path);
        if(dir.exists()) {
            if (dir.isDirectory()) {
                String[] children = dir.list();
                //递归删除目录中的子目录下
                for (int i = 0; i < children.length; i++) {
                    boolean success = deleteDir(dir + "/" + children[i]);
                    if (!success) {
                        return false;
                    }
                }
            }
            // 目录此时为空，可以删除
            return dir.delete();
        }else{
            return false;
        }
    }
    public static boolean deleteDirContent(String path) {
        File dir = new File(path);
        if (dir.isDirectory()) {
            String[] children = dir.list();
            //递归删除目录中的子目录下
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(dir+"/"+children[i]);
                if (!success) {
                    return false;
                }
            }
        }else{
            dir.delete();
        }
        return true;
    }


    //
    public static void replaceLineText(String filePath, String key, String repcontent) throws Exception {
        String[] ss = getFileLines(filePath);
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < ss.length; i++) {
            if (ss[i].indexOf(key) >= 0) {
                ss[i] = repcontent;
            }
            sb.append(ss[i] + "\n");
        }
        saveStringBufferFile(sb, filePath, false);
    }    
    
    public static void replaceLines(String filePath, String[] keys, String[] repcontents) throws Exception {
        String[] ss = getFileLines(filePath);
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < ss.length; i++) {
        	for(int j=0;j<keys.length;j++){
	            if (ss[i].indexOf(keys[j]) >= 0) {
	                ss[i] = repcontents[j];
	            }
        	}
            sb.append(ss[i] + "\n");
        }
        saveStringBufferFile(sb, filePath, false);
    } 
    
    public static void replaceFilesLines(String dir,String like ,String notLike,String[] keys,String[] repcontents)throws Exception{
    	List li = new ArrayList();
		getAllDirectoryMarchFiles(new File(dir), like, li);
		for(int i=0;i<li.size();i++){
			String f = (String)li.get(i);
			if(null != notLike && !"".equals(notLike) && f.indexOf(notLike)>0){
				continue;
			}else{
				replaceLines(f, keys, repcontents);
				log.info(f);
			}
		}
    }

    public static String[] getLikeLineText(String filePath, String key) throws Exception {
        String[] ss = getFileLines(filePath);
        Vector v = new Vector();
        for (int i = 0; i < ss.length; i++) {
            if (ss[i].indexOf(key) >= 0) {
                v.add(ss[i]);
            }
        }
        return (String[]) v.toArray(new String[v.size()]);
    }

    // �����ļ�
    public static void copyFile(String sourcePath, String targetPath) throws Exception {
        copyFile(new File(sourcePath), new File(targetPath));
    }

    /**
     * ������ǰ�ļ����µ��ļ�
     * @param sourcepath
     * @param targetpath
     * @throws Exception
     */
    public static void copyCurFloderFiles(String sourcepath,String targetpath,List li,String[] filterFilesName)throws Exception{
        File f = new File(sourcepath);
        if(f.isDirectory()){
            File[] fs = f.listFiles();
            if(null != fs){
                String s =null;
                String t= null;
                for(int k=0;k<fs.length;k++){
                    if(fs[k].isFile()){
                        s = fs[k].getPath();
                        boolean isin = true;
                        if(null != filterFilesName){
                            for(int j=0;j<filterFilesName.length;j++){
                                if(filterFilesName[j].equals(getFileSimpleName(s))){
                                    isin = false;
                                    break;
                                }
                            }
                        }
                        if(isin){
                            t = s.replace(sourcepath,targetpath);
                            li.add(s+"="+t);
                            copyFile(s,t);
                        }
                    }
                }
            }
        }
    }

    public static void copyCurFloderFiles(String sourcepath,String targetpath,List li,String filterFilesName)throws Exception{
        File f = new File(sourcepath);
        if(f.isDirectory()){
            File ta = new File(targetpath);
            if(!ta.exists()) ta.mkdirs();
            File[] fs = f.listFiles();
            if(null != fs && fs.length>0){
                String s =null;
                String t= null;
                for(int k=0;k<fs.length;k++){
                    if(fs[k].isFile()){
                        s = fs[k].getPath();
                        boolean isin = true;
                        if(null != filterFilesName){
                        	if(filterFilesName.indexOf(","+getFileSimpleName(s))>=0){
                        		isin = false;
                        	}                            
                        }
                        if(isin){
                            s = s.replaceAll("\\\\","/");
                            sourcepath = sourcepath.replaceAll("\\\\","/");
                            targetpath = targetpath.replaceAll("\\\\","/");
                            t = s.replace(sourcepath,targetpath);
                            if(null != li)
                                li.add(s+"="+t);
                            copyFile(s,t);
                        }
                    }
                }
            }
        }
    }
    
    public static void appendContentToFile(String filePath,StringBuffer sb)throws Exception{
        RandomAccessFile log=new RandomAccessFile(filePath, "rw");
        log.seek(log.length());
        log.write(sb.toString().getBytes());
        log.close();
    }
    public static void appendHeadToFile(String filePath,String head)throws Exception{
        RandomAccessFile log=new RandomAccessFile(filePath, "rw");
        log.seek(0);
        log.write(head.getBytes());
        log.close();
    }

    public static void copyOneFile(File source, File target) {// copy �ļ�  
        FileInputStream inFile = null;  
        FileOutputStream outFile = null;  
        try {  
            inFile = new FileInputStream(source);  
            outFile = new FileOutputStream(target);  
            byte[] buffer = new byte[1024];  
            int i = 0;  
            while ((i = inFile.read(buffer)) != -1) {  
                outFile.write(buffer, 0, i);  
            }  
            inFile.close();  
            outFile.close();  
        } catch (FileNotFoundException e) {  
            e.printStackTrace();  
        } catch (IOException e) {  
            e.printStackTrace();  
        } finally {  
            try {  
                if (inFile != null) {  
                    inFile.close();  
                }  
                if (outFile != null) {  
                    outFile.close();  
                }  
            } catch (IOException e) {  
                e.printStackTrace();  
            }  
        }  
    }  
  
    // ����Ŀ¼  
    public static void copyDict(File source, File target,String filterFloderNames[],String[] filterFlodersPath,String filterFilesName,RandomAccessFile log) throws IOException {  
        File[] file = source.listFiles();// �õ�Դ�ļ��µ��ļ���Ŀ
        if(null != file) {
            for (int i = 0; i < file.length; i++) {
                if (file[i].isFile()) {// �ж����ļ�
                    if (null != filterFilesName && filterFilesName.indexOf("," + file[i].getName()) >= 0) {
                        continue;
                    }
                    if (null != filterFlodersPath) {
                        boolean isIn = true;
                        for (int j = 0; j < filterFlodersPath.length; j++) {
                            if (file[i].getPath().indexOf(filterFlodersPath[j]) >= 0) {
                                isIn = false;
                                break;
                            }
                        }
                        if (!isIn) {
                            continue;
                        }
                    }
                    File sourceDemo = new File(source.getAbsolutePath() + "/"
                            + file[i].getName());
                    File destDemo = new File(target.getAbsolutePath() + "/"
                            + file[i].getName());
                    copyOneFile(sourceDemo, destDemo);
                    if (null != log) {
                        log.write(new StringBuffer(sourceDemo.getPath()).append("=").append(destDemo.getPath()).append("\n").toString().getBytes());
                    }
                }
                if (file[i].isDirectory()) {// �ж����ļ���
                    if (null != filterFloderNames) {
                        boolean is = false;
                        for (String floder : filterFloderNames) {
                            if (file[i].getPath().indexOf(floder) >= 0) {
                                is = true;
                                break;
                            }
                        }
                        if (is) {
                            continue;
                        }
                    }
                    if (null != filterFlodersPath) {
                        boolean isIn = true;
                        for (int j = 0; j < filterFlodersPath.length; j++) {
                            if (file[i].getPath().indexOf(filterFlodersPath[j]) >= 0) {
                                isIn = false;
                                break;
                            }
                        }
                        if (!isIn) {
                            continue;
                        }
                    }

                    File sourceDemo = new File(source.getAbsolutePath() + "/"
                            + file[i].getName());
                    File destDemo = new File(target.getAbsolutePath() + "/"
                            + file[i].getName());
                    destDemo.mkdir();// �����ļ���
                    copyDict(sourceDemo, destDemo, filterFloderNames, filterFlodersPath, filterFilesName, log);
                }
            }// end copyDict
        }
  
    }  
    
    /**
     * copy much file's floder,maybe cost long time
     * @param sourceFloder
     * @param targetFloder
     * @param logPath
     * @throws Exception
     */
    public static void copyMuchFloderFiles(String sourceFloder,String targetFloder,String filterFloderNames[],String[] filterFlodersPath,String filterFilesName,String logPath)throws Exception{
        List<File> li = new ArrayList();
        getAllDirectory(new File(sourceFloder),null,li);
        RandomAccessFile log=null;
        if(null != logPath){
            log = new RandomAccessFile(logPath, "rw");
            log.seek(log.length());
        }
        
        ArrayList t2 = new ArrayList();
        copyCurFloderFiles(sourceFloder,targetFloder,t2,filterFilesName);

        if(null != log){
            for(int k=0;k<t2.size();k++){
                log.write((t2.get(k).toString()+"\n").getBytes());
            }            
        }

        //����ÿһ��Ŀ¼
        String s = "";
        String t = "";
        List temli = null;
        for(int i=0;i<li.size();i++){

            s = li.get(i).getPath();

            //����Ŀ¼���
            if(null != filterFloderNames){
                boolean isIn = true;
                for(int j=0;j<filterFloderNames.length;j++){
                    if(filterFloderNames[j].equals(getFileSimpleName(s))){
                        isIn = false;
                        break;
                    }
                }
                if(!isIn){
                    continue;
                }
            }

            //����Ŀ¼·��
            if(null != filterFlodersPath){
                boolean isIn = true;
                for(int j=0;j<filterFlodersPath.length;j++){
                    if(s.indexOf(filterFlodersPath[j])>=0){
                        isIn = false;
                        break;
                    }
                }
                if(!isIn){
                    continue;
                }
            }

            t = s.replace(sourceFloder,targetFloder);
            temli = new ArrayList();
            copyCurFloderFiles(s,t,temli,filterFilesName);
            if(null != log){
                log.write((s+"="+t+"\n").getBytes());
                for(int k=0;k<temli.size();k++){
                        log.write((temli.get(k).toString()+"\n").getBytes());
                }
            }
        }
        log.close();
    }


    public static void copyFile(File in, File out) throws Exception {
        FileInputStream fis = null;
        FileOutputStream fos = null;
        try {
            fis = new FileInputStream(in);
            if (!out.exists()) {
                makeFilePath(out.getPath());
            }
            if (out.isDirectory()) {
                out = new File(out.getPath() + "/" + in.getName());
                fos = new FileOutputStream(out);
            } else
                fos = new FileOutputStream(out);
            int len = fis.available();
            if (len > 0) {
                byte[] buf = new byte[len];
                int i = 0;
                while ((i = fis.read(buf, 0, len)) != -1) {
                    fos.write(buf);
                }
            }
        }finally {
            if(null !=fis)
            fis.close();
            if(null!=fos)
            fos.close();
        }

    }

    public static String[] getJavaFileSetFunctions(String filePath) throws Exception {
        if (getExtension(filePath).equals("java")) {
            try {
                String[] content = getFileContent(filePath);
                if (null != content && content.length > 0) {
                    Vector v = new Vector();
                    for (int i = 0; i < content.length; i++) {
                        String s = content[i];
                        if (s.indexOf("public") >= 0 && s.indexOf("void") >= 0 && s.indexOf("set") > 0 && s.indexOf("(") > 0) {
                            String funName = s.substring(s.indexOf("set"), s.indexOf("("));
                            v.add(funName);
                        }
                    }
                    return (String[]) v.toArray(new String[v.size()]);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            throw new Exception("����java�ļ�");
        }
        return null;
    }

    public static String[] getDirectoryFileNames(String path) {
        File f = new File(path);
        if (null != f && f.isDirectory()) {
            File[] fs = f.listFiles();
            Vector ret = new Vector();
            if (null != fs && fs.length > 0) {
                for (int i = 0; i < fs.length; i++) {
                    if (fs[i].isFile()) {
                        ret.add(fs[i].getName());
                    }
                }

            }
            return (String[]) ret.toArray(new String[ret.size()]);
        }
        return null;
    }
    
    public static void getCycleDirectoryFileNamesList(File f, String fileName, List list) throws Exception{
        File[] fs;
        if (null != f && f.isDirectory()) {
            fs = f.listFiles();
            for (int i = 0; i < fs.length; i++) {
                if (fs[i].isDirectory()) {                    
                    getCycleDirectoryFileNamesList(fs[i], fileName, list);
                }else{
                	if (null != fileName) {
                        if (getFileSimpleName(fs[i].getName()).equals(fileName)) {
                            list.add(fs[i].getPath());
                        }
                    } else {
                        list.add(fs[i].getPath());
                    }
                }

            }
        }
    }

    /**
     * Ŀ¼�µ������ļ�����ǰ׺
     *
     * @param path
     * @param prefix
     */
    public static void addPrefixFileNameInDirectory(String path, String prefix) {
        File f = new File(path);
        File nf;
        if (null != f && f.isDirectory()) {
            File[] fs = f.listFiles();
            if (null != fs && fs.length > 0) {
                for (int i = 0; i < fs.length; i++) {
                    if (fs[i].isFile()) {
                        nf = new File(path + File.separator + prefix + fs[i].getName());
                        fs[i].renameTo(nf);
                    }
                }

            }
        }
    }

    

    public static File findFileByFileName(File f ,String name)throws Exception{
        if(f.exists() && f.isDirectory()){
            File[] fs = f.listFiles();
            for(int i=0;i<fs.length;i++){
                File tf = findFileByFileName(fs[i],name);
                if(null != tf){
                    return tf;
                }
            }
        }else{
            if(f.getName().indexOf(name)>=0){
                return f;
            }
        }
        return null;

    }

    public static List<String> getCurDirFileNames(String dir){
        File[] fs = new File(dir).listFiles();
        List li = new ArrayList();
        if(null !=fs){
            for(File f:fs){
                if(f.isFile())
                    li.add(f.getName());
            }
        }
        Collections.sort(li);
        return li;
    }
    public static List<File> getSortFiles(String dir,String type){
        List<File> fs = new ArrayList();
        getAllFile(new File(dir),null,fs);
        if("update.desc".equals(type)){
        Collections.sort(fs,new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                if(o1.lastModified()>o2.lastModified())
                    return 0;
                else
                    return 1;  //To change body of implemented methods use File | Settings | File Templates.
            }
        });
        }
        return fs;
    }
    //get all files with suffix file and sort by length of file name, more len and laster
    public static List<String> getAllFileNames(String dir,String suffix){
        List<File> fs = new ArrayList();
        getAllFile(new File(dir),null,fs);
        if(null != fs){
            List<String> ret = new ArrayList<String>();
            for(File f:fs) {
                if(StringUtils.isNotBlank(suffix)){
                    if(f.getName().endsWith(suffix)){
                        ret.add(f.getPath());
                    }
                }else {
                    ret.add(f.getPath());
                }
            }

            ArrayUtils.sortByLen(ret,ArrayUtils.ABS);
            return ret;
        }
        return null;
    }

    public static List<String> getSortCurDirFileNames(String dir,String type){
        if(StringUtils.isNotBlank(type)){
            System.out.println("sort by update.desc");
            List<File> fs = getSortFiles(dir,type);
            List li = new ArrayList();
            if(null !=fs){
                for(File f:fs){
                    if(f.isFile()){
                        li.add(f.getName());
                    }
                }
            }
            return li;
        }else{
            return getCurDirFileNames(dir);
        }

    }
    public static List<String> getCurDirNames(String dir){
        File[] fs = new File(dir).listFiles();
        List li = new ArrayList();
        if(null !=fs){
            for(File f:fs){
                if(f.isDirectory())
                    li.add(f.getName());
            }
        }
        Collections.sort(li);
        return li;
    }

    /**
	 * ��ȡһ��Ŀ¼�µ������ļ�,����Ŀ¼;�ų�extFiles ��׺���ļ�
	 * 
	 * @param path
	 * @param extFiles
	 * @return
	 */
	public static List<File> getAllFile(String path, String[] extFiles) {
		ArrayList li = new ArrayList();
		File f = new File(path);
		privateGetAllFile(f, extFiles, li);
		return li;
	}
    public static boolean filterName(String name,String express){
        return RegexpUtils.getMatcher(express).matches(name);
    }
	public static void getAllLocalFiles(File f,String[] expresses,boolean isExpressInclude,List<File> li){
		File[] fs;
		if (null != f && f.isDirectory()) {
			fs = f.listFiles();
			for (int i = 0; i < fs.length; i++) {
				if (fs[i].isFile()) {
					if (null != expresses && expresses.length > 0) {
						for(String express:expresses) {
                            boolean b = filterName(f.getPath(),express);
                            if( (b && isExpressInclude) || (!b && !isExpressInclude))
							    li.add(fs[i]);

						}
					} else {
						li.add(fs[i]);
					}
				}
				if (fs[i].isDirectory()) {
                    getAllLocalFiles(fs[i], expresses,isExpressInclude, li);
				}
			}
		}
        if(null != f && f.isFile() && f.exists()){
            li.add(f);
        }
		
	}
	public static void getAllFileNamesToFile2(File f,String extfixes,RandomAccessFile out) throws IOException{
		File[] fs;
		
		fs = f.listFiles();
		for(File c:fs){
			if (c.isFile()) {
				String p = c.getName();
				if(null != extfixes){
					if(!(extfixes.indexOf(","+p.substring(p.lastIndexOf(".")+1))>=0))
						out.write((p+"\n").getBytes());
				}else{
					out.write((p+"\n").getBytes());
				}
			}else{
				getAllFileNamesToFile2(c,extfixes,out);
			}
		}
	}
	
	public static long countFile(File f,String subfix) throws IOException{
		File[] fs;
		long ret=0;
		fs = f.listFiles();
		for(File c:fs){
			if (c.isFile()) {
				String p = c.getName();
				if(null != subfix){
					if(p.endsWith(subfix))
						ret++;
				}else{
					ret++;
				}
			}else{
				ret +=countFile(c,subfix);
			}
		}
		return ret;
	}
	
	public static long countFiles(File f,String likestr) throws IOException{
		File[] fs;
		long ret=0;
		fs = f.listFiles();
		for(File c:fs){
			if (c.isFile()) {
				String p = c.getName();
				if(null != likestr){
					if(p.indexOf(likestr)>=0)
						ret++;
				}else{
					ret++;
				}
			}else{
				ret +=countFiles(c,likestr);
			}
		}
		return ret;
	}
	
	public static void getAllFileNamesToFile(File f,String suffixes,RandomAccessFile out) throws IOException{
		File[] fs;
		
		fs = f.listFiles();
		for(File c:fs){
			if (c.isFile()) {
				String p = c.getPath();
				if(null != suffixes){
					if(suffixes.indexOf(","+p.substring(p.lastIndexOf(".")+1))>=0)
						out.write((p+"\n").getBytes());
				}else{
					out.write((p+"\n").getBytes());
				}
			}else{
				getAllFileNamesToFile(c,suffixes,out);
			}
		}
		
	}
	private static List<File> privateGetAllFile(File f, String[] extFiles, ArrayList<File> list) {
		File[] fs;
		if (null != f && f.isDirectory()) {
			fs = f.listFiles();
			for (int i = 0; i < fs.length; i++) {
				if (fs[i].isFile()) {
					if (null != extFiles && extFiles.length > 0) {
						if (!ArrayUtils.isInStringArray(extFiles, getExtension(fs[i].getName()))) {
							list.add(fs[i]);
						}
					} else {
						list.add(fs[i]);
					}
				}
				if (fs[i].isDirectory()) {
					privateGetAllFile(fs[i], extFiles, list);
				}
			}
		}
		return list;
	}

    private static List<File> getAllFile(File f, String fileName, List list) {
        File[] fs;
        if (null != f && f.isDirectory()) {
            fs = f.listFiles();
            for (int i = 0; i < fs.length; i++) {
                if (fs[i].isFile()) {
                    if (null != fileName) {
                        if (getFileSimpleName(fs[i].getName()).equals(fileName)) {
                            list.add(fs[i]);
                        }
                    } else {
                        if(log.isDebugEnabled()) {
                            log.debug("add file:" + fs[i]);
                        }
                        list.add(fs[i]);
                    }
                }
                if (fs[i].isDirectory()) {
                    getAllFile(fs[i], fileName, list);
                }
            }
        }
        return list;
    }

    public static void getAllDirectoryFiles(File f, String fileName, List list) {
        File[] fs;
        if (null != f && f.isDirectory()) {
            fs = f.listFiles();
            for (int i = 0; i < fs.length; i++) {
                if (fs[i].isDirectory()) {
                    getAllDirectoryFiles(fs[i], fileName, list);
                } else {
                    if (null != fileName) {
                        if (getFileSimpleName(fs[i].getName()).equals(fileName)) {
                            list.add(fs[i]);
                        }
                    } else {
                        list.add(fs[i]);
                    }
                }

            }
        }
    }
    
    public static void getAllDirectoryMarchFiles(File f, String marchName, List list) {
        File[] fs;
        if (null != f && f.isDirectory()) {
            fs = f.listFiles();
            for (int i = 0; i < fs.length; i++) {
                if (fs[i].isDirectory()) {                    
                	getAllDirectoryMarchFiles(fs[i], marchName, list);
                }else{
                	if (null != marchName) {
                        if (getFileSimpleName(fs[i].getName()).indexOf(marchName)>=0) {
                            list.add(fs[i].getPath());
                        }
                    } else {
                        list.add(fs[i].getPath());
                    }
                }

            }
        }
    }
    
    public static void findAllHeadDirectory(File f, String endcatalog, List list) {
        File[] fs;
        if (null != f && f.isDirectory()) {
            fs = f.listFiles();
            for (int i = 0; i < fs.length; i++) {
                if (fs[i].isDirectory()) {                    
                	findAllHeadDirectory(fs[i], endcatalog, list);
                }else{
                	if (null != endcatalog) {
                        if (fs[i].getPath().indexOf(endcatalog)>=0) {
                            String t = fs[i].getPath().substring(0, fs[i].getPath().lastIndexOf(endcatalog)+endcatalog.length());
                            if(!list.contains(t)){
                            	list.add(t);
                            }
                        }
                    } else {
                        list.add(fs[i].getPath());
                    }
                }

            }
        }
    }

    public static void getAllDirectory(File f, String fileName, List<File> list) {
		File[] fs;
		if (null != f && f.isDirectory()) {
			fs = f.listFiles();
			for (int i = 0; i < fs.length; i++) {
				if (fs[i].isDirectory()) {
					if (StringUtils.isNotBlank(fileName)) {
						if (getFileSimpleName(fs[i].getName()).equals(fileName)) {
							list.add(fs[i]);
						}
					} else {
						list.add(fs[i]);
					}
					getAllDirectory(fs[i], fileName, list);
				}else{
                    list.add(fs[i]);
                }

			}
		}else{
            list.add(f);
        }
	}

    public static void delAllDirectory(String path, String directoryName) throws Exception {
        File f = new File(path);
        List list = new ArrayList();
        getAllDirectory(f, directoryName, list);
        for (int i = 0; i < list.size(); i++) {             	
            File file = (File) list.get(i);
            file.delete();

        }

    }

    public static void chgFileContent(String path,String[] oldContent,String[] newContent )throws Exception{
        File f = new File(path);
        if(f.exists()){
	        BufferedInputStream bin = new BufferedInputStream(new FileInputStream(f));
	        byte[]   buff=new   byte[((int)f.length())];
	        bin.read(buff);
	        String str = new String(buff);
	        boolean isc = false;
	        for(int i=0;i<oldContent.length;i++){
	        	if(str.contains(oldContent[i])){
	        		str = StringUtils.replace(str, oldContent[i], newContent[i]);
	        		isc = true;
	        	}
	        }
	        //StringBuffer ns = StringUtil.replace(str,oldContent, newContent);
	        
	        if(isc){
	        	OutputStream   fout=null;
	            fout=new FileOutputStream(f);
		        fout.write(str.getBytes());
		        fout.flush();
		        fout.close();
	        }
	        bin.close();
        }

    }
    
    public static void replaceFilesContent(String dir,String like,String notLike,String[] olds,String[] news)throws Exception{
    	List li = new ArrayList();
		getAllDirectoryMarchFiles(new File(dir), like, li);
		for(int i=0;i<li.size();i++){
			String f = (String)li.get(i);
			if(null != notLike && !"".equals(notLike) && f.indexOf(notLike)>0){
				continue;
			}else{
				chgFileContent(f, olds, news);
				log.info(f);
			}
		}
    }

    /**
     * create new zip file through replace some content files or add new files in an old zip file
     * @param zipfile
     * @param addFiles
     * @param replaceZipItems
     * @param targetFile
     * @throws Exception
     */
    public static void replaceZipFileContent(InputStream zipfile,Map<String,InputStream> addFiles,ReplaceZipItem[] replaceZipItems,OutputStream targetFile,String encode) throws Exception {
        ByteArrayInputStream chg = ZipUtil.changeFile(zipfile,addFiles,replaceZipItems,encode);
        ByteArrayOutputStream of = new ByteArrayOutputStream();
        byte[] b = new byte[1024];
        int len = 0;
        while (( len = chg.read( b ) ) > 0) {
            of.write( b, 0, len );
        }
        targetFile.write(of.toByteArray());
        targetFile.close();
    }

    /**
     * replace some content in file or add new file in the zip file
     * @param zipfile
     * @param addFiles
     * @param replaceZipItems
     * @throws Exception
     */
    public static void replaceZipFileContent(String zipfile,Map<String,InputStream> addFiles,ReplaceZipItem[] replaceZipItems,String encode) throws Exception {
        File f= new File(zipfile);
        FileInputStream in = new FileInputStream(f);
        ByteArrayInputStream chg = ZipUtil.changeFile(in,addFiles,replaceZipItems,encode);
        in.close();
        ByteArrayOutputStream of = new ByteArrayOutputStream();
        byte[] b = new byte[1024];
        int len = 0;
        while (( len = chg.read( b ) ) > 0) {
            of.write( b, 0, len );
        }
        if(f.delete()) {
            FileOutputStream targetFile = new FileOutputStream(zipfile);
            targetFile.write(of.toByteArray());
            targetFile.close();
        }
    }

    /**
     *
     * @param zipfile
     * @param addFiles
     * @param replaceItems key is file name in zip file, key in value is old value and value in value is new value.
     * @throws Exception
     */
    public static void replaceZipFileContent(String zipfile,Map<String,InputStream> addFiles,Map<String,Map<String,String>> replaceItems,String encode) throws Exception {
        List<ReplaceZipItem> zl = new ArrayList();
        if(null != replaceItems && replaceItems.size()>0){
            Iterator<String> its = replaceItems.keySet().iterator();
            while(its.hasNext()){
                String k = its.next();
                Map<String,String> v= replaceItems.get(k);
                zl.add(new ReplaceZipItem(k,v));
            }
        }
        replaceZipFileContent(zipfile,addFiles,(ReplaceZipItem[])zl.toArray(new ReplaceZipItem[0]),encode);
    }

    public static byte[] replaceFile(InputStream file,Map<String,String> chgvalue,String encode) throws IOException {
        ByteArrayOutputStream fo = new ByteArrayOutputStream();
        byte[] b = new byte[1024];
        int len = 0;
        while (( len = file.read( b ) ) > 0) {
            fo.write( b, 0, len );
        }
        String sb= new String(fo.toByteArray(),encode);
        if(null != sb && null != chgvalue){
            Iterator<String> its = chgvalue.keySet().iterator();
            while(its.hasNext()){
                String k= its.next();
                String v = chgvalue.get(k);
                if(log.isDebugEnabled())
                    log.debug("change text:\n"+sb+"\nold Str:"+k+"\nnew Str"+v);
                if(v==null)v="";
                sb = StringUtils.replace(sb, k, v);
            }

        }
        return sb.getBytes(encode);
    }

    public static void replaceFileContentByExcel(String file,String chgExcelFileName,String sheetName,String fieldNameForchangeId,List changeIdValues,String fieldOfInerFileName,String fieldNameForOldValue,String fieldNameForNewValue,String encode,String saveFile ) throws Exception{
        byte[] bs = replaceFileContentByExcel( file, chgExcelFileName, sheetName,fieldNameForchangeId, (String[])changeIdValues.toArray(new String[0]),fieldOfInerFileName,fieldNameForOldValue,fieldNameForNewValue,encode);
        saveStringBufferFile(new StringBuffer(new String(bs)), saveFile, false);
    }

    public static byte[] replaceFileContentByExcel(String file,String chgExcelFileName,String sheetName,String fieldNameForchangeId,String[] changeIdValues,String fieldOfInerFileName,String fieldNameForOldValue,String fieldNameForNewValue,String encode ) throws Exception {
        Object[] data = getReplaceDataFromExcel(chgExcelFileName,sheetName,fieldNameForchangeId,changeIdValues,fieldOfInerFileName,fieldNameForOldValue,fieldNameForNewValue);
        ReplaceZipItem[] rs = (ReplaceZipItem[])data[1];
        if(null != rs && rs.length>0) {
            return replaceFile(new FileInputStream(file), rs[0].getChgvalue(), encode);
        }else{
            return replaceFile(new FileInputStream(file), null, encode);
        }
    }

    public static Object[] getReplaceDataFromExcel(String chgExcelFileName,String sheetName,String fieldNameForchangeId,String[] changeIdValues,String filedNameForFileNameInZip,String fieldNameForOldValue,String fieldNameForNewValue) throws Exception {
        //get configuration data form excel
        HashMap<String,Map<String,Map<String,String>>> cache = new HashMap<String, Map<String, Map<String, String>>>();
        HashMap<String,Map<String,List<String>>> addCache = new HashMap<String, Map<String, List<String>>>();
        log.debug("begin to read config excel file "+chgExcelFileName);
        ExcelReader ex = new ExcelReader(new FileInputStream(new File(chgExcelFileName)));
        log.debug("readed config excel file "+chgExcelFileName);
        List<Map<String,String>> tm = ex.getSheepData(sheetName);
        for(Map<String,String> m:tm){
            String id = m.get(fieldNameForchangeId);
            String f = m.get(filedNameForFileNameInZip);
            String k = m.get(fieldNameForOldValue);
            String v = m.get(fieldNameForNewValue);
            if(StringUtils.isBlank(k) && StringUtils.isNotBlank(v)){
                File ft = new File(v);
                if(ft.exists() && ft.isDirectory()){
                    if(!addCache.containsKey(id))addCache.put(id,new HashMap<String, List<String>>());
                    if(!addCache.get(id).containsKey(f))addCache.get(id).put(f,new ArrayList<String>());
                    addCache.get(id).get(f).add(v);
                }
            }else{
                if(null == f)f="";
                if(!cache.containsKey(id))cache.put(id,new HashMap<String, Map<String, String>>());
                if(!cache.get(id).containsKey(f))cache.get(id).put(f,new HashMap<String, String>());
                cache.get(id).get(f).put(k,v);
            }
        }
        log.debug("get configuration data from excel");
        //get add new Files configuration data
        Map<String,ReplaceZipItem> replaces = new HashMap<String, ReplaceZipItem>();
        Map<String,List<String>> addFiles = new HashMap();
        if(null != changeIdValues){
            for(String chgid:changeIdValues){
                if(addCache.containsKey(chgid)){
                    addFiles.putAll(addCache.get(chgid));

                }
                Map m = cache.get(chgid);
                if(null != m){
                    Iterator<String> is = m.keySet().iterator();
                    while(is.hasNext()){
                        String file = is.next();
                        String filename=file;
                        ReplaceZipItem item = new ReplaceZipItem();
                        if(file.contains("!")){
                            String[] ts = file.split("!");
                            filename = ts[1];
                            item.setJars(new String[]{ts[0]});
                        }
                        item.setFileName(filename);
                        item.setChgvalue(cache.get(chgid).get(file));

                        replaces.put(file,item);


                    }
                }
            }
        }
        Map<String,InputStream> addFileInputs = new HashMap();
        if(null != addFiles && addFiles.size()>0){
            Iterator<String> its = addFiles.keySet().iterator();
            while(its.hasNext()){
                String head = its.next();
                List<String> ps = addFiles.get(head);
                for(String p:ps){
                    File f = new File(p);
                    if(f.exists()){
                        if(f.isDirectory()){
                            List<File> ls =FileUtils.getAllFile(p,null);
                            for(File ff:ls){
                                addFileInputs.put(getName(head,p,ff.getPath()),new FileInputStream(ff));
                            }
                        }else{
                            addFileInputs.put(getName(head,"",p),new FileInputStream(f));
                        }
                    }
                }
            }
        }
        log.debug("will add new file "+addFiles);
        log.debug("will replace file "+replaces.keySet());
        return new Object[]{addFiles,replaces.values().toArray(new ReplaceZipItem[0])};

    }
    /**
     * replace some content in file by excel
     * if filedNameForFileNameInZip is a folder and fieldNameForOldValue is null and fieldNameForNewValue is a file path , it will add file to zip file
     * @param zipfile
     * @param chgExcelFileName
     * @param sheetName
     * @param fieldNameForchangeId
     * @param changeIdValues
     * @param filedNameForFileNameInZip
     * @param fieldNameForOldValue
     * @param fieldNameForNewValue
     * @throws Exception
     */

    public static void replaceZipFileContentByExcel(String zipfile,String chgExcelFileName,String sheetName,String fieldNameForchangeId,String[] changeIdValues,String filedNameForFileNameInZip,String fieldNameForOldValue,String fieldNameForNewValue,String encode) throws Exception {
        Object[] ret = getReplaceDataFromExcel(chgExcelFileName,sheetName,fieldNameForchangeId,changeIdValues,filedNameForFileNameInZip,fieldNameForOldValue,fieldNameForNewValue);
        replaceZipFileContent(zipfile,(Map<String,InputStream>)ret[0],(ReplaceZipItem[])ret[1],encode);
        log.debug("changed zip file finished ");
    }
    static String getName(String header,String catalog,String filePath){
        return header+filePath.replaceAll("\\\\","/").substring(catalog.length());
    }

    public static List<String> findFiles(String dir,String fileNamelike,String fileNameNotLike,String[] keys){
    	List li = new ArrayList();
    	List ret = new ArrayList();
    	getAllDirectoryMarchFiles(new File(dir), fileNamelike, li);
		for(int i=0;i<li.size();i++){
			String f = (String)li.get(i);
			if(null != fileNameNotLike && !"".equals(fileNameNotLike) && f.indexOf(fileNameNotLike)>0){
				continue;
			}else{						
				try{
					boolean is= true;
					if(null != keys){
						StringBuffer sb  = getFileContentStringBuffer(f);
						for(String key:keys){
							if(sb.indexOf(key)<0){
								is = false;
								break;
							}
						}
					}
					if(is){
						ret.add(f);
					}
				}catch(Exception e){
					
				}
			}
		}
		return ret;
    }
    
    public static void replaceFilesBeginEndMark(String dir,String like,String notLike,String beginMark,String endMark,String newBeginMark,String newEndMark)throws Exception{
    	List<File> li = new ArrayList();
    	getAllDirectoryFiles(new File(dir),null,li);
    	if(null != li){
    		for(File f:li){
    			if(null != notLike && !"".equals(notLike) && f.getPath().indexOf(notLike)>=0){
    				continue;
    			}else{
    				if(null != like){
    					if(f.getPath().indexOf(like)>=0){
    						replaceFileBeginEndMark(f,beginMark,endMark,newBeginMark,newEndMark);
        					log.info(f);
    					}
    				}else{
    					replaceFileBeginEndMark(f,beginMark,endMark,newBeginMark,newEndMark);
    					log.info(f);
    				}
    			}
    		}
    	}
    }
    public static void replaceFileBeginEndMark(File f,String beginMark,String endMark,String newBeginMark,String newEndMark)throws Exception{
    	StringBuffer sb = new StringBuffer();
    	replaceStringBeginEndMark(getFileContentStringBuffer(new FileInputStream(f)).toString(),0,beginMark,endMark,newBeginMark,newEndMark,sb);    			
    	saveStringBufferFile(sb, f.getPath(), false);
    }
    public static void findFilesBetween(String dir,String like,String notLike,String beginMark,String endMark,boolean isIncludeMark,String targetFile)throws Exception{
    	List<File> li = new ArrayList();
    	StringBuffer ret = new StringBuffer();
    	getAllDirectoryFiles(new File(dir),null,li);
    	if(null != li){
    		for(File f:li){
    			if(null != notLike && !"".equals(notLike) && f.getPath().indexOf(notLike)>=0){
    				continue;
    			}else{
    				if(null != like){
    					if(f.getPath().indexOf(like)>=0){
    						findFileBetween(f,beginMark,endMark,isIncludeMark,ret);
        					log.info(f);
    					}
    				}else{
    					findFileBetween(f,beginMark,endMark,isIncludeMark,ret);
    					log.info(f);
    				}
    			}
    		}
    		saveStringBufferFile(ret, targetFile, false);
    	}
    }
    public static void findFileBetween(File f,String beginMark,String endMark,boolean isIncludeMark,StringBuffer sb)throws Exception{
    	StringBuffer ret = new StringBuffer();
    	findStringBetween(getFileContentStringBuffer((new FileInputStream(f))).toString(),0,beginMark,endMark,ret,isIncludeMark);
    	String[] rs = ret.toString().split("\n");
    	if(null != rs){
    		for(String s:rs){
    			sb.append(f.getPath()).append("	").append(s).append("\n");
    		}
    	}
    }
    
    public static void main(String[] args){
    	try{
    		//filterSameRowString("C:\\temp\\u_key.txt","C:\\temp\\u_key2.txt");
    		//String[] keys = getFileLines("C:\\temp\\key2.txt");
    		//findFiles("D:\\products\\UMobile\\Umobile_Globe\\html\\crm",".jsp","svn-base",new String[]{"ai:listbox"});
    		//StringBuffer sb = findFileBetween(new File("D:\\products\\UMobile\\Umobile_Globe\\html\\crm\\so\\workflow\\WorkflowInst.jsp")
    		//,"codeType=","\"",true);
    		/*String[] files = getFileContent("C:\\temp\\tt.txt");
    		if(null != files){
    			StringBuffer sb = new StringBuffer();
    			for(String f:files){
    				findFileBetween(new File(f.trim()),"codeType=\"","\'",true,sb);
    			}
    			System.out.println(sb);
    		}*/
    		
    		/*List<String> li = findFiles("D:\\my work\\asiainfo\\UMbile\\documents\\08ά��\\60-�����",".sql","svn",null);
    		for(String s:li){
    			System.out.println(s);
    		}*/
    		//String path = "C:\\log\\poc\\POC_old\\src\\main\\java\\com\\asiainfo\\crm\\inter\\exe\\webservice\\crm4openplat\\Crm4Open.java";
            /*List<String> ll = FileUtils.getSortCurDirFileNames("C:\\log\\log","update.desc");
            for(String l:ll){
                System.out.println(l);
            }*/
            /*FileUtils.replaceZipFileContentByExcel("D:/Work/asiainfo/umobile/CRM_R318/treasurebag/release/onside_crm1/tb.war",
                    "D:\\Work\\asiainfo\\umobile\\CRM_R318\\treasurebag\\release\\onside_tb\\change_crm1.xlsx",
                    "Variable","ChangeId",new String[]{"main_chg"},"ChangeFile","Variable","Value","utf-8");*/
            HashMap map = new HashMap();
            Map v = new HashMap();
            v.put("<f>ffff</f>","<t>v</t>\nee\n<e>eeee</e>");
            map.put("classes/tb_web.app",v);
            FileUtils.replaceZipFileContent("C:\\work\\treasurebag\\build\\console\\console_nj_115.zip",null,map,"utf-8");
            //FileUtils.replaceFileContentByExcel(null,"C:\\work\\umobile\\CRM_R318\\treasurebag\\build\\autodeploy\\AI-UMobile-ServerEnvironment-online.xlsx","Variable","ChangeId",new String[]{"nanjing_test_id"},"ChangeFile","Variable","Value","utf-8");
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    }
    
    
    public static void findStringBetween(String p,int start,String beginMark,String endMark,StringBuffer ret,boolean isIncludeMark)throws Exception{
    	int beginStart=0;
    	if(start==0){
    		beginStart = p.indexOf(beginMark);
    	}else{
    		beginStart = p.indexOf(beginMark,start);
    	}
	    if (beginStart < 0){	    	
	    	return;
	    }
	    int beginEnd = beginStart+beginMark.length();
	    int endStart = p.indexOf(endMark, beginEnd);
	    if (endStart < 0){	    	
	    	return;
	    }
	    int endEnd = endStart+endMark.length();
	    if(isIncludeMark){
	    	ret.append(p.substring(beginStart, endEnd)).append("\n");
	    }else{
	    	ret.append(p.substring(beginEnd, endStart)).append("\n");
	    }
	    findStringBetween(p,endEnd,beginMark,endMark,ret,isIncludeMark);
    }
    
    public static void replaceStringBeginEndMark(String p,int start,String beginMark,String endMark,String newBeginMark,String newEndMark,StringBuffer ret)throws Exception{
    	
    	int beginStart=0;
    	if(start==0){
    		beginStart = p.indexOf(beginMark);
    	}else{
    		beginStart = p.indexOf(beginMark,start);
    	}
	    if (beginStart < 0){
	    	if(start<p.length()){
	    		ret.append(p.substring(start));
	    	}
	    	return;
	    }
	    int beginEnd = beginStart+beginMark.length();
	    int endStart = p.indexOf(endMark, beginEnd);
	    if (endStart < 0){
	    	if(start<p.length()){
	    		ret.append(p.substring(start));
	    	}
	    	return;
	    }
	    int endEnd = endStart+endMark.length();
	    boolean appendStart=false,appendEnd=false;
	    
	   
	    for(;start<endEnd;start++){
		    if(null != newBeginMark && start>=beginStart && start<beginEnd){
		    	if(!appendStart){
		    		ret.append(newBeginMark);
		    		appendStart=true;
		    	}
		    	continue;
		    }
		    if(null != newEndMark && start>=endStart && start<endEnd){
		    	if(!appendEnd){
		    		ret.append(newEndMark);
		    		appendEnd=true;
		    	}
		    	continue;
		    }
	    	ret.append(p.charAt(start));
	    }
	    replaceStringBeginEndMark(p,endEnd,beginMark,endMark,newBeginMark,newEndMark,ret);	    
    }

    public static String[] getPackageFile(String c,String k)throws Exception{
        List li = null;
        int in = c.indexOf(k);
        int index = in;
        if(in>=0){
            li = new ArrayList();
            while(index<c.length()){
                StringBuffer sb = new StringBuffer(k);
                int j;
                for(j=(in+k.length());j<c.length();j++){
                    if( (c.charAt(j)>=48 && c.charAt(j)<=57) || (c.charAt(j)>=65 && c.charAt(j)<=90) || c.charAt(j)==46 || c.charAt(j)==95 || (c.charAt(j)>=97 && c.charAt(j)<=122)){
                        sb.append(c.charAt(j));
                    }else{
                        in = c.indexOf(k,j);
                        if(in>=0){
                            index = in;
                        }else{
                            index = c.length()+1;
                        }
                        break;
                    }
                }
                if(sb.length()>k.length()){
                    li.add(sb.toString());
                }
                if(j>=c.length()){
                    index = c.length()+1;
                }
                sb=null;
            }
        }
        if(null != li){
            return (String[])li.toArray(new String[0]);
        }else{
            return null;
        }

    }

    /**
     * ������ͬ���м�¼
     * @Function: filterSameRowString
     * @Description: �ú���Ĺ�������
     * @param sourFile
     * @param targetFile
     * @throws Exception
     * @return�����ؽ������
     * @author: robai
     * @date: 2011-7-18 ����10:32:37
     */
    public static void filterSameRowString(String sourFile,String targetFile)throws Exception{
    	List li = new ArrayList();
		StringBuffer sb = new StringBuffer();
		try{
			String[] rows = getFileContent(sourFile);
			if(null != rows){
				for(int i=0;i<rows.length;i++){
					if(!li.contains(rows[i].trim())){
						li.add(rows[i].trim());
						sb.append(rows[i].trim()+"\n");
					}
				}
			}
			
			saveStringBufferFile(sb,targetFile,false);
		}catch(Exception e){
			e.printStackTrace();
		}
    }
    
    


   /*public static void changeFileEncoding(String file,String encoding){
	     write(file, encoding,read(file,guessEncoding(file)));
	 }*/


   /*public static String guessEncoding(String filename) {
       return guessEncoding(new File(filename));
   }*/
   
   /*public static String guessEncoding(File filename) {
       try {
           CharsetPrinter charsetPrinter = new CharsetPrinter();
           String encode = charsetPrinter.guessEncoding(filename);
           return encode;
       }catch(Exception e){
           return null;
       }
   }*/


   /**

    * @describe Read file with specified encode

    * @param encoding

    * @return the content of the file in the form of string

    */

   public static String read(String fileName, String encoding) {



       String string = "";

       try {

           BufferedReader in = new BufferedReader(new InputStreamReader(

                   new FileInputStream(fileName), encoding));



           String str = "";

           while ((str = in.readLine()) != null) {

               string += str + "\n";

           }

           in.close();



       } catch (Exception ex) {

           ex.printStackTrace();

       }

       return string;

   }



   /**

    * @describe write str to fileName with specified encode

    * @param fileName

    * @param encoding

    * @param str

    * @return null

    */

   public static void write(String fileName, String encoding, String str) {

       try {

           Writer out = new BufferedWriter(new OutputStreamWriter(

                   new FileOutputStream(fileName), encoding));

           out.write(str);

           out.close();

       } catch (Exception ex) {

           ex.printStackTrace();

       }

   }


   
   
   
   public static String[] findFileNameFromJarFloader(String path, String key)
   throws Exception
 {
   List list = new ArrayList();
   File file = new File(path);
   if (file.isDirectory())
     findFileNameFromJarFloader(list, file, key);

   return ((String[])(String[])list.toArray(new String[list.size()]));
 }

 private static void findFileNameFromJarFloader(List list, File f, String key) throws Exception
 {
   if (f.isDirectory()) {
     File[] fs = f.listFiles();
     if (fs != null)
       for (int i = 0; i < fs.length; ++i)
         if (fs[i].isFile()) {
           if ((getExtension(fs[i].getPath()).equalsIgnoreCase("jar")) || (getExtension(fs[i].getPath()).equalsIgnoreCase("zip"))) {
             String[] ret = findFileNameFromJar(fs[i].getPath(), key);
             if (ret != null)
               for (int k = 0; k < ret.length; ++k)
                 list.add(fs[i].getPath() + ":" + ret[k]);

           }

         }
         else if (fs[i].isDirectory()) {
           if (fs[i].getName().indexOf(key) >= 0)
             list.add(fs[i].getPath() + ":");

           findFileNameFromJarFloader(list, fs[i], key);
         }
   }
 }
 
 public static String[] findFileNameFromJar(String jarfilepath, String key)
 throws Exception
{
 ZipFile zipFile = new ZipFile(jarfilepath);
 Enumeration e = zipFile.entries();
 List list = new ArrayList();
 while (e.hasMoreElements()) {
   ZipEntry zipEntry = (ZipEntry)(ZipEntry)e.nextElement();
   if (zipEntry.getName().indexOf(key) >= 0)
     list.add(zipEntry.getName());
 }

 return ((String[])(String[])list.toArray(new String[list.size()]));
}
 
 public static void delFolder(String folderPath) {
     try {
        delAllFile(folderPath); //ɾ����������������
        String filePath = folderPath;
        filePath = filePath.toString();
        File myFilePath = new File(filePath);
        myFilePath.delete(); //ɾ����ļ���
     } catch (Exception e) {
       e.printStackTrace(); 
     }
}

//param path �ļ���������·��
 public static boolean delAllFile(String path) {
     boolean flag = false;
     File file = new File(path);
     if (!file.exists()) {
       return flag;
     }
     if (!file.isDirectory()) {
       return flag;
     }
     String[] tempList = file.list();
     File temp = null;
     for (int i = 0; i < tempList.length; i++) {
        if (path.endsWith(File.separator)) {
           temp = new File(path + tempList[i]);
        } else {
            temp = new File(path + File.separator + tempList[i]);
        }
        if (temp.isFile()) {
           temp.delete();
        }
        if (temp.isDirectory()) {
           delAllFile(path + "/" + tempList[i]);//��ɾ���ļ���������ļ�
           delFolder(path + "/" + tempList[i]);//��ɾ����ļ���
           flag = true;
        }
     }
     return flag;
 }

    public static String getProtocolFile(String url) throws IOException {
        List<FileInfo> fs = getAllProtocolFiles(url,null,false);
        if(null != fs && fs.size()>0){
            return getFileContentString(fs.get(0).getInputStream());
        }
        return null;
    }

    public static List<FileInfo> getAllProtocolFiles(String url,String[] expresses,boolean isExpressInclude){
        if(StringUtils.isNotBlank(url)){
            int n = url.indexOf(":");
            if(n>0){
                String protocol = url.substring(0,url.indexOf(":"));
                if(protocol.equalsIgnoreCase("file")){
                    return getLocalFiles(url.substring(n + 1), expresses, isExpressInclude);
                }
                if(protocol.equalsIgnoreCase("classpath")){
                    return getClassPathFiles(url.substring(n+1),expresses,isExpressInclude);
                }
                if(protocol.equalsIgnoreCase("ftp")){
                    return getFtpFiles(url.substring(n+1),expresses,isExpressInclude);
                }
            }

        }
        return null;
    }

    public static  List<FileInfo> getLocalFiles(String url,String[] expresses,boolean isExpressInclude){
        List<File> list = new ArrayList();
        getAllLocalFiles(new File(url), expresses, isExpressInclude, list);
        return file2FileInfos("file:",list);
    }

    /**
     * get text content in classpath
     * @param path
     * @return
     * @throws IOException
     */
    public static String getClassSourceText(String path) throws IOException {
        InputStream in = FileUtils.class.getClassLoader().getResourceAsStream(path);
        if(null != in){
            return getFileContentString(in);
        }
        return null;
    }
    public static  List<FileInfo> getClassPathFiles(String url,String[] expresses,boolean isExpressInclude){
        InputStream in = FileUtils.class.getClassLoader().getResourceAsStream(url);
        if(null == in)
            in = Thread.currentThread().getContextClassLoader().getResourceAsStream(url);
        if(null == in)return null;
        FileInfo f = new FileInfo();
        f.setInputStream(in);
        f.setAllName(url);
        f.setPath(url.substring(0,url.lastIndexOf(".")));
        List ret = new ArrayList();
        ret.add(f);
        return ret;
    }
    public static  List<FileInfo> getFtpFiles(String url,String[] expresses,boolean isExpressInclude){
        throw new RuntimeException("not support get ftp file now.");
    }
    static List<FileInfo> file2FileInfos(String protocol,List<File> fs){
        List<FileInfo> ret = new ArrayList();
        for(File f:fs){
            FileInfo fi =file2FileInfo(protocol,f);
            if(null != fi)
                ret.add(fi);
        }
        if(ret.size()>0)
            return ret;
        return null;
    }
    static FileInfo file2FileInfo(String protocol,File f){
        FileInfo fi = new FileInfo();
        fi.setProtocol(protocol);
        fi.setAllName(protocol+f.getPath());
        fi.setName(f.getName());
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(f.lastModified());
        fi.setUpdateDate(cal.getTime());
        try{
            fi.setInputStream(new FileInputStream(f));
        }catch (Exception e){
            e.printStackTrace();
        }
        fi.setPath(f.getParentFile().getPath());
        return fi;
    }
    public static boolean saveAllProtocolFile(String url,String content) throws Exception {
        if(StringUtils.isNotBlank(url)){
            int n = url.indexOf(":");
            if(n>0){
                String protocol = url.substring(0,url.indexOf(":"));
                if(protocol.equalsIgnoreCase("file")){
                    saveStringBufferFile(new StringBuffer(content),url.substring(n + 1),false);
                    return true;
                }
                if(protocol.equalsIgnoreCase("ftp")){
                    return saveFtpFiles(url.substring(n + 1), content);
                }
            }

        }
        return false;
    }
    public static boolean saveFtpFiles(String url,String content){
        throw new RuntimeException("not support ftp save now.");
    }

    public static List<Map> getDirectoryStructure(String file,List suffix,List excludeNames){
        List ret = new LinkedList();
        file=file.replaceAll("\\\\","/");
        if(file.endsWith("/")){
            file = file.substring(0,file.length()-1);
        }
        getDirectoryStructureFiles(file,1,new File(file),suffix,ret,excludeNames);
        return ret;
    }
    /**
     * get match all files by suffix, one record with {id,pId,name,path,type[file,dir],suffix}
     * @param
     * @param suffix
     * @param childParentList
     */
    protected static void getDirectoryStructureFiles(String rootpath,long parentId,File f, List suffix, List childParentList,List excludeNames) {
        File[] fs;
        if (null != f && f.isDirectory()) {
            fs = f.listFiles();
            for (int i = 0; i < fs.length; i++) {
                if(ArrayUtils.isInStringArray(excludeNames,fs[i].getName())){
                    continue;
                }
                long id = new Double(parentId * Math.pow(10 ,(String.valueOf(fs.length).length()))+i).longValue();
                if (fs[i].isDirectory()) {
                    HashMap m = new HashMap();
                    m.put("id",id);
                    m.put("pId",parentId);
                    m.put("name",fs[i].getName());
                    m.put("path",f.getPath().replaceAll("\\\\","/").substring(rootpath.length()));
                    m.put("type","dir");
                    m.put("suffix","");
                    childParentList.add(m);
                    getDirectoryStructureFiles(rootpath,id,fs[i], suffix, childParentList,excludeNames);
                }else{
                    if (null!= suffix && suffix.size()>0 ) {
                        if(ArrayUtils.isInStringArray(suffix,fs[i].getName().substring(fs[i].getName().lastIndexOf(".")+1))) {
                            HashMap m = new HashMap();
                            m.put("id",id);
                            m.put("pId",parentId);
                            m.put("name",FileUtils.getFileSimpleName(fs[i].getName()));
                            m.put("path",fs[i].getPath().replaceAll("\\\\","/").substring(rootpath.length()));
                            m.put("type","file");
                            m.put("suffix",fs[i].getName().substring(fs[i].getName().lastIndexOf(".")+1));
                            childParentList.add(m);
                        }
                    } else {
                        HashMap m = new HashMap();
                        m.put("id",id);
                        m.put("pId",parentId);
                        m.put("name",FileUtils.getFileSimpleName(fs[i].getName()));
                        m.put("path",fs[i].getPath().replaceAll("\\\\","/").substring(rootpath.length()));
                        m.put("type","file");
                        m.put("suffix",fs[i].getName().substring(fs[i].getName().lastIndexOf(".")+1));
                        childParentList.add(m);
                    }
                }

            }
        }
    }

}
