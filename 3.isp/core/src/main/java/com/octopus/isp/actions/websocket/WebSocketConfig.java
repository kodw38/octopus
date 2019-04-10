package com.octopus.isp.actions.websocket;

import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpSession;
import javax.websocket.Endpoint;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 依赖于tomcate 7以上的websocket-api.jar
 * User: wfgao_000
 * Date: 16-3-25
 * Time: 下午9:41
 */
public class WebSocketConfig  extends ServerEndpointConfig.Configurator implements ServerApplicationConfig {
    transient static Log log = LogFactory.getLog(WebSocketConfig.class);

    public void modifyHandshake(ServerEndpointConfig config,HandshakeRequest request,HandshakeResponse response)
    {
        HttpSession httpSession = (HttpSession)request.getHttpSession();
        config.getUserProperties().put("HttpSession",httpSession);
    }

    public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> set) {
        log.info("begin to register /websocket");
        final ServerEndpointConfig b = ServerEndpointConfig.Builder.create(WebSocketAdapter.class, "/websocket").configurator(this).build();
        return new HashSet<ServerEndpointConfig>() {
            {
                add(b);
            }
        };
    }

    public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> set) {
        return Collections.emptySet();
    }

}
