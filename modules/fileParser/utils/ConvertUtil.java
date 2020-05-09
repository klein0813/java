package XXX.parser.fileparser.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import XXX.util.StringUtil;

public class ConvertUtil {
    private static final Log log = LogFactory.getLog(ConvertUtil.class);
    private static final float TIMEOUT = 60;  // 秒
    private static final int SLEEP_PERIOD = 100; // 毫秒

    public static void convert(String srcPath, String desPath, String type) {
        String command = "";
        String osName = System.getProperty("os.name");
        if (osName.contains("Windows")) {
            command = "soffice --headless --convert-to " + type + " " + srcPath
                    + " --outdir " + desPath;
            exec(command);
        }
    }

    /**
     * 返回值表示调用外部可执行程序是否正常结束，不等价于文件是否转换成功
     * @param command
     * @return
     */
    public static boolean exec(String command) {
        Process process;// Process可以控制该子进程的执行或获取该子进程的信息
        try {
            process = Runtime.getRuntime().exec(command);// exec()方法指示Java虚拟机创建一个子进程执行指定的可执行程序，并返回与该子进程对应的Process对象实例。
            // 下面两个可以获取输入输出流
        } catch (IOException e) {
            log.error(" exec error with : " + command, e);
            return false;
        }

        float timeout = TIMEOUT;
        boolean isAlive;
        boolean success = true;
        while (isAlive = isAlive(process) && timeout > 0) { 
            try {
                TimeUnit.MILLISECONDS.sleep(SLEEP_PERIOD);
                timeout -= 0.1;
            } catch (InterruptedException e) {
                log.error("InterruptedException : " + command, e);
                success = false;
                break;
            }
        }

        if (timeout <= 0) {
            log.info("转换已超时， timeout为" + TIMEOUT + " command: " + command);
            success = false;
        } else if (!isAlive) {
            try {
                InputStream errorStream = process.getErrorStream();
                InputStream inputStream = process.getInputStream();
                String errorReturns = StringUtil.convertInputStream2String(errorStream, "GBK");
                String infoReturns = StringUtil.convertInputStream2String(inputStream, "GBK");
                log.info("执行时间: " + (TIMEOUT-timeout) + "s 转换结果: " + errorReturns + infoReturns);
            } catch (IOException e) {
                log.error("IOException , message: " + e.getMessage());
            }
        }

        process.destroy(); // 销毁子进程
        process = null;

        return success;
    }

    public static boolean isAlive(Process process) {  
        try {  
            process.exitValue();  
            return false;  
        } catch (IllegalThreadStateException e) {  
            return true;  
        }  
    }  
}
