package com.octopus.tools.i18n.impl.date;

import com.octopus.tools.i18n.IFormat;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.time.DateTimeUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;

import java.util.Date;

/**
 * User: wangfeng2
 * Date: 14-8-21
 * Time: 下午3:03
 */
public class DateFormate extends XMLObject implements IFormat {
    String pattern;

    public DateFormate(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    @Override
    public void notifyObject(String op, Object obj) throws Exception {

    }

    @Override
    public void addTrigger(Object cond, InvokeTaskByObjName task) throws Exception {

    }

    @Override
    public void destroy() throws Exception {

    }

    @Override
    public void initial() throws Exception {

    }

    @Override
    public Object formate(Object o) {
        return DateTimeUtils.date2String((Date) o,pattern);
    }

    @Override
    public Object formate2System(Object o) {
        return DateTimeUtils.string2Date((String)o,pattern);
    }
}
