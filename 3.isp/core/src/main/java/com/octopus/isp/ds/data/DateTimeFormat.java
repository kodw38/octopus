package com.octopus.isp.ds.data;

import com.octopus.isp.ds.Context;
import com.octopus.utils.time.DateTimeUtils;

import java.text.SimpleDateFormat;

/**
 * Created by admin on 2019/12/9.
 */
public class DateTimeFormat implements IDataFormat {
    SimpleDateFormat format=null;
    public DateTimeFormat(String style, Context context){
        format = DateTimeUtils.getDateFormat(style,context.getLocale());
    }
    @Override
    public String format(Object o) {
        return format.format(o);
    }
}
