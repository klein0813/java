package XXX.parser.fileparser.utils;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import XXX.util.MyX509TrustManager;
import XXX.util.FileUtil;

public class FileHelper {
    private static final Log log = LogFactory.getLog(FileHelper.class);

    public static void mkdirFiles(String filePath, String fileType) {
        File file = new File(filePath + "/" + fileType);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public static void saveToFile(String destUrl, String outPath) {
        FileOutputStream fos = null;
        BufferedInputStream bis = null;
        HttpURLConnection httpUrl = null;
        URL url = null;
        int BUFFER_SIZE = 1024;
        byte[] buf = new byte[BUFFER_SIZE];
        int size = 0;
        try {
            url = new URL(destUrl);
            httpUrl = (HttpURLConnection) url.openConnection();
            httpUrl.connect();
            bis = new BufferedInputStream(httpUrl.getInputStream());
            fos = new FileOutputStream(outPath);
            while ((size = bis.read(buf)) != -1) {
                fos.write(buf, 0, size);
            }
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (ClassCastException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
                if (bis != null) {
                    bis.close();
                }
                if (httpUrl != null) {
                    httpUrl.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static File getNetUrl(String openId, String netUrl) {
        // 判断http和https
        File file = null;
        if (netUrl.startsWith("https://")) {
            file = getNetUrlHttps(openId, netUrl);
        } else {
            file = getNetUrlHttp(openId, netUrl);
        }
        return file;
    }

    @SuppressWarnings("deprecation")
    public static File getNetUrlHttp(String openId, String netUrl) {
        // 对本地文件命名
        String fileName = StringUtils.getFilename(netUrl);
        File file = null;
        String fileType = netUrl.substring(
                netUrl.indexOf("response-content-type=")).replace(
                "response-content-type=", "");
        if (fileType.contains("png")) {
            fileType = ".png";
        } else if (fileType.contains("jpg") || fileType.contains("jpeg")) {
            fileType = ".jpg";
        } else if (fileType.contains("gif")) {
            fileType = ".gif";
        } else {
            fileType = ".png";
        }

        URL urlfile;
        InputStream inStream = null;
        OutputStream os = null;
        try {
            file = File.createTempFile(openId + "_",
                    URLEncoder.encode(fileType));
            // 下载
            urlfile = new URL(netUrl);
            inStream = urlfile.openStream();
            os = new FileOutputStream(file);

            int bytesRead = 0;
            byte[] buffer = new byte[8192];
            while ((bytesRead = inStream.read(buffer, 0, 8192)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        } catch (Exception e) {
            log.error("远程图片获取错误：" + netUrl);
            e.printStackTrace();
        } finally {
            try {
                if (null != os) {
                    os.close();
                }
                if (null != inStream) {
                    inStream.close();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return file;
    }

    public static File createResumeFile(String openId, String content) {
        File file = null;
        PrintStream printStream = null;
        FileOutputStream fos = null;
        try {
            file = File.createTempFile(openId + "_", ".html");
            fos = new FileOutputStream(file);
            printStream = new PrintStream(fos, true, "utf-8");
            printStream.print(content);
            printStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != fos) {
                    fos.close();
                }
                if (null != printStream) {
                    printStream.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return file;
    }

    /**
     * 下载文件到本地(支持https)
     *
     * @param fileUrl
     *            远程地址
     * @throws Exception
     */
    public static File getNetUrlHttps(String openId, String fileUrl) {
        // 对本地文件进行命名
        String fileName = StringUtils.getFilename(fileUrl);
        String filenameExtension = StringUtils.getFilenameExtension(fileUrl);
        String fileType = fileUrl.substring(
                fileUrl.indexOf("response-content-type=")).replace(
                "response-content-type=", "");
        File file = null;
        if (fileType.contains("png")) {
            fileType = ".png";
        } else if (fileType.contains("jpg") || fileType.contains("jpeg")) {
            fileType = ".jpg";
        } else if (fileType.contains("gif")) {
            fileType = ".gif";
        } else {
            fileType = ".png";
        }

        DataInputStream in = null;
        DataOutputStream out = null;
        try {
            file = File.createTempFile(openId + "_", fileType);

            SSLContext sslcontext = SSLContext.getInstance("SSL", "SunJSSE");
            sslcontext.init(null,
                    new TrustManager[] { new MyX509TrustManager() },
                    new java.security.SecureRandom());
            URL url = new URL(fileUrl);
            HostnameVerifier ignoreHostnameVerifier = new HostnameVerifier() {
                @Override
                public boolean verify(String s, SSLSession sslsession) {
                    log.warn("WARNING: Hostname is not matched for cert.");
                    return true;
                }
            };
            HttpsURLConnection
                    .setDefaultHostnameVerifier(ignoreHostnameVerifier);
            HttpsURLConnection.setDefaultSSLSocketFactory(sslcontext
                    .getSocketFactory());
            HttpsURLConnection urlCon = (HttpsURLConnection) url
                    .openConnection();
            urlCon.setConnectTimeout(60000);
            urlCon.setReadTimeout(60000);

            // 读文件流
            in = new DataInputStream(urlCon.getInputStream());
            out = new DataOutputStream(new FileOutputStream(file));
            byte[] buffer = new byte[2048];
            int count = 0;
            while ((count = in.read(buffer)) > 0) {
                out.write(buffer, 0, count);
            }
        } catch (Exception e) {
            log.error("远程图片获取错误：" + fileUrl);
            e.printStackTrace();
        } finally {
            try {
                if (null != out) {
                    out.close();
                }
                if (null != in) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }

        return file;
    }

    public static void main(String[] args) {
        File file = getNetUrl(
                "yssasf",
                "https://staging-statics.maiscrm.com/modules/mediaMaterial"
                        + "/yazBUgCsaPMJ8YyvgCsqCHS7LMLuB09ECI-r6wlGQW6Fk3q8fQ5hh3RZnxF83G1k?"
                        + "response-content-type=image%2Fpng");
    }

    public static File renameFile2AccessDir(String outputFileDir, String inputFilePath, String newFileName) {
    	File dir = new File(outputFileDir); 
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File tempFile = new File(outputFileDir + File.separator + newFileName);
        try {
            FileUtil.saveFile(new FileInputStream(inputFilePath), tempFile);

            if (tempFile.exists()) {
                return tempFile;
            }

            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
