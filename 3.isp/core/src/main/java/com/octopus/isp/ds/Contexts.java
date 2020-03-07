package com.octopus.isp.ds;

import com.octopus.tools.i18n.II18N;
import com.octopus.tools.i18n.impl.I18N;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.exception.ISPException;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * User: Administrator
 * Date: 14-9-3
 * Time: 上午8:53
 */
public class Contexts extends XMLObject {
    I18N i18n;
    public Contexts(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
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


    public Context getContext(RequestParameters requestData)throws ISPException {
        //find a context by user requestData or sessionUserData
        XMLMakeup xs=null;
        XMLMakeup[] dx = getXML().getByProperty("default","true");
        if(null !=requestData && null !=requestData.getSession()){
            Map i8 = (Map)requestData.getSession().get("I18N");
            xs = getOwnXml(getXML().getChild("context"),i8);
        }
        if(null == xs){
            xs = (XMLMakeup) ArrayUtils.getFirst(getXML().getChild("context"));
            //to generator a Context
        }
        Context c = (Context) XMLParameter.newInstance(xs, Context.class, requestData.getRequestData(), true, this);
        Context def = null;

        if(null != dx && dx.length>0 && null != dx[0])
            def = (Context) XMLParameter.newInstance(dx[0], Context.class, requestData.getRequestData(), true, this);
        c.setI18n(i18n);
        if(null != def)
        c.setDefaultContext(def);
        c.setRootXMLMakeup(xs);
        return c;
    }
    XMLMakeup getOwnXml(XMLMakeup[] xs, Map i8 ){
        if(null != xs){
            if(null != i8) {
                for (XMLMakeup x : xs) {
                    Iterator its = i8.keySet().iterator();
                    int n = i8.size();
                    while(its.hasNext()) {
                        String k = (String)its.next();
                        String l = x.getFirstCurChildText("property", "key", k);
                        if(StringUtils.isNotBlank(k) && StringUtils.isNotBlank(l) && i8.get(k).equals(l)){
                            n--;
                        }
                    }
                    if(n==0) {
                        return x;
                    }
                }
            }
        }
        return null;
    }

}
