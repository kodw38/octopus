package com.octopus.tools.jvminsmgr.invokehandler;

import com.octopus.tools.jvminsmgr.InstancesUtils;
import com.octopus.utils.alone.BooleanUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLMakeup;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Administrator
 * Date: 14-9-18
 * Time: 下午6:06
 */
public class RemoteCalLocalResourceInvokeAction implements IExtendInvokeAction {
    List<String> ms = new ArrayList<String>();
    Object ret=null;
    XMLMakeup xml;
    public RemoteCalLocalResourceInvokeAction(){
        super();
        String m = xml.getProperties().getProperty("targetmethods");
        if(StringUtils.isNotBlank(m)){
            String[] ss =StringUtils.chomp(m).split(",");
            for(String s:ss)
                ms.add(s);
        }
    }

    @Override
    public void setXml(XMLMakeup xml) {
        this.xml=xml;
    }

    @Override
    public boolean isExecute(String xmlid,String id,String mn) {
        if(ms.size()>0 && (StringUtils.isNotBlank(xmlid) || StringUtils.isNotBlank(id))){
            if(StringUtils.isNotBlank(xmlid) && ms.contains(xmlid+"#"+mn)){
                return true;
            }
            if( StringUtils.isNotBlank(id) && ms.contains(id+"#"+mn)){
                return true;
            }
            return false;
        }
        return true;
    }

    @Override
    public void doBeforeInvoke(Object impl, String m, Object[] args) throws Exception {
        ret = InstancesUtils.remoteWaitCalLocalResourceExtendInvoke(impl.getClass().getSuperclass().getName(), m, args, null);
        if(BooleanUtils.isTrue(ret)){
            throw new SuccessfulException();
        }
    }

    @Override
    public void doAfterInvoke(Object impl, String m, Object[] args, boolean isSuccess, Throwable e, Object result) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object doResult(Object result) {
        if(null != ret)return ret;
        return result;
    }
}
