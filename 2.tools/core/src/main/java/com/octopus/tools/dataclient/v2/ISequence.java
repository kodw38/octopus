package com.octopus.tools.dataclient.v2;

/**
 * User: wfgao_000
 * Date: 15-9-18
 * Time: 下午6:01
 */
public interface ISequence {
    public long getNextSequence(String name)throws Exception;
}
