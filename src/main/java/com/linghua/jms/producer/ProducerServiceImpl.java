package com.linghua.jms.producer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.jms.*;

@Service
public class ProducerServiceImpl implements ProducerService{ //

    @Autowired
    private JmsTemplate jmsTemplate;
    @Resource(name="queueDestination")
    private Destination destination;

    public void sendMessage(final String message) {
        //使用jmsTemplate发送消息
        jmsTemplate.send(destination, new MessageCreator() {
            //创建消息
            public Message createMessage(Session session) throws JMSException {
                TextMessage textMessage = session.createTextMessage(message);
                return textMessage;
            }
        });
        System.out.println("发送消息："+message);

    }
}
