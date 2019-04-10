package com.octopus.utils.cls.jcl.resource;

import java.net.URL;

public class URLFileInfo implements Comparable {

	URL url;
	long judge;
	
	public URLFileInfo(){}
	
	public URLFileInfo(URL url,long judge){
		this.url = url;
		this.judge=judge;
	}
	
	public long getJudge() {
		return judge;
	}
	public void setJudge(long judge) {
		this.judge = judge;
	}
	public URL getUrl() {
		return url;
	}
	public void setUrl(URL url) {
		this.url = url;
	}

	public int compareTo(Object o) {
		String name1 = this.getUrl().getFile(); 
		String name2 = ((URLFileInfo)o).getUrl().getFile();
		if (name1.compareTo(name2) > 0) {
            return 1;
        } else if (name1.compareTo(name2) < 0) {
            return 0;
        } else {
            return 0;
        }
	}
	
	
}
