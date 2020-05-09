package XXX.util.wordParser;

import java.io.IOException;
import java.net.URLEncoder;

import javax.annotation.PostConstruct;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import XXX.common.Constants;
import XXX.core.ConfigLoader;
import XXX.entity.po.business.ThirdpartyProperty;
import XXX.exception.RemoteApiException;
import XXX.service.ThirdPartyPropertyService;
import XXX.util.HttpsClientUtil;

@Component
public class BaiDuAIImageTextExtractionUtil {
    private static final Log log = LogFactory.getLog(BaiDuAIImageTextExtractionUtil.class);
    
    @Autowired
    public ThirdPartyPropertyService thirdPartyPropertyService;
    public static BaiDuAIImageTextExtractionUtil baiDuAIImageTextExtractionUtil;

    private static final String contentType = "x-www-form-urlencoded";
    private static final String accpect = "*/*";
    private static String[] header = new String[] {"Content-Type:application/x-www-form-urlencoded"};
    private static final String requestMethod = "POST";
    private static String extractionUrl = "https://aip.baidubce.com/rest/2.0/ocr/v1/accurate_basic?access_token=ACCESS_TOKENT";

    @PostConstruct
    public void init() {  
        baiDuAIImageTextExtractionUtil = this;  
    }

    public String[] imageTextExtraction(String imageData) {
        if (imageData == null) {
             log.warn("imageTextExtraction | enter, imageData is null");
             return null;
        }

        try {
            String requestParams = URLEncoder.encode("image", "UTF-8") + "=" + URLEncoder.encode(imageData, "UTF-8");
            String urlPath = extractionUrl.replace("ACCESS_TOKENT", getAccessToken(false));
            JSONObject json = HttpsClientUtil.httpRequest(urlPath, requestParams, header, contentType, accpect, requestMethod);
            if (json.has("error_code") && "111".equals(json.get("error_code").toString())) {
                urlPath = extractionUrl.replace("ACCESS_TOKENT", getAccessToken(true));
                json = HttpsClientUtil.httpRequest(urlPath, requestParams, header, contentType, accpect, requestMethod);
            }

            if (json.has("error_code") || ! json.has("words_result") || ! json.has("words_result_num")) {
                log.warn("imageTextExtraction | failed to extract text from image, response: " + json.toString());
                return null;
            }

            int wordsResultNum = Integer.parseInt(json.get("words_result_num").toString());
            String[] words = new String[wordsResultNum];
            JSONArray wordsResult = json.getJSONArray("words_result");
            for (int index = 0; index < wordsResultNum; index++) {
                words[index] = wordsResult.getJSONObject(index).get("words").toString();
            }

            return words;
        } catch (RemoteApiException e) {
            log.error("imageTextExtraction | RemoteApiException, exceptionMassage " + e.getMessage());
            return null;
        } catch (IOException e) {
            log.error("imageTextExtraction | IOException, exceptionMassage " + e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("imageTextExtraction | Exception, exceptionMassage " + e.getMessage());
            return null;
        }
    }

    private String getAccessToken(Boolean isExpired) throws RemoteApiException, IOException {
        long currentTimeStamp = System.currentTimeMillis() / 1000;
        ThirdpartyProperty property = thirdPartyPropertyService.getThirdPartyProperty(Constants.TPS_BAIDU_AI);

        if (! isExpired) {
            long expiresIn = Long.parseLong(property.getBaiduAiExpiresIn());
            long refreshAccessTokenTimestamp = Long.parseLong(property.getBaiduAiRefreshTimestamp());
            if (currentTimeStamp - refreshAccessTokenTimestamp < (expiresIn - 86400)) {
                return property.getBaiduAiAccessToken();
            }
        }

        String getTokentUrl = "https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials&client_id=CLIENT_ID&client_secret=CLIENT_SECRENT";
        getTokentUrl = getTokentUrl.replace("CLIENT_ID", ConfigLoader.getProperty("client_id"));
        getTokentUrl = getTokentUrl.replace("CLIENT_SECRENT", ConfigLoader.getProperty("client_secret"));
        JSONObject json = HttpsClientUtil.httpGet(getTokentUrl);
        if (json.has("error")) {
            log.error("getAccessToken | error, response: " + json.toString());
            return "";
        }

        property.setBaiduAiExpiresIn(json.get("expires_in").toString());
        property.setBaiduAiAccessToken(json.get("access_token").toString());
        property.setBaiduAiRefreshTimestamp(Long.toString(currentTimeStamp));
        thirdPartyPropertyService.batchUpdate(property);

        return property.getBaiduAiAccessToken();
    }

}
