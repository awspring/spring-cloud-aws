/*
 * Copyright [2011] [Agim Emruli]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticspring.messaging.jms;

import com.amazonaws.services.sqs.AmazonSQS;

import javax.jms.*;
import java.io.Serializable;

/**
 *
 */
public class SimpleSqsSession implements Session{

	private final AmazonSQS amazonSQS;

	public SimpleSqsSession(AmazonSQS amazonSQS) {
		this.amazonSQS = amazonSQS;
	}


	public BytesMessage createBytesMessage() throws JMSException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public MapMessage createMapMessage() throws JMSException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public Message createMessage() throws JMSException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public ObjectMessage createObjectMessage() throws JMSException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public ObjectMessage createObjectMessage(Serializable serializable) throws JMSException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public StreamMessage createStreamMessage() throws JMSException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public TextMessage createTextMessage() throws JMSException {
		return new StringMessage();
	}

	public TextMessage createTextMessage(String s) throws JMSException {
		return new StringMessage(s);
	}

	public boolean getTransacted() throws JMSException {
		return false;
	}

	public int getAcknowledgeMode() throws JMSException {
		return AUTO_ACKNOWLEDGE;
	}

	public void commit() throws JMSException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	public void rollback() throws JMSException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	public void close() throws JMSException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	public void recover() throws JMSException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	public MessageListener getMessageListener() throws JMSException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public void setMessageListener(MessageListener messageListener) throws JMSException {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	public void run() {
		//To change body of implemented methods use File | Settings | File Templates.
	}

	public MessageProducer createProducer(Destination destination) throws JMSException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public MessageConsumer createConsumer(Destination destination) throws JMSException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public MessageConsumer createConsumer(Destination destination, String s) throws JMSException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public MessageConsumer createConsumer(Destination destination, String s, boolean b) throws JMSException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public Queue createQueue(String s) throws JMSException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public Topic createTopic(String s) throws JMSException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public TopicSubscriber createDurableSubscriber(Topic topic, String s) throws JMSException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public TopicSubscriber createDurableSubscriber(Topic topic, String s, String s1, boolean b) throws JMSException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public QueueBrowser createBrowser(Queue queue) throws JMSException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public QueueBrowser createBrowser(Queue queue, String s) throws JMSException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public TemporaryQueue createTemporaryQueue() throws JMSException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public TemporaryTopic createTemporaryTopic() throws JMSException {
		return null;  //To change body of implemented methods use File | Settings | File Templates.
	}

	public void unsubscribe(String s) throws JMSException {
		//To change body of implemented methods use File | Settings | File Templates.
	}
}
