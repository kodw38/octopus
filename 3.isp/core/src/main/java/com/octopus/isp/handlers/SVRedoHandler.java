package com.octopus.isp.handlers;

import com.octopus.isp.ds.RequestParameters;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cls.proxy.IMethodAddition;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by robai on 2017/12/21.
 */
public class SVRedoHandler extends XMLDoObject implements IMethodAddition {
    XMLDoObject suspendTheRequest;

    //Handler must exist these properties
    List<String> methods;
    boolean isWaitBefore;
    boolean isWaitAfter;
    boolean isWaitResult;
    boolean isNextInvoke;
    Map in=null;


    Map ids = new HashMap();
    public SVRedoHandler(XMLMakeup xml, XMLObject parent, Object[] containers) throws Exception {
        super(xml, parent, containers);
        isWaitBefore= StringUtils.isTrue(xml.getProperties().getProperty("iswaitebefore"));
        isWaitAfter= StringUtils.isTrue(xml.getProperties().getProperty("iswaitafter"));
        isWaitResult= StringUtils.isTrue(xml.getProperties().getProperty("iswaitresult"));
        isNextInvoke= StringUtils.isTrue(xml.getProperties().getProperty("isnextinvoke"));

    }

    /**
     * 记录最外层的一个isRedo=ture的requestId，如果里面节点还有isRedo=true将不再记录
     * @param impl
     * @param m
     * @param args
     * @return
     * @throws Exception
     */
    @Override
    public Object beforeAction(Object impl, String m, Object[] args) throws Exception {
        if (((XMLObject) impl).isRedo()) {
            String s = (String)((XMLParameter)args[0]).getParameter("${requestId}");
            if(StringUtils.isNotBlank(s) && !ids.containsKey(s)) {
                XMLMakeup x = (XMLMakeup)args[1];
                if(null != x) {
                    ids.put(s,x.getId());
                }else{
                    ids.put(s,((XMLObject) impl).getXML().getId());
                }
            }
        }
        return null;
    }

    /**
     * 在发生异常的节点服务记录异常信息，下次重做时，从这个异常点继续往下执行。
     * @param impl
     * @param m
     * @param args
     * @param isInvoke
     * @param isSuccess
     * @param e
     * @param result
     * @return
     */
    @Override
    public Object afterAction(Object impl, String m, Object[] args, boolean isInvoke, boolean isSuccess, Throwable e, Object result) {
        try {
            String s = ((RequestParameters)args[0]).getRequestId();
            String id = null;
            if(null ==(XMLMakeup)args[1]){
                id=((XMLObject)impl).getXML().getId();
            }else{
                id = ((XMLMakeup)args[1]).getId();
            }
            if (StringUtils.isNotBlank(s) && impl instanceof XMLObject) {
                if(ids.containsKey(s) && (ids.get(s).equals(id))){
                    ids.remove(s);
                    if(null != e ) {
                        if(null == suspendTheRequest){
                            suspendTheRequest = (XMLDoObject)getObjectById("suspendTheRequest");
                        }
                        ((XMLParameter) args[0]).setSuspendXMlId(id);
                        ((XMLObject)impl).getXML().getProperties().put("nodeid",id);
                        suspendTheRequest.doThing((XMLParameter) args[0], (XMLMakeup) args[1]);
                    }

                }
            }
        }catch (Exception ex){

        }
        return null;
    }

    @Override
    public Object resultAction(Object impl, String m, Object[] args, Object result) {
        return null;
    }

    @Override
    public int getLevel() {
        return 0;
    }

    @Override
    public boolean isWaiteBefore() {
        return false;
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
        return false;
    }

    @Override
    public void setMethods(List<String> methods) {
        this.methods=methods;
    }

    @Override
    public List<String> getMethods() {
        return methods;
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return false;
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return null;
    }

    @Override
    public ResultCheck checkReturn(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return null;
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret, Exception e) throws Exception {
        return false;
    }
}
