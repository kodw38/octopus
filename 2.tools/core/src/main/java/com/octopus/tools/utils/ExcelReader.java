package com.octopus.tools.utils;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * User: wfgao_000
 * Date: 16-1-31
 * Time: 下午8:19
 */
public class ExcelReader extends XMLDoObject{
    static transient Log log = LogFactory.getLog(ExcelReader.class);
    public ExcelReader(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }


    protected XSSFWorkbook getBook(String file) throws IOException {
        try{
        FileInputStream in = new FileInputStream(new File(file));
        XSSFWorkbook book =  new XSSFWorkbook(in);
        in.close();
        return book;
        }catch (Exception e){
            log.error(e);
        }
        return null;
    }

    public List<Map<String,String>> read(String file,String sheetName) throws IOException {
        return read(getBook(file).getSheet(sheetName));
    }
    int getCol(XSSFSheet s,String cn){
        XSSFRow titls = s.getRow(0);
        for(int i=0;i<titls.getLastCellNum();i++){
            if(cn.trim().equals(getStringValue(titls.getCell(i)))){
                return i;
            }
        }
        log.error("not get column index by value["+cn+"]");
        return -1;
    }
    int getRow(XSSFSheet s,String cn,String value){
        int coli = getCol(s,cn);
        if(coli>=0){
        for(int r=1;r<=s.getLastRowNum();r++){
            if(getStringValue(s.getRow(r).getCell(coli)).equals(value))
                return r;
        }
        }
        log.error("not get row index by column["+cn+"] value["+value+"]");
        return -1;
    }
    public List<Map<String,String>> read(XSSFSheet sheet){
        if(null ==sheet)return null;
        List ret = new ArrayList();
        XSSFRow titls = sheet.getRow(0);
        for(int r=1;r<=sheet.getLastRowNum();r++){
            XSSFRow rd = sheet.getRow(r);
            if(null != rd){
                Map map = new LinkedHashMap();
                for(int i=0;i<rd.getLastCellNum();i++){
                    XSSFCell cell = rd.getCell(i);
                    String v = getStringValue(cell);
                    if(null != v && !"".equals(v))
                        map.put(StringUtils.getNameFilterSpace(getStringValue(titls.getCell(i))),v);
                }

                ret.add(map);
            }
        }
        return ret;
    }

    public String getStringValue(XSSFCell cell){
        if(null == cell)return null;
        if(cell.getCellType()==XSSFCell.CELL_TYPE_BLANK){
            return null;
        }
        if(cell.getCellType()==XSSFCell.CELL_TYPE_BOOLEAN){
            return String.valueOf(cell.getBooleanCellValue());
        }
        if(cell.getCellType()==XSSFCell.CELL_TYPE_ERROR){
            return cell.getErrorCellString();
        }
        if(cell.getCellType()==XSSFCell.CELL_TYPE_FORMULA){
            return cell.getCellFormula();
        }
        if(cell.getCellType()==XSSFCell.CELL_TYPE_NUMERIC){
            return String.valueOf((long)cell.getNumericCellValue());
        }
        if(cell.getCellType()==XSSFCell.CELL_TYPE_STRING){
            return cell.getStringCellValue().trim();
        }
        return null;

    }
    void setCellValue(XSSFSheet s,int col,int row,String value){
        if(col>=0 && row>=0) {
            if(null != s.getRow(row)) {
                if (null == s.getRow(row).getCell(col)) {
                    s.getRow(row).createCell(col);
                }
                s.getRow(row).getCell(col).setCellValue(value);
            }
        }
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(null != input){
            String f = (String)input.get("file");
            String sheet = (String)input.get("sheetName");
            String op = (String)input.get("op");
            Object col = input.get("col");
            Object row = input.get("row");
            String value = null;
            if(null != input.get("value") && input.get("value") instanceof String) value=(String)input.get("value");
            if(null !=input.get("file") && null !=input.get("sheetName") && StringUtils.isBlank(op) && col==null && row==null && StringUtils.isBlank(value)){
                return read(f,sheet);
            }else if(StringUtils.isNotBlank(f) && StringUtils.isNotBlank(sheet) && StringUtils.isNotBlank(op)){
                if(op.equals("set") && null!=col && null !=row && StringUtils.isNotBlank(value)){
                    XSSFWorkbook bo = getBook(f);
                    XSSFSheet s =bo.getSheet(sheet);
                    if(row instanceof Map){
                        Iterator its = ((Map)row).keySet().iterator();
                        while(its.hasNext()){
                            String m = (String)its.next();
                            String v = (String)((Map) row).get(m);
                            setCellValue(s,getCol(s,(String)col),getRow(s,m,v),value);
                        }
                    }
                    FileOutputStream out = new FileOutputStream(new File(f));
                    bo.write(out);
                    out.close();

                }
            }
        }
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ResultCheck checkReturn(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return new ResultCheck(true,ret);  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config,Object ret,Exception e) throws Exception {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
