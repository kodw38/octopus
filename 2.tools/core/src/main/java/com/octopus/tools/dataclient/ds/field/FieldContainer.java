package com.octopus.tools.dataclient.ds.field;


import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.NoSuchElementException;

/**
 * User: wf
 * Date: 2008-8-25
 * Time: 23:15:47
 */
public class FieldContainer {
    private static final Logger log = Logger.getLogger(FieldContainer.class);
    
    private static final HashMap<String,FieldDef> allFieldDefs = new HashMap();

    public static void addFieldDef(FieldDef fieldDef){
        if(null == fieldDef.getFieldCode() || "".equals(fieldDef.getFieldCode().trim())){
            throw new NoSuchElementException("FieldDef's FieldDefCode can't null.");
        }

        if(fieldDef.getFieldType()==null){
            throw new NoSuchElementException("FieldDef's FieldDefType can't null.");
        }
        if(!allFieldDefs.containsKey(fieldDef.getFieldCode()))
            allFieldDefs.put(fieldDef.getFieldCode(),fieldDef);
    }



    public static FieldDef getField(String FieldDefCode){
        return allFieldDefs.get(FieldDefCode);
    }


    public static void clearFields(){
        allFieldDefs.clear();
    }


    public static HashMap getAllFields() {
        return allFieldDefs;
    }


}
