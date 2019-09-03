package com.octopus.tools.client;

import com.octopus.utils.net.weixin.WeChatMsgSend;
import com.octopus.utils.net.weixin.WeChatUrlData;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;

import java.util.Map;

public class WeiXinCropClientObject extends XMLDoObject {
    public WeiXinCropClientObject(XMLMakeup xml, XMLObject parent, Object[] containers) throws Exception {
        super(xml, parent, containers);
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;
    }
    WeChatMsgSend swx = new WeChatMsgSend();
    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(null !=input){
            String corpid = (String)input.get("corpid");
            String corpsecret = (String)input.get("corpsecret");
            String toUser = (String)input.get("touser");
            String msgtype = (String)input.get("msgtype");
            int applicationid = (Integer)input.get("applicationid");
            String contentkey = (String)input.get("contentkey");
            String contentvalue = (String)input.get("contentvalue");
            try {
                String token = swx.getToken(corpid,corpsecret);
                String postdata = swx.createpostdata(toUser, msgtype, applicationid, contentkey,contentvalue);
                String resp = swx.post("utf-8", WeChatMsgSend.CONTENT_TYPE,(new WeChatUrlData()).getSendMessage_Url(), postdata, token);
                //System.out.println("获取到的token======>" + token);
                //System.out.println("请求数据======>" + postdata);
                //System.out.println("发送微信的响应数据======>" + resp);
                log.debug(resp);
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public ResultCheck checkReturn(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return new ResultCheck(true,ret);
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return true;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret, Exception e) throws Exception {
        return false;
    }
}
