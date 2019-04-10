package com.octopus.utils.file.impl.excel;

import com.octopus.utils.alone.ObjectUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * User: Administrator
 * Date: 15-1-13
 * Time: 下午2:05
 */
public class ExcelReader {
    protected XSSFWorkbook book =null;

    public ExcelReader(InputStream excel) throws IOException, InvalidFormatException {
        read(excel);
    }

    public Object read(InputStream in) throws IOException, InvalidFormatException {
        book = new XSSFWorkbook(in);
        return book;
    }
    public static String chg(String s){
        if(null != s){
            StringBuffer sb = new StringBuffer();
            String[] ts = s.split(" ");
            for(String t: ts){
                if(null != t && !" ".equals(t) && !"".equals(t)){
                    sb.append((t.charAt(0)+"").toUpperCase()+t.substring(1));
                }
            }
            return sb.toString();
        }
        return "";
    }

    public static List<Map<String,String>> read(XSSFSheet sheet){
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
                        map.put(chg(getStringValue(titls.getCell(i))),v);
                }

                ret.add(map);
            }
        }
        return ret;

    }
    public static String getStringValue(XSSFCell cell){
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
            return cell.getStringCellValue();
        }
        return null;

    }

    public List getSheetData(Class c,String sheetName) throws Exception {
        List rs = read(book.getSheet(sheetName));
        List ds = ObjectUtils.generatorObjectByMapData(c,rs);
        return  ds;
    }
    public List<Map<String,String>> getSheepData(String sheetName)throws Exception{
        List rs = read(book.getSheet(sheetName));
        return rs;
    }
}
