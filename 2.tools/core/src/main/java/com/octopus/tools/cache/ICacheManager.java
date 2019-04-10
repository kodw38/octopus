package com.octopus.tools.cache;

/**
 * 只管各种类的缓存，每个缓存的存储，均衡实例由具体Cache实现。
 * User: Administrator
 * Date: 14-9-17
 * Time: 下午1:52
 */
public interface ICacheManager {
    public boolean addCache(ICache cache);

    public ICache getCache(String name);

    public void addListener(ICacheListener cacheListener);
}
