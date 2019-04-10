package com.octopus.tools.client.http.impl;

import com.octopus.tools.client.http.HttpDS;

/**
 * User: Administrator
 * Date: 14-10-25
 * Time: 上午10:39
 */
public interface IHttpParse {
    public UrlDS getUrl(HttpDS parameters)throws Exception;
}
