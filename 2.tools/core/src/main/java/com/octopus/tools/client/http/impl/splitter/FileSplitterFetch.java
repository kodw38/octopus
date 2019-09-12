package com.octopus.tools.client.http.impl.splitter;

import com.octopus.tools.client.http.impl.MyX509TrustManager;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class FileSplitterFetch extends Thread  {
    String sURL;
    //File URLlong nStartPos;
    long nStartPos;
    long nEndPos;
    int nThreadID;
    FileAccessI fileAccessI;
    boolean bStop;
    public boolean bDownOver;
    //File Snippet Start Positionlong nEndPos;
    //File Snippet End Positionint nThreadID;
    //Thread's IDboolean bDownOver = false;
    //Downing is overboolean bStop = false;
    //Stop identicalFileAccessI fileAccessI = null;
    public FileSplitterFetch(String sURL, String sName, long nStart, long nEnd, int id) throws IOException {
        this.sURL = sURL;
        this.nStartPos = nStart;
        this.nEndPos = nEnd;
        nThreadID = id;
        fileAccessI = new FileAccessI(sName,nStartPos);
    }
    public void run(){
        while(nStartPos < nEndPos && !bStop){
            try{
                URL url = new URL(sURL);
                HttpURLConnection httpConnection = (HttpURLConnection)url.openConnection();
                if(sURL.startsWith("https")){
                    SSLContext sslcontext = SSLContext.getInstance("SSL","SunJSSE");
                    sslcontext.init(null, new TrustManager[]{new MyX509TrustManager()}, new java.security.SecureRandom());
                    ((HttpsURLConnection)httpConnection).setSSLSocketFactory(sslcontext.getSocketFactory());
                    httpConnection.setInstanceFollowRedirects(false);
                }
                httpConnection.setRequestProperty("User-Agent","NetFox");
                String sProperty = "bytes="+nStartPos+"-";
                httpConnection.setRequestProperty("RANGE",sProperty);
                Utility.log(sProperty);
                int code=httpConnection.getResponseCode();
                if (code==206) {
                    InputStream input = httpConnection.getInputStream();
                    //logResponseHead(httpConnection);
                    byte[] b = new byte[1024];
                    int nRead;
                    while ((nRead = input.read(b, 0, 1024)) > 0 && nStartPos < nEndPos && !bStop) {
                        nStartPos += fileAccessI.write(b, 0, nRead);
                        //if(nThreadID == 1)
                        // Utility.log("nStartPos = " + nStartPos + ", nEndPos = " + nEndPos);
                    }
                    Utility.log("Thread " + nThreadID + " is over!");
                    bDownOver = true;
                    //nPos = fileAccessI.write (b,0,nRead);
                }
            }catch(Exception e){

            }
        }
    }
    //打印回应的头信息
    public void logResponseHead(HttpURLConnection con){
        for(int i=1;;i++){
            String header=con.getHeaderFieldKey(i);
            if(header!=null)
                //responseHeaders.put(header,httpConnection.getHeaderField(header));
                Utility.log(header+" : "+con.getHeaderField(header));
            else
                break;
        }
    }
    public void splitterStop(){
        bStop = true;
    }
}
