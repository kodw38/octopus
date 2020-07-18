package com.octopus.utils.zip;

import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.file.FileUtils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.*;

/**
 * User: wf
 * description:
 * use this function ,please set jvm  -Xss2m
 * Date: 2008-10-20
 * Time: 17:12:23
 */
public class ZipUtil {	
	private static Logger log = Logger.getLogger(ZipUtil.class);
	static String[] ziptypes = {"jar","zip","war","ear"};
	
	private static final Integer BUFFER_SIZE = 512;
	
	public static void compressFile(String sourceFile, String jarFile)throws IOException{
	    File f = new File(sourceFile);  
	    String base = f.getName();  
	    JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile));  
	    jos.putNextEntry(new ZipEntry(base));
	      
	    BufferedInputStream bin = new BufferedInputStream(new FileInputStream(f));  
	      
	    byte[] data = new byte[BUFFER_SIZE];  
	    while ((bin.read(data)) != -1) {  
	        jos.write(data);  
	    }  
	    bin.close();  
	    jos.closeEntry();  
	    jos.close();  
	}  

	public static void addFile(OutputStream jarOutputStream,File addFile) throws IOException{
		String base = addFile.getName();
		JarOutputStream jos = new JarOutputStream(jarOutputStream);
		jos.putNextEntry(new ZipEntry(base));
		BufferedInputStream bin = new BufferedInputStream(new FileInputStream(addFile));  	      
	    byte[] data = new byte[BUFFER_SIZE];  
	    while ((bin.read(data)) != -1) {  
	        jos.write(data);  
	    }  
	    bin.close();
	    jos.closeEntry(); 
	    jos.close();
	}

	public static void addFile(JarOutputStream jos,String fileName,InputStream addFile) throws IOException{

		jos.putNextEntry(new ZipEntry(fileName));
        if(null != addFile){
            BufferedInputStream bin = new BufferedInputStream(addFile);
            byte[] data = new byte[bin.available()];
            while ((bin.read(data)) >0) {
                jos.write(data);
            }
            bin.close();
        }
	    jos.closeEntry(); 
	}

	/** 
	 * ѹ���ļ��м������ļ��� 
	 * @param source String Դ�ļ���,��: d:/tmp 
	 * @param dest String Ŀ���ļ�,��: e:/tmp.jar 
	 * @throws java.io.IOException
	 */  
	public static void jarFolder(String source, String dest)throws IOException{  
	    JarOutputStream jos = new JarOutputStream(new FileOutputStream(dest));  
	    jos.setLevel(Deflater.BEST_COMPRESSION);  
	    jarFolder(jos, new File(source),"");  
	    jos.close();  
	}  
	
	private static void jarFolder(JarOutputStream jos, File f, String base)throws IOException{  
	    //System.out.println("compressJarFolder:"+f.getName()+" base:"+base);
	    if(f.isFile()){  
	        jarFile(jos, f, base);  
	    }else if(f.isDirectory()){  
	        jarDirEntry(jos, f, base);  	          
	        String[] fileList = f.list();  
	        for(String file:fileList){  
	            String newSource = f.getAbsolutePath() + File.separator + file;  
	            File newFile = new File(newSource);  
	            String newBase = base + "/" + f.getName()+"/"+newFile.getName();  
	            if(base.equals("")){  
	                newBase = newFile.getName();//f.getName()+"/"+newFile.getName();  
	            }else{  
	                newBase = base + "/" + newFile.getName();  
	            }  	            
	            //logger.info("����ѹ���ļ��� "+newSource+"    �� "+newBase);  
	            jarFolder(jos, newFile, newBase);  	              
	        }//for  
	          
	    }//if  
	}  

