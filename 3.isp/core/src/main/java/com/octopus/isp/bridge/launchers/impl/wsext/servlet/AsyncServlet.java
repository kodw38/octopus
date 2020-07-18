package com.octopus.isp.bridge.launchers.impl.wsext.servlet;/**
 * Created by admin on 2020/7/18.
 */

import com.octopus.isp.bridge.launchers.impl.wsext.bean.IotData;
import com.octopus.tools.dataclient.dataquery.redis.RedisClient;
import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cls.POJOUtil;
import com.octopus.utils.xml.auto.XMLDoObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.util.SocketAddressResolver;
import redis.clients.jedis.Jedis;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 *     //构造一个异步servlet用来处理iot请求在云服务中专给家庭服务
 //@WebServlet(urlPatterns = {"/async/iot"}, asyncSupported = true)
 * @ClassName AsyncServlet
 * @Description ToDo
 * @Author Kod Wong
 * @Date 2020/7/18 13:36
 * @Version 1.0
 **/
public class AsyncServlet extends HttpServlet {
    transient static Log log = LogFactory.getLog(AsyncServlet.class);
    Map<String ,AsyncContext> asyncServletMap = new HashMap<>();
    ExecutorService req_executor = Executors.newFixedThreadPool(10);
    ExecutorService res_executor = Executors.newFixedThreadPool(10);
    boolean isStartAsyncIot=false;
    boolean StartedAsyncIot=false;
    RedisClient redis;
    XMLDoObject manager;

    String redisDB,responseRedisKey,requestRedisKeyPrefix;

    public AsyncServlet(Properties properties,XMLDoObject manager){
        this.redisDB=properties.getProperty("reidsDB");
        this.responseRedisKey=properties.getProperty("responseRedisKey");
        this.requestRedisKeyPrefix=properties.getProperty("requestRedisKeyPrefix");
        this.manager = manager;
    }
    //Web应用线程池，用来处理异步Servlet

    public void service(HttpServletRequest req, HttpServletResponse resp) {
        //1. 调用startAsync或者异步上下文
        final AsyncContext ctx = req.startAsync();
        //用线程池来执行耗时操作
        req_executor.execute(new Runnable() {
            @Override
            public void run() {
                //在这里做耗时的操作
                Jedis jedis=null;
                try {
                    isStartAsyncIot=true;
                    redis = (RedisClient)manager.getObjectById("RedisClient");
                    //放入待处理队列，交由家庭服务处理
                    jedis = redis.getRedis(redisDB);
                    IotData id = new IotData(ctx);
                    //HashMap<String,String> m = new HashMap<>();
                    //m.put(id.getRequestId(),ObjectUtils.toString(id));
                    Map m = POJOUtil.convertPojo2Map(id,new AtomicLong(0));
                    if(log.isDebugEnabled()){
                        log.debug("ServletAsync push :"+requestRedisKeyPrefix+id.getLoginCode()+"  "+m);
                    }
                    jedis.lpush(requestRedisKeyPrefix+id.getLoginCode(), ObjectUtils.convertMap2String(m));
                    //获取处理返回结果
                    asyncServletMap.put(id.getRequestId(),ctx);
                    startReceive();
                } catch (Exception e) {
                    log.error("",e);
                    try {
                        ctx.getResponse().getWriter().write(e.getMessage());
                        ctx.complete();
                    }catch (Exception ex){
                        log.error("",ex);
                    }
                }finally {
                    if(null != jedis)
                        jedis.close();
                }
                //3. 异步Servlet处理完了调用异步上下文的complete方法

            }

        });
    }
    void reply(final String s){
        try {
            res_executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        if(null != s){
                            Map m = StringUtils.convert2MapJSONObject(s);
                            if (null != m && m.size()>0 && m.containsKey("requestId")) {

                                IotData data = (IotData) POJOUtil.convertMap2POJO(m, IotData.class, new AtomicLong(0));
                                if (null != data) {
                                    AsyncContext x = asyncServletMap.get(data.getRequestId());
                                    if (null != x) {
                                        if (null != data.getOutputData()) {
                                            if(data.getOutputData() instanceof Map) {
                                                String s = ObjectUtils.convertMap2String((Map)data.getOutputData());
                                                x.getResponse().getWriter().write(s);
                                                if(log.isDebugEnabled()){
                                                    log.debug("Servlet Async resposne write :"+s);
                                                }
                                            }else if(data.getOutputData() instanceof Collection){
                                                String s =ObjectUtils.convertList2String((List)data.getOutputData());
                                                x.getResponse().getWriter().write(s);
                                                if(log.isDebugEnabled()){
                                                    log.debug("Servlet Async resposne write :"+s);
                                                }
                                            }else if(data.getOutputData() instanceof ByteArrayOutputStream){
                                                x.getResponse().getOutputStream().write(((ByteArrayOutputStream)data.getOutputData()).toByteArray());
                                            }else if(data.getOutputData() instanceof ByteArrayInputStream){
                                                byte[] b = new byte[((ByteArrayInputStream)data.getOutputData()).available()];
                                                ((ByteArrayInputStream)data.getOutputData()).read(b);
                                                x.getResponse().getOutputStream().write(b);
                                            }else{
                                                String s = data.getOutputData().toString();
                                                x.getResponse().getWriter().write(s);
                                                if(log.isDebugEnabled()){
                                                    log.debug("Servlet Async resposne write :"+s);
                                                }
                                            }
                                        }
                                        x.complete();
                                        asyncServletMap.remove(data.getRequestId());
                                    }
                                }
                            }


                        }
                    }catch (Exception e){
                        log.error("",e);
                    }

                }
            });

        }catch (Exception e){
            log.error("",e);
        }
    }
    void startReceive(){
        if(isStartAsyncIot && !StartedAsyncIot){
            StartedAsyncIot=true;
            new Thread(new Runnable(){
                @Override
                public void run() {
                    while(true) {
                        Jedis jedis=null;
                        try {
                            jedis = redis.getRedis(redisDB);
                            List<String> reply = jedis.blpop(60000,responseRedisKey);
                            if(null != reply){
                                for(String s:reply){
                                    if(log.isDebugEnabled()){
                                        log.debug("Servlet Async reply:"+s);
                                    }
                                    reply(s);
                                }
                            }
                        }catch (Exception e){
                            log.error("",e);
                        }finally {
                            if(null != jedis)jedis.close();
                        }

                    }
                }
            }).start();

        }
    }
}
