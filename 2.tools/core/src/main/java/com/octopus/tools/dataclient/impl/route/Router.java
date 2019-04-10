package com.octopus.tools.dataclient.impl.route;

import com.octopus.tools.dataclient.IDataEngine;
import com.octopus.tools.dataclient.IDataRouter;
import com.octopus.utils.alone.ArrayUtils;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * User: Administrator
 * Date: 14-9-22
 * Time: 下午3:57
 */
public class Router extends XMLObject implements IDataRouter {

    public Router(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
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
    public IDataEngine[] getRouter(String dataSource,Object env) {
        XMLMakeup[] orms= getXML().find("orm");
        List<IDataEngine> ret = new ArrayList<IDataEngine>();
        if(null != orms){
            for(XMLMakeup x:orms){
                if(StringUtils.isNotBlank(x.getProperties().getProperty("datasources"))){
                    if(ArrayUtils.isInStringArray(x.getProperties().getProperty("datasources").split(","),dataSource)){
                        if(StringUtils.isNotBlank(x.getProperties().getProperty("engines"))){
                            String[] engs = x.getProperties().getProperty("engines").split(",");
                            Map map = (Map)getPropertyObject("engines");
                            if(null != map){
                                for(String e:engs){
                                    IDataEngine eng = (IDataEngine)map.get(e);
                                    if(null != eng){
                                        ret.add(eng);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if(ret.size()>0)
            return ret.toArray(new IDataEngine[0]);
        return null;
    }
}
