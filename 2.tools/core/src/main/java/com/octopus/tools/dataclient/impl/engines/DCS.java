package com.octopus.tools.dataclient.impl.engines;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.thread.ds.InvokeTaskByObjName;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.XMLUtil;

import java.io.IOException;
import java.util.HashMap;

/**
 * User: Administrator
 * Date: 14-9-24
 * Time: 上午11:31
 */
public class DCS extends XMLObject implements IDCS{
    HashMap<String,DC> map = new HashMap();
    public DCS(XMLMakeup xml,XMLObject parent,Object[] containers) throws Exception {
        super(xml,parent,containers);
        String path = this.getXML().getProperties().getProperty("path");
        if(StringUtils.isNotBlank(path)){
            String[] ps = path.split(",");
            for(String p:ps){
                try {
                    XMLMakeup x = XMLUtil.getDataFromStream(this.getClass().getClassLoader().getResourceAsStream(path));
                    this.getXML().addChild(x);
                    XMLMakeup[] dcs = x.getChild("dc");
                    if(null != dcs){
                        String id;
                        for(XMLMakeup d:dcs){
                            id = d.getProperties().getProperty("id");
                            if(StringUtils.isNotBlank(id) && !map.containsKey(id)){
                                DC dc = new DC(d);
                                map.put(id,dc);
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
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
    public DC getDC(String opid) {
        return map.get(opid);
    }
}
