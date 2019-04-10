package com.octopus.tools.client.http.impl;

/**
 * User: Administrator
 * Date: 14-10-25
 * Time: 上午11:09
 */
public class UrlDS {
    String url;
    String proxyName;
    String proxyPwd;
    String proxyUrl;
    int proxyPort;
    String[] removeRequestCookies;
    String[] saveResponseCookies;
    int connectionTimeout,readTimeout;
    boolean redirect;

    public boolean isRedirect() {
        return redirect;
    }

    public void setRedirect(boolean redirect) {
        this.redirect = redirect;
    }

    public String getUrl() {
        return url;
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
}
