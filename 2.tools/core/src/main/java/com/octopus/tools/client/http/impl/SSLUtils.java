package com.octopus.tools.client.http.impl;

import com.octopus.utils.alone.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import javax.net.ssl.*;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * Created by admin on 2020/1/18.
 */
public class SSLUtils {
    static String PASSWORD="<password>";
    //---------------------------------------------------------------------------------------
    public static HttpClient getSSLSocktetBidirectional(String TYPE,String CA_PATH,String CRT_PATH,String KEY_PATH) throws Exception {
        // CA certificate is used to authenticate server
        CertificateFactory cAf = CertificateFactory.getInstance("X.509");
        FileInputStream caIn = new FileInputStream(CA_PATH);
        X509Certificate ca = (X509Certificate) cAf.generateCertificate(caIn);
        KeyStore caKs = KeyStore.getInstance("JKS");
        caKs.load(null, null);
        caKs.setCertificateEntry("ca-certificate", ca);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
        tmf.init(caKs);

        // client key and certificates are sent to server so it can authenticate us
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        FileInputStream crtIn = new FileInputStream(CRT_PATH);
        X509Certificate caCert = (X509Certificate) cf.generateCertificate(crtIn);
        crtIn.close();

        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        ks.setCertificateEntry("certificate", caCert);
        ks.setKeyEntry("private-key", getPrivateKey( KEY_PATH), PASSWORD.toCharArray(), new java.security.cert.Certificate[] { caCert });
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("PKIX");
        kmf.init(ks, PASSWORD.toCharArray());

        // finally, create SSL socket factory
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

        SSLConnectionSocketFactory ssf= new SSLConnectionSocketFactory(context,new String[]{"TLSv1","TLSv1.1","TLSv1.2"},null,new HttpsHostnameVerifier ());
        //注册
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create().register("http", PlainConnectionSocketFactory.INSTANCE).register("https", ssf).build();
        //池化管理
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(connManager).build();
        return httpClient;
    }
    private static PrivateKey getPrivateKey(String path) throws Exception {
        //byte[] buffer = Base64.getDecoder().decode(getPem(path));
        byte[] buffer = Base64.getMimeDecoder().decode(getPem(path));

        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(buffer);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);

    }

    private static String getPem(String path) throws Exception {
        FileInputStream fin = new FileInputStream(path);
        BufferedReader br = new BufferedReader(new InputStreamReader(fin));
        String readLine = null;
        StringBuilder sb = new StringBuilder();
        while ((readLine = br.readLine()) != null) {
            if (readLine.charAt(0) == '-') {
                continue;
            } else {
                sb.append(readLine);
                sb.append('\r');
            }
        }
        fin.close();
        return sb.toString();
    }

    //-----------------------------------------------------------------------------------------------------------------
    public static HttpClient getSocketFactoryPEM(String pemPath,String client_key,String client_crt) throws Exception {
        byte[] certAndKey = fileToBytes(pemPath);

        byte[] certBytes = parseDERFromPEM(certAndKey, "-----BEGIN CERTIFICATE-----", "-----END CERTIFICATE-----");
        byte[] keyBytes = parseDERFromPEM(certAndKey, "-----BEGIN PRIVATE KEY-----", "-----END PRIVATE KEY-----");

        X509Certificate cert = null;
        if(null != certBytes)
            cert = generateCertificateFromDER(certBytes);

        RSAPrivateKey key  = null;
        if(null != keyBytes)
            key = generatePrivateKeyFromDER(keyBytes);
        if(null == key){
            byte[] k = fileToBytes(client_key);
            keyBytes = parseDERFromPEM(k, "-----BEGIN RSA PRIVATE KEY-----", "-----END RSA PRIVATE KEY-----");
            key = generatePrivateKeyFromDER(keyBytes);
        }
        if(null != keyBytes)
            key = generatePrivateKeyFromDER(keyBytes);

        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(null);
        keystore.setCertificateEntry("cert-alias", cert);
        keystore.setKeyEntry("key-alias", key, "<password>".toCharArray(), new Certificate[] {cert});

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keystore, "<password>".toCharArray());

        KeyManager[] km = kmf.getKeyManagers();

        SSLContext context = SSLContext.getInstance("TLS");
        if(StringUtils.isNotBlank(client_crt) && StringUtils.isNotBlank(client_key)){
            TrustManagerFactory trustFactory = TrustManagerFactory.getInstance("SunX509");
            KeyStore tsstore = KeyStore.getInstance("JKS");
            tsstore.load(null);
            X509Certificate trustCert = generateCertificateFromDER(certBytes);
            tsstore.setCertificateEntry("cert-alias", trustCert);

            tsstore.setKeyEntry("key-alias", key, "<password>".toCharArray(), new Certificate[] {trustCert});
            trustFactory.init(tsstore);

            TrustManager[] trustManagers = trustFactory.getTrustManagers();
            context.init(km, trustManagers, null);
        }else{
            context.init(km, null, null);
        }

        //设置规则限制
        SSLConnectionSocketFactory ssf = new SSLConnectionSocketFactory(context,new String[]{"TLSv1","TLSv1.1","TLSv1.2"},null,new HttpsHostnameVerifier ());
        //注册
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create().register("http", PlainConnectionSocketFactory.INSTANCE).register("https", ssf).build();
        //池化管理
        PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(connManager).build();
        return httpClient;
    }
    private static byte[] fileToBytes(String filePath) {
        byte[] buffer = null;
        File file = new File(filePath);

        FileInputStream fis = null;
        ByteArrayOutputStream bos = null;

        try {
            fis = new FileInputStream(file);
            bos = new ByteArrayOutputStream();

            byte[] b = new byte[1024];

            int n;

            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }

            buffer = bos.toByteArray();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (null != bos) {
                    bos.close();
                }
            } catch (IOException ex) {
            } finally{
                try {
                    if(null!=fis){
                        fis.close();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

        return buffer;
    }

    protected static byte[] parseDERFromPEM(byte[] pem, String beginDelimiter, String endDelimiter) {
        String data = new String(pem);
        String[] tokens = data.split(beginDelimiter);
        if(tokens.length==2) {
            tokens = tokens[1].split(endDelimiter);
            return DatatypeConverter.parseBase64Binary(tokens[0]);
        }else {
            return null;
        }
    }

    protected static RSAPrivateKey generatePrivateKeyFromDER(byte[] keyBytes) throws InvalidKeySpecException, NoSuchAlgorithmException {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);

        KeyFactory factory = KeyFactory.getInstance("RSA");

        return (RSAPrivateKey)factory.generatePrivate(spec);
    }

    protected static X509Certificate generateCertificateFromDER(byte[] certBytes) throws CertificateException {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");

        return (X509Certificate)factory.generateCertificate(new ByteArrayInputStream(certBytes));
    }

    //------------------------------TSL-------------------------------
    public static HttpClient getSSLHttpClient(String keyStorePath,String keyStorePassword ,String keyStoreType ,String trustStorePath ,String trustStorePassword,String trustStoreType)throws Exception{
        KeyManagerFactory keyFactory = null;
        try {
            keyFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            KeyStore keystore = KeyStore.getInstance(keyStoreType);
            keystore.load(new FileInputStream(new File(keyStorePath)), null);
            keyFactory.init(keystore, keyStorePassword.toCharArray());

            KeyManager[] keyManagers = keyFactory.getKeyManagers();

            TrustManagerFactory trustFactory = TrustManagerFactory.getInstance("SunX509");
            KeyStore tsstore = KeyStore.getInstance(trustStoreType);
            tsstore.load(new FileInputStream(new File(trustStorePath)), trustStorePassword.toCharArray());
            trustFactory.init(tsstore);

            TrustManager[] trustManagers = trustFactory.getTrustManagers();

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, trustManagers, null);

            //设置规则限制
            SSLConnectionSocketFactory ssf = new SSLConnectionSocketFactory(sslContext,new String[]{"TLSv1","TLSv1.1","TLSv1.2"},null,new HttpsHostnameVerifier ());
            //注册
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create().register("http", PlainConnectionSocketFactory.INSTANCE).register("https", ssf).build();
            //池化管理
            PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(connManager).build();
            return httpClient;
        }catch (Exception e){
            throw e;
        }
    }


}
