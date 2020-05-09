package XXX.parser.fileparser;

import java.io.File;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import XXX.parser.fileparser.product.ConvertToPDF;
import XXX.parser.fileparser.utils.ConvertEngineer;
import XXX.parser.fileparser.utils.DateUtil;
import XXX.parser.fileparser.utils.FileHelper;
import XXX.util.FileUtil;
import XXX.util.Mht2HtmlUtil;

public class ConvertFile {

    private static final Log log = LogFactory.getLog(ConvertFile.class);

    public static final String getAccessPath(String outputFileRootDir, String inputFilePath, String format) {

        if (FileUtil.isDOC(inputFilePath) && (format == null || FileUtil.isDOC(format))) {
            return convertToPDF(outputFileRootDir, inputFilePath);
        } else {
            String date =  DateUtil.getYYYY() + "-"
                    + DateUtil.getMM() + "-" + DateUtil.getDD() ;
            String outputFileDir = outputFileRootDir + File.separator + date + File.separator + "pdf";
            String baseName = UUID.randomUUID().toString().replaceAll("-", "");

            String suffix;
            if (".html".equalsIgnoreCase(format)) {
                suffix = format;
            } else if (".mht".equalsIgnoreCase(format)) {
                outputFileDir = outputFileDir + File.separator + baseName;
                if (!new File(outputFileDir).mkdirs()) {
                    return null;
                }

                String destPath = outputFileDir + File.separator + baseName + ".html";
                Mht2HtmlUtil.mht2html(inputFilePath, destPath);

                if (new File(destPath).exists()) {
                    return File.separator + destPath;
                }

               return null;
            } else {
                suffix = inputFilePath.substring(inputFilePath.lastIndexOf("."));
            }

            File tempFile = FileHelper.renameFile2AccessDir(outputFileDir, inputFilePath, baseName.concat(suffix));
            if (tempFile == null) {
                log.error("getAccessPath | 重命名文件到可访问文件目录失败，");
                return null;
            }
            String outputFilePath = File.separator + outputFileDir + File.separator + baseName + suffix;

            return outputFilePath;
        }
    }

    /**
     * 文件转换入口
     */
    public static final String convertToPDF(String outputFileRootDir, String inputFilePath) {
        String date =  DateUtil.getYYYY() + "-"
                + DateUtil.getMM() + "-" + DateUtil.getDD() ;
        String outputFileDir = outputFileRootDir + File.separator + date + File.separator + "pdf";

        String suffix = inputFilePath.substring(inputFilePath.lastIndexOf("."));
        String baseName = UUID.randomUUID().toString().replaceAll("-", "");

        File tempFile = FileHelper.renameFile2AccessDir(outputFileDir, inputFilePath, baseName.concat(suffix));
        if (tempFile == null) {
            log.error("convertToPDF | 重命名文件到可访问文件目录失败，");
            return null;
        }

        String outputFilePath = outputFileDir + File.separator + baseName + ".pdf";
        File outFile = new File(outputFilePath);
        if (!outFile.exists()) {
            ConvertEngineer engineer = new ConvertEngineer(tempFile.getAbsolutePath(),
                    outputFileDir);
            engineer.convert(new ConvertToPDF());
        }
 
        tempFile.delete();
        if (!outFile.exists()) {
            return null;
        }

        return File.separator + outputFilePath;
    }
}
