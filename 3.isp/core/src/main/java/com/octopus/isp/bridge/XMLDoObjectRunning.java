package com.octopus.isp.bridge;

import com.octopus.isp.ds.RequestParameters;
import com.octopus.utils.xml.auto.XMLDoObject;

/**
 * User: Administrator
 * Date: 14-9-3
 * Time: 下午6:19
 */
public class XMLDoObjectRunning implements Runnable {
    XMLDoObject object;
    RequestParameters parameters;
    public XMLDoObjectRunning(XMLDoObject object,RequestParameters parameters){
        this.object=object;
        this.parameters=parameters;
    }
    @Override
    public void run() {
        try{
            object.doSomeThing(null,parameters,null,null,null);
        }catch (Exception e){
            parameters.setError(true);
            parameters.setException(e);
        }
    }
}
