package com.octopus.isp.bridge.launchers;

import com.octopus.utils.xml.XMLObject;

/**
 * User: wfgao_000
 * Date: 15-11-22
 * Time: 下午6:48
 */
public class MainSimpleLauncher {

    public static void main(String[] args){
        try{
            long l = System.currentTimeMillis();
            XMLObject.loadApplication("classpath:"+args[0], null,true,true);
            System.out.println("has launch system.");

        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
