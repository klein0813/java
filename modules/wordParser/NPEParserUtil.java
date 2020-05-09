package XXX.util.wordParser;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.mail.MessagingException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poiII.hwpf.HWPFDocument;
import org.dom4j.Attribute;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import XXX.core.ConfigLoader;
import XXX.exception.UnhandleException;
import XXX.util.Mht2HtmlUtil;
import XXX.util.StringUtil;

/**
 * The class is to get name/phone/email from resume file.<br>
 * @author klein.zhou
 *
 */
public class NPEParserUtil {
    private static final Log log = LogFactory.getLog(NPEParserUtil.class);

    private static String nameReg = "(NAME|姓(\\s)*名(\\s)*)(:)*(：)*(\\x20)*[\\u4e00-\\u9fa5]+";
    private static String phoneReg = "1[3456789]\\d[-\\x20]*\\d{4}[-\\x20]*\\d{4}";
    private static String emailReg = "([A-Za-z0-9_\\-\\.])+@([A-Za-z0-9_\\-\\.])+\\.([A-Za-z]{2,4})";

    private static String suffix;

    public static Map<String, String> parser(String filePath) {
        suffix = filePath.substring(filePath.lastIndexOf("."));
        Map<String, String> resultMap = new HashMap<String, String>();

        try {
            if (".doc".equalsIgnoreCase(suffix)) {
                resultMap = analyDoc(filePath);
            } else if (".docx".equalsIgnoreCase(suffix)) {
                resultMap = analyDocx(filePath);
            } else if (".pdf".equalsIgnoreCase(suffix)) {
                resultMap = analyPDF(filePath);
            } else if (".html".equalsIgnoreCase(suffix)) {
                resultMap = analyHtml(filePath);
            } else if (suffix.matches("(?i)(.png|.jpg|.jpeg|.bmp)")) {
                resultMap = analyImage(filePath);
            } else {
                log.error("parser | the format of file is Unsupported: " + suffix);
                resultMap = null;
            }
            log.info("parser | end, parse document, result: " + resultMap.toString());

            return resultMap;
        } catch (Exception e) {
            log.error("parser | failed, parse document, errorMessage: " + e.getMessage());
            return null;
        }
    }

    /**
     * 如果文字上面有蒙层，会解析失败
     * @param filePath
     * @return
     * @throws InvalidPasswordException
     * @throws IOException
     */
    private static Map<String, String> analyPDF(String filePath) throws InvalidPasswordException, IOException {
        File file = new File(filePath);
        PDFParser parser = new PDFParser(new RandomAccessFile(file,"r"));
        parser.parse();
        PDDocument pdfdocument = parser.getPDDocument();
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(false);
        String data = stripper.getText(pdfdocument);

        Map<String, String> resultMap;
        if (data.matches("(\r*\n*\r*)*")) {
//            data = getTextFromPDFImage(pdfdocument);
            resultMap = getFromPDFImage(pdfdocument, filePath);
        } else {
            resultMap = analyData(filePath, data);
        }

        pdfdocument.close();

        return resultMap;
    }

    private static Map<String, String> analyDocx(String filePath) throws IOException {
        String data = DocxParserUtil.parser(filePath);

        return analyData(filePath, data);
    }

    /**
     * @param filePath
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     * @throws MessagingException 
     */
    private static Map<String, String> analyDoc(String filePath) throws IOException, MessagingException {
        String data = null;
        String format = ".doc";
        try {
            FileInputStream in = new FileInputStream(filePath);
            HWPFDocument doc = new HWPFDocument(in);
            data = doc.getDocumentText();
            doc.close();
        } catch (IllegalArgumentException e) {
            //目前伪doc有html, mht, docx三种形式，均已处理
            // handle HTML
            data = getHtmlText(filePath);
            if (data.contains("[Content_Types].xml") && data.contains("word/document.xml")) {            // handle docx
               data = DocxParserUtil.parser(filePath);
               format = ".docx";
            } else if (data != null && data.matches("[\\s\\S]*(?i)(MIME-Version:)[\\s\\S]*")) {            // handle MHT

                String htmlText =  Mht2HtmlUtil.getHtmlText(filePath);
                Document doc = Jsoup.parse(htmlText);
                data = doc.text();
                format = ".mht";
            } else {
                format = ".html";
            }
        }

        Map<String, String> resultMap = analyData(filePath, data);
        resultMap.put("format", format);

        return resultMap;
    }

    private static Map<String, String> analyHtml(String filePath) throws IOException {
        String data = getHtmlText(filePath);

        return analyData(filePath, data);
    }

