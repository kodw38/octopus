package com.octopus.utils.net.weixin;

public class Test {
    public static void main(String[] args) {
        WeChatMsgSend swx = new WeChatMsgSend();
        try {
            String token = swx.getToken("ww03c6444d8cb7e230","xkQ6biW8oFEo0ryVK1VOCm0ZVny0FG6Rnibxv9jwfWw");
            String postdata = swx.createpostdata("WangFeng", "text", 1000002, "content","这ye是一条测试信息");
            String resp = swx.post("utf-8", WeChatMsgSend.CONTENT_TYPE,(new WeChatUrlData()).getSendMessage_Url(), postdata, token);
            System.out.println("获取到的token======>" + token);
            System.out.println("请求数据======>" + postdata);
            System.out.println("发送微信的响应数据======>" + resp);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}
