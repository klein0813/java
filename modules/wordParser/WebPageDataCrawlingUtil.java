package XXX.util.wordParser;


import XXX.common.Constants;
import XXX.core.ConfigLoader;
import XXX.util.FileUtil;
import XXX.util.HttpsClientUtil;
import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.UUID;

import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

 
public class WebPageDataCrawlingUtil {
    
    private static final Log log = LogFactory.getLog(WebPageDataCrawlingUtil.class);

    private static String resumeName = null;

    /**
     * 此方法仅适用于“实习僧”，邮件简历附件地址对应的网页
     * 根据emailAttachUrl，获取简历加密字符串email_auth
     * 调用hrapi，解密获取dlv_id，dlv_id作为参数再次调用hrapi，即可获取简历
     * @param emailAttachUrl
     * @param attachmentFilePath
     * @return
     */
    public static String getResume(String emailAttachUrl) {
        String emailAuth = emailAttachUrl.substring(emailAttachUrl.lastIndexOf("/") + 1);
        String emailDecodeUrl = null;

        try {
            emailDecodeUrl = "https://hrapi.shixiseng.com/email_decode?email_auth=" + URLEncoder.encode(emailAuth, "utf-8");
        } catch (UnsupportedEncodingException e1) {
            log.warn("getResume | failed, encode url(utf-8), errorMessage: " + e1.getMessage());
            return null;
        }

        try {
            JSONObject emailDecode = HttpsClientUtil.httpGet(emailDecodeUrl);
            if (emailDecode != null) {

            	 if ("success".equals(emailDecode.get("msg"))) {
                    resumeName = null;
                    JSONObject data = (JSONObject) emailDecode.get("data");
                    String dlvId = data.get("dlv_id").toString();

                    String iframeUrl = "https://hrapi.shixiseng.com/api/v2/resume/action?dlv_id=" + dlvId + "&lan=chinese&only_data=true";
                    JSONObject object = HttpsClientUtil.httpGet(iframeUrl);
                    if ("success".equals(object.get("msg"))) {
                        String coreData = object.get("data").toString();
                        String docUrl = getDocumentUrl(coreData, dlvId);
                        InputStream dataStream = getResumeStream(docUrl);

                        String attachmentFilePath = new StringBuilder(ConfigLoader.getTempPath()).append(
                                 Constants.DIR_SEPARATOR).append(new Date().getTime()).append("_").append(resumeName).toString();
                        FileUtil.saveFile(dataStream, new File(attachmentFilePath));

                        return attachmentFilePath;
                    }
                }
            }

            log.info("getResume | failed, crawl resume, cause: failed to call shixiseng api");
            return null;
        } catch (Exception e) {
            log.warn("getResume | failed, crawl resume, errorMessage: " + e.getMessage());
            return null;
        }
    }

    /**
     * Just for ShiXiSeng
     * @param url
     * @return inputStream 
     * @throws FailingHttpStatusCodeException
     * @throws MalformedURLException
     * @throws IOException
     * 2019/8/13
     */
    private static InputStream getResumeStream(String url) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
        Page page = getPage(url);
        InputStream inputStream = page.getWebResponse().getContentAsStream();
        resumeName = page.getUrl().getPath();
        resumeName = resumeName.substring(resumeName.lastIndexOf("/") + 1);

        if ("action".equals(resumeName)) {//        This situation currently corresponds to html
            resumeName = UUID.randomUUID().toString().replaceAll("-", "") + ".html";
        }

        return inputStream;
    }

    /**
     * Just for ShiXiSeng
     * @param coreData
     * @param dlvId
     * @return
     */
    private static String getDocumentUrl(String coreData, String dlvId) {
        String suffix = coreData.substring(coreData.lastIndexOf("."), coreData.length());
        if (".docx".equalsIgnoreCase(suffix) || ".doc".equalsIgnoreCase(suffix)) { // “.doc”情况因无相应简历邮件，未测，可能存在问题(因实习僧网页是以Office Web Apps word展示docx，故而增加doc)
            return coreData.replaceAll("[\\s\\S]+src=", "https:");
        } else if (".pdf".equalsIgnoreCase(suffix)) {
            return "https:" + coreData;
        } else {
            return "https://hrapi.shixiseng.com/api/v2/resume/action?dlv_id=" + dlvId + "&lan=chinese&only_data=false";
        }
    }

    public static Page getPage(String url) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
    	 WebClient webClient = new WebClient(BrowserVersion.CHROME);
         webClient.getOptions().setUseInsecureSSL(true);
         webClient.getOptions().setJavaScriptEnabled(true);
         webClient.getOptions().setCssEnabled(false);
         webClient.getOptions().setRedirectEnabled(true);
         webClient.getOptions().setActiveXNative(false);
         webClient.setAjaxController(new NicelyResynchronizingAjaxController());
         webClient.getOptions().setThrowExceptionOnScriptError(false);
         webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
         webClient.getOptions().setTimeout(100000);
         webClient.waitForBackgroundJavaScript(100000);
         Page page = webClient.getPage(url);

         return page;
    }
}