//	ѹ�������ļ�  
	private static void jarFile(JarOutputStream jos, File f, String base)throws IOException{  	      
	    jos.putNextEntry(new ZipEntry(base));  	      
	    BufferedInputStream bin = new BufferedInputStream(new FileInputStream(f));  	      
	    byte[] data = new byte[BUFFER_SIZE];  
	    int iRead = 0;  
	    while ((iRead = bin.read(data)) != -1) {  
	        jos.write(data,0,iRead);  
	    }  
	    bin.close();  
	    jos.closeEntry();  
	}  
	
	private static void jarDirEntry(JarOutputStream jos, File f, String base)throws IOException{  
	    jos.putNextEntry(new ZipEntry(base + "/"));  	      
	    jos.closeEntry();  
	} 


 

	
	/**
	 * ��һ���ļ�ѹ������һ���ļ�
	 * @param filepath
	 * @param zipFilePath
	 * @throws Exception
	 */
	public static void zipTxtFile(String filepath,String zipFilePath)throws Exception{
		FileInputStream file = new FileInputStream(new File(filepath));
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(new File(zipFilePath)));
		try{				
			out.putNextEntry(new ZipEntry(ZipUtil.encode(filepath.substring(filepath.lastIndexOf("\\")+1),"UTF-8")));
			int tempbyte;
            while ((tempbyte = file.read()) != -1) {
                out.write(tempbyte);
            }
		}finally{
			file.close();	
			out.close();
		}
		
	}
	
	/**
	 * @param response
	 * @param sb
	 * @param zipFileName
	 * @throws Exception
	 */
    public static void zipTxtToResponse(HttpServletResponse response,StringBuffer sb,String zipFileName) throws Exception {

        response.setHeader("Content-Disposition","attachment;filename= "+ZipUtil.encode(zipFileName+".zip","UTF-8"));
        response.setContentType("application/x-msdownload");

        //����
        //ѹ��
        ZipOutputStream out = new ZipOutputStream(response.getOutputStream());
        out.putNextEntry(new ZipEntry(ZipUtil.encode(zipFileName,"UTF-8")));
        ByteArrayInputStream in = new ByteArrayInputStream(sb.toString().getBytes());
        byte[] b = new byte[2048];
		while (in.read(b) != -1) 
		    out.write(b);
		in.close();
        out.close();
        response.flushBuffer();
    }
    

    /**
     * ��ȡһ��HttpServletResponse���ļ�ѹ�����������Ϻ���Ҫ����response.flushBuffer();
     * @param response
     * @param zipFileName
     * @return
     * @throws Exception
     */
    public static OutputStream getZipOutputStreamFromResponse(HttpServletResponse response,String zipFileName) throws Exception {
        response.setHeader("Content-Disposition","attachment;filename= "+ZipUtil.encode(zipFileName+".zip","UTF-8"));
        response.setContentType("application/x-msdownload");
        
        ZipOutputStream out = new ZipOutputStream(response.getOutputStream());
        out.putNextEntry(new ZipEntry(ZipUtil.encode(zipFileName,"UTF-8")));
        return out;
    }
    
    /**
     * ADD BY CAOHX
     * �õ�ZipOutputStream����
     * @param response
     * @param zipFileName
     * @return
     * @throws java.io.IOException
     */
/*    public static ZipOutputStream getZipOutputStreamForXlsFromResponse(HttpServletResponse response,String zipFileName) throws Exception {
        return getZipOutputStreamForXlsFromResponse(response, "application/x-msdownload", zipFileName);
    }
*/    
    /**
     * ADD BY CAOHX
     * �õ�ZipOutputStream����
     * @param response
     * @param zipFileName
     * @return	new String((zipFileName+".zip").getBytes(System.getProperty("file.encoding")),"UTF-8")
     * @throws java.io.IOException
     */
