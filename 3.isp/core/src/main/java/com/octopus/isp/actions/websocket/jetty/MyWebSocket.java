package com.octopus.isp.actions.websocket.jetty;

import com.octopus.isp.ds.RequestParameters;
import com.octopus.utils.cls.ClassUtils;
import com.octopus.utils.xml.auto.XMLDoObject;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

import javax.servlet.http.HttpSession;
import java.net.HttpCookie;
import java.util.List;

/**
 * Created by Administrator on 2018/10/11.
 */
public class MyWebSocket extends WebSocketAdapter {
    @Override
    public void onWebSocketConnect(Session session) {
        super.onWebSocketConnect(session);
        try{
            List<HttpCookie> ls = session.getUpgradeRequest().getCookies();
            String id=null;
            if(null != ls && ls.size()>0){
                for(HttpCookie c :ls){
                    if(c.getName().equals("JSESSIONID")){
                        id = c.getValue();
                    }
                }
            }
            if(null != id){
                com.octopus.isp.ds.Session se= RequestParameters.getSessionManager().getSessionById(null, id);
                if(null != se){
                    se.put("websocket-remote",getRemote());
                }
            }

        }catch (Exception e){

        }

        /*XMLDoObject obj = (XMLDoObject)session..getUserProperties().get("HttpSession")).getAttribute("WEB-Launcher");
        //session.getUserProperties().put("websocket-remote",remote);
        try {
            Object id = ClassUtils.getFieldValue(session, "httpSessionId", false);
            if(null != id && null != RequestParameters.getSessionManager().getSessionById(null,(String)id)) {
                RequestParameters.getSessionManager().getSessionById(null, (String)id).put("websocket-remote", session);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        super.onWebSocketClose(statusCode, reason);

        try {
            /*Object id = ClassUtils.getFieldValue(session, "httpSessionId", false);
            //log.info("close web socket connection "+id);
            if (null != id && null != RequestParameters.getSessionManager().getSessionById(null, (String) id)) {
                RequestParameters.getSessionManager().getSessionById(null, (String) id).remove("websocket-remote");
            }*/

        }catch (Exception e){

        }
    }

    @Override
    public void onWebSocketText(String message) {
        if (isNotConnected()) {
            return;
        }
        //System.out.println("000000000000000000000");
        if(isConnected()){

        //    System.out.println("11111");
        }
        try {             // echo the data back
                 //getBlockingConnection().write(message);
        /*    System.out.println(message);
            if(null != getRemote()) {
                getRemote().sendString("{\"cookie\":\"" + getSession().getUpgradeRequest().getHeader("Sec-WebSocket-Key")+"\"}");
            }*/
        }
        catch (Exception e)         {
            e.printStackTrace();
        }
    }
}