    /**
     * note: the words in image must are positive, not scribbled<br>
     * the method calls baiduAI API to extract words<br>
     * @param filePath
     * @return
     * @throws IOException
     */
    private static Map<String, String> analyImage(String filePath) throws IOException {
        BufferedImage bufferImage =ImageIO.read(new FileInputStream(filePath));
        ByteArrayOutputStream os = new ByteArrayOutputStream(); 
        ImageIO.write(bufferImage, suffix.replace(".", ""), os);
        StringBuffer words = getImageText(os.toByteArray());
        String data = words.toString();

        return analyData(filePath, data);
    }

    /**
     * Get name from fileName by split fileName
     * @param fileName
     * @return
     */
    public static String analyFileName(String fileName) {
        if (suffix != null) {
            fileName = fileName.substring(fileName.lastIndexOf("\\") + 1).replaceAll(suffix, "");
        }

        return NameUtil.getNameFromString(fileName);
    }

    public static Map<String, String> analyData(String filePath, String str) {
        Map<String, String> resultMap = new HashMap<String, String>();
        str = filterNoisyData(str);
        if (str == null || str.trim().length() == 0) {
            return resultMap;
        }

        String email = getStringByReg(str, emailReg);
        String name = getStringByReg(str, nameReg);

        String tempStr = str;
        if (email != null) {
            tempStr = tempStr.replaceAll(email, "");
        } else {
            tempStr = tempStr.replaceAll(" ", "");
            email = getStringByReg(tempStr, emailReg);
        }

        String phone = getStringByReg(tempStr, phoneReg);
        if (phone != null) {
            phone = phone.replaceAll("-|\\x20", "");
        }

        if (name != null) {
            String[] temps = name.split("：|\\x20|:");
            name = temps[temps.length - 1];
            if (name.length() == 1) {
                name = null;
            }

        } 

        if (name == null) {
            String nameFromAnalysisfileName = analyFileName(filePath);
//            if (nameFromAnalysisfileName != null && str.contains(nameFromAnalysisfileName)) {
            if (nameFromAnalysisfileName != null) {
                name = nameFromAnalysisfileName;
            }
        }

        if (name == null) {
            SAXReader reader = new SAXReader();

            try {
                InputStream in = new FileInputStream(new File(ConfigLoader.getWordParseNameRegulars()));
                org.dom4j.Document doc = reader.read(in);
                String nodesPath = "//regulars-mapping/mapping-field";
                @SuppressWarnings("unchecked")
                List<Node> nodes = doc.selectNodes(nodesPath);

                for (int nodeIndex = 1; nodeIndex <= nodes.size(); nodeIndex++) {
                    Attribute matchReg = (Attribute) doc.selectSingleNode(nodesPath + "[" + nodeIndex + "]/@matchReg");
                    Attribute replaceRge = (Attribute) doc.selectSingleNode(nodesPath + "[" + nodeIndex + "]/@replaceRge");
                    name = getStringByReg(str, matchReg.getStringValue());
                    if (name != null) {
                        name = name.replaceAll(replaceRge.getStringValue(), "").replaceAll(" ", "");
                        if (NameUtil.isName(name)) {
                            break;
                        }
                        name = null;
                    }
                }
            } catch (Exception e) {
                log.error("analyData | Exception, message: " + e.getMessage());
                throw new UnhandleException(e, "Exception when handle name-regulars.xml.");
            }
        }

        resultMap.put("name", name);
        resultMap.put("phone", phone);
        resultMap.put("email", email);

        return resultMap;
    }

    /**
     * Filter ID card number and blank Chinese placeholders.<br>
     * @param data
     * @return
     */
    private static String filterNoisyData(String data) {
        data = handleSpecialCharacters(data);
        /*
         * 100000 19000101 000[0Xx] ~ 999999 20991231 999[9Xx]
         * 100000 000101 000 ~ 999999 991231 999
         */
        String regIdNumber = "([1-9]\\d{5}(19|20)\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx])|([1-9]\\d{5}\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{2})";
        data = data.replaceAll(regIdNumber, " ");

        data = data.replaceAll("\\u3000", " ");

        return data;
    }

