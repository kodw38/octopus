package com.octopus.tools.dataclient;

import com.octopus.tools.dataclient.ds.field.FieldDef;

/**
 * User: Administrator
 * Date: 14-10-23
 * Time: 上午11:14
 */
public interface IDictionary {

    public FieldDef[] getFieldDef(String likeFieldName);


}
