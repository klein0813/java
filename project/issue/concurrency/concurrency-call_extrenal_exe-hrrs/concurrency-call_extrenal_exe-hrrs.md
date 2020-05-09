## 问题起由
HRRS系统中简历转换功能涉及到将doc、docx文档转换为pdf，实现此功能的方法是：调用外部可执行程序(soffice)完成转换；当多个用户同时使用此功能时，会同时打开多个可执行程序，产生并发问题。

## 解决思路
控制打开的外部可执行程序的个数，使其不受用户量的影响

## 解决方案
使用类的信号量控制可执行程序的个数，使用jmx消息队列（生产者-消费者模式）存放需要转换的数据

## 系统架构简单介绍
spring + hibernate + mysql + jboss

## 步骤
> 类的信号量的使用
```
public class CallSofficeSemaphore {

    /**
     * 信号量数值为1，因此每次只能一条线程执行，其他线程进入等待状态
     */
    static private final Semaphore semaphore = new Semaphore(1);

    static private String outputFileRootDir = ConfigLoader.DATA_CONVERT_RESUMES_PATH;

    public static String toConvert(String inputFilePath) {
        semaphore.acquireUninterruptibly();
        String outputFilePath = doTask(inputFilePath);
        semaphore.release();

        return outputFilePath;
    }

    private static String doTask(String inputFilePath) {

        return ConvertFile.convertToPDF(outputFileRootDir, inputFilePath);
    }
}
```
> 消息队列
* 队列的配置
* * jboss配置
<br> 路径：[ \jboss\server\default\deploy\jms\jbossmq-destinations-service.xml ]
```
    <mbean code="org.jboss.mq.server.jmx.Queue" name="jboss.mq.destination:service=Queue,name=HRRSResumeConvertInfoQueue">
        <depends optional-attribute-name="DestinationManager">jboss.mq:service=DestinationManager</depends>
        <attribute name="MessageCounterHistoryDayLimit">-1</attribute>
        <attribute name="RedeliveryLimit">3</attribute>
        <attribute name="RedeliveryDelay">5000</attribute>
    </mbean>
```
* * spring context 配置
<br> 应具体实现而变化
```
<!-- Resume Convert Info Queue -->
    <bean id="resumeDestination" class="org.springframework.jndi.JndiObjectFactoryBean">
        <property name="jndiTemplate">
            <ref bean="jndiTemplate" />
        </property>
        <property name="jndiName">
            <value>queue/HRRSResumeConvertInfoQueue</value>
        </property>
    </bean>

    <bean id="resumeConvertInfoMessageSender" class="XXX.mdb.ResumeConvertInfoMessageSender">
        <property name="template" ref="jmsTemplate" />
        <property name="destination" ref="resumeDestination" />
    </bean>

    <bean id="resumeConvertInfoMessageListener" class="XXX.mdb.ResumeConvertInfoMessageListener" />

    <bean id="resumeJMSContainer" class="org.springframework.jms.listener.DefaultMessageListenerContainer">
        <property name="connectionFactory" ref="jmsConnectionFactory" />
        <property name="destination" ref="resumeDestination" />
        <property name="messageListener" ref="resumeConvertInfoMessageListener" />
        <property name="sessionTransacted" value="true" />
    </bean>
```
* 代码实现
* * 生产者实现
```
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;


public class ResumeConvertInfoMessageSender {
    private static final Log log = LogFactory.getLog(ResumeConvertInfoMessageSender.class);

    public static final String CANDIDATE_ID = "candidateId";
    public static final String RESUME_ABSOLUTE_PATH = "resumeAbsolutePath";

    private JmsTemplate template;
    private Destination destination;

    public void sendRsumeConvertInfo(final String candidateId, final String resumeAbsolutePath) {
        template.send(destination, new MessageCreator() {
            public Message createMessage(Session session) throws JMSException {
                Message msg = session.createMapMessage();
                msg.setStringProperty(CANDIDATE_ID, candidateId);
                msg.setStringProperty(RESUME_ABSOLUTE_PATH, resumeAbsolutePath);

                return msg;
            }
        });

        log.info("sendRsumeConvertInfo, " + " candidateId： " + candidateId + " resumeAbsolutePath: " + resumeAbsolutePath);
    }

    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    public void setTemplate(JmsTemplate template) {
        this.template = template;
    }
}
```
* * 消费者实现
```
import java.util.ArrayList;
import java.util.List;

import javax.jms.Message;
import javax.jms.MessageListener;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import XXX.core.Exp;
import XXX.entity.po.candidate.Candidate;
import XXX.entity.po.candidate.Resume;
import XXX.entity.po.candidate.ResumeContent;
import XXX.parser.fileparser.async.CallSofficeSemaphore;
import XXX.service.CandidateService;
import XXX.service.ResumeService;
import XXX.util.StringUtil;

@Transactional
public class ResumeConvertInfoMessageListener implements MessageListener {

    private static final Logger log = Logger.getLogger(ResumeConvertInfoMessageListener.class.getName());

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private CandidateService candidateService;

    @Override
    public void onMessage(Message message) {
        log.info("onMessae, Starting convert resumes...");
        try {
            String candidateId = message.getStringProperty(ResumeConvertInfoMessageSender.CANDIDATE_ID);
            String resumeAbsolutePath = message.getStringProperty(ResumeConvertInfoMessageSender.RESUME_ABSOLUTE_PATH);
            String outputFilePath = CallSofficeSemaphore.toConvert(resumeAbsolutePath);
            String showHtml = null;
            if (outputFilePath != null) {
                showHtml = StringUtil.assembleHtml4AccessPath(outputFilePath);
            }
            update(candidateId, showHtml);
        } catch (Exception e) {
            log.error("onMessage, Exception: message, " + e.getMessage());
        }
    }
}
```
* 超时处理
```
    float timeout = TIMEOUT;
    boolean isAlive;
    boolean success = true;
    while (isAlive = isAlive(process) && timeout > 0) { 
        try {
            TimeUnit.MILLISECONDS.sleep(SLEEP_PERIOD);
            timeout -= SLEEP_PERIOD / 1000;
        } catch (InterruptedException e) {
            log.error("InterruptedException : " + command, e);
            success = false;
            break;
        }
    }

    public static boolean isAlive(Process process) {  
        try {  
            process.exitValue();  
            return false;  
        } catch (IllegalThreadStateException e) {  
            return true;  
        }  
    }  

```