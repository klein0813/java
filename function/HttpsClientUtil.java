package XXX.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import XXX.exception.RemoteApiException;

public class HttpsClientUtil {
    
    private static final Log log = LogFactory.getLog(HttpsClientUtil.class);
    
    private static final String contentType = "application/json";
    private static final String accpect = "*/*";
    private static final int retry = 3;
    private static AtomicLong requestCount = new AtomicLong();
    private static final String charSet = "UTF-8";

    /**
     * content-type类型为json方式发送post请求
     * 
     * @param urlPath
     * @param data
     * @param charSet
     * @return
     * @throws RemoteApiException 
     */
    public static JSONObject httpPost(String urlPath, String data) throws RemoteApiException {
        JSONObject result = sendRequest(urlPath, data, "POST", retry);
        return result;
    } 
    
    public static JSONObject httpGet(String urlPath) throws RemoteApiException {
        JSONObject result = sendRequest(urlPath, null, "GET", retry);
        return result;
    } 
    
    public static JSONObject httpGet(String urlPath, String data) throws RemoteApiException {
        JSONObject result = sendRequest(urlPath, data, "GET", retry);
        return result;
    } 
    
    private static JSONObject sendRequest(String urlPath, String data, String requestMethod, int retry) throws RemoteApiException {
        JSONObject json = new JSONObject();  
        if (retry >= 1) {
            requestCount.incrementAndGet();
            log.info("retry count is: " + retry + " call api and the url is " + urlPath);
            try {
                json = httpRequest(urlPath, data, null, contentType, accpect, requestMethod);
            } catch (RemoteApiException e) {
                sendRequest(urlPath, data, requestMethod, retry - 1);
                log.error("Failed to send request." + e.getMessage());
            }
        } else {
            throw new RemoteApiException("Failed to execute https request.");
        }
        return json;
    }

    public static JSONObject httpRequest(String urlPath, String data,
            String[] header, String contentType, String accpect, String requestMethod) throws RemoteApiException {
        JSONObject json = new JSONObject();  
        URL url = null;
        HttpsURLConnection conn = null;
        OutputStreamWriter out = null;
        BufferedReader reader = null;
        try {
            SSLContext sslContext=SSLContext.getInstance("SSL");
            TrustManager[] tm={new MyX509TrustManager()};
            sslContext.init(null, tm, new java.security.SecureRandom());;
            SSLSocketFactory ssf=sslContext.getSocketFactory();
            url=new URL(urlPath);
            conn=(HttpsURLConnection)url.openConnection();
            conn.setHostnameVerifier(new HttpsClientUtil().new TrustAnyHostnameVerifier());
            conn.setSSLSocketFactory(ssf);
            conn.setConnectTimeout(200000);
            
            if (header != null) {
                for (int i = 0; i < header.length; i++) {
                    String[] content = header[i].split(":");
                    conn .setRequestProperty(content[0], content[1]);
                }
            }

            conn.setRequestMethod(requestMethod);
            conn.setRequestProperty("Content-Type", contentType);
            if (null != accpect) {
                conn.setRequestProperty("Accpect", accpect);
            }
            
            if(requestMethod.toUpperCase().equals("POST")){  
                conn.setDoOutput(true);  
                conn.setDoInput(true);  
                conn.setUseCaches(false);  
                out = new OutputStreamWriter(conn.getOutputStream(), charSet);    //utf-8编码
                out.append(data);
                out.flush();
                out.close();
            }else{
                conn.connect();  
            }  

            if (data != null) {
                out = new OutputStreamWriter(conn.getOutputStream(),
                        charSet);   // utf-8编码
                out.append(data);
                out.flush();
                out.close();
            }
            int code = conn.getResponseCode();
            
            if (code == 200) {
                //读取响应
                InputStream is = conn.getInputStream();
                reader = new BufferedReader(new InputStreamReader(is, charSet));
                String line = reader.readLine();
                StringBuilder builder = new StringBuilder();
                while (line != null) {
                    builder.append(line);
                    line = reader.readLine();
                }
                String result = builder.toString();
                log.info("result is " + result);
                json = JSONObject.fromObject(result);
            } else if (code == 500) {
                log.error("Got error message: " + conn.getResponseMessage());
            } else {
                throw new RemoteApiException("Failed to get result when sending request.");
            }
        } catch (Exception e) {
            throw new RemoteApiException(e);
        } finally {
            url = null;
            if (conn != null) {
                conn.disconnect();
            }
            try {
                if (out != null) {
                    out.close();
                }
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                throw new RemoteApiException(e);
            }
        }
        return json;
    }

    public static JSONObject postByDataMap(String urlPath, Map<String, String> data) throws RemoteApiException {
        log.info("call api and the url is " + urlPath);
        JSONObject json = new JSONObject();  
        URL url = null;
        HttpsURLConnection httpurlconnection = null;
        StringBuilder out = new StringBuilder();
        BufferedReader reader = null;
        try {
              url = new URL(urlPath);
              httpurlconnection = (HttpsURLConnection) url.openConnection();
              httpurlconnection.setSSLSocketFactory(new TLSSocketConnectionFactory());
              httpurlconnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
              httpurlconnection.setRequestMethod("POST");
              httpurlconnection.setConnectTimeout(200000);
              
              httpurlconnection.setDoOutput(true);  
              httpurlconnection.setDoInput(true);  
              httpurlconnection.setUseCaches(false);
              
              for (String key : data.keySet()) {
                  if(out.length() != 0){
                      out.append("&");
                  }
                  out.append(key).append("=").append(URLEncoder.encode(data.get(key), charSet));
              }
  
              DataOutputStream dos=new DataOutputStream(httpurlconnection.getOutputStream());
              dos.writeBytes(out.toString());
              dos.flush();
              dos.close();
              
              //读取响应
    
              int code = httpurlconnection.getResponseCode();
            
              if (code == 200) {
                  InputStream is = httpurlconnection.getInputStream();
                  reader = new BufferedReader(new InputStreamReader(is, charSet));
                  String line = reader.readLine();
                  StringBuilder builder = new StringBuilder();
                  while (line != null) {
                      builder.append(line);
                      line = reader.readLine();
                  }
                  String result = builder.toString();
                  log.info("result is " + result);
                  json = JSONObject.fromObject(result);  
              } else {
                  throw new RemoteApiException("Failed to get result when sending request.");
              }
        } catch (Exception e) {
            throw new RemoteApiException(e);
        } finally {
              url = null;
              if (httpurlconnection != null) {
                  httpurlconnection.disconnect();
              }
              try {
                  if (reader != null) {
                      reader.close();
                  }
              } catch (IOException e) {
                  throw new RemoteApiException(e);
              }
        }
        return json;
    }

    public class TrustAnyHostnameVerifier implements HostnameVerifier {
        public boolean verify(String hostname, SSLSession session) {
         // 直接返回true
         return true;
        }
    }
}