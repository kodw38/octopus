package com.octopus.utils.file.impl.excel;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.ds.Condition;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;

/**
 * User: wfgao_000
 * Date: 16-6-29
 * Time: 下午6:47
 */
public class ExcelWriter {
    protected XSSFWorkbook book =null;
    String file =null ;

    public ExcelWriter(String file) throws Exception {
        book = new XSSFWorkbook(new FileInputStream(file));
        this.file = file;
    }

    public Writer getSheet(String name){
        XSSFSheet sheet = book.getSheet(name);
        if(null != sheet) {
            return new Writer(sheet);
        }else{
            return null;
        }

    }

    public void save() throws Exception {
        book.write(new FileOutputStream(file));
    }
    public class Writer{
        XSSFSheet sheet;
        XSSFRow titls;
        //Map<String,Integer> titleIndex = new HashMap();
        HashMap<String,Integer> cels = new HashMap<String, Integer>();

        public Writer(XSSFSheet sheet){
            this.sheet=sheet;
            titls = sheet.getRow(0);
            for(int i=0;i<titls.getLastCellNum();i++){
                if(null != getStringValue(titls.getCell(i)))
                    cels.put(getStringValue(titls.getCell(i)),i);
            }
            //titleIndex = getTitleIndexMap();

       }
        /*Map getTitleIndexMap(){
            XSSFRow titls = sheet.getRow(0);
            Map map = new HashMap();
            for(int i=0;i<titls.getLastCellNum();i++){
                XSSFCell cell = titls.getCell(i);
                String v = getStringValue(cell);
                if(null != v && !"".equals(v))
                    map.put(chg(getStringValue(titls.getCell(i))),i);
            }
            return map;
        }*/
        public String chg(String s){
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
        public boolean remove(List<Condition> conds){
            if(null != conds) {
                List<Integer> ret = new ArrayList();
                int rows = sheet.getLastRowNum();
                for (int i=1;i<=rows;i++) {
                    Row row = sheet.getRow(i);
                    if(row==null)continue;
                    if(conds.size()>0) {
                        int n = conds.size();
                        for (Condition c:conds) {
                            Integer inx = getCellId(c.getFieldName());
                            if (null != inx && inx >= 0) {
                                if (c.getOp().equals(Condition.OP_EQUAL)) {
                                    if (c.getValues().equals(getStringValue((XSSFCell)row.getCell(inx)))) {
                                        n--;
                                        /**modify by ligs 由break改为continue，支持多条件判断**/
                                        continue;
                                    }
                                }
                                if (c.getOp().equals(Condition.OP_LIKE)) {
                                    if (c.getValues().toString().contains(getStringValue((XSSFCell)row.getCell(inx)))) {
                                        n--;
                                        /**modify by ligs 由break改为continue，支持多条件判断**/
                                        continue;
                                    }
                                }
                            }

                        }
                        if(n==0){
                            ret.add(i);
                        }

                    }
                }
                for (int i=ret.size()-1;i>=0;i--) {
                    removeRow(ret.get(i));
                }
                return true;
            }else{
                return false;
            }
        }
        public void removeRow(int rowIndex) {
            int lastRowNum=sheet.getLastRowNum();
            if(rowIndex>=0&&rowIndex<lastRowNum){
                sheet.shiftRows(rowIndex+1,lastRowNum, -1);
                Row row=sheet.getRow(lastRowNum);
                if(null != row) {
                    sheet.removeRow(row);
                }
            }
            if(rowIndex==lastRowNum){
                Row removingRow=sheet.getRow(rowIndex);
                if(removingRow!=null){
                    sheet.removeRow(removingRow);
                }
            }
        }
        public boolean update(List<Condition> conds,Map<String,Object> data){
            if(null != conds) {
                int rows = sheet.getLastRowNum();
                for (int i=1;i<=rows;i++) {
                    Row row = sheet.getRow(i);
                    int n = conds.size();
                    for(Condition c:conds){
                        Integer inx =getCellId(c.getFieldName());
                        if(null != inx && inx>=0) {
                            if (c.getOp().equals(Condition.OP_EQUAL)) {
                                if (c.getValues().equals(getStringValue((XSSFCell) row.getCell(inx)))) {
                                    n--;
                                    break;
                                }
                            }
                            if (c.getOp().equals(Condition.OP_LIKE)) {
                                if (c.getValues().toString().contains(getStringValue((XSSFCell) row.getCell(inx)))) {
                                    n--;
                                    break;
                                }
                            }
                        }
                    }
                    if(n==0 && null != data){
                        Iterator<String> its = data.keySet().iterator();
                        while(its.hasNext()) {
                            String k = its.next();
                            Integer x = getCellId(k);
                            if(null != x && x>=0) {
                                row.getCell(x).setCellValue((String)data.get(k));
                            }
                        }
                    }
                }

                return true;
            }else{
                return false;
            }
        }
        public void append(List<Map<String,String>> datas){
            int row = sheet.getLastRowNum()+1;
            for(Map m :datas){
                Iterator<String> its = m.keySet().iterator();
                /**modify by ligs createRow调用提前，解决每行记录只保存最后一个元素**/
                XSSFRow newRow = sheet.createRow(row);
                while(its.hasNext()){
                    String field = its.next();
                    //cellId允许为0，解决第0个元素不保存
                    if(getCellId(field)>=0 && StringUtils.isNotBlank(m.get(field)))
                    	newRow.createCell(getCellId(field)).setCellValue((String)m.get(field));
                }
                row++;
            }
        }
        public void append(Map<String,String> m){
            int row = sheet.getLastRowNum()+1;
            Iterator<String> its = m.keySet().iterator();
            XSSFRow newRow = sheet.createRow(row);
            while(its.hasNext()){
                String field = its.next();
                if(getCellId(field)>=0 && StringUtils.isNotBlank(m.get(field)))
                    newRow.createCell(getCellId(field)).setCellValue((String)m.get(field));
            }

        }
        public void setValue(int row,String field,String value){
            if(null != sheet.getRow(row) && null !=sheet.getRow(row).getCell(getCellId(field)))
                sheet.getRow(row).getCell(getCellId(field)).setCellValue(value);
        }
        int getCellId(String name){
            if(null != name){
                Integer n= cels.get(name);
                if(n ==null)
                    return -1;
                return n;
            }
            return -1;
        }
        String getStringValue(XSSFCell cell){
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
    }
}
