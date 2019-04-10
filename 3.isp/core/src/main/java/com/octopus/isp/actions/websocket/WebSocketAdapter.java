package com.octopus.isp.actions.websocket;

import com.octopus.isp.bridge.launchers.impl.WebPageFrameLauncher;
import com.octopus.isp.ds.RequestParameters;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.cls.ClassUtils;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpSession;
import javax.websocket.*;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * 依赖于tomcate 7以上的websocket-api.jar
 * User: wfgao_000
 * Date: 16-3-25
 * Time: 下午6:58
 */
public class WebSocketAdapter extends javax.websocket.Endpoint {
    transient static Log log = LogFactory.getLog(WebSocketAdapter.class);
    @Override
    public void onOpen(final Session session, final EndpointConfig endpointConfig) {
        log.info("web socket has opened");

        //final RemoteEndpoint.Basic remote = session.getBasicRemote();
        final XMLDoObject obj = (XMLDoObject)((HttpSession)session.getUserProperties().get("HttpSession")).getAttribute("WEB-Launcher");
        //session.getUserProperties().put("websocket-remote",remote);
        try {
            Object id = ClassUtils.getFieldValue(session,"httpSessionId",false);
            if(null != id && null != RequestParameters.getSessionManager().getSessionById(null,(String)id)) {
                RequestParameters.getSessionManager().getSessionById(null, (String)id).put("websocket-remote", session);
            }

            /*if(null != RequestParameters.getSessionManager().getSessionById(null,(session).getId())) {
                RequestParameters.getSessionManager().getSessionById(null, session.getId()).put("websocket-remote", remote);
            }*/
        } catch (Exception e) {
            e.printStackTrace();
        }

        session.addMessageHandler (new MessageHandler.Whole<String>() {
            @Override
            public void onMessage(String s) {
                Map m = StringUtils.convert2MapJSONObject(s);
                String actino = (String)m.get("action");
                if(StringUtils.isBlank(actino))
                    actino = (String)m.get("actions");
                if(StringUtils.isNotBlank(actino)){
                    if(null != obj){
                        XMLDoObject dom = (XMLDoObject)obj.getObjectById(actino);
                        XMLDoObject envobj = (XMLDoObject)obj.getPropertyObject("env");
                        XMLParameter env=null;
                        if(null != envobj){
                            try {
                                env = (XMLParameter)envobj.doSomeThing(null,null,null,null,null);
                            } catch (Exception e) {
                                env = new XMLParameter();
                                e.printStackTrace();
                            }
                        } else{
                            env = new XMLParameter();
                        }
                        try {
                            env.addParameter("websocket-remote",session);
                            env.addParameter("${input_data}",m);
                            dom.doCheckThing(null,env,(Map)m.get("input"),(Map)m.get("output"),(Map)m.get("config"),null);
                        } catch (Exception e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        }finally {
                            env.removeParameter("websocket-remote");
                        }
                    }

                }

            }

        });


    }
    public void onClose(Session session, CloseReason closeReason) {
        try {
            Object id = ClassUtils.getFieldValue(session, "httpSessionId", false);
            log.info("close web socket connection "+id);
            if (null != id && null != RequestParameters.getSessionManager().getSessionById(null, (String) id)) {
                RequestParameters.getSessionManager().getSessionById(null, (String) id).remove("websocket-remote");
            }

        }catch (Exception e){

        }

    }
    /*
    public void onError (Session session, Throwable throwable) {

    }*/
}
