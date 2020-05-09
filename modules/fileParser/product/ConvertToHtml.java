package XXX.parser.fileparser.product;

import XXX.parser.fileparser.utils.ConvertUtil;
import XXX.parser.fileparser.utils.TypeConstant;

public class ConvertToHtml implements Convert {

    @Override
    public void convert(String srcPath, String destPath) {
        ConvertUtil.convert(srcPath, destPath, TypeConstant.CONVERT_HTML);
    }

}
