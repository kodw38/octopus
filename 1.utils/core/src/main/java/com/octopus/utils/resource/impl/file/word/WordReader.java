package com.octopus.utils.resource.impl.file.word;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLUtil;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/*
import org.apache.poi.POIXMLDocument;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.hwpf.model.PicturesTable;
import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.Picture;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
*/

/**
 * User: wfgao_000
 * Date: 16-4-28
 * Time: 下午2:02
 */
public class WordReader {
    String picLink;
    String startTableTag,startFirstRowTag,startFirstRowCellTag,endFirstRowCellTag,endFirstRowTag,startSecondRowTag,startSeondRowCellTag,endSecondRowCellTag,endSecondRowTag,endTableTag;
    String enter;
    public Object readText(String file) throws Exception {
        try {
/*
            if(file.endsWith("doc")){
                WordExtractor w =  new WordExtractor(new FileInputStream(file));
                return w.getText();
            }else if(file.endsWith("docx"))             {
                OPCPackage opcPackage = POIXMLDocument.openPackage(file);
                XWPFWordExtractor w = new XWPFWordExtractor(opcPackage);
                return w.getText();
            }else
*/
                throw new Exception("not support word file "+file );
        } catch (Exception e) {
            throw e;
        }
    }
    public String read(String file,String imgStoreDir)throws Exception{
        try{
/*
            int BUFFER=2048;
            if(file.endsWith("doc")){
            FileInputStream in = new FileInputStream(file);
            HWPFDocument word = new HWPFDocument(in);
            int numCharacterRuns = word.getRange().numCharacterRuns();
            int underlinecode=0;
            int imgcount=0;
            String text="";
            OutputStream out = null;
            int startOffset=0,endOffset;
            int enterCount=0;
            PicturesTable pics = word.getPicturesTable();
            for(int i=0;i<numCharacterRuns;i++){
                CharacterRun characterRun = word.getRange().getCharacterRun(i);
                startOffset = characterRun.getStartOffset();
                underlinecode = characterRun.getUnderlineCode();
                endOffset = characterRun.getEndOffset();
                for(int m=startOffset;m<endOffset;m++){
                    Range range = new Range(m,m+1,word);
                    int hashCode = range.text().hashCode();
                    CharacterRun cr = range.getCharacterRun(0);
                    underlinecode = cr.getUnderlineCode();
                    if(underlinecode !=0 && range.text() !=null){
                        text = text +"_";
                    }
                    if(m<endOffset && (hashCode == 13 || hashCode == 7)){
                        enterCount++;
                    }
                    text=text + range.text();
                }
                if(pics.hasPicture(characterRun)){
                    Picture pic = pics.extractPicture(characterRun,true);
                    String fileName = pic.suggestFullFileName();
                    byte[] content = pic.getContent();
                    out = new FileOutputStream(new File(imgStoreDir + File.separator+fileName));
                    out.write(content);
                    out.flush();
                    out.close();
                    text += "<image src='"+fileName+"'>";
                    imgcount++;
                }

            }

             in.close();
            return text;
            }else if(file.endsWith("docx")){
            }
*/
            return null;
        }catch (Exception e){
            throw e;
        }
    }

    public void setPictureLinkTemplate(String link){
        picLink = link;
    }
    public void setEnterChar(String enter){
        this.enter = enter;
    }
    public void setTableTemplate(String startTableTag,String startFirstRowTag,String startFirstRowCellTag,String endFirstRowCellTag,String endFirstRowTag,String startSecondRowTag,String startSeondRowCellTag,String endSecondRowCellTag,String endSecondRowTag,String endTableTag){
        this.startTableTag=startTableTag;
        this.startFirstRowTag=startFirstRowTag;
        this.startFirstRowCellTag=startFirstRowCellTag;
        this.endFirstRowCellTag=endFirstRowCellTag;
        this.endFirstRowTag=endFirstRowTag;
        this.startSecondRowTag=startSecondRowTag;
        this.startSeondRowCellTag=startSeondRowCellTag;
        this.endSecondRowCellTag=endSecondRowCellTag;
        this.endSecondRowTag=endSecondRowTag;
        this.endTableTag=endTableTag;
    }

