package com.octopus.utils.si;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

/**
 * User: wangfeng2
 * Date: 14-8-12
 * Time: 下午6:18
 */
public class AgentMain {
    private static Instrumentation inst;
    public static void agentmain(String agentArgs, Instrumentation inst)
            throws ClassNotFoundException, UnmodifiableClassException,
            InterruptedException {
        AgentMain.inst=inst;
    }
    public static Instrumentation getInstrumentation(){
        return inst;
    }
}
