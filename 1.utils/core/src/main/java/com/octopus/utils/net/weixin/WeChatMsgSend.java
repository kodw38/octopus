package com.octopus.utils.net.weixin;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.octopus.utils.alone.ObjectUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cls.POJOUtil;
import net.sf.json.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.slf4j.LoggerFactory;


/**
 * 微信发送消息
 *
 * @author PC-MXF
 *
 */
public class WeChatMsgSend {

    private static HttpClient httpClient;

    /**
     * 用于提交登录数据
     */
    private HttpPost httpPost;

    /**
     * 用于获得登陆后页面
     */
    private HttpGet httpGet;

    public static final String CONTENT_TYPE = "Content-Type";

    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    void getClient(){
        HttpParams params = new BasicHttpParams();
        Integer CONNECTION_TIMEOUT = 2 * 1000; //设置请求超时2秒钟 根据业务调整
        Integer SO_TIMEOUT = 2 * 1000; //设置等待数据超时时间2秒钟 根据业务调整
        Long CONN_MANAGER_TIMEOUT = 500L; //该值就是连接不够用的时候等待超时时间，一定要设置，而且不能太大 ()
        params.setIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, CONNECTION_TIMEOUT);
        params.setIntParameter(CoreConnectionPNames.SO_TIMEOUT, SO_TIMEOUT);
        params.setLongParameter(ClientPNames.CONN_MANAGER_TIMEOUT, CONN_MANAGER_TIMEOUT);
        //在提交请求之前 测试连接是否可用
        params.setBooleanParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, true);
        PoolingClientConnectionManager conMgr = new PoolingClientConnectionManager();
        conMgr.setMaxTotal(200); //设置整个连接池最大连接数 根据自己的场景决定
        //是路由的默认最大连接（该值默认为2），限制数量实际使用DefaultMaxPerRoute并非MaxTotal。
        //设置过小无法支持大并发(ConnectionPoolTimeoutException: Timeout waiting for connection from pool)，路由是对maxTotal的细分。
        conMgr.setDefaultMaxPerRoute(conMgr.getMaxTotal());//（目前只有一个路由，因此让他等于最大值）
        //另外设置http client的重试次数，默认是3次；当前是禁用掉（如果项目量不到，这个默认即可）

        httpClient = new DefaultHttpClient(conMgr,params);
    }
    /**
     * 微信授权请求，GET类型，获取授权响应，用于其他方法截取token
     *
     * @param Get_Token_Url
     * @return String 授权响应内容
     * @throws IOException
     */
    protected String toAuth(String Get_Token_Url) throws IOException {
        if(httpClient==null){
            getClient();
        }
        httpGet = new HttpGet(Get_Token_Url);

        HttpResponse response = httpClient.execute(httpGet);
        String resp = "";

        try {
            HttpEntity entity = response.getEntity();
            resp = EntityUtils.toString(entity, "utf-8");
            EntityUtils.consume(entity);
        } catch (Exception e) {
            e.getStackTrace();
        } finally {
            //response.close();
        }
        LoggerFactory.getLogger(getClass()).info(" resp:{}", resp);
        return resp;
    }

    /**
     * corpid应用组织编号 corpsecret应用秘钥 获取toAuth(String
     * Get_Token_Url)返回结果中键值对中access_token键的值
     *
     * @param
     */
    public String getToken(String corpid, String corpsecret) throws IOException {
        WeChatMsgSend sw = new WeChatMsgSend();
        WeChatUrlData uData = new WeChatUrlData();
        uData.setGet_Token_Url(corpid, corpsecret);
        String resp = sw.toAuth(uData.getGet_Token_Url());
        System.out.println("resp=====:" + resp);
        try {
            Map map = StringUtils.convert2MapJSONObject(resp);
            return map.get("access_token").toString();
        } catch (Exception e) {
            e.getStackTrace();
            return resp;
        }
    }

    /**
     * 创建微信发送请求post数据 touser发送消息接收者 ，msgtype消息类型（文本/图片等）， application_id应用编号。
     * 本方法适用于text型微信消息，contentKey和contentValue只能组一对
     *
     * @param touser
     * @param msgtype
     * @param application_id
     * @param contentKey
     * @param contentValue
     * @return
     */
    public String createpostdata(String touser, String msgtype, int application_id, String contentKey,
                                 String contentValue)throws Exception {
        WeChatData wcd = new WeChatData();
        wcd.setTouser(touser);
        wcd.setAgentid(application_id + "");
        wcd.setMsgtype(msgtype);
        Map<Object, Object> content = new HashMap<Object, Object>();
        content.put(contentKey, contentValue);
        wcd.setText(content);
        return ObjectUtils.convertMap2String(POJOUtil.convertPojo2Map(wcd,new AtomicLong(0)));
    }

    /**
     * @Title  创建微信发送请求post实体，charset消息编码    ，contentType消息体内容类型，
     * url微信消息发送请求地址，data为post数据，token鉴权token
     * @param charset
     * @param contentType
     * @param url
     * @param data
     * @param token
     * @return
     * @throws IOException
     */
    public String post(String charset, String contentType, String url, String data, String token) throws IOException {
        if(null == httpClient){
            getClient();
        }
        httpPost = new HttpPost(url + token);
        httpPost.setHeader(CONTENT_TYPE, contentType);
        httpPost.setEntity(new StringEntity(data, charset));
        HttpResponse response = httpClient.execute(httpPost);
        String resp;
        try {
            HttpEntity entity = response.getEntity();
            resp = EntityUtils.toString(entity, charset);
            EntityUtils.consume(entity);
        } finally {
            //response.close();
        }
        LoggerFactory.getLogger(getClass()).info("call [{}], param:{}, resp:{}", url, data, resp);
        return resp;
    }
}
