package XXX.util.wordParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import XXX.core.ConfigLoader;

public class NameUtil {
    private static final Log log = LogFactory.getLog(NameUtil.class);

    public static String invalidName = "应届生%拉勾招聘%实习僧%简历%";
    public static String invalidStrReg = "^p-";
    private static String[] normalSeparator = new String[] {" ", "-", "_"};
    private static String fristNameSet = null;

    public static boolean includeFristName(String fristName) {
        fristName = fristName.concat("%");

        if (fristNameSet.contains(fristName)) {
            return true;
        }

        return false;
    }

    public static boolean isName(String str) {

         if (str !=null && str.length() >= 2) {
             if (str.matches("[^\u4e00-\u9fa5]")) {
                  return false;
             }

             if (str.length() > 5) {
                  return false;
             }

             if (fristNameSet == null) {
                 fristNameSet = getFristName();
             }
             if (fristNameSet == null) {
                 return false;
             }

             String fristName = str.substring(0, 1);
              if (includeFristName(fristName)) {
                   return true;
              }

              fristName = str.substring(0, 2);
              if (includeFristName(fristName) && str.length() >= 3) {
                   return true;
              }
         }

         return false;
    }

    public static String getNameFromString(String str) {
         if (str == null || str.isEmpty()) {
              return null;
         }
         str = str.replaceAll(invalidStrReg, "[");

         String[] fileNameSplit = null;
        /**
         * 获取命名风格
         * 依据：以style split fileName 数组最长时
         */
        for (String style : normalSeparator) {
            String reg = style + "|【|】|[|]";

            if (fileNameSplit != null) {
                String[] tempSplit = str.split(reg);
                if (fileNameSplit.length < tempSplit.length) {
                    fileNameSplit = tempSplit;
                }
            } else {
                fileNameSplit = str.split(reg);
            }
        }

        List<String> names = new ArrayList<String>();

        for (String splitUnit : fileNameSplit) {
            if (isName(splitUnit) && !invalidName.contains(splitUnit.concat("%"))) {
                names.add(splitUnit);
            }
        }

        /**
         * 若上述提取名字没有或者两个及以上，则提取失败
         */
        if (names.size() == 1) {
            return names.get(0);
        }

        return null;
    }

    private static String getFristName() {
        File file = new File(ConfigLoader.getWordParserFristName());
        BufferedReader reader = null;
        StringBuffer buf = new StringBuffer();
        try {
            reader = new BufferedReader(new FileReader(file));
            String tempStr;
            while ((tempStr = reader.readLine()) != null) {
                buf.append(tempStr);
            }
            reader.close();
            return buf.toString();
        } catch (IOException e) {
            log.error("getFristName | IOException, message: " + e.getMessage());
            return null;
        }
    }
}
