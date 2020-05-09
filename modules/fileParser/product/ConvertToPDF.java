package XXX.parser.fileparser.product;

import XXX.parser.fileparser.utils.ConvertUtil;
import XXX.parser.fileparser.utils.TypeConstant;

public class ConvertToPDF implements Convert {

    @Override
    public void convert(String srcPath, String desPath) {
        ConvertUtil.convert(srcPath, desPath, TypeConstant.CONVERT_PDF);
    }

}
