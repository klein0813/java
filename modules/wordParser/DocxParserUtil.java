package XXX.util.wordParser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import XXX.util.FileUtil;
import XXX.util.ZipUtil;

/**
 * Docx is compressed by a certain structure of xml
 * @author klein.zhou
 *
 */
public class DocxParserUtil {

    private static final Log log = LogFactory.getLog(DocxParserUtil.class);

    private static StringBuffer strBuf = null;

    private static String DefaultEncoding = "UTF-8";
    private static String baseUri = "http://augmentum.com.cn";   //useless here

    /**
     * not best way
     * @param docxFilePath
     * @return
     */
	public static String parser(String docxFilePath) {
        log.info("begin, parse docx, path: " + docxFilePath);
        strBuf = new StringBuffer();
        try {
        	File file = new File(docxFilePath);

            Document mainXml = null;
            
    		try {
    			ZipFile docxFile =new ZipFile(file);

    	        Document contentTypesXml = getDocumentFromZip(docxFile, "[Content_Types].xml");
    	        String mainXMLPath = getMainXmlPath(contentTypesXml);
    	        mainXml = getDocumentFromZip(docxFile, mainXMLPath);

    		} catch (ZipException ze) {
    	        String baseName = UUID.randomUUID().toString().replaceAll("-", "");
    	        String directoryPath = "data/temp" + File.separator + baseName;
                File directory = new File("data/temp/", baseName);
                directory.mkdirs();
    			if (ZipUtil.unZipByCommonsCompress(file, directoryPath)) {
    				Document contentTypesXml = Jsoup.parse(new File(directoryPath + File.separator + "[Content_Types].xml"), DefaultEncoding, baseUri);
        			String mainXMLPath = getMainXmlPath(contentTypesXml);
        			mainXml = Jsoup.parse(new File(directoryPath + File.separator + mainXMLPath), DefaultEncoding, baseUri);
    			}

    			file.delete();
    			ZipUtil.zipDirByCommonsCompress(directory, docxFilePath);

    			FileUtil.deleteDir(directory);
    		}

    		if (mainXml == null) {
    			log.error("failed, parse docx, mainXml is null");
    		}

    		DataAnaly(mainXml);
	        String str = strBuf.toString();

	        return str;
        } 
		catch (Exception e) {
	        log.error("failed, parse docx, errorMessage: " + e.getMessage());
			return null;
		}  
	}

	/**
	 * Remove all nodes which tag is "mc:Choice".<br>
	 * The case that the blank line exists is that textElement is blank.<br>
	 * Jsoup can remove the front and end space of text like trim, a defect does not make xml:space="preserve" used well.<br>
	 * @param mainXml
	 */
	private static void DataAnaly(Document mainXml) {
		mainXml.getElementsByTag("mc:Choice").remove();
		Elements paragraphElements = mainXml.getElementsByTag("w:p");
        for (int i = 0; i < paragraphElements.size(); i++) {

        	Element paragraphElement = paragraphElements.get(i);
        	if (paragraphElement.getElementsByTag("w:p").size() > 1) {
        		paragraphElement.getElementsByTag("w:p").remove();	
        	}

        	Elements pPrElements = paragraphElement.getElementsByTag("w:pPr");
        	if (pPrElements.size() > 0) {
            	Elements indElement = pPrElements.get(0).getElementsByTag("w:ind");
            	if (indElement.size() > 0) {
            		strBuf.append(" ");
            	}
        	}

        	Elements rElements = paragraphElement.getElementsByTag("w:r");
        	if (rElements.size() == 0) {
        		continue;
        	}

        	for (int j = 0; j < rElements.size(); j++) {

        		if (rElements.get(j).getElementsByTag("w:r").size() > 1) {
        			continue;
        		}

            	Elements tabElement = rElements.get(j).getElementsByTag("w:tab");
            	if (tabElement.size() > 0) {
            		strBuf.append(" ");
            		continue;
            	}

            	Elements textElement = rElements.get(j).getElementsByTag("w:t");
            	if (textElement.size() > 0) {
            		String temp = "";
        			if (textElement.get(0).hasAttr("xml:space")) {
        				temp = " ";
        				strBuf.append(temp);
        			}

            		if (textElement.hasText()) {
            			String text = textElement.text();
            			strBuf.append(text).append(temp);
            		}
            	}
        	}

        	strBuf.append("\r\n");
		}
	}

	private static String getMainXmlPath(Document contentTypesXml) {
        Elements elements = contentTypesXml.getElementsByTag("Override");
        String mainXMLPath = null;
        for (int i = 0; i < elements.size(); i++) {
			if ("application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml".equals(elements.get(i).attr("ContentType"))) {
				mainXMLPath = elements.get(i).attr("PartName");
				if (mainXMLPath.startsWith("/")) {
					return mainXMLPath.substring(1);
				}
				return mainXMLPath;
			}
		}

        log.error("failed, parse docx, docx文档结构错误: main xml未获取成功");
        return null;
	}

	/**
	 * Jsoup.parse(InputStream in, String charsetName, String baseUri); baseUri helps relative url.<br>
	 * @param docxFile
	 * @param filePath
	 * @return
	 * @throws IOException
	 */
	private static Document getDocumentFromZip(ZipFile docxFile, String filePath) throws IOException {
		ZipEntry zipEntry =docxFile.getEntry(filePath);
        InputStream fileInputStream = docxFile.getInputStream(zipEntry);
        Document document = Jsoup.parse(fileInputStream, DefaultEncoding, baseUri);

        return document;
	}
}
