package com.octopus.isp.utils;

import com.octopus.utils.alone.StringUtils;
import com.octopus.utils.exception.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

/**
 * Created by kod on 2017/4/16.
 */
public class CXFOutInterceptor extends AbstractSoapInterceptor {
    static transient Log log = LogFactory.getLog(CXFOutInterceptor.class);

    public CXFOutInterceptor() {
        super(Phase.PRE_STREAM);
    }

    @Override
    public void handleMessage(SoapMessage message) throws Fault {
        try {
            if(log.isDebugEnabled()) {
                OutputStream os = message.getContent(OutputStream.class);
                CachedStream cs = new CachedStream();
                message.setContent(OutputStream.class, cs);
                message.getInterceptorChain().doIntercept(message);
                CachedOutputStream csnew = (CachedOutputStream) message.getContent(OutputStream.class);
                InputStream in = csnew.getInputStream();
                String xml = IOUtils.toString(in);
                //这里对xml做处理，处理完后同理，写回流中
                if(log.isDebugEnabled() && log.isDebugEnabled()) {
                    log.debug("["+Logger.getDateFormat().format(new Date())+"] ["+Thread.currentThread().getName()+"] \n"+ StringUtils.formatXml(xml));
                }
                IOUtils.copy(new ByteArrayInputStream(xml.getBytes()), os);
                cs.close();
                os.flush();
                message.setContent(OutputStream.class, os);

            }

        } catch (Exception e) {

        }
    }

    private class CachedStream extends CachedOutputStream {
        public CachedStream() {
            super();
        }
        protected void doFlush() throws IOException {
            currentStream.flush();
        }
        protected void doClose() throws IOException {}
        protected void onWrite() throws IOException {}
    }

}
