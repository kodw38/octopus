package com.octopus.isp.bridge.launchers.out;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLObject;

/**
 * User: Administrator
 * Date: 15-1-5
 * Time: 下午9:54
 */
public class MainLauncher {
    public static void main(String[] args){
        try {
            String bridgePath = args[0];
            if(StringUtils.isBlank(bridgePath))
                bridgePath = "file:E:/my work/asiainfo/ISP/4.isp/models/base/trunk/core/src/main/java/com/octopus/isp/bridge/bridge.xml";
            XMLObject.loadApplication(bridgePath, null,true,true);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
