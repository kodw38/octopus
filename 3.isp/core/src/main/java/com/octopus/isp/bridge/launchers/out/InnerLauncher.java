package com.octopus.isp.bridge.launchers.out;

import com.octopus.isp.bridge.IBridge;
import com.octopus.isp.ds.RequestParameters;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLObject;

/**
 * User: wfgao_000
 * Date: 15-10-13
 * Time: 下午9:41
 */
public class InnerLauncher {
    IBridge bridge;
    public InnerLauncher()throws Exception{
        String bridgePath = System.getProperty("isp");
        if(StringUtils.isBlank(bridgePath))
            throw new Exception("please set isp jvm parameter") ;
        bridge = (IBridge) XMLObject.loadApplication(bridgePath, null,true,true);
    }

    public Object invoke(RequestParameters parameters){
        return bridge.evaluate(parameters);
    }
}
