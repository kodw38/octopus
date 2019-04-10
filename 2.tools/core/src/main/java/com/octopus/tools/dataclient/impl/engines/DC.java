package com.octopus.tools.dataclient.impl.engines;

import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLMakeup;

import java.util.HashMap;

/**
 * User: Administrator
 * Date: 14-9-24
 * Time: 上午11:12
 */
public class DC extends HashMap {
    public static String KEY_OP_STR="opstr";
    public static String KEY_ROLLBACK_OP_STR="opstr";
    public DC(XMLMakeup x){
        this.putAll(x.getProperties());
        XMLMakeup c = (XMLMakeup)ArrayUtils.getFirst(x.getChild("commd"));
        XMLMakeup r = (XMLMakeup)ArrayUtils.getFirst(x.getChild("rollback"));
        if(null != c && null != r && StringUtils.isNotBlank(c.getText()) && StringUtils.isNotBlank(r.getText())){
            this.put(KEY_OP_STR,c.getText());
            this.put(KEY_ROLLBACK_OP_STR,r.getText());
        }
    }
}
