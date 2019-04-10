package com.octopus.tools.synchro.canal.impl;

import com.alibaba.otter.canal.instance.manager.model.CanalParameter;

import java.util.HashMap;

/**
 * User: wfgao_000
 * Date: 16-6-5
 * Time: 下午5:00
 */
public class MyCanalParameter extends CanalParameter {
    HashMap addition = new HashMap();

    public void put(String k,Object o){
        addition.put(k,o);
    }
    public Object get(String k){
        return addition.get(k);
    }
}
