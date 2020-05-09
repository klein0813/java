package XXX.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

public class MessageUtils {

    public static final String JNDI_RESUME_CONVERT_INFO_QUEUE_NAME = "queue/ResumeConvertInfoMDB";

    public static final String MESSAGE_TYPE = "type";
    
    public static final String CANDIDATE_ID = "candidateId";
    public static final String RESUME_ABSOLUTE_PATH = "resumeAbsolutePath";
    public static final String RESUME_CONVERT_INFO = "resumeConvertInfo";

    private static final Logger log = Logger.getLogger(MessageUtils.class);

    private static final Map<String, String> JNDI_QUEUE_NAME_MAP =
            new HashMap<String, String>();

    static {
        // viewerName -> JNDI queue name
        JNDI_QUEUE_NAME_MAP.put("ResumeConvertInfo", JNDI_RESUME_CONVERT_INFO_QUEUE_NAME);
    }

    public static String getJndiQueueName(String viewerName) {
        return JNDI_QUEUE_NAME_MAP.get(viewerName);
    }

    public static void sendResumeConvertMessage(String candidateId, String resumeAbsolutePath)
            throws HrrsException {
        log.info(">>>>> Method: sendResumeConvertMessage. candidateId: " + candidateId + "resumeAbsolutePath: " + resumeAbsolutePath + " Starting convert resumes...");
        Map<String, String> properties = new HashMap<String, String>();
        properties.put(CANDIDATE_ID, candidateId);
        properties.put(RESUME_ABSOLUTE_PATH, resumeAbsolutePath);

        ServiceLocator service = ServiceLocator.getInstance();
        service.sendTextMessage(JNDI_RESUME_CONVERT_INFO_QUEUE_NAME, RESUME_CONVERT_INFO, properties);
    }

//    public static void sendMessage(Map<String, String> params)
//        throws HrrsException {
//
//        String messageType = params.get(MESSAGE_TYPE);
//
//        //messageType must be there
//        if (messageType == null || getJndiQueueName(messageType) == null) {
//            String error = "Failed to send message, unknow message type '"
//                          + messageType + "'";
//            logger.error(error);
//            throw new HrrsException(error);
//        }
//
//
//        Map<String, String> properties = new HashMap<String, String>();
//        properties.putAll(params);
//
//        ServiceLocator service = ServiceLocator.getInstance();
//        service.sendTextMessage(getJndiQueueName(messageType), messageType, properties);
//    }
}
