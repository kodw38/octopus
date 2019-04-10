package com.octopus.tools.jvminsmgr.invokehandler;

import com.octopus.tools.jvminsmgr.InstancesUtils;
import com.octopus.tools.jvminsmgr.ds.SynResults;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLMakeup;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * User: Administrator
 * Date: 14-9-18
 * Time: 下午6:05
 */
public class MargeResultInvokeAction implements IExtendInvokeAction {
    SynResults results;
    XMLMakeup xml;
    List<String> ms = new ArrayList<String>();
    public MargeResultInvokeAction(){
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
        results = InstancesUtils.remoteSynInvokeAllInstances(impl.getClass().getSuperclass().getName(),m,args,null);
    }

    @Override
    public void doAfterInvoke(Object impl, String m, Object[] args, boolean isSuccess, Throwable e, Object result) {
        results.waitAllFinished();
    }

    @Override
    public Object doResult(Object result) {
        //最合合并结果返回
        if(null != results){
            Object ret = null;
            for(Object c:results.getResults()){
                if(null != c && c.getClass().equals(result.getClass())){
                    if(result.getClass().isArray()){
                        if(null == ret){
                            ret = result;
                        }
                        int ol = Array.getLength(ret);
                        int nl = Array.getLength(c);
                        Object temp = Array.newInstance(result.getClass(),ol+nl);
                        System.arraycopy(ret,0,temp,0,ol);
                        System.arraycopy(c,0,temp,ol-1,nl);
                        ret = temp;
                    }
                    if(List.class.isAssignableFrom(result.getClass()) && c.getClass().equals(result.getClass())){
                        if(null == ret){
                            ret = result;
                        }
                        ((List)ret).addAll((List)c);
                    }
                    if(Map.class.isAssignableFrom(result.getClass()) && c.getClass().equals(result.getClass())){
                        if(null == ret){
                            ret = result;
                        }
                        ((Map)ret).putAll((Map) c);
                    }
                }
            }
            return ret;
        }
        return result;
    }
}
