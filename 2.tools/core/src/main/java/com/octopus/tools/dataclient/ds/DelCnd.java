package com.octopus.tools.dataclient.ds;

import com.octopus.tools.dataclient.ds.store.FieldCondition;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Administrator
 * Date: 14-10-10
 * Time: 下午5:53
 */
public class DelCnd {
    List<FieldCondition> conditions = new ArrayList<FieldCondition>();

    public void addFieldCondition(FieldCondition fc){
        conditions.add(fc);
    }

    public List<FieldCondition> getConditions() {
        return conditions;
    }

    public void setConditions(List<FieldCondition> conditions) {
        this.conditions = conditions;
    }
}
