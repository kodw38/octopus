package com.octopus.isp.actions;

import com.octopus.isp.ds.RequestParameters;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cls.jcl.SingleClassLoader;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.XMLUtil;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import com.octopus.utils.xml.auto.logic.XMLLogic;

import java.util.Map;

/**
 * Created by robai on 2017/11/24.
 */
public class Redo extends XMLDoObject {
    public Redo(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(null != input){
            String id = (String)input.get("id");
            Object sv = input.get("sv");
            Object d = input.get("data");
            if(null !=d && d instanceof String){
                d = StringUtils.convert2MapJSONObject((String)d);
            }
            XMLMakeup xml = null;
            if(sv instanceof String){
                xml = XMLUtil.getDataFromString((String)sv);
            }

            RequestParameters p = new RequestParameters();
            if(null != d) {
                p.putAll((Map) d);
            }
            p.setSuspend(id);
            if(id.contains("|")) {
                String[] ss = id.split("\\|");
                if(ss.length>1) {
                    p.setSuspendXMlId(ss[1]);
                }
            }
            p.setRedoService();
            String[] ids = id.split("\\|");
            XMLDoObject obj = (XMLDoObject)getObjectById(ids[0]);
            if(null == obj)throw  new Exception("not find redo service by ["+id+"]");
            obj.doThing(p,xml);
            return p.getResult();
            /*p.setSuspend(id);
            String[] ids = id.split("\\|");
            // ids[0] srvname, ids[1] node name, ids[2] node seq,ids[3] requestid
            if(null != xml && null != ids && ids[0].equals(xml.getId()) && p.getRequestId().equals(ids[4])) {


             //@todo 1. create new running env , 2. rollback (trade xmllogic dochildren ʼfor do), 3. locate suspend point.

                XMLObject o = XMLObject.loadApplication(xml, null, false, false);
                if (o instanceof XMLDoObject) {
                    ((XMLDoObject) o).doThing(p, xml);
                    return p.getResult();
                }
            }*/

        }
        return null;
    }

    @Override
    public ResultCheck checkReturn(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return new ResultCheck(true,ret);
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
