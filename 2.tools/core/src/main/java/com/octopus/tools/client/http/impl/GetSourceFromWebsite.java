package com.octopus.tools.client.http.impl;

import com.octopus.tools.client.http.HttpDS;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.file.FileUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GetSourceFromWebsite extends XMLDoObject {
    public GetSourceFromWebsite(XMLMakeup xml, XMLObject parent, Object[] containers) throws Exception {
        super(xml, parent, containers);
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(null != input) {
            List<String> urls = (List)input.get("urls");
            String savePath = (String)input.get("savePath");
            List<String> suffix = (List)input.get("suffix");
            Integer minSizeLimit = (Integer) input.get("minSizeLimit");
            Integer timeout = (Integer) input.get("timeout");
               doGetSrouce(urls,savePath,suffix, minSizeLimit,timeout);
        }
        return null;
    }

    @Override
    public ResultCheck checkReturn(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return new ResultCheck(true,ret);
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return true;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret, Exception e) throws Exception {
        return false;
    }

    static void doGetSrouce(List<String> us,String savePath,List<String> suffix,int sourceMinSizeLimit,int timeout){
        if(null != us){
            for(String url:us){
                try {
                    HttpDS ds = HttpURLConnectionUtils.sendRequest(url, "GET", null, null, timeout);
                    if (null != ds && null != ds.getResponseHeaders()) {
                        if (HttpURLConnectionUtils.isHtml(url, ds.getResponseHeaders().get("Content-Type"))) {
                            String s = new String(((ByteArrayOutputStream) ds.getResponseOutputStream()).toByteArray());
                            List<String> images = StringUtils.getTagsNoMark(s, "image=\"", "\"");
                            if (null != images && images.size() > 0) {
                                doGetSrouce(getUrls(url, images), savePath, suffix, sourceMinSizeLimit, timeout);
                            }
                            List<String> srcs = StringUtils.getTagsNoMark(s, "src=\"", "\"");
                            if (null != srcs && srcs.size() > 0) {
                                doGetSrouce(getUrls(url, srcs), savePath, suffix, sourceMinSizeLimit, timeout);
                            }
                            List<String> hrefs = StringUtils.getTagsNoMark(s, "href=\"", "\"");
                            if (null != hrefs && hrefs.size() > 0) {
                                doGetSrouce(getUrls(url, hrefs), savePath, suffix, sourceMinSizeLimit, timeout);
                            }


                        } else {
                            if (null != suffix && StringUtils.endsWithAny(StringUtils.getFileNameFromUrl(url), suffix.toArray(new String[0]))
                                    && ((ByteArrayOutputStream) ds.getResponseOutputStream()).size() > sourceMinSizeLimit) {
                                try {
                                    FileUtils.saveFile(savePath + "/" + StringUtils.getFileNameFromUrl(url), (ByteArrayOutputStream) ds.getResponseOutputStream(), false);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }catch (Throwable e){
                    log.error("",e);
                }
            }
        }
    }

    static List<String> getUrls(String base,List<String> us){
        if(null != us) {
            List l = new ArrayList();
            for(String u:us){
                if(u.startsWith("//")) {
                    l.add(base.substring(0,base.indexOf("//"))+u);
                }else if(u.startsWith("/") ){
                    if(base.indexOf("/",8)>0) {
                        String b = base.substring(0, base.indexOf("/", 8));
                        l.add(b + u);
                    }
                }else if(u.startsWith("http")){
                    l.add(u);
                }else{
                    l.add(StringUtils.getFileParentUrl(base)+"/"+u);
                }
            }
            return l;
        }
        return null;
    }
    public static void main(String[] args){
        List<String> us = new ArrayList();
        //us.add("https://www.ivsky.com/bizhi/1920x1080/");
        //us.add("https://cnsexy.net");
        //us.add("https://cnsexy.net/page/7/");
        //us.add("https://cnsexy.net/%e6%9e%81%e5%93%81%e7%88%86%e4%b9%b3%e5%95%86%e5%8a%a1%e6%a8%a1%e7%89%b9%e3%80%8e%e8%8b%8f%e5%84%bf%e3%80%8f%e6%bf%80%e6%83%85%e4%ba%92%e5%8a%a8%e7%b2%89%e5%ab%a9%e7%be%8e%e7%a9%b4%e6%97%a0%e5%a5%97/");
        //us.add("https://cnsexy.net/wp-content/uploads/2018/10/CNSEXY.NET_006_2014-11-20-14-40-44.jpg?gid=247");
        us.add("https://cnsexy.net/page/2/");
        //us.add("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1567225613639&di=ef7672051b33a8c4fca59aaf74d0bf1d&imgtype=0&src=http%3A%2F%2Fimg.pconline.com.cn%2Fimages%2Fupload%2Fupc%2Ftx%2Fphotoblog%2F1707%2F24%2Fc5%2F53579438_1500865799809.jpg");
        String savePath = "c:/logs/im";
        List<String> suffix = new ArrayList<>();
        suffix.add(".jpg");
        suffix.add(".png");
        suffix.add(".gif");
        int minSizeLimit=100000;
        doGetSrouce(us,savePath,suffix,minSizeLimit,6000000);
        System.out.println("running is finished");
        /*ry {
            File f = new File("C:\\Users\\Public\\Pictures\\Sample Pictures\\Jellyfish.jpg");
            System.out.println(f.length());
            FileInputStream s = new FileInputStream(f);
            System.out.println(s.available());
        }catch (Exception e){
            e.printStackTrace();
        }*/
    }

}
