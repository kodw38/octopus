package com.octopus.utils.zip;

import java.util.HashMap;
import java.util.Map;

public class ReplaceZipItem {
	String[] jars;
	String fileName;
	Map<String,String> chgvalue = new HashMap<String, String>();
    public ReplaceZipItem(){

    }
    public ReplaceZipItem(String fileName,Map<String,String> values){
        if(fileName.contains("!")){
            String[] ts = fileName.split("!");
            setFileName(ts[1]);
            setJars(new String[]{ts[0]});
        }
        setFileName(fileName);
        setChgvalue(values);
    }
	public Map<String,String> getChgvalue() {
		return chgvalue;
	}
	public void setChgvalue(Map<String,String> chgvalue) {
		this.chgvalue = chgvalue;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String[] getJars() {
		return jars;
	}
	public void setJars(String[] jars) {
		this.jars = jars;
	}
	public void addReplace(String key,String value){
		chgvalue.put(key,value);
	}
}
