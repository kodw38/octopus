package com.octopus.utils.xml.auto.logic;

import com.octopus.utils.xml.auto.XMLParameter;

/**
 * User: wfgao_000
 * Date: 15-11-13
 * Time: 下午11:13
 */
public interface ITradeFinish extends Runnable{
    public void setParameter(XMLParameter xml);
    public void clear();
}
