package com.octopus.isp.utils;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.w3c.dom.NodeList;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;

/**
 * Created by robai on 2017/12/20.
 */
public class ReadSoapHeader extends AbstractPhaseInterceptor<SoapMessage> {
    private SAAJInInterceptor saa = new SAAJInInterceptor();
    public ReadSoapHeader() {
        super(Phase.PRE_PROTOCOL);
        getAfter().add(SAAJInInterceptor.class.getName());

    }
    public void handleMessage(SoapMessage message) throws Fault {
        SOAPMessage mess = message.getContent(SOAPMessage.class);
        if (mess == null) {
            saa.handleMessage(message);
            mess = message.getContent(SOAPMessage.class);
        }
        SOAPHeader head = null;
        try {
            head = mess.getSOAPHeader();
        } catch (SOAPException e) {
            e.printStackTrace();
        }

        if (head == null) {
            return;
        }
        try {
            //读取自定义的节点
            NodeList nodes = head.getElementsByTagName("tns:username");
            NodeList nodepass = head.getElementsByTagName("tns:password");
            //获取节点值，简单认证
            if (nodes.item(0).getTextContent().equals("wdw")) {
                if (nodepass.item(0).getTextContent().equals("wdwsb")) {
                    System.out.println("认证成功");
                }

            } else {
                SOAPException soapExc = new SOAPException("认证错误");
                throw new Fault(soapExc);
            }
        } catch (Exception e) {
            SOAPException soapExc = new SOAPException("认证错误");
            throw new Fault(soapExc);
        }

    }
}