/*    public static ZipOutputStream getZipOutputStreamForXlsFromResponse(HttpServletResponse response,String contentType,String zipFileName) throws Exception {
    	//response.setHeader("Content-Disposition","attachment;filename= "+ZipUtil.encode(zipFileName+".zip","UTF-8"));
    	response.setHeader("Content-Disposition","attachment;filename= "+new String((zipFileName+".zip").getBytes(System.getProperty("file.encoding")),"UTF-8"));
        response.setContentType(contentType);
        ZipOutputStream out = new ZipOutputStream(response.getOutputStream());
        return out;
    }
*/    /**
     * ѹ���ļ�����entry�ļ�
     * @param out
     * @throws java.io.IOException
     */
    public static void addZipEntry(ZipOutputStream out,String zipFileName) throws Exception {
    	out.putNextEntry(new ZipEntry(ZipUtil.encode(zipFileName,"UTF-8")));
    }

    public static void unTarGz(String targzfile, String targzFilePath) {
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        GZIPInputStream gzis = null;
        TarArchiveInputStream tais = null;
        OutputStream out = null;
        try {
            //要解压到某个指定的目录下
            fis = new FileInputStream(targzfile);
            bis = new BufferedInputStream(fis);
            gzis = new GZIPInputStream(bis);
            tais = new TarArchiveInputStream(gzis);
            TarArchiveEntry tae = null;
            while ((tae = tais.getNextTarEntry()) != null) {
                File tmpFile = new File(targzFilePath + "/" + tae.getName());
                if (tae.isDirectory()) {
                    //使用 mkdirs 可避免因文件路径过多而导致的文件找不到的异常
                    tmpFile.mkdirs();
                    continue;
                }
                out = new FileOutputStream(tmpFile);
                int length = 0;
                byte[] b = new byte[1048576];
                while ((length = tais.read(b)) != -1) {
                    out.write(b, 0, length);
                }
                out.flush();
                out.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (tais != null) tais.close();
                if (gzis != null) gzis.close();
                if (bis != null) bis.close();
                if (fis != null) fis.close();
                if (out != null) {
                    out.flush();
                    out.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 
     * @Function: unZipFile
     * @Description: ��ѹ���ļ�
     * @return�����ؽ������
     * @author: robai
     * @date: 2011-2-16 ����08:57:51
     */
    public static void unZipFile(String zipFilePath,String unZipPath)throws Exception{
        if(zipFilePath.endsWith(".tar.gz")){
            unTarGz(zipFilePath,unZipPath);
        }else {
            ZipFile zipfile = new ZipFile(zipFilePath);
            if (null != zipfile) {
                File zipdir = new File(unZipPath);
                if (!zipdir.exists()) {
                    zipdir.mkdirs();
                }
                Enumeration eu = zipfile.entries();
                while (eu.hasMoreElements()) {
                    InputStream in = null;
                    FileOutputStream out = null;

                        java.util.zip.ZipEntry zipEntry = (java.util.zip.ZipEntry) eu.nextElement();
                    try {
                        String fileName = zipEntry.getName();
                        if (fileName.endsWith("/")) {
                            new File(unZipPath + File.separator + fileName).mkdir();
                            continue;
                        }
                        File f = new File(unZipPath + File.separator + fileName);
                        if (!f.getParentFile().isDirectory()) {
                            boolean b = f.getParentFile().mkdirs();
                            if(!b){
                                if(f.getParentFile().isFile() && f.getParentFile().exists()){
                                    f.getParentFile().delete();
                                    f.getParentFile().mkdirs();
                                }
                                log.error("mkdirs fail:"+f.getParentFile().getPath());
                            }
                        }
                        in = zipfile.getInputStream(zipEntry);
                        out = new FileOutputStream(f);

                        byte[] by = new byte[1024];
                        int c;
                        while ((c = in.read(by)) != -1) {
                            out.write(by, 0, c);
                        }
                        out.close();
                        in.close();
//                    log.info("unzip "+zipFilePath+"�е�"+fileName+"��Ŀ¼:"+f.getPath());
                    }catch (Exception e){

                        log.error("write file fail:"+zipEntry.getName()+" , maybe the file not exist or replace by same name directory.");
                    } finally {
                        if (null != out)
                            out.close();
                        if (null != in)
                            in.close();
                    }
                }
            }
        }
    }
    
    public static String encode(String s, String enc)throws Exception{
        BitSet dontNeedEncoding;
        int caseDiff = ('a' - 'A');

        dontNeedEncoding = new BitSet(256);
        int i;
        for (i = 'a'; i <= 'z'; i++) {
            dontNeedEncoding.set(i);
        }
        for (i = 'A'; i <= 'Z'; i++) {
            dontNeedEncoding.set(i);
        }
        for (i = '0'; i <= '9'; i++) {
            dontNeedEncoding.set(i);
        }
        dontNeedEncoding.set(' '); /* encoding a space to a + is done
                                    * in the encode() method */
        dontNeedEncoding.set('-');
        dontNeedEncoding.set('_');
        dontNeedEncoding.set('.');
        dontNeedEncoding.set('*');



          boolean needToChange = false;
          boolean wroteUnencodedChar = false;
          int maxBytesPerChar = 10; // rather arbitrary limit, but safe for now
          StringBuffer out = new StringBuffer(s.length());
          ByteArrayOutputStream buf = new ByteArrayOutputStream(maxBytesPerChar);

          OutputStreamWriter writer = new OutputStreamWriter(buf, enc);

          for (i = 0; i < s.length(); i++) {
              int c = (int) s.charAt(i);
              //System.out.println("Examining character: " + c);
              if (dontNeedEncoding.get(c)) {
                  if (c == ' ') {
                      c = '+';
                      needToChange = true;
                  }
                  //System.out.println("Storing: " + c);
                  out.append((char)c);
                  wroteUnencodedChar = true;
              } else {
                  // convert to external encoding before hex conversion
                  try {
                      if (wroteUnencodedChar) { // Fix for 4407610
                              writer = new OutputStreamWriter(buf, enc);
                          wroteUnencodedChar = false;
                      }
                      writer.write(c);
                      /*
                       * If this character represents the start of a Unicode
                       * surrogate pair, then pass in two characters. It's not
                       * clear what should be done if a bytes reserved in the
                       * surrogate pairs range occurs outside of a legal
                       * surrogate pair. For now, just treat it as if it were
                       * any other character.
                       */
                      if (c >= 0xD800 && c <= 0xDBFF) {
                          /*
                            System.out.println(Integer.toHexString(c)
                            + " is high surrogate");
                          */
                          if ( (i+1) < s.length()) {
                              int d = (int) s.charAt(i+1);
                              /*
                                System.out.println("\tExamining "
                                + Integer.toHexString(d));
                              */
                              if (d >= 0xDC00 && d <= 0xDFFF) {
                                  /*
                                    System.out.println("\t"
                                    + Integer.toHexString(d)
                                    + " is low surrogate");
                                  */
                                  writer.write(d);
                                  i++;
                              }
                          }
                      }
                      writer.flush();
                  } catch(IOException e) {
                      buf.reset();
                      continue;
                  }
                  byte[] ba = buf.toByteArray();
                  for (int j = 0; j < ba.length; j++) {
                      out.append('%');
                      char ch = Character.forDigit((ba[j] >> 4) & 0xF, 16);
                      // converting to use uppercase letter as part of
                      // the hex value if ch is a letter.
                      if (Character.isLetter(ch)) {
                          ch -= caseDiff;
                      }
                      out.append(ch);
                      ch = Character.forDigit(ba[j] & 0xF, 16);
                      if (Character.isLetter(ch)) {
                          ch -= caseDiff;
                      }
                      out.append(ch);
                  }
                  buf.reset();
                  needToChange = true;
              }
          }

          return (needToChange? out.toString() : s);
      }

    public static File[] unzipJar2TempFloder(String zip) throws Exception {
        ZipFile file = getZipFile(zip);
        if (null != file) {
            return saveZipEntry2Temp(file, findjars(file));
        }
        return null;
    }

    public static ZipFile getZipFile(String jarpath) throws IOException {
        return new ZipFile(jarpath);
    }

    public static ZipEntry[] findjars(ZipFile file) throws IOException {
        Enumeration eu = file.entries();
        List li = new ArrayList();
        while (eu.hasMoreElements()) {
            ZipEntry en = (ZipEntry) eu.nextElement();
            if (en.getName().endsWith(".jar")) {
                li.add(en);
            }
        }
        if (li.size() > 0) {
            return (ZipEntry[]) li.toArray(new ZipEntry[0]);
        } else {
            return null;
        }
    }
    public static Map<String,InputStream> findFiles(ZipFile file,String path,String endwith) throws IOException {
        Enumeration eu = file.entries();
        Map<String,InputStream> map = new HashMap();
        if(null !=path) {
            path = path.replaceAll("\\\\", "/");
        }
        while (eu.hasMoreElements()) {
            ZipEntry en = (ZipEntry) eu.nextElement();
            if(en.getName().contains("zip")){
                System.out.println();
            }
            if ((null == path || (null != path && en.getName().indexOf(path)>=0)) && (null == endwith||(null != endwith && en.getName().endsWith(endwith)))) {
                InputStream in = file.getInputStream(en);
                map.put(en.getName(),in);
            }
        }
        if(map.size()>0)
            return map;
        return null;
    }
    public static List<String> findFileNames(ZipFile file,String path,String endwith) throws IOException {
        Enumeration eu = file.entries();
        List<String> ret = new LinkedList();
        if(null !=path) {
            path = path.replaceAll("\\\\", "/");
        }
        while (eu.hasMoreElements()) {
            ZipEntry en = (ZipEntry) eu.nextElement();
            if ((null == path || (null != path && en.getName().indexOf(path)>=0)) && (null == endwith||(null != endwith && en.getName().endsWith(endwith)))) {
                InputStream in = file.getInputStream(en);
                ret.add(en.getName());
            }
        }
        if(ret.size()>0)
            return ret;
        return null;
    }
    public static Map<String,InputStream> getZipFiles(String filepath,String endWith)throws IOException {
        ZipFile zf = new ZipFile(filepath);
        if(null != zf) {
            return findFiles(zf, null, endWith);
        }
        return null;
    }
    public static List<String> getZipFileNames(String filepath,String endWith)throws IOException{
        ZipFile zf = new ZipFile(filepath);
        if(null != zf) {
            return findFileNames(zf, null, endWith);
        }
        return null;
    }
    public static Map<String,InputStream> getZipFiles(URI path,String endWith) throws IOException {
        String p = path.toString();
        log.info("load files from "+p);
        String pro = p.substring(0,p.indexOf("/"));
        String ph = p.substring(p.indexOf("/"));
        Stack spro = StringUtils.splitWithStack(pro,':');
        String[] sph = StringUtils.split(ph,'!');
        File file=null;
        int n=0;
        while(spro.size()>0) {
            String pt = (String) sph[n];
            String proto = (String) spro.pop();
            if("file".equals(proto)){
                File f = new File(pt);
                if(f.isFile()){
                    file = f;
                }else{

                }
            }else if("jar".equals(proto)){
                if(null != file) {
                    ZipFile zf = new ZipFile(file);
                    return findFiles(zf,pt.substring(1),endWith);
                }
            }
            n++;
        }
        return null;
    }
    public static boolean isJar(String file){
		if(null != file){
			String subfix = file.substring(file.lastIndexOf(".")+1);
			for(String type:ziptypes){
				if(type.equals(subfix))
					return true;
			}
		}
		return false;
	}


    
    /**
     * 
     * @param in
     * @param out
     * @param rzis 
     * 	if ReplaceZipItem.Jars=null means not replace in sub jars.
     *  if ReplaceZipItem.Jars.length=0 means replace in all sub jars.
     *  is ReplaceZipItem.Jars.length>0 means replace in refer sub jars.
     * @throws Exception
     */
	public static void changeFileInJar(JarInputStream in,JarOutputStream out,ReplaceZipItem[] rzis,String encode) throws Exception{
		JarEntry jarEntry = null;
        if(null == in)return;
        while (( jarEntry = in.getNextJarEntry() ) != null) {
            log.error("do file:"+jarEntry.getName());
            if (jarEntry.isDirectory()) {
                String name = jarEntry.getName();
            	out.putNextEntry(new ZipEntry(name));
            	out.closeEntry();
            	
            	//add file in folder
            	if(null != rzis){
                    for(ReplaceZipItem it:rzis){
                        if(it.getFileName().equals(jarEntry.getName())){
                            log.error("do file:"+jarEntry.getName());
                            Map<String,String> chgs = it.getChgvalue();
                            Iterator<String> its = chgs.keySet().iterator();
                            while(its.hasNext()){
                                String k= its.next();
                                String v = chgs.get(k);
                                File f = new File(v);
                                if(f.exists() && null != k && !"".equals(k)){
                                    ZipUtil.addFile(out,jarEntry.getName()+k,new FileInputStream(f));
                                }
                            }
                        }
                    }
                }
            	/*System.out.println(name);*/
            	changeFileInJar(in,out,rzis,encode);
            }else if(isJar(jarEntry.getName())){
            	out.putNextEntry(new ZipEntry(jarEntry.getName()));
            	ByteArrayOutputStream fo = new ByteArrayOutputStream(); 
            	byte[] b = new byte[1024];		                               
                int len = 0;
                while (( len = in.read( b ) ) > 0) {
                    fo.write( b, 0, len );
                }
                fo.close();
                ByteArrayInputStream fi = new ByteArrayInputStream(fo.toByteArray());
            	boolean isSearchInterJar=false;
                if(null != rzis ){
                	for(ReplaceZipItem rzi :rzis){
                		if(null != rzi.getJars()){
                			if(rzi.getJars().length==0){
                    			isSearchInterJar = true;
                    			break;
                			}
                			if(ArrayUtils.isInStringArray(rzi.getJars(), jarEntry.getName())){
                				isSearchInterJar = true;
                				break;
                			}
                		}
                		
                	}
                	if(isSearchInterJar){
		                JarInputStream zipin = new JarInputStream(fi);
		            	ByteArrayOutputStream tt  =new ByteArrayOutputStream();
		            	JarOutputStream zipout = new JarOutputStream(tt);
                        /*System.out.println(fi);*/
		            	changeFileInJar(zipin,zipout,rzis,encode);
		            	zipout.finish();
		            	zipout.close();            	
		            	fi = new ByteArrayInputStream(tt.toByteArray());            	
		            	zipin.close();       
                	}
                }
                
            	len = 0;
                while (( len = fi.read( b ) ) > 0) {
                    out.write( b, 0, len );
                }                
            	fi.close();            	
            	out.closeEntry();
            }else{
            	boolean isadd = true;
                if(null != rzis){
                    for(ReplaceZipItem it:rzis){
                        if(it.getFileName().endsWith("/")){
                            Map<String,String> chgs = it.getChgvalue();
                            Iterator<String> its = chgs.keySet().iterator();
                            while(its.hasNext()){
                                String k= its.next();
                                String v = chgs.get(k);
                                File f = new File(v);
                                if(f.exists() && null != k && !"".equals(k)){
                                    if((it.getFileName()+k).equals(jarEntry.getName())){
                                        isadd=false;
                                        break;
                                    }
                                }
                            }
                            if(!isadd){
                                break;
                            }
                        }
                    }
                }
            	if(!isadd){
            		continue;
            	}

            	/*System.out.println(jarEntry.getName());*/
            	out.putNextEntry(new ZipEntry(jarEntry.getName()));
            	boolean isReplace=false;
            	Map<String,String> chgvalue = null;
            	if(null != rzis){
            		for(ReplaceZipItem r:rzis){            			
            			if(null != r.getFileName() && r.getFileName().equals(jarEntry.getName()) && null != r.getChgvalue() && r.getChgvalue().size()>0){
            				isReplace= true;
            				chgvalue = r.getChgvalue();
            			}
            		}                    
            	}
            	if(isReplace){
                    out.write(FileUtils.replaceFile(in, chgvalue, encode));

            	}else{
            		try{
            		byte[] b = new byte[1024];		                               
                    int len = 0;
                    while (( len = in.read( b ) ) > 0) {
                        out.write( b, 0, len );
                    }
            		}catch(Exception ex){            			
            			System.out.println(jarEntry.getName());
            			throw ex;
            		}
            	}
            	out.closeEntry();
            }
        }
	}

    public static ByteArrayInputStream changeFile(InputStream in,ReplaceZipItem[] rzis,String encode) throws Exception{
    	BufferedInputStream bis = new BufferedInputStream( in );
    	JarInputStream zipFile = new JarInputStream(bis);

    	ByteArrayOutputStream retout = new ByteArrayOutputStream();
    	JarOutputStream jarout = new JarOutputStream(retout);
    	jarout.setLevel(Deflater.BEST_COMPRESSION);
    	changeFileInJar(zipFile,jarout,rzis,encode);
    	jarout.finish();
        ByteArrayInputStream ret = new ByteArrayInputStream(retout.toByteArray());        
        jarout.close();
        retout.close();
        in.close();
        bis.close();
        zipFile.close();
        return ret ;
    }

    /**
     * replace some content in file in zip file or add some files to zip file
     * @param in source zip file
     * @param appendFiles the new files need add to zip file, key is the path in zip file include new file name.
     * @param rzis replace some content files info .
     * @return
     * @throws Exception
     */
    public static ByteArrayInputStream changeFile(InputStream in,Map<String,InputStream> appendFiles,ReplaceZipItem[] rzis,String encode)throws Exception{
        if(null != appendFiles && appendFiles.size()>0){
            BufferedInputStream tin = new BufferedInputStream( in );
            JarInputStream tj = new JarInputStream(tin);

            ByteArrayOutputStream to = new ByteArrayOutputStream();
            JarOutputStream out = new JarOutputStream(to);
            out.setLevel(Deflater.BEST_COMPRESSION);
            Iterator<String> its = appendFiles.keySet().iterator();
            while(its.hasNext()){
                String k = its.next();
                addFile(out,k,appendFiles.get(k));
            }
            copyFile(tj,out);
            out.finish();
            out.close();
            tj.close();
            in = new ByteArrayInputStream(to.toByteArray());
        }
        BufferedInputStream bis = new BufferedInputStream( in );
        JarInputStream zipFile = new JarInputStream(bis);
        ByteArrayOutputStream retout = new ByteArrayOutputStream();
        JarOutputStream jarout = new JarOutputStream(retout);
        jarout.setLevel(Deflater.BEST_COMPRESSION);

        retout = new ByteArrayOutputStream();
        jarout = new JarOutputStream(retout);
        changeFileInJar(zipFile,jarout,rzis,encode);
        jarout.finish();
        ByteArrayInputStream ret = new ByteArrayInputStream(retout.toByteArray());
        jarout.close();
        retout.close();
        in.close();
        bis.close();
        zipFile.close();
        return ret ;
    }

    public static void copyFile(JarInputStream in,JarOutputStream out)throws Exception{
        JarEntry jarEntry;
        while (( jarEntry = in.getNextJarEntry() ) != null) {
            if (jarEntry.isDirectory()) {
                String name = jarEntry.getName();
                try{
                    out.putNextEntry(new ZipEntry(name));
                    out.closeEntry();
                }catch (Exception e){}
                copyFile(in,out);
            }else if(isJar(jarEntry.getName())){
                try{
                    out.putNextEntry(new ZipEntry(jarEntry.getName()));
                    ByteArrayOutputStream fo = new ByteArrayOutputStream();
                    byte[] b = new byte[1024];
                    int len = 0;
                    while (( len = in.read( b ) ) > 0) {
                        fo.write( b, 0, len );
                    }
                    fo.close();
                    ByteArrayInputStream fi = new ByteArrayInputStream(fo.toByteArray());
                    len = 0;
                    while (( len = fi.read( b ) ) > 0) {
                        out.write( b, 0, len );
                    }
                    fi.close();
                    out.closeEntry();
                }catch (Exception ex){

                }
            }else{
                try{
                    /*System.out.println(jarEntry.getName());*/
                    out.putNextEntry(new ZipEntry(jarEntry.getName()));
                    byte[] b = new byte[1024];
                    int len = 0;
                    while (( len = in.read( b ) ) > 0) {
                        out.write( b, 0, len );
                    }
                }catch(Exception ex){

                }

                out.closeEntry();
            }
        }

    }

    public static File[] saveZipEntry2Temp(ZipFile zipFile, ZipEntry[] entries) throws Exception {

        try {
            List li = new ArrayList();
            if (null != entries) {
                for (ZipEntry zipEntry : entries) {
                    String fileName = zipEntry.getName();
                    fileName = fileName.replace('\\', '/');

                    File f = File.createTempFile(fileName.substring(fileName.lastIndexOf("/") + 1, fileName.length()), ".jar");
                    f.createNewFile();

                    InputStream in = zipFile.getInputStream(zipEntry);
                    FileOutputStream out = new FileOutputStream(f);
                    byte[] by = new byte[1024];
                    int c;
                    while ((c = in.read(by)) != -1) {
                        out.write(by, 0, c);
                    }
                    out.close();
                    in.close();
                    log.info("unzip jar"+zipFile.getName()+"�е�"+fileName+"��Ŀ¼:"+f.getPath());
                    li.add(f);
                }
                if (li.size() > 0) {
                    return (File[]) li.toArray(new File[0]);
                }
            }

        } catch (Exception ex) {
            log.error(ex);
        }
        return null;

    }

    /**
     * zip muti folders to a zip file
     * @param destZipFilePath
     * @param sroucepath
     */
    public static void zipFiles(String destZipFilePath,String[] sroucepath){
        File zipFile = new File(destZipFilePath);
        try {
            if(!zipFile.exists()){
                if(!zipFile.getParentFile().exists()){
                    zipFile.getParentFile().mkdirs();
                }
            }
            FileOutputStream fos = new FileOutputStream(zipFile);
            ZipOutputStream zos = new ZipOutputStream(fos);
            String baseDir = "";
            if(null != sroucepath) {
                for(String p:sroucepath) {
                    File src = new File(p);
                    if (src.isFile()) {
                        //src是文件，调用此方法
                        compressFile(src, zos, baseDir);

                    } else if (src.isDirectory()) {
                        //src是文件夹，调用此方法
                        compressDir(src, zos, baseDir);

                    }
                }
            }
            zos.close();

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

        }

    }

    /**s
     * 压缩文件
     * @param srcFilePath 压缩源路径
     * @param destFilePath 压缩目的路径
     */
    public static void compress(String srcFilePath, String destFilePath) {
        //
        File src = new File(srcFilePath);

        if (!src.exists()) {
            throw new RuntimeException(srcFilePath + "不存在");
        }
        File zipFile = new File(destFilePath);

        try {

            FileOutputStream fos = new FileOutputStream(zipFile);
            ZipOutputStream zos = new ZipOutputStream(fos);
            String baseDir = "";
            compressbyType(src, zos, baseDir);
            zos.close();

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();

        }
    }
    /**
     * 按照原路径的类型就行压缩。文件路径直接把文件压缩，
     * @param src
     * @param zos
     * @param baseDir
     */
    private static void compressbyType(File src, ZipOutputStream zos,String baseDir) {

        if (!src.exists())
            return;
        //System.out.println("压缩路径" + baseDir + src.getName());
        //判断文件是否是文件，如果是文件调用compressFile方法,如果是路径，则调用compressDir方法；
        if (src.isFile()) {
            //src是文件，调用此方法
            compressFile(src, zos, baseDir);

        } else if (src.isDirectory()) {
            //src是文件夹，调用此方法
            compressDir(src, zos, baseDir);

        }

    }

    /**
     * 压缩文件
     */
    private static void compressFile(File file, ZipOutputStream zos,String baseDir) {
        if (!file.exists())
            return;
        try {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            ZipEntry entry = new ZipEntry(baseDir + file.getName());
            zos.putNextEntry(entry);
            int count;
            byte[] buf = new byte[1024];
            while ((count = bis.read(buf)) != -1) {
                zos.write(buf, 0, count);
            }
            bis.close();

        } catch (Exception e) {
            // TODO: handle exception

        }
    }

    /**
     * 压缩文件夹
     */
    public static void compressDir(File dir, ZipOutputStream zos,String baseDir) {
        if (!dir.exists())
            return;
        File[] files = dir.listFiles();
        if(files.length == 0){
            try {
                zos.putNextEntry(new ZipEntry(baseDir + dir.getName()+File.separator));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (File file : files) {
            compressbyType(file, zos, baseDir + dir.getName() + File.separator);
        }
    }
    
    public static void main(String[] args){
    	try{
    		ReplaceZipItem[] rzis=  new ReplaceZipItem[4];
    		ReplaceZipItem r = new ReplaceZipItem();
    		r.setJars(null);
    		r.setFileName("config/system/service/defaults.xml");
    		r.addReplace(":dburl", "jdbc:oracle:thin:@(DESCRIPTION =(ADDRESS_LIST =(LOAD_BALANCE = yes)(FAILOVER = ON)(ADDRESS = (PROTOCOL = TCP)(HOST = 172.20.9.111)(PORT = 1521))(ADDRESS = (PROTOCOL = TCP)(HOST = 172.20.9.112)(PORT = 1521))    )        (CONNECT_DATA =(SERVER = DEDICATED)(SERVICE_NAME = crmttdc) ))");
    		rzis[0]=r;
    		
    		ReplaceZipItem r2 = new ReplaceZipItem();
    		r2.setJars(null);
    		r2.setFileName("instance1/config/aiint.xml");
    		r2.addReplace(":userpath", "/home/aiint/");
    		rzis[1]=r2;

    		ReplaceZipItem r3 = new ReplaceZipItem();
    		r3.setJars(new String[]{"aconfig.jar"});
    		r3.setFileName("system/service/defaults.xml");
    		r3.addReplace(":dburl", "jdbc:oracle:thin:@(DESCRIPTION =(ADDRESS_LIST =(LOAD_BALANCE = yes)(FAILOVER = ON)(ADDRESS = (PROTOCOL = TCP)(HOST = 172.20.9.111)(PORT = 1521))(ADDRESS = (PROTOCOL = TCP)(HOST = 172.20.9.112)(PORT = 1521))    )        (CONNECT_DATA =(SERVER = DEDICATED)(SERVICE_NAME = crmttdc) ))");
    		rzis[2]=r3;
    		
    		ReplaceZipItem r4 = new ReplaceZipItem();
    		r4.setJars(new String[]{"WEB-INF/lib/aconfig.jar"});
    		r4.setFileName("com/octopus/crm/cs/report/jasper/");
    		r4.addReplace("caseStatisticsBySQL.jasper", "d:/poc/caseStatisticsBySQL.jasper");
    		r4.addReplace("caseStatisticsBySQL.jrxml", "d:/poc/caseStatisticsBySQL.jrxml");
    		r4.addReplace("caseStatisticsBySQL_prod.jasper", "d:/poc/caseStatisticsBySQL_prod.jasper");
    		r4.addReplace("caseStatisticsBySQL_prod.jrxml", "d:/poc/caseStatisticsBySQL_prod.jrxml");
    		r4.addReplace("caseStatisticsBySQL_prod_region.jasper", "d:/poc/caseStatisticsBySQL_prod_region.jasper");
    		r4.addReplace("caseStatisticsBySQL_prod_region.jrxml", "d:/poc/caseStatisticsBySQL_prod_region.jrxml");
    		r4.addReplace("caseStatisticsBySQL_region.jasper", "d:/poc/caseStatisticsBySQL_region.jasper");
    		r4.addReplace("caseStatisticsBySQL_region.jrxml", "d:/poc/caseStatisticsBySQL_region.jrxml");
    		rzis[3]=r4;

/*    		ReplaceZipItem r5 = new ReplaceZipItem();
    		r5.setJars(new String[]{"aconfig.jar"});
    		r5.setFileName("com/octopus/crm/cs/report/jasper/");
    		r5.addReplace("caseStatisticsBySQL.jasper", "d:/poc/caseStatisticsBySQL.jasper");
    		r5.addReplace("caseStatisticsBySQL.jrxml", "d:/poc/caseStatisticsBySQL.jrxml");
    		r5.addReplace("caseStatisticsBySQL_prod.jasper", "d:/poc/caseStatisticsBySQL_prod.jasper");
    		r5.addReplace("caseStatisticsBySQL_prod.jrxml", "d:/poc/caseStatisticsBySQL_prod.jrxml");
    		r5.addReplace("caseStatisticsBySQL_prod_region.jasper", "d:/poc/caseStatisticsBySQL_prod_region.jasper");
    		r5.addReplace("caseStatisticsBySQL_prod_region.jrxml", "d:/poc/caseStatisticsBySQL_prod_region.jrxml");
    		r5.addReplace("caseStatisticsBySQL_region.jasper", "d:/poc/caseStatisticsBySQL_region.jasper");
    		r5.addReplace("caseStatisticsBySQL_region.jrxml", "d:/poc/caseStatisticsBySQL_region.jrxml");
    		rzis[4]=r5;
*/    		
    		InputStream in = ZipUtil.changeFile(new FileInputStream(new File("d:\\intframe.zip")),rzis,"utf-8");
    		FileOutputStream out = new FileOutputStream(new File("d:\\intframe_chg.jar"));
    		byte[] by = new byte[1024];
            int c;
            while ((c = in.read(by)) != -1) {
                out.write(by, 0, c);
            }
            in.close();
            out.close();
    	}catch(Exception e){
    		e.printStackTrace();
    	}
    }
}