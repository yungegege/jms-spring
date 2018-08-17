
# Activemq整合spring
整合spring可以用spring的JmsTemplate来发送消息
## maven依赖
```java
<properties>
    <spring.version>4.2.5.RELEASE</spring.version>
</properties>
<dependencies>
    <dependency>
        <groupId>junit</groupId>
        <artifactId>junit</artifactId>
        <version>4.12</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-context</artifactId>
        <version>${spring.version}</version>
    </dependency>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-jms</artifactId>
        <version>${spring.version}</version>
    </dependency>
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-test</artifactId>
        <version>${spring.version}</version>
    </dependency>
    <dependency>
        <groupId>org.apache.activemq</groupId>
        <artifactId>activemq-core</artifactId>
        <version>5.7.0</version>
        <exclusions>
        <exclusion>
        <artifactId>spring-context</artifactId>
        <groupId>org.springframework</groupId>
        </exclusion>
        </exclusions>
    </dependency>
</dependencies>
```
## 代码
主题模式或者队列模式再commen.xml里面已经配置了，想用哪个就把consumer.xml的jmsContainer的property改一下就行。
这里的代码是队列模式，启动AppConsumer和AppProducer就能看到效果。
下面是项目的目录：
1. AppConsumer
```java
package com.linghua.jms.consumer;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class AppConsumer {
    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("consumer.xml","producer.xml);
    }
}
```
2. ConsumerMessageListener
```java
package com.linghua.jms.consumer;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

public class ConsumerMessageListener implements MessageListener {
    public void onMessage(Message message){
        TextMessage textMessage = (TextMessage)message;
        try{
            System.out.println("接收消息"+textMessage.getText());
        }catch (JMSException e){
            e.printStackTrace();
        }

    }
}
```
3. AppProducer
```java
package com.linghua.jms.producer;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class AppProducer {
    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("producer.xml");
        ProducerService producerService = context.getBean(ProducerService.class);
        for (int i = 0; i < 100; i++) {
            producerService.sendMessage("test"+i);
        }
        context.close();
    }
}
```
4. ProducerServiceImpl
```java
package com.linghua.jms.producer;

public interface ProducerService {
    void sendMessage(String mesage);
}

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

```
5. common.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:context="http://www.springframework.org/schema/context"
    xsi:schemaLocation="http://www.springframework.org/schema/beans
                        http://www.springframework.org/schema/beans/spring-beans.xsd
                        http://www.springframework.org/schema/context
                        http://www.springframework.org/schema/context/spring-context.xsd">

    <!--<context:annotation-config/>-->
    <context:component-scan base-package="com.linghua.jms"/>
    <!-- ActiveMQ提供的connectinFactory -->
    <bean id="targetConnectionFactory" class="org.apache.activemq.ActiveMQConnectionFactory">
    <property name="brokerURL" value="tcp://localhost:61616"/>
    </bean>
    <!-- spring jms提供的连接池 -->
    <bean id="connectinFactory" class="org.springframework.jms.connection.SingleConnectionFactory">
    <property name="targetConnectionFactory" ref="targetConnectionFactory"/>
    </bean>

    <!-- 一个队列目的地，点对点的 -->
    <bean id="queueDestination" class="org.apache.activemq.command.ActiveMQQueue">
    <constructor-arg value="queue"/>
    </bean>

    <!-- 一个主题目的地，发布订阅的 -->
    <bean id="topicDestination" class="org.apache.activemq.command.ActiveMQTopic">
    <constructor-arg value="topic"/>
    </bean>
</beans>
```
6. comsumer.xml
```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd">

    <import resource="common.xml"/>
    <!-- 配置消息监听器 -->
    <bean id="consumerMessageListener" class="com.linghua.jms.consumer.ConsumerMessageListener"/>

    <!-- 配置消息容器 -->
    <bean id="jmsContainer" class="org.springframework.jms.listener.DefaultMessageListenerContainer">
    <property name="connectionFactory" ref="connectinFactory"/>
    <property name="destination" ref="queueDestination"/>
    <!--<property name="destination" ref="topicDestination"/>-->
    <property name="messageListener" ref="consumerMessageListener"/>
    </bean>
</beans>
```
7. producer.xml
```java
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:context="http://www.springframework.org/schema/context"
    xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd 
    http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd">

    <import resource="common.xml"/>

    <!-- 配置jmsTemplate用于发送消息 -->
    <bean id="jmsTemplate" class="org.springframework.jms.core.JmsTemplate">
    <property name="connectionFactory" ref="connectinFactory"/>
    </bean>
</beans>
```
