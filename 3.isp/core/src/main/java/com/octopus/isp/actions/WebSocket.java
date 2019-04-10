package com.octopus.isp.actions;

import com.octopus.isp.bridge.launchers.impl.pageframe.SessionManager;
import com.octopus.isp.ds.RequestParameters;
import com.octopus.isp.ds.Session;
import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.thread.ExecutorUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;
import org.eclipse.jetty.websocket.api.RemoteEndpoint;

import java.util.List;
import java.util.Map;

/**
 * User: wfgao_000
 * Date: 2016/10/1
 * Time: 19:20
 */
public class WebSocket extends XMLDoObject {
    public WebSocket(XMLMakeup xml, XMLObject parent, Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    @Override
    public Object doSomeThing(String s, XMLParameter xmlParameter, Map input, Map output, Map config) throws Exception {
        if(null != xmlParameter ){
            Object user = input.get("user");
            if(null == user){
                Session session = (Session)xmlParameter.get("${session}");
                if(null != session){
                    user = session.getUserName();
                }
            }
            if(StringUtils.isNotBlank(user)) {
                SessionManager sm = (SessionManager)xmlParameter.getParameter("${sessionManager}");
                if (null != sm) {
                    List<Session> ss =  sm.getSessions();
                    if(null != ss){
                        for(Session si:ss) {
                            if(null != si && si.getUserName().equals(user)){
                                Object remote = si.get("websocket-remote");
                                if (null != remote) {
                                    if (remote instanceof RemoteEndpoint) {
                                        ((RemoteEndpoint) remote).sendString((String) input.get("data"));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        /*javax.websocket.Session remote = (javax.websocket.Session)xmlParameter.getParameter("websocket-remote");
        if(null == remote) {
            SessionManager sm = (SessionManager)xmlParameter.getParameter("${sessionManager}");
            if (null != sm) {
                List<Session> ss =  sm.getSessions();
                if(null != ss){
                    for(Session si:ss){
                        remote = (javax.websocket.Session)si.get("websocket-remote");
                        if(null != remote)
                            synchronized (remote) {
                                try {
                                    if(remote.isOpen()) {
                                        remote.getBasicRemote().sendText((String) input.get("data"));
                                        //ExecutorUtils.synWork(remote, "sendText", new Class[]{String.class}, new Object[]{input.get("data")});
                                    }else{
                                        remote.close();
                                        si.remove("websocket-remote");
                                    }
                                }catch (Exception e){
                                    remote.close();
                                    si.remove("websocket-remote");
                                }
                            }
                    }
                }
            }
        }*/
        return null;
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String s, XMLParameter xmlParameter, Map map, Map map2, Map map3) throws Exception {
        return true;
    }

    @Override
    public ResultCheck checkReturn(String s, XMLParameter xmlParameter, Map map, Map map2, Map map3, Object ret) throws Exception {
        return new ResultCheck(true,ret);
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String s, XMLParameter xmlParameter, Map map, Map map2, Map map3,Object ret,Exception e) throws Exception {
        return false;
    }
}