    public DocInfo readDocx(String file)throws Exception{
        DocInfo info = new DocInfo();
        info.docFile=file;

        ZipFile zipFile = new ZipFile(file);
        Enumeration enu = zipFile.entries();
        ZipEntry relation=null,body=null,number=null;

        while(enu.hasMoreElements()){
            ZipEntry zipEntry = (ZipEntry)enu.nextElement();
            if(zipEntry.isDirectory()){
/*
                new File(imgStoreDir+File.separator+zipEntry.getName()).mkdirs();
*/
                continue;
            }
            String name = zipEntry.getName();
            name = name.replaceAll("\\\\","/");
            if(name.contains("/")){
                name = name.substring(name.lastIndexOf("/")+1);
            }
            //get pictures
            if(name.endsWith("wmf") || name.endsWith("png") || name.endsWith("jpg")){
                BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(zipEntry));
    /*
                File f = new File(imgStoreDir+File.separator+zipEntry.getName());
                File parent = f.getParentFile();
                if(parent != null && !parent.exists()){
                    parent.mkdirs() ;
                }
                FileOutputStream fos = new FileOutputStream(f);

                BufferedOutputStream bos = new BufferedOutputStream(fos,BUFFER);
    */
                int count=bis.available();
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] array = new byte[count];
                while((count = bis.read(array,0,count))!=-1){
                    bos.write(array,0,count);
                }
                bos.close();
                bis.close();
                info.pics.put(name,bos);
                //System.out.println("pic:"+name);
            }

