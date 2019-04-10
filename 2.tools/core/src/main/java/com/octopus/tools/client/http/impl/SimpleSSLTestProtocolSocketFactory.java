package com.octopus.tools.client.http.impl;

import com.octopus.utils.alone.StringUtils;
import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ControllerThreadSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.rmi.UnknownHostException;
import java.security.KeyStore;

/**
 * Created by Administrator on 2018/5/31.
 */
public class SimpleSSLTestProtocolSocketFactory implements SecureProtocolSocketFactory {
    private static final Log LOG = LogFactory.getLog(SimpleSSLTestProtocolSocketFactory.class);
    private static SSLContext SSLCONTEXT = null;
    String keyStoreType="PKCS12",keyStroeFile,password,managePassword;
    public SimpleSSLTestProtocolSocketFactory(String keyStoreType,String keyStoreFile,String password,String managePassword) {
        super();
        if(StringUtils.isNotBlank(keyStoreType)) {
            this.keyStoreType = keyStoreType;
        }
        this.keyStroeFile=keyStoreFile;
        this.password=password;
        this.managePassword=managePassword;
    }

    private SSLContext createSSLContext() {
        try {
            KeyStore keystore = KeyStore.getInstance(keyStoreType);
            FileInputStream ksInstream = new FileInputStream(new File(keyStroeFile));

            //KeyStore trustStore = KeyStore.getInstance(keyStoreType);
            //FileInputStream trustInstream = new FileInputStream(new File(keyStroeFile));
            try {
                keystore.load(ksInstream, password.toCharArray());
                //trustStore.load(trustInstream, password.toCharArray());
            } finally {
                if (ksInstream != null)
                    ksInstream.close();
                //if (trustInstream!=null)
                //    trustInstream.close();
            }
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            //TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keystore, password.toCharArray());
            //trustManagerFactory.init(trustStore);
            KeyManager[] keymanagers=keyManagerFactory.getKeyManagers();
            //TrustManager[] trustmanagers = trustManagerFactory.getTrustManagers();
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            //sslcontext.init(keymanagers, trustmanagers, null);
            sslcontext.init(keymanagers, null, null);
            return sslcontext;
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
            throw new IllegalStateException(ex.getMessage());
        }

    }

    private SSLContext getSSLContext() {
        if (SSLCONTEXT == null) {
            SSLCONTEXT = createSSLContext();
        }
        return SSLCONTEXT;
    }

    public Socket createSocket(final String host, final int port,
                               final InetAddress localAddress, final int localPort,
                               final HttpConnectionParams params) throws IOException,
            UnknownHostException, ConnectTimeoutException {
        if (params == null) {
            throw new IllegalArgumentException("Parameters may not be null");
        }
        int timeout = params.getConnectionTimeout();
        if (timeout == 0) {
            return createSocket(host, port, localAddress, localPort);
        } else {
            // To be eventually deprecated when migrated to Java 1.4 or above
            return ControllerThreadSocketFactory.createSocket(this, host, port,
                    localAddress, localPort, timeout);
        }
    }

    public Socket createSocket(String host, int port, InetAddress clientHost,
                               int clientPort) throws IOException, UnknownHostException {
        return getSSLContext().getSocketFactory().createSocket(host, port,
                clientHost, clientPort);
    }

    public Socket createSocket(String host, int port) throws IOException,
            UnknownHostException {
        return getSSLContext().getSocketFactory().createSocket(host, port);
    }

    public Socket createSocket(Socket socket, String host, int port,
                               boolean autoClose) throws IOException, UnknownHostException {
        return getSSLContext().getSocketFactory().createSocket(socket, host,
                port, autoClose);
    }
}
