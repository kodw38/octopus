package com.octopus.tools.client.http.impl.splitter;

public class SiteInfoBean {
    private String sSiteURL;
    //Site's URLprivate String sFilePath;
    private String sFilePath;
    //Saved File's Pathprivate String sFileName;
    private String sFileName;
    //Saved File's Nameprivate int nSplitter;
    private int nSplitter;
    //Count of Splited Downloading
    public SiteInfoBean(){
        //default value of nSplitter is 5
        this("","","",5);
    }
    public SiteInfoBean(String sURL,String sPath,String sName,int nSpiltter){
        sSiteURL= sURL;sFilePath = sPath;sFileName = sName;this.nSplitter = nSpiltter;
    }
    public String getSSiteURL(){return sSiteURL;}
    public void setSSiteURL(String value){sSiteURL = value;}
    public String getSFilePath(){return sFilePath;}
    public void setSFilePath(String value){sFilePath = value;}
    public String getSFileName(){return sFileName;}
    public void setSFileName(String value){sFileName = value;}
    public int getNSplitter(){return nSplitter;}
    public void setNSplitter(int nCount){nSplitter = nCount;}
}
