package XXX.util;

import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;

public class ServiceLocator {

    private static final String QUEUE_CONNECTION_FACTORY = "QueueConnectionFactory";

    private static final Logger logger = Logger.getLogger(ServiceLocator.class);

    private static final ServiceLocator locator = new ServiceLocator();
    
    private static Context context = null;

    public static ServiceLocator getInstance() {
        return locator;
    }

    public Object lookingUp(String jndiName) throws HrrsException {
        try {
            return getContext().lookup(jndiName);
        } catch (NamingException e) {
            logger.error("Failed to find object", e);
            throw new HrrsException("Failed to find object", e);
        }
    }

    public QueueSession getQueueSession(String jndiName) throws HrrsException {
        Queue queue = (Queue) lookingUp(jndiName);
        return getQueueSession(queue);

    }

    public QueueSession getQueueSession(Queue queue) throws HrrsException {

        try {
            QueueConnectionFactory queueConnectionFactory = 
                (QueueConnectionFactory) lookingUp(QUEUE_CONNECTION_FACTORY);
            QueueConnection con = queueConnectionFactory
                    .createQueueConnection();
            QueueSession session = con.createQueueSession(false,
                    QueueSession.AUTO_ACKNOWLEDGE);
            //QueueSender sender = session.createSender(queue);
            return session;
        } catch (JMSException e) {
            logger.error("Failed to create sender", e);
            throw new HrrsException("Failed to create sender", e);
        }

    }
    
    public void sendTextMessage(String queueJndi, String text, 
            Map<String, String> properties) throws HrrsException {
        QueueConnection con = null;
        QueueSession session = null;

        try {
            QueueConnectionFactory queueConnectionFactory = 
                (QueueConnectionFactory) lookingUp(QUEUE_CONNECTION_FACTORY);
            
            con = queueConnectionFactory.createQueueConnection();
            session = con.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);
            con.start();
            
            Queue destQueue = (Queue) lookingUp(queueJndi);
            QueueSender sender = session.createSender(destQueue);
            TextMessage message = session.createTextMessage(text);
            //fill messages property
            for (Map.Entry<String, String> item : properties.entrySet()) {
                //fix for jboss issue, 
                //possibly dead lock if the value of message is null
                if (item.getValue() == null) {
                    logger.error("Queue Jndi: " + queueJndi);
                    logger.error(properties.toString());
                    throw new HrrsException("fail to gerenete message of JMS," +
                            " the value of " + item.getKey() + " cannot be null");
                }
                message.setStringProperty(item.getKey(), item.getValue());
            }
            
            sender.send(message);
            sender.close();
            logger.info("Send text Massage to '" + queueJndi + "', text=" + text);
        } catch (JMSException e) {
            String msg = "Failed to send Text Message to '" + queueJndi 
                       + "', text=" + text;
            logger.error(msg, e);
            throw new HrrsException(msg, e);
        } finally {
            try {
                if (session != null) { 
                    session.close(); 
                }
                if (con != null) { 
                    con.close(); 
                }
            } catch (JMSException e) {
                logger.error("Failed to close queue", e);
                throw new HrrsException("Failed to close queue", e);
            }
        }
    }

    private static Context getContext() throws HrrsException {
        try {
            
            if (context == null) {
                context = new InitialContext();             
            }
            
            return context;
            
        } catch (NamingException e) {
            logger.error("Failed to get context.", e);
            throw new HrrsException("Failed to initial context.", e);
        }
    }

}
