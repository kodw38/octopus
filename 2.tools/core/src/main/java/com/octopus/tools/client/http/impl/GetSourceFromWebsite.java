package com.octopus.tools.client.http.impl;

import com.octopus.tools.client.http.HttpDS;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.file.FileUtils;
import com.octopus.tools.client.http.impl.splitter.SiteFileFetch;
import com.octopus.tools.client.http.impl.splitter.SiteInfoBean;
import com.octopus.utils.thread.ThreadPool;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class GetSourceFromWebsite extends XMLDoObject {
    public GetSourceFromWebsite(XMLMakeup xml, XMLObject parent, Object[] containers) throws Exception {
        super(xml, parent, containers);
        start();
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
            List<Map> urls = (List)input.get("urls");
            String savePath = (String)input.get("savePath");
            List<String> suffix = (List)input.get("suffix");
            Integer minSizeLimit = (Integer) input.get("minSizeLimit");
            Integer timeout = (Integer) input.get("timeout");
            doGetSrouce(urls,savePath,suffix, minSizeLimit,timeout,null);
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

    static void doGetSrouce(List<Map> us,String savePath,List<String> suffix,int sourceMinSizeLimit,int timeout,Map headers){
        if(null != us){
            for(Map ul:us){
                try {
                    String url = (String)ul.get("url");
                    List fi = (List)ul.get("filter");
                    HttpDS ds = HttpURLConnectionUtils.sendRequest(url, "GET", headers, null, timeout);
                    if (null != ds && null != ds.getResponseHeaders()) {
                        if (HttpURLConnectionUtils.isHtml(url, ds.getResponseHeaders().get("Content-Type"))) {
                            String s = new String(((ByteArrayOutputStream) ds.getResponseOutputStream()).toByteArray());
                            List<String> images = StringUtils.getTagsNoMark(s, "image=\"", "\"");
                            if (null != images && images.size() > 0) {
                                if(null == headers){headers = new HashMap();}
                                headers.put("Referer",url);
                                doGetSrouce(getUrls(url, images,fi), savePath, suffix, sourceMinSizeLimit, timeout,headers);
                            }
                            List<String> srcs = StringUtils.getTagsNoMark(s, "src=\"", "\"");
                            if (null != srcs && srcs.size() > 0) {
                                if(null == headers){headers = new HashMap();}
                                headers.put("Referer",url);
                                doGetSrouce(getUrls(url, srcs,fi), savePath, suffix, sourceMinSizeLimit, timeout,headers);
                            }
                            List<String> hrefs = StringUtils.getTagsNoMark(s, "href=\"", "\"");
                            if (null != hrefs && hrefs.size() > 0) {
                                if(null == headers){headers = new HashMap();}
                                headers.put("Referer",url);
                                doGetSrouce(getUrls(url, hrefs,fi), savePath, suffix, sourceMinSizeLimit, timeout,headers);
                            }

                        } else {
                            if (null != suffix && StringUtils.endsWithAny(StringUtils.getFileNameFromUrl(url), suffix.toArray(new String[0]))
                                    && null != ds.getResponseHeaders().get("Content-Length") && Integer.parseInt(ds.getResponseHeaders().get("Content-Length")) > sourceMinSizeLimit) {
                                try {
                                    if(((ByteArrayOutputStream)ds.getResponseOutputStream()).size()>=Integer.parseInt(ds.getResponseHeaders().get("Content-Length"))) {
                                        FileUtils.saveFile(savePath + "/" + StringUtils.getFileNameFromUrl(url), (ByteArrayOutputStream) ds.getResponseOutputStream(), false);
                                    }else{
                                        File f = new File(savePath + "/" + StringUtils.getFileNameFromUrl(url));
                                        if(!f.exists()){
                                            inputQueue(url,savePath , StringUtils.getFileNameFromUrl(url));
                                        }else{
                                            if(f.length()!=Integer.parseInt(ds.getResponseHeaders().get("Content-Length"))){
                                                String filename = StringUtils.getFileNameFromUrl(url);
                                                String end = filename.substring(filename.lastIndexOf(".")+1,filename.length());
                                                String name = filename.substring(0,filename.lastIndexOf("."));
                                                filename = name + "_" + System.currentTimeMillis()+"."+end;
                                                inputQueue(url,savePath , filename);
                                            }
                                        }

                                    }
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
    static ArrayBlockingQueue queue = new ArrayBlockingQueue(1000000);
    static Executor ep = Executors.newFixedThreadPool(10);
    static void inputQueue(String url,String savePath,String fileName){
        boolean b = queue.add(url+"|"+savePath+"|"+fileName);
        if(!b){
            try {
                Thread.sleep(60000);
                inputQueue(url, savePath, fileName);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    public static void start(){
        ThreadPool.getInstance().getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                while(true){
                    try {
                        String s = (String)queue.take();

                        String[] s3 = s.split("\\|");
                        SiteInfoBean bean = new SiteInfoBean(s3[0]
                                , s3[1]
                                , s3[2]
                                , 5);
                        SiteFileFetch fileFetch = new SiteFileFetch(bean);
                        ep.execute(fileFetch);
                    }catch (Exception e){

                    }
                }
            }
        });

    }


    static List<Map> getUrls(String base,List<String> us,List fi){
        if(null == base || base.length()<8) return null;
        if(null != us) {
            List<Map> l = new ArrayList();
            String host=null;
            if(base.indexOf("/",8)>0) {
                host = base.substring(base.indexOf("//") + 2, base.indexOf("/", 8));
            }else{
                host = base.substring(base.indexOf("//") + 2);
            }
            for(String u:us){
                if(u.startsWith("//")) {
                    l.add(getUrlMap(base.substring(0,base.indexOf("//"))+u,fi));
                }else if(u.startsWith("/") ){
                    if(base.indexOf("/",8)>0) {
                        String b = base.substring(0, base.indexOf("/", 8));
                        l.add(getUrlMap(b + u,fi));
                    }
                }else if(u.startsWith("http")){
                    if(null == fi || ArrayUtils.isLikeArrayInString(u,fi)) {
                        l.add(getUrlMap(u,fi));
                    }
                }else{
                    l.add(getUrlMap(StringUtils.getFileParentUrl(base)+"/"+u,fi));
                }
            }
            return l;
        }
        return null;
    }
    static Map getUrlMap(String url,List fi){
        Map m = new HashMap();
        m.put("url",url);
        m.put("filter",fi);
        return m;
    }
    public static void main(String[] args){
        List<Map> us = new ArrayList();
        //us.add("https://www.ivsky.com/bizhi/1920x1080/");
        //us.add("https://cnsexy.net");
        //us.add("https://cnsexy.net/page/7/");
        //us.add("https://cnsexy.net/%e6%9e%81%e5%93%81%e7%88%86%e4%b9%b3%e5%95%86%e5%8a%a1%e6%a8%a1%e7%89%b9%e3%80%8e%e8%8b%8f%e5%84%bf%e3%80%8f%e6%bf%80%e6%83%85%e4%ba%92%e5%8a%a8%e7%b2%89%e5%ab%a9%e7%be%8e%e7%a9%b4%e6%97%a0%e5%a5%97/");
        //us.add("https://cnsexy.net/wp-content/uploads/2018/10/CNSEXY.NET_006_2014-11-20-14-40-44.jpg?gid=247");
        HashMap m = new HashMap();
        m.put("url","https://cnsexy.net/page/2/");
        us.add(m);
        //us.add("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1567225613639&di=ef7672051b33a8c4fca59aaf74d0bf1d&imgtype=0&src=http%3A%2F%2Fimg.pconline.com.cn%2Fimages%2Fupload%2Fupc%2Ftx%2Fphotoblog%2F1707%2F24%2Fc5%2F53579438_1500865799809.jpg");
        String savePath = "c:/logs/im";
        List<String> suffix = new ArrayList<>();
        suffix.add(".jpg");
        suffix.add(".png");
        suffix.add(".gif");
        int minSizeLimit=100000;
        doGetSrouce(us,savePath,suffix,minSizeLimit,6000000,null);
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
