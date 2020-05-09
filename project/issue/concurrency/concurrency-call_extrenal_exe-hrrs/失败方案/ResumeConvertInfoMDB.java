package XXX.mdb;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.apache.log4j.Logger;

import XXX.bean.CommonBean;
import XXX.bean.util.BeanUtil;
import XXX.entity.po.candidate.Candidate;
import XXX.entity.po.candidate.Resume;
import XXX.entity.po.candidate.ResumeContent;
import XXX.parser.fileparser.async.CallSofficeSemaphore;
import XXX.util.MessageUtils;
import XXX.util.StringUtil;

@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "queue/ResumeConvertInfoMDB") })
public class ResumeConvertInfoMDB implements MessageListener {

    private static transient final Logger log = Logger
            .getLogger(ResumeConvertInfoMDB.class.getName());

    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    public void onMessage(Message message) {
        // TODO Auto-generated method stub
        log.info(">>>>> Enter class: " + ResumeConvertInfoMDB.class.getName()
                + ", Method: onMessage. Starting convert resumes...");
        try {
            String candidateId = message.getStringProperty(MessageUtils.CANDIDATE_ID);
            String resumeAbsolutePath = message.getStringProperty(MessageUtils.RESUME_ABSOLUTE_PATH);
            String outputFilePath = CallSofficeSemaphore.toConvert(resumeAbsolutePath);
            String showHtml = null;
            if (outputFilePath != null) {
                showHtml = StringUtil.assembleHtml4AccessPath(outputFilePath);
            }
            update(candidateId, showHtml);
//            List<String> candidateIds = new ArrayList<String>();
//            List<String> resumeAbsolutePaths = new ArrayList<String>();
            
//            Enumeration<?> propertyMap = message.getPropertyNames();
//            while(propertyMap.hasMoreElements()) {
//                String candidateId = propertyMap.nextElement().toString();
//                String resumeAbsolutePath = message.getStringProperty(candidateId);
//                candidateIds.add(candidateId);
//                resumeAbsolutePaths.add(resumeAbsolutePath);
//            }

//            List<String> outputFilePaths = new ArrayList<String>();
//            int end = candidateIds.size() - 1;
//            int begin = 0;
//            int mid = begin + 9 < end ? begin + 9 : end;
//            while (end >= begin) {
//                List<String> tempResumeAbsolutePaths = resumeAbsolutePaths.subList(begin, mid);
//                List<String> tempCandidateIds = resumeAbsolutePaths.subList(begin, mid);
//                List<String> tempOutputFilePaths = CallExternalProgramSemaphore.toConvert(tempResumeAbsolutePaths, tempCandidateIds);
//
//                outputFilePaths.addAll(tempOutputFilePaths);
//                begin = mid + 1;
//                mid = begin + 9 < end ? begin + 9 : end;
//            }
//
//             List<String> showHtmls = new ArrayList<String>();
//                for (int j = 0; j < outputFilePaths.size(); j++) {
//                    String outputFilePath = outputFilePaths.get(j);
//                    String candidateId = outputFilePath.substring(outputFilePath.lastIndexOf(File.separator)).split("_")[0];
//                    String showHtml = null;
//
//                    if (candidateIds.contains(candidateId)) {
//                        candidateIds.remove(candidateId);
//                        showHtml = StringUtil.assembleHtml4AccessPath(outputFilePath);
//                    }
//                    showHtmls.add(showHtml);
//                }
//
//                batchUpdateAll(candidateIds.toArray(new String[candidateIds.size()]), showHtmls);
            
        } catch (JMSException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void update(String candidateId, String showHtml) {
        log.info(">>>>>> update, candidateId: " + candidateId + " showHtml: " + showHtml);
        CommonBean serviceBean = BeanUtil.getInstance().getBean();
        Candidate candidate = serviceBean.findById(Candidate.class, candidateId);
        Resume mainResume = candidate.getMainResume();
        Resume resume = candidate.getResumes().iterator().next();
        ResumeContent resumeContent = mainResume.getResumeContent();

        if (showHtml != null) {
            resumeContent.setContent(showHtml.getBytes());
        } else {
            resumeContent.setContent(null);
        }
        mainResume.setResumeContent(resumeContent);
        resume.setResumeContent(resumeContent);

        List<Resume> updateList = new ArrayList<Resume>();
        updateList.add(mainResume);
        updateList.add(resume);
        serviceBean.batchUpdate(updateList);
    }

    @SuppressWarnings("unused")
    private void batchUpdateAll(String[] candidateId, List<String> showHtmls) {
        CommonBean serviceBean = BeanUtil.getInstance().getBean();

         String query = "select * from Candidate c "
                    + " where c.candidateId in '" + candidateId;
        @SuppressWarnings("unchecked")
        List<Candidate> candidates = (List<Candidate>) serviceBean.findByQuery(query);

        List<Resume> updateList = new ArrayList<Resume>();
        for (int i = 0; i < candidates.size(); i++) {
            Resume mainResume = candidates.get(i).getMainResume();
            Resume resume = candidates.get(i).getResumes().iterator().next();
            ResumeContent resumeContent = mainResume.getResumeContent();

            String showHtml = showHtmls.get(i);
            if (showHtml != null) {
                resumeContent.setContent(showHtml.getBytes());
            } else {
                resumeContent.setContent(null);
            }

            mainResume.setResumeContent(resumeContent);
            resume.setResumeContent(resumeContent);

            updateList.add(mainResume);
            updateList.add(resume);
        }

        serviceBean.batchUpdate(updateList);
    }
}