            //get pic relation
            if(name.equals("document.xml.rels")){
                relation=zipEntry;
            }
            if(name.equals("numbering.xml")){
                number=zipEntry;
            }
            if(name.equals("document.xml")){
                body=zipEntry;
            }

        }
        //relation
        if(null != relation){
            BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(relation));
            XMLMakeup makeup = XMLUtil.getDataFromStream(bis);
            XMLMakeup[] ms = makeup.getChild("Relationship");
            if(null != ms){
                for(XMLMakeup m:ms){
                    String id = m.getProperties().getProperty("Id");
                    String value = m.getProperties().getProperty("Target");
                    if(value.contains("/")){
                        value = value.substring(value.indexOf("/")+1);
                    }
                    if(StringUtils.isNotBlank(id) && StringUtils.isNotBlank(value)) {
                        info.relation.put(id,value);
                        //System.out.println("rel:"+id+" "+value);
                    }
                }
            }
        }
        if(null != number){
            BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(number));
            XMLMakeup makeup = XMLUtil.getDataFromStream(bis);
            XMLMakeup[] ms = makeup.getChild("w:num");
            if(null != ms){
                HashMap<Integer,Integer> idMap = new HashMap();
                for(XMLMakeup m:ms){
                    String numid = m.getProperties().getProperty("w:numId");
                    XMLMakeup[] cs = m.getChild("w:abstractNumId");
                    if(null !=cs && cs.length>0){
                        String absNumId = cs[0].getProperties().getProperty("w:val");
                        if(StringUtils.isNotBlank(numid) && StringUtils.isNotBlank(absNumId)){
                            idMap.put(Integer.valueOf(numid),Integer.valueOf(absNumId));
                        }
                    }
                }
                info.numIdMap=idMap;
            }
            XMLMakeup[] ams = makeup.getChild("w:abstractNum");
            if(null != ams){
                HashMap<Integer,XMLMakeup> aidMap = new HashMap();
                for(XMLMakeup m:ams){
                    String numid = m.getProperties().getProperty("w:abstractNumId");
                    if(StringUtils.isNotBlank(numid)){
                        aidMap.put(Integer.parseInt(numid),m);
                    }
                }
                info.absNumIdMap=aidMap;
            }
        }
        if(null != body){
            BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(body));
            parseText(bis,info);
        }
        return info;
    }

    public class DocInfo{
        String docFile;
        Map<String,OutputStream> pics = new HashMap<String,OutputStream>();
        Map<String,String> relation = new HashMap<String,String>();
        HashMap<Integer,Integer> numIdMap;
        HashMap<Integer,XMLMakeup> absNumIdMap;
        StringBuffer text;
        List<DocLine>  lines = new LinkedList<DocLine>();
        public  Map<String,OutputStream> getPictures(){
            return pics;
        }
        public StringBuffer getText(){
             return text;
        }
        public List<DocLine> getLines(){
            return lines;
        }
    }
    public class DocLine{
        List<DocField> fields = new LinkedList<DocField>();
        DocTable table;
        String styleCode;
        int numId=-1;
        int ilvl=-1;
        public List<DocField> getFields(){
            return fields;
        }
        public DocTable getTable(){
            return table;
        }
        public void addField(DocField field){
            if(null == table)
                fields.add(field);
            else{
                table.addCellField(field);
            }
        }
    }
    public class DocTable{
        int rowCount;
        int columnCount;
        boolean isEnd;
        List<List<DocLine>> rows = new LinkedList<List<DocLine>>();
        StringBuffer sb = new StringBuffer();
        public List<List<DocLine>> getRows(){
            return rows;
        }
        public int getRowCount(){
            return rowCount;
        }
        public  int getColumnCount(){
            return columnCount;
        }
        void addCellField(DocField field){

            rows.get(rows.size()-1).get(rows.get(rows.size()-1).size()-1).addField(field);

            if(field.getType().equals("P")){
                if(null != picLink)
                    sb.append(picLink.replace(":PicName",field.getText()));
                else
                    sb.append(" ");
            }else if(field.getType().equals("T")){
                 sb.append(field.getText());
            }

        }
        void appendStartTable(){
            if(StringUtils.isNotBlank(startTableTag))
                sb.append(startTableTag);
        }
        boolean setfinished(){
            if(rows.size()==rowCount && rows.get(rows.size()-1).size()==columnCount) {
                isEnd=true;
                if(StringUtils.isNotBlank(endSecondRowCellTag))
                    sb.append(endSecondRowCellTag) ;
                if(StringUtils.isNotBlank(endSecondRowTag))
                    sb.append(endSecondRowTag);
                if(StringUtils.isNotBlank(endTableTag))
                    sb.append(endTableTag);
                return true;
            }
            return false;
        }
        void newCell(){
            if(rows.size()==1 && StringUtils.isNotBlank(endFirstRowCellTag)){
                sb.append(endFirstRowCellTag);
            }else if(rows.size()>1 && StringUtils.isNotBlank(endSecondRowCellTag)){
                sb.append(endSecondRowCellTag);
            }

            if(rows.size()==0||rows.get(rows.size()-1).size()==columnCount){
                if(rows.size()==0){
                    if(StringUtils.isNotBlank(startFirstRowTag))
                        sb.append(startFirstRowTag);
                }
                if(rows.size()>0){
                    if(StringUtils.isNotBlank(endFirstRowTag))
                        sb.append(endFirstRowTag);
                    if(StringUtils.isNotBlank(startSecondRowTag))
                        sb.append(startSecondRowTag);
                    else {
                        if(StringUtils.isNotBlank(enter))
                            sb.append(enter);
                        else
                            sb.append("\n");
                    }
                }
                rows.add(new LinkedList());
            }

            if(rows.size()==1 && StringUtils.isNotBlank(startFirstRowCellTag))
                sb.append(startFirstRowCellTag);
            else if(rows.size()>1 && StringUtils.isNotBlank(startSeondRowCellTag)){
                sb.append(startSeondRowCellTag);
            }else
                sb.append(" ");

            rows.get(rows.size()-1).add(new DocLine());
        }
        public String toString(){
            return sb.toString();
        }

    }
    public class DocField{
        String type="";//T:text ,P:pic
        String text;
        public String getType() {
            return type;
        }
        public String getText() {
            return text;
        }
    }

    String getCharNum(int[] v,String format,String template,int ilv)throws Exception{
        for(int i=0;i<ilv+1;i++){
            String c = getChar(v[i],format);
            template = template.replace(String.valueOf((i+1)),c);
        }
        return StringUtils.replace(template,"%","");
    }
    String[]  japaneseCounting={"一","二","三","四","五","六","七","八","九"};
    String getChar(int v,String format)throws Exception{
        if(format.equals("decimal")){
            return String.valueOf(v);
        }else if(format.equals("japaneseCounting")){
             return japaneseCounting[v-1];
        }else if(format.equals("lowerLetter")){
            return "";
        }else if(format.equals("lowerRoman")){
            return "" ;
        }else if(format.equals("bullet")){
            return "";
        }
        throw new Exception("not support the num format "+format);
    }

    /**
     * @return
     */
    Map<String,int[]> curMaxNum = new HashMap();
    String getNum(DocInfo info,DocLine line)throws Exception{
        if(line.numId>=0){
            Integer absNumId = info.numIdMap.get(line.numId);
            if(null != absNumId && absNumId>=0){
                 XMLMakeup x = info.absNumIdMap.get(absNumId);
                if(null != x && line.ilvl>=0){
                    XMLMakeup[] ms = x.getByTagProperty("w:lvl","w:ilvl",String.valueOf(line.ilvl));
                    if(null != ms && ms.length>0){
                        String start = ms[0].getFirstCurChildKeyValue("w:start","w:val");
                        String format = ms[0].getFirstCurChildKeyValue("w:numFmt","w:val");
                        String text = ms[0].getFirstCurChildKeyValue("w:lvlText","w:val");
                        int[] cur = curMaxNum.get(line.numId+"-"+line.ilvl);
                        if(null !=cur){
                            //have save level num
                            cur[cur.length-1]=cur[cur.length-1]+1;
                            return getCharNum(cur,format,text,line.ilvl);
                        }else if(line.ilvl>0){
                           //mutil level
                            int[] parent = curMaxNum.get(line.numId+"-"+(line.ilvl-1));
                            int[] c = Arrays.copyOf(parent,parent.length+1);
                            c[c.length-1]=Integer.valueOf(start);
                            curMaxNum.put(line.numId+"-"+line.ilvl,c);
                            return getCharNum(c,format,text,line.ilvl);
                        }else{
                            //root level
                            int [] c = new int[]{Integer.valueOf(start)};
                            curMaxNum.put(line.numId+"-"+line.ilvl,c);
                            return getCharNum(c,format,text,line.ilvl);
                        }
                    }
                }
            }
        }
        return "";
    }

    /**
     编号，同一编号一下值一样
     <w:pStyle w:val="a5"/>
     <w:numPr>
     <w:ilvl w:val="0"/>
     <w:numId w:val="1"/>
     </w:numPr>

     标题 同一层值一样，
     <w:pPr>
     <w:pStyle w:val="4"/>
     </w:pPr>
     * @param in
     * @param info
     * @throws Exception
     */
    void parseText(InputStream in,DocInfo info)throws Exception{
        XMLMakeup root = XMLUtil.getDataFromStream(in);
        XMLMakeup[] bodys = root.getChild("w:body");

        if(null != bodys){
            for(XMLMakeup m:bodys){
                StringBuffer sb = new StringBuffer();
                XMLMakeup[] ps = m.finds(new String[]{"w:p", "w:br", "w:t", "v:imagedata", "a:blip","w:tbl"});
                if(null != ps && ps.length>0){
                    DocLine curLine=null;
                    for(XMLMakeup p:ps){
                        if(p.getName().equals("w:p") && (null != curLine && curLine.getTable()!=null) ){
                            if(curLine.getTable().setfinished()){
                                sb.append(curLine.getTable().toString());
                                curLine=null;
                            }else{
                                curLine.getTable().newCell();
                            }
                        }
                        if(p.getName().equals("w:p") && (curLine==null || (null != curLine && curLine.getTable()==null)) ){
                            if(StringUtils.isNotBlank(enter))
                                sb.append(enter);
                            else
                                sb.append("\n");
                            DocLine line = new DocLine();

                            XMLMakeup[] pr = p.getChild("w:pPr");
                            if(null != pr && pr.length>0){
                                XMLMakeup[] pstyle = pr[0].getChild("w:pStyle");
                                if(null != pstyle && pstyle.length>0){
                                    String s = pstyle[0].getProperties().getProperty("w:val");
                                    if(StringUtils.isNotBlank(s))
                                        line.styleCode=s;
                                }
                                XMLMakeup[] num = pr[0].getChild("w:numPr");
                                if(null != num && num.length>0){
                                    XMLMakeup[] ns = num[0].getChild("w:numId");
                                    if(null != ns && ns.length>0){
                                        String s = ns[0].getProperties().getProperty("w:val");
                                        if(StringUtils.isNotBlank(s)) {
                                            line.numId=Integer.valueOf(s);
                                        }

                                    }
                                    XMLMakeup[] ls = num[0].getChild("w:ilvl");
                                    if(null != ls && ls.length>0){
                                        String s = ls[0].getProperties().getProperty("w:val");
                                        if(StringUtils.isNotBlank(s)) {
                                            line.ilvl=Integer.valueOf(s);
                                        }

                                    }
                                    sb.append(getNum(info,line));
                                }
                            }

                            info.getLines().add(line);
                            curLine=line;
                            continue;
                        }
                        if(p.getName().equals("w:br") && (curLine==null || (null != curLine && curLine.getTable()==null))){
                            if(StringUtils.isNotBlank(enter))
                                sb.append(enter);
                            else
                                sb.append("\n");
                            DocLine line = new DocLine();
                            info.getLines().add(line);
                            curLine=line;
                            continue;
                        }
                        if(p.getName().equals("w:tbl")){
                            DocTable t = new DocTable();
                            t.columnCount=p.find("w:tblGrid")[0].getChild("w:gridCol").length;
                            t.rowCount=p.find("w:tr").length;
                            if(StringUtils.isNotBlank(enter))
                                sb.append(enter);
                            else
                                sb.append("\n");
                            DocLine line = new DocLine();
                            line.table=t;
                            line.table.appendStartTable();
                            info.getLines().add(line);

                            curLine=line;
                            continue;
                        }

                        if(p.getName().equals("w:t")){
                            if(null != curLine){
                                if(curLine.getTable()==null)
                                    sb.append(p.getText());

                                DocField f = new DocField();
                                f.type="T";
                                f.text=p.getText();
                                curLine.addField(f);
                            }
                        }
                        if(p.getName().equals("v:imagedata")){
                            String picid = p.getProperties().getProperty("r:id");
                            if(StringUtils.isNotBlank(picid)){
                                if(info.relation.containsKey(picid) && info.pics.containsKey(info.relation.get(picid))){
                                    if(null != curLine){
                                        if(null == curLine.getTable()){
                                            if(StringUtils.isNotBlank(picLink)){
                                                sb.append(picLink.replace(":PicName",info.relation.get(picid)));
                                            }else {
                                                sb.append(" ");
                                            }
                                        }
                                        DocField f = new DocField();
                                        f.type="P";
                                        f.text=info.relation.get(picid);
                                        curLine.addField(f);
                                    }
                                }
                            }
                        }
                        if(p.getName().equals("a:blip")){
                            String picid = p.getProperties().getProperty("r:embed");
                            if(StringUtils.isNotBlank(picid)){
                                if(info.relation.containsKey(picid) && info.pics.containsKey(info.relation.get(picid))){
                                    if(null != curLine){
                                        if(null == curLine.getTable()){
                                            if(StringUtils.isNotBlank(picLink)){
                                                sb.append(picLink.replace(":PicName",info.relation.get(picid)));
                                            }else
                                            sb.append(" ");
                                        }
                                        DocField f = new DocField();
                                        f.type="P";
                                        f.text=info.relation.get(picid);
                                        curLine.addField(f);
                                    }
                                }
                            }
                        }

                        /*if(p.getName().equals("w:br")){
                            System.out.println("\n");
                            continue;
                        }
                        StringBuffer sb = new StringBuffer();
                        XMLMakeup[] ts = p.find("w:t");
                        if(null != ts){
                            for(XMLMakeup t:ts){
                                sb.append(t.getText());
                            }
                        }
                        System.out.println(sb.toString());*/
                    }
                    info.text = sb;
                    //System.out.println(sb.toString());
                }
            }
        }
    }

    public static void main(String[] args){
        try{
            WordReader w = new WordReader();
            //设置图片模板 :PicName为word文档中的图片名称
            //w.setPictureLinkTemplate("<a href=\"download?picname=:PicName\"/>");
            //设置表格的模板，要有表格,第一行,第一列，第二行,第一列的样式。
            //w.setEnterChar("<br>");
            //
            //w.setTableTemplate("<table>","<tr>","<td>","</td>","</tr>","<tr>","<td>","</td>","</tr>","</table>");

            String file = "D:\\Work\\other\\webreader\\test\\高中理科试题模板\\高中物理试题模板_1.docx";
            String file2 = "D:\\Work\\other\\webreader\\test\\高中理科试题模板\\高中生物试题模板.docx";
            String file3 = "D:\\Work\\other\\webreader\\test\\高中理科试题模板\\高中数学试题模板.docx";
            String file4 = "D:\\Work\\other\\webreader\\test\\高中理科试题模板\\菁优网试题理科公式.docx";
            String file5 = "C:\\Users\\wfgao_000\\Documents\\Tencent Files\\48582164\\FileRecv\\（已做好）1.2 第一章第二节 太阳对地球的影响.docx";
            System.out.println("-------"+file+"----------");
            //读取Word文件
            DocInfo doc = w.readDocx(file5);
            //获取Word文本内容
            System.out.println(doc.getText());

/*
            System.out.println("-------"+file2+"----------");
            DocInfo doc2 = w.readDocx(file2);
            System.out.println(doc2.getText());

            System.out.println("-------"+file3+"----------");
            DocInfo doc3 = w.readDocx(file3);
            System.out.println(doc3.getText());

            System.out.println("-------"+file4+"----------");
            DocInfo doc4 = w.readDocx(file4);
            System.out.println(doc4.getText());*/




            //System.out.println(r);
        }catch (Exception e){
            e.printStackTrace();
        }
    }


}
