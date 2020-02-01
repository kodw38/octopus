package com.octopus.tools.client.http.impl;

import javax.net.ssl.*;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.net.URL;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

/**
 * Created by admin on 2020/1/18.
 */
public class SSLCacertPem {
    public void testClientCertPEM() throws Exception {
        String requestURL = "https://mydomain/authtest";
        String pemPath = "C:/Users/myusername/Desktop/client.pem";

        HttpsURLConnection con;

        URL url = new URL(requestURL);
        con = (HttpsURLConnection) url.openConnection();
        //con.setSSLSocketFactory(getSocketFactoryPEM(pemPath));
        con.setRequestMethod("GET");
        con.setDoInput(true);
        con.setDoOutput(false);
        con.connect();

        String line;

        BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));

        while((line = reader.readLine()) != null) {
            System.out.println(line);
        }

        reader.close();
        con.disconnect();
    }

}
