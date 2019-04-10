package com.octopus.tools.client;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.xml.XMLMakeup;
import com.octopus.utils.xml.XMLObject;
import com.octopus.utils.xml.auto.ResultCheck;
import com.octopus.utils.xml.auto.XMLDoObject;
import com.octopus.utils.xml.auto.XMLParameter;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

/**
 * User: wfgao_000
 * Date: 2016/11/6
 * Time: 19:48
 */
public class MailClientObject extends XMLDoObject {
    public MailClientObject(XMLMakeup xml, XMLObject parent,Object[] containers) throws Exception {
        super(xml, parent,containers);
    }

    public static void sendMail(String host,String fromMail,String user,String password,
                                String toMail,
                                String mailTitle,
                                String mailContent) throws Exception{
        Properties props=new Properties();//可以加载一个配置文件
//使用smtp：简单邮件传输协议
        props.put("mail.smtp.host",host);//存储发送邮件服务器的信息
        props.put("mail.smtp.auth","true");//同时通过验证

        Session session=Session.getInstance(props);//根据属性新建一个邮件会话
//session.setDebug(true);//有他会打印一些调试信息。

        MimeMessage message=new MimeMessage(session);//由邮件会话新建一个消息对象
        message.setFrom(new InternetAddress(fromMail));//设置发件人的地址
        String[] ms = toMail.split("\\;");
        for(String m:ms) {
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(m));//设置收件人,并设置其接收类型为TO
        }
        message.setSubject(mailTitle);//设置标题
//设置信件内容
//message.setText(mailContent);//发送纯文本邮件todo
        message.setContent(mailContent,"text/html;charset=gbk");//发送HTML邮件，内容样式比较丰富
        message.setSentDate(new Date());//设置发信时间
        message.saveChanges();//存储邮件信息

//发送邮件
        Transport transport=session.getTransport("smtp");
//        Transport transport = session.getTransport();
        transport.connect(user,password);
        transport.sendMessage(message,message.getAllRecipients());//发送邮件,其中第二个参数是所有已设好的收件人地址
        transport.close();
    }

    public static void main(String[] args)throws Exception{
        sendMail("smtp.126.com","kodw38@126.com","kodw38","8ik*IK",
                "kodw38@126.com",
                "JavaMail测试邮件",
                "<a>html元素</a>：<b>邮件内容</b>");
    }

    @Override
    public Object doSomeThing(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        if(null != input){
            String host = (String)input.get("host");
            String sender = (String)input.get("sender");
            String user = (String)input.get("user");
            String pwd = (String)input.get("pwd");
            String receiver = (String)input.get("receiver");
            String title = (String)input.get("title");
            String content = (String)input.get("content");
            if(StringUtils.isNotBlank(host) && StringUtils.isNotBlank(sender)
                    && StringUtils.isNotBlank(user) && StringUtils.isNotBlank(pwd)
                    && StringUtils.isNotBlank(receiver) && StringUtils.isNotBlank(title) && StringUtils.isNotBlank(content))
            sendMail(host,sender,user,pwd,receiver,title,content);
        }
        return null;
    }

    @Override
    public void doInitial() throws Exception {

    }

    @Override
    public boolean checkInput(String xmlid, XMLParameter env, Map input, Map output, Map config) throws Exception {
        return true;
    }

    @Override
    public ResultCheck checkReturn(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return new ResultCheck(true,ret);
    }

    @Override
    public boolean commit(String xmlid, XMLParameter env, Map input, Map output, Map config, Object ret) throws Exception {
        return false;
    }

    @Override
    public boolean rollback(String xmlid, XMLParameter env, Map input, Map output, Map config,Object ret,Exception e) throws Exception {
        return false;
    }
}