    private static String getStringByReg(String str, String reg) {
        if (str == null || reg == null) {
            return null;
        }
        Matcher matcher = Pattern.compile(reg).matcher(str);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    /**
     * Replace special characters (values of ASCII < 31 or = 127 or = 160) with space (32).<br>
     * Replace CJK radical character(exclude 0x2e\d{2}).<br>
     * 127 is "DEL", 160 is non-breaking space<br>
     * @param str
     * @return
     */
    private static String handleSpecialCharacters(String str) {
        if (str != null) {
            char[] charArray = str.toCharArray();
            for (int i = 0; i < charArray.length; i ++) {
                char c = charArray[i];
                if (c < 31 || c == 127 || c == 160) {
                    charArray[i] = 32;
                } else if (c >= 0x2f00 && c <= 0x2fd5) {
                    c = CJKRadicals.getUnifiedIdeograph(c);
                    charArray[i] = c == 0xffff ? charArray[i] : c;
                }
            }

            return String.valueOf(charArray);
        }

        return null;
    }

    private static String getHtmlText(String filePath) throws IOException {
        File file = new File(filePath);
        String charset = StringUtil.getHtmlCharset(file);
        Document doc = Jsoup.parse(file, charset);
        String data = doc.text();

        return data;
    }

    @SuppressWarnings("unused")
    private static String getTextFromPDFImage(PDDocument pdfdocument) throws IOException {

        if(null != pdfdocument){
            PDPageTree pages = pdfdocument.getPages();
            StringBuffer buf = new StringBuffer();

            PDPageTree analyPage = new PDPageTree();
            if (pages.getCount() > 4) {
                analyPage.add(pages.get(0));
                analyPage.add(pages.get(1));
                analyPage.add(pages.get(pages.getCount() - 1));
            } else {
                analyPage = pages;
            }

            for(PDPage page : analyPage){
                Iterable<COSName> objectNames = page.getResources().getXObjectNames();

                for(COSName imageObjectName : objectNames){
                    if(page.getResources().isImageXObject(imageObjectName)){

                        PDImageXObject image = (PDImageXObject) page.getResources().getXObject(imageObjectName);
                        BufferedImage bufferImage = image.getImage();
                        ByteArrayOutputStream os = new ByteArrayOutputStream(); 
                        ImageIO.write(bufferImage, image.getSuffix(), os);
                        buf.append(getImageText(os.toByteArray()));
                    }
                }
            }
            return buf.toString();
        }
        return null;
    }

    private static Map<String, String> getFromPDFImage(PDDocument pdfdocument, String fileName) throws IOException {
        Map<String, String> resultMap = new HashMap<String, String>();
        if(null != pdfdocument){
            PDPageTree pages = pdfdocument.getPages();

            PDPageTree analyPage = new PDPageTree();
            if (pages.getCount() > 4) {
                analyPage.add(pages.get(0));
                analyPage.add(pages.get(1));
                analyPage.add(pages.get(pages.getCount() - 1));
            } else {
                analyPage = pages;
            }

            for(PDPage page : analyPage){
                Iterable<COSName> objectNames = page.getResources().getXObjectNames();

                for(COSName imageObjectName : objectNames){
                    if(page.getResources().isImageXObject(imageObjectName)){

                        PDImageXObject image = (PDImageXObject) page.getResources().getXObject(imageObjectName);
                        BufferedImage bufferImage = image.getImage();
                        ByteArrayOutputStream os = new ByteArrayOutputStream(); 
                        ImageIO.write(bufferImage, image.getSuffix(), os);
                        StringBuffer text = getImageText(os.toByteArray());
                        Map<String, String> result = analyData(fileName, text.toString());
                        if (result != null) {
                            if (result.get("name") != null) {
                                resultMap.put("name", result.get("name"));
                            }
                            if (result.get("phone") != null) {
                                resultMap.put("phone", result.get("phone"));
                            }
                            if (result.get("email") != null) {
                                resultMap.put("email", result.get("email"));
                            }
                            if (resultMap.get("name") != null && resultMap.get("phone") != null && resultMap.get("email") != null) {
                                return resultMap;
                            }
                        }
                    }
                }
            }
        }
        return resultMap;
    }

    /**
     * note: 图片方向不正确，获取的数据也将不正确
     * @param imagebytes
     * @return
     */
    private static StringBuffer getImageText(byte[] imagebytes) {
        StringBuffer buf = new StringBuffer();
        String str = Base64.encodeBase64String(imagebytes);

        if (str == null || str.trim().length() == 0) {
            return buf.append("");
        }
        String[] words = BaiDuAIImageTextExtractionUtil.baiDuAIImageTextExtractionUtil.imageTextExtraction(str);
        if (words == null) {
            return buf.append("");
        }
        for (String word : words) {
            buf.append(word).append(" ");
        }

        return buf;
    }
}