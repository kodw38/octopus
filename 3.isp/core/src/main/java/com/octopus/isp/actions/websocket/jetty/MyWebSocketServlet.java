package com.octopus.isp.actions.websocket.jetty;

import com.octopus.utils.xml.auto.XMLDoObject;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

/**
 * Created by Administrator on 2018/10/11.
 */
public class MyWebSocketServlet extends WebSocketServlet
{

    @Override
    public void configure(WebSocketServletFactory webSocketServletFactory) {
        webSocketServletFactory.register(MyWebSocket.class);
    }
}
