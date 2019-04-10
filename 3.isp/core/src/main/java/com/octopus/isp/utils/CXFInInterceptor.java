package com.octopus.isp.utils;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.exception.Logger;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.eclipse.jetty.server.Request;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceContext;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

/**
 * Created by kod on 2017/4/16.
 */
public class CXFInInterceptor extends AbstractSoapInterceptor {
    static transient Log log = LogFactory.getLog(CXFInInterceptor.class);
    public CXFInInterceptor(){
        super(Phase.RECEIVE);
    }


    @Override
    public void handleMessage(SoapMessage soapMessage) throws Fault {

        /*QName qname=new QName("RequestSOAPHeader");
        if(null != qname) {
            Header header = soapMessage.getHeader(qname);
            if(null != header) {
                Element elementOrderCredential = (Element) header.getObject();
                if(null != elementOrderCredential) {
                    NodeList user = elementOrderCredential.getElementsByTagName("username");
                    NodeList pad = elementOrderCredential.getElementsByTagName("password");
                    String userName = "";
                    if (null != user) {
                        userName = user.item(0).getTextContent();
                    }
                    String password = "";
                    if (null != pad) {
                        password = pad.item(0).getTextContent();
                    }
                    System.out.println(userName + ":" + password);
                }
            }
        }*/
       /* InputStream is = soapMessage.getContent(InputStream.class);
        if(is != null) {
            try {
                soapMessage.setContent(InputStream.class, is);
                log.debug(IOUtils.toString(is));
            }catch (Exception e){

            }
        }*/


        try {
            if(log.isDebugEnabled()) {
                InputStream in = soapMessage.getContent(InputStream.class);
                String s = IOUtils.toString(in);
                if(log.isDebugEnabled()) {
                    HttpServletRequest request = (HttpServletRequest)soapMessage.get(AbstractHTTPDestination.HTTP_REQUEST);
                    if(null != request && log.isDebugEnabled()){
                        log.debug(Logger.getString(request)+ "\n" + StringUtils.formatXml(s));
                    }
                    //log.debug(soapMessage.get("org.apache.cxf.request.url") + "\n" + StringUtils.formatXml(s));
                }
                if (in != null)
                    soapMessage.setContent(InputStream.class, new ByteArrayInputStream(s.getBytes()));
            }

        } catch (Exception e) {


        }
        /*String spPassword="wdwsb";
        String spName="wdw";
        QName qname=new QName("RequestSOAPHeader");
        Document doc= DOMUtils.createDocument();
        //自定义节点
        Element spId=doc.createElement("tns:spId");
        spId.setTextContent(spName);
        //自定义节点
        Element spPass=doc.createElement("tns:spPassword");
        spPass.setTextContent(spPassword);

        Element root=doc.createElementNS(nameURI, "tns:RequestSOAPHeader");
        root.appendChild(spId);
        root.appendChild(spPass);

        SoapHeader head=new SoapHeader(qname,root);
        List<Header> headers=soapMessage.getHeaders();
        headers.add(head);*/
    }

}
