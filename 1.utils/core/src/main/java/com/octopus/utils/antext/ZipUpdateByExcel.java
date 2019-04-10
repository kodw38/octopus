package com.octopus.utils.antext;

import com.octopus.utils.file.FileUtils;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Created by kod on 2017/6/17.
 */
public class ZipUpdateByExcel extends Task {
    String zipfile;
    String chgexcelfilename;
    String sheetname;
    String fieldnameforchangeid;
    String changeidvalues;
    String filednameforfilenameinzip;
    String fieldnameforoldvalue;
    String fieldnamefornewvalue;
    String encode;


    public void setZipfile(String zipfile) {
        this.zipfile = zipfile;
    }

    public void setChgexcelfilename(String chgexcelfilename) {
        this.chgexcelfilename = chgexcelfilename;
    }

    public void setSheetname(String sheetname) {
        this.sheetname = sheetname;
    }

    public void setFieldnameforchangeid(String fieldnameforchangeid) {
        this.fieldnameforchangeid = fieldnameforchangeid;
    }

    public void setChangeidvalues(String changeidvalues) {
        this.changeidvalues = changeidvalues;
    }

    public void setFilednameforfilenameinzip(String filednameforfilenameinzip) {
        this.filednameforfilenameinzip = filednameforfilenameinzip;
    }

    public void setFieldnameforoldvalue(String fieldnameforoldvalue) {
        this.fieldnameforoldvalue = fieldnameforoldvalue;
    }

    public void setFieldnamefornewvalue(String fieldnamefornewvalue) {
        this.fieldnamefornewvalue = fieldnamefornewvalue;
    }

    public void setEncode(String encode) {
        this.encode = encode;
    }

    public void execute() throws BuildException {
        /*String zipFile = getProject().getProperty("zipFile");
        String chgExcelFileName = getProject().getProperty("chgExcelFileName");
        String sheetName = getProject().getProperty("sheetName");
        String fieldNameForchangeId = getProject().getProperty("fieldNameForChangeId");
        String changeIdValues = getProject().getProperty("changeIdValues");
        String filedNameForFileNameInZip = getProject().getProperty("filedNameForFileNameInZip");
        String fieldNameForOldValue = getProject().getProperty("fieldNameForOldValue");
        String fieldNameForNewValue = getProject().getProperty("fieldNameForNewValue");*/
        try {
            FileUtils.replaceZipFileContentByExcel(zipfile, chgexcelfilename, sheetname, fieldnameforchangeid, changeidvalues.split(","), filednameforfilenameinzip, fieldnameforoldvalue, fieldnamefornewvalue,encode);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
