package com.octopus.isp.actions;

import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.logic.XMLLogic;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.util.HashMap;

/**
 * Created by Administrator on 2018/3/4.
 */
public class SystemSignalHandler extends XMLLogic implements SignalHandler {
    static transient Log log = LogFactory.getLog(SystemSignalHandler.class);

    public SystemSignalHandler(XMLMakeup xml, XMLObject parent, Object[] containers) throws Exception {
        super(xml, parent, containers);
    }

    @Override
    public void handle(Signal signal) {
        log.info("when be killed and destroy sub app :");
        try {
            doThing(getEmptyParameter(),getXML());
        } catch (Exception e) {
            log.error("clear system when be killed error", e);
        }finally {
            System.exit(0);
        }
    }

    @Override
    public void doInitial() throws Exception {
        log.info("register signal TERM handel "+this.getClass().getName());
        Signal.handle(new Signal("TERM"), this);  // kill -15 common kill
        //Signal.handle(new Signal("INT"), this);   // Ctrl c
        //Signal.handle(new Signal("KILL"), this);  // kill -9  no Support
        //Signal.handle(new Signal("USR1"), this);   // kill -10
        //Signal.handle(new Signal("USR2"), this);   // kill -12
    }

}