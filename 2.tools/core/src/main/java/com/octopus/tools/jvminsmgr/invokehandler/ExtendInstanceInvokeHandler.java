package com.octopus.tools.jvminsmgr.invokehandler;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cls.proxy.IMethodAddition;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;

import java.util.ArrayList;
import java.util.List;

/**
 * User: Administrator
 * Date: 14-9-17
 * Time: 下午2:48
 */
public class ExtendInstanceInvokeHandler extends XMLObject implements IMethodAddition {
    List<Class> actions= new ArrayList<Class>();
    XMLMakeup[] as;
    public ExtendInstanceInvokeHandler(XMLMakeup xml,XMLObject parent,Object[] containers) throws Exception {
        super(xml,parent,containers);
        as = getXML().getChild("action");
        if(null != as){
            for(XMLMakeup a:as){
                if(StringUtils.isNotBlank(a.getProperties().getProperty("path"))){
                    try {
                        actions.add(Class.forName(a.getProperties().getProperty("path")));
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
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
    public Object beforeAction(Object impl, String m, Object[] args)throws Exception{
        if(null != actions){
            for(int i=0;i<actions.size();i++){
                try{
                    IExtendInvokeAction an = (IExtendInvokeAction)actions.get(i).newInstance();
                    an.setXml(as[i]);
                    if(an.isExecute(((XMLObject)impl).getXML().getProperties().getProperty("xmlid"),((XMLObject)impl).getXML().getProperties().getProperty("id"),m)){
                        an.doBeforeInvoke(impl, m, args);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    @Override
    public Object afterAction(Object impl, String m, Object[] args,boolean isInvoke, boolean isSuccess, Throwable e, Object result) {
        if(null != actions){
            for(int i=0;i<actions.size();i++){
                try{
                    IExtendInvokeAction an = (IExtendInvokeAction)actions.get(i).newInstance();
                    an.setXml(as[i]);
                    if(an.isExecute(((XMLObject)impl).getXML().getProperties().getProperty("xmlid"),((XMLObject)impl).getXML().getProperties().getProperty("id"),m)){
                        an.doAfterInvoke(impl, m, args,isSuccess,e,result);
                    }
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        }
        return null;
    }


    @Override
    public int getLevel() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isWaiteBefore() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isWaiteAfter() {
        return false;
    }

    @Override
    public boolean isWaiteResult() {
        return false;
    }

    @Override
    public boolean isNextInvoke() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setMethods(List<String> methods) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public List<String> getMethods() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Object resultAction(Object impl, String m, Object[] args,Object result) {
        if(null != actions){
            for(int i=0;i<actions.size();i++){
                try{
                    IExtendInvokeAction an = (IExtendInvokeAction)actions.get(i).newInstance();
                    an.setXml(as[i]);
                    if(an.isExecute(((XMLObject)impl).getXML().getProperties().getProperty("xmlid"),((XMLObject)impl).getXML().getProperties().getProperty("id"),m)){
                        result = an.doResult(result);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
            return result;
        }
        return result;
    }


}
