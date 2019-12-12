package com.octopus.isp.ds.data;

/**
 * Created by admin on 2019/12/9.
 */
public class DefaultDataFormat implements IDataFormat {
    @Override
    public String format(Object o) {
        if(null!=o)
            return o.toString();
        else{
            return "";
        }
    }
}
