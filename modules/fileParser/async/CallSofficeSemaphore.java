package XXX.parser.fileparser.async;

import java.util.concurrent.Semaphore;

import XXX.core.ConfigLoader;
import XXX.parser.fileparser.ConvertFile;

public class CallSofficeSemaphore {

    /**
     * 信号量数值为1，因此每次只能一条线程执行，其他线程进入等待状态
     */
    static private final Semaphore semaphore = new Semaphore(1);

    static private String outputFileRootDir = ConfigLoader.DATA_CONVERT_RESUMES_PATH;

    public static String toConvert(String inputFilePath) {
        semaphore.acquireUninterruptibly(); // 使等待进入acquire（）方法的线程，不允许被中断
        String outputFilePath = doTask(inputFilePath);
        semaphore.release();

        return outputFilePath;
    }

    private static String doTask(String inputFilePath) {

        return ConvertFile.convertToPDF(outputFileRootDir, inputFilePath);
    }
}
