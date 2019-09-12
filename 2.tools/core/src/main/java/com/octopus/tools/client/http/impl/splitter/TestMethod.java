package com.octopus.tools.client.http.impl.splitter;

import com.octopus.tools.client.http.impl.HttpURLConnectionUtils;

public class TestMethod {
    public TestMethod() {
        try {
            SiteInfoBean bean = new SiteInfoBean("http://pic1.win4000.com/wallpaper/c/53cdd1f7c1f21.jpg"
                    , "c:/logs/tmp"
                    , "53cdd1f7c1f21.jpg"
                    , 5);
            //SiteInfoBean bean = new SiteInfoBean("http://localhost:8080/down.zip","L:temp","weblogic60b2_win.exe",1);
            SiteFileFetch fileFetch = new SiteFileFetch(bean);
            //fileFetch.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args){
        System.out.println(HttpURLConnectionUtils.removeRelative("http://gmpg.org/."));
        //System.out.println("http://sss/.".endsWith("/."));
        //new TestMethod();
    }
    }

