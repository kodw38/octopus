package com.octopus.tools.client.http;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * User: Administrator
 * Date: 14-10-25
 * Time: 上午10:59
 */
public class HttpDS {
    Hashtable<String,String> requestHeaders;
    Hashtable<String,String> responseHeaders;
    String methodName;
    InputStream requestInputStream;
    int requestInputStreamLength;
    OutputStream responseOutputStream;
    String contentType;
    long contentLength;
    int statusCode;
    String base;
    boolean isSendCookie;
    HashMap<String,String> properties=new HashMap<String, String>();

    String proxyName;
    String proxyPwd;
    String proxyUrl=null;
    int proxyPort=0;
    String[] removeRequestCookies=null;
    String[] saveResponseCookies=null;
    int connectionTimeout=0,readTimeout=0;
    Boolean redirect;
    String url;
    Map cacert;

    public boolean isSendCookie() {
        return isSendCookie;
    }

    public void setSendCookie(boolean isSendCookie) {
        this.isSendCookie = isSendCookie;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getProxyName() {
        return proxyName;
    }

    public void setProxyName(String proxyName) {
        this.proxyName = proxyName;
    }

    public String getProxyPwd() {
        return proxyPwd;
    }

    public void setProxyPwd(String proxyPwd) {
        this.proxyPwd = proxyPwd;
    }

    public String getProxyUrl() {
        return proxyUrl;
    }

    public void setProxyUrl(String proxyUrl) {
        this.proxyUrl = proxyUrl;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public String[] getRemoveRequestCookies() {
        return removeRequestCookies;
    }

    public void setRemoveRequestCookies(String[] removeRequestCookies) {
        this.removeRequestCookies = removeRequestCookies;
    }

    public String[] getSaveResponseCookies() {
        return saveResponseCookies;
    }

    public void setSaveResponseCookies(String[] saveResponseCookies) {
        this.saveResponseCookies = saveResponseCookies;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public Boolean isRedirect() {
        return redirect;
    }

    public void setRedirect(boolean redirect) {
        this.redirect = redirect;
    }

    public HashMap<String, String> getProperties() {
        return properties;
    }

    public void setProperties(HashMap<String, String> properties) {
        this.properties = properties;
    }

    public int getRequestInputStreamLength() {
        return requestInputStreamLength;
    }

    public void setRequestInputStreamLength(int requestInputStreamLength) {
        this.requestInputStreamLength = requestInputStreamLength;
    }

    public Hashtable<String, String> getRequestHeaders() {
        if(null == requestHeaders)requestHeaders= new Hashtable();
        return requestHeaders;
    }

    public void setRequestHeaders(Hashtable<String, String> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public Hashtable<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(Hashtable<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public InputStream getRequestInputStream() {
        return requestInputStream;
    }

    public void setRequestInputStream(InputStream requestInputStream) {
        this.requestInputStream = requestInputStream;
    }

    public OutputStream getResponseOutputStream() {
        return responseOutputStream;
    }

    public void setResponseOutputStream(OutputStream responseOutputStream) {
        this.responseOutputStream = responseOutputStream;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getContentLength() {
        return contentLength;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getBase() {
        return base;
    }

    public Map<String,String> getSSLCacert(){
        return cacert;
    }
    public void addCertFile(String name,String filePath){
        if(cacert==null)cacert=new HashMap();
        cacert.put(name,filePath);
    }
    public void setSSLCacert(Map m){
        cacert=m;
    }

    public void setBase(String base) {
        this.base = base;
    }
}
